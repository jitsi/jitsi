/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Our SIP implementation of the default CallPeer;
 *
 * @author Emil Ivov
 */
public class CallPeerSipImpl
    extends AbstractCallPeer
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerSipImpl.class);

    /**
     * The sip address of this peer
     */
    private Address peerAddress = null;

    /**
     * A byte array containing the image/photo representing the call peer.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    /**
     * The call this peer belongs to.
     */
    private CallSipImpl call;

    /**
     * The JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    private Dialog jainSipDialog = null;

    /**
     * The SDP session description that we have received from this call
     * peer.
     */
    private String sdpDescription = null;

    /**
     * The SIP transaction that established this call. This was previously kept
     * in the jain-sip dialog but got deprected there so we're now keeping it
     * here.
     */
    private Transaction latestInviteTransaction = null;

    /**
     * The jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     */
    private SipProvider jainSipProvider = null;

    /**
     * The transport address that we are using to address the peer or the
     * first one that we'll try when we next send them a message (could be the
     * address of our sip registrar).
     */
    private InetSocketAddress transportAddress = null;

    /**
     * A URL pointing to a location with call information or a call control
     * web interface related to this peer.
     */
    private URL callControlURL = null;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * A reference to the <tt>OperationSetBasicTelephonySipImpl</tt> that
     * created us;
     */
    private final OperationSetBasicTelephonySipImpl parentOpSet;

    /**
     * The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
     * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
     * both classes are only separated for reasons of readability.
     */
    private final CallPeerMediaHandler mediaHandler;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the JAIN SIP <tt>Address</tt> of the new call peer.
     * @param owningCall the call that contains this call peer.
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     */
    public CallPeerSipImpl(Address     peerAddress,
                           CallSipImpl owningCall,
                           Transaction containingTransaction,
                           SipProvider sourceProvider)
    {
        this.peerAddress = peerAddress;
        this.call = owningCall;
        this.parentOpSet = owningCall.getParentOperationSet();
        this.messageFactory = getProtocolProvider().getMessageFactory();

        this.mediaHandler = new CallPeerMediaHandler(this);

        setDialog(containingTransaction.getDialog());
        setLatestInviteTransaction(containingTransaction);
        setJainSipProvider(sourceProvider);

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return this.getPeerAddress().getURI().toString();
    }

    /**
     * Returns the address of the remote party (making sure that it corresponds
     * to the latest address we've received) and caches it.
     *
     * @return the most recent <tt>javax.sip.address.Address</tt> that we have
     * for the remote party.
     */
    public Address getPeerAddress()
    {
        if (getDialog() != null
            && getDialog().getRemoteParty() != null)
        {
            //update the address we've cached.
            peerAddress = getDialog().getRemoteParty();
        }

        return peerAddress;
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        String displayName = getPeerAddress().getDisplayName();
        return (displayName == null)
                    ? getPeerAddress().getURI().toString()
                    : displayName;
    }

    /**
     * Sets a human readable name representing this peer.
     *
     * @param displayName the peer's display name
     */
    protected void setDisplayName(String displayName)
    {
        String oldName = getDisplayName();
        try
        {
            this.peerAddress.setDisplayName(displayName);
        }
        catch (ParseException ex)
        {
            //couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_DISPLAY_NAME_CHANGE,
                oldName,
                displayName);
    }

    /**
     * The method returns an image representation of the call peer if one is
     * available.
     *
     * @return byte[] a byte array containing the image or null if no image is
     * available.
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call peer.
     *
     * @param image a byte array containing the image
     */
    protected void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_IMAGE_CHANGE,
                oldImage,
                image);
    }

    /**
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Returns the latest sdp description that this peer sent us.
     * @return the latest sdp description that this peer sent us.
     */
    public String getSdpDescription()
    {
        return sdpDescription;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * CallPeer.
     * @param peerID the ID of this call peer.
     */
    protected void setPeerID(String peerID)
    {
        this.peerID = peerID;
    }

    /**
     * Returns a reference to the call that this peer belongs to. Calls
     * are created by underlying telephony protocol implementations.
     *
     * @return a reference to the call containing this peer.
     */
    public CallSipImpl getCall()
    {
        return call;
    }

    /**
     * Sets the call containing this peer.
     * @param call the call that this call peer is
     * partdicipating in.
     */
    protected void setCall(CallSipImpl call)
    {
        this.call = call;
    }

    /**
     * Sets the sdp description for this call peer.
     *
     * @param sdpDescription the sdp description for this call peer.
     */
    public void setSdpDescription(String sdpDescription)
    {
        this.sdpDescription = sdpDescription;
    }

    /**
     * Sets the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     * @param dialog the JAIN SIP dialog that has been created by the
     * application for this call.
     */
    public void setDialog(Dialog dialog)
    {
        this.jainSipDialog = dialog;
    }

    /**
     * Returns the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     *
     * @return the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    public Dialog getDialog()
    {
        return jainSipDialog;
    }

    /**
     * Sets the transaction instance that contains the INVITE which started
     * this call.
     *
     * @param transaction the Transaction that initiated this call.
     */
    public void setLatestInviteTransaction(Transaction transaction)
    {
        this.latestInviteTransaction = transaction;
    }

    /**
     * Returns the transaction instance that contains the INVITE which started
     * this call.
     *
     * @return the Transaction that initiated this call.
     */
    public Transaction getLatestInviteTransaction()
    {
        return latestInviteTransaction;
    }

    /**
     * Sets the jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     *
     * @param jainSipProvider the <tt>SipProvider</tt> that serves this call
     * peer.
     */
    public void setJainSipProvider(SipProvider jainSipProvider)
    {
        this.jainSipProvider = jainSipProvider;
    }

    /**
     * Returns the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     *
     * @return the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     */
    public SipProvider getJainSipProvider()
    {
        return jainSipProvider;
    }

    /**
     * The address that we have used to contact this peer. In cases
     * where no direct connection has been established with the peer,
     * this method will return the address that will be first tried when
     * connection is established (often the one used to connect with the
     * protocol server). The address may change during a session and
     *
     * @param transportAddress The address that we have used to contact this
     * peer.
     */
    public void setTransportAddress(InetSocketAddress transportAddress)
    {
        InetSocketAddress oldTransportAddress = this.transportAddress;
        this.transportAddress = transportAddress;

        this.fireCallPeerChangeEvent(
            CallPeerChangeEvent.CALL_PEER_TRANSPORT_ADDRESS_CHANGE,
                oldTransportAddress, transportAddress);
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that this
     * peer belongs to.
     */
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return this.getCall().getProtocolProvider();
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
        ProtocolProviderService pps = call.getProtocolProvider();
        OperationSetPresenceSipImpl opSetPresence
            = (OperationSetPresenceSipImpl) pps
                .getOperationSet(OperationSetPresence.class);

        return opSetPresence.resolveContactID(getAddress());
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this peer or <tt>null</tt> if no such URL is available for this
     * call peer.
     *
     * @return a URL link to a location with call information or a call control
     * web interface related to this peer or <tt>null</tt> if no such URL
     * is available.
     */
    public URL getCallInfoURL()
    {
        return this.callControlURL;
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this peer.
     *
     * @param callControlURL a URL link to a location with call information or
     * a call control web interface related to this peer.
     */
    public void setCallInfoURL(URL callControlURL)
    {
        this.callControlURL = callControlURL;
    }

    /**
     * Determines whether the audio stream (if any) being sent to this
     * peer is mute.
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         peer and it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        return getMediaHandler().isMute();
    }

    /**
     * Sets the security status to ON for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the cipher
     * @param securityString the SAS
     * @param isVerified indicates if the SAS has been verified
     */
    public void securityOn(  int sessionType,
                                String cipher,
                                String securityString,
                                boolean isVerified)
    {
        fireCallPeerSecurityOnEvent(
                        sessionType, cipher, securityString, isVerified);
    }

    /**
     * Sets the security status to OFF for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityOff(int sessionType)
    {
        fireCallPeerSecurityOffEvent(sessionType);
    }

    /**
     * Sets the security message associated with a failure/warning or
     * information coming from the encryption protocol.
     *
     * @param messageType the type of the message.
     * @param i18nMessage the message
     * @param severity severity level
     */
    public void securityMessage( String messageType,
                                    String i18nMessage,
                                    int severity)
    {
        fireCallPeerSecurityMessageEvent(messageType,
                                         i18nMessage,
                                         severity);
    }

    /**
     * Reinitializes the media session of the <tt>CallPeer</tt> that this
     * INVITE request is destined to.
     *
     * @param serverTransaction a reference to the {@link ServerTransaction}
     * that contains the reINVITE request.
     */
    public void processReInvite(ServerTransaction serverTransaction)
    {
        Request invite = serverTransaction.getRequest();

        setLatestInviteTransaction(serverTransaction);

        // SDP description may be in ACKs - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            setSdpDescription(new String(invite.getRawContent()));
        }

        Response response = null;
        try
        {
            response = messageFactory.createResponse(Response.OK, invite);
            attachSdpAnswer(response);

            logger.trace("will send an OK response: ");
            serverTransaction.sendResponse(response);
            logger.debug("sent a an OK response: "+ response);
        }
        catch (Exception ex)//no need to distinguish among exceptions.
        {
            logger.error("Error while trying to send a response", ex);
            setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            getProtocolProvider().sayErrorSilently(
                            serverTransaction, Response.SERVER_INTERNAL_ERROR);
            return;
        }

        try
        {
            updateMediaFlags();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Error after sending response " + response, ex);
        }
    }

    /**
     * Creates an SDP description that could be sent to <tt>peer</tt> and adds
     * it to <tt>response</tt>. Provides a hook for this instance to take last
     * configuration steps on a specific <tt>Response</tt> before it is sent to
     * a specific <tt>CallPeer</tt> as part of the execution of.
     *
     * @param response the <tt>Response</tt> to be sent to the <tt>peer</tt>
     *
     * @throws OperationFailedException if we fail parsing call peer's media.
     * @throws ParseException if we try to attach invalid SDP to response.
     */
    private void attachSdpAnswer(Response response)
        throws OperationFailedException, ParseException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */
        /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        String sdpAnswer = null;
        try
        {
            sdpAnswer = callSession.processSdpOffer(this, getSdpDescription());
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        response.setContent(
            sdpAnswer,
            getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
                */
    }

    /**
     * Updates the media flags for this peer according to the value of the SDP
     * field.
     *
     * @throws OperationFailedException if we fail parsing callPeer's media.
     */
    private void updateMediaFlags()
        throws OperationFailedException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */
        /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        int mediaFlags = 0;
        try
        {
            mediaFlags = callSession.getSdpOfferMediaFlags(getSdpDescription());
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        */
        /*
         * Comply with the request of the SDP offer with respect to putting on
         * hold.
         */
        /**
         * @todo update to neomedia.
        boolean on = ((mediaFlags & CallSession.ON_HOLD_REMOTELY) != 0);

        callSession.putOnHold(on, false);

        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (on)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                setState(CallPeerState.ON_HOLD_LOCALLY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!on)
                setState(CallPeerState.CONNECTED);
        }
        else if (on)
        {
            setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        */
        /*
         * Reflect the request of the SDP offer with respect to the modification
         * of the availability of media.
         */
        /**
         * @todo update to neomedia.
        callSession.setReceiveStreaming(mediaFlags);
        */
    }

    /**
     * Sets the state of the corresponding call peer to DISCONNECTED and
     * sends an OK response.
     *
     * @param byeTran the ServerTransaction the the BYE request arrived in.
     */
    public void processBye(ServerTransaction byeTran)
    {
        Request byeRequest = byeTran.getRequest();
        // Send OK
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(Response.OK, byeRequest);
        }
        catch (ParseException ex)
        {
            logger.error("Error while trying to send a response to a bye", ex);
            // no need to let the user know about the error since it doesn't
            // affect them
            return;
        }

        try
        {
            byeTran.sendResponse(ok);
            logger.debug("sent response " + ok);
        }
        catch (Exception ex)
        {
            // This is not really a problem according to the RFC
            // so just dump to stdout should someone be interested
            logger.error("Failed to send an OK response to BYE request,"
                + "exception was:\n", ex);
        }

        // change status
        boolean dialogIsAlive;
        try
        {
            dialogIsAlive = EventPackageUtils.processByeThenIsDialogAlive(
                            byeTran.getDialog());
        }
        catch (SipException ex)
        {
            dialogIsAlive = false;

            logger.error(
                "Failed to determine whether the dialog should stay alive.",ex);
        }

        //if the Dialog is still alive (i.e. we are in the middle of a xfer)
        //then only stop streaming, otherwise Disconnect.

        /**
         * @todo update to neomedia.
         *
        if (dialogIsAlive)
        {
            getMediaCallSession().stopStreaming();
        }
        else
        {
            setState(CallPeerState.DISCONNECTED);
        }
        */
    }

    /**
     * Sets the state of the specifies call peer as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     */
    public void processCancel(ServerTransaction serverTransaction)
    {
        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        Request cancel = serverTransaction.getRequest();
        try
        {
            Response ok = messageFactory.createResponse(Response.OK, cancel);
            serverTransaction.sendResponse(ok);

            logger.debug("sent an ok response to a CANCEL request:\n" + ok);
        }
        catch (ParseException ex)
        {
            logAndFail("Failed to create an OK Response to a CANCEL.", ex);
            return;
        }
        catch (Exception ex)
        {
            logAndFail("Failed to send an OK Response to a CANCEL.", ex);
            return;
        }

        try
        {
            // stop the invite transaction as well
            Transaction tran = getLatestInviteTransaction();
            // should be server transaction and misplaced cancels should be
            // filtered by the stack but it doesn't hurt checking anyway
            if (!(tran instanceof ServerTransaction))
            {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = getLatestInviteTransaction().getRequest();
            Response requestTerminated = messageFactory
                .createResponse(Response.REQUEST_TERMINATED, invite);

            inviteTran.sendResponse(requestTerminated);
            if (logger.isDebugEnabled())
                logger.debug("sent request terminated response:\n"
                    + requestTerminated);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create a REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send an REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }

        // change status
        setState(CallPeerState.DISCONNECTED);
    }

    /**
     * Sets the mute property for this call peer.
     *
     * @param newMuteValue the new value of the mute property for this call peer
     */
    public void setMute(boolean newMuteValue)
    {
        /**
         * @todo update to neomedia.
        getMediaCallSession().setMute(newMuteValue);
        super.setMute(newMuteValue);
        */
    }

    /**
     * Logs <tt>message</tt> and <tt>cause</tt> and sets this <tt>peer</tt>'s
     * state to <tt>CallPeerState.FAILED</tt>
     *
     * @param message a message to log and display to the user.
     * @param throwable the exception that cause the error we are logging
     */
    public void logAndFail(String message, Throwable throwable)
    {
        logger.error(message, throwable);
        setState(CallPeerState.FAILED, message);
    }

    /**
     * Updates the session description and sends the state of the corresponding
     * call peer to CONNECTED.
     *
     * @param serverTransaction the transaction that the ACK was received in.
     * @param ack the ACK <tt>Request</tt> we need to process
     */
    public void processAck(ServerTransaction serverTransaction, Request ack)
    {
        ContentLengthHeader contentLength = ack.getContentLength();
        if ((contentLength != null) && (contentLength.getContentLength() > 0))
        {
            setSdpDescription( new String(ack.getRawContent()));
        }

        // change status
        CallPeerState peerState = getState();
        if (!CallPeerState.isOnHold(peerState))
        {
            if (CallPeerState.CONNECTED.equals(peerState))
            {
                /**
                 * @todo update to neomedia.
                try
                {
                    getMediaCallSession().startStreamingAndProcessingMedia();
                }
                catch (MediaException ex)
                {
                    logger.error( "Failed to start the streaming"
                            + " and the processing of the media", ex);
                }
                */
            }
            else
                setState(CallPeerState.CONNECTED);
        }
    }

    /**
     * Handles early media in 183 Session Progress responses. Retrieves the SDP
     * and makes sure that we start transmitting and playing early media that we
     * receive. Puts the call into a CONNECTING_WITH_EARLY_MEDIA state.
     *
     * @param tran the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param response the 183 <tt>Response</tt> to process
     */
    public void processSessionProgress(ClientTransaction tran,
                                       Response          response)
    {
        if (response.getContentLength().getContentLength() == 0)
        {
            logger.debug("Ignoring a 183 with no content");
            return;
        }

        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) response
                .getHeader(ContentTypeHeader.NAME);

        if (!contentTypeHeader.getContentType().equalsIgnoreCase("application")
            || !contentTypeHeader.getContentSubType().equalsIgnoreCase("sdp"))
        {
            //This can happen when receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        // set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        setSdpDescription(new String(response.getRawContent()));

        // notify the media manager of the sdp content
        /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        if (callSession == null)
        {
            // unlikely to happen because it would mean we didn't send an offer
            // in the invite and we always send one.
            logger.warn("Could not find call session.");
            return;
        }

        try
        {
            callSession.processSdpAnswer(this, getSdpDescription());
        }
        catch (ParseException exc)
        {
            logAndFail("There was an error parsing the SDP description of "
                + getDisplayName() + "(" + getAddress() + ")", exc);
            return;
        }
        catch (MediaException exc)
        {
            logAndFail("We failed to process the SDP description of "
                + getDisplayName() + "(" + getAddress() + ")" + ". Error was: "
                + exc.getMessage(), exc);
            return;
        }

        // set the call url in case there was one
        setCallInfoURL(callSession.getCallInfoURL());

        // change status
        setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
        */
    }

    /**
     * Sets our state to CONNECTED, sends an ACK and processes the SDP
     * description in the <tt>ok</tt> <tt>Response</tt>.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    public void processInviteOK(ClientTransaction clientTransaction,
                                 Response         ok)
    {
        try
        {
            // Send the ACK. Do it now since we already got all the info we need
            // and processSdpAnswer() can take a while (patch by Michael Koch)
            getProtocolProvider().sendAck(clientTransaction);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            logAndFail("Error creating an ACK (CSeq?)", ex);
            return;
        }
        catch (SipException ex)
        {
            logAndFail("Failed to create ACK request!", ex);
            return;
        }

        // !!! set SDP content before setting call state as that is where
        // listeners get alerted and they need the SDP
        // ignore SDP if we've just had one in early media
        if(!CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState()))
        {
            setSdpDescription(new String(ok.getRawContent()));
        }

        // notify the media manager of the sdp content
        /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        try
        {
             //Process SDP unless we've just had an answer in a 18X response
            if (!CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState()))
            {
                callSession.processSdpAnswer(this, getSdpDescription());
            }

            // set the call url in case there was one
            setCallInfoURL(callSession.getCallInfoURL());
        }
        //at this point we have already sent our ack so in addition to logging
        //an error we also need to hangup the call peer.
        catch (Exception exc)//Media or parse exception.
        {
            logger.error("There was an error parsing the SDP description of "
                + getDisplayName() + "(" + getAddress() + ")", exc);
            try
            {
                //we are connected from a SIP point of view (cause we sent our
                //ACK) so make sure we set the state accordingly or the hangup
                //method won't know how to end the call.
                setState(CallPeerState.CONNECTED);
                hangup();
            }
            catch (Exception e)
            {
                //I don't see what more we could do.
                logAndFail("We couldn't hangup", e);
            }
            return;
        }

        // change status
        if (!CallPeerState.isOnHold(getState()))
            setState(CallPeerState.CONNECTED);
        */
    }

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangup()
        throws OperationFailedException
    {
        // do nothing if the call is already ended
        if (CallPeerState.DISCONNECTED.equals(getState())
            || CallPeerState.FAILED.equals(getState()))
        {
            logger.debug("Ignoring a request to hangup a call peer "
                + "that is already DISCONNECTED");
            return;
        }

        CallPeerState peerState = getState();
        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            boolean dialogIsAlive = sayBye();
            if (!dialogIsAlive)
            {
                setState(CallPeerState.DISCONNECTED);
            }
        }
        else if (CallPeerState.CONNECTING.equals(getState())
            || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState())
            || CallPeerState.ALERTING_REMOTE_SIDE.equals(getState()))
        {
            if (getLatestInviteTransaction() != null)
            {
                // Someone knows about us. Let's be polite and say we are
                // leaving
                sayCancel();
            }
            setState(CallPeerState.DISCONNECTED);
        }
        else if (peerState.equals(CallPeerState.INCOMING_CALL))
        {
            setState(CallPeerState.DISCONNECTED);
            sayBusyHere();
        }
        // For FAILED and BUSY we only need to update CALL_STATUS
        else if (peerState.equals(CallPeerState.BUSY))
        {
            setState(CallPeerState.DISCONNECTED);
        }
        else if (peerState.equals(CallPeerState.FAILED))
        {
            setState(CallPeerState.DISCONNECTED);
        }
        else
        {
            setState(CallPeerState.DISCONNECTED);
            logger.error("Could not determine call peer state!");
        }
    }

    /**
     * Sends a BUSY_HERE response to the peer represented by this instance.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayBusyHere()
        throws OperationFailedException
    {
        if (!(getLatestInviteTransaction() instanceof ServerTransaction))
        {
            logger.error("Cannot send BUSY_HERE in a client transaction");
            throw new OperationFailedException(
                "Cannot send BUSY_HERE in a client transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        Request request = getLatestInviteTransaction().getRequest();
        Response busyHere = null;
        try
        {
            busyHere = messageFactory.createResponse(
                                Response.BUSY_HERE, request);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create the BUSY_HERE response!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        ServerTransaction serverTransaction =
            (ServerTransaction) getLatestInviteTransaction();

        try
        {
            serverTransaction.sendResponse(busyHere);
            logger.debug("sent response:\n" + busyHere);
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the BUSY_HERE response",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Sends a Cancel request to the peer represented by this instance.
     *
     * @throws OperationFailedException we failed to construct or send the
     * CANCEL request.
     */
    private void sayCancel()
        throws OperationFailedException
    {
        if (getLatestInviteTransaction() instanceof ServerTransaction)
        {
            logger.error("Cannot cancel a server transaction");
            throw new OperationFailedException(
                "Cannot cancel a server transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        ClientTransaction clientTransaction =
            (ClientTransaction) getLatestInviteTransaction();
        try
        {
            Request cancel = clientTransaction.createCancel();
            ClientTransaction cancelTransaction =
                getJainSipProvider().getNewClientTransaction(
                    cancel);
            cancelTransaction.sendRequest();
            logger.debug("sent request:\n" + cancel);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the CANCEL request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Sends a BYE request to <tt>callPeer</tt>.
     *
     * @return <tt>true</tt> if the <tt>Dialog</tt> should be considered
     * alive after sending the BYE request (e.g. when there're still active
     * subscriptions); <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    private boolean sayBye() throws OperationFailedException
    {
        Dialog dialog = getDialog();

        Request bye = messageFactory.createRequest(dialog, Request.BYE);

        getProtocolProvider().sendInDialogRequest(
                        getJainSipProvider(), bye, dialog);

        /*
         * Let subscriptions such as the ones associated with REFER requests
         * keep the dialog alive and correctly delete it when they are
         * terminated.
         */
        try
        {
            return EventPackageUtils.processByeThenIsDialogAlive(dialog);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to determine whether the dialog should stay alive.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
            return false;
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
        Transaction transaction = getLatestInviteTransaction();

        if (transaction == null ||
            !(transaction instanceof ServerTransaction))
        {
            setState(CallPeerState.DISCONNECTED);
            throw new OperationFailedException(
                "Failed to extract a ServerTransaction "
                    + "from the call's associated dialog!",
                OperationFailedException.INTERNAL_ERROR);
        }

        CallPeerState peerState = getState();

        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            logger.info("Ignoring user request to answer a CallPeer "
                + "that is already connected. CP:");
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(
                            Response.OK, serverTransaction.getRequest());
        }
        catch (ParseException ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // Content
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            // content type should be application/sdp (not applications)
            // reported by Oleg Shevchenko (Miratech)
            contentTypeHeader = getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp");
        }
        catch (ParseException ex)
        {
            // Shouldn't happen
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a content type header for the OK response",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        /**
         * @todo update to neomedia.
        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession( getCall() );
            setMediaCallSession(callSession);

            callSession.setSessionCreatorCallback(this);

            String sdpOffer = getSdpDescription();
            String sdp;
            // if the offer was in the invite create an sdp answer
            if ((sdpOffer != null) && (sdpOffer.length() > 0))
            {
                sdp = callSession.processSdpOffer(this, sdpOffer);

                // set the call url in case there was one
                setCallInfoURL(callSession.getCallInfoURL());
            }
            // if there was no offer in the invite - create an offer
            else
            {
                sdp = callSession.createSdpOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (MediaException ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error(
                "Failed to create an SDP description for an OK response "
                    + "to an INVITE request!", ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }
        catch (ParseException ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error("Failed to parse SDP of an invoming INVITE",ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }
        */

        try
        {
            serverTransaction.sendResponse(ok);
            if (logger.isDebugEnabled())
                logger.debug("sent response\n" + ok);
        }
        catch (Exception ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Puts the <tt>CallPeer</tt> represented by this instance on or off hold.
     *
     * @param on <tt>true</tt> to have the <tt>CallPeer</tt> put on hold;
     * <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    public void putOnHold(boolean on)
        throws OperationFailedException
    {
        /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        try
        {
            sendReInvite(callSession.createSdpDescriptionForHold(
                    getSdpDescription(), on));
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP offer to hold.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        */
        /*
         * Putting on hold isn't a negotiation (i.e. the issuing side takes the
         * decision and executes it) so we're muting now regardless of the
         * desire of the peer to accept the offer.
         */
        /**
         * @todo update to neomedia.
        callSession.putOnHold(on, true);
         */
        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!on)
                setState(CallPeerState.CONNECTED);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (on)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (on)
        {
            setState(CallPeerState.ON_HOLD_LOCALLY);
        }
    }

    /**
     * Sends a reINVITE request with a specific <tt>sdpOffer</tt> (description)
     * within the current <tt>Dialog</tt> with a the call peer represented by
     * this instance.
     *
     * @param sdpOffer the offer that we'd like to use for the newly created
     * INVITE request.
     *
     * @throws OperationFailedException if sending the request fails for some
     * reason.
     */
    private void sendReInvite(String sdpOffer)
        throws OperationFailedException
    {
        Dialog dialog = getDialog();
        Request invite = messageFactory.createRequest(dialog, Request.INVITE);

        try
        {
            invite.setContent(sdpOffer, getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to parse SDP offer for the new invite.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        getProtocolProvider().sendInDialogRequest(
                        getJainSipProvider(), invite, dialog);
    }

    /**
     * Creates a <tt>CallPeerSipImpl</tt> from <tt>calleeAddress</tt> and sends
     * them an invite request. The invite request will be initialized according
     * to any relevant parameters in the <tt>cause</tt> message (if different
     * from <tt>null</tt>) that is the reason for creating this call.
     *
     * @throws OperationFailedException  with the corresponding code if we fail
     *  to create the call or in case we someone calls us mistakenly while we
     *  are actually wrapped around an invite transaction.
     */
    public void invite()
        throws OperationFailedException
    {
        ClientTransaction inviteTran;
        try
        {
            inviteTran = (ClientTransaction)getLatestInviteTransaction();
        }
        catch(ClassCastException exc)
        {
            throw new OperationFailedException(
                "Can't invite someone that is actually inviting us",
                OperationFailedException.INTERNAL_ERROR, exc);
        }

        attachSdpOffer(inviteTran.getRequest());

        try
        {
            inviteTran.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + inviteTran.getRequest());
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An error occurred while sending invite request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Creates an SDP offer destined to <tt>callPeer</tt> and attaches it to
     * the <tt>invite</tt> request.
     *
     * @param invite the invite <tt>Request</tt> that we'd like to attach an
     * SDP offer to.
     *
     * @throws OperationFailedException if we fail constructing the session
     * description.
     */
    private void attachSdpOffer(Request invite)
        throws OperationFailedException
    {
        InetAddress intendedDestination = getProtocolProvider()
            .getIntendedDestination(getPeerAddress());

        getMediaHandler().init(intendedDestination);
        /**
         * @todo update to neomedia.
        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession(getCall());

            setMediaCallSession(callSession);

            callSession.setSessionCreatorCallback(this);

            // indicate the address of the callee so that the media service can
            // choose the most proper local address to advertise.
            InetAddress intendedDestination = getProtocolProvider()
                .getIntendedDestination(getPeerAddress());

            ContentTypeHeader contentTypeHeader = getProtocolProvider()
                .getHeaderFactory().createContentTypeHeader(
                        "application", "sdp");

            invite.setContent(callSession.createSdpOffer(intendedDestination),
                              contentTypeHeader);
        }
        catch (IllegalArgumentException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to obtain an InetAddress for " + ex.getMessage(),
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to parse sdp data while creating invite request!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Could not access media devices!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        */

    }

    /**
     * Modifies the local media setup to reflect the requested setting for the
     * streaming of the local video and then re-invites the peer represented by
     * this class using a corresponding SDP description..
     *
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(boolean allowed)
        throws OperationFailedException
    {
         /**
         * @todo update to neomedia.
        CallSession callSession = getMediaCallSession();

        if(callSession.isLocalVideoAllowed() == allowed)
            return;

        try
        {
        */
            /*
             * Modify the local media setup to reflect the requested setting for
             * the streaming of the local video.
             */
        /**
         * @todo update to neomedia.
            callSession.setLocalVideoAllowed(allowed);
        }
        catch (MediaException ex)
        {
            throw new OperationFailedException(
                    "Failed to allow/disallow the streaming of local video.",
                    OperationFailedException.INTERNAL_ERROR, ex);
        }

        String sdpOffer = null;

        try
        {
            sdpOffer = callSession.createSdpOffer(getSdpDescription());
        }
        catch (MediaException ex)
        {
            throw new OperationFailedException(
                    "Failed to create re-invite offer for peer "
                        + this,OperationFailedException.INTERNAL_ERROR, ex);
        }

        sendReInvite(sdpOffer);
        */
    }

    /**
     * Determines whether we are currently streaming video toward whoever this
     * <tt>CallPeerSipImpl</tt> represents.
     *
     * @return <tt>true</tt> if we are currently streaming video toward this
     *  <tt>CallPeer</tt> and  <tt>false</tt> otherwise.
     */
    public boolean isLocalVideoStreaming()
    {
        return getMediaHandler().isLocalVideoTransmissionEnabled();
    }

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners which get notified when the properties (e.g.
     * LOCAL_VIDEO_STREAMING) associated with this <tt>CallPeer</tt> change
     * their values.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with the specified <tt>Call</tt> change
     * their values
     */
    public void addVideoPropertyChangeListener(PropertyChangeListener listener)
    {
        /**
         * @todo update to neomedia.
        getMediaCallSession().addPropertyChangeListener(listener);
        */
    }

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners which get notified when the properties (e.g.
     * LOCAL_VIDEO_STREAMING) associated with this <tt>CallPeer</tt> change
     * their values.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to no longer be
     * notified when the properties associated with the specified <tt>Call</tt>
     * change their values
     */
    public void removeVideoPropertyChangeListener(
                                               PropertyChangeListener listener)
    {
        /**
         * @todo update to neomedia.
         getMediaCallSession().removePropertyChangeListener(listener);
         */
    }

    /**
     * Updates this call so that it would record a new transaction and dialog
     * that have been recreated because of a re-authentication.
     *
     * @param retryTran the new transaction
     */
    public void handleAuthenticationChallenge(ClientTransaction retryTran)
    {
        // There is a new dialog that will be started with this request. Get
        // that dialog and record it into the Call object for later use (by
        // BYEs for example).
        // if the request was BYE then we need to authorize it anyway even
        // if the call and the call peer are no longer there
        setDialog(retryTran.getDialog());
        setLatestInviteTransaction(retryTran);
        setJainSipProvider(jainSipProvider);
    }

    /**
     * Returns a reference to the <tt>CallPeerMediaHandler</tt> used by this
     * peer. The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt>
     * always corresponds to exactly one instance of
     * <tt>CallPeerMediaHandler</tt> and both classes are only separated for
     * reasons of readability.
     *
     * @return a reference to the <tt>CallPeerMediaHandler</tt> instance that
     * this peer uses for media related tips and tricks.
     */
    private CallPeerMediaHandler getMediaHandler()
    {
        return mediaHandler;
    }
}
