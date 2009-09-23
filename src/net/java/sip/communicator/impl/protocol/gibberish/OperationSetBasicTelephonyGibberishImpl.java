/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A Gibberish implementation of a basic telephony operation set.
 * @author Yana Stamcheva
 */
public class OperationSetBasicTelephonyGibberishImpl
    extends AbstractOperationSetBasicTelephony
    implements CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicTelephonyGibberishImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private ProtocolProviderServiceGibberishImpl protocolProvider = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable<String, Call> activeCalls = new Hashtable<String, Call>();

    public OperationSetBasicTelephonyGibberishImpl(
        ProtocolProviderServiceGibberishImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     *
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void answerCallPeer(CallPeer peer) throws
        OperationFailedException
    {
        CallPeerGibberishImpl callPeer
            = (CallPeerGibberishImpl) peer;
        if(peer.getState().equals(CallPeerState.CONNECTED))
        {
            logger.info("Ignoring user request to answer a CallPeer "
                        + "that is already connected. CP:" + peer);
            return;
        }

        callPeer.setState(CallPeerState.CONNECTED, null);
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     *   call.
     * @return CallPeer the CallPeer that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we
     *   fail to create the call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     *   string.
     */
    public Call createCall(String uri) throws OperationFailedException,
        ParseException
    {
        return createNewCall(uri);
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
     * @throws OperationFailedException with the corresponding code if we
     *   fail to create the call.
     */
    public Call createCall(Contact callee) throws OperationFailedException
    {
        return createNewCall(callee.getAddress());
    }

    private Call createNewCall(String address)
    {
        CallGibberishImpl newCall = new CallGibberishImpl(protocolProvider);

        newCall.addCallChangeListener(this);
        activeCalls.put(newCall.getCallID(), newCall);

        CallPeerGibberishImpl callPeer
            = new CallPeerGibberishImpl(address, newCall);

        newCall.addCallPeer(callPeer);

        this.fireCallEvent(CallEvent.CALL_INITIATED, newCall);

        return newCall;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return Iterator
     */
    public Iterator<Call> getActiveCalls()
    {
        return activeCalls.values().iterator();
    }

    /**
     * Indicates a user request to end a call with the specified call
     * particiapnt.
     *
     * @param peer the peer that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void hangupCallPeer(CallPeer peer) throws
        OperationFailedException
    {
        //do nothing if the call is already ended
        if (peer.getState().equals(CallPeerState.DISCONNECTED))
        {
            logger.debug("Ignoring a request to hangup a call peer "
                         +"that is already DISCONNECTED");
            return;
        }

        CallPeerGibberishImpl callPeer
            = (CallPeerGibberishImpl) peer;

        logger.info("hangupCallPeer");
        callPeer.setState(CallPeerState.DISCONNECTED, null);
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOffHold(CallPeer peer)
    {

    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOnHold(CallPeer peer) throws
        OperationFailedException
    {
    }

    public Call receiveCall(String fromAddress)
        throws Exception
    {
        Call newCall = createCall(fromAddress);
        fireCallEvent(CallEvent.CALL_RECEIVED, newCall);

        return newCall;
    }

    public Call placeCall(String toAddress)
        throws Exception
    {
        Call newCall = createCall(toAddress);
        fireCallEvent(CallEvent.CALL_INITIATED, newCall);

        // must have one peer
        CallPeerGibberishImpl callPArt =
            (CallPeerGibberishImpl) newCall.getCallPeers().next();

        callPArt.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");
        callPArt.setState(CallPeerState.CONNECTED, "no reason");

        return newCall;
    }

    public CallPeer addNewCallPeer(Call call, String address)
    {
        CallPeerGibberishImpl callPArt
            = new CallPeerGibberishImpl(address, (CallGibberishImpl) call);

        callPArt.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");
        callPArt.setState(CallPeerState.CONNECTED, "no reason");

        return callPArt;
    }

    public void callPeerAdded(CallPeerEvent evt)
    {
    }

    public void callPeerRemoved(CallPeerEvent evt)
    {
    }

    public void callStateChanged(CallChangeEvent evt)
    {
        if(evt.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE)
           && ((CallState)evt.getNewValue()).equals(CallState.CALL_ENDED))
        {
            CallGibberishImpl sourceCall = (CallGibberishImpl) this.activeCalls
                .remove(evt.getSourceCall().getCallID());

            logger.trace(  "Removing call " + sourceCall + " from the list of "
                         + "active calls because it entered an ENDED state");

            fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
        }
    }
}
