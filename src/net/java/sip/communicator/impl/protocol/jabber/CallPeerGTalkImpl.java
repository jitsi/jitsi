/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.lang.reflect.*;
import java.util.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a Google Talk <tt>CallPeer</tt>.
 *
 * @author Sebastien Vincent
 */
public class CallPeerGTalkImpl
    extends MediaAwareCallPeer<CallGTalkImpl,
        CallPeerMediaHandlerGTalkImpl,
        ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerGTalkImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerGTalkImpl.class);

    /**
     * The {@link SessionIQ} that created the session that this call represents.
     */
    private SessionIQ sessionInitIQ = null;

    /**
     * The jabber address of this peer
     */
    private String peerJID = null;

    /**
     * Indicates whether this peer was the one that initiated the session.
     */
    protected boolean isInitiator = false;

    /**
     * Session ID.
     */
    private String sid = null;

    /**
     * Temporary variable for handling client like FreeSwitch that sends
     * "accept" message before sending candidates.
     */
    private SessionIQ freeswitchSession = null;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Google Talk address of the new call peer.
     * @param owningCall the call that contains this call peer.
     */
    public CallPeerGTalkImpl(String peerAddress, CallGTalkImpl owningCall)
    {
        super(owningCall);
        this.peerJID = peerAddress;
        setMediaHandler( new CallPeerMediaHandlerGTalkImpl(this) );
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
     * Processes the session initiation {@link SessionIQ} that we were created
     * with, passing its content to the media handler and then sends either a
     * "session-info/ringing" or a "terminate" response.
     *
     * @param sessionInitIQ The {@link SessionIQ} that created the session that
     * we are handling here.
     */
    protected synchronized void processSessionInitiate(SessionIQ sessionInitIQ)
    {
        // Do initiate the session.
        this.sessionInitIQ = sessionInitIQ;
        this.isInitiator = true;

        RtpDescriptionPacketExtension description = null;

        for(PacketExtension ext : sessionInitIQ.getExtensions())
        {
            if(ext.getElementName().equals(
                    RtpDescriptionPacketExtension.ELEMENT_NAME))
            {
                description = (RtpDescriptionPacketExtension)ext;
                break;
            }
        }

        if(description == null)
        {
            logger.info("No description in incoming session initiate");

            //send an error response;
            String reasonText = "Error: no description";
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.INCOMPATIBLE_PARAMETERS,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        try
        {
            getMediaHandler().processOffer(description);
        }
        catch(Exception ex)
        {
            logger.info("Failed to process an incoming session initiate", ex);

            //send an error response;
            String reasonText = "Error: " + ex.getMessage();
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.INCOMPATIBLE_PARAMETERS,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }
    }

    /**
     * Initiate a Google Talk session {@link SessionIQ}.
     *
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>initiate</tt>
     * {@link SessionIQ} which is to initiate the session with this
     * <tt>CallPeerGTalkImpl</tt>
     * @throws OperationFailedException exception
     */
    protected synchronized void initiateSession(
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        sid = SessionIQ.generateSID();
        isInitiator = false;

        //Create the media description that we'd like to send to the other side.
        RtpDescriptionPacketExtension offer
            = getMediaHandler().createDescription();

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();

        sessionInitIQ
            = GTalkPacketFactory.createSessionInitiate(
                    protocolProvider.getOurJID(),
                    this.peerJID,
                    sid,
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

        getMediaHandler().harvestCandidates(offer.getPayloadTypes(),
                new CandidatesSender()
        {
            public void sendCandidates(
                    Iterable<GTalkCandidatePacketExtension> candidates)
            {
                CallPeerGTalkImpl.this.sendCandidates(candidates);
            }
        });
    }

    /**
     * Puts this peer into a {@link CallPeerState#DISCONNECTED}, indicating a
     * reason to the user, if there is one.
     *
     * @param sessionIQ the {@link SessionIQ} that's terminating our session.
     */
    public void processSessionReject(SessionIQ sessionIQ)
    {
        processSessionTerminate(sessionIQ);
    }

    /**
     * Puts this peer into a {@link CallPeerState#DISCONNECTED}, indicating a
     * reason to the user, if there is one.
     *
     * @param sessionIQ the {@link SessionIQ} that's terminating our session.
     */
    public void processSessionTerminate(SessionIQ sessionIQ)
    {
        String reasonStr = "Call ended by remote side.";
        ReasonPacketExtension reasonExt = sessionIQ.getReason();

        if(reasonStr != null && reasonExt != null)
        {
            Reason reason = reasonExt.getReason();

            if(reason != null)
                reasonStr += " Reason: " + reason.toString() + ".";

            String text = reasonExt.getText();

            if(text != null)
                reasonStr += " " + text;
        }

        getMediaHandler().getTransportManager().close();
        setState(CallPeerState.DISCONNECTED, reasonStr);
    }

    /**
     * Processes the session initiation {@link SessionIQ} that we were created
     * with, passing its content to the media handler.
     *
     * @param sessionInitIQ The {@link SessionIQ} that created the session that
     * we are handling here.
     */
    public void processSessionAccept(SessionIQ sessionInitIQ)
    {
        this.sessionInitIQ = sessionInitIQ;

        CallPeerMediaHandlerGTalkImpl mediaHandler = getMediaHandler();
        Collection<PacketExtension> extensions =
            sessionInitIQ.getExtensions();
        RtpDescriptionPacketExtension answer = null;

        for(PacketExtension ext : extensions)
        {
            if(ext.getElementName().equalsIgnoreCase(
                    RtpDescriptionPacketExtension.ELEMENT_NAME))
            {
                answer = (RtpDescriptionPacketExtension)ext;
                break;
            }
        }

        try
        {
            mediaHandler.getTransportManager().
                wrapupConnectivityEstablishment();
            mediaHandler.processAnswer(answer);
        }
        catch(IllegalArgumentException e)
        {
            // HACK for FreeSwitch that send accept message before sending
            // candidates
            freeswitchSession = sessionInitIQ;
        }
        catch(Exception exc)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to process a session-accept", exc);

            //send an error response
            String reasonText = "Error: " + exc.getMessage();
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.GENERAL_ERROR,
                        reasonText);

            getMediaHandler().getTransportManager().close();
            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        //tell everyone we are connecting so that the audio notifications would
        //stop
        setState(CallPeerState.CONNECTED);

        mediaHandler.start();
    }

    /**
     * Process candidates received.
     *
     * @param sessionInitIQ The {@link SessionIQ} that created the session we
     * are handling here
     */
    public void processCandidates(SessionIQ sessionInitIQ)
    {
        Collection<PacketExtension> extensions = sessionInitIQ.getExtensions();
        List<GTalkCandidatePacketExtension> candidates =
            new ArrayList<GTalkCandidatePacketExtension>();

        for(PacketExtension ext : extensions)
        {
            if(ext.getElementName().equalsIgnoreCase(
                    GTalkCandidatePacketExtension.ELEMENT_NAME))
            {
                GTalkCandidatePacketExtension cand =
                    (GTalkCandidatePacketExtension)ext;
                candidates.add(cand);
            }
        }

        try
        {
            getMediaHandler().processCandidates(candidates);
        }
        catch (OperationFailedException ofe)
        {
            logger.warn("Failed to process an incoming candidates", ofe);

            //send an error response
            String reasonText = "Error: " + ofe.getMessage();
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.GENERAL_ERROR,
                        reasonText);

            getMediaHandler().getTransportManager().close();
            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        // HACK for FreeSwitch that send accept message before sending
        // candidates
        if(freeswitchSession != null)
        {
            if(!isInitiator)
                processSessionAccept(freeswitchSession);
            else
            {
                try
                {
                    answer();
                }
                catch(OperationFailedException e)
                {
                    logger.info("Failed to answer call (FreeSwitch hack)");
                }
            }
            freeswitchSession = null;
        }
    }

    /**
     * Returns the session ID of the Jingle session associated with this call.
     *
     * @return the session ID of the Jingle session associated with this call.
     */
    public String getSessionID()
    {
        return sessionInitIQ != null ? sessionInitIQ.getID() : sid;
    }

    /**
     * Returns the IQ ID of the Jingle session-initiate packet associated with
     * this call.
     *
     * @return the IQ ID of the Jingle session-initiate packet associated with
     * this call.
     */
    public String getSessInitID()
    {
        return sessionInitIQ != null ? sessionInitIQ.getPacketID() : null;
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
        getMediaHandler().getTransportManager().close();
        setState(CallPeerState.DISCONNECTED);
        SessionIQ responseIQ = null;

        if (prevPeerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(prevPeerState))
        {
            responseIQ = GTalkPacketFactory.createBye(
                getProtocolProvider().getOurJID(), peerJID, getSessionID());
            responseIQ.setInitiator(isInitiator() ? getAddress() :
                getProtocolProvider().getOurJID());
        }
        else if (CallPeerState.CONNECTING.equals(prevPeerState)
            || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(prevPeerState)
            || CallPeerState.ALERTING_REMOTE_SIDE.equals(prevPeerState))
        {
            responseIQ = GTalkPacketFactory.createCancel(
                getProtocolProvider().getOurJID(), peerJID, getSessionID());
            responseIQ.setInitiator(isInitiator() ? getAddress() :
                getProtocolProvider().getOurJID());
        }
        else if (prevPeerState.equals(CallPeerState.INCOMING_CALL))
        {
            responseIQ = GTalkPacketFactory.createBusy(
                getProtocolProvider().getOurJID(), peerJID, getSessionID());
            responseIQ.setInitiator(isInitiator() ? getAddress() :
                getProtocolProvider().getOurJID());
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
        RtpDescriptionPacketExtension answer = null;

        try
        {
            getMediaHandler().getTransportManager().
                wrapupConnectivityEstablishment();
            answer = getMediaHandler().generateSessionAccept(true);
        }
        catch(IllegalArgumentException e)
        {
            freeswitchSession = new SessionIQ();

            // HACK apparently FreeSwitch need to have accept before
            answer = getMediaHandler().generateSessionAccept(false);

            SessionIQ response
                = GTalkPacketFactory.createSessionAccept(
                    sessionInitIQ.getTo(),
                    sessionInitIQ.getFrom(),
                    getSessionID(),
                    answer);

            getProtocolProvider().getConnection().sendPacket(response);
            return;
        }
        catch(Exception exc)
        {
            logger.info("Failed to answer an incoming call", exc);

            //send an error response
            String reasonText = "Error: " + exc.getMessage();
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.FAILED_APPLICATION,
                        reasonText);

            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        SessionIQ response
            = GTalkPacketFactory.createSessionAccept(
                    sessionInitIQ.getTo(),
                    sessionInitIQ.getFrom(),
                    getSessionID(),
                    answer);

        //send the packet first and start the stream later  in case the media
        //relay needs to see it before letting hole punching techniques through.
        if(freeswitchSession == null)
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
            SessionIQ errResp
                = GTalkPacketFactory.createSessionTerminate(
                        sessionInitIQ.getTo(),
                        sessionInitIQ.getFrom(),
                        sessionInitIQ.getID(),
                        Reason.GENERAL_ERROR,
                        reasonText);

            getMediaHandler().getTransportManager().close();
            setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);
            return;
        }

        //tell everyone we are connecting so that the audio notifications would
        //stop
        setState(CallPeerState.CONNECTED);
    }

    /**
     * Returns whether or not the <tt>CallPeer</tt> is an Android phone or
     * a call pass throught Google Voice.
     *
     * We base the detection of the JID's resource which in the case of Android
     * is androidXXXXXXX (where XXXXXX is a suite of numbers/letters).
     */
    private static boolean isAndroid(String fullJID)
    {
        int idx = fullJID.indexOf('/');

        if(idx != -1)
        {
            String res = fullJID.substring(idx + 1);
            if(res.startsWith("android"))
            {
                return true;
            }
        }

        if(fullJID.contains("@voice.google.com"))
            return true;

        return false;
    }

    /**
     * Sends local candidate addresses from the local peer to the remote peer
     * using the <tt>candidates</tt> {@link SessionIQ}.
     *
     * @param candidates the local candidate addresses to be sent from the local
     * peer to the remote peer using the <tt>candidates</tt>
     * {@link SessionIQ}
     */
    protected void sendCandidates(
            Iterable<GTalkCandidatePacketExtension> candidates)
    {
        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();

        SessionIQ candidatesIQ = new SessionIQ();

        candidatesIQ.setGTalkType(GTalkType.CANDIDATES);
        candidatesIQ.setFrom(protocolProvider.getOurJID());
        candidatesIQ.setInitiator(isInitiator() ? getAddress() :
            protocolProvider.getOurJID());
        candidatesIQ.setID(getSessionID());
        candidatesIQ.setTo(getAddress());
        candidatesIQ.setType(IQ.Type.SET);

        for (GTalkCandidatePacketExtension candidate : candidates)
        {
            // Android phone does not seems to like IPv6 candidates since it
            // reject the IQ candidates with an error
            // so do not send IPv6 candidates to Android phone
            if(isAndroid(getAddress()) &&
                NetworkUtils.isIPv6Address(candidate.getAddress()))
                continue;

            candidatesIQ.addExtension(candidate);
        }

        protocolProvider.getConnection().sendPacket(candidatesIQ);
    }
}
