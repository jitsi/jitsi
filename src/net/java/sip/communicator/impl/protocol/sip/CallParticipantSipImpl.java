/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;
import javax.sip.address.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import javax.sip.*;

/**
 * Our SIP implementation of the default CallParticipant;
 *
 * @author Emil Ivov
 */
public class CallParticipantSipImpl
    extends AbstractCallParticipant
{
    private static final Logger logger
        = Logger.getLogger(CallParticipantSipImpl.class);

    /**
     * The sip address of this participant
     */
    private Address participantAddress = null;

    /**
     * The state of the call participant.
     */
    protected CallParticipantState callParticipantState =
                                                   CallParticipantState.UNKNOWN;
    /**
     * Indicates the date when  is call participant passed into its current state.
     */
    protected Date currentStateStartDate = new Date();

    /**
     * A byte array containing the image/photo representing the call participant.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the participant.
     */
    private String participantID;

    /**
     * The call participant belongs to.
     */
    private CallSipImpl call;

    /**
     * The JAIN SIP dialog that has been created by the application for
     * communication with this call participant.
     */
    private Dialog jainSipDialog = null;

    /**
     * The SDP session description that we have received from this call
     * participant.
     */
    private String sdpDescription = null;

    /**
     * The SIP transaction that established this call. This was previously kept
     * in the jain-sip dialog but got deprected there so we're now keeping it
     * here.
     */
    private Transaction firstTransaction = null;

    /**
     * The jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call participant.
     */
    private SipProvider jainSipProvider = null;

    /**
     * Creates a new call participant with address <tt>participantAddress</tt>.
     *
     * @param participantAddress the JAIN SIP <tt>Address</tt> of the new call
     * participant.
     * @param owningCall the call that contains this call participant.
     */
    public CallParticipantSipImpl(Address participantAddress,
                                  CallSipImpl    owningCall)
    {
        this.participantAddress = participantAddress;
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
        return this.participantAddress.toString();
    }

    /**
     * Specifies the address, phone number, or other protocol specific
     * identifier that represents this call participant. This method is to be
     * used by service users and MUST NOT be called by the implementation.
     *
     * @param address The address of this call participant.
     */
    public void setAddress(Address address)
    {
        String oldAddress = getAddress();

        if(participantAddress.equals(address))
            return;

        this.participantAddress = address;
        //Fire the Event
        fireCallParticipantChangeEvent(
                CallParticipantChangeEvent.CALL_PARTICIPANT_ADDRESS_CHANGE,
                oldAddress,
                address.toString());
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

    /**
     * Causes this CallParticipant to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallParticipantChangeEvent.
     *
     * @param newState the state this call participant should enter.
     */
    protected void setState(CallParticipantState newState)
    {
        setState(newState, null);
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
        return participantAddress.getDisplayName();
    }

    /**
     * Sets a human readable name representing this participant.
     *
     * @param displayName the participant's display name
     */
    protected void setDisplayName(String displayName)
    {
        String oldName = getDisplayName();
        try
        {
            this.participantAddress.setDisplayName(displayName);
        }
        catch (ParseException ex)
        {
            //couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        //Fire the Event
        fireCallParticipantChangeEvent(
                CallParticipantChangeEvent.CALL_PARTICIPANT_DISPLAY_NAME_CHANGE,
                oldName,
                displayName);
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
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call participant.
     *
     * @param image a byte array containing the image
     */
    protected void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallParticipantChangeEvent(
                CallParticipantChangeEvent.CALL_PARTICIPANT_IMAGE_CHANGE,
                oldImage,
                image);
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
     * Returns the latest sdp description that this participant sent us.
     * @return the latest sdp description that this participant sent us.
     */
    public String getSdpDescription()
    {
        return sdpDescription;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * CallParticipant.
     * @param participantID the ID of this call participant.
     */
    protected void setParticipantID(String participantID)
    {
        this.participantID = participantID;
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
    protected void setCall(CallSipImpl call)
    {
        this.call = call;
    }

    /**
     * Sets the sdp description for this call participant.
     *
     * @param sdpDescription the sdp description for this call participant.
     */
    public void setSdpDescription(String sdpDescription)
    {
        this.sdpDescription = sdpDescription;
    }

    /**
     * Returns the javax.sip Address of this call participant.
     * @return the javax.sip Address of this call participant.
     */
    public Address getJainSipAddress()
    {
        return participantAddress;
    }

    /**
     * Sets the JAIN SIP dialog that has been created by the application for
     * communication with this call participant.
     * @param dialog the JAIN SIP dialog that has been created by the
     * application for this call.
     */
    public void setDialog(Dialog dialog)
    {
        this.jainSipDialog = dialog;
    }

    /**
     * Returns the JAIN SIP dialog that has been created by the application for
     * communication with this call participant.
     *
     * @return the JAIN SIP dialog that has been created by the application for
     * communication with this call participant.
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
    public void setFirstTransaction(Transaction transaction)
    {
        this.firstTransaction = transaction;
    }

    /**
     * Returns the transaction instance that contains the INVITE which started
     * this call.
     *
     * @return the Transaction that initiated this call.
     */
    public Transaction getFirstTransaction()
    {
        return firstTransaction;
    }

    /**
     * Sets the jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call participant.
     *
     * @param jainSipProvider the <tt>SipProvider</tt> that serves this call
     * participant.
     */
    public void setJainSipProvider(SipProvider jainSipProvider)
    {
        this.jainSipProvider = jainSipProvider;
    }

    /**
     * Returns the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call participant.
     *
     * @return the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call participant.
     */
    public SipProvider getJainSipProvider()
    {
        return jainSipProvider;
    }
}
