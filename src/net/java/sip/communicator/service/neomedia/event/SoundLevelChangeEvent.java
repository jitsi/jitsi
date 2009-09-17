/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * <tt>SoundLevelChangeEvent</tt>s are distributed by <tt>MediaStream</tt>s to
 * <tt>SoundLevelChangeListener</tt> during conference calls and only by some
 * mixers which support the feature. A single event instance contains the new
 * sound level values for one or more participants being mixed by the remote
 * party. Every participant is represented by an SSRC <tt>String</tt> identifier
 * which corresponds to the SSRC field that it is using when communicating with
 * the mixer. If a certain SSRC identifier is reported in a particular
 * <tt>SoundLevelChangeEvent</tt> and is not present in a following instance
 * should be interpreted as a sound-level value of 0 for that participant.
 * <p>
 * Listeners should assume that the absence of sound level events indicates the
 * absence of changes in the sound level of all known conference members.
 *
 * @author Emil Ivov
 */
public class SoundLevelChangeEvent
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
     * The hashtable mapping SSRC identifiers to sound levels.
     */
    private final Map<String, Integer> levels;

    /**
     * Creates a new instance of a <tt>SoundLevelChangeEvent</tt> for the
     * specified source stream and level mappings.
     *
     * @param source
     * @param levels
     */
    public SoundLevelChangeEvent(MediaStream source,
                                 Map<String, Integer> levels)
    {
        super(source);

        this.levels = levels;
    }

    /**
     * Returns a reference to the <tt>MediaStream</tt> that this event is
     * pertaining to.
     *
     * @return a reference to the <tt>MediaStream</tt> that this event is
     * pertaining to.
     */
    public MediaStream getSourceMediaStream()
    {
        return (MediaStream)getSource();
    }

    /**
     * Returns a hashtable mapping SSRC identifiers to sound levels. The table
     * contains the SSRC identifiers of all participants that whose sound levels
     * have changed to non-zero values. All known participants that are not
     * reported in this table are assumed to have zero values of their levels.
     *
     * @return a hashtable mapping SSRC identifiers to sound levels.
     */
    public Map<String, Integer> getLevels()
    {
        return levels;
    }
}
