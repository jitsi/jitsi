/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Our Jabber implementation of the default CallPeer;
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class CallPeerJabberImpl
    extends AbstractCallPeer<CallJabberImpl, ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerJabberImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(CallPeerJabberImpl.class.getName());

    /**
     * The jabber address of this peer
     */
    private String peerJID = null;

    /**
     * A byte array containing the image/photo representing the call peer.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the peer. The reason we are keeping an ID
     * of our own rather than using the Jingle session id is that we can't
     * guarantee uniqueness of of jingle SIDs from one client to the next.
     */
    private String peerID;

    /**
     * The call this peer belongs to.
     */
    private CallJabberImpl call;

    /**
     * The session ID of the Jingle session associated with this call.
     */
    private final String jingleSID;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Jabber address of the new call peer.
     * @param owningCall the call that contains this call peer.
     * @param jingleSID the ID of the session that we are maintaining with this
     * peer.
     */
    public CallPeerJabberImpl(String         peerAddress,
                              CallJabberImpl owningCall,
                              String         jingleSID)
    {
        this.peerJID = peerAddress;
        this.call = owningCall;
        this.jingleSID = jingleSID;

        call.addCallPeer(this);

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return peerJID;
    }

    /**
     * Specifies the address, phone number, or other protocol specific
     * identifier that represents this call peer. This method is to be
     * used by service users and MUST NOT be called by the implementation.
     *
     * @param address The address of this call peer.
     */
    public void setAddress(String address)
    {
        String oldAddress = getAddress();

        if(peerJID.equals(address))
            return;

        this.peerJID = address;
        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_ADDRESS_CHANGE,
                oldAddress,
                address.toString());
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        if (call != null)
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            OperationSetPresence opSetPresence
                = pps.getOperationSet(OperationSetPresence.class);

            Contact cont = opSetPresence.findContactByID(getAddress());
            if (cont != null)
            {
                return cont.getDisplayName();
            }
        }
        return peerJID;
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
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call peer.
     *
     * @param image a byte array containing the image
     */
    protected void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_IMAGE_CHANGE,
                oldImage,
                image);
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
     * Returns the latest sdp description that this peer sent us.
     * @return the latest sdp description that this peer sent us.
     */
    /*public String getSdpDescription()
    {
        return sdpDescription;
    }*/

    /**
     * Sets the String that serves as a unique identifier of this
     * CallPeer.
     * @param peerID the ID of this call peer.
     */
    protected void setPeerID(String peerID)
    {
        this.peerID = peerID;
    }

    /**
     * Returns a reference to the call that this peer belongs to. Calls
     * are created by underlying telephony protocol implementations.
     *
     * @return a reference to the call containing this peer.
     */
    public CallJabberImpl getCall()
    {
        return call;
    }

    /**
     * Sets the call containing this peer.
     *
     * @param call the call that this call peer is participating in.
     */
    protected void setCall(CallJabberImpl call)
    {
        this.call = call;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    public ProtocolProviderServiceJabberImpl getProtocolProvider()
    {
        return this.getCall().getProtocolProvider();
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
        ProtocolProviderService pps = call.getProtocolProvider();
        OperationSetPresence opSetPresence
            = pps.getOperationSet(OperationSetPresence.class);

        return opSetPresence.findContactByID(getAddress());
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addStreamSoundLevelListener(
        SoundLevelListener listener)
    {

    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to remove
     */
    public void removeStreamSoundLevelListener(
        SoundLevelListener listener)
    {

    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list
     * of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {

    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the
     * list of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to
     * remove
     */
    public void removeConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {

    }


    ////////////////////////////// OK CODE starts here ////////////////////////

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangup()
        throws OperationFailedException
    {
        // do nothing if the call is already ended
        if (CallPeerState.DISCONNECTED.equals(getState())
            || CallPeerState.FAILED.equals(getState()))
        {
            if (logger.isDebugEnabled())
                logger.debug("Ignoring a request to hangup a call peer "
                        + "that is already DISCONNECTED");
            return;
        }

        //get a reference to the provider before we change the state to
        //DISCONNECTED because at that point we may lose our Call reference
        ProtocolProviderServiceJabberImpl provider = getProtocolProvider();

        CallPeerState prevPeerState = getState();
        setState(CallPeerState.DISCONNECTED);
        JingleIQ responseIQ = null;

        if (prevPeerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(prevPeerState))
        {
            responseIQ = JinglePacketFactory.createBye(
                provider.getOurJID(), peerJID, jingleSID);
        }
        else if (CallPeerState.CONNECTING.equals(getState())
            || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState())
            || CallPeerState.ALERTING_REMOTE_SIDE.equals(getState()))
        {
            responseIQ = JinglePacketFactory.createCancel(
                provider.getOurJID(), peerJID, jingleSID);
        }
        else if (prevPeerState.equals(CallPeerState.INCOMING_CALL))
        {
            responseIQ = JinglePacketFactory.createBusy(
                provider.getOurJID(), peerJID, jingleSID);
        }
        else if (prevPeerState.equals(CallPeerState.BUSY)
                 || prevPeerState.equals(CallPeerState.FAILED))
        {
            // For FAILED and BUSY we only need to update CALL_STATUS
            // as everything else has been done already.
        }
        else
        {
            logger.info("Could not determine call peer state!");
        }

        if (responseIQ != null)
            provider.getConnection().sendPacket(responseIQ);
    }

    /**
     * Returns the session ID of the Jingle session associated with this call.
     *
     * @return the session ID of the Jingle session associated with this call.
     */
    public String getJingleSID()
    {
        return jingleSID;
    }
}
