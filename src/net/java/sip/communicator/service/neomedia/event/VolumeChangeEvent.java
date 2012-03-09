/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Represents the event fired when playback volume value has changed.
 *
 * @author Damian Minkov
 */
public class VolumeChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The volume level.
     */
    private final float level;

    /**
     * The indicator which determines whether the volume is muted.
     */
    private final boolean mute;

    /**
     * Initializes a new <tt>VolumeChangeEvent</tt> which is to notify about a
     * specific volume level and its mute state.
     *
     * @param source the <tt>VolumeControl</tt> which is the source of the
     * change
     * @param level the volume level
     * @param mute <tt>true</tt> if the volume is muted; otherwise,
     * <tt>false</tt>
     * @throws IllegalArgumentException if source is <tt>null</tt>
     */
    public VolumeChangeEvent(VolumeControl source, float level, boolean mute)
    {
        super(source);

        this.level = level;
        this.mute = mute;
    }

    /**
     * Gets the <tt>VolumeControl</tt> which is the source of the change
     * notified about by this <tt>VolumeChangeEvent</tt>.
     *
     * @return the <tt>VolumeControl</tt> which is the source of the change
     * notified about by this <tt>VolumeChangeEvent</tt>
     */
    public VolumeControl getSourceVolumeControl()
    {
        return (VolumeControl) getSource();
    }

    /**
     * Gets the volume level notified about by this <tt>VolumeChangeEvent</tt>.
     *
     * @return the volume level notified about by this
     * <tt>VolumeChangeEvent</tt>
     */
    public float getLevel()
    {
        return level;
    }

    /**
     * Gets the indicator which determines whether the volume is muted.
     *
     * @return <tt>true</tt> if the volume is muted; otherwise, <tt>false</tt>
     */
    public boolean getMute()
    {
        return mute;
    }
}
