/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.media.*;

/**
 * A {@link TransportManager} implementation that would use ICE for
 * candidate management.
 *
 * @author Emil Ivov
 */
public class IceTransportManagerSipImpl
    extends TransportManagerSipImpl
{
    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeerMediaHandler} whose traffic we
     * will be taking care of.
     */
    public IceTransportManagerSipImpl(CallPeerSipImpl callPeer)
    {
        super(callPeer);
        iceAgent = createIceAgent();
        iceAgent.addStateChangeListener(this);
    }
}
