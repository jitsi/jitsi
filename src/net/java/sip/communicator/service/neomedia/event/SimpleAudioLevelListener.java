/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

/**
 * A very simple listener that delivers <tt>int</tt> values every time the audio
 * level of an audio source changes.
 *
 * @author Emil Ivov
 */
public interface SimpleAudioLevelListener
{
    /**
     * Indicates a new audio level for the source that this listener was
     * registered with.
     * @param level the new/current level of the audio source that this
     * listener is registered with.
     */
    public void audioLevelChanged(int level);
}
