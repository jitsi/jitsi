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
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing <tt>OperationSetBasicTelephony</tt>.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 * @author Lyubomir Marinov
 */
public class OperationSetBasicTelephonyJabberImpl
   extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceJabberImpl>
   implements RegistrationStateChangeListener,
              PacketListener,
              PacketFilter,
              OperationSetSecureTelephony,
              OperationSetAdvancedTelephony<ProtocolProviderServiceJabberImpl>
{

    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetBasicTelephonyJabberImpl</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicTelephonyJabberImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * that created us.
     */
    private final ProtocolProviderServiceJabberImpl protocolProvider;

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
        this.protocolProvider.addRegistrationStateChangeListener(this);
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
        RegistrationState registrationState = evt.getNewState();

        if (registrationState == RegistrationState.REGISTERING)
        {
            ProviderManager.getInstance().addIQProvider(
                    JingleIQ.ELEMENT_NAME,
                    JingleIQ.NAMESPACE,
                    new JingleIQProvider());

            subscribeForJinglePackets();

            if (logger.isInfoEnabled())
                logger.info("Jingle : ON ");
        }
        else if (registrationState == RegistrationState.UNREGISTERED)
        {
            // plug jingle unregistration
            unsubscribeForJinglePackets();

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
        CallJabberImpl call = new CallJabberImpl(this);

        if (createOutgoingCall(call, callee) == null)
        {
            throw new OperationFailedException(
                    "Failed to create outgoing call"
                        + " because no peer was created",
                    OperationFailedException.INTERNAL_ERROR);
        }
        return call;
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
        return createCall(callee.getAddress());
    }

    /**
     * Init and establish the specified call.
     *
     * @param call the <tt>CallJabberImpl</tt> that will be used
     * to initiate the call
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return the <tt>CallPeer</tt> that represented by the specified uri. All
     * following state change events will be delivered through that call peer.
     * The <tt>Call</tt> that this peer is a member of could be retrieved from
     * the <tt>CallPeer</tt> instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    CallPeerJabberImpl createOutgoingCall(
            CallJabberImpl call,
            String calleeAddress)
        throws OperationFailedException
    {
        return createOutgoingCall(call, calleeAddress, null);
    }

    /**
     * Init and establish the specified call.
     *
     * @param call the <tt>CallJabberImpl</tt> that will be used
     * to initiate the call
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to init the specified <tt>call</tt>
     *
     * @return the <tt>CallPeer</tt> that represented by the specified uri. All
     * following state change events will be delivered through that call peer.
     * The <tt>Call</tt> that this peer is a member of could be retrieved from
     * the <tt>CallPeer</tt> instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    CallPeerJabberImpl createOutgoingCall(
            CallJabberImpl call,
            String calleeAddress,
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        if (logger.isInfoEnabled())
            logger.info("creating outgoing call...");
        if (protocolProvider.getConnection() == null || call == null)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession."
                        + " We don't have a valid XMPPConnection.",
                    OperationFailedException.INTERNAL_ERROR);
        }

        // we determine on which resource the remote user is connected if the
        // resource isn't already provided
        String fullCalleeURI = null;

        DiscoverInfo di = null;
        int bestPriority = -1;

        Iterator<Presence> it =
            getProtocolProvider().getConnection().getRoster().getPresences(
                calleeAddress);
        String calleeURI = null;

        // choose the resource that has the highest priority AND support Jingle
        while(it.hasNext())
        {
            Presence presence = it.next();
            int priority = (presence.getPriority() == Integer.MIN_VALUE) ? 0 :
                presence.getPriority();
            calleeURI = presence.getFrom();

            try
            {
                // check if the remote client supports telephony.
                DiscoverInfo discoverInfo =
                    protocolProvider.getDiscoveryManager().
                        discoverInfo(calleeURI);

                if (discoverInfo.containsFeature(
                        ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE))
                {
                    if(priority > bestPriority)
                    {
                        bestPriority = priority;
                        di = discoverInfo;
                        fullCalleeURI = calleeURI;
                    }
                }
            }
            catch (XMPPException ex)
            {
                logger.warn("could not retrieve info for " + fullCalleeURI, ex);
            }
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

        if(di != null)
        {
            if (logger.isInfoEnabled())
                logger.info(fullCalleeURI + ": jingle supported ");
        }
        else
        {
            if (logger.isInfoEnabled())
                logger.info(calleeURI + ": jingle not supported ?");
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                        + calleeURI + " does not support jingle",
                    OperationFailedException.INTERNAL_ERROR);
        }

        if(logger.isInfoEnabled())
        {
            logger.info("Choose one is: " + fullCalleeURI + " " + bestPriority);
        }

        CallPeerJabberImpl peer = null;

        // initiate call
        try
        {
            peer
                = call.initiateSession(
                        fullCalleeURI,
                        di,
                        sessionInitiateExtensions);
        }
        catch(Throwable t)
        {
            /*
             * The Javadoc on ThreadDeath says: If ThreadDeath is caught by a
             * method, it is important that it be rethrown so that the thread
             * actually dies.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;

            throw new OperationFailedException(
                    "Failed to create a call",
                    OperationFailedException.INTERNAL_ERROR,
                    t);
        }

        return peer;
    }

    /**
     * Gets the full callee URI for a specific callee address.
     *
     * @param calleeAddress the callee address to get the full callee URI for
     * @return the full callee URI for the specified <tt>calleeAddress</tt>
     */
    String getFullCalleeURI(String calleeAddress)
    {
        return
            (calleeAddress.indexOf('/') > 0)
                ? calleeAddress
                : protocolProvider
                    .getConnection()
                        .getRoster()
                            .getPresence(calleeAddress)
                                .getFrom();
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
        ((CallPeerJabberImpl) peer).putOnHold(on);
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
        ((CallJabberImpl) call).setMute(mute);
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
        ((CallPeerJabberImpl) peer).hangup(null, null);
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
        ((CallPeerJabberImpl) peer).answer();
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
                    logger.warn("Failed to properly hangup peer " + peer, ex);
                }
            }
        }
    }

    /**
     * Subscribes us to notifications about incoming jingle packets.
     */
    private void subscribeForJinglePackets()
    {
        protocolProvider.getConnection().addPacketListener(this, this);
    }

    /**
     * Unsubscribes us to notifications about incoming jingle packets.
     */
    private void unsubscribeForJinglePackets()
    {
        if(protocolProvider.getConnection() != null)
        {
            protocolProvider.getConnection().removePacketListener(this);
        }
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
        {
            CallPeerJabberImpl callPeer =
                activeCallsRepository.findCallPeerBySessInitPacketID(
                    packet.getPacketID());

            if(callPeer != null)
            {
                /* packet is a response to a Jingle call but is not a JingleIQ
                 * so it is for sure an error (peer does not support Jingle or
                 * does not belong to our roster)
                 */
                XMPPError error = packet.getError();

                if (error != null)
                {
                    logger.error("Received an error: code=" + error.getCode()
                            + " message=" + error.getMessage());
                    String message = "Service unavailable";
                    Roster roster = getProtocolProvider().getConnection().
                        getRoster();

                    if(!roster.contains(packet.getFrom()))
                    {
                        message += ": try adding the contact to your contact " +
                                "list first.";
                    }

                    if (error.getMessage() != null)
                        message = error.getMessage();

                    callPeer.setState(CallPeerState.FAILED, message);
                }
            }
            return false;
        }

        JingleIQ jingleIQ = (JingleIQ)packet;

        if( jingleIQ.getAction() == JingleAction.SESSION_INITIATE)
        {
            //we only accept session-initiate-s dealing RTP
            return
                jingleIQ.containsContentChildOfType(
                        RtpDescriptionPacketExtension.class);
        }

        //if this is not a session-initiate we'll only take it if we've
        //already seen its session ID.
        return (activeCallsRepository.findJingleSID(jingleIQ.getSID()) != null);
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

        try
        {
            processJingleIQ(jingleIQ);
        }
        catch(Throwable t)
        {
            logger.info("Error while handling incoming Jingle packet: ", t);

            /*
             * The Javadoc on ThreadDeath says: If ThreadDeath is caught by a
             * method, it is important that it be rethrown so that the thread
             * actually dies.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
    }

    /**
     * Analyzes the <tt>jingleIQ</tt>'s action and passes it to the
     * corresponding handler.
     *
     * @param jingleIQ the {@link JingleIQ} packet we need to be analyzing.
     */
    private void processJingleIQ(JingleIQ jingleIQ)
    {
        //let's first see whether we have a peer that's concerned by this IQ
        CallPeerJabberImpl callPeer
            = activeCallsRepository.findCallPeer(jingleIQ.getSID());
        IQ.Type type = jingleIQ.getType();

        if (type == Type.ERROR)
        {
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
            return;
        }
        else if (callPeer == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a stray trying response.");
            return;
        }

        //the rest of these cases deal with existing peers
        else if(action == JingleAction.SESSION_TERMINATE)
        {
            callPeer.processSessionTerminate(jingleIQ);
        }
        else if(action == JingleAction.SESSION_ACCEPT)
        {
            callPeer.processSessionAccept(jingleIQ);
        }
        else if (action == JingleAction.SESSION_INFO)
        {
            SessionInfoPacketExtension info = jingleIQ.getSessionInfo();

            if(info != null)
            {
                // change status.
                callPeer.processSessionInfo(info);
            }
            else
            {
                PacketExtension packetExtension
                    = jingleIQ.getExtension(
                            TransferPacketExtension.ELEMENT_NAME,
                            TransferPacketExtension.NAMESPACE);

                if (packetExtension instanceof TransferPacketExtension)
                {
                    TransferPacketExtension transfer
                        = (TransferPacketExtension) packetExtension;

                    if (transfer.getFrom() == null)
                        transfer.setFrom(jingleIQ.getFrom());

                    try
                    {
                        callPeer.processTransfer(transfer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Failed to transfer to " + transfer.getTo(),
                                ofe);
                    }
                }
            }
        }
        else if (action == JingleAction.CONTENT_ACCEPT)
        {
            callPeer.processContentAccept(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_ADD)
        {
            callPeer.processContentAdd(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_MODIFY)
        {
            callPeer.processContentModify(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_REJECT)
        {
            callPeer.processContentReject(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_REMOVE)
        {
            callPeer.processContentRemove(jingleIQ);
        }
        else if (action == JingleAction.TRANSPORT_INFO)
        {
            callPeer.processTransportInfo(jingleIQ);
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

    /**
     * Gets the secure state of the call session in which a specific peer
     * is involved
     *
     * @param peer the peer for who the call state is required
     * @return the call state
     */
    public boolean isSecure(CallPeer peer)
    {
        return ((CallPeerJabberImpl) peer).getMediaHandler().isSecure();
    }

    /**
     * Sets the SAS verifications state of the call session in which a specific
     * peer is involved
     *
     * @param peer the peer who toggled (or for whom is remotely
     *        toggled) the SAS verified flag
     * @param verified the new SAS verification status
     */
    public void setSasVerified(CallPeer peer, boolean verified)
    {
        ((CallPeerJabberImpl) peer).getMediaHandler().setSasVerified(verified);
    }

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which already participates in an active
     * <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address in the form of <tt>CallPeer</tt> of the callee
     * to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong
     * @see OperationSetAdvancedTelephony#transfer(CallPeer, CallPeer)
     */
    public void transfer(CallPeer peer, CallPeer target)
        throws OperationFailedException
    {
        CallPeerJabberImpl targetJabberImpl = (CallPeerJabberImpl) target;
        String to = getFullCalleeURI(targetJabberImpl.getAddress());

        /*
         * XEP-0251: Jingle Session Transfer says: Before doing
         * [attended transfer], the attendant SHOULD verify that the callee
         * supports Jingle session transfer.
         */
        try
        {
            DiscoverInfo discoverInfo
                = protocolProvider.getDiscoveryManager().discoverInfo(to);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl
                        .URN_XMPP_JINGLE_TRANSFER_0))
            {
                throw new OperationFailedException(
                        "Callee "
                            + to
                            + " does not support"
                            + " XEP-0251: Jingle Session Transfer",
                        OperationFailedException.INTERNAL_ERROR);
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        transfer(
            peer,
            to, targetJabberImpl.getJingleSID());
    }

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which may or may not already be participating
     * in an active <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of unattended
     * call transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address of the callee to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong
     * @see OperationSetAdvancedTelephony#transfer(CallPeer, String)
     */
    public void transfer(CallPeer peer, String target)
        throws OperationFailedException
    {
        transfer(peer, target, null);
    }

    /**
     * Transfer (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which may optionally be participating in an
     * active <tt>Call</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param to the address of the callee to transfer <tt>peer</tt> to
     * @param sid the Jingle session ID of the active <tt>Call</tt> between the
     * local peer and the callee in the case of attended transfer; <tt>null</tt>
     * in the case of unattended transfer
     * @throws OperationFailedException if something goes wrong
     */
    private void transfer(CallPeer peer, String to, String sid)
        throws OperationFailedException
    {
        String caller = getFullCalleeURI(peer.getAddress());

        try
        {
            DiscoverInfo discoverInfo
                = protocolProvider.getDiscoveryManager().discoverInfo(caller);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl
                        .URN_XMPP_JINGLE_TRANSFER_0))
            {
                throw new OperationFailedException(
                        "Caller "
                            + caller
                            + " does not support"
                            + " XEP-0251: Jingle Session Transfer",
                        OperationFailedException.INTERNAL_ERROR);
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        ((CallPeerJabberImpl) peer).transfer(getFullCalleeURI(to), sid);
    }
}
