/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;


/**
 * Receives events notifying of changes that have occurred within a
 * <tt>CallPeer</tt>. Such changes may pertain to current call
 * peer state, their display name, address, image and (possibly in the
 * future) others.
 *
 * @author Emil Ivov
 */
public interface CallPeerListener
    extends EventListener
{

    /**
     * Indicates that a change has occurred in the status of the source
     * CallPeer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void peerStateChanged(CallPeerChangeEvent evt);

    /**
     * Indicates that a change has occurred in the display name of the source
     * CallPeer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new display names.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt);

    /**
     * Indicates that a change has occurred in the address of the source
     * CallPeer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new address.
     */
    public void peerAddressChanged(CallPeerChangeEvent evt);

    /**
     * Indicates that a change has occurred in the transport address that we
     * use to communicate with the peer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new transport address.
     */
    public void peerTransportAddressChanged(
                                        CallPeerChangeEvent evt);

    /**
     * Indicates that a change has occurred in the image of the source
     * CallPeer.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new image.
     */
    public void peerImageChanged(CallPeerChangeEvent evt);
}
