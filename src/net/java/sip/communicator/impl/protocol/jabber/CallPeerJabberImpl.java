/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;
import java.lang.reflect.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a Jabber <tt>CallPeer</tt>.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class CallPeerJabberImpl
    extends MediaAwareCallPeer<CallJabberImpl,
                               CallPeerMediaHandlerJabberImpl,
                               ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerJabberImpl.class);

    /**
     * The jabber address of this peer
     */
    private String peerJID = null;

    /**
     * The {@link JingleIQ} that created the session that this call represents.
     */
    private JingleIQ sessionInitIQ;

    /**
     * Any discovery information that we have for this peer.
     */
    private DiscoverInfo discoverInfo;

    /**
     * Indicates whether this peer was the one that initiated the session.
     */
    private boolean isInitiator = false;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Jabber address of the new call peer.
     * @param owningCall the call that contains this call peer.
     */
    public CallPeerJabberImpl(String         peerAddress,
                              CallJabberImpl owningCall)
    {
        super(owningCall);

        this.peerJID = peerAddress;
        setMediaHandler( new CallPeerMediaHandlerJabberImpl(this) );
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return peerJID;
    }

    /**
     * Specifies the address, phone number, or other protocol specific
     * identifier that represents this call peer. This method is to be
     * used by service users and MUST NOT be called by the implementation.
     *
     * @param address The address of this call peer.
     */
    public void setAddress(String address)
    {
        String oldAddress = getAddress();

        if(peerJID.equals(address))
            return;

        this.peerJID = address;
        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_ADDRESS_CHANGE,
                oldAddress,
                address);
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        if (getCall() != null)
        {
            Contact contact = getContact();

            if (contact != null)
                return contact.getDisplayName();
        }
        return peerJID;
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        ProtocolProviderService pps = getCall().getProtocolProvider();
        OperationSetPresence opSetPresence
            = pps.getOperationSet(OperationSetPresence.class);

        return opSetPresence.findContactByID(getAddress());
    }

    /**
     * Processes the session initiation {@link JingleIQ} that we were created
     * with, passing its content to the media handler and then sends either a
     * "session-info/ringing" or a "session-terminate" response.
     *
     * @param sessionInitIQ The {@link JingleIQ} that created the session that
     * we are handling here.
     */
    protected synchronized void processSessionInitiate(JingleIQ sessionInitIQ)
    {
        /*
         * We've already sent ack to the specified session-initiate so if it has
         * been sent as part of an attended transfer, we have to hang up on the
         * attendant.
         */
        try
        {
            TransferPacketExtension transfer
                = (TransferPacketExtension)
                    sessionInitIQ.getExtension(
                            TransferPacketExtension.ELEMENT_NAME,
                            TransferPacketExtension.NAMESPACE);

            if (transfer != null)
            {
                String sid = transfer.getSID();

                if (sid != null)
                {
                    ProtocolProviderServiceJabberImpl protocolProvider
                        = getProtocolProvider();
                    OperationSetBasicTelephonyJabberImpl basicTelephony
                        = (OperationSetBasicTelephonyJabberImpl)
                            protocolProvider
                                .getOperationSet(
                                        OperationSetBasicTelephony.class);
                    CallJabberImpl attendantCall
                        = basicTelephony
                            .getActiveCallsRepository()
                                .findJingleSID(sid);

                    if (attendantCall != null)
                    {
                        CallPeerJabberImpl attendant
                            = attendantCall.getPeer(sid);

                        if ((attendant != null)
                                && basicTelephony
                                    .getFullCalleeURI(attendant.getAddress())
                                        .equals(transfer.getFrom())
                                && protocolProvider.getOurJID().equals(
                                        transfer.getTo()))
                        {
                            basicTelephony.hangupCallPeer(attendant);
                        }
                    }
                }
            }
        }
        catch (Throwable t)
        {
            logger.error(
                    "Failed to hang up on attendant"
                        + " as part of session transfer",
                    t);

            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }

        // Do initiate the session.
        this.sessionInitIQ = sessionInitIQ;
        this.isInitiator = true;

        // This is the SDP offer that came from the initial session-initiate.
        // Contrary to SIP, we are guaranteed to have content because XEP-0166
        // says: "A session consists of at least one content type at a time."
        List<ContentPacketExtension> offer = sessionInitIQ.getContentList();

        try
        {
            getMediaHandler().processOffer(offer);
        }
        catch(Exception ex)
        {
            logger.info("Failed to process an incoming session initiate", ex);

            //send an error response;
            String reasonText = "Error: " + ex.getMessage();
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getSID(),
                        Reason.INCOMPATIBLE_PARAMETERS,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        //send a ringing response
        if (logger.isTraceEnabled())
            logger.trace("will send ringing response: ");

        getProtocolProvider().getConnection().sendPacket(
                JinglePacketFactory.createRinging(sessionInitIQ));
    }

    /**
     * Processes the session initiation {@link JingleIQ} that we were created
     * with, passing its content to the media handler and then sends either a
     * "session-info/ringing" or a "session-terminate" response.
     *
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to initiate the session with this
     * <tt>CallPeerJabberImpl</tt>
     * @throws OperationFailedException exception
     */
    protected synchronized void initiateSession(
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        isInitiator = false;

        //Create the media description that we'd like to send to the other side.
        List<ContentPacketExtension> offer
            = getMediaHandler().createContentList();

        //send a ringing response
        if (logger.isTraceEnabled())
            logger.trace("will send ringing response: ");

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();

        sessionInitIQ
            = JinglePacketFactory.createSessionInitiate(
                    protocolProvider.getOurJID(),
                    this.peerJID,
                    JingleIQ.generateSID(),
                    offer);
        if (sessionInitiateExtensions != null)
        {
            for (PacketExtension sessionInitiateExtension
                    : sessionInitiateExtensions)
            {
                sessionInitIQ.addExtension(sessionInitiateExtension);
            }
        }
        protocolProvider.getConnection().sendPacket(sessionInitIQ);
    }

    /**
     * Indicates a user request to answer an incoming call from this
     * <tt>CallPeer</tt>.
     *
     * Sends an OK response to <tt>callPeer</tt>. Make sure that the call
     * peer contains an SDP description when you call this method.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response.
     */
    public synchronized void answer()
        throws OperationFailedException
    {
        Iterable<ContentPacketExtension> answer;

        try
        {
            answer = getMediaHandler().generateSessionAccept();
        }
        catch(Exception exc)
        {
            logger.info("Failed to answer an incoming call", exc);

            //send an error response
            String reasonText = "Error: " + exc.getMessage();
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getSID(),
                        Reason.FAILED_APPLICATION,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        JingleIQ response
            = JinglePacketFactory.createSessionAccept(
                    sessionInitIQ.getTo(),
                    sessionInitIQ.getFrom(),
                    getJingleSID(),
                    answer);

        //send the packet first and start the stream later  in case the media
        //relay needs to see it before letting hole punching techniques through.
        getProtocolProvider().getConnection().sendPacket(response);

        try
        {
            getMediaHandler().start();
        }
        catch(UndeclaredThrowableException e)
        {
            Throwable exc = e.getUndeclaredThrowable();

            logger.info("Failed to establish a connection", exc);

            //send an error response
            String reasonText = "Error: " + exc.getMessage();
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getSID(),
                        Reason.GENERAL_ERROR,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        //tell everyone we are connecting so that the audio notifications would
        //stop
        setState(CallPeerState.CONNECTED);
    }

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @param reasonText the text, if any, to be set on the
     * <tt>ReasonPacketExtension</tt> as the value of its
     * @param reasonOtherExtension the <tt>PacketExtension</tt>, if any, to be
     * set on the <tt>ReasonPacketExtension</tt> as the value of its
     * <tt>otherExtension</tt> property
     */
    public void hangup(String reasonText, PacketExtension reasonOtherExtension)
    {
        // do nothing if the call is already ended
        if (CallPeerState.DISCONNECTED.equals(getState())
            || CallPeerState.FAILED.equals(getState()))
        {
            if (logger.isDebugEnabled())
                logger.debug("Ignoring a request to hangup a call peer "
                        + "that is already DISCONNECTED");
            return;
        }

        CallPeerState prevPeerState = getState();
        setState(CallPeerState.DISCONNECTED);
        JingleIQ responseIQ = null;

        if (prevPeerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(prevPeerState))
        {
            responseIQ = JinglePacketFactory.createBye(
                getProtocolProvider().getOurJID(), peerJID, getJingleSID());
        }
        else if (CallPeerState.CONNECTING.equals(prevPeerState)
            || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(prevPeerState)
            || CallPeerState.ALERTING_REMOTE_SIDE.equals(prevPeerState))
        {
            responseIQ = JinglePacketFactory.createCancel(
                getProtocolProvider().getOurJID(), peerJID, getJingleSID());
        }
        else if (prevPeerState.equals(CallPeerState.INCOMING_CALL))
        {
            responseIQ = JinglePacketFactory.createBusy(
                getProtocolProvider().getOurJID(), peerJID, getJingleSID());
        }
        else if (prevPeerState.equals(CallPeerState.BUSY)
                 || prevPeerState.equals(CallPeerState.FAILED))
        {
            // For FAILED and BUSY we only need to update CALL_STATUS
            // as everything else has been done already.
        }
        else
        {
            logger.info("Could not determine call peer state!");
        }

        if (responseIQ != null)
        {
            if (reasonOtherExtension != null)
            {
                ReasonPacketExtension reason
                    = (ReasonPacketExtension)
                        responseIQ.getExtension(
                                ReasonPacketExtension.ELEMENT_NAME,
                                ReasonPacketExtension.NAMESPACE);

                if (reason != null)
                    reason.setOtherExtension(reasonOtherExtension);
            }

            getProtocolProvider().getConnection().sendPacket(responseIQ);
        }
    }

    /**
     * Returns the session ID of the Jingle session associated with this call.
     *
     * @return the session ID of the Jingle session associated with this call.
     */
    public String getJingleSID()
    {
        return sessionInitIQ.getSID();
    }

    /**
     * Puts this peer into a {@link CallPeerState#DISCONNECTED}, indicating a
     * reason to the user, if there is one.
     *
     * @param jingleIQ the {@link JingleIQ} that's terminating our session.
     */
    public void processSessionTerminate(JingleIQ jingleIQ)
    {
        String reasonStr = "Call ended by remote side.";
        ReasonPacketExtension reasonExt = jingleIQ.getReason();

        if(reasonStr != null)
        {
            Reason reason = reasonExt.getReason();

            if(reason != null)
                reasonStr += " Reason: " + reason.toString() + ".";

            String text = reasonExt.getText();

            if(text != null)
                reasonStr += " " + text;
        }

        setState(CallPeerState.DISCONNECTED, reasonStr);
    }

    /**
     * Processes the session initiation {@link JingleIQ} that we were created
     * with, passing its content to the media handler and then sends either a
     * "session-info/ringing" or a "session-terminate" response.
     *
     * @param sessionInitIQ The {@link JingleIQ} that created the session that
     * we are handling here.
     */
    public void processSessionAccept(JingleIQ sessionInitIQ)
    {
        this.sessionInitIQ = sessionInitIQ;

        CallPeerMediaHandlerJabberImpl mediaHandler = getMediaHandler();
        List<ContentPacketExtension> answer = sessionInitIQ.getContentList();

        try
        {
            mediaHandler.processAnswer(answer);
        }
        catch(Exception exc)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to process a session-accept", exc);

            //send an error response;
            JingleIQ errResp = JinglePacketFactory.createSessionTerminate(
                sessionInitIQ.getTo(), sessionInitIQ.getFrom(),
                sessionInitIQ.getSID(), Reason.INCOMPATIBLE_PARAMETERS,
                exc.getClass().getName() + ": " + exc.getMessage());

            setState(CallPeerState.FAILED, "Error: " + exc.getMessage());
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        //tell everyone we are connecting so that the audio notifications would
        //stop
        setState(CallPeerState.CONNECTED);

        mediaHandler.start();
    }

    /**
     * Puts the <tt>CallPeer</tt> represented by this instance on or off hold.
     *
     * @param onHold <tt>true</tt> to have the <tt>CallPeer</tt> put on hold;
     * <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    public void putOnHold(boolean onHold)
        throws OperationFailedException
    {
        CallPeerMediaHandlerJabberImpl mediaHandler = getMediaHandler();

        mediaHandler.setLocallyOnHold(onHold);

        SessionInfoType type;

        if(onHold)
            type = SessionInfoType.hold;
        else
        {
            type = SessionInfoType.unhold;
            getMediaHandler().reinitAllContents();
        }

        //we are now on hold and need to realize this before potentially
        //spoiling it all with an exception while sending the packet :).
        reevalLocalHoldStatus();

        JingleIQ onHoldIQ = JinglePacketFactory.createSessionInfo(
                        getProtocolProvider().getOurJID(),
                        peerJID,
                        getJingleSID(),
                        type);

        getProtocolProvider().getConnection().sendPacket(onHoldIQ);
    }

    /**
     * Sets the service discovery information that we have for this peer.
     *
     * @param discoverInfo the discovery information that we have obtained for
     * this peer.
     */
    public void setDiscoverInfo(DiscoverInfo discoverInfo)
    {
        this.discoverInfo = discoverInfo;
    }

    /**
     * Returns the service discovery information that we have for this peer.
     *
     * @return the service discovery information that we have for this peer.
     */
    public DiscoverInfo getDiscoverInfo()
    {
        return discoverInfo;
    }

    /**
     * Determines whether this peer was the one that initiated the session. Note
     * that if this peer is the initiator of the session then this means we are
     * the responder!
     *
     * @return <tt>true</tt> if this peer is the one that initiated the session
     * and <tt>false</tt> otherwise (i.e. if _we_ initiated the session).
     */
    public boolean isInitiator()
    {
        return isInitiator;
    }

    /**
     * Handles the specified session <tt>info</tt> packet according to its
     * content.
     *
     * @param info the {@link SessionInfoPacketExtension} that we just received.
     */
    public void processSessionInfo(SessionInfoPacketExtension info)
    {
        switch (info.getType())
        {
        case ringing:
            setState(CallPeerState.ALERTING_REMOTE_SIDE);
            break;
        case hold:
            getMediaHandler().setRemotelyOnHold(true);
            reevalRemoteHoldStatus();
            break;
        case unhold:
        case active:
            getMediaHandler().setRemotelyOnHold(false);
            reevalRemoteHoldStatus();
            break;
        default:
            logger.warn("Received SessionInfoPacketExtension of unknown type");
        }
    }

    /**
     * Processes a specific "XEP-0251: Jingle Session Transfer"
     * <tt>transfer</tt> packet (extension).
     *
     * @param transfer the "XEP-0251: Jingle Session Transfer" transfer packet
     * (extension) to process
     * @throws OperationFailedException if anything goes wrong while processing
     * the specified <tt>transfer</tt> packet (extension)
     */
    public void processTransfer(TransferPacketExtension transfer)
        throws OperationFailedException
    {
        String attendantAddress = transfer.getFrom();

        if (attendantAddress == null)
        {
            throw new OperationFailedException(
                    "Session transfer must contain a \'from\' attribute value.",
                    OperationFailedException.ILLEGAL_ARGUMENT);
        }

        String calleeAddress = transfer.getTo();

        if (calleeAddress == null)
        {
            throw new OperationFailedException(
                    "Session transfer must contain a \'to\' attribute value.",
                    OperationFailedException.ILLEGAL_ARGUMENT);
        }

        putOnHold(true);

        OperationSetBasicTelephonyJabberImpl basicTelephony
            = (OperationSetBasicTelephonyJabberImpl)
                getProtocolProvider()
                    .getOperationSet(OperationSetBasicTelephony.class);
        CallJabberImpl calleeCall = new CallJabberImpl(basicTelephony);
        TransferPacketExtension calleeTransfer = new TransferPacketExtension();
        String sid = transfer.getSID();

        calleeTransfer.setFrom(attendantAddress);
        if (sid != null)
        {
            calleeTransfer.setSID(sid);
            calleeTransfer.setTo(calleeAddress);
        }
        basicTelephony.createOutgoingCall(
                calleeCall,
                calleeAddress,
                Arrays.asList(new PacketExtension[] { calleeTransfer }));

        hangup(
            ((sid == null) ? "Unattended" : "Attended") + " transfer success",
            new TransferredPacketExtension());
    }

    /**
     * Send a <tt>content-add</tt> to add video setup.
     */
    private void sendAddVideoContent()
    {
        List<ContentPacketExtension> contents;

        try
        {
            contents = getMediaHandler().createContentList(MediaType.VIDEO);
        }
        catch(Exception exc)
        {
            logger.warn("Failed to gather content for video type", exc);
            return;
        }

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();
        JingleIQ contentIQ
            = JinglePacketFactory.createContentAdd(
                    protocolProvider.getOurJID(),
                    this.peerJID,
                    getJingleSID(),
                    contents);

        protocolProvider.getConnection().sendPacket(contentIQ);
    }

    /**
     * Send a <tt>content-remove</tt> to remove video setup.
     */
    private void sendRemoveVideoContent()
    {
        CallPeerMediaHandlerJabberImpl mediaHandler = getMediaHandler();

        ContentPacketExtension content = new ContentPacketExtension();
        ContentPacketExtension remoteContent
            = mediaHandler.getRemoteContent(MediaType.VIDEO.toString());

        content.setName(remoteContent.getName());
        content.setCreator(remoteContent.getCreator());
        content.setSenders(remoteContent.getSenders());

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();
        JingleIQ contentIQ
            = JinglePacketFactory.createContentRemove(
                    protocolProvider.getOurJID(),
                    this.peerJID,
                    getJingleSID(),
                    Arrays.asList(content));

        protocolProvider.getConnection().sendPacket(contentIQ);
        mediaHandler.removeContent(remoteContent.getName());
    }

    /**
     * Send a <tt>content</tt> message to reflect change in video setup (start
     * or stop). Message can be content-modify if video content exists,
     * content-add if we start video but video is not enabled on the peer or
     * content-remove if we stop video and video is not enabled on the peer.
     *
     * @param allowed if the local video is allowed or not
     */
    public void sendModifyVideoContent(boolean allowed)
    {
        ContentPacketExtension ext = new ContentPacketExtension();
        ContentPacketExtension remoteContent = getMediaHandler().
            getRemoteContent(MediaType.VIDEO.toString());

        if(remoteContent == null)
        {
            if(allowed)
                sendAddVideoContent();
            return;
        }
        else if(!allowed &&
                ((!isInitiator &&
                 remoteContent.getSenders() == SendersEnum.initiator) ||
                (isInitiator &&
                 remoteContent.getSenders() == SendersEnum.responder)))
        {
            sendRemoveVideoContent();
            return;
        }

        SendersEnum senders = remoteContent.getSenders();

        /* adjust the senders attribute depending on current value and if we
         * allowed or not local video streaming
         */
        if(allowed)
        {
            if(senders != SendersEnum.none)
            {
                senders = SendersEnum.both;
            }
            else if(senders == SendersEnum.none)
            {
                senders
                    = isInitiator
                        ? SendersEnum.responder
                        : SendersEnum.initiator;
            }
        }
        else
        {
            if(senders == SendersEnum.both)
            {
                senders
                    = isInitiator
                        ? SendersEnum.initiator
                        : SendersEnum.responder;
            }
            else
            {
                senders = SendersEnum.none;
            }
        }

        ext.setSenders(senders);
        ext.setCreator(remoteContent.getCreator());
        ext.setName(remoteContent.getName());

        JingleIQ contentIQ = JinglePacketFactory
            .createContentModify(getProtocolProvider().getOurJID(),
                            this.peerJID, getJingleSID(), ext);

        getProtocolProvider().getConnection().sendPacket(contentIQ);

        try
        {
            getMediaHandler().reinitContent(remoteContent.getName(), senders);
        }
        catch(Exception e)
        {
            logger.warn("Exception occurred when media reinitialization", e);
        }
     }

    /**
     * Processes the content-add {@link JingleIQ}.
     *
     * @param content The {@link JingleIQ} that contains content that remote
     * peer wants to be added
     */
    public void processContentAdd(JingleIQ content)
    {
        CallPeerMediaHandlerJabberImpl mediaHandler = getMediaHandler();
        List<ContentPacketExtension> contents = content.getContentList();
        Iterable<ContentPacketExtension> answerContents;
        JingleIQ contentIQ;

        try
        {
            mediaHandler.processOffer(contents);
            answerContents = mediaHandler.generateSessionAccept();
            contentIQ = null;
        }
        catch(Exception e)
        {
            logger.warn("Exception occurred", e);

            answerContents = null;
            contentIQ
                = JinglePacketFactory.createContentReject(
                        getProtocolProvider().getOurJID(),
                        this.peerJID,
                        getJingleSID(),
                        answerContents);
        }

        if(contentIQ == null)
        {
            /* send content-accept */
            contentIQ
                = JinglePacketFactory.createContentAccept(
                        getProtocolProvider().getOurJID(),
                        this.peerJID,
                        getJingleSID(),
                        answerContents);
        }

        getProtocolProvider().getConnection().sendPacket(contentIQ);
        mediaHandler.start();
    }

    /**
     * Processes the content-accept {@link JingleIQ}.
     *
     * @param content The {@link JingleIQ} that contains content that remote
     * peer has accepted
     */
    public void processContentAccept(JingleIQ content)
    {
        List<ContentPacketExtension> contents = content.getContentList();

        try
        {
            getMediaHandler().processAnswer(contents);
        }
        catch(Exception exc)
        {
            logger.warn("Failed to process a content-accept", exc);
            //send an error response;
            JingleIQ errResp = JinglePacketFactory.createSessionTerminate(
                    sessionInitIQ.getTo(), sessionInitIQ.getFrom(),
                    sessionInitIQ.getSID(), Reason.INCOMPATIBLE_PARAMETERS,
                    "Error: " + exc.getMessage());

            setState(CallPeerState.FAILED, "Error: " + exc.getMessage());
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        getMediaHandler().start();
    }

    /**
     * Processes the content-modify {@link JingleIQ}.
     *
     * @param content The {@link JingleIQ} that contains content that remote
     * peer wants to be modified
     */
    public void processContentModify(JingleIQ content)
    {
        ContentPacketExtension ext = content.getContentList().get(0);
        SendersEnum senders = ext.getSenders();

        try
        {
            getMediaHandler().reinitContent(ext.getName(), senders);
        }
        catch(Exception exc)
        {
            logger.info("Failed to process an incoming content-modify", exc);

            //send an error response;
            JingleIQ errResp = JinglePacketFactory.createSessionTerminate(
                    sessionInitIQ.getTo(), sessionInitIQ.getFrom(),
                    sessionInitIQ.getSID(), Reason.INCOMPATIBLE_PARAMETERS,
                    "Error: " + exc.getMessage());

            setState(CallPeerState.FAILED, "Error: " + exc.getMessage());
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }
    }

    /**
     * Processes the content-remove {@link JingleIQ}.
     *
     * @param content The {@link JingleIQ} that contains content that remote
     * peer wants to be removed
     */
    public void processContentRemove(JingleIQ content)
    {
        List<ContentPacketExtension> contents = content.getContentList();

        if (!contents.isEmpty())
        {
            CallPeerMediaHandlerJabberImpl mediaHandler = getMediaHandler();

            for(ContentPacketExtension c : contents)
                mediaHandler.removeContent(c.getName());

            /*
             * TODO XEP-0166: Jingle says: If the content-remove results in zero
             * content definitions for the session, the entity that receives the
             * content-remove SHOULD send a session-terminate action to the
             * other party (since a session with no content definitions is
             * void).
             */
        }
    }

    /**
     * Processes the content-reject {@link JingleIQ}.
     *
     * @param content The {@link JingleIQ}
     */
    public void processContentReject(JingleIQ content)
    {
        if(content.getContentList().isEmpty())
        {
            //send an error response;
            JingleIQ errResp = JinglePacketFactory.createSessionTerminate(
                sessionInitIQ.getTo(), sessionInitIQ.getFrom(),
                sessionInitIQ.getSID(), Reason.INCOMPATIBLE_PARAMETERS,
                "Error: content rejected");

            setState(CallPeerState.FAILED, "Error: content rejected");
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }
    }

    /**
     * Processes the <tt>transport-info</tt> {@link JingleIQ}.
     *
     * @param jingleIQ the <tt>transport-info</tt> {@link JingleIQ} to process
     */
    public void processTransportInfo(JingleIQ jingleIQ)
    {
        /*
         * The transport-info action is used to exchange transport candidates so
         * it only concerns the mediaHandler.
         */
        try
        {
            getMediaHandler().processTransportInfo(jingleIQ.getContentList());
        }
        catch (OperationFailedException ofe)
        {
            logger.warn("Failed to process an incoming transport-info", ofe);

            //send an error response
            String reasonText = "Error: " + ofe.getMessage();
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getSID(),
                        Reason.GENERAL_ERROR,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }
    }

    /**
     * Sends local candidate addresses from the local peer to the remote peer
     * using the <tt>transport-info</tt> {@link JingleIQ}.
     *
     * @param contents the local candidate addresses to be sent from the local
     * peer to the remote peer using the <tt>transport-info</tt>
     * {@link JingleIQ}
     */
    protected void sendTransportInfo(Iterable<ContentPacketExtension> contents)
    {
        JingleIQ transportInfo = new JingleIQ();

        for (ContentPacketExtension content : contents)
            transportInfo.addContent(content);

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();

        transportInfo.setAction(JingleAction.TRANSPORT_INFO);
        transportInfo.setFrom(protocolProvider.getOurJID());
        transportInfo.setSID(getJingleSID());
        transportInfo.setTo(getAddress());
        transportInfo.setType(IQ.Type.SET);

        protocolProvider.getConnection().sendPacket(transportInfo);
    }

    /**
     * Transfer (in the sense of call transfer) this <tt>CallPeer</tt> to a
     * specific callee address which may optionally be participating in an
     * active <tt>Call</tt>.
     *
     * @param to the address of the callee to transfer this <tt>CallPeer</tt> to
     * @param sid the Jingle session ID of the active <tt>Call</tt> between the
     * local peer and the callee in the case of attended transfer; <tt>null</tt>
     * in the case of unattended transfer
     * @throws OperationFailedException if something goes wrong
     */
    protected void transfer(String to, String sid)
        throws OperationFailedException
    {
        JingleIQ transferSessionInfo = new JingleIQ();
        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();

        transferSessionInfo.setAction(JingleAction.SESSION_INFO);
        transferSessionInfo.setFrom(protocolProvider.getOurJID());
        transferSessionInfo.setSID(getJingleSID());
        transferSessionInfo.setTo(getAddress());
        transferSessionInfo.setType(IQ.Type.SET);

        TransferPacketExtension transfer = new TransferPacketExtension();

        if (sid != null)
        {
            /*
             * Not really sure what the value of the "from" attribute of the
             * "transfer" element should be but the examples in "XEP-0251:
             * Jingle Session Transfer" has it in the case of attended transfer.
             */
            transfer.setFrom(protocolProvider.getOurJID());
            transfer.setSID(sid);
        }
        transfer.setTo(to);

        transferSessionInfo.addExtension(transfer);

        protocolProvider.getConnection().sendPacket(transferSessionInfo);
    }
}
