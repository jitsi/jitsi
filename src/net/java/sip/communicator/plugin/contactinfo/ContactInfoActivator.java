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
package net.java.sip.communicator.plugin.contactinfo;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The Activator of the Contact Info bundle.
 *
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoActivator implements BundleActivator
{
    private Logger logger = Logger.getLogger(ContactInfoActivator.class);

    /**
     * Indicates if the contact info button is enabled in the chat window.
     */
    private static final String ENABLED_IN_CHAT_WINDOW_PROP
        = "net.java.sip.communicator.plugin.contactinfo." +
            "ENABLED_IN_CHAT_WINDOW_PROP";

    /**
     * Indicates if the contact info button is enabled in the call window.
     */
    private static final String ENABLED_IN_CALL_WINDOW_PROP
        = "net.java.sip.communicator.plugin.contactinfo." +
            "ENABLED_IN_CALL_WINDOW_PROP";

    private static BrowserLauncherService browserLauncherService;

    /**
     * The image loader service implementation.
     */
    private static ImageLoaderService<?> imageLoaderService = null;

    /**
     * The contact list service implementation.
     */
    private static MetaContactListService metaCListService;

    static BundleContext bundleContext;

    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new ContactInfoPluginComponentFactory(
                    Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU),
            containerFilter);

        if(getConfigService().getBoolean(ENABLED_IN_CHAT_WINDOW_PROP, false))
        {
            containerFilter = new Hashtable<String, String>();
            containerFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_CHAT_TOOL_BAR.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new ContactInfoPluginComponentFactory(
                        Container.CONTAINER_CHAT_TOOL_BAR),
                containerFilter);
        }

        if(getConfigService().getBoolean(ENABLED_IN_CALL_WINDOW_PROP, false))
        {
            containerFilter = new Hashtable<String, String>();
            containerFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_CALL_DIALOG.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new ContactInfoPluginComponentFactory(
                        Container.CONTAINER_CALL_DIALOG),
                containerFilter);
        }

        if (logger.isInfoEnabled())
            logger.info("CONTACT INFO... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }

    /**
     * Returns the imageLoaderService instance, if missing query osgi for it.
     * @return the imageLoaderService.
     */
    public static ImageLoaderService<?> getImageLoaderService()
    {
        if(imageLoaderService == null)
        {
            imageLoaderService
                = ServiceUtils.getService(
                        bundleContext,
                        ImageLoaderService.class);
        }

        return imageLoaderService;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null)
        {
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        return ServiceUtils.getService(bundleContext,
            ConfigurationService.class);
    }

    /**
     * Contact info create factory.
     */
    private class ContactInfoPluginComponentFactory
        extends PluginComponentFactory
    {
        ContactInfoPluginComponentFactory(Container c)
        {
            super(c);
        }

        @Override
        protected PluginComponent getPluginInstance()
        {
            return new ContactInfoMenuItem(getContainer(), this);
        }
    }
}
