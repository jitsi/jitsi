/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.notification.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.util.*;

/**
 * An implementation of the <tt>SoundNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class SoundNotificationHandlerImpl
    implements SoundNotificationHandler
{
    WeakHashMap<SCAudioClip, NotificationData> playedClips =
        new WeakHashMap<SCAudioClip, NotificationData>();

    /**
     * If the sound is currently disabled.
     */
    private boolean isMute;

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_SOUND;
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     * @param action The action to act upon.
     * @param data Additional data for the event.
     */
    public void start(SoundNotificationAction action, NotificationData data)
    {
        if(isMute())
            return;

        boolean playOnlyOnPlayback = true;

        AudioNotifierService audioNotifService
                    = NotificationActivator.getAudioNotifier();
        if(audioNotifService != null)
            playOnlyOnPlayback =
                audioNotifService.audioOutAndNotificationsShareSameDevice();

        if(playOnlyOnPlayback)
        {
            if(action.isSoundNotificationEnabled()
                || action.isSoundPlaybackEnabled())
            {
                play(action, data, true);
            }
        }
        else
        {
            if(action.isSoundNotificationEnabled())
            {
                play(action, data, false);
            }

            if(action.isSoundPlaybackEnabled())
            {
                play(action, data, true);
            }
        }

        if(action.isSoundPCSpeakerEnabled())
        {
            PCSpeakerClip audio = new PCSpeakerClip();
            playedClips.put(audio, data);

            if(action.getLoopInterval() > -1)
                audio.playInLoop(action.getLoopInterval());
            else
                audio.play();
        }
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     * @param action The action to act upon.
     * @param data Additional data for the event.
     * @param playback to use or not the playback or notification device.
     */
    private void play(SoundNotificationAction action, NotificationData data,
                      boolean playback)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null
            || StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        SCAudioClip audio = audioNotifService
            .createAudio(action.getDescriptor(), playback);

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        playedClips.put(audio, data);
        if(action.getLoopInterval() > -1)
            audio.playInLoop(action.getLoopInterval());
        else
            audio.play();
    }

    /**
     * Stops the sound.
     * @param data Additional data for the event.
     */
    public void stop(NotificationData data)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null)
            return;

        for (Map.Entry<SCAudioClip, NotificationData> entry : playedClips
            .entrySet())
        {
            if(entry.getValue() == data)
            {
                SCAudioClip audio = entry.getKey();
                audio.stop();
                audioNotifService.destroyAudio(audio);
            }
        }
    }

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param isMute mute or not currently playing sounds
     */
    public void setMute(boolean isMute)
    {
        this.isMute = isMute;

        if(isMute)
        {
            AudioNotifierService audioNotifService
                = NotificationActivator.getAudioNotifier();

            if(audioNotifService == null)
                return;

            // stop all sounds
            for (Map.Entry<SCAudioClip, NotificationData> entry : playedClips
                        .entrySet())
            {
                SCAudioClip audio = entry.getKey();
                audio.stop();
                audioNotifService.destroyAudio(audio);
            }
        }
    }

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    public boolean isMute()
    {
        return isMute;
    }

    /**
     * Plays beep on the pc speaker.
     */
    private class PCSpeakerClip
        implements SCAudioClip
    {
        /**
         * Synching start/stop.
         */
        private final Object syncObject = new Object();

        /**
         * Is beep started.
         */
        private boolean started = false;

        /**
         * Is looping.
         */
        private boolean isLooping;

        /**
         * The interval to loop.
         */
        private int loopInterval;

        /**
         * Plays this audio.
         */
        public void play()
        {
            started = true;
            new Thread()
                    {
                        @Override
                        public void run()
                        {
                            runInPlayThread();
                        }
                    }.start();
        }

        /**
         * Plays this audio in loop.
         *
         * @param silenceInterval interval between loops
         */
        public void playInLoop(int silenceInterval)
        {
            setLoopInterval(silenceInterval);
            setIsLooping(true);

            play();
        }

        /**
         * Stops this audio.
         */
        public void stop()
        {
            internalStop();
            setIsLooping(false);
        }

        /**
         * Stops this audio without setting the isLooping property in the case of
         * a looping audio.
         */
        public void internalStop()
        {
            synchronized (syncObject)
            {
                if (started)
                {
                    started = false;
                    syncObject.notifyAll();
                }
            }
        }

        /**
         * Runs in a separate thread to perform the actual playback.
         */
        private void runInPlayThread()
        {
            while (started)
            {
                if (!runOnceInPlayThread())
                    break;

                if(isLooping())
                {
                    synchronized(syncObject)
                    {
                        if (started)
                        {
                            try
                            {
                                if(getLoopInterval() > 0)
                                    syncObject.wait(getLoopInterval());
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    }
                }
                else
                    break;
            }
        }

        /**
         * Beeps.
         *
         * @return <tt>true</tt> if the playback was successful;
         * otherwise, <tt>false</tt>
         */
        private boolean runOnceInPlayThread()
        {
            try
            {
                Toolkit.getDefaultToolkit().beep();
            }
            catch (Throwable t)
            {
                //logger.error("Failed to get audio stream " + url, ioex);
                return false;
            }

            return true;
        }

        /**
         * Returns TRUE if this audio is currently playing in loop,
         * FALSE otherwise.
         * @return TRUE if this audio is currently playing in loop,
         * FALSE otherwise.
         */
        public boolean isLooping()
        {
            return isLooping;
        }

        /**
         * Returns the loop interval if this audio is looping.
         * @return the loop interval if this audio is looping
         */
        public int getLoopInterval()
        {
            return loopInterval;
        }

        /**
         * @param isLooping the isLooping to set
         */
        public void setIsLooping(boolean isLooping)
        {
            this.isLooping = isLooping;
        }

        /**
         * @param loopInterval the loopInterval to set
         */
        public void setLoopInterval(int loopInterval)
        {
            this.loopInterval = loopInterval;
        }
    }
}
