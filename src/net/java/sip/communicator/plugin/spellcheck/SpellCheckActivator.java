/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

import javax.swing.*;

/**
 * Enabling and disabling osgi functionality for the spell checker.
 *
 * @author Damian Johnson
 */
public class SpellCheckActivator
    extends AbstractServiceDependentActivator
{
    /**
     * Our Logger.
     */
    private static final Logger logger = Logger
            .getLogger(SpellCheckActivator.class);

    static BundleContext bundleContext;

    private static UIService uiService;

    private static FileAccessService faService;

    private static ConfigurationService configService;

    private static SpellChecker checker = null;

    /**
     * Called when this bundle is started.
     *
     * @param dependentService the service we depend on.
     */
    @Override
    public void start(Object dependentService)
    {
        // UI-Service started.

         // adds button to toggle spell checker
        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CHAT_TOOL_BAR.getID());

        // adds field to change language
        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(Container.CONTAINER_CHAT_TOOL_BAR,
                                       Container.RIGHT,
                                       -1,
                                       false)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    LanguageMenuBarCreator creator =
                        new LanguageMenuBarCreator(this);

                    try
                    {
                        if(!SwingUtilities.isEventDispatchThread())
                            SwingUtilities.invokeAndWait(creator);
                        else
                            creator.run();

                        return creator.menuBar;
                    }
                    catch(Throwable t)
                    {
                        logger.error("Error creating LanguageMenuBar", t);
                    }

                    return null;
                }
            },
            containerFilter);

    }

    /**
     * Setting context to the activator, as soon as we have one.
     *
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * This activator depends on UIService.
     * @return the class name of uiService.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * Creates and loads everything when needed.
     */
    private class LanguageMenuBarCreator
        implements Runnable
    {
        LanguageMenuBar menuBar;
        final PluginComponentFactory parentFactory;

        LanguageMenuBarCreator(PluginComponentFactory parentFactory)
        {
            this.parentFactory = parentFactory;
        }

        public void run()
        {
            synchronized(SpellCheckActivator.this)
            {
                if(checker == null)
                {
                    checker = new SpellChecker();
                }
            }

            try
            {
                checker.start(bundleContext);
            }
            catch(Exception ex)
            {
                logger.error("Error starting SpellChecker", ex);
            }
            menuBar = new LanguageMenuBar(checker, parentFactory);
            menuBar.createSpellCheckerWorker(checker.getLocale()).start();
        }
    }

    /**
     * Returns the <tt>UIService</tt>.
     *
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        if (uiService != null)
            return uiService;

        // retrieves needed services
        ServiceReference uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        return uiService;
    }

    /**
     * Returns the <tt>FileAccessService</tt>.
     *
     * @return the <tt>FileAccessService</tt>
     */
    public static FileAccessService getFileAccessService()
    {
        if (faService != null)
            return faService;

        ServiceReference faServiceReference =
            bundleContext
                .getServiceReference(FileAccessService.class.getName());
        faService =
            (FileAccessService) bundleContext.getService(faServiceReference);

        return faService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     *
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfigService()
    {
        if (configService != null)
            return configService;

        ServiceReference configServiceRef =
            bundleContext.getServiceReference(ConfigurationService.class
                .getName());

        configService =
            (ConfigurationService) bundleContext.getService(configServiceRef);

        return configService;
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {
    }
}
