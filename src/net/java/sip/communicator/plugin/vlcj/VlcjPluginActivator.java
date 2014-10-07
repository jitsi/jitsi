package net.java.sip.communicator.plugin.vlcj;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import org.osgi.framework.*;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.logger.Logger;

import javax.swing.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by frank on 10/1/14.
 * TODO: the VlcjMediaPlayerController should proably go in the service and the VlcjPlayer should probably be in the impl
 */
public class VlcjPluginActivator implements BundleActivator, MessageListener, ServiceListener {
    /**
     * The logger for this class.
     */
    private static net.java.sip.communicator.util.Logger logger = net.java.sip.communicator.util.Logger.getLogger(VlcjPluginActivator.class);
    private final VlcjMediaPlayerController mediaPlayerController;
    private volatile  BundleContext bundleContext = null;

    public VlcjPluginActivator() {
        mediaPlayerController = new VlcjMediaPlayerController();
    }

    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        LibVlc libVlc = LibVlcFactory.factory().create();

        Logger.info("  version: {}", libVlc.libvlc_get_version());
        Logger.info(" compiler: {}", libVlc.libvlc_get_compiler());
        Logger.info("changeset: {}", libVlc.libvlc_get_changeset());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new VlcjPlayer(mediaPlayerController);
            }
        });
        java.util.Timer timer = new Timer();
        initalizeProviders();
    }


    private void schedulePause(final VlcjMediaPlayerController mediaPlayerController, Timer timer) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    mediaPlayerController.pause();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, 0, 10000);
    }

    private void initalizeProviders() {
        // start listening for newly register or removed protocol providers
        logger.info("start initializeProviders");
        bundleContext.addServiceListener(this);
        logger.info("added service listener");
        ServiceReference[] protocolProviderRefs;
        try {
            protocolProviderRefs = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    null);
        } catch (InvalidSyntaxException ex) {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                    "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null) {
            if (logger.isDebugEnabled())
                logger.debug("Found "
                        + protocolProviderRefs.length
                        + " already installed providers.");
            for (ServiceReference protocolProviderRef : protocolProviderRefs) {
                ProtocolProviderService provider
                        = (ProtocolProviderService)
                        bundleContext.getService(protocolProviderRef);

                this.handleProviderAdded(provider);
            }
        }
    }

    private void handleProviderAdded(ProtocolProviderService provider) {
        logger.info("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm
                = provider
                .getOperationSet(OperationSetBasicInstantMessaging.class);
        logger.info("past getOperationSet");
        if (opSetIm != null) {
            logger.info("opSetIm is not null, addingMessageListener");
            opSetIm.addMessageListener(this);
            logger.info("added MessageListener");
        } else {
                logger.info("Service did not have a im op. set.");
        }
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }

    public void messageReceived(MessageReceivedEvent evt) {
        Message message = evt.getSourceMessage();
        logger.info("VlcjPluginActivator: message.getContent(): " + message.getContent());
        if (message.getContent().equals("pause")) {
            mediaPlayerController.pause();
        }
    }

    public void messageDelivered(MessageDeliveredEvent evt) {

    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {

    }

    public void serviceChanged(ServiceEvent serviceEvent) {
        Object sService
                = bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: " +
                    sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        switch (serviceEvent.getType())
        {
            case ServiceEvent.REGISTERED:
                this.handleProviderAdded((ProtocolProviderService)sService);
                break;

        }
    }
}
