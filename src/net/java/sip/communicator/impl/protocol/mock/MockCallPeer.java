/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Damian Minkov
 */
public class MockCallPeer
    extends AbstractCallPeer
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

    public MockCallPeer(String address, MockCall owningCall)
    {
        this.participantAddress = address;
        this.call = owningCall;

        call.addCallPeer(this);

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
    public String getPeerID()
    {
        return participantID;
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
        /** @todo implement getContact() */
        return null;
    }

    /**
     * Returns the protocol provider that this participant belongs to.
     * @return a reference to the ProtocolProviderService that this participant
     * belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.call.getProtocolProvider();
    }

}
