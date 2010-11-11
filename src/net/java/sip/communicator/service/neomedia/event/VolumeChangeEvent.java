/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import net.java.sip.communicator.service.neomedia.*;

import java.util.*;

/**
 * Represents the event fired when playback volume value has changed.
 * 
 * @author Damian Minkov
 */
public class VolumeChangeEvent
    extends EventObject
{
    /**
     * Current level of volume.
     */
    private float level;

    /**
     * Is volume muted.
     */
    private boolean mute;

    /**
     * Constructs a VolumeChangeEvent with current values.
     *
     * @param source Volume control from which the change comes.
     * @param level volume level.
     * @param mute is muted.
     * @throws IllegalArgumentException if source is null.
     */
    public VolumeChangeEvent(VolumeControl source, float level, boolean mute)
    {
        super(source);

        this.level = level;
        this.mute = mute;
    }

    /**
     * The source control which has changed the volume.
     * @return the volume control.
     */
    public VolumeControl getSourceVolumeControl()
    {
        return (VolumeControl)getSource();
    }

    /**
     * Current volume level.
     * @return current volume level.
     */
    public float getLevel()
    {
        return level;
    }

    /**
     * Is current volume muted.
     * @return is current volume muted.
     */
    public boolean getMute()
    {
        return mute;
    }
}
