/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * The <tt>CallPeerSoundLevelListener</tt> notifies all interested parties when
 * a sound level change has appeared for a certain call peer.
 *
 * @author Dilshan Amadoru
 */
public interface CallPeerSoundLevelListener
{
    /**
     * Notifies all interested parties when a peer changes its sound level.
     * 
     * @param evt the <tt>PeerSoundLevelEvent</tt> triggered
     */
    public void peerSoundLevelChanged(CallPeerSoundLevelEvent evt);
}
