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
public class SoundLevelChangeEvent extends EventObject
{
    /**
     * Creates a new instance of a <tt>SoundLevelChangeEvent</tt> for the
     * specified source stream and level mappings.
     * @param source
     */
    public SoundLevelChangeEvent(MediaStream source,
                                 Hashtable<String, Integer> levels)
    {
        super(source);

    }

    public MediaStream getSourceMediaStream()
    {
        return (MediaStream)getSource();
    }
}
