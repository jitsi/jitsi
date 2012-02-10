/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.lang.ref.*;
import java.util.*;
import java.util.List;

import javax.media.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Controls media service volume input or output. If a playback volume level
 * is set we change it on all current players, as we synchronize volume
 * on all players. Implements interface exposed from media service, also
 * implements the interface used in the Renderer part of JMF and merges the two
 * functionalities to work as one.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class AbstractVolumeControl
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
     * The <tt>VolumeChangeListener</tt>s interested in volume change events
     * through the <tt>VolumeControl</tt> interface.
     * <p>
     * Because the instances of <tt>AbstractVolumeControl</tt> are global at the
     * time of this writing and, consequently, they cause the
     * <tt>VolumeChangeListener</tt>s to be leaked, the listeners are referenced
     * using <tt>WeakReference</tt>s. 
     * </p>
     */
    private final List<WeakReference<VolumeChangeListener>>
        volumeChangeListeners
            = new ArrayList<WeakReference<VolumeChangeListener>>();

    /**
     * Listeners interested in volume change inside FMJ/JMF.
     */
    private List<GainChangeListener> gainChangeListeners;

    /**
     * The current volume level.
     */
    private float volumeLevel = DEFAULT_VOLUME_LEVEL;

    /**
     * Current mute state, by default we start unmuted.
     */
    private boolean mute = false;

    /**
     * Current level in db.
     */
    private float db;

    /**
     * The initial volume level, when this instance was created.
     */
    private final float initialVolumeLevel;

    /**
     * The name of the configuration property which specifies the value of the
     * volume level of this <tt>AbstractVolumeControl</tt>.
     */
    private final String volumeLevelConfigurationPropertyName;

    /**
     * Creates volume control instance and initializes initial level value
     * if stored in the configuration service.
     *
     * @param volumeLevelConfigurationPropertyName the name of the configuration
     * property which specifies the value of the volume level of the new
     * instance
     */
    public AbstractVolumeControl(
        String volumeLevelConfigurationPropertyName)
    {
        this.volumeLevelConfigurationPropertyName
            = volumeLevelConfigurationPropertyName;

        // read initial level from config service if any
        String initialVolumeLevelString
            = NeomediaActivator.getConfigurationService().getString(
                    this.volumeLevelConfigurationPropertyName);
        float initialVolumeLevel = DEFAULT_VOLUME_LEVEL;

        try
        {
            if (initialVolumeLevelString != null)
            {
                initialVolumeLevel = Float.parseFloat(initialVolumeLevelString);
                if(logger.isDebugEnabled())
                {
                    logger.debug(
                            "Restored volume: " + initialVolumeLevelString);
                }
            }
        }
        catch(Throwable t)
        {
            logger.warn("Error restoring volume", t);
        }

        this.initialVolumeLevel = initialVolumeLevel;
        this.volumeLevel = this.initialVolumeLevel;
    }

    /**
     * Applies the gain specified by <tt>gainControl</tt> to the signal defined
     * by the <tt>length</tt> number of samples given in <tt>buffer</tt>
     * starting at <tt>offset</tt>.
     *
     * @param gainControl the <tt>GainControl</tt> which specifies the gain to
     * apply
     * @param buffer the samples of the signal to apply the gain to
     * @param offset the start of the samples of the signal in <tt>buffer</tt>
     * @param length the number of samples of the signal given in
     * <tt>buffer</tt>
     */
    public static void applyGain(
            GainControl gainControl,
            byte[] buffer, int offset, int length)
    {
        if (gainControl.getMute())
            Arrays.fill(buffer, offset, offset + length, (byte) 0);
        else
        {
            // Assign the maximum of 200% to the volume scale.
            float level = gainControl.getLevel() * 2;

            if (level != 1)
            {
                for (int i = offset, toIndex = offset + length;
                        i < toIndex;
                        i += 2)
                {
                    int i1 = i + 1;
                    short s = (short) ((buffer[i] & 0xff) | (buffer[i1] << 8));

                    /* Clip, don't wrap. */
                    int si = s;

                    si = (int) (si * level);
                    if (si > Short.MAX_VALUE)
                        s = Short.MAX_VALUE;
                    else if (si < Short.MIN_VALUE)
                        s = Short.MIN_VALUE;
                    else
                        s = (short) si;

                    buffer[i] = (byte) s;
                    buffer[i1] = (byte) (s >> 8);
                }
            }
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
        return volumeLevel;
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
        return volumeLevel;
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
        if (volumeLevel == value)
            return value;

        if(value < MIN_VOLUME_LEVEL)
            volumeLevel = MIN_VOLUME_LEVEL;
        else if(value > MAX_VOLUME_LEVEL)
            volumeLevel = MAX_VOLUME_LEVEL;
        else
            volumeLevel = value;

        fireVolumeChange();

        // save the level change, so we can restore it on next run
        NeomediaActivator.getConfigurationService().setProperty(
                this.volumeLevelConfigurationPropertyName,
                String.valueOf(volumeLevel));

        float f1 = value / initialVolumeLevel;
        db = (float)((Math.log((double)f1 != 0.0D ?
                f1
                : 0.0001D) / Math.log(10D)) * 20D);

        fireGainEvents();

        return volumeLevel;
    }

    /**
     * Mutes current sound.
     *
     * @param mute mutes/unmutes.
     */
    public void setMute(boolean mute)
    {
        if (this.mute != mute)
        {
            this.mute = mute;

            fireVolumeChange();
            fireGainEvents();
        }
    }

    /**
     * Get mute state of sound.
     *
     * @return mute state of sound.
     */
    public boolean getMute()
    {
        return mute;
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
            float volumeLevel = f1 * this.initialVolumeLevel;

            if(volumeLevel < 0.0F)
                volumeLevel = 0.0F;
            else if(volumeLevel > 1.0F)
                volumeLevel = 1.0F;

            setVolumeLevel(volumeLevel);
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
        synchronized (volumeChangeListeners)
        {
            Iterator<WeakReference<VolumeChangeListener>> i
                = volumeChangeListeners.iterator();
            boolean contains = false;

            while (i.hasNext())
            {
                VolumeChangeListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    contains = true;
            }
            if(!contains)
                volumeChangeListeners.add(
                        new WeakReference<VolumeChangeListener>(listener));
        }
    }

    /**
     * Removes a <tt>VolumeChangeListener</tt>.
     *
     * @param listener the volume change listener to be removed.
     */
    public void removeVolumeChangeListener(VolumeChangeListener listener)
    {
        synchronized (volumeChangeListeners)
        {
            Iterator<WeakReference<VolumeChangeListener>> i
                = volumeChangeListeners.iterator();

            while (i.hasNext())
            {
                VolumeChangeListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
        }
    }

    /**
     * Fire a change in volume to interested listeners.
     */
    private void fireVolumeChange()
    {
        List<VolumeChangeListener> ls;

        synchronized (volumeChangeListeners)
        {
            Iterator<WeakReference<VolumeChangeListener>> i
                = volumeChangeListeners.iterator();

            ls
                = new ArrayList<VolumeChangeListener>(
                        volumeChangeListeners.size());
            while (i.hasNext())
            {
                VolumeChangeListener l = i.next().get();

                if (l == null)
                    i.remove();
                else
                    ls.add(l);
            }
        }

        VolumeChangeEvent changeEvent
            = new VolumeChangeEvent(this, volumeLevel, mute);

        for(VolumeChangeListener l : ls)
            l.volumeChange(changeEvent);
    }

    /**
     * Fire events informing for our current state.
     */
    private void fireGainEvents()
    {
        if(gainChangeListeners != null)
        {
            GainChangeEvent gainchangeevent
                = new GainChangeEvent(this, mute, db, volumeLevel);

            for(GainChangeListener gainchangelistener : gainChangeListeners)
                gainchangelistener.gainChange(gainchangeevent);
        }
    }

    /**
     * Not used.
     * @return null
     */
    public Component getControlComponent()
    {
        return null;
    }
}
