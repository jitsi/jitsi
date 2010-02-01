/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Our SIP implementation of the default CallPeer;
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallPeerSipImpl
    extends AbstractCallPeer
    implements SimpleAudioLevelListener,
               CallPeerConferenceListener,
               CsrcAudioLevelListener,
               ZrtpListener
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
     * The SIP transaction that established this call. This was previously kept
     * in the jain-sip dialog but got deprecated there so we're now keeping it
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
     * The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
     * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
     * both classes are only separated for reasons of readability.
     */
    private final CallPeerMediaHandler mediaHandler;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to
     * {@link #mediaHandler} for changes in the values of its properties.
     */
    private PropertyChangeListener mediaHandlerPropertyChangeListener;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * The <tt>List</tt> of <tt>MethodProcessorListener</tt>s interested in how
     * this <tt>CallPeer</tt> processes SIP signaling.
     */
    private final List<MethodProcessorListener> methodProcessorListeners
        = new LinkedList<MethodProcessorListener>();

    /**
     * The <tt>List</tt> of <tt>PropertyChangeListener</tt>s listening to this
     * <tt>CallPeer</tt> for changes in the values of its properties related to
     * video.
     */
    private final List<PropertyChangeListener> videoPropertyChangeListeners
            = new LinkedList<PropertyChangeListener>();

    /**
     * Holds listeners registered for level changes in the audio we are getting
     * from the remote participant.
     */
    private final List<SoundLevelListener> streamAudioLevelListeners
        = new ArrayList<SoundLevelListener>();

    /**
     * Holds listeners registered for level changes in the audio of participants
     * that this peer might be mixing and that we are not directly communicating
     * with.
     */
    private final List<ConferenceMembersSoundLevelListener>
        conferenceMemberAudioLevelListeners
            = new ArrayList<ConferenceMembersSoundLevelListener>();

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
        this.messageFactory = getProtocolProvider().getMessageFactory();

        this.mediaHandler = new CallPeerMediaHandler(this);

        setDialog(containingTransaction.getDialog());
        setLatestInviteTransaction(containingTransaction);
        setJainSipProvider(sourceProvider);

        //create the uid
        this.peerID = String.valueOf(System.currentTimeMillis())
                             + String.valueOf(hashCode());

        // we listen fr events when the call will become focus or not
        // of a conference so we will add or remove our sound level listeners
        super.addCallPeerConferenceListener(this);
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
        Dialog dialog = getDialog();

        if (dialog != null)
        {
            Address remoteParty = dialog.getRemoteParty();

            if (remoteParty != null)
            {
                //update the address we've cached.
                peerAddress = remoteParty;
            }
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
     * Returns a URL pointing to a location with call control information for
     * this peer or <tt>null</tt> if no such URL is available for this
     * call peer.
     *
     * @return a URL link to a location with call information or a call control
     * web interface related to this peer or <tt>null</tt> if no such URL
     * is available.
     */
    @Override
    public URL getCallInfoURL()
    {
        return getMediaHandler().getCallInfoURL();
    }

    /**
     * Determines whether the audio stream (if any) being sent to this
     * peer is mute.
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         peer and it is currently mute; <tt>false</tt>, otherwise
     */
    @Override
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
        String sdpOffer = null;
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            sdpOffer = SdpUtils.getContentAsString(invite);
        }

        Response response = null;
        try
        {
            response = messageFactory.createResponse(Response.OK, invite);

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(response);

            String sdpAnswer;
            if(sdpOffer != null)
                sdpAnswer = getMediaHandler().processOffer( sdpOffer );
            else
                sdpAnswer = getMediaHandler().createOffer();

            response.setContent( sdpAnswer, getProtocolProvider()
                .getHeaderFactory().createContentTypeHeader(
                                "application", "sdp"));

            logger.trace("will send an OK response: " + response);
            serverTransaction.sendResponse(response);
            logger.debug("OK response sent");
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

        reevalRemoteHoldStatus();

        fireRequestProcessed(invite, response);
    }

    /**
     * Updates the state of this <tt>CallPeer</tt> to match the remotely-on-hold
     * status of our media handler.
     */
    private void reevalRemoteHoldStatus()
    {
        boolean remotelyOnHold = getMediaHandler().isRemotelyOnHold();

        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (remotelyOnHold)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!remotelyOnHold)
                setState(CallPeerState.ON_HOLD_LOCALLY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!remotelyOnHold)
                setState(CallPeerState.CONNECTED);
        }
        else if (remotelyOnHold)
        {
            setState(CallPeerState.ON_HOLD_REMOTELY);
        }
    }

    /**
     * Updates the state of this <tt>CallPeer</tt> to match the locally-on-hold
     * status of our media handler.
     */
    private void reevalLocalHoldStatus()
    {
        boolean locallyOnHold = getMediaHandler().isLocallyOnHold();

        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!locallyOnHold)
                setState(CallPeerState.CONNECTED);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!locallyOnHold)
                setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (locallyOnHold)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (locallyOnHold)
        {
            setState(CallPeerState.ON_HOLD_LOCALLY);
        }
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
            /*
             * No need to let the user know about the error since it doesn't
             * affect them. And just as the comment on sendResponse bellow
             * says, this is not really a problem according to the RFC so we
             * should proceed with the execution bellow in order to gracefully
             * hangup the call.
             */
        }

        if (ok != null)
            try
            {
                byeTran.sendResponse(ok);
                logger.debug("sent response " + ok);
            }
            catch (Exception ex)
            {
                /*
                 * This is not really a problem according to the RFC so just
                 * dump to stdout should someone be interested.
                 */
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
        if (dialogIsAlive)
        {
            getMediaHandler().close();
        }
        else
        {
            setState(CallPeerState.DISCONNECTED);
        }
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
    @Override
    public void setMute(boolean newMuteValue)
    {
        getMediaHandler().setMute(newMuteValue);
        super.setMute(newMuteValue);
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
            try
            {
                getMediaHandler().processAnswer(
                                    SdpUtils.getContentAsString(ack));
            }
            catch (Exception exc)
            {
                logAndFail("There was an error parsing the SDP description of "
                            + getDisplayName() + "(" + getAddress() + ")", exc);
                return;
            }
        }

        // change status
        CallPeerState peerState = getState();
        if (!CallPeerState.isOnHold(peerState))
        {
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

        //handle media
        try
        {
            getMediaHandler().processAnswer(
                            SdpUtils.getContentAsString(response));
        }
        catch (Exception exc)
        {
            logAndFail("There was an error parsing the SDP description of "
                + getDisplayName() + "(" + getAddress() + ")", exc);
            return;
        }

        // change status
        setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
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

        try
        {
             //Process SDP unless we've just had an answer in a 18X response
            if (!CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState()))
            {
                getMediaHandler()
                    .processAnswer(SdpUtils.getContentAsString(ok));
            }
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
                //handle in finally.
            }
            finally
            {
                logAndFail("Remote party sent a faulty session description.", exc);
            }
            return;
        }

        // change status
        if (!CallPeerState.isOnHold(getState()))
            setState(CallPeerState.CONNECTED);

        fireResponseProcessed(ok, null);
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
        Request invite = serverTransaction.getRequest();
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(Response.OK, invite);

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(ok);
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

        try
        {
            String sdpOffer = null;

            // extract the SDP description.
            // beware: SDP description may be in ACKs so it could be that there's
            // nothing here - bug report Laurent Michel
            ContentLengthHeader cl = invite.getContentLength();
            if (cl != null && cl.getContentLength() > 0)
            {
                sdpOffer = SdpUtils.getContentAsString(invite);
            }

            String sdp;
            // if the offer was in the invite create an SDP answer
            if ((sdpOffer != null) && (sdpOffer.length() > 0))
            {
                sdp = getMediaHandler().processOffer(sdpOffer);
            }
            // if there was no offer in the invite - create an offer
            else
            {
                sdp = getMediaHandler().createOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (Exception ex)
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

        fireRequestProcessed(invite, ok);
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
        CallPeerMediaHandler mediaHandler = getMediaHandler();

        mediaHandler.setLocallyOnHold(onHold);

        try
        {
            sendReInvite(mediaHandler.createOffer());
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP offer to hold.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        reevalLocalHoldStatus();
    }

    /**
     * Sends a reINVITE request to this <tt>CallPeer</tt> within its current
     * <tt>Dialog</tt>.
     *
     * @throws OperationFailedException if sending the reINVITE request fails
     */
    void sendReInvite()
        throws OperationFailedException
    {
        sendReInvite(getMediaHandler().createOffer());
    }

    /**
     * Sends a reINVITE request with a specific <tt>sdpOffer</tt> (description)
     * within the current <tt>Dialog</tt> with the call peer represented by
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
            // Content-Type
            invite.setContent(sdpOffer, getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(invite);
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
        try
        {
            ClientTransaction inviteTran
                = (ClientTransaction) getLatestInviteTransaction();
            Request invite = inviteTran.getRequest();

            // Content-Type
            ContentTypeHeader contentTypeHeader
                = getProtocolProvider()
                    .getHeaderFactory()
                        .createContentTypeHeader("application", "sdp");

            invite
                .setContent(getMediaHandler().createOffer(), contentTypeHeader);

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(invite);

            inviteTran.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + inviteTran.getRequest());
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An error occurred while sending invite request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Reflects the value of the <tt>conferenceFocus</tt> property of the
     * <tt>Call</tt> of this <tt>CallPeer</tt> in the specified SIP
     * <tt>Message</tt>.
     *
     * @param message the SIP <tt>Message</tt> in which the value of the
     * <tt>conferenceFocus</tt> property of the <tt>Call</tt> of this
     * <tt>CallPeer</tt> is to be reflected
     * @throws ParseException if modifying the specified SIP <tt>Message</tt> to
     * reflect the <tt>conferenceFocus</tt> property of the <tt>Call</tt> of
     * this <tt>CallPeer</tt> fails
     */
    private void reflectConferenceFocus(javax.sip.message.Message message)
        throws ParseException
    {
        ContactHeader contactHeader
            = (ContactHeader) message.getHeader(ContactHeader.NAME);

        if (contactHeader != null)
        {
            // we must set the value of the parameter as null
            // in order to avoid wrong generation of the tag - ';isfocus='
            // as it must be ';isfocus'
            if (getCall().isConferenceFocus())
                contactHeader.setParameter("isfocus", null);
            else
                contactHeader.removeParameter("isfocus");
        }
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
        CallPeerMediaHandler mediaHandler = getMediaHandler();

        if(mediaHandler.isLocalVideoTransmissionEnabled() == allowed)
            return;

        // Modify the local media setup to reflect the requested setting for
        // the streaming of the local video.
        mediaHandler.setLocalVideoTransmissionEnabled(allowed);

        String sdpOffer = null;

        try
        {
            sdpOffer = mediaHandler.createOffer();
        }
        catch (Exception ex)
        {
            throw new OperationFailedException(
                    "Failed to create re-invite offer for peer "
                        + this,OperationFailedException.INTERNAL_ERROR, ex);
        }

        sendReInvite(sdpOffer);
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
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (videoPropertyChangeListeners)
        {
            /*
             * The video is part of the media-related functionality and thus it
             * is the responsibility of mediaHandler. So listen to mediaHandler
             * for video-related property changes and re-fire them as
             * originating from this instance.
             */
            if (!videoPropertyChangeListeners.contains(listener)
                    && videoPropertyChangeListeners.add(listener)
                    && (mediaHandlerPropertyChangeListener == null))
            {
                mediaHandlerPropertyChangeListener
                    = new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent event)
                        {
                            Iterable<PropertyChangeListener> listeners;

                            synchronized (videoPropertyChangeListeners)
                            {
                                listeners
                                    = new LinkedList<PropertyChangeListener>(
                                            videoPropertyChangeListeners);
                            }

                            PropertyChangeEvent thisEvent
                                = new PropertyChangeEvent(
                                        this,
                                        event.getPropertyName(),
                                        event.getOldValue(),
                                        event.getNewValue());

                            for (PropertyChangeListener listener : listeners)
                                listener.propertyChange(thisEvent);
                        }
                    };
                getMediaHandler()
                    .addPropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
            }
        }
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
        if (listener != null)
            synchronized (videoPropertyChangeListeners)
            {
                /*
                 * The video is part of the media-related functionality and thus
                 * it is the responsibility of mediaHandler. So we're listening
                 * to mediaHandler for video-related property changes and w're
                 * re-firing them as originating from this instance. Make sure
                 * that we're not listening to mediaHandler if noone is
                 * interested in video-related property changes originating from
                 * this instance.
                 */
                if (videoPropertyChangeListeners.remove(listener)
                        && videoPropertyChangeListeners.isEmpty()
                        && (mediaHandlerPropertyChangeListener != null))
                {
//                    getMediaHandler()
//                        .removePropertyChangeListener(
//                            mediaHandlerPropertyChangeListener);
                    mediaHandlerPropertyChangeListener = null;
                }
            }
    }

    /**
     * Registers a specific <tt>MethodProcessorListener</tt> with this
     * <tt>CallPeer</tt> so that it gets notified by this instance about the
     * processing of SIP signaling. If the specified <tt>listener</tt> is
     * already registered with this instance, does nothing
     *
     * @param listener the <tt>MethodProcessorListener</tt> to be registered
     * with this <tt>CallPeer</tt> so that it gets notified by this instance
     * about the processing of SIP signaling
     */
    void addMethodProcessorListener(MethodProcessorListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (methodProcessorListeners)
        {
            if (!methodProcessorListeners.contains(listener))
                methodProcessorListeners.add(listener);
        }
    }

    /**
     * Notifies the <tt>MethodProcessorListener</tt>s registered with this
     * <tt>CallPeer</tt> that it has processed a specific SIP <tt>Request</tt>
     * by sending a specific SIP <tt>Response</tt>.
     *
     * @param request the SIP <tt>Request</tt> processed by this
     * <tt>CallPeer</tt>
     * @param response the SIP <tt>Response</tt> this <tt>CallPeer</tt> sent as
     * part of its processing of the specified <tt>request</tt>
     */
    protected void fireRequestProcessed(Request request, Response response)
    {
        Iterable<MethodProcessorListener> listeners;

        synchronized (methodProcessorListeners)
        {
            listeners
                = new LinkedList<MethodProcessorListener>(
                        methodProcessorListeners);
        }

        for (MethodProcessorListener listener : listeners)
            listener.requestProcessed(this, request, response);
    }

    /**
     * Notifies the <tt>MethodProcessorListener</tt>s registered with this
     * <tt>CallPeer</tt> that it has processed a specific SIP <tt>Response</tt>
     * by sending a specific SIP <tt>Request</tt>.
     *
     * @param response the SIP <tt>Response</tt> processed by this
     * <tt>CallPeer</tt>
     * @param request the SIP <tt>Request</tt> this <tt>CallPeer</tt> sent as
     * part of its processing of the specified <tt>response</tt>
     */
    protected void fireResponseProcessed(Response response, Request request)
    {
        Iterable<MethodProcessorListener> listeners;

        synchronized (methodProcessorListeners)
        {
            listeners
                = new LinkedList<MethodProcessorListener>(
                        methodProcessorListeners);
        }

        for (MethodProcessorListener listener : listeners)
            listener.responseProcessed(this, response, request);
    }

    /**
     * Unregisters a specific <tt>MethodProcessorListener</tt> from this
     * <tt>CallPeer</tt> so that it no longer gets notified by this instance
     * about the processing of SIP signaling. If the specified <tt>listener</tt>
     * is not registered with this instance, does nothing.
     *
     * @param listener the <tt>MethodProcessorListener</tt> to be unregistered
     * from this <tt>CallPeer</tt> so that it no longer gets notified by this
     * instance about the processing of SIP signaling
     */
    void removeMethodProcessorListener(MethodProcessorListener listener)
    {
        if (listener != null)
            synchronized (methodProcessorListeners)
            {
                methodProcessorListeners.remove(listener);
            }
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
    CallPeerMediaHandler getMediaHandler()
    {
        return mediaHandler;
    }

    /**
     * Overrides the parent set state method in order to make sure that we
     * close our media handler whenever we enter a disconnected state.
     *
     * @param newState the <tt>CallPeerState</tt> that we are about to enter and
     * that we pass to our predecessor.
     * @param reason a reason phrase explaining the state (e.g. if newState
     * indicates a failure) and that we pass to our predecessor.
     */
    @Override
    public void setState(CallPeerState newState, String reason)
    {
        super.setState(newState, reason);

        if (CallPeerState.DISCONNECTED.equals(newState)
                || CallPeerState.FAILED.equals(newState))
            getMediaHandler().close();
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of listeners
     * interested in and notified about changes in the sound level of the audio
     * sent by the remote party. When the first listener is being registered
     * the method also registers its single listener with the media handler so
     * that it would receive level change events and delegate them to the listeners
     * that have registered with us.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addStreamSoundLevelListener(SoundLevelListener listener)
    {
        synchronized (streamAudioLevelListeners)
        {
            if (streamAudioLevelListeners.size() == 0)
            {
                // if this is the first listener that's being registered with
                // us, we also need to register ourselves as an audio level
                // listener with the media handler. we do this so that audio
                // levels would only be calculated if anyone is interested in
                // receiving them.
                getMediaHandler().setStreamAudioLevelListener(this);
            }

            streamAudioLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to remove
     */
    public void removeStreamSoundLevelListener(SoundLevelListener listener)
    {
        synchronized (streamAudioLevelListeners)
        {
            streamAudioLevelListeners.remove(listener);

            if (streamAudioLevelListeners.size() == 0)
            {
                // if this was the last listener then we also need to remove
                // ourselves as an audio level so that audio levels would only
                // be calculated if anyone is interested in receiving them.
                getMediaHandler().setStreamAudioLevelListener(null);
            }
        }
    }

    /**
     * Adds a specific <tt>ConferenceMembersSoundLevelListener</tt> to the list
     * of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>ConferenceMembersSoundLevelListener</tt> to add
     */
    public void addConferenceMembersSoundLevelListener(
                                ConferenceMembersSoundLevelListener listener)
    {
        synchronized (conferenceMemberAudioLevelListeners)
        {

            if (conferenceMemberAudioLevelListeners.size() == 0)
            {
                // if this is the first listener that's being registered with
                // us, we also need to register ourselves as a CSRC audio level
                // listener with the media handler.
                getMediaHandler().setCsrcAudioLevelListener(this);
            }

            conferenceMemberAudioLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>ConferenceMembersSoundLevelListener</tt> of the
     * list of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>ConferenceMembersSoundLevelListener</tt> to
     * remove
     */
    public void removeConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
        synchronized (conferenceMemberAudioLevelListeners)
        {
            conferenceMemberAudioLevelListeners.remove(listener);

            if (conferenceMemberAudioLevelListeners.size() == 0)
            {
                // if this was the last listener then we also remove ourselves
                // as a CSRC audio level listener from the handler so that we
                // don't have to create new events and maps for something no one
                // is interested in.
                getMediaHandler().setCsrcAudioLevelListener(null);
            }
        }
    }

    /**
     * Implements {@link CsrcAudioLevelListener#audioLevelsReceived(long[][])}
     * so that we could deliver to {@link ConferenceMembersSoundLevelListener}s
     * the events corresponding to the audio level changes that are being
     * reported here.
     *
     * @param audioLevels the levels that we need to dispatch to all registered
     * <tt>ConferenceMemberSoundLevelListeners</tt>.
     */
    public void audioLevelsReceived(long[][] audioLevels)
    {
        if (getConferenceMemberCount() == 0)
            return;

        Map<ConferenceMember, Integer> levelsMap
            = new HashMap<ConferenceMember, Integer>();

        for (int i = 0; i < audioLevels.length; i++)
        {
            ConferenceMember mmbr = findConferenceMember(audioLevels[i][0]);

            if (mmbr == null)
                continue;
            else
                levelsMap.put(mmbr, (int)audioLevels[i][1]);
        }

        ConferenceMembersSoundLevelEvent evt
            = new ConferenceMembersSoundLevelEvent(this, levelsMap);

        synchronized( conferenceMemberAudioLevelListeners)
        {
            int conferenceMemberAudioLevelListenerCount
                = conferenceMemberAudioLevelListeners.size();

            for (int i = 0; i < conferenceMemberAudioLevelListenerCount; i++)
                conferenceMemberAudioLevelListeners
                    .get(i).soundLevelChanged(evt);
        }
    }

    /**
     * Does nothing.
     * @param evt the event.
     */
    public void callPeerAdded(CallPeerEvent evt)
    {}

    /**
     * Does nothing.
     * @param evt the event.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {}

    /**
     * Sets the security status to ON for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the cipher
     * @param securityString the SAS
     * @param isVerified indicates if the SAS has been verified
     * @param multiStreamData the data for the multistream
     *        used by non master streams.
     */
    public void securityTurnedOn(
        int sessionType,
        String cipher, String securityString, boolean isVerified,
        byte[] multiStreamData)
    {
        if(multiStreamData != null)
        {
            getMediaHandler().startZrtpMultistream(multiStreamData);
        }

        fireCallPeerSecurityOnEvent(
                        sessionType, cipher, securityString, isVerified);
    }

    /**
     * Sets the security status to OFF for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityTurnedOff(int sessionType)
    {
        // If this event has been triggered because of a call end event and the
        // call is already ended we don't need to alert the user for
        // security off.
        Call call = getCall();

        if((call != null) && !call.getCallState().equals(CallState.CALL_ENDED))
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
    public void securityMessageReceived(
        String messageType, String i18nMessage, int severity)
    {
        fireCallPeerSecurityMessageEvent(messageType,
                                         i18nMessage,
                                         severity);
    }

    /**
     * Dummy implementation of {@link CallPeerConferenceListener
     * #conferenceFocusChanged(CallPeerConferenceEvent)}.
     *
     * @param evt ignored
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent evt)
    {
    }

    /**
     * Called when this peer becomes a mixer. The method add removes this class
     * as the stream audio level listener for the media coming from this peer
     * because the levels it delivers no longer represent the level of a
     * particular member. The method also adds this class as a member (CSRC)
     * audio level listener.
     *
     * @param conferenceEvent the event containing information (that we don't
     * really use) on the newly add member.
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        if (getConferenceMemberCount() > 2)
        {
            // this peer is now a conference focus with more than three
            // participants. This means that the this peer is mixing and sending
            // us audio for at least two separate participants. We therefore
            // need to remove the stream level listeners and switch to CSRC
            // level listening
            CallPeerMediaHandler mediaHandler = getMediaHandler();

            mediaHandler.setStreamAudioLevelListener(null);
            mediaHandler.setCsrcAudioLevelListener(this);
        }
    }

    /**
     * Called when this peer stops being a mixer. The method add removes this
     * class as the stream audio level listener for the media coming from this
     * peer because the levels it delivers no longer represent the level of a
     * particular member. The method also adds this class as a member (CSRC)
     * audio level listener.
     *
     * @param conferenceEvent the event containing information (that we don't
     * really use) on the freshly removed member.
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {
        if (getConferenceMemberCount() < 3)
        {
            // this call peer is no longer mixing audio from multiple sources
            // since there's only us and her in the call. Lets stop being a CSRC
            // listener and move back to listening the audio level of the
            // stream itself.
            CallPeerMediaHandler mediaHandler = getMediaHandler();

            mediaHandler.setStreamAudioLevelListener(this);
            mediaHandler.setCsrcAudioLevelListener(null);
        }
    }

    /**
     * Notified by its very majesty the media service about changes in the
     * audio level of the stream coming from this peer, this method generates
     * the corresponding events and delivers them to the listeners that have
     * registered here.
     *
     * @param newLevel the new audio level of the local user.
     */
    public void audioLevelChanged(int newLevel)
    {
        /*
         * If we're in a conference in which this CallPeer is the focus and
         * we're the only member in it besides the focus, we will not receive
         * audio levels in the RTP and our media will instead measure the audio
         * levels of the received media. In order to make the UI oblivious of
         * the difference, we have to translate the event to the appropriate
         * type of listener.
         *
         * We may end up in a conference call with 0 members if the server
         * for some reason doesn't support sip conference (our subscribes
         * doesn't go to the focus of the conference) and so we must
         * pass the sound levels measured on the stream so we can see
         * the stream activity of the call.
         */
        if (isConferenceFocus() && (getConferenceMemberCount() > 0)
            && (getConferenceMemberCount() < 3))
        {
            long audioRemoteSSRC = getMediaHandler().getAudioRemoteSSRC();

            if (audioRemoteSSRC != CallPeerMediaHandler.SSRC_UNKNOWN)
            {
                long[][] audioLevels = new long[1][2];
                audioLevels[0][0] = audioRemoteSSRC;
                audioLevels[0][1] = newLevel;

                audioLevelsReceived(audioLevels);
                return;
            }
        }

        synchronized( streamAudioLevelListeners )
        {
            if (streamAudioLevelListeners.size() > 0)
            {
                SoundLevelChangeEvent evt
                    = new SoundLevelChangeEvent(this, newLevel);

                for(SoundLevelListener listener : streamAudioLevelListeners)
                    listener.soundLevelChanged(evt);
            }
        }
    }
}
