/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Notifies interested parties in sound level changes of the main audio stream
 * coming from a given <tt>CallPeer</tt>.
 * <p>
 * What does this mean in the different cases:
 * 1) In the case of a <tt>CallPeer</tt>, which is not a conference focus and
 * has no <tt>ConferenceMember</tt>s we would be notified of any change in the
 * sound level of the stream coming from this peer.
 * 2) In the case of a <tt>CallPeer</tt>, which is also a conference focus and
 * is participating in the conference as a <tt>ConferenceMember</tt> the level
 * would be the aggregated level of all <tt>ConferenceMember</tt>s levels
 * including the one corresponding to the peer itself.
 * 3) In the case of a <tt>CallPeer</tt>, which is also a conference focus, but
 * is NOT participating in the conference as a <tt>ConferenceMember</tt>
 * (server) the level would be the aggregated level of all
 * <tt>ConferenceMember</tt>s.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface SoundLevelListener
    extends EventListener
{

     /**
     * Indicates that a change has occurred in the audio stream level coming
     * from a given <tt>CallPeer</tt>. In the case of conference focus the audio
     * stream level would be the total level including all
     * <tt>ConferenceMember</tt>s levels.
     * <p>
     * In contrast to the conventions of Java and Jitsi,
     * <tt>SoundLevelListener</tt> does not fire an <tt>EventObject</tt> (i.e.
     * <tt>SoundLevelChangeEvent</tt>) in order to try to reduce the number of
     * allocations related to sound level changes since their number is expected
     * to be very large.
     * </p>
     *
     * @param source the <tt>Object</tt> which is the source of the sound level
     * change event being fired to this <tt>SoundLevelListener</tt>
     * @param level the sound level to notify this <tt>SoundLevelListener</tt>
     * about
     */
    public void soundLevelChanged(Object source, int level);
}
