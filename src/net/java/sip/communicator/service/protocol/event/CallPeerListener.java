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
