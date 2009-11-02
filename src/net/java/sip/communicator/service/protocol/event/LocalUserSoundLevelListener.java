/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * Notifies interested parties in sound level changes of the local user audio
 * stream.
 * @author Yana Stamcheva
 */
public interface LocalUserSoundLevelListener
{
    /**
     * Indicates that a change has occurred in the sound level of the local
     * user.
     * @param event the <tt>LocalUserSoundLevelEvent</tt> containing the new
     * level
     */
    public void localUserSoundLevelChanged(LocalUserSoundLevelEvent event);
}
