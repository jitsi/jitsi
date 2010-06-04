/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.mailbox;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * This class is meant to serve as an Audio/Video Mailbox. It's basic functions
 * are to listen for incoming calls, answer incoming calls that do not get
 * picked up, play an outgoing message from a file, and record incoming audio
 * to a file.
 *
 * @author Ryan Ricard
 */
public class Mailbox
    implements CallListener,
               ServiceListener
{
    private static Logger logger = Logger.getLogger(Mailbox.class.getName());

    /**
     * A reference to the currently valid bundle context instance.
     */
    private BundleContext bundleContext = null;

    /**
     * The name of the property that we use to determine how long we wait before
     * answering a call.
     */
    public static final String WAIT_TIME_PROPERTY_NAME =
            "net.java.sip.communicator.pugin.mailbox.WAIT_TIME";

    /**
     * The name of the property that contains the name of the file where we
     * store our video voice-mail message.
     */
    public static final String OUTGOING_MESSAGE_PROPERTY_NAME
        = "net.java.sip.communicator.plugin.mailbox.OUTGOING_MESSAGE";

    /**
     * The name of the file (relative path included) containing a default
     * audio/video message that we will be playing if the user hasn't recorded
     * one of its own.
     */
    public static final String DEFAULT_OUTGOING_MESSAGE_FILE
        = "resources/sounds/default_outgoing_message.wav";

    /**
     * The name of the property that contains the name of the file where we
     * store incoming voice-mail messages.
     */
    public static final String INCOMING_MESSAGE_PROPERTY_NAME
        = "net.java.sip.communicator.plugin.mailbox.INCOMING_MESSAGE";

    /**
     * The name of the property that contains the maximum interval that we will
     * be recording incoming messages for.
     */
    public static final String MAX_MSG_DURATION_PROPERTY_NAME
        = "net.java.sip.communicator.plugin.mailbox.MAX_MSG_DURATION";

    /**
     * The default name of the directory where we store incoming audio/video
     * messages.
     */
    public static final String DEFAULT_INCOMING_MSG_DIR_NAME
        = "mailbox";

    /**
     * Constructs a new Mailbox
     */
    public Mailbox()
    {
    }

    /**
     * Returns the length of time to wait until picking up an unanswered
     * incoming call. If no length of time is defined in the configuration file,
     * the mailbox will wait 30 seconds before answering an incoming call.
     *
     * @return an integer value for the length of time, in milliseconds,
     * that the mailbox will wait before picking up an incoming call
     */
    public static int getWaitTime()
    {
        int timeDelay = 30000;

        //if the configuration object is null or won't parse into an int we
        //use the default
        try
        {
            String waitTimeStr = MailboxActivator.getConfigurationService()
                .getString(Mailbox.WAIT_TIME_PROPERTY_NAME);

            if(waitTimeStr != null)
                timeDelay = Integer.parseInt(waitTimeStr);
        }
        catch(NumberFormatException e)
        {
            if (logger.isInfoEnabled())
                logger.info("failed to load the wait time out of the config file"+
                        ", using default value of 30 seconds",e);
            timeDelay = 30000;
        }

        return timeDelay;
    }

    /**
     * Returns the maximum length of time allowed for incoming messages. If no
     * length of time is defined in the configuration file, the mailbox will
     * allow callers to leave a message up to 60 seconds.
     *
     * @return an integer value for the length of time, in milliseconds,
     * that the mailbox will wait before hanging up on an incoming message.
     */
    public static int getMaxMessageDuration()
    {
        int duration = 60000;
        //if the configuration object is null or won't parse into an int
        //we use the default
        try
        {
            String durationStr = MailboxActivator.getConfigurationService()
                .getString(Mailbox.MAX_MSG_DURATION_PROPERTY_NAME);

            if(durationStr != null)
                duration = Integer.parseInt(durationStr);
        }
        catch (NumberFormatException e)
        {
            if (logger.isInfoEnabled())
                logger.info("failed to load the max msg time out of the config"+
                        "file, using default value of 60 seconds",e);
            duration = 60000;
        }

        return duration;
    }

    /**
     * Returns the location of the file that we are using for our outgoing
     * message. If no file is defined in the configuration file or if the file
     * defined in the configuration file cannot be read,the default outgoing
     * message will be used.
     *
     * @return a string describing the location of the outgoing message file.
     */
    public static File getOutgoingMessageFileLocation()
    {
        //if the configuration object is null or the file doesn't exist
        //we use the default
        String fileLocationStr = MailboxActivator.getConfigurationService()
            .getString(Mailbox.OUTGOING_MESSAGE_PROPERTY_NAME);
        if (fileLocationStr != null)
        {
            File fileLocation = new File(fileLocationStr);
            if(fileLocation.canRead())
            {
                return fileLocation;
            }
        }
        return new File(DEFAULT_OUTGOING_MESSAGE_FILE);
    }

    /**
     * Returns the location of the directory that we are storing incoming
     * messages in. If no location is defined in the configuration file or if
     * we cannot create new files in that location, the default location is the
     * SIP Communicator Home Directory.
     *
     * @return the location of the directory where incoming messages should be
     * stored.
     */
    public static File getIncomingMessageDirectory()
    {
        String locationStr = MailboxActivator.getConfigurationService()
            .getString(Mailbox.INCOMING_MESSAGE_PROPERTY_NAME);
        if (locationStr != null)
        {
            File location = new File(locationStr);
            if (location.canWrite() && location.isDirectory())
            {
                return location;
            }
        }

        try
        {
            return MailboxActivator.getFileAccessService()
                .getPrivatePersistentDirectory(DEFAULT_INCOMING_MSG_DIR_NAME);
        }
        catch (Exception ex)
        {
            logger.error("Failed to create a private directory.", ex);
            return new File(MailboxActivator.getConfigurationService()
                            .getScHomeDirLocation()
                            + File.separator
                            + DEFAULT_INCOMING_MSG_DIR_NAME);
        }
    }

    /**
     * Implements CallListener.incomingCallReceived(). When a call is received,
     * waits to see if the call goes unanswered.
     *
     * @param event the <tt>CallEvent</tt> that has just been fired.
     */
    public void incomingCallReceived(CallEvent event)
    {
        logger.logEntry();

        new IncomingCallTracker(event).start();
    }

    /**
     * Implements <tt>CallListener.callEnded()</tt>. When a call ends, releases
     * custom data source mappings for that call.
     *
     * @param event the <tt>CallEvent</tt> containing the corresponding call.
     */
    public void callEnded(CallEvent event)
    {
        //tell the media service to release this call's data source mapping
        ServiceReference mediaServiceReference
            = bundleContext.getServiceReference(
                MediaService.class.getName());
        MediaService mediaService = (MediaService)bundleContext
            .getService(mediaServiceReference);
        mediaService.unsetCallDataSource(event.getSourceCall());
        mediaService.unsetCallDataSink(event.getSourceCall());

    }

    /**
     * Empty implementation  for <tt>CallListener.outgoingCallCreated()</tt>.
     *
     * @param event unused.
     */
    public void outgoingCallCreated(CallEvent event)
    {
        //we don't really care about outgoing calls.
    }

    /**
     * Starts the mailbox service, adds the mailbox as a call listener to the
     * currently registered protocol providers.
     *
     * @param bc a reference to the currently valid <tt>BundleContext</tt>.
    */
    public void start(BundleContext bc)
    {
        if (logger.isDebugEnabled())
            logger.debug("Starting the mailbox implementation.");
        this.bundleContext = bc;

        // start listening for newly registered or removed protocol providers
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
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            if (logger.isDebugEnabled())
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
     * Stops the service.
     *
     * @param bc a reference to the currently valid <tt>BundleContext</tt>.
     */
    public void stop(BundleContext bc)
    {
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
            logger.error("Error while retrieving service refs", ex);
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

    /**
     * Used to attach the Mailbox to existing or just registered protocol
     * provider. Checks if the provider has implementation of
     * <tt>OperationSetBasicTelephony</tt>.
     *
     * @param provider the <tt>ProtocolProviderService</tt> that has just been
     * added.
     */
    public void handleProviderAdded(ProtocolProviderService provider)
    {
        if (logger.isDebugEnabled())
            logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic telephony operation set
        OperationSetBasicTelephony opSetTelephony =
            provider.getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.addCallListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a basic telephony op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the calls made by it.
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicTelephony opSetTelephony =
            provider.getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.removeCallListener(this);
        }
    }

    /**
     * When new protocol provider is registered we check whether it supports
     * BasicTelephony and if so add a listener to it
     *
     * @param serviceEvent he <tt>ServiceEvent</tt> object containing details
     * on the change.
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = bundleContext.getService(
                                serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: "
                                    + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
                logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }

    }

    /**
     * We start this thread every time a new call arrives so that it would track
     * its state and answer it if nobody else does for <tt>getWaitTime()</tt>
     * milliseconds to pass.
     */
    private static class IncomingCallTracker extends Thread
    {
        /**
         * The call event that made us create this thread.
         */
        private final CallEvent callEvent;

        /**
         * Creates a new daemon thread that would track changes in the state of
         * the <tt>CallEvent</tt> source <tt>Call</tt>.
         *
         * @param callEv the <tt>CallEvent</tt> that made us create this thread.
         */
        public IncomingCallTracker(CallEvent callEv)
        {
            setDaemon(true);
            setName("IncomingCallTracker");
            callEvent = callEv;
        }

        /**
         * Waits for getWaitTime() milliseconds and if after that the Call is
         * still in the CALL_INITIALIZATION state, it sets the corresponding
         * data source and data sink on the media service and answers it.
         */
        public void run()
        {
            long timeCallReceived = System.currentTimeMillis();
            Call call = callEvent.getSourceCall();

            //first wait and see if someone picks up the call.
            while (call.getCallState() == CallState.CALL_INITIALIZATION)
            {
                //if we haven't waited long enough
                if (System.currentTimeMillis() < timeCallReceived
                                                    + getWaitTime())
                {
                    //wait more
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException exc)
                    {
                        logger.error("mailbox sleep was interrupted:\n"+ exc,
                                     exc);
                        return;
                    }
                }
            }

            if (call.getCallState() != CallState.CALL_INITIALIZATION)
            {
                logger.error("Someone else took care of this call. "
                             +"Bailing out.");
                return;
            }

            if (logger.isInfoEnabled())
                logger.info("Call waited long enough, picking up the phone");

            //add our datasource to our rtp manager and answer the call
            MediaService mediaService = MailboxActivator.getMediaService();
            URL mediaURL = null;

            try
            {
                mediaURL = getOutgoingMessageFileLocation().toURI().toURL();
                mediaService.setCallDataSource(call, mediaURL);
            }
            catch (Exception exc)
            {
                logger.error("Failed to set a call specific datasource.",
                             exc);
                return;
            }

            //right now I stamp the file with the system time.
            //seems as logical as anything else
            File directory = getIncomingMessageDirectory();

            File inFile = new File(directory.getAbsolutePath()
                                    + "/incoming_messsage"
                                    + System.currentTimeMillis()+".wav");
            //tell the media service to record this call in a file.
            try
            {
                URL incomingURL = inFile.toURI().toURL();
                mediaService.setCallDataSink(call, incomingURL);
            }
            catch (Exception exc)
            {
                logger.error("Failed to set a mailbox specific data sink.",
                             exc);
                mediaService.unsetCallDataSource(call);
                return;
            }

            //and here's where it all starts...'
            answerCall(call);

            //wait for someone to hangup ...
            waitForCallEnd(call);

            //... and if no one did - let's hangup ourselves.'
            if(call.getCallState() !=  CallState.CALL_ENDED)
                hangupCall(call);
        }

        /**
         * Waits for <tt>call</tt> to enter in the
         * {@link #net.java.sip.communicator.protocol.CallState.CALL_ENDED}
         * state.
         *
         * @param call the call that we'll be waiting to change state.
         */
        private void waitForCallEnd(Call call)
        {
            //if we picked up the call, we need to wait to hang it up in case
            //the caller does not
            double timeToWaitBeforeHangup = System.currentTimeMillis()
                        + getMaxMessageDuration()
                        + MailboxActivator.getMediaService()
                            .getDataSourceDurationSeconds(call)
                        * 1000;
            while (call.getCallState() !=  CallState.CALL_ENDED)
            {
                //if we haven't waited long enough
                if (System.currentTimeMillis() < timeToWaitBeforeHangup)
                {
                    //wait more
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException exc)
                    {
                        logger.error("mailbox sleep was interrupted:\n"
                                     + exc,
                                     exc);
                    }
                }
            }
        }

        /**
         * Goes through all peers in <tt>call</tt> and calls
         * <tt>telephony.aswerCallPeer()</tt> for every one of them.
         *
         * @param call the <tt>Call</tt> that we'd like to answer.
         */
        private void answerCall(Call call)
        {
            OperationSetBasicTelephony telephony
                = call
                    .getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while(peers.hasNext())
            {
                CallPeer peer = peers.next();

                try
                {
                    telephony.answerCallPeer(peer);
                }
                catch (OperationFailedException exc)
                {
                    logger.error("Could not answer to : "
                                 + peer
                                    + " caused by the following exception: "
                                    + exc.getMessage(),
                                    exc);
                }
            }
        }

        /**
         * Goes through all peers in <tt>call</tt> and calls
         * <tt>telephony.hangupCallPeer()</tt> for every one of them.
         *
         * @param call the <tt>Call</tt> that we'd like to answer.
         */
        private void hangupCall(Call call)
        {
            OperationSetBasicTelephony telephony
                = call
                    .getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);
            if (logger.isInfoEnabled())
                logger.info("Max Message Length Reached, Mailbox is"
                        +" disconnecting the call");
            Iterator<? extends CallPeer> callPeers = call.getCallPeers();

            while(callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();

                try
                {
                    telephony.hangupCallPeer(peer);
                }
                catch (OperationFailedException exc)
                {
                    logger.error("Could not Hang up on : "
                            + peer
                            + " caused by the following exception: "
                            + exc,
                            exc);
                }
            }
        }
    }

}

