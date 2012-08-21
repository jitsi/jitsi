/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An implementation of the <tt>CallPeer</tt> abstract class for the common part
 * of Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 * @author Lyubomir Marinov
 */
public abstract class AbstractCallPeerJabberGTalkImpl
        <T extends AbstractCallJabberGTalkImpl<?>,
         U extends AbstractCallPeerMediaHandlerJabberGTalkImpl<?>>
    extends MediaAwareCallPeer<T, U, ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractCallPeerJabberGTalkImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractCallPeerJabberGTalkImpl.class);

    /**
     * Any discovery information that we have for this peer.
     */
    private DiscoverInfo discoverInfo;

    /**
     * The indicator which determines whether this peer was initiated the
     * session.
     */
    protected boolean initiator = false;

    /**
     * The jabber address of this peer
     */
    protected String peerJID;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Jabber or Google Talk address of the new call
     * peer.
     * @param owningCall the call that contains this call peer.
     */
    protected AbstractCallPeerJabberGTalkImpl(String peerAddress, T owningCall)
    {
        super(owningCall);

        this.peerJID = peerAddress;
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
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        ProtocolProviderService pps = getCall().getProtocolProvider();
        OperationSetPresence opSetPresence
            = pps.getOperationSet(OperationSetPresence.class);

        return opSetPresence.findContactByID(getAddress());
    }

    /**
     * Returns the service discovery information that we have for this peer.
     *
     * @return the service discovery information that we have for this peer.
     */
    public DiscoverInfo getDiscoverInfo()
    {
        return discoverInfo;
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        if (getCall() != null)
        {
            Contact contact = getContact();

            if (contact != null)
                return contact.getDisplayName();
        }
        return peerJID;
    }

    /**
     * Returns full URI of the address.
     *
     * @return full URI of the address
     */
    public String getURI()
    {
        return "xmpp:" + peerJID;
    }

    /**
     * Determines whether this peer initiated the session. Note that if this
     * peer is the initiator of the session, then we are the responder!
     *
     * @return <tt>true</tt> if this peer initiated the session; <tt>false</tt>,
     * otherwise (i.e. if _we_ initiated the session).
     */
    public boolean isInitiator()
    {
        return initiator;
    }

    /**
     * Retrieves the DiscoverInfo for a given peer identified by its URI.
     *
     * @param calleeURI The URI of the call peer.
     * @param ppsJabberImpl The call protocol provider service.
     *
     * @return The retrieved DiscoverInfo, or null if not available.
     */
    protected void retrieveDiscoverInfo(String calleeURI)
    {
        DiscoverInfo tmpDiscoverInfo = null;
        try
        {
            tmpDiscoverInfo = this.getCall().getProtocolProvider()
                .getDiscoveryManager().discoverInfo(calleeURI);
            if(tmpDiscoverInfo != null)
            {
                this.setDiscoverInfo(tmpDiscoverInfo);
            }
        }
        catch (XMPPException ex)
        {
            logger.warn("could not retrieve info for " + calleeURI, ex);
        }
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
        if (!peerJID.equals(address))
        {
            String oldAddress = getAddress();

            peerJID = address;

            fireCallPeerChangeEvent(
                    CallPeerChangeEvent.CALL_PEER_ADDRESS_CHANGE,
                    oldAddress,
                    address);
        }
    }

    /**
     * Sets the service discovery information that we have for this peer.
     *
     * @param discoverInfo the discovery information that we have obtained for
     * this peer.
     */
    public void setDiscoverInfo(DiscoverInfo discoverInfo)
    {
        this.discoverInfo = discoverInfo;
    }
}
