/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import org.jivesoftware.smackx.jingle.*;

/**
 * Our Jabber implementation of the default CallParticipant;
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class CallParticipantJabberImpl
    extends AbstractCallParticipant
{

    /**
     * The jabber address of this participant
     */
    private String participantAddress = null;

    /**
     * A byte array containing the image/photo representing the call participant.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the participant.
     */
    private String participantID;

    /**
     * The call this participant belongs to.
     */
    private CallJabberImpl call;

    /**
     * The jingle session that has been created by the application for
     * communication with this call participant.
     */
    private JingleSession jingleSession = null;

    /**
     * Creates a new call participant with address <tt>participantAddress</tt>.
     *
     * @param participantAddress the Jabber address of the new call
     * participant.
     * @param owningCall the call that contains this call participant.
     */
    public CallParticipantJabberImpl(String participantAddress,
                                     CallJabberImpl owningCall)
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
        return participantAddress;
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

        if(participantAddress.equals(address))
            return;

        this.participantAddress = address;
        //Fire the Event
        fireCallParticipantChangeEvent(
                CallPeerChangeEvent.CALL_PARTICIPANT_ADDRESS_CHANGE,
                oldAddress,
                address.toString());
    }

    /**
     * Returns a human readable name representing this participant.
     *
     * @return a String containing a name for that participant.
     */
    public String getDisplayName()
    {
        if (call != null)
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            OperationSetPresence opSetPresence = (OperationSetPresence) pps
                    .getOperationSet(OperationSetPresence.class);

            Contact cont = opSetPresence.findContactByID(getAddress());
            if (cont != null)
            {
                return cont.getDisplayName();
            }
        }
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
                CallPeerChangeEvent.CALL_PARTICIPANT_IMAGE_CHANGE,
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
    /*public String getSdpDescription()
    {
        return sdpDescription;
    }*/

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
    protected void setCall(CallJabberImpl call)
    {
        this.call = call;
    }

    /**
     * Sets the jingle session that has been created by the application for
     * communication with this call participant.
     * @param session the jingle session that has been created by the
     * application for this call.
     */
    public void setJingleSession(JingleSession session)
    {
        this.jingleSession = session;
    }

    /**
     * Returns the jingle session that has been created by the application for
     * communication with this call participant.
     *
     * @return the jingle session that has been created by the application for
     * communication with this call participant.
     */

    public JingleSession getJingleSession()
    {
        return jingleSession;
    }

    /**
     * Returns the protocol provider that this participant belongs to.
     * @return a reference to the ProtocolProviderService that this participant
     * belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.getCall().getProtocolProvider();
    }

    /**
     * Returns the contact corresponding to this participant or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this participant or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        ProtocolProviderService pps = call.getProtocolProvider();
        OperationSetPresence opSetPresence = (OperationSetPresence) pps
                .getOperationSet(OperationSetPresence.class);

        return opSetPresence.findContactByID(getAddress());
    }
}
