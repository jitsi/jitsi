/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.text.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * Our Jabber implementation of the default CallPeer;
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class CallPeerJabberImpl
    extends MediaAwareCallPeer<CallJabberImpl,
                               CallPeerMediaHandlerJabberImpl,
                               ProtocolProviderServiceJabberImpl>
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
     * The call this peer belongs to.
     */
    private CallJabberImpl call;

    /**
     * The session ID of the Jingle session associated with this call.
     */
    private final String jingleSID;

    /**
     * The {@link JingleIQ} that created the session that this call represents.
     */
    private final JingleIQ sessionInitIQ;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Jabber address of the new call peer.
     * @param owningCall the call that contains this call peer.
     * @param sessInitIQ the {@link JingleIQ} that initiated that session
     * represented by this peer.
     */
    public CallPeerJabberImpl(String         peerAddress,
                              CallJabberImpl owningCall,
                              JingleIQ       sessInitIQ)
    {
        super(owningCall);
        this.peerJID = peerAddress;
        this.jingleSID = sessInitIQ.getSID();
        this.sessionInitIQ = sessInitIQ;

        super.setMediaHandler( new CallPeerMediaHandlerJabberImpl(this) );
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
     * Indicates a user request to answer an incoming call from this
     * <tt>CallPeer</tt>.
     *
     * Sends an OK response to <tt>callPeer</tt>. Make sure that the call
     * peer contains an SDP description when you call this method.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response.
     */
    public synchronized void answer()
        throws OperationFailedException
    {
        // This is the SDP offer that came from the initial session-initiate,
        //contrary to sip we we are guaranteed to have content because XEP-0166
        //says: "A session consists of at least one content type at a time."
        ContentPacketExtension offer = sessionInitIQ
            .getContentForType(RtpDescriptionPacketExtension.class);

        ContentPacketExtension answer = getMediaHandler().processOffer(offer);

        JingleIQ response = JinglePacketFactory.createSessionAccept(
                sessionInitIQ.getTo(), sessionInitIQ.getFrom(),
                getJingleSID(), answer);

        //tell everyone we are connecting so that the audio notifications would
        //stop
        setState(CallPeerState.CONNECTING_INCOMING_CALL_WITH_MEDIA);
    }

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
