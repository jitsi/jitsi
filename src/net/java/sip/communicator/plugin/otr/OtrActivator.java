/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * @author George Politis
 * @author Pawel Domas
 */
public class OtrActivator
    extends AbstractServiceDependentActivator
    implements ServiceListener
{
    /**
     * A property used in configuration to disable the OTR plugin.
     */
    public static final String OTR_DISABLED_PROP =
        "net.java.sip.communicator.plugin.otr.DISABLED";

    /**
     * Indicates if the security/chat config form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String OTR_CHAT_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.plugin.otr.otrchatconfig.DISABLED";

    /**
     * A property specifying whether private messaging should be made mandatory.
     */
    public static final String OTR_MANDATORY_PROP =
        "net.java.sip.communicator.plugin.otr.PRIVATE_MESSAGING_MANDATORY";

    /**
     * A property specifying whether private messaging should be automatically
     * initiated.
     */
    public static final String AUTO_INIT_OTR_PROP =
        "net.java.sip.communicator.plugin.otr.AUTO_INIT_PRIVATE_MESSAGING";

    /**
     * The {@link BundleContext} of the {@link OtrActivator}.
     */
    public static BundleContext bundleContext;

    private OtrTransformLayer otrTransformLayer;

    /**
     * The {@link ScOtrEngine} of the {@link OtrActivator}.
     */
    public static ScOtrEngine scOtrEngine;

    /**
     * The {@link ScOtrKeyManager} of the {@link OtrActivator}.
     */
    public static ScOtrKeyManager scOtrKeyManager = new ScOtrKeyManagerImpl();

    /**
     * The {@link ResourceManagementService} of the {@link OtrActivator}. Can
     * also be obtained from the {@link OtrActivator#bundleContext} on demand,
     * but we add it here for convenience.
     */
    public static ResourceManagementService resourceService;

    /**
     * The {@link UIService} of the {@link OtrActivator}. Can also be obtained
     * from the {@link OtrActivator#bundleContext} on demand, but we add it here
     * for convenience.
     */
    public static UIService uiService;

    /**
     * The {@link ConfigurationService} of the {@link OtrActivator}. Can also be
     * obtained from the {@link OtrActivator#bundleContext} on demand, but we
     * add it here for convenience.
     */
    public static ConfigurationService configService;

    /**
     * The <tt>Logger</tt> used by the <tt>OtrActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(OtrActivator.class);

    /*
     * Implements AbstractServiceDependentActivator#start(UIService).
     */
    @Override
    public void start(Object dependentService)
    {
        configService
            = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        // Check whether someone has disabled this plug-in.
        if(configService.getBoolean(OTR_DISABLED_PROP, false))
        {
            configService = null;
            return;
        }

        resourceService
            = ResourceManagementServiceUtils.getService(bundleContext);
        if (resourceService == null)
        {
            configService = null;
            return;
        }

        uiService = (UIService) dependentService;

        // Init static variables, don't proceed without them.
        scOtrEngine = new ScOtrEngineImpl();
        otrTransformLayer = new OtrTransformLayer();

        // Register Transformation Layer
        bundleContext.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = ServiceUtils
                .getServiceReferences(
                        bundleContext,
                        ProtocolProviderService.class);

        if (protocolProviderRefs != null && protocolProviderRefs.length > 0)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + protocolProviderRefs.length
                + " already installed providers.");
            for (ServiceReference protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRef);

                this.handleProviderAdded(provider);
            }
        }

        if(!OSUtils.IS_ANDROID)
        {
            Hashtable<String, String> containerFilter = new Hashtable<String, String>();

            // Register the right-click menu item.
            containerFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());


            bundleContext
                .registerService(PluginComponent.class.getName(),
                    new OtrMetaContactMenu(
                            Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU),
                            containerFilter);

            // Register the chat window menu bar item.
            containerFilter.put(Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_MENU_BAR.getID());

            bundleContext.registerService(PluginComponent.class.getName(),
                new OtrMetaContactMenu(Container.CONTAINER_CHAT_MENU_BAR),
                containerFilter);

            // Register the chat button bar default-action-button.
            containerFilter.put(Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_TOOL_BAR.getID());

            bundleContext.registerService(
                PluginComponent.class.getName(),
                new OtrMetaContactButton(Container.CONTAINER_CHAT_TOOL_BAR),
                containerFilter);

            // Register Swing OTR action handler
            bundleContext.registerService(
                OtrActionHandler.class.getName(),
                new SwingOtrActionHandler(), null);
        }



        // If the general configuration form is disabled don't register it.
        if (!configService.getBoolean(OTR_CHAT_CONFIG_DISABLED_PROP, false)
                && !OSUtils.IS_ANDROID)
        {
            Dictionary<String, String> properties
                = new Hashtable<String, String>();
            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.SECURITY_TYPE);
            // Register the configuration form.
            bundleContext.registerService(ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.otr.OtrConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.otr.configform.ICON",
                    "service.gui.CHAT", 1),
                    properties);
        }
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetInstantMessageTransform opSetMessageTransform
            = provider
                .getOperationSet(OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
        {
            opSetMessageTransform.addTransformLayer(this.otrTransformLayer);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a transform op. set.");
        }
    }

    /*
     * Implements BundleActivator#stop(BundleContext).
     */
    public void stop(BundleContext bc) throws Exception
    {
        // Unregister transformation layer.
        // start listening for newly register or removed protocol providers
        bundleContext.removeServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        if (protocolProviderRefs != null && protocolProviderRefs.length > 0)
        {
            // in case we found any
            for (ServiceReference protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRef);

                this.handleProviderRemoved(provider);
            }
        }
    }

    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        // check whether the provider has a basic im operation set
        OperationSetInstantMessageTransform opSetMessageTransform
            = provider
                .getOperationSet(OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
        {
            opSetMessageTransform.removeTransformLayer(this.otrTransformLayer);
        }
    }

    /*
     * Implements ServiceListener#serviceChanged(ServiceEvent).
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService =
            bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: "
            + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
                logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService) sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved((ProtocolProviderService) sService);
        }

    }

    /**
     * Gets all the available accounts in SIP Communicator.
     *
     * @return a {@link List} of {@link AccountID}.
     */
    public static List<AccountID> getAllAccountIDs()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            OtrActivator.getProtocolProviderFactories();

        if (providerFactoriesMap == null)
            return null;

        List<AccountID> accountIDs = new Vector<AccountID>();
        for (ProtocolProviderFactory providerFactory : providerFactoriesMap
            .values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                accountIDs.add(accountID);
            }
        }

        return accountIDs;
    }

    /**
     * Gets an {@link AccountID} by its UID.
     *
     * @param uid The {@link AccountID} UID.
     * @return The {@link AccountID} with the requested UID or null.
     */
    public static AccountID getAccountIDByUID(String uid)
    {
        if (uid == null || uid.length() < 1)
            return null;

        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            OtrActivator.getProtocolProviderFactories();

        if (providerFactoriesMap == null)
            return null;

        for (ProtocolProviderFactory providerFactory : providerFactoriesMap
            .values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                if (accountID.getAccountUniqueID().equals(uid))
                    return accountID;
            }
        }

        return null;
    }

    private static Map<Object, ProtocolProviderFactory> getProtocolProviderFactories()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Error while retrieving service refs", ex);
            return null;
        }

        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            new Hashtable<Object, ProtocolProviderFactory>();
        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory =
                    (ProtocolProviderFactory) bundleContext.getService(serRef);

                providerFactoriesMap.put(serRef
                    .getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
