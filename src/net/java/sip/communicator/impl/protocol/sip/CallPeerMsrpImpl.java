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

import java.net.*;
import java.net.URI;
import java.text.ParseException;
import java.util.*;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.Transaction;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import javax.net.msrp.*;
import javax.net.msrp.exceptions.*;

import org.jitsi.service.neomedia.MediaDirection;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * An MSRP variant of {@link CallPeerSipImpl} with media handling capabilities.
 * @author tuijldert
 */
public class CallPeerMsrpImpl
    extends CallPeerSipImpl
{
    private static final Logger logger =
                    Logger.getLogger(CallPeerMsrpImpl.class);

    /** Reference to basic IM operation set */
    private OperationSetBasicInstantMessagingMsrpImpl opsetBasicIM;

    /** Reference to file transfer operation set    */
    private OperationSetFileTransferMsrpImpl opsetFileTransfer;

    /** The MSRP session that will be created with peer. */
    private Session session;

    /** Whether this is a file transfer session */
    private boolean isFileTransfer = false;

    /** ...if it is, we also have a transfer object */
    private FileTransferImpl transferActivity = null;

    /** The peer addresses */
    private ArrayList<URI> toList;

    private SessionDescription localSess = null;

    /**
     * Create an MSRP handler for <tt>peer</tt>
     * @param peer  the peer we'll be managing the chat for.
     */
    public CallPeerMsrpImpl(Address peerAddress, CallSipImpl owningCall,
                        javax.sip.Transaction containingTransaction,
                        SipProvider sourceProvider)
    {
        super(peerAddress, owningCall, containingTransaction, sourceProvider);
        this.opsetBasicIM       = (OperationSetBasicInstantMessagingMsrpImpl)
            getProtocolProvider().getOperationSet(
                                OperationSetBasicInstantMessaging.class);
        this.opsetFileTransfer  = (OperationSetFileTransferMsrpImpl)
            getProtocolProvider().getOperationSet(
                                OperationSetFileTransfer.class);
    }

    /**
     * Create an MSRP session and return its' <tt>SessionDescription</tt>.
     * @see CallPeerMediaHandlerSipImpl#createFirstOffer()
     */
    protected SessionDescription createFirstOffer()
        throws OperationFailedException
    {
        SessionDescription sDes = null;
        try
        {
            this.session = Session.create(false, false, getLastUsedLocalHost());
            SessionListener listener = new MsrpSessionListener(this);
            session.addListener(listener);
            sDes = SdpUtils.createMessageSessionDescription(session.getURI());
            if (isFileTransfer())
            {
                addFileTransferOffer(sDes);
            }
            localSess = sDes;
        }
        catch (InternalErrorException iee)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Error creating SIMPLE(MSRP) session, check your network",
                OperationFailedException.INTERNAL_ERROR, iee, logger);
        }
        return sDes;
    }
    /**
     * Make an sdp offer, referring to the file to transfer. 
     * @param sdp   the descrption to add the transfer offer to
     * @throws InternalErrorException
     */
    private void addFileTransferOffer(SessionDescription sdp)
        throws InternalErrorException
    {
        try
        {
            if (getTransferActivity() == null)
                throw new InternalErrorException(
                    "Weird: transferring file but no activity??");
            MediaDescription m = (MediaDescription)
                sdp.getMediaDescriptions(false).firstElement();
            m.setAttribute(MediaDirection.SENDONLY.toString(), "");
            m.setAttribute(SdpUtils.FILE_SELECTOR, getTransferActivity().toString());
            m.setAttribute(SdpUtils.TRANSFER_ID, getTransferActivity().getID());
        }
        catch (SdpException e)
        {
            throw new InternalErrorException(e);
        }
    }

    /**
     * Specialised version of {@link TransportManager#getLastUsedLocalHost()}
     * @return address to use for this connection.
     */
    private InetAddress getLastUsedLocalHost()
    {
        NetworkAddressManagerService nam
            = ProtocolMediaActivator.getNetworkAddressManagerService();
        InetAddress intendedDestination = getProtocolProvider()
                    .getIntendedDestination(getPeerAddress()).getAddress(); 

        return nam.getLocalHost(intendedDestination);
    }

    /**
     * Inspect INVITE and trigger file transfer when offered. 
     * @return  whether file transfer is triggered.
     */
    protected boolean isFileTransferTriggered()
    {
        Request invite = getLatestInviteTransaction().getRequest();
        ContentLengthHeader clh = invite.getContentLength();
        if (clh != null && clh.getContentLength() > 0)
        {
            try
            {
                SessionDescription sdp = SdpUtils
                    .parseSdpString(SdpUtils.getContentAsString(invite));
                //TODO: handle multiple "m=" lines...
                MediaDescription mdes = (MediaDescription)
                    sdp.getMediaDescriptions(false).firstElement();
                if (SdpUtils.hasFileSelector(mdes))
                {
                    opsetFileTransfer.handleTransferRequest(this, mdes);
                    this.isFileTransfer = true;
                }
            }
            catch (Exception e)
            {
                logger.warn("Error parsing file transfer SDP: ", e);
            }
        }
        return isFileTransfer;
    }

    /**
     * Create MSRP session and return its' <tt>SessionDescription</tt>.
     * @see CallPeerMediaHandlerSipImpl#processFirstOffer(
     *              javax.sdp.SessionDescription)
     */
    protected SessionDescription processFirstOffer(SessionDescription offer)
        throws OperationFailedException,
               IllegalArgumentException
   {
        SessionDescription sDes = null;
        try
        {
            //TODO: handle multiple "m=" lines...
            MediaDescription mdes = (MediaDescription)
                offer.getMediaDescriptions(false).firstElement();
            URI toURI = SdpUtils.getFromPath(mdes);
            this.session = Session.create(false, false, toURI,
                                          getLastUsedLocalHost());
            SessionListener listener = new MsrpSessionListener(this);
            session.addListener(listener);
            sDes = SdpUtils.createMessageSessionDescription(session.getURI());
            if (SdpUtils.hasFileSelector(mdes))
            {
                logger.debug("We're going to do a file transfer...");
                SdpUtils.copyFile2Receive(sDes, mdes);
                localSess = sDes;
            }
            else
            {
                localSess = sDes;
                opsetBasicIM.addSession(this);
            }
        }
        catch (Exception e)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Error creating (MSRP) session, check your network",
                OperationFailedException.INTERNAL_ERROR, e, logger);
        }
        return sDes;
   }

    /**
     * Create or complete media connection using supplied session description.
     * @param answer    session description received as answer to offer.
     * @throws IllegalArgumentException
     * @throws OperationFailedException
     */
    protected void processMsrpAnswer(String answer)
        throws  IllegalArgumentException,
                OperationFailedException
    {
        doNonSynchronisedProcessAnswer(SdpUtils.parseSdpString(answer));
    }

    /**
     * Complete session with peer.
     * @see CallPeerMediaHandlerSipImpl#doNonSynchronisedProcessAnswer(
     *              javax.sdp.SessionDescription)
     */
    protected void doNonSynchronisedProcessAnswer(SessionDescription answer)
        throws  OperationFailedException,
                IllegalArgumentException
    {
        /* TODO: -when MSRP implementation is more elaborate- possibly
         * scan for changed or added paths and forward these changes to the
         * library. For now, use a one-shot.
         */
        if (toList != null)
            return;
        Vector<MediaDescription> media =
                        SdpUtils.extractMediaDescriptions(answer);
        if (media.size() > 0) {
            try
            {
                toList = new ArrayList<URI>(media.size());
                for (MediaDescription md : media) {
                    toList.add(
                        new URI(md.getAttribute(SdpUtils.PATH_ATTRIBUTE)));
                }
                session.setToPath(toList);
                if (isFileTransfer())
                {
                    session.sendMessage(
                        getTransferActivity().getContentType(),
                        getTransferActivity().getLocalFile());
                    /*
                     * TODO: should really be IN_PROGRESS but no update
                     *      result/callback defined yet.
                     */
                    getTransferActivity().fireStatusChangeEvent(
                        FileTransferStatusChangeEvent.COMPLETED);
                }
            }
            catch (Exception e)
            {
                ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Remote party probably sent an invalid SDP answer.",
                     OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
            }
        }
    }

    /**
     * Send given message over MSRP session and signal its' delivery.
     * @param message the message to send
     */
    public void sendMessage(
                    net.java.sip.communicator.service.protocol.Message message)
    {
        this.session.sendWrappedMessage(
            javax.net.msrp.wrap.cpim.Message.WRAP_TYPE,
            session.getURI().toString(),
            getContact().getAddress(),
            message.getContentType(), message.getRawData());
        opsetBasicIM.messageDelivered(getContact(), message);
    }

    /**
     * @see net.java.sip.communicator.service.protocol.media.
     *              CallPeerMediaHandler#close()
     */
    public synchronized void close()
    {
        this.session.tearDown();
        if (isFileTransfer())
            try
            {
                hangup();
            }
            catch (OperationFailedException e)
            {
                logger.warn("Error closing file transfer: ", e);
            }
    }

    /**
     * @return Basic IM operation set
     */
    protected OperationSetBasicInstantMessagingMsrpImpl getOpsetBasicIM()
    {
        return opsetBasicIM;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.protocol.sip.CallPeerSipImpl#invite()
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
                    createOffer(),
                    contentTypeHeader);

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
     * Specialised version of {@link CallPeerMediaHandlerSipImpl#createOffer()}
     */
    public String createOffer()
        throws OperationFailedException
    {
        if (localSess == null)
            return createFirstOffer().toString();
        else
            return ""; // TODO: createUpdateOffer(localSess).toString();
    }

    public void processInviteOK(ClientTransaction clientTransaction,
        Response ok)
    {
        try
        {
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
                processMsrpAnswer(SdpUtils.getContentAsString(ok));
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
        if (!CallPeerState.isOnHold(getState()))
        {
            setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.protocol.sip.CallPeerSipImpl#answer()
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
                        + "that is already connected.");
            return;
        }
        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Request invite = serverTransaction.getRequest();
        Response ok = null;
        try
        {
            ok = getMessageFactory().createResponse(Response.OK, invite);
        }
        catch (ParseException ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            contentTypeHeader = getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp");
        }
        catch (ParseException ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a content type header for the OK response",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        /*
         * This is the sdp offer that came from the initial invite,
         * also that invite can have no offer.
         */
        String sdpOffer = null;
        try
        {
            ContentLengthHeader cl = invite.getContentLength();
            if (cl != null && cl.getContentLength() > 0)
            {
                sdpOffer = SdpUtils.getContentAsString(invite);
            }

            String sdp;
            // if the offer was in the invite, create an SDP answer
            if ((sdpOffer != null) && (sdpOffer.length() > 0))
            {
                sdp = processOffer(sdpOffer);
            }
            // if there was no offer in the invite - create an offer
            else
            {
                sdp = createOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (Exception ex)
        {
            /*
             * log the error and tell remote party. Don't throw exceptions, it
             * would go to the stack and there's nothing it could do with it.
             */
            logger.error(
                "Failed to create an SDP description for an OK response "
                    + "to an INVITE request!", ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);

            //do not continue processing - we already cancelled the peer here
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
     * Specialised version of {@link CallPeerMediaHandlerSipImpl#
     *              processOffer(String)}
     */
    public String processOffer(String offerString)
        throws OperationFailedException,
               IllegalArgumentException
    {
        SessionDescription offer = SdpUtils.parseSdpString(offerString);

        if (localSess == null)
            return processFirstOffer(offer).toString();
        else
            return ""; //TODO: processUpdateOffer(offer, localSess).toString();
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.impl.protocol.sip.CallPeerSipImpl#
     * processAck(javax.sip.ServerTransaction, javax.sip.message.Request)
     */
    public void processAck(ServerTransaction serverTransaction, Request ack)
    {
        ContentLengthHeader clh = ack.getContentLength();
        if ((clh != null) && (clh.getContentLength() > 0))
        {
            try
            {
                processMsrpAnswer(SdpUtils.getContentAsString(ack));
            }
            catch (Exception exc)
            {
                logAndFail("There was an error parsing the SDP description of "
                            + getDisplayName() + "(" + getAddress() + ")", exc);
                return;
            }
        }
        CallPeerState peerState = getState();
        if (!CallPeerState.isOnHold(peerState))
        {
            setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
    }

    /**
     * @return whether this session is a file transfer.
     */
    protected boolean isFileTransfer()
    {
        return isFileTransfer;
    }

    /**
     * @return the transferActivity
     */
    protected FileTransferImpl getTransferActivity()
    {
        return transferActivity;
    }

    /**
     * @param transferActivity the transferActivity to set
     */
    protected void setTransferActivity(FileTransferImpl transferActivity)
    {
        if (transferActivity != null)
        {
            this.isFileTransfer = true;
            transferActivity.setHandler(this);
        }
        this.transferActivity = transferActivity;
    }
}
