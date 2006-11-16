package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class MockCall
    extends Call
    implements CallParticipantListener
{
    private static final Logger logger = Logger.getLogger(MockCall.class);

    /**
     * A list containing all <tt>CallParticipant</tt>s of this call.
     */
    private Vector callParticipants = new Vector();

    /**
     * The state that this call is currently in.
     */
    private CallState callState = CallState.CALL_INITIALIZATION;


    public MockCall(MockProvider sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Returns an iterator over all call participants.
     *
     * @return an Iterator over all participants currently involved in the
     *   call.
     */
    public Iterator getCallParticipants()
    {
        return callParticipants.iterator();
    }

    /**
     * Returns the number of participants currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of participants
     *   currently associated with this call.
     */
    public int getCallParticipantsCount()
    {
        return callParticipants.size();
    }

    /**
     * Returns the state that this call is currently in.
     *
     * @return a reference to the <tt>CallState</tt> instance that the call
     *   is currently in.
     */
    public CallState getCallState()
    {
        return callState;
    }

    /**
     * Adds <tt>callParticipant</tt> to the list of participants in this call.
     * If the call participant is already included in the call, the method has
     * no effect.
     *
     * @param callParticipant the new <tt>CallParticipant</tt>
     */
    public void addCallParticipant(MockCallParticipant callParticipant)
    {
        if(callParticipants.contains(callParticipant))
            return;

        callParticipant.addCallParticipantListener(this);

        this.callParticipants.add(callParticipant);

        logger.info("Will fire participant added");

        fireCallParticipantEvent(
            callParticipant, CallParticipantEvent.CALL_PARTICIPANT_ADDED);
    }

    /**
     * Removes <tt>callParticipant</tt> from the list of participants in this
     * call. The method has no effect if there was no such participant in the
     * call.
     *
     * @param callParticipant the <tt>CallParticipant</tt> leaving the call;
     */
    public void removeCallParticipant(MockCallParticipant callParticipant)
    {
        if(!callParticipants.contains(callParticipant))
            return;

        this.callParticipants.remove(callParticipant);
        callParticipant.removeCallParticipantListener(this);

        fireCallParticipantEvent(
            callParticipant, CallParticipantEvent.CALL_PARTICIPANT_REMVOVED);

        if(callParticipants.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Sets the state of this call and fires a call change event notifying
     * registered listenres for the change.
     *
     * @param newState a reference to the <tt>CallState</tt> instance that
     * the call is to enter.
     */
    public void setCallState(CallState newState)
    {
        CallState oldState = getCallState();

        if(oldState == newState)
            return;

        this.callState = newState;

        fireCallChangeEvent(
            CallChangeEvent.CALL_STATE_CHANGE, oldState, newState);
    }

    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        if ( ( (CallParticipantState) evt.getNewValue())
            == CallParticipantState.DISCONNECTED
            || ( (CallParticipantState) evt.getNewValue())
            == CallParticipantState.FAILED)
        {
            removeCallParticipant(
                (MockCallParticipant) evt.getSourceCallParticipant());
        }
        else if ( ( (CallParticipantState) evt.getNewValue())
                 == CallParticipantState.CONNECTED
                 && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {
    }

    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {
    }

    public void participantImageChanged(CallParticipantChangeEvent evt)
    {
    }

    public void participantTransportAddressChanged(CallParticipantChangeEvent
        evt)
    {
    }

}
