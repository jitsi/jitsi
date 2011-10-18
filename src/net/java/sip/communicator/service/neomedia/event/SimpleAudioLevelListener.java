/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * Level values should be distributed between <tt>MAX_LEVEL</tt> and
     * {@link #MIN_LEVEL} in a way that would appear uniform to users.
     * <p>
     * <b>Note</b>: The value of <tt>127</tt> is specifically chosen as the
     * value of <tt>MAX_LEVEL</tt> because (1) we transport the levels within
     * RTP and it gives us a signed <tt>byte</tt> for it, and (2) the range of
     * <code>[0, 127]</tt> is pretty good to directly express the sound pressure
     * level decibels as heard by humans in Earth's atmosphere.
     * </p>
     */
    public static final int MAX_LEVEL = 127;

    /**
     * The maximum (zero) level that can be reported for a participant in a
     * conference. Level values should be distributed among {@link #MAX_LEVEL}
     * and <tt>MIN_LEVEL</tt> in a way that would appear uniform to users.
     * <p>
     * <b>Note</b>: The value of <tt>0</tt> is specifically chosen as the value
     * of <tt>MIN_LEVEL</tt> because (1) we transport the levels within RTP and
     * it gives us a signed <tt>byte</tt> for it, and (2) the range of
     * <code>[0, 127]</tt> is pretty good to directly express the sound pressure
     * level decibels as heard by humans in Earth's atmosphere.
     * </p>
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
