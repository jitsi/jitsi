/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import static net.java.sip.communicator.service.protocol.OperationSetBasicTelephony.*;
import gov.nist.javax.sip.header.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.MediaType; // disambiguate
import org.jitsi.service.neomedia.control.*;

/**
 * Our SIP implementation of the default CallPeer;
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallPeerSipImpl
    extends MediaAwareCallPeer<CallSipImpl,
                               CallPeerMediaHandlerSipImpl,
                               ProtocolProviderServiceSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerSipImpl.class);

    /**
     * The sub-type of the content carried by SIP INFO <tt>Requests</tt> for the
     * purposes of <tt>picture_fast_update</tt>.
     */
    static final String PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE
        = "media_control+xml";

    /**
     * The sip address of this peer
     */
    private Address peerAddress = null;

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
     * The indicator which determines whether the local peer may send
     * <tt>picture_fast_update</tt> to this remote peer (as part of the
     * execution of {@link #requestKeyFrame()}).
     */
    private boolean sendPictureFastUpdate
        = KeyFrameControl.KeyFrameRequester.SIGNALING.equals(
                SipActivator.getConfigurationService().getString(
                        KeyFrameControl.KeyFrameRequester.PREFERRED_PNAME,
                        KeyFrameControl.KeyFrameRequester.DEFAULT_PREFERRED));

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
        super(owningCall);
        this.peerAddress = peerAddress;
        this.messageFactory = getProtocolProvider().getMessageFactory();

        super.setMediaHandler(
                new CallPeerMediaHandlerSipImpl(this)
                {
                    @Override
                    protected boolean requestKeyFrame()
                    {
                        return CallPeerSipImpl.this.requestKeyFrame();
                    }
                });

        setDialog(containingTransaction.getDialog());
        setLatestInviteTransaction(containingTransaction);
        setJainSipProvider(sourceProvider);
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        SipURI sipURI = (SipURI) peerAddress.getURI();

        return sipURI.getUser() + "@" + sipURI.getHost();
    }

    /**
     * Returns full URI of the address.
     *
     * @return full URI of the address
     */
    public String getURI()
    {
        return getPeerAddress().getURI().toString();
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

        if(displayName == null)
        {
            Contact contact = getContact();

            if (contact != null)
                displayName = contact.getDisplayName();
            else
            {
                URI peerURI = getPeerAddress().getURI();
                if (peerURI instanceof SipURI)
                {
                    String userName = ((SipURI) peerURI).getUser();

                    if (userName != null && userName.length() > 0)
                        displayName = userName;
                }

                if (displayName == null)
                {
                    displayName = peerURI.toString();
                }
            }
        }

        if(displayName.startsWith("sip:"))
            displayName = displayName.substring(4);

        return displayName;
    }

    /**
     * Sets a human readable name representing this peer.
     *
     * @param displayName the peer's display name
     */
    public void setDisplayName(String displayName)
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
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        // if this peer has no call, most probably it means
        // its disconnected and no more in call
        // and we cannot obtain the contact
        if(getCall() == null)
            return null;

        ProtocolProviderService pps = getCall().getProtocolProvider();
        OperationSetPresenceSipImpl opSetPresence
            = (OperationSetPresenceSipImpl) pps
                .getOperationSet(OperationSetPresence.class);

        if(opSetPresence != null)
            return opSetPresence.resolveContactID(getAddress());
        else
            return null;
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

    void processPictureFastUpdate(
            ClientTransaction clientTransaction,
            Response response)
    {
        /*
         * Disable the sending of picture_fast_update because it seems to be
         * unsupported by this remote peer.
         */
        if ((response.getStatusCode() != 200) && sendPictureFastUpdate)
            sendPictureFastUpdate = false;
    }

    boolean processPictureFastUpdate(
            ServerTransaction serverTransaction,
            Request request)
        throws OperationFailedException
    {
        CallPeerMediaHandlerSipImpl mediaHandler = getMediaHandler();
        boolean requested
            = (mediaHandler == null)
                ? false
                : mediaHandler.processKeyFrameRequest();

        Response response;

        try
        {
            response
                = getProtocolProvider().getMessageFactory().createResponse(
                        Response.OK,
                        request);
        }
        catch (ParseException pe)
        {
            throw new OperationFailedException(
                    "Failed to create OK Response.",
                    OperationFailedException.INTERNAL_ERROR,
                    pe);
        }

        if (!requested)
        {
            ContentType ct
                = new ContentType(
                        "application",
                        PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE);
            String content
                = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n"
                    + "<media_control>\r\n"
                    + "<general_error>\r\n"
                    + "Failed to process picture_fast_update request.\r\n"
                    + "</general_error>\r\n"
                    + "</media_control>";

            try
            {
                response.setContent(content, ct);
            }
            catch (ParseException pe)
            {
                throw new OperationFailedException(
                        "Failed to set content of OK Response.",
                        OperationFailedException.INTERNAL_ERROR,
                        pe);
            }
        }

        try
        {
            serverTransaction.sendResponse(response);
        }
        catch (Exception e)
        {
            throw new OperationFailedException(
                    "Failed to send OK Response.",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }

        return true;
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
            sdpOffer = SdpUtils.getContentAsString(invite);

        Response response = null;
        try
        {
            response = messageFactory.createResponse(Response.OK, invite);

            processExtraHeaders(response);

            String sdpAnswer;
            if(sdpOffer != null)
                sdpAnswer = getMediaHandler().processOffer( sdpOffer );
            else
                sdpAnswer = getMediaHandler().createOffer();

            response.setContent( sdpAnswer, getProtocolProvider()
                .getHeaderFactory().createContentTypeHeader(
                                "application", "sdp"));

            if (logger.isTraceEnabled())
                logger.trace("will send an OK response: " + response);
            serverTransaction.sendResponse(response);
            if (logger.isDebugEnabled())
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
                if (logger.isDebugEnabled())
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
            ReasonHeader reasonHeader =
                    (ReasonHeader)byeRequest.getHeader(ReasonHeader.NAME);

            if(reasonHeader != null)
            {
                setState(
                        CallPeerState.DISCONNECTED,
                        reasonHeader.getText(),
                        reasonHeader.getCause());
            }
            else
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

            if (logger.isDebugEnabled())
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

        ReasonHeader reasonHeader =
                    (ReasonHeader)cancel.getHeader(ReasonHeader.NAME);

        if(reasonHeader != null)
        {
            setState(
                    CallPeerState.DISCONNECTED,
                    reasonHeader.getText(),
                    reasonHeader.getCause());
        }
        else
            setState(CallPeerState.DISCONNECTED);
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
            getMediaHandler().start();

            // as its connected, set initial mute status,
            // corresponding call status
            // this would also unmute calls that were previously mute because
            // of early media.
            if(this.getCall() != null && isMute() != this.getCall().isMute())
                setMute(this.getCall().isMute());
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
            if (logger.isDebugEnabled())
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

        //change status
        setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
        getMediaHandler().start();

        // set the call on mute. we don't want the user to be heard unless they
        //know they are.
        setMute(true);
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
                setState(CallPeerState.CONNECTED,
                    "Error:" + exc.getLocalizedMessage());
                hangup();
            }
            catch (Exception e)
            {
                //handle in finally.
            }
            finally
            {
                logAndFail("Remote party sent a faulty session description.",
                        exc);
            }
            return;
        }

        // change status
        if (!CallPeerState.isOnHold(getState()))
        {
            setState(CallPeerState.CONNECTED);
            getMediaHandler().start();

            // as its connected, set initial mute status,
            // corresponding call status
            if(isMute() != this.getCall().isMute())
                setMute(this.getCall().isMute());
        }

        fireResponseProcessed(ok, null);
    }

    /**
     * Sends a <tt>picture_fast_update</tt> SIP INFO request to this remote
     * peer.
     *
     * @throws OperationFailedException if anything goes wrong while sending the
     * <tt>picture_fast_update</tt> SIP INFO request to this remote peer
     */
    private void pictureFastUpdate()
        throws OperationFailedException
    {
        Request info
            = getProtocolProvider().getMessageFactory().createRequest(
                    getDialog(),
                    Request.INFO);

        //here we add the body
        ContentType ct
            = new ContentType(
                    "application",
                    PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE);
        String content
            = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n"
                + "<media_control>\r\n"
                + "<vc_primitive>\r\n"
                + "<to_encoder>\r\n"
                + "<picture_fast_update/>\r\n"
                + "</to_encoder>\r\n"
                + "</vc_primitive>\r\n"
                + "</media_control>";

        ContentLength cl = new ContentLength(content.length());
        info.setContentLength(cl);

        try
        {
            info.setContent(content.getBytes(), ct);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to construct the INFO request", ex);
            throw new OperationFailedException(
                    "Failed to construct a client the INFO request",
                    OperationFailedException.INTERNAL_ERROR,
                    ex);

        }
        //body ended
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction
                = getJainSipProvider().getNewClientTransaction(info);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                    "Failed to construct a client transaction from the INFO request",
                    ex);
            throw new OperationFailedException(
                    "Failed to construct a client transaction from the INFO request",
                    OperationFailedException.INTERNAL_ERROR,
                    ex);
        }

        try
        {
            if (getDialog().getState()
                == DialogState.TERMINATED)
            {
                //this is probably because the call has just ended, so don't
                //throw an exception. simply log and get lost.
                logger.warn(
                        "Trying to send a dtmf tone inside a "
                            + "TERMINATED dialog.");
                return;
            }

            getDialog().sendRequest(clientTransaction);
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + info);
        }
        catch (SipException ex)
        {
            throw new OperationFailedException(
                    "Failed to send the INFO request",
                    OperationFailedException.NETWORK_FAILURE,
                    ex);
        }
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
        // By default we hang up by indicating no failure has happened.
        hangup(HANGUP_REASON_NORMAL_CLEARING, null);
    }

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @param reasonCode indicates if the hangup is following to a call failure
     * or simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangup(int reasonCode, String reason)
        throws OperationFailedException
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

        boolean failed = (reasonCode != HANGUP_REASON_NORMAL_CLEARING);

        CallPeerState peerState = getState();
        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            // if we fail to send the bye, lets close the call anyway
            try
            {
                boolean dialogIsAlive = sayBye(reasonCode, reason);
                if (!dialogIsAlive)
                {
                    setDisconnectedState(failed, reason);
                }
            }
            catch(Throwable ex)
            {
                logger.error(
                    "Error while trying to hangup, trying to handle!", ex);

                // make sure we end media if exception occurs
                setDisconnectedState(true, null);

                // if its the handled OperationFailedException, pass it
                if(ex instanceof OperationFailedException)
                    throw (OperationFailedException)ex;
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
            setDisconnectedState(failed, reason);
        }
        else if (peerState.equals(CallPeerState.INCOMING_CALL))
        {
            setDisconnectedState(failed, reason);
            sayBusyHere();
        }
        // For FAILED and BUSY we only need to update CALL_STATUS
        else if (peerState.equals(CallPeerState.BUSY))
        {
            setDisconnectedState(failed, reason);
        }
        else if (peerState.equals(CallPeerState.FAILED))
        {
            setDisconnectedState(failed, reason);
        }
        else
        {
            setDisconnectedState(failed, reason);
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
            if (logger.isDebugEnabled())
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
            if (logger.isDebugEnabled())
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
    private boolean sayBye(int reasonCode, String reason)
        throws OperationFailedException
    {
        Dialog dialog = getDialog();

        Request bye = messageFactory.createRequest(dialog, Request.BYE);

        if(reasonCode != HANGUP_REASON_NORMAL_CLEARING && reason != null)
        {
            int sipCode = convertReasonCodeToSIPCode(reasonCode);

            if(sipCode != -1)
            {
                try
                {
                    // indicate reason for failure
                    // using Reason header rfc3326
                    ReasonHeader reasonHeader =
                        getProtocolProvider().getHeaderFactory()
                            .createReasonHeader(
                                "SIP",
                                sipCode,
                                reason);
                    bye.setHeader(reasonHeader);
                }
                catch(Throwable e)
                {
                    logger.error("Cannot set reason header", e);
                }
            }
        }

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
     * Converts the codes for hangup from OperationSetBasicTelephony one
     * to the sip codes.
     * @param reasonCode the reason code.
     * @return the sip code or -1 if not found.
     */
    private static int convertReasonCodeToSIPCode(int reasonCode)
    {
        switch(reasonCode)
        {
            case HANGUP_REASON_NORMAL_CLEARING :
                return Response.ACCEPTED;
            case HANGUP_REASON_ENCRYPTION_REQUIRED :
                return Response.SESSION_NOT_ACCEPTABLE;
            case HANGUP_REASON_TIMEOUT :
                return Response.REQUEST_TIMEOUT;
            case HANGUP_REASON_BUSY_HERE :
                return Response.BUSY_HERE;
            default : return -1;
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
            if (logger.isInfoEnabled())
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

            processExtraHeaders(ok);
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

        // This is the sdp offer that came from the initial invite,
        // also that invite can have no offer.
        String sdpOffer = null;
        try
        {
            // extract the SDP description.
            // beware: SDP description may be in ACKs so it could be that
            // there's nothing here - bug report Laurent Michel
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

            //do not continue processing - we already canceled the peer here
            setState(CallPeerState.FAILED, ex.getMessage());

            return;
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

        // the ACK to our answer might already be processed before we get here
        if(CallPeerState.INCOMING_CALL.equals(getState()))
        {
            if(sdpOffer != null && sdpOffer.length() > 0)
                setState(CallPeerState.CONNECTING_INCOMING_CALL_WITH_MEDIA);
            else
                setState(CallPeerState.CONNECTING_INCOMING_CALL);
        }
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
        CallPeerMediaHandlerSipImpl mediaHandler = getMediaHandler();

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
            invite.setContent(
                    sdpOffer,
                    getProtocolProvider()
                        .getHeaderFactory()
                            .createContentTypeHeader("application", "sdp"));

            processExtraHeaders(invite);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Failed to parse SDP offer for the new invite.",
                    OperationFailedException.INTERNAL_ERROR,
                    ex,
                    logger);
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

            invite.setContent(
                    getMediaHandler().createOffer(),
                    contentTypeHeader);

            processExtraHeaders(invite);

            inviteTran.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + inviteTran.getRequest());
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "An error occurred while sending invite request",
                    OperationFailedException.NETWORK_FAILURE,
                    ex,
                    logger);
        }
    }

    /**
     * A place where we can handle any headers we need for requests
     * and responses. Such a case is reflecting the focus state in contact
     * header.
     * @param message the SIP <tt>Message</tt> in which a header change
     * is to be reflected
     * @throws ParseException if modifying the specified SIP <tt>Message</tt> to
     * reflect the header change fails
     */
    protected void processExtraHeaders(javax.sip.message.Message message)
        throws ParseException
    {
        /*
         * If the local peer represented by the Call of this CallPeer is
         * acting as a conference focus, it must indicate it in its Contact
         * header.
         */
        reflectConferenceFocus(message);
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
     * Requests a (video) key frame from this remote peer of the associated.
     *
     * @return <tt>true</tt> if a key frame has indeed been requested from this
     * remote peer in response to the call; otherwise, <tt>false</tt>
     */
    private boolean requestKeyFrame()
    {
        boolean requested = false;

        if (sendPictureFastUpdate)
        {
            try
            {
                pictureFastUpdate();
                requested = true;
            }
            catch (OperationFailedException ofe)
            {
                /*
                 * Apart from logging, it does not seem like there are a lot of
                 * ways to handle it.
                 */
            }
        }
        return requested;
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
     * Causes this CallPeer to enter either the DISCONNECTED or the FAILED
     * state.
     *
     * @param failed indicates if the disconnection is due to a failure
     * @param reason the reason of the disconnection
     */
    private void setDisconnectedState(boolean failed, String reason)
    {
        if (failed)
            setState(CallPeerState.FAILED, reason);
        else
            setState(CallPeerState.DISCONNECTED, reason);
    }

    /**
     * {@inheritDoc}
     */
    public String getEntity()
    {
        return AbstractOperationSetTelephonyConferencing
                .stripParametersFromAddress(getURI());
    }

    /**
     * {@inheritDoc}
     *
     * Uses the direction of the media stream as a fallback.
     * TODO: return the direction negotiated via SIP
     */
    @Override
    public MediaDirection getDirection(MediaType mediaType)
    {
        MediaStream stream = getMediaHandler().getStream(mediaType);
        if (stream != null)
        {
            MediaDirection direction = stream.getDirection();
            return direction == null
                    ? MediaDirection.INACTIVE
                    : direction;
        }

        return MediaDirection.INACTIVE;
    }
}
