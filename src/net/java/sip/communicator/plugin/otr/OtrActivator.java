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
 * 
 * @author George Politis
 * 
 */
public class OtrActivator
    implements BundleActivator, ServiceListener
{

    public static BundleContext bundleContext;

    private OtrTransformLayer otrTransformLayer;

    public static ScOtrEngine scOtrEngine;

    public static ResourceManagementService resourceService;

    public static UIService uiService;

    public static ConfigurationService configService;

    private static Logger logger = Logger.getLogger(OtrActivator.class);

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        if (!initServices())
            return;

        if (!registerTransformLayer())
            return;

        registerUI();
    }

    private boolean registerTransformLayer()
    {
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
            return false;
        }

        if (protocolProviderRefs == null || protocolProviderRefs.length < 1)
            return false;

        logger.debug("Found " + protocolProviderRefs.length
            + " already installed providers.");
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider =
                (ProtocolProviderService) bundleContext
                    .getService(protocolProviderRefs[i]);

            this.handleProviderAdded(provider);
        }

        return true;
    }

    private void registerUI()
    {
        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();

        OtrMetaContactMenu rightClickMenu =
            new OtrMetaContactMenu(
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU,
                OtrActivator.resourceService
                .getImage("plugin.otr.MENU_ITEM_ICON_16x16"));
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(PluginComponent.class.getName(),
            rightClickMenu, containerFilter);

        OtrMetaContactMenu chatMenuBarMenu =
                new OtrMetaContactMenu(Container.CONTAINER_CHAT_MENU_BAR,
                null);
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CHAT_MENU_BAR.getID());

        bundleContext.registerService(PluginComponent.class.getName(),
            chatMenuBarMenu, containerFilter);

        OtrMetaContactButton btn =
            new OtrMetaContactButton(Container.CONTAINER_CHAT_TOOL_BAR);
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CHAT_TOOL_BAR.getID());

        bundleContext.registerService(PluginComponent.class.getName(), btn,
            containerFilter);

        bundleContext.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.otr.OtrConfigurationPanel",
                getClass().getClassLoader(),
                "plugin.otr.configform.ICON",
                "plugin.otr.configform.TITLE", 30), null);
    }

    private boolean initServices()
    {
        scOtrEngine = new ScOtrEngineImpl();
        otrTransformLayer = new OtrTransformLayer();

        ServiceReference ref =
            OtrActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

        if (ref == null)
            return false;

        resourceService =
            (ResourceManagementService) OtrActivator.bundleContext
                .getService(ref);

        ServiceReference refConfigService =
            OtrActivator.bundleContext
                .getServiceReference(ConfigurationService.class.getName());

        if (refConfigService == null)
            return false;

        configService =
            (ConfigurationService) OtrActivator.bundleContext
                .getService(refConfigService);

        ServiceReference refUIService =
            OtrActivator.bundleContext.getServiceReference(UIService.class
                .getName());

        if (refUIService == null)
            return false;

        uiService =
            (UIService) OtrActivator.bundleContext.getService(refUIService);

        return true;
    }

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

    public void stop(BundleContext bc) throws Exception
    {
        unregisterTransformLayer();
        unregisterUI();
    }

    private void unregisterUI()
    {
        // TODO Auto-generated method stub
    }

    private void unregisterTransformLayer()
    {
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

        if (protocolProviderRefs == null || protocolProviderRefs.length < 1)
            return;

        // in case we found any
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider =
                (ProtocolProviderService) bundleContext
                    .getService(protocolProviderRefs[i]);

            this.handleProviderRemoved(provider);
        }
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

    private static final Map<Object, ProtocolProviderFactory> providerFactoriesMap =
        new Hashtable<Object, ProtocolProviderFactory>();

    public static Map<Object, ProtocolProviderFactory> getProtocolProviderFactories()
    {

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {

                ProtocolProviderFactory providerFactory =
                    (ProtocolProviderFactory) bundleContext
                        .getService(serRefs[i]);

                providerFactoriesMap.put(serRefs[i]
                    .getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
