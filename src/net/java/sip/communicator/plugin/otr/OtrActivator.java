/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author George Politis
 */
public class OtrActivator
    implements BundleActivator, ServiceListener
{
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
     * but we add it here for convinience.
     */
    public static ResourceManagementService resourceService;

    /**
     * The {@link UIService} of the {@link OtrActivator}. Can also be obtained
     * from the {@link OtrActivator#bundleContext} on demand, but we add it here
     * for convinience.
     */
    public static UIService uiService;

    /**
     * The {@link ConfigurationService} of the {@link OtrActivator}. Can also be
     * obtained from the {@link OtrActivator#bundleContext} on demand, but we
     * add it here for convinience.
     */
    public static ConfigurationService configService;

    private static Logger logger = Logger.getLogger(OtrActivator.class);

    /*
     * Implements BundleActivator#start(BundleContext).
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        // Init static variables, don't proceed without them.
        scOtrEngine = new ScOtrEngineImpl();
        otrTransformLayer = new OtrTransformLayer();

        resourceService =
            ResourceManagementServiceUtils
                .getService(OtrActivator.bundleContext);
        if (resourceService == null)
            return;

        ServiceReference refConfigService =
            OtrActivator.bundleContext
                .getServiceReference(ConfigurationService.class.getName());

        if (refConfigService == null)
            return;

        configService =
            (ConfigurationService) OtrActivator.bundleContext
                .getService(refConfigService);

        ServiceReference refUIService =
            OtrActivator.bundleContext.getServiceReference(UIService.class
                .getName());

        if (refUIService == null)
            return;

        uiService =
            (UIService) OtrActivator.bundleContext.getService(refUIService);

        // Register Transformation Layer
        bundleContext.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        if (protocolProviderRefs != null && protocolProviderRefs.length > 0)
        {
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

        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();

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

        bundleContext.registerService(PluginComponent.class.getName(),
            new OtrMetaContactButton(Container.CONTAINER_CHAT_TOOL_BAR),
            containerFilter);

        // Register the configuration form.
        bundleContext.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.otr.OtrConfigurationPanel",
                getClass().getClassLoader(), "plugin.otr.configform.ICON",
                "plugin.otr.configform.TITLE", 30), null);
    }

    private ServiceRegistration regRightClickMenu;

    private ServiceRegistration regMenuBarMenu;

    private ServiceRegistration regButtonBarButton;

    private ServiceRegistration regConfigurationForm;

    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetInstantMessageTransform opSetMessageTransform =
            (OperationSetInstantMessageTransform) provider
                .getOperationSet(OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
        {
            opSetMessageTransform.addTransformLayer(this.otrTransformLayer);
        }
        else
        {
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

        // Unregister UI
        if (this.regButtonBarButton != null)
            this.regButtonBarButton.unregister();

        if (this.regConfigurationForm != null)
            this.regConfigurationForm.unregister();

        if (this.regMenuBarMenu != null)
            this.regMenuBarMenu.unregister();

        if (this.regRightClickMenu != null)
            this.regRightClickMenu.unregister();
    }

    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        // check whether the provider has a basic im operation set
        OperationSetInstantMessageTransform opSetMessageTransform =
            (OperationSetInstantMessageTransform) provider
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

        logger.trace("Received a service event for: "
            + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
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
