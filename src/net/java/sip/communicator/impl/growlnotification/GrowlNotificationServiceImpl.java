/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import java.util.*;

import org.osgi.framework.*;
import com.growl.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import java.lang.reflect.*;


/**
 * The Growl Notification Service displays on-screen information such as
 * messages or call received, etc.
 *
 * @author Romain Kuntz
 */
public class GrowlNotificationServiceImpl
    implements  MessageListener,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(GrowlNotificationServiceImpl.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The Growl notifier
     */
    private Growl notifier;

    /**
     * The notifyGrowlOf/setAllowedNotifications/setDefaultNotifications 
     * methods of the growl class. We use reflection to access them
     * in order to avoid compilation errors on non mac platforms.
     */
    private Method notifyMethod = null;
    private Method setAllowedNotifMethod = null;
    private Method setDefaultNotifMethod = null;

    /* All Growl Notifications and the default ones */
    private String [] allNotif =
        new String[] { "SIP Communicator Started",
                       "Protocol events",
                       "Message Received",
                       "Message Sent"};

    private String [] defaultNotif =
        new String[] { "SIP Communicator Started",
                       "Message Received" };

    /** 
     * The path to the SIP Communicator icon used in Growl's configuration 
     * menu and protocol events messages
     */
    private String sipIconPath = "resources/images/logo/sc_logo_128x128.icns";

    /**
     * starts the service. Creates a Growl notifier, and check the current
     * registerd protocol providers which supports BasicIM and adds message
     * listener to them.
     *
     * @param bc a currently valid bundle context
     * @throws java.lang.Exception if we fail initializing the growl notifier.
     */
    public void start(BundleContext bc)
        throws Exception
    {
        logger.debug("Starting the Growl Notification implementation.");
        this.bundleContext = bc;

        /* Register to Growl */
        try
        {
            Constructor constructor = Growl.class.getConstructor(new Class[]
                                {String.class, String.class});
            notifier = (Growl)constructor.newInstance(
                    new Object[]{"SIP Communicator", sipIconPath});

            //init the setAllowedNotifications method
            setAllowedNotifMethod = Growl.class.getMethod(
                    "setAllowedNotifications"
                    , new Class[]{String[].class});

            //init the setDefaultNotifications method
            setDefaultNotifMethod = Growl.class.getMethod(
                    "setDefaultNotifications"
                    , new Class[]{String[].class});

            //init the notifyGrowlOf method
            notifyMethod = Growl.class.getMethod(
                    "notifyGrowlOf"
                    , new Class[]{String.class, String.class, 
                                  String.class, String.class});

            setAllowedNotifications(allNotif);
            setDefaultNotifications(defaultNotif);
            notifier.register();

            notifyGrowlOf("SIP Communicator Started"
                          , sipIconPath
                          , "Welcome to SIP Communicator"
                          , "http://www.sip-communicator.org");
        }
        catch (Exception ex)
        {
            logger.error("Could not send the message to Growl", ex);
            throw ex;
        }

        /* Start listening for newly register or removed protocol providers */
        bc.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
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
            logger.debug("Found "
                            + protocolProviderRefs.length
                            + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        // start listening for newly register or removed protocol providers
        bc.removeServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderRemoved(provider);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // MessageListener implementation methods

    /**
     * Passes the newly received message to growl.
     * @param evt MessageReceivedEvent the vent containing the new message.
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        //byte[] contactImage = null;
        //try
        //{
        //    contactImage = evt.getSourceContact().getImage();
        //}
        //catch (Exception ex)
        //{
        //    logger.error("Failed to load contact photo for Growl", ex);
        //}

        try
        {
            notifyGrowlOf("Message Received"
                          , sipIconPath
                          , evt.getSourceContact().getDisplayName()
                          , evt.getSourceMessage().getContent());
        }
        catch (Exception ex)
        {
            logger.error("Could not notify the received message to Growl", ex);
        }
    }

    /**
     * Notify growl that a message has been sent.
     * @param evt the event containing the message that has just been sent.
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        try
        {
            notifyGrowlOf("Message Sent"
                          , sipIconPath
                          , "Me"
                          , evt.getSourceMessage().getContent());
        }
        catch (Exception ex)
        {
            logger.error("Could not pass the sent message to Growl", ex);
        }
    }

    /**
     * Currently unused
     * @param evt ignored
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }
    // //////////////////////////////////////////////////////////////////////////

    /**
     * When new protocol provider is registered we check
     * does it supports BasicIM and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService
            = bundleContext.getService(serviceEvent.getServiceReference());

        logger.trace("Received a service event for: "
                     + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }

    }

    /**
     * Used to attach the Growl Notification Service to existing or
     * just registered protocol provider. Checks if the provider has
     * implementation of OperationSetBasicInstantMessaging
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm
            = (OperationSetBasicInstantMessaging) provider
            .getSupportedOperationSets().get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
            try
            {
                notifyGrowlOf("Protocol events"
                              , sipIconPath
                              , "New Protocol Registered"
                              , provider.getProtocolName() + " registered");
            }
            catch (Exception ex)
            {
                logger.error("Could not notify the message to Growl", ex);
            }
        }
        else
        {
            logger.trace("Service did not have a im op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the messages exchanged by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
            = (OperationSetBasicInstantMessaging) provider
            .getSupportedOperationSets().get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetIm != null)
        {
            opSetIm.removeMessageListener(this);
            try
            {
                notifyGrowlOf("Protocol events"
                              , sipIconPath
                              , "Protocol deregistered"
                              , provider.getProtocolName()
                              + " deregistered");
            }
            catch (Exception ex)
            {
                logger.error("Could not notify the message to Growl", ex);
            }
        }
    }

    /**
     * Convenience method that defers to notifier.notifyGrowlOf() using
     * reflection without referencing it directly. The purpose of this method
     * is to allow the class to compile on non-mac systems.
     *
     * @param inNotificationName The name of one of the notifications we told
     * growl about.
     * @param inTitle The Title of our Notification as Growl will show it
     * @param inDescription The Description of our Notification as Growl will
     * display it
     *
     * @throws Exception When a notification is not known
     */
    public void notifyGrowlOf(String inNotificationName,
                              String inImagePath,
                              String inTitle,
                              String inDescription)
        throws Exception
    {
        notifyMethod.invoke(
            notifier, new Object[]{inNotificationName, inImagePath, 
                                   inTitle, inDescription});
    }
    
    /**
     * Convenience method that defers to notifier.setAllowedNotifications() 
     * using reflection without referencing it directly. The purpose of this 
     * method is to allow the class to compile on non-mac systems.
     *
     * @param inAllNotes The list of allowed Notifications
     */
    public void setAllowedNotifications(String [] inAllNotes)
        throws Exception
    {
        setAllowedNotifMethod.invoke(notifier, new Object[]{inAllNotes});
    }

    /**
     * Convenience method that defers to notifier.setDefaultNotifications() 
     * using reflection without referencing it directly. The purpose of this 
     * method is to allow the class to compile on non-mac systems.
     *
     * @param inDefNotes The list of default Notifications
     */
    public void setDefaultNotifications(String [] inDefNotes)
        throws Exception
    {
        setDefaultNotifMethod.invoke(notifier, new Object[]{inDefNotes});
    }

}
