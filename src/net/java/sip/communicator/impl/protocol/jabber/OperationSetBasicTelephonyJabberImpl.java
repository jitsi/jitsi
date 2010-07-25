/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing OperationSetBasicTelephony.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class OperationSetBasicTelephonyJabberImpl
   extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceJabberImpl>
   implements RegistrationStateChangeListener,
              PacketListener,
              PacketFilter
{

    /**
     * The logger used by this class
     */
    private static final Logger logger
            = Logger.getLogger(OperationSetBasicTelephonyJabberImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * that created us.
     */
    private ProtocolProviderServiceJabberImpl protocolProvider = null;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private ActiveCallsRepositoryJabberImpl activeCallsRepository
            = new ActiveCallsRepositoryJabberImpl(this);

    /**
     * Creates a new instance.
     *
     * @param protocolProvider a reference to the
     * <tt>ProtocolProviderServiceJabberImpl</tt> instance that created us.
     */
    public OperationSetBasicTelephonyJabberImpl(
            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        protocolProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>JingleManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if ((evt.getNewState() == RegistrationState.REGISTERING))
        {
            ProviderManager providerManager = ProviderManager.getInstance();
            providerManager.addIQProvider( JingleIQ.ELEMENT_NAME,
                                           JingleIQ.NAMESPACE,
                                           new JingleIQProvider());

            subscribeForJinglePackets();

            if (logger.isInfoEnabled())
                logger.info("Jingle : ON ");
        }
        else if ((evt.getNewState() == RegistrationState.UNREGISTERED))
        {
            // TODO: plug jingle unregistraion
            if (logger.isInfoEnabled())
                logger.info("Jingle : OFF ");
        }
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the jabber address of the callee that we should invite to a
     * new call.
     * @return CallPeer the CallPeer that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(String callee)
            throws OperationFailedException
    {
        return createOutgoingCall(callee);
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a
     *   new call.
     * @return CallPeer the CallPeer that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(Contact callee)
            throws OperationFailedException
    {

        return createOutgoingCall(callee.getAddress());
    }

    /**
     * Init and establish the specified call.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return CallPeer the CallPeer that represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    private CallJabberImpl createOutgoingCall(String calleeAddress)
            throws OperationFailedException
    {
        if (logger.isInfoEnabled())
            logger.info("creating outgoing call...");
        if (protocolProvider.getConnection() == null) {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "we don't have a valid XMPPConnection."
                    , OperationFailedException.INTERNAL_ERROR);
        }

        // we determine on which resource the remote user is connected if the
        // resource isn't already provided
        String fullCalleeURI = null;
        if (calleeAddress.indexOf('/') > 0)
        {
            fullCalleeURI = calleeAddress;
        }
        else
        {
            fullCalleeURI = protocolProvider.getConnection()
                .getRoster().getPresence(calleeAddress).getFrom();
        }

        /* in case we figure that calling people without a resource id is
           impossible, we'll have to uncomment the following lines. keep in mind
           that this would mean - no calls to pstn though
        if (fullCalleeURI.indexOf('/') < 0)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "User " + calleeAddress + " is unknown to us."
                    , OperationFailedException.INTERNAL_ERROR);
        }
        */

        try
        {
            // check if the remote client supports telephony.
            DiscoverInfo di = protocolProvider.getDiscoveryManager()
                    .discoverInfo(fullCalleeURI);
            if (di.containsFeature(ProtocolProviderServiceJabberImpl
                            .URN_XMPP_JINGLE))
            {
                if (logger.isInfoEnabled())
                    logger.info(fullCalleeURI + ": jingle supported ");
            }
            else
            {
                logger.info(calleeAddress + ": jingle not supported ??? ");
                throw new OperationFailedException(
                        "Failed to create OutgoingJingleSession.\n"
                        + fullCalleeURI + " does not support jingle"
                        , OperationFailedException.INTERNAL_ERROR);
            }
        }
        catch (XMPPException ex)
        {
            logger.warn("could not retrieve info for " + fullCalleeURI, ex);
        }

        //create the actual jingle call

        CallJabberImpl call = new CallJabberImpl(this);
try
{
        call.initiateSession(fullCalleeURI);
}
catch(Throwable t)
{
    t.printStackTrace();
}

        return call;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return an iterator over all currently active calls.
     */
    public Iterator<CallJabberImpl> getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    public synchronized void putOffHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, false);
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    public synchronized void putOnHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, true);
    }

    /**
     * Puts the specified <tt>CallPeer</tt> on or off hold.
     *
     * @param peer the <tt>CallPeer</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallPeer</tt>
     * put on hold; <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    private void putOnHold(CallPeer peer, boolean on)
        throws OperationFailedException
    {
        CallPeerJabberImpl jabberPeer = (CallPeerJabberImpl) peer;
        //jabberPeer.putOnHold(on);
    }

    /**
     * Sets the mute state of the <tt>CallJabberImpl</tt>.
     *
     * @param call the <tt>CallJabberImpl</tt> whose mute state is set
     * @param mute <tt>true</tt> to mute the call streams being sent to
     *            <tt>peers</tt>; otherwise, <tt>false</tt>
     */
    @Override
    public void setMute(Call call, boolean mute)
    {
        /** @todo implement putOnHold() */
    }

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     *
     * @throws ClassCastException if peer is not an instance of this
     * CallPeerSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public synchronized void hangupCallPeer(CallPeer peer)
        throws ClassCastException,
        OperationFailedException
    {
        CallPeerJabberImpl peerJabberImpl = (CallPeerJabberImpl)peer;
        peerJabberImpl.hangup();
    }



    /**
     * Implements method <tt>answerCallPeer</tt>
     * from <tt>OperationSetBasicTelephony</tt>.
     *
     * @param peer the call peer that we want to answer
     * @throws OperationFailedException if we fails to answer
     */
    public void answerCallPeer(CallPeer peer)
            throws OperationFailedException
    {
        CallPeerJabberImpl callPeer = (CallPeerJabberImpl)peer;

        callPeer.answer();
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public void shutdown()
    {
        if (logger.isTraceEnabled())
            logger.trace("Ending all active calls. ");
        Iterator<CallJabberImpl> activeCalls
            = this.activeCallsRepository.getActiveCalls();

        // this is fast, but events aren't triggered ...
        //jingleManager.disconnectAllSessions();

        //go through all active calls.
        while(activeCalls.hasNext())
        {
            CallJabberImpl call = activeCalls.next();
            Iterator<CallPeerJabberImpl> callPeers = call.getCallPeers();

            //go through all call peers and say bye to every one.
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                try
                {
                    this.hangupCallPeer(peer);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup peer "
                            + peer
                            , ex);
                }
            }
        }
    }

    /**
     * Subscribes us for notifications on incoming jingle packets.
     */
    private void subscribeForJinglePackets()
    {
        protocolProvider.getConnection().addPacketListener(this, this);
    }

    /**
     * Tests whether or not the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery
     * and it would only accept <tt>JingleIQ</tt>s that are either session
     * initiations with RTP content or belong to sessions that are already
     * handled by this operation set.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet)
    {
        //we only handle JingleIQ-s
        if( ! (packet instanceof JingleIQ) )
            return false;

        JingleIQ jingleIQ = (JingleIQ)packet;

        if( jingleIQ.getAction() == JingleAction.SESSION_INITIATE)
        {
            //we only accept session-initiate-s dealing RTP
            if( jingleIQ.containsContentChildOfType(
                        RtpDescriptionPacketExtension.class))
                return true;
            else
                return false;
        }

        //if this is not a session-initiate we'll only take it if we've
        //already seen its session ID.
        if( activeCallsRepository.findJingleSID(jingleIQ.getSID()) != null )
            return true;
        else
            return false;
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        //this is not supposed to happen because of the filter ... but still
        if (! (packet instanceof JingleIQ) )
            return;

        JingleIQ jingleIQ = (JingleIQ)packet;

        //to prevent hijacking sessions from other jingle based features
        //like file transfer for example,  we should only send the
        //ack if this is a session-initiate with rtp content or if we are
        //the owners of this packet's sid



        //first ack all "set" requests.
        if(jingleIQ.getType() == IQ.Type.SET)
        {
            IQ ack = IQ.createResultIQ(jingleIQ);
            protocolProvider.getConnection().sendPacket(ack);
        }

        processJinglePacket(jingleIQ);
    }

    /**
     * Analyzes the <tt>jingleIQ</tt>'s action and passes it to the
     * corresponding handler.
     *
     * @param jingleIQ the {@link JingleIQ} packet we need to be analyzing.
     */
    private void processJinglePacket(JingleIQ jingleIQ)
    {
        if (jingleIQ.getType() == Type.ERROR)
        {
            CallPeerJabberImpl callPeer =
                activeCallsRepository.findCallPeer(jingleIQ.getSID());

            logger.error("Received error");
            XMPPError error = jingleIQ.getError();
            String message = "Remote party returned an error!";

            if(error != null)
            {
                logger.error(" code=" + error.getCode()
                                + " message=" + error.getMessage());

                if (error.getMessage() != null)
                    message = error.getMessage();
            }

            if (callPeer != null)
                callPeer.setState(CallPeerState.FAILED, message);

            return;
        }

        JingleAction action = jingleIQ.getAction();

        if(action == JingleAction.SESSION_INITIATE)
        {
            CallJabberImpl call = new CallJabberImpl(this);
            call.processSessionInitiate(jingleIQ);
        }
        else if(action == JingleAction.SESSION_TERMINATE)
        {

        }
        else if(action == JingleAction.SESSION_ACCEPT)
        {

        }
        else if (action == JingleAction.SESSION_INFO)
        {
            SessionInfoPacketExtension info = (SessionInfoPacketExtension)
                jingleIQ.getExtension(SessionInfoPacketExtension.NAMESPACE);

            if(info == null)
                return;

            if( info.getType() == SessionInfoType.ringing)
            {
                CallPeerJabberImpl callPeer =
                    activeCallsRepository.findCallPeer(jingleIQ.getSID());

                if (callPeer == null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Received a stray trying response.");
                    return;
                }

                // change status.
                callPeer.setState(CallPeerState.ALERTING_REMOTE_SIDE);
            }
        }
    }

    /**
     * Returns a reference to the {@link ActiveCallsRepositoryJabberImpl} that
     * we are currently using.
     *
     * @return a reference to the {@link ActiveCallsRepositoryJabberImpl} that
     * we are currently using.
     */
    protected ActiveCallsRepositoryJabberImpl getActiveCallsRepository()
    {
        return activeCallsRepository;
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public ProtocolProviderServiceJabberImpl getProtocolProvider()
    {
        return protocolProvider;
    }
}
