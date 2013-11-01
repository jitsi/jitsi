/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.ConfigurationService;
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
     * The <tt>Logger</tt> used by the
     * <tt>MUCActivator</tt> class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MUCActivator.class);

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
     * The contact source.
     */
    private static final ChatRoomContactSourceService chatRoomContactSource
        = new ChatRoomContactSourceService();

    /**
     * The resource service.
     */
    private static ResourceManagementService resources = null;

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
            chatRoomContactSource,
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
     * Returns a list of all currently registered providers.
     *
     * @return a list of all currently registered providers
     */
    public static List<ProtocolProviderService> getChatRoomProviders()
    {
        if (chatRoomProviders != null)
            return chatRoomProviders;

        chatRoomProviders = new LinkedList<ProtocolProviderService>();

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
                OperationSetMultiUserChat.class) != null
            && protocolProvider.isRegistered()
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
}
