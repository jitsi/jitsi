/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>AudioStreamLevelEvent</tt>s are triggered whenever a change occurs in the
 * sound level of the audio stream coming from a certain <tt>CallPeer</tt>.
 * <p>
 * In the case of a <tt>CallPeer</tt>, which is also a conference focus and
 * is participating in the conference as a <tt>ConferenceMember</tt> the level
 * would be the aggregated level of all <tt>ConferenceMember</tt>s levels
 * including the one corresponding to the peer itself.
 * <p>
 * In the case of a <tt>CallPeer</tt>, which is also a conference focus, but
 * is NOT participating in the conference as a <tt>ConferenceMember</tt>
 * (server) the level would be the aggregated level of all attached
 * <tt>ConferenceMember</tt>s.
 *
 * @author Yana Stamcheva
 */
public class StreamSoundLevelEvent
    extends EventObject
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
     * The audio stream level, for the change of which this event is about.
     */
    private final int level;

    /**
     * Creates an <tt>StreamSoundLevelEvent</tt> for the given <tt>callPeer</tt>
     * by indicating the current sound level of the audio stream.
     * 
     * @param callPeer the <tt>CallPeer</tt> from which the stream is received
     * @param level the current sound level of the audio stream
     */
    public StreamSoundLevelEvent(   CallPeer callPeer,
                                    int level)
    {
        super(callPeer);

        this.level = level;
    }

    /**
     * Returns the <tt>CallPeer</tt> from which the stream is received.
     * @return the <tt>CallPeer</tt> from which the stream is received
     */
    public CallPeer getSourcePeer()
    {
        return (CallPeer) getSource();
    }

    /**
     * Returns the current sound level of the audio stream.
     * @return the current sound level of the audio stream
     */
    public int getLevel()
    {
        return level;
    }
}
