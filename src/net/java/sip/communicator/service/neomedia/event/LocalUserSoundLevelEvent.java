/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Yana Stamcheva
 */
public class LocalUserSoundLevelEvent
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
     * @param source the source of the new <tt>LocalUserSoundLevelEvent</tt>.
     * @param level the current sound level of the audio stream
     */
    public LocalUserSoundLevelEvent(Object source, int level)
    {
        super(source);
        this.level = level;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>.
     * @return the <tt>ProtocolProviderService</tt>
     */
    public ProtocolProviderService getSourceProvider()
    {
        return (ProtocolProviderService) getSource();
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
