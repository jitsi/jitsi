package net.java.sip.communicator.impl.protocol.mock;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A mock implementation of a basic telephony opearation set
 *
 * @author Damian Minkov
 */
public class MockOperationSetBasicTelephony
    extends AbstractOperationSetBasicTelephony
    implements CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(MockOperationSetBasicTelephony.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private MockProvider protocolProvider = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable activeCalls = new Hashtable();


    public MockOperationSetBasicTelephony(MockProvider protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallParticipant.
     *
     * @param participant the call participant that we'd like to anwer.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void answerCallPeer(CallPeer participant) throws
        OperationFailedException
    {
        MockCallParticipant callParticipant
            = (MockCallParticipant)participant;
        if(participant.getState().equals(CallPeerState.CONNECTED))
        {
            logger.info("Ignoring user request to answer a CallParticipant "
                        + "that is already connected. CP:" + participant);
            return;
        }

        callParticipant.setState(CallPeerState.CONNECTED, null);
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param uri the address of the callee that we should invite to a new
     *   call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
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
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param callee the address of the callee that we should invite to a
     *   new call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
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
        MockCall newCall = new MockCall(protocolProvider);

        newCall.addCallChangeListener(this);
        activeCalls.put(newCall.getCallID(), newCall);

        new MockCallParticipant(address, newCall);

        return newCall;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return Iterator
     */
    public Iterator getActiveCalls()
    {
        return activeCalls.values().iterator();
    }

    /**
     * Indicates a user request to end a call with the specified call
     * particiapnt.
     *
     * @param participant the participant that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     */
    public void hangupCallPeer(CallPeer participant) throws
        OperationFailedException
    {
        //do nothing if the call is already ended
        if (participant.getState().equals(CallPeerState.DISCONNECTED))
        {
            logger.debug("Ignoring a request to hangup a call participant "
                         +"that is already DISCONNECTED");
            return;
        }

        MockCallParticipant callParticipant
            = (MockCallParticipant)participant;

        logger.info("hangupCallParticipant");
        callParticipant.setState(CallPeerState.DISCONNECTED, null);
    }

    /**
     * Resumes communication with a call participant previously put on hold.
     *
     * @param participant the call participant to put on hold.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOffHold(CallPeer participant)
    {

    }

    /**
     * Puts the specified CallParticipant "on hold".
     *
     * @param participant the participant that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *   encounter an error while performing this operation.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public void putOnHold(CallPeer participant) throws
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

        // must have one participant
        MockCallParticipant callPArt =
            (MockCallParticipant)newCall.getCallPeers().next();

        callPArt.setState(CallPeerState.ALERTING_REMOTE_SIDE, "no reason");
        callPArt.setState(CallPeerState.CONNECTED, "no reason");

        return newCall;
    }

    public CallPeer addNewCallParticipant(Call call, String address)
    {
        MockCallParticipant callPArt = new MockCallParticipant(address, (MockCall)call);

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
            MockCall sourceCall = (MockCall)this.activeCalls
                .remove(evt.getSourceCall().getCallID());

            logger.trace(  "Removing call " + sourceCall + " from the list of "
                         + "active calls because it entered an ENDED state");

            fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
        }
    }
}
