/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

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

    /**
     * Creates an instance of <tt>CallPeerGibberishImpl</tt> by specifying the
     * call peer <tt>address</tt> and the parent <tt>owningCall</tt>.
     * @param address the address of the peer
     * @param owningCall the parent call
     */
    public CallPeerGibberishImpl(String address, CallGibberishImpl owningCall)
    {
        this.peerAddress = address;
        this.call = owningCall;

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());

        final Random random = new Random();

        Timer timer = new Timer(false);
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                fireStreamSoundLevelEvent(random.nextInt(255));
            }
        }, 500, 100);

        // Make this peer a conference focus.
        if (owningCall.getCallPeerCount() > 1)
        {
            this.setConferenceFocus(true);
            final ConferenceMemberGibberishImpl member1
                = new ConferenceMemberGibberishImpl(this);
            member1.setDisplayName("Dragancho");
            this.addConferenceMember(member1);

            final ConferenceMemberGibberishImpl member2
                = new ConferenceMemberGibberishImpl(this);
            member2.setDisplayName("Ivancho");
            this.addConferenceMember(member2);

            Timer timer1 = new Timer(false);
            timer1.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    fireConferenceMembersSoundLevelEvent(
                        new HashMap<ConferenceMember, Integer>()
                            {
                                {
                                    put(member1, new Integer(random.nextInt(255)));
                                    put(member2, new Integer(random.nextInt(255)));
                                }
                        });
                }
            }, 500, 100);
        }
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
}
