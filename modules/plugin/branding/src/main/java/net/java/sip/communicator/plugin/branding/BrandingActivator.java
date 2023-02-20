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

import java.awt.*;
import java.awt.Desktop.*;
import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Branding bundle activator.
 */
public class BrandingActivator
    extends DependentActivator
{
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BrandingActivator.class);
    private static ResourceManagementService resourcesService;
    private static BrowserLauncherService browserLauncherService;

    public BrandingActivator()
    {
        super(
            UIService.class,
            ResourceManagementService.class,
            BrowserLauncherService.class
        );
    }

    /**
     * The dependent service is available and the bundle will start.
     */
    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        resourcesService = getService(ResourceManagementService.class);
        browserLauncherService = getService(BrowserLauncherService.class);

        // register the about dialog menu entry
        if (!registerAboutHandler())
        {
            registerMenuEntryNonMacOSX(bundleContext);
        }
    }

    private boolean registerAboutHandler()
    {
        try
        {
            if (Desktop.isDesktopSupported())
            {
                var desktop = Desktop.getDesktop();
                if (desktop != null && desktop.isSupported(Action.APP_ABOUT))
                {
                    desktop.setAboutHandler(e -> AboutWindow.showAboutWindow());
                }
            }
        }
        catch (Exception ex)
        {
            logger.error("Failed to register About handling", ex);
        }

        return false;
    }

    private void registerMenuEntryNonMacOSX(
        BundleContext bundleContext)
    {
        // Register the about window plugin component in the main help menu.
        Hashtable<String, String> helpMenuFilter = new Hashtable<>();
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

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        return resourcesService;
    }

    public static BrowserLauncherService getBrowserLauncherService()
    {
        return browserLauncherService;
    }
}
