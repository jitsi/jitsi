/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;
import javax.sip.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends Call
    implements CallParticipantListener
{
    /**
     * A list containing all <tt>CallParticipant</tt>s of this call.
     */
    private Vector callParticipants = new Vector();

    /**
     * The state that this call is currently in.
     */
    private CallState callState = CallState.CALL_INITIALIZATION;

    /**
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param sourceProvider the ProtocolProviderServiceSipImpl instance in the
     * context of which this call has been created.
     */
    protected CallSipImpl(ProtocolProviderServiceSipImpl sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Adds <tt>callParticipant</tt> to the list of participants in this call.
     * If the call participant is already included in the call, the method has
     * no effect.
     *
     * @param callParticipant the new <tt>CallParticipant</tt>
     */
    public void addCallParticipant(CallParticipantSipImpl callParticipant)
    {
        if(callParticipants.contains(callParticipant))
            return;

        callParticipant.addCallParticipantListener(this);

        this.callParticipants.add(callParticipant);
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
    public void removeCallParticipant(CallParticipantSipImpl callParticipant)
    {
        if(!callParticipants.contains(callParticipant))
            return;

        this.callParticipants.remove(callParticipant);
        callParticipant.setCall(null);
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

    /**
     * Returns the state that this call is currently in.
     *
     * @return a reference to the <tt>CallState</tt> instance that the call is
     * currently in.
     */
    public CallState getCallState()
    {
        return callState;
    }

    /**
     * Returns an iterator over all call participants.
     * @return an Iterator over all participants currently involved in the call.
     */
    public Iterator getCallParticipants()
    {
        return callParticipants.iterator();
    }

    /**
     * Returns the number of participants currently associated with this call.
     * @return an <tt>int</tt> indicating the number of participants currently
     * associated with this call.
     */
    public int getCallParticipantsCount()
    {
        return callParticipants.size();
    }

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Verifies whether the call participant has entered a state.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        if(((CallParticipantState)evt.getNewValue())
                     == CallParticipantState.DISCONNECTED
            || ((CallParticipantState)evt.getNewValue())
                     == CallParticipantState.FAILED)
        {
            removeCallParticipant(
                (CallParticipantSipImpl)evt.getSourceCallParticipant());
        }
    }

    /**
     * Returns <tt>true</tt> if <tt>dialog</tt> matches the jain sip dialog
     * established with one of the participants in this call.
     *
     * @param dialog the dialog whose corresponding participant we're looking
     * for.
     * @return true if this call contains a call participant whose jain sip
     * dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallParticipant(dialog) != null;
    }

    /**
     * Returns the call participant whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding participant we're
     * looking for.
     * @return the call participant whose jain sip dialog is the same as the
     * specified or null if no such call participant was found.
     */
    public CallParticipantSipImpl findCallParticipant(Dialog dialog)
    {
        Iterator callParticipants = this.getCallParticipants();

        while (callParticipants.hasNext())
        {
            CallParticipantSipImpl cp
                = (CallParticipantSipImpl)callParticipants.next();

            if( cp.getDialog() == dialog)
                return cp;
        }

        return null;
    }
}
