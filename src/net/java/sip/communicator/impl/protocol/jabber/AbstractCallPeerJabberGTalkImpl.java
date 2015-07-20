/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
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
         U extends AbstractCallPeerMediaHandlerJabberGTalkImpl<?>,
         V extends IQ>
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
     * The indicator which determines whether this peer has initiated the
     * session.
     */
    protected boolean initiator = false;

    /**
     * The jabber address of this peer
     */
    protected String peerJID;

    /**
     * The {@link IQ} that created the session that this call represents.
     */
    protected V sessionInitIQ;

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
        OperationSetPresence presence
            = getProtocolProvider().getOperationSet(OperationSetPresence.class);

        return
            (presence == null) ? null : presence.findContactByID(getAddress());
    }

    /**
     * Returns the service discovery information that we have for this peer.
     *
     * @return the service discovery information that we have for this peer.
     */
    public DiscoverInfo getDiscoveryInfo()
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
     *
     * @return The retrieved DiscoverInfo, or null if not available.
     */
    protected void retrieveDiscoveryInfo(String calleeURI)
    {
        try
        {
            DiscoverInfo discoveryInfo
                = getProtocolProvider().getDiscoveryManager().discoverInfo(
                        calleeURI);

            if(discoveryInfo != null)
                setDiscoveryInfo(discoveryInfo);
        }
        catch (XMPPException xmppex)
        {
            logger.warn("Could not retrieve info for " + calleeURI, xmppex);
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
    public void setDiscoveryInfo(DiscoverInfo discoverInfo)
    {
        this.discoverInfo = discoverInfo;
    }

    /**
     * Returns the session ID of the Jingle session associated with this call.
     *
     * @return the session ID of the Jingle session associated with this call.
     */
    public abstract String getSID();

    /**
     * Returns the IQ ID of the Jingle session-initiate packet associated with
     * this call.
     *
     * @return the IQ ID of the Jingle session-initiate packet associated with
     * this call.
     */
    public String getSessInitID()
    {
        return sessionInitIQ != null ? sessionInitIQ.getPacketID() : null;
    }
}
