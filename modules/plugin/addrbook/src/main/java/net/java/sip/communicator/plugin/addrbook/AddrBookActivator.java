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
package net.java.sip.communicator.plugin.addrbook;

import java.util.*;
import net.java.sip.communicator.plugin.addrbook.macosx.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.calendar.*;
import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the addrbook plug-in which provides
 * support for OS-specific Address Book.
 *
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class AddrBookActivator
    extends DependentActivator
{
    /**
     * Boolean property that defines whether the integration of the Outlook
     * address book is enabled.
     */
    public static final String PNAME_ENABLE_MICROSOFT_OUTLOOK_SEARCH =
        "plugin.addrbook.ENABLE_MICROSOFT_OUTLOOK_SEARCH";

    /**
     * Boolean property that defines whether the integration of the OS X
     * address book is enabled.
     */
    public static final String PNAME_ENABLE_MACOSX_ADDRESS_BOOK_SEARCH =
        "plugin.addrbook.ENABLE_MACOSX_ADDRESS_BOOK_SEARCH";

    /**
     * Boolean property that defines whether changing the default IM application
     * is enabled or not.
     */
    public static final String PNAME_ENABLE_DEFAULT_IM_APPLICATION_CHANGE =
        "plugin.addrbook.ENABLE_DEFAULT_IM_APPLICATION_CHANGE";

    /**
     * Boolean property that defines whether Jitsi should be the default IM
     * Application or not.
     */
    public static final String PNAME_MAKE_JITSI_DEFAULT_IM_APPLICATION =
        "plugin.addrbook.REGISTER_AS_DEFAULT_IM_PROVIDER";

    /**
     * The <tt>Logger</tt> used by the <tt>AddrBookActivator</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AddrBookActivator.class);

    /**
     * The <tt>BundleContext</tt> in which the addrbook plug-in is started.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ContactSourceService</tt> implementation for the OS-specific
     * Address Book.
     */
    private static AsyncContactSourceService css;

    /**
     * The <tt>ServiceRegistration</tt> of {@link #css} in the
     * <tt>BundleContext</tt> in which this <tt>AddrBookActivator</tt> has been
     * started.
     */
    private static ServiceRegistration<ContactSourceService> cssServiceRegistration;

    /**
     * The <tt>ResourceManagementService</tt> through which we access resources.
     */
    private static ResourceManagementService resourceService;

    /**
     * The <tt>ConfigurationService</tt> through which we access configuration
     * properties.
     */
    private static ConfigurationService configService;

    /**
     * The calendar service
     */
    private static CalendarServiceImpl calendarService = null;

    /**
     * List of the providers with registration listener.
     */
    private static List<ProtocolProviderService> providers
        = new ArrayList<ProtocolProviderService>();

    /**
     * The registered PhoneNumberI18nService.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * The registration change listener.
     */
    private static RegistrationStateChangeListener providerListener
        = new RegistrationStateChangeListener()
        {
            @Override
            public void registrationStateChanged(
                    RegistrationStateChangeEvent ev)
            {
                if(ev.getNewState().equals(RegistrationState.REGISTERED)
                        && (calendarService != null))
                {
                    calendarService.handleProviderAdded(ev.getProvider());
                }
            }
        };

    /**
     * A listener for addition of <tt>ProtocolProviderService</tt>
     */
    private static ServiceListener serviceListener
        = new ServiceListener()
        {
            @Override
            public void serviceChanged(ServiceEvent ev)
            {
                Object service
                    = bundleContext.getService(ev.getServiceReference());

                if (! (service instanceof ProtocolProviderService))
                    return;

                ProtocolProviderService pps = (ProtocolProviderService) service;

                switch (ev.getType())
                {
                case ServiceEvent.REGISTERED:
                    synchronized(providers)
                    {
                        providers.add(pps);
                    }
                    if(!pps.isRegistered())
                        pps.addRegistrationStateChangeListener(providerListener);
                    break;

                case ServiceEvent.UNREGISTERING:
                    synchronized(providers)
                    {
                        providers.remove(pps);
                    }
                    pps.removeRegistrationStateChangeListener(providerListener);
                    break;
                }
            }
        };

    public AddrBookActivator()
    {
        super(
            ConfigurationService.class,
            ResourceManagementService.class
        );
    }

    /**
     * Gets the <tt>ResourceManagementService</tt> to be used by the
     * functionality of the addrbook plug-in.
     *
     * @return the <tt>ResourceManagementService</tt> to be used by the
     * functionality of the addrbook plug-in
     */
    public static ResourceManagementService getResources()
    {
        return resourceService;
    }

    public static CalendarServiceImpl getCalendarService()
    {
        return calendarService;
    }

    /**
     * Gets the <tt>ConfigurationService</tt> to be used by the
     * functionality of the addrbook plug-in.
     *
     * @return the <tt>ConfigurationService</tt> to be used by the
     * functionality of the addrbook plug-in
     */
    public static ConfigurationService getConfigService()
    {
        return configService;
    }

    /**
     * Starts the addrbook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the addrbook
     * plug-in is to be started
     * @see BundleActivator#start(BundleContext)
     */
    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        if (logger.isInfoEnabled())
        {
            logger.info(
                    "Address book \"plugin.addrbook.ADDRESS_BOOKS\" ..."
                        + " [STARTED]");
        }

        configService = getService(ConfigurationService.class);
        resourceService = getService(ResourceManagementService.class);
        AddrBookActivator.bundleContext = bundleContext;

        Dictionary<String, String> properties = new Hashtable<String, String>();

        // Registers the sip config panel as advanced configuration form.
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.CONTACT_SOURCE_TYPE);

        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        AdvancedConfigForm.class.getName(),
                        getClass().getClassLoader(),
                        null,
                        "plugin.addrbook.ADDRESS_BOOKS",
                        101,
                        false),
                properties);

        try
        {
            startService();
        }
        catch (Exception e)
        {
            logger.error("Failed to load OS native contact address book");
        }

        startCalendarService();
    }

    /**
     * Stops the addrbook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the addrbook
     * plug-in is to be stopped
     * @throws Exception if anything goes wrong while stopping the addrbook
     * plug-in
     * @see BundleActivator#stop(BundleContext)
     */
    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        super.stop(bundleContext);
        if (logger.isInfoEnabled())
        {
            logger.info(
                    "Address book \"plugin.addrbook.ADDRESS_BOOKS\" ..."
                        + " [STOPPED]");
        }

        stopService();
        stopCalendarService();
    }

    /**
     * Starts the address book service.
     */
    static void startService()
    {
        // Register the ContactSourceService implementation (if any)
        if (OSUtils.IS_WINDOWS)
        {
            checkImDefaultSettings();
            if (configService.getBoolean(
                PNAME_ENABLE_MICROSOFT_OUTLOOK_SEARCH,
                true))
            {
                if (MsOutlookAddrBookContactSourceService.isLoaded)
                {
                    try
                    {
                        css = new MsOutlookAddrBookContactSourceService();
                        MsOutlookAddrBookContactSourceService contactSource
                            = (MsOutlookAddrBookContactSourceService) css;

                        MsOutlookAddrBookContactSourceService.initMAPI(
                            contactSource.createNotificationDelegate());
                    }
                    catch (MsOutlookMAPIHResultException e)
                    {
                        logger.error("Failed to start Outlook source", e);
                        return;
                    }
                }
            }
        }
        else if (OSUtils.IS_MAC
            && configService.getBoolean(
            PNAME_ENABLE_MACOSX_ADDRESS_BOOK_SEARCH,
            true))
        {
            css = new MacOSXAddrBookContactSourceService();
        }
        else
        {
            return;
        }

        cssServiceRegistration
            = bundleContext.registerService(
            ContactSourceService.class,
            css,
            null);
        logger.info(
            "Address book \"" + css.getDisplayName()
                + "\" ... [REGISTERED]");
    }

    /**
     * Tries to start the calendar service.
     */
    static void startCalendarService()
    {
        if(OSUtils.IS_WINDOWS
                && !getConfigService().getBoolean(
                        CalendarService.PNAME_FREE_BUSY_STATUS_DISABLED,
                        false))
        {
            try
            {
                calendarService = new CalendarServiceImpl();
            }
            catch (UnsatisfiedLinkError e)
            {
                logger
                    .error("Native calendar not available: " + e.getMessage());
                return;
            }

            try
            {
                MsOutlookAddrBookContactSourceService.initMAPI(null);
            }
            catch (UnsatisfiedLinkError e)
            {
                logger.error("Native library not avail");
            }
            catch (MsOutlookMAPIHResultException ex)
            {
                String msg = "Failed to initialize MAPI: " + ex.getMessage();

                logger.error(msg);
                if (logger.isDebugEnabled())
                    logger.debug(msg, ex);
                return;
            }

            bundleContext.addServiceListener(serviceListener);
            for(ProtocolProviderService pps : getProtocolProviders())
            {
                if(!pps.isRegistered())
                    pps.addRegistrationStateChangeListener(providerListener);
            }
            bundleContext.registerService(CalendarService.class.getName(),
                calendarService, null);
            calendarService.start();
        }
    }

    /**
     * Stops the calendar service.
     */
    static void stopCalendarService()
    {
        if(OSUtils.IS_WINDOWS
                && !getConfigService().getBoolean(
                        CalendarService.PNAME_FREE_BUSY_STATUS_DISABLED,
                        false))
        {
            bundleContext.removeServiceListener(serviceListener);
            synchronized(providers)
            {
                for(ProtocolProviderService pps : getProtocolProviders())
                   pps.removeRegistrationStateChangeListener(providerListener);
            }
            calendarService = null;
            MsOutlookAddrBookContactSourceService.UninitializeMAPI();
        }
    }

    /**
     * Stop the previously registered service.
     */
    static void stopService()
    {
        try
        {
            if (cssServiceRegistration != null)
            {
                cssServiceRegistration.unregister();
                cssServiceRegistration = null;
            }
        }
        finally
        {
            if (css != null)
            {
                if (css instanceof AsyncContactSourceService)
                    ((AsyncContactSourceService) css).stop();

                if (logger.isInfoEnabled())
                {
                    logger.info(
                            "Address book \"" + css.getDisplayName()
                                + "\" ... [UNREGISTERED]");
                }

                css = null;
            }
        }
    }

    private static void checkImDefaultSettings()
    {
        if (!configService.getBoolean(
            PNAME_ENABLE_DEFAULT_IM_APPLICATION_CHANGE,
            true))
        {
            return;
        }

        String isDefaultIMAppString
            = configService.getString(PNAME_MAKE_JITSI_DEFAULT_IM_APPLICATION);

        if(isDefaultIMAppString == null)
        {
            configService.setProperty(
                PNAME_MAKE_JITSI_DEFAULT_IM_APPLICATION,
                RegistryHandler.isJitsiDefaultIMApp());
        }
        else
        {
            boolean isDefaultIMApp
                = Boolean.parseBoolean(isDefaultIMAppString);

            if(RegistryHandler.isJitsiDefaultIMApp() != isDefaultIMApp)
            {
                if(isDefaultIMApp)
                    setAsDefaultIMApplication();
                else
                    unsetDefaultIMApplication();
            }
        }
    }

    /**
     * Sets Jitsi as Default IM application.
     */
    public static void setAsDefaultIMApplication()
    {
        if (OSUtils.IS_WINDOWS)
            RegistryHandler.setJitsiAsDefaultApp();
    }

    /**
     * Unsets Jitsi as Default IM application.
     */
    public static void unsetDefaultIMApplication()
    {
        if (OSUtils.IS_WINDOWS)
            RegistryHandler.unsetDefaultApp();
    }

    public static List<ProtocolProviderService> getProtocolProviders()
    {
        List<ProtocolProviderService> result;
        synchronized(providers)
        {
            ServiceReference[] ppsRefs;
            try
            {
                ppsRefs
                    = bundleContext.getServiceReferences(
                            ProtocolProviderService.class.getName(),
                            null);
            }
            catch (InvalidSyntaxException ise)
            {
                ppsRefs = null;
            }

            if ((ppsRefs != null) && (ppsRefs.length != 0))
            {
                for (ServiceReference ppsRef : ppsRefs)
                {
                    ProtocolProviderService pps
                        = (ProtocolProviderService)
                            bundleContext.getService(ppsRef);
                    providers.add(pps);
                }
            }
        }

        synchronized(providers)
        {
            result = new ArrayList<ProtocolProviderService>(providers);
        }

        return result;
    }

    /**
     * Returns the PhoneNumberI18nService.
     * @return returns the PhoneNumberI18nService.
     */
    public static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if(phoneNumberI18nService == null)
        {
            phoneNumberI18nService
                = ServiceUtils.getService(
                        bundleContext,
                        PhoneNumberI18nService.class);
        }
        return phoneNumberI18nService;
    }
}
