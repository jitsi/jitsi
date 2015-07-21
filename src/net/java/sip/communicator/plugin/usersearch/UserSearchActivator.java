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
package net.java.sip.communicator.plugin.usersearch;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.ResourceManagementService;
import org.osgi.framework.*;

/**
 * Activates the user search plugin which includes the user search contact
 * source.
 *
 * @author Hristo Terezov
 */
public class UserSearchActivator
    implements  BundleActivator
{
    /**
     * List with the available protocol providers that may support user search.
     */
    private static LinkedList<ProtocolProviderService> userSearchProviders;

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * A listener for
     */
    private static UserSearchProviderStateListener userSeachListener = null;

    /**
     * A list with providers that support user search.
     */
    private static LinkedList<ProtocolProviderService> supportedProviders
        = new LinkedList<ProtocolProviderService>();

    /**
     * A list of listeners that will be notified about adding and removing
     * providers that support user search.
     */
    private static LinkedList<UserSearchSupportedProviderListener> listeners
        = new LinkedList<UserSearchSupportedProviderListener>();

    /**
     * The <tt>ServiceRegistration</tt> instance for the contact source.
     */
    private static ServiceRegistration contactSourceRegistration = null;

    /**
     * The <tt>Logger</tt> used by the
     * <tt>UserSearchActivator</tt> class for logging output.
     */
    private static Logger logger = Logger.getLogger(UserSearchActivator.class);

    /**
     * Contact source instance.
     */
    private static UserSearchContactSource userSearchContactSource = null;

    /**
     * The resource service.
     */
    private static ResourceManagementService resources = null;

    /**
     * Initializes a list of all currently providers and a list with the
     * providers that support user search.
     */
    public static void initUserSearchProviders()
    {
        if (userSearchProviders != null)
            return;

        userSearchProviders = new LinkedList<ProtocolProviderService>();

        bundleContext.addServiceListener(new ProtocolProviderRegListener());

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
        return;
    }

    /**
     * Returns the list of providers that support user search.
     * @return the list of providers that support user search.
     */
    public static LinkedList<ProtocolProviderService> getSupportedProviders()
    {
        return supportedProviders;
    }

    /**
     * Adds new <tt>UserSearchSupportedProviderListener</tt> to the list of
     * listeners.
     * @param listener the listener to be added.
     */
    public static void addUserSearchSupportedProviderListener(
        UserSearchSupportedProviderListener listener)
    {
        synchronized (listeners)
        {
            if(!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Removes <tt>UserSearchSupportedProviderListener</tt> from the list of
     * listeners.
     * @param listener the listener to be removed.
     */
    public static void removeUserSearchSupportedProviderListener(
        UserSearchSupportedProviderListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Listens for <tt>ProtocolProviderService</tt> registrations.
     */
    private static class ProtocolProviderRegListener
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
            && !userSearchProviders.contains(protocolProvider))
        {
            OperationSetUserSearch opSet
                = protocolProvider.getOperationSet(OperationSetUserSearch.class);
            if(opSet == null)
                return;
            if(userSeachListener == null)
                userSeachListener = new UserSearchProviderStateListener();
            opSet.addUserSearchProviderListener(userSeachListener);
            if(opSet.isEnabled())
                addSupportedProvider(protocolProvider);
            userSearchProviders.add(protocolProvider);
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
        if (userSearchProviders.contains(protocolProvider))
        {
            userSearchProviders.remove(protocolProvider);
            removeSupportedProvider(protocolProvider);
            if(userSeachListener == null)
                return;
            OperationSetUserSearch opSet
                = protocolProvider.getOperationSet(OperationSetUserSearch.class);
            if(opSet == null)
                return;
            opSet.removeUserSearchProviderListener(userSeachListener);
        }

    }

    /**
     * Adds provider to the list of providers that support user search.
     * @param provider the provider to be added
     */
    private static void addSupportedProvider(ProtocolProviderService provider)
    {
        if(!supportedProviders.contains(provider))
        {
            supportedProviders.add(provider);
            LinkedList<UserSearchSupportedProviderListener> tmpListeners;
            synchronized (listeners)
            {
                tmpListeners
                    = new LinkedList<UserSearchSupportedProviderListener>(
                        listeners);
            }

            for(UserSearchSupportedProviderListener l : tmpListeners)
            {
                l.providerAdded(provider);
            }
            if(supportedProviders.size() == 1)
            {
                if(userSearchContactSource == null)
                    userSearchContactSource = new UserSearchContactSource();
                //register contact source
                contactSourceRegistration = bundleContext.registerService(
                    ContactSourceService.class.getName(),
                    userSearchContactSource ,
                    null);
            }
        }

    }

    /**
     * Removes provider from the list of providers that support user search.
     * @param provider the procider to be removed.
     */
    private static void removeSupportedProvider(
        ProtocolProviderService provider)
    {
        if(supportedProviders.contains(provider))
        {
            supportedProviders.remove(provider);
            for(UserSearchSupportedProviderListener l : listeners)
            {
                l.providerRemoved(provider);
            }

            if(supportedProviders.isEmpty()
                && contactSourceRegistration != null)
            {
                contactSourceRegistration.unregister();
                contactSourceRegistration = null;
            }
        }
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        initUserSearchProviders();

    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        userSeachListener = null;
        userSearchProviders.clear();
        supportedProviders.clear();
        listeners.clear();
        contactSourceRegistration = null;
        userSearchContactSource = null;
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
     * Listens for added or removed providers that support user search.
     */
    private static class UserSearchProviderStateListener
        implements UserSearchProviderListener
    {
        @Override
        public void onUserSearchProviderEvent(UserSearchProviderEvent event)
        {
            if(event.getType() == UserSearchProviderEvent.PROVIDER_ADDED)
            {
                addSupportedProvider(event.getProvider());
            }
            else if(event.getType() == UserSearchProviderEvent.PROVIDER_REMOVED)
            {
                removeSupportedProvider(event.getProvider());
            }
        }
    }

}
