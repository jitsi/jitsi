package net.java.sip.communicator.plugin.otr;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class OtrActivator
    implements BundleActivator, ServiceListener
{

    private BundleContext bundleContext = null;

    private OtrTransformLayer transformLayer = new OtrTransformLayer();

    private static Logger logger = Logger.getLogger(OtrActivator.class);

    @Override
    public void start(BundleContext bc) throws Exception
    {
        this.bundleContext = bc;
        bc.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                bc.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        if (protocolProviderRefs != null)
        {
            logger.debug("Found " + protocolProviderRefs.length
                + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) bc
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }

        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();
        containerFilter.put(Container.CONTAINER_ID,
            Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(PluginComponent.class.getName(),
            new OtrMenu(ResourceManagementServiceUtils
                .getService(bc)), containerFilter);
    }

    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetInstantMessageTransform opSetMessageTransform =
            (OperationSetInstantMessageTransform) provider
                .getOperationSet(OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
        {
            opSetMessageTransform.addTransformLayer(transformLayer);
        }
        else
        {
            logger.trace("Service did not have a transform op. set.");
        }

    }

    @Override
    public void stop(BundleContext bc) throws Exception
    {
        // start listening for newly register or removed protocol providers
        bc.removeServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                bc.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) bc
                        .getService(protocolProviderRefs[i]);

                this.handleProviderRemoved(provider);
            }
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
            opSetMessageTransform.removeTransformLayer(transformLayer);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService =
            bundleContext.getService(serviceEvent.getServiceReference());

        logger.trace("Received a service event for: "
            + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
        {
            return;
        }

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

}
