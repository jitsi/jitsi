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
     * The maximum level that can be reported for a participant in a conference.
     * Level values should be distributed among MAX_LEVEL and MIN_LEVEL in a
     * way that would appear uniform to users.
     */
    public static final int MAX_LEVEL = 255;

    /**
     * The maximum (zero) level that can be reported for a participant in a
     * conference. Level values should be distributed among MAX_LEVEL and
     * MIN_LEVEL in a way that would appear uniform to users.
     */
    public static final int MIN_LEVEL = 0;

    /**
     * Indicates a new audio level for the source that this listener was
     * registered with.
     * @param level the new/current level of the audio source that this
     * listener is registered with.
     */
    public void audioLevelChanged(int level);
}
