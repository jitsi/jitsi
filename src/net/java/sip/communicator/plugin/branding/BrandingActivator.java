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
package net.java.sip.communicator.plugin.branding;

import java.lang.reflect.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Branding bundle activator.
 */
public class BrandingActivator
    extends AbstractServiceDependentActivator
{
    private final Logger logger = Logger.getLogger(BrandingActivator.class);
    private static BundleContext bundleContext;
    private static ResourceManagementService resourcesService;

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
     * The dependent service is available and the bundle will start.
     * @param dependentService the UIService this activator is waiting.
     */
    @Override
    public void start(Object dependentService)
    {
        // register the about dialog menu entry
        registerMenuEntry((UIService)dependentService);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
    }

    /**
     * Register the about menu entry.
     * @param uiService
     */
    private void registerMenuEntry(UIService uiService)
    {
        if ((uiService == null)
                || !uiService.useMacOSXScreenMenuBar()
                || !registerMenuEntryMacOSX(uiService))
        {
            registerMenuEntryNonMacOSX(uiService);
        }
    }

    private boolean registerMenuEntryMacOSX(UIService uiService)
    {
        try
        {
            Class<?> clazz =
                Class
                    .forName("net.java.sip.communicator.plugin.branding.MacOSXAboutRegistration");
            Method method = clazz.getMethod("run", (Class<?>[]) null);
            Object result = method.invoke(null, (Object[]) null);

            if (result instanceof Boolean)
            {
                return ((Boolean) result).booleanValue();
            }
        }
        catch (Exception ex)
        {
            logger.error("Failed to register Mac OS X-specific About handling.",
                ex);
        }

        return false;
    }

    private void registerMenuEntryNonMacOSX(UIService uiService)
    {
        // Register the about window plugin component in the main help menu.
        Hashtable<String, String> helpMenuFilter
            = new Hashtable<String, String>();
        helpMenuFilter.put( Container.CONTAINER_ID,
                            Container.CONTAINER_HELP_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                    Container.CONTAINER_HELP_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AboutWindowPluginComponent(getContainer(), this);
                }
            },
            helpMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("ABOUT WINDOW ... [REGISTERED]");

        // Register the about window plugin component in the chat help menu.
        Hashtable<String, String> chatHelpMenuFilter
            = new Hashtable<String, String>();
        chatHelpMenuFilter.put( Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_HELP_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                    Container.CONTAINER_CHAT_HELP_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AboutWindowPluginComponent(getContainer(), this);
                }
            },
            chatHelpMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("CHAT ABOUT WINDOW ... [REGISTERED]");
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
