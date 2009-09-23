/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The event triggered when the sound level of a call peer has changed.
 *
 * @author Dilshan Amadoru
 */
public class CallPeerSoundLevelEvent
    extends EventObject
{
    /**
     * The new sound level indicator.
     */
    private int soundLevel;

    /**
     * Create an event instance.
     *
     * @param source The object that triggered this event
     * @param level The new sound level
     */
    public CallPeerSoundLevelEvent(Object source, int level)
    {
        super(source);

        this.soundLevel = level;
    }

    /**
     * Returns the new sound level.
     * @return the new sound level
     */
    public int getSoundLevel()
    {
        return soundLevel;
    }
}
