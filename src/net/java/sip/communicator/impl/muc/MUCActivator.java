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
package net.java.sip.communicator.impl.muc;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The activator for the chat room contact source bundle.
 *
 * @author Hristo Terezov
 */
public class MUCActivator
    implements  BundleActivator
{
    /**
     * The configuration property to disable
     */
    private static final String DISABLED_PROPERTY
        = "net.java.sip.communicator.impl.muc.MUCSERVICE_DISABLED";

    /**
     * The bundle context.
     */
    static BundleContext bundleContext = null;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;
    
    /**
     * Providers of contact info.
     */
    private static List<ProtocolProviderService> chatRoomProviders;

    /**
     * The chat room contact source.
     */
    private static final ChatRoomContactSourceService chatRoomContactSource
        = new ChatRoomContactSourceService();

    /**
     * The resource service.
     */
    private static ResourceManagementService resources = null;
    
    /**
     * The MUC service.
     */
    private static MUCServiceImpl mucService;

    /**
     * The account manager.
     */
    private static AccountManager accountManager;

    /**
     * The alert UI service.
     */
    private static AlertUIService alertUIService;

    /**
     * The credential storage service.
     */
    private static CredentialsStorageService credentialsService;
    
    /**
     * The custom actions service.
     */
    private static MUCCustomContactActionService mucCustomActionService;
    
    /**
     * The UI service.
     */
    private static UIService uiService = null;

    /**
     * Listens for ProtocolProviderService registrations.
     */
    private static ProtocolProviderRegListener protocolProviderRegListener 
        = null;
    
    /**
     * The message history service.
     */
    private static MessageHistoryService messageHistoryService;

    /**
     * The global display details service instance.
     */
    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    /**
     * Starts this bundle.
     *
     * @param context the bundle context where we register and obtain services.
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        if(getConfigurationService().getBoolean(DISABLED_PROPERTY, false))
            return;

        bundleContext.registerService(
            ContactSourceService.class.getName(),
            chatRoomContactSource,
            null);
        mucService = new MUCServiceImpl();
        bundleContext.registerService(
            MUCService.class.getName(),
            mucService,
            null);
        mucCustomActionService = new MUCCustomContactActionService();
        bundleContext.registerService(
            CustomContactActionsService.class.getName(),
            mucCustomActionService,
            null);
        bundleContext.registerService(
            CustomContactActionsService.class.getName(),
            new MUCGroupCustomContactActionService(),
            null);
    }

    public void stop(BundleContext context) throws Exception
    {
        if(protocolProviderRegListener != null)
        {
            bundleContext.removeServiceListener(protocolProviderRegListener);
        }

        if(chatRoomProviders != null)
            chatRoomProviders.clear();
    }

    /**
     * Returns the MUC service instance.
     * @return the MUC service instance.
     */
    public static MUCServiceImpl getMUCService()
    {
        return mucService;
    }
    
    /**
     * Returns the chat room contact source.
     * 
     * @return the chat room contact source
     */
    public static ChatRoomContactSourceService getContactSource()
    {
        return chatRoomContactSource;
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
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }
    
    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }
    
    /**
     * Returns the <tt>AlertUIService</tt> obtained from the bundle
     * context.
     * @return the <tt>AlertUIService</tt> obtained from the bundle
     * context
     */
    public static AlertUIService getAlertUIService()
    {
        if(alertUIService == null)
        {
            alertUIService
                = ServiceUtils.getService(
                        bundleContext,
                        AlertUIService.class);
        }
        return alertUIService;
    }
    
    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        if (credentialsService == null)
        {
            credentialsService
                = ServiceUtils.getService(
                    bundleContext, CredentialsStorageService.class);
        }
        return credentialsService;
    }
    
    /**
     * Returns a list of all currently registered providers.
     *
     * @return a list of all currently registered providers
     */
    public static List<ProtocolProviderService> getChatRoomProviders()
    {
        if (chatRoomProviders != null)
            return chatRoomProviders;
        
        chatRoomProviders = new LinkedList<ProtocolProviderService>();
        
        protocolProviderRegListener = new ProtocolProviderRegListener();
        bundleContext.addServiceListener(protocolProviderRegListener);

        Collection<ServiceReference<ProtocolProviderFactory>> serRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderFactory.class);

        if (!serRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderFactory> ppfSerRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = bundleContext.getService(ppfSerRef);

                for (AccountID accountID
                        : providerFactory.getRegisteredAccounts())
                {
                    ServiceReference<ProtocolProviderService> ppsSerRef
                        = providerFactory.getProviderForAccount(accountID);
                    ProtocolProviderService protocolProvider
                        = bundleContext.getService(ppsSerRef);

                    handleProviderAdded(protocolProvider);
                }
            }
        }
        return chatRoomProviders;
    }

    /**
     * Listens for <tt>ProtocolProviderService</tt> registrations.
     */
    private static class ProtocolProviderRegListener
        implements ServiceListener
    {
        /**
         * Handles service change events.
         */
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference<?> serviceRef = event.getServiceReference();

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
                OperationSetMultiUserChat.class) != null
            && !chatRoomProviders.contains(protocolProvider))
        {
            chatRoomProviders.add(protocolProvider);
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
        if (chatRoomProviders.contains(protocolProvider))
            chatRoomProviders.remove(protocolProvider);
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * @return the <tt>UIService</tt> obtained from the bundle
     * context
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            uiService
                = ServiceUtils.getService(
                        bundleContext,
                        UIService.class);
        }
        return uiService;
    }

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if(globalDisplayDetailsService == null)
        {
            globalDisplayDetailsService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalDisplayDetailsService.class);
        }
        return globalDisplayDetailsService;
    }
    
    /**
     * Gets the service giving access to message history.
     *
     * @return the service giving access to message history.
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if (messageHistoryService == null)
            messageHistoryService = ServiceUtils.getService(bundleContext, 
                MessageHistoryService.class);
        return messageHistoryService;
    }
}
