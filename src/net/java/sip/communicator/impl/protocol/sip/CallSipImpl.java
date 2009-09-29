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
import javax.sip.address.URI;//disambiguates java.net.URI
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends Call
    implements CallPeerListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private final List<CallPeerSipImpl> callPeers =
        new Vector<CallPeerSipImpl>();

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
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param parentOpSet a reference to the operation set that's creating us
     * and that we would be able to use for even dispatching.
     */
    protected CallSipImpl(OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(parentOpSet.getProtocolProvider());
        this.messageFactory = getProtocolProvider().getMessageFactory();
        this.parentOpSet = parentOpSet;

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
    }

    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    private void addCallPeer(CallPeerSipImpl callPeer)
    {
        if (callPeers.contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        this.callPeers.add(callPeer);
        fireCallPeerEvent(callPeer,
            CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerSipImpl callPeer)
    {
        if (!callPeers.contains(callPeer))
            return;

        this.callPeers.remove(callPeer);
        callPeer.removeCallPeerListener(this);

        try
        {
            fireCallPeerEvent(callPeer,
                CallPeerEvent.CALL_PEER_REMVOVED);
        }
        finally
        {

            /*
             * The peer should loose its state once it has finished
             * firing its events in order to allow the listeners to undo.
             */
            callPeer.setCall(null);
        }

        if (callPeers.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<CallPeer> getCallPeers()
    {
        return new LinkedList<CallPeer>(callPeers).iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     *         associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {
        //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerTransportAddressChanged(
        CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Verifies whether the call peer has entered a state.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeerState newState =
            (CallPeerState) evt.getNewValue();
        if (newState == CallPeerState.DISCONNECTED
            || newState == CallPeerState.FAILED)
        {
            removeCallPeer((CallPeerSipImpl) evt
                .getSourceCallPeer());
        }
        else if ((newState == CallPeerState.CONNECTED
               || newState == CallPeerState.CONNECTING_WITH_EARLY_MEDIA))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    /**
     * Returns <tt>true</tt> if <tt>dialog</tt> matches the jain sip dialog
     * established with one of the peers in this call.
     *
     * @param dialog the dialog whose corresponding peer we're looking for.
     *
     * @return true if this call contains a call peer whose jain sip
     * dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallPeer(dialog) != null;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're looking
     * for.
     * @return the call peer whose jain sip dialog is the same as the specified
     * or null if no such call peer was found.
     */
    public CallPeerSipImpl findCallPeer(Dialog dialog)
    {
        Iterator<CallPeer> callPeers = this.getCallPeers();

        if (logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                + "among " + this.callPeers.size() + " calls");
        }

        while (callPeers.hasNext())
        {
            CallPeerSipImpl cp =
                (CallPeerSipImpl) callPeers.next();

            if (cp.getDialog() == dialog)
            {
                logger.trace("Returing cp=" + cp);
                return cp;
            }
            else
            {
                logger.trace("Ignoring cp=" + cp + " because cp.dialog="
                    + cp.getDialog() + " while dialog=" + dialog);
            }
        }

        return null;
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     *
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     */
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return (ProtocolProviderServiceSipImpl)super.getProtocolProvider();
    }

    /**
     * Returns a reference to the <tt>OperationSetBasicTelephonySipImpl</tt>
     * instance that created this call.
     *
     * @return a reference to the <tt>OperationSetBasicTelephonySipImpl</tt>
     * instance that created this call.
     */
    public OperationSetBasicTelephonySipImpl getParentOperationSet()
    {
        return parentOpSet;
    }

    /**
     * Creates a <tt>CallPeerSipImpl</tt> from <tt>calleeAddress</tt> and sends
     * them an invite request. The invite request will be initialized according
     * to any relevant parameters in the <tt>cause</tt> message (if different
     * from <tt>null</tt>) that is the reason for creating this call.
     *
     * @param calleeAddress the party that we would like to invite to this call.
     * @param cause the message (e.g. a Refer request), that is the reason for
     * this invite or <tt>null</tt> if this is a user-initiated invitation
     *
     * @return the newly created <tt>CallPeer</tt> corresponding to
     * <tt>calleeAddress</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException  with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerSipImpl invite(Address                   calleeAddress,
                                  javax.sip.message.Message cause)
        throws OperationFailedException
    {
        // create the invite request
        Request invite = messageFactory
            .createInviteRequest(calleeAddress, cause);

        // Transaction
        ClientTransaction inviteTransaction = null;
        SipProvider jainSipProvider
            = getProtocolProvider().getDefaultJainSipProvider();
        try
        {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create inviteTransaction.\n"
                    + "This is most probably a network connection error.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        // create the call peer
        CallPeerSipImpl callPeer =
            createCallPeerFor(inviteTransaction, jainSipProvider);

        // invite content
        attachSdpOffer(invite, callPeer);

        try
        {
            inviteTransaction.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + invite);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An error occurred while sending invite request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }

        return callPeer;
    }

    /**
     * Creates an SDP offer destined to <tt>callPeer</tt> and attaches it to
     * the <tt>invite</tt> request.
     *
     * @param invite the invite <tt>Request</tt> that we'd like to attach an
     * SDP offer to.
     * @param callPeer the <tt>callPeer</tt> that we'd like to address our
     * offer to
     *
     * @throws OperationFailedException if we fail constructing the session
     * description.
     */
    private void attachSdpOffer(Request invite, CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession(callPeer.getCall());
            callPeer.setMediaCallSession(callSession);

            callSession.setSessionCreatorCallback(callPeer);

            // if possible try to indicate the address of the callee so
            // that the media service can choose the most proper local
            // address to advertise.
            URI calleeURI = callPeer.getJainSipAddress().getURI();

            if (calleeURI.isSipURI())
            {
                // content type should be application/sdp (not applications)
                // reported by Oleg Shevchenko (Miratech)
                ContentTypeHeader contentTypeHeader = getProtocolProvider()
                    .getHeaderFactory().createContentTypeHeader(
                        "application", "sdp");

                String host = ((SipURI) calleeURI).getHost();
                InetAddress intendedDestination = getProtocolProvider()
                        .resolveSipAddress(host).getAddress();

                invite.setContent(callSession
                            .createSdpOffer(intendedDestination),
                            contentTypeHeader);
            }
        }
        catch (UnknownHostException ex)
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

    }

    /**
     * Creates a new call peer associated with <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     *
     * @return a new instance of a <tt>CallPeerSipImpl</tt> corresponding
     * to the <tt>containingTransaction</tt>.
     */
    private CallPeerSipImpl createCallPeerFor(
        Transaction containingTransaction, SipProvider sourceProvider)
    {
        CallPeerSipImpl callPeer = new CallPeerSipImpl(
                containingTransaction.getDialog().getRemoteParty(),
                this, containingTransaction, sourceProvider);
        addCallPeer(callPeer);

        boolean incomingCall
            = (containingTransaction instanceof ServerTransaction);
        callPeer.setState( incomingCall
                        ? CallPeerState.INCOMING_CALL
                        : CallPeerState.INITIATING_CALL);

        // notify everyone
        parentOpSet.fireCallEvent( (incomingCall
                                    ? CallEvent.CALL_RECEIVED
                                    : CallEvent.CALL_INITIATED),
                                   this);

        return callPeer;
    }


    /**
     * Processes an incoming INVITE that is meant to replace an existing
     * <tt>CallPeerSipImpl</tt> that is participating in this call. Typically
     * this would happen as a result of an attended transfer.
     *
     * @param jainSipProvider the JAIN-SIP <tt>SipProvider</tt> that received
     * the request.
     * @param serverTransaction the transaction containing the INVITE request.
     * @param callPeerToReplace a reference to the <tt>CallPeer</tt> that this
     * INVITE is trying to replace.
     */
    public void processReplacingInvite(SipProvider       jainSipProvider,
                                       ServerTransaction serverTransaction,
                                       CallPeerSipImpl   callPeerToReplace)
    {
        CallPeerSipImpl newCallPeer
                    = createCallPeerFor(serverTransaction, jainSipProvider);
        try
        {
            newCallPeer.answer();
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to auto-answer the referred call peer "
                    + newCallPeer, ex);
            /*
             * RFC 3891 says an appropriate error response MUST be returned
             * and callPeerToReplace must be left unchanged.
             */
            //TODO should we send a response here?
            return;
        }



        //we just accepted the new peer and if we got here then it went well
        //now let's hangup the other call.
        try
        {
            callPeerToReplace.hangup();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to hangup the referer "
                            + callPeerToReplace, ex);
            callPeerToReplace.setState(
                            CallPeerState.FAILED, "Internal Error: " + ex);
        }

    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param jainSipProvider the provider containing
     * <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     *
     * @return CallPeerSipImpl the newly created call peer (the one that sent
     * the INVITE).
     */
    public CallPeerSipImpl processInvite(SipProvider       jainSipProvider,
                    ServerTransaction serverTransaction)
{
        Request invite = serverTransaction.getRequest();

        CallPeerSipImpl peer
            = createCallPeerFor(serverTransaction, jainSipProvider);

        // extract the SDP description.
        // beware: SDP description may be in ACKs so it could be that there's
        // nothing here - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            peer.setSdpDescription(new String(invite.getRawContent()));
        }

        //send a ringing response
        Response response = null;
        try
        {
            logger.trace("will send ringing response: ");
            response = messageFactory.createResponse(Response.RINGING, invite);
            serverTransaction.sendResponse(response);
            logger.debug("sent a ringing response: " + response);
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            peer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return peer;
        }

        return peer;
    }

    /**
     * Logs <tt>message</tt> and <tt>cause</tt> and sets <tt>peer</tt> state
     * to <tt>CallPeerState.FAILED</tt>
     *
     * @param message a message to log and display to the user.
     * @param throwable the exception that cause the error we are logging
     * @param peer the peer that caused the error and that we are failing.
     */
    public static void logAndFailCallPeer(String message,
        Throwable throwable, CallPeerSipImpl peer)
    {
        logger.error(message, throwable);
        peer.setState(CallPeerState.FAILED, message);
    }
}
