package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.CallParticipantChangeEvent;

/**
 * <p> </p>
 *
 * <p> </p>
 *
 * <p> </p>
 *
 * <p> </p>
 *
 * @author Damian Minkov
 */
public class MockCallParticipant
    extends AbstractCallParticipant
{
    /**
     * The sip address of this participant
     */
    private String participantAddress = null;

    /**
     * The call participant belongs to.
     */
    private MockCall call;

    /**
     * A string uniquely identifying the participant.
     */
    private String participantID;

    /**
     * Indicates the date when  is call participant passed into its current state.
     */
    protected Date currentStateStartDate = new Date();

    /**
     * The state of the call participant.
     */
    protected CallParticipantState callParticipantState =
                                                   CallParticipantState.UNKNOWN;


    public MockCallParticipant(String address, MockCall owningCall)
    {
        this.participantAddress = address;
        this.call = owningCall;

        call.addCallParticipant(this);

        //create the uid
        this.participantID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());
    }

    /**
     * Returns a String locator for that participant.
     *
     * @return the participant's address or phone number.
     */
    public String getAddress()
    {
        return participantAddress;
    }

    /**
     * Returns a reference to the call that this participant belongs to.
     *
     * @return a reference to the call containing this participant.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns the date (time) when this call participant acquired its
     * current status.
     *
     * @return a java.util.Date object containing the date when this call
     *   participant entered its current state.
     */
    public Date getCurrentStateStartDate()
    {
        return currentStateStartDate;
    }

    /**
     * Returns a human readable name representing this participant.
     *
     * @return a String containing a name for that participant.
     */
    public String getDisplayName()
    {
        return participantAddress;
    }

    /**
     * The method returns an image representation of the call participant
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image
     *   is available.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns a unique identifier representing this participant.
     *
     * @return an identifier representing this call participant.
     */
    public String getParticipantID()
    {
        return participantID;
    }

    /**
     * Returns an object representing the current state of that participant.
     *
     * @return a CallParticipantState instance representin the participant's
     *   state.
     */
    public CallParticipantState getState()
    {
        return callParticipantState;
    }

    /**
     * Causes this CallParticipant to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallParticipantChangeEvent.
     *
     * @param newState the state this call participant should enter.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    protected void setState(CallParticipantState newState, String reason)
    {
        CallParticipantState oldState = getState();

        if(oldState == newState)
            return;

        this.callParticipantState = newState;
        this.currentStateStartDate = new Date();
        fireCallParticipantChangeEvent(
                CallParticipantChangeEvent.CALL_PARTICIPANT_STATE_CHANGE,
                oldState,
                newState);
    }
}
