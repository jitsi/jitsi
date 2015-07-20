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
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The activator for the phone number contact source bundle.
 *
 * @author Yana Stamcheva
 */
public class PNContactSourceActivator
    implements  BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>PNContactSourceActivator</tt> class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PNContactSourceActivator.class);

    /**
     * The bundle context.
     */
    static BundleContext bundleContext = null;

    /**
     * Providers of contact info.
     */
    private static List<ProtocolProviderService> phoneProviders;

    /**
     * Listens for registration state changes.
     */
    private static ProtocolProviderRegistrationListener ppRegListener;

    /**
     * The contact source.
     */
    private static final PhoneNumberContactSource phoneNumberContactSource
        = new PhoneNumberContactSource();

    /**
     * The resource service.
     */
    private static ResourceManagementService resources = null;

    /**
     * The registered PhoneNumberI18nService.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * Starts this bundle.
     *
     * @param context the bundle context where we register and obtain services.
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        bundleContext.registerService(
            ContactSourceService.class.getName(),
            phoneNumberContactSource,
            null);
    }

    public void stop(BundleContext context) throws Exception
    {
    }

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                        bundleContext, ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Returns a list of all currently registered server stored contact info
     * providers.
     *
     * @return a list of all currently registered server stored contact info
     * providers
     */
    public static List<ProtocolProviderService> getPhoneNumberProviders()
    {
        if (phoneProviders != null)
            return phoneProviders;

        phoneProviders = new LinkedList<ProtocolProviderService>();

        ppRegListener = new ProtocolProviderRegistrationListener();

        bundleContext.addServiceListener(
            new ProtocolProviderServiceRegListener());

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                ProtocolProviderService protocolProvider;

                for (AccountID accountID
                        : providerFactory.getRegisteredAccounts())
                {
                    serRef = providerFactory.getProviderForAccount(accountID);

                    protocolProvider
                        = (ProtocolProviderService) bundleContext
                            .getService(serRef);

                    handleProviderAdded(protocolProvider);
                }
            }
        }
        return phoneProviders;
    }

    /**
     * Listens for <tt>ProtocolProviderService</tt> registrations.
     */
    private static class ProtocolProviderServiceRegListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            Object service = bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof ProtocolProviderService))
            {
                return;
            }

            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                handleProviderAdded((ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                handleProviderRemoved((ProtocolProviderService) service);
                break;
            }
        }
    }

    /**
     * Handles the registration of a new <tt>ProtocolProviderService</tt>. Adds
     * the given <tt>protocolProvider</tt> to the list of queried providers.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to add
     */
    private static void handleProviderAdded(
            ProtocolProviderService protocolProvider)
    {
        if (protocolProvider.getOperationSet(
                OperationSetServerStoredContactInfo.class) != null
            && !phoneProviders.contains(protocolProvider))
        {
            if(protocolProvider.isRegistered())
            {
                phoneProviders.add(protocolProvider);
            }
            else
            {
                protocolProvider.addRegistrationStateChangeListener(
                    ppRegListener);
            }
        }
    }

    /**
     * Handles the un-registration of a <tt>ProtocolProviderService</tt>.
     * Removes the given <tt>protocolProvider</tt> from the list of queried
     * providers.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to remove
     */
    private static void handleProviderRemoved(
            ProtocolProviderService protocolProvider)
    {
        if (phoneProviders.contains(protocolProvider))
            phoneProviders.remove(protocolProvider);

        protocolProvider.removeRegistrationStateChangeListener(ppRegListener);
    }

    /**
     * Returns the PhoneNumberI18nService.
     * @return returns the PhoneNumberI18nService.
     */
    public static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if(phoneNumberI18nService == null)
        {
            phoneNumberI18nService = ServiceUtils.getService(
                bundleContext,
                PhoneNumberI18nService.class);
        }

        return phoneNumberI18nService;
    }

    /**
     * Listens for provider change in registration state.
     */
    private static class ProtocolProviderRegistrationListener
        implements RegistrationStateChangeListener
    {
        @Override
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            ProtocolProviderService protocolProvider = evt.getProvider();

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                if(!phoneProviders.contains(protocolProvider))
                    phoneProviders.add(protocolProvider);
            }
        }
    }
}
