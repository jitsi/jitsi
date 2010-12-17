/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

import javax.media.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Controls media service volume input or output. If a playback volume level
 * is set we change it on all current players, as we synchronize volume
 * on all players. Implements interface exposed from media service, also
 * implements the interface used in the Renderer part of JMF and merges the two
 * functionalities to work as one.
 * 
 * @author Damian Minkov
 */
public abstract class AbstractVolumeControl
    implements VolumeControl,
               GainControl
{
    /**
     * The <tt>Logger</tt> used by the <tt>VolumeControlImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractVolumeControl.class);

    /**
     * The minimum volume level we can handle.
     */
    private static final float MIN_VOLUME_LEVEL = 0.0F;

    /**
     * The maximum volume level we can handle.
     */
    private static final float MAX_VOLUME_LEVEL = 1.0F;

    /**
     * The default volume level.
     */
    private static final float DEFAULT_VOLUME_LEVEL = 0.5F;

    /**
     * The <tt>VolumeChangeListener</tt> interested in volume change events
     * through VolumeControl Interface.
     */
    private List<VolumeChangeListener> volumeChangeListeners =
            new ArrayList<VolumeChangeListener>();

    /**
     * Listeners interested in volume change inside jmf.
     */
    private List<GainChangeListener> gainChangeListeners;

    /**
     * The current volume level.
     */
    private float currentVolumeLevel = DEFAULT_VOLUME_LEVEL;

    /**
     * Current mute state, by default we start unmuted.
     */
    private boolean currentMuteState = false;

    /**
     * Current level in db.
     */
    private float db;

    /**
     * The initial volume level, when this instance was created.
     */
    private float initialVolumeLevel = DEFAULT_VOLUME_LEVEL;

    /**
     * Creates volume control instance and initialise initial level value
     * if stored in config service.
     */
    AbstractVolumeControl()
    {
        // read initial level from config service if any
        String initialLevel =
            NeomediaActivator.getConfigurationService()
                .getString(getStoreLevelPropertyName());
        try
        {
            if(initialLevel != null)
            {
                currentVolumeLevel = Float.valueOf(initialLevel);
                initialVolumeLevel = currentVolumeLevel; 

                if(logger.isDebugEnabled())
                    logger.debug("Restore volume: "
                            + currentVolumeLevel);
            }
        }
        catch(Throwable t)
        {
            logger.warn("Error restoring volume", t);
        }
    }

    /**
     * Current volume value.
     *
     * @return the current volume level.
     *
     * @see net.java.sip.communicator.service.neomedia.VolumeControl
     */
    public float getVolume()
    {
        return currentVolumeLevel;
    }

    /**
     * Get the current gain set for this
     * object as a value between 0.0 and 1.0
     *
     * @return The gain in the level scale (0.0-1.0).
     *
     * @see javax.media.GainControl
     */
    public float getLevel()
    {
        return this.currentVolumeLevel;
    }

    /**
     * Returns the minimum allowed volume value.
     *
     * @return the minimum allowed volume value.
     *
     * @see net.java.sip.communicator.service.neomedia.VolumeControl
     */
    public float getMinValue()
    {
        return MIN_VOLUME_LEVEL;
    }

    /**
     * Returns the maximum allowed volume value.
     *
     * @return the maximum allowed volume value.
     *
     * @see net.java.sip.communicator.service.neomedia.VolumeControl
     */
    public float getMaxValue()
    {
        return MAX_VOLUME_LEVEL;
    }

    /**
     * Changes volume level.
     *
     * @param value the new level to set.
     * @return the actual level which was set.
     *
     * @see net.java.sip.communicator.service.neomedia.VolumeControl
     */
    public float setVolume(float value)
    {
        return this.setVolumeLevel(value);
    }

    /**
     * Set the gain using a floating point scale
     * with values between 0.0 and 1.0.
     * 0.0 is silence; 1.0 is the loudest
     * useful level that this <code>GainControl</code> supports.
     *
     * @param level The new gain value specified in the level scale.
     * @return The level that was actually set.
     *
     * @see javax.media.GainControl
     */
    public float setLevel(float level)
    {
        return this.setVolumeLevel(level);
    }

    /**
     * Internal implementation combining setting level from JMF
     * and from outside Media Service.
     *
     * @param value the new value, changed if different from current
     * volume settings.
     * @return the value that was changed or just the current one if
     * the same.
     */
    private float setVolumeLevel(float value)
    {
        if(this.currentVolumeLevel == value)
            return value;

        if(value < MIN_VOLUME_LEVEL)
            this.currentVolumeLevel = MIN_VOLUME_LEVEL;
        else if(value > MAX_VOLUME_LEVEL)
            this.currentVolumeLevel = MAX_VOLUME_LEVEL;
        else
            this.currentVolumeLevel = value;

        fireVolumeChange();

        // save the level change, so we can restore it on next run
        NeomediaActivator.getConfigurationService().setProperty(
                getStoreLevelPropertyName(),
                String.valueOf(currentVolumeLevel));

        float f1 = value / initialVolumeLevel;
        db = (float)((Math.log((double)f1 != 0.0D ?
                f1
                : 0.0001D) / Math.log(10D)) * 20D);
        
        fireGainEvents();

        return this.currentVolumeLevel;
    }

    /**
     * Mutes current sound.
     *
     * @param mute mutes/unmutes.
     */
    public void setMute(boolean mute)
    {
        if(mute == this.currentMuteState)
            return;

        this.currentMuteState = mute;

        fireVolumeChange();

        fireGainEvents();
    }

    /**
     * Get mute state of sound.
     *
     * @return mute state of sound.
     */
    public boolean getMute()
    {
        return this.currentMuteState;
    }

    /**
     * Set the gain in decibels.
     * Setting the gain to 0.0 (the default) implies that the audio
     * signal is neither amplified nor attenuated.
     * Positive values amplify the audio signal and negative values attenuate
     * the signal.
     *
     * @param gain The new gain in dB.
     * @return The gain that was actually set.
     *
     * @see javax.media.GainControl
     */
    public float setDB(float gain)
    {
        if(this.db != gain)
        {
            this.db = gain;
            float f1 = (float)Math.pow(10D, (double)this.db / 20D);
            this.currentVolumeLevel = f1 * this.initialVolumeLevel;
            if((double)this.currentVolumeLevel < 0.0D)
            {
                setVolumeLevel(0.0F);
            }
            else if((double)this.currentVolumeLevel > 1.0D)
            {
                setVolumeLevel(1.0F);
            }
            else
            {
                setVolumeLevel(this.currentVolumeLevel);
            }
        }
        return this.db;
    }

    /**
     * Get the current gain set for this object in dB.
     * @return The gain in dB.
     */
    public float getDB()
    {
        return this.db;
    }

    /**
     * Register for gain change update events.
     * A <code>GainChangeEvent</code> is posted when the state
     * of the <code>GainControl</code> changes.
     *
     * @param listener The object to deliver events to.
     */
    public void addGainChangeListener(GainChangeListener listener)
    {
        if(listener != null)
        {
            if(gainChangeListeners == null)
                gainChangeListeners = new ArrayList<GainChangeListener>();

            gainChangeListeners.add(listener);
        }
    }

    /**
     * Remove interest in gain change update events.
     *
     * @param listener The object that has been receiving events.
     */
    public void removeGainChangeListener(GainChangeListener listener)
    {
        if(listener != null && gainChangeListeners != null)
            gainChangeListeners.remove(listener);
    }

    /**
     * Adds a <tt>VolumeChangeListener</tt> to be informed for any change
     * in the volume levels.
     *
     * @param listener volume change listener.
     */
    public void addVolumeChangeListener(VolumeChangeListener listener)
    {
        synchronized(volumeChangeListeners)
        {
            if(!volumeChangeListeners.contains(listener))
            {
                volumeChangeListeners.add(listener);
            }
        }
    }

    /**
     * Removes a <tt>VolumeChangeListener</tt>.
     *
     * @param listener the volume change listener to be removed.
     */
    public void removeVolumeChangeListener(VolumeChangeListener listener)
    {
        synchronized(volumeChangeListeners)
        {
            volumeChangeListeners.remove(listener);
        }
    }

    /**
     * Fire a change in volume to interested listeners.
     */
    private void fireVolumeChange()
    {
        List<VolumeChangeListener> copyVolumeListeners;
        synchronized(volumeChangeListeners)
        {
            copyVolumeListeners =
                    new ArrayList<VolumeChangeListener>(volumeChangeListeners);
        }

        VolumeChangeEvent changeEvent = new VolumeChangeEvent(
                this, this.currentVolumeLevel, this.currentMuteState);


        for(VolumeChangeListener l : copyVolumeListeners)
        {
            l.volumeChange(changeEvent);
        }
    }

    /**
     * Fire events informing for our current state.
     */
    private void fireGainEvents()
    {
        if(gainChangeListeners != null)
        {
            GainChangeEvent gainchangeevent =
                    new GainChangeEvent(
                            this, currentMuteState, db, currentVolumeLevel);

            for(GainChangeListener gainchangelistener : gainChangeListeners)
            {
                gainchangelistener.gainChange(gainchangeevent);
            }
        }
    }

    /**
     * Not used.
     * @return
     */
    public Component getControlComponent()
    {
        return null;
    }

    /**
     * Implementers return the property name they use to store
     * sound level information.
     *
     * @return sound level property name for storing configuration.
     */
    abstract String getStoreLevelPropertyName();
}
