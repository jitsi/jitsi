/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An implementation of the <tt>CallPeer</tt> abstract class for the common part
 * of Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractCallPeerJabberGTalkImpl
        <T extends AbstractCallJabberGTalkImpl<?>,
        U extends AbstractCallPeerMediaHandlerJabberGTalkImpl<?>>
    extends MediaAwareCallPeer<
        T,
        U,
        ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractCallPeerJabberGTalkImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractCallPeerJabberGTalkImpl.class);

    /**
     * The jabber address of this peer
     */
    protected String peerJID = null;

    /**
     * Any discovery information that we have for this peer.
     */
    private DiscoverInfo discoverInfo;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the Jabber or Google Talk address of the new call
     * peer.
     * @param owningCall the call that contains this call peer.
     */
    protected AbstractCallPeerJabberGTalkImpl(
            String peerAddress,
            T owningCall)
    {
        super(owningCall);

        this.peerJID = peerAddress;
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
     * Retrives the DiscoverInfo for a given peer identified by its URI.
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
}
