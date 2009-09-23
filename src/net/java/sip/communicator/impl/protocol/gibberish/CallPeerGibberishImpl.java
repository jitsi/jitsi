/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A Gibberish implementation of the <tt>CallPeer</tt> interface.
 * @author Yana Stamcheva
 */
public class CallPeerGibberishImpl
    extends AbstractCallPeer
{
    /**
     * The sip address of this peer
     */
    private String peerAddress = null;

    /**
     * The call peer belongs to.
     */
    private CallGibberishImpl call;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    public CallPeerGibberishImpl(String address, CallGibberishImpl owningCall)
    {
        this.peerAddress = address;
        this.call = owningCall;

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());

        ConferenceMemberGibberishImpl member1
            = new ConferenceMemberGibberishImpl(this);
        member1.setDisplayName("conference member1");
        member1.setState(ConferenceMemberState.CONNECTED);

        ConferenceMemberGibberishImpl member2
            = new ConferenceMemberGibberishImpl(this);
        member2.setDisplayName("conference member2");
        member2.setState(ConferenceMemberState.CONNECTED);

        this.addConferenceMember(member1);
        this.addConferenceMember(member2);
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return peerAddress;
    }

    /**
     * Returns a reference to the call that this peer belongs to.
     *
     * @return a reference to the call containing this peer.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        return peerAddress;
    }

    /**
     * The method returns an image representation of the call peer
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
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        /** @todo implement getContact() */
        return null;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.call.getProtocolProvider();
    }

    public void addCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
    }

    public void removeCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
    }
}
