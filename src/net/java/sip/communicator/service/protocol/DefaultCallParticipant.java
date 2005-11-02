/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;


/**
 * The DefaultCallParticipant provides a default implementation for most of the
 * CallParticpant methods with the purpose of only leaving custom protocol
 * development to clients using the PhoneUI service.
 * <p> </p>
 *
 * @author Emil Ivov
 */
public class DefaultCallParticipant
    implements CallParticipant
{

    /**
     * All the CallParticipant listeners registered with this CallParticipant.
     */
    protected ArrayList callParticipantListeners = new ArrayList();

    /**
     * The address (sip address, phone number or other protocol specific
     * identifier) of this call participant.
     */
    protected String address = null;

    /**
     * The state of the call participant.
     */
    protected CallParticipantState callParticipantState =
                                                   CallParticipantState.UNKNOWN;
    /**
     * Indicates the date when this call participant passed into its current state.
     */
    protected Date currentStateStartDate = new Date();

    /**
     * A human readable name corresponding to the call participant.
     * (e.g. John Travolta)
     */
    private String displayName = null;

    /**
     * A byte array containing the image/photo representing the call participant.
     */
    protected byte[] image;

    /**
     * A string provided by the underlying implementationm uniquely identifying
     * the participant.
     */
    protected String participantID;

    /**
     * Specifies whether or not the participant is the one that initiated the
     * call.
     */
    protected boolean isCaller = false;

    /**
     * Returns a String identifying the call that this participant belongs to.
     * We have been thinking of returning an instance of a Call interface here
     * but this would mean too much to implement for users of this service.
     */
    protected Call call;

    /**
     * @param listener a listener instance to register with this participant.
     *
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public void addCallParticipantListener(CallParticipantListener listener)
    {
        this.callParticipantListeners.add(listener);
    }

    /**
     * Unregisters the specified listener.
     * @param listener the listener to unregister.
     */
    public void removeCallParticipantListener(CallParticipantListener listener)
    {
        if(listener == null)
            return;
        callParticipantListeners.remove(listener);
    }

    /**
     * Returns a String locator for that participant.
     *
     * @return the participant's address or phone number.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Specifies the address, phone number, or other protocol specific
     * identifier that represents this call participant. This method is to be
     * used by service users and MUST NOT be called by the implementation.
     *
     * @param address The address of this call participant.
     */
    public void setAddress(String address)
    {
        String oldAddress = getAddress();
        this.address = address;
        //Fire the Event
        fireCallParticipantChangeEvent(
            new CallParticipantChangeEvent(
                this,
                CallParticipantChangeEvent.CALL_PARTICIPANT_ADDRESS_CHANGE,
                oldAddress,
                address
            ));
    }

    /**
     * Returns an object representing the current state of that participant.
     *
     * @return a CallParticipantState instance representin the participant's
     *   state.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
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
     */
    public void enterState(CallParticipantState newState)
    {
        CallParticipantState oldState = getState();
        this.callParticipantState = newState;
        this.currentStateStartDate = new Date();
        fireCallParticipantChangeEvent(
            new CallParticipantChangeEvent(
                this,
                CallParticipantChangeEvent.CALL_PARTICIPANT_STATUS_CHANGE,
                oldState,
                newState));
    }

    /**
     * Notifies all registered CallParticipantListener-s of the specified
     * change event.
     *
     * @param evt the event to dispatch.
     */
    protected void fireCallParticipantChangeEvent(CallParticipantChangeEvent
                                                                            evt)
    {
        for (int i = 0; i < callParticipantListeners.size(); i++)
        {
            ((CallParticipantListener)callParticipantListeners.get(i))
                                                        .participantChange(evt);
        }
    }

    /**
     * Returns the date (time) when this call participant acquired its
     * current status.
     *
     * @return a java.util.Date object containing the date when this call
     *   participant entered its current state.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public Date getCurrentStateStartDate()
    {
        return currentStateStartDate;
    }

    /**
     * Returns a human readable name representing this participant.
     *
     * @return a String containing a name for that participant.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets a human readable name representing this participant.
     *
     * @param displayName the participant's display name
     */
    public void setDisplayName(String displayName)
    {
        String oldName = getDisplayName();
        this.displayName = displayName;

        //Fire the Event
        fireCallParticipantChangeEvent(
            new CallParticipantChangeEvent(
                this,
                CallParticipantChangeEvent.CALL_PARTICIPANT_DISPLAY_NAME_CHANGE,
                oldName,
                displayName
            ));
    }

    /**
     * The method returns an image representation of the call participant
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image
     *   is available.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call participant.
     *
     * @param image a byte array containing the image
     */
    public void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallParticipantChangeEvent(
            new CallParticipantChangeEvent(
                this,
                CallParticipantChangeEvent.CALL_PARTICIPANT_IMAGE_CHANGE,
                oldImage,
                image
            ));
    }

    /**
     * Returns a unique identifier representing this participant.
     *
     * @return an identifier representing this call participant.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public String getParticipantID()
    {
        return participantID;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * CallParticipant.
     * @param participantID the ID of this call participant.
     */
    public void setParticipantID(String participantID)
    {
        this.participantID = participantID;
    }

    /**
     * Sets this call participant to be (or not) the one that initiated the
     * current call.
     * @param isCaller a bool specifying whether or not the participant is a
     * caller.
     */
    public void setIsCaller(boolean isCaller)
    {
        this.isCaller = isCaller;
    }

    /**
     * Determines whether or not this is the participant that originated the
     * call (as opposed to the one that was called).
     *
     * @return true if this is the participant that calls us and falls if
     *   otherwise.
     * @todo Implement this
     *   net.java.sip.communicator.service.phoneui.CallParticipant method
     */
    public boolean isCaller()
    {
        return isCaller;
    }

    /**
     * Returns a reference to the call that this participant belongs to. Calls
     * are created by underlying telephony protocol implementations.
     *
     * @return a reference to the call containing this participant.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Sets the call containing this participant.
     * @param call the call that this call participant is
     * partdicipating in.
     */
    public void setCall(Call call)
    {
        this.call = call;
    }

    /**
     * Returns a string representation of the participant in the form of
     * <br>
     * Display Name <address>;status=CallParticipantStatus
     * @return a string representation of the participant and its state.
     */
    public String toString()
    {
        return getDisplayName() + " <" + getAddress()
            + ">;status=" + getState().getStateString();
    }
}
