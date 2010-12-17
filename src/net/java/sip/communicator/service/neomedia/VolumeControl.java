/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Control for volume level in media service.
 * @see InputVolumeControl
 * @see OutputVolumeControl
 *
 * @author Damian Minkov
 */
public interface VolumeControl
{
    /**
     * Current volume value.
     * @return the current volume level.
     */
    public float getVolume();

    /**
     * Returns the minimum allowed volume value.
     * @return the minimum allowed volume value.
     */
    public float getMinValue();

    /**
     * Returns the maximum allowed volume value.
     * @return the maximum allowed volume value.
     */
    public float getMaxValue();

    /**
     * Changes volume level.
     * @param value the new level to set.
     * @return the actual level which was set.
     */
    public float setVolume(float value);

    /**
     * Mutes current sound playback.
     * @param mute mutes/unmutes playback.
     */
    public void setMute(boolean mute);

    /**
     * Get mute state of sound playback.
     * @return mute state of sound playback.
     */
    public boolean getMute();

    /**
     * Adds a <tt>VolumeChangeListener</tt> to be informed for any change
     * in the volume levels.
     *
     * @param listener volume change listener.
     */
    public void addVolumeChangeListener(VolumeChangeListener listener);

    /**
     * Removes a <tt>VolumeChangeListener</tt>.
     * @param listener the volume change listener to be removed.
     */
    public void removeVolumeChangeListener(VolumeChangeListener listener);
}
