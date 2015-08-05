/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.notification;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

/**
 * An implementation of the <tt>SoundNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class SoundNotificationHandlerImpl
    implements SoundNotificationHandler
{
    /**
     * The logger that will be used to log messages.
     */
    private static Logger logger
        = Logger.getLogger(SoundNotificationHandlerImpl.class);

    /**
     * The indicator which determines whether this
     * <tt>SoundNotificationHandler</tt> is currently muted i.e. the sounds are
     * off.
     */
    private boolean mute;

    private Map<SCAudioClip, NotificationData> playedClips
        = new WeakHashMap<SCAudioClip, NotificationData>();

    /**
     * Property to disable sound notification during an on-going call.
     */
    private static final String PROP_DISABLE_NOTIFICATION_DURING_CALL =
    "net.java.sip.communicator.impl.notification.disableNotificationDuringCall";

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_SOUND;
    }

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    public boolean isMute()
    {
        return mute;
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     * @param action The action to act upon.
     * @param data Additional data for the event.
     * @param device
     */
    private void play(
            SoundNotificationAction action,
            NotificationData data,
            SCAudioClipDevice device)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if((audioNotifService == null)
                || StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        // this is hack, seen on some os (particularly seen on macosx with
        // external devices).
        // when playing notification in the call, can break the call and
        // no further communicating can be done after the notification.
        // So we skip playing notification if we have a call running
        ConfigurationService cfg
            = NotificationActivator.getConfigurationService();
        if(cfg != null
                && cfg.getBoolean(
                    PROP_DISABLE_NOTIFICATION_DURING_CALL,
                    false)
                && SCAudioClipDevice.PLAYBACK.equals(device))
        {
            UIService uiService = NotificationActivator.getUIService();

            if(!uiService.getInProgressCalls().isEmpty())
                return;
        }

        SCAudioClip audio = null;

        switch (device)
        {
        case NOTIFICATION:
        case PLAYBACK:
            audio
                = audioNotifService.createAudio(
                        action.getDescriptor(),
                        SCAudioClipDevice.PLAYBACK.equals(device));
            break;

        case PC_SPEAKER:
            if(!OSUtils.IS_ANDROID)
                audio = new PCSpeakerClip();
            break;
        }

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        synchronized(playedClips)
        {
            playedClips.put(audio, data);
        }

        boolean played = false;

        try
        {
            @SuppressWarnings("unchecked")
            Callable<Boolean> loopCondition
                = (Callable<Boolean>)
                    data.getExtra(
                            NotificationData
                                .SOUND_NOTIFICATION_HANDLER_LOOP_CONDITION_EXTRA);

            audio.play(action.getLoopInterval(), loopCondition);
            played = true;
        }
        finally
        {
            synchronized(playedClips)
            {
                if (!played)
                    playedClips.remove(audio);
            }
        }
    }

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param mute mute or not currently playing sounds
     */
    public void setMute(boolean mute)
    {
        this.mute = mute;

        if (mute)
        {
            AudioNotifierService ans
                = NotificationActivator.getAudioNotifier();

            if ((ans != null) && (ans.isMute() != this.mute))
                ans.setMute(this.mute);
        }
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
        {
            playOnlyOnPlayback
                = audioNotifService.audioOutAndNotificationsShareSameDevice();
        }

        if(playOnlyOnPlayback)
        {
            if(action.isSoundNotificationEnabled()
                    || action.isSoundPlaybackEnabled())
            {
                play(action, data, SCAudioClipDevice.PLAYBACK);
            }
        }
        else
        {
            if(action.isSoundNotificationEnabled())
                play(action, data, SCAudioClipDevice.NOTIFICATION);
            if(action.isSoundPlaybackEnabled())
                play(action, data, SCAudioClipDevice.PLAYBACK);
        }

        if(action.isSoundPCSpeakerEnabled())
            play(action, data, SCAudioClipDevice.PC_SPEAKER);
    }

    /**
     * Stops the sound.
     * @param data Additional data for the event.
     */
    public void stop(NotificationData data)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if (audioNotifService != null)
        {
            List<SCAudioClip> clipsToStop = new ArrayList<SCAudioClip>();

            synchronized(playedClips)
            {
                Iterator<Map.Entry<SCAudioClip, NotificationData>> i
                    = playedClips.entrySet().iterator();

                while (i.hasNext())
                {
                    Map.Entry<SCAudioClip, NotificationData> e = i.next();

                    if (e.getValue() == data)
                    {
                        clipsToStop.add(e.getKey());
                        i.remove();
                    }
                }
            }

            for(SCAudioClip clip : clipsToStop)
            {
                try
                {
                    clip.stop();
                }
                catch(Throwable t)
                {
                    logger.error("Error stopping audio clip", t);
                }
            }
        }
    }

    /**
     * Tells if the given notification sound is currently played.
     *
     * @param data Additional data for the event.
     */
    public boolean isPlaying(NotificationData data)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if (audioNotifService != null)
        {
            synchronized(playedClips)
            {
                Iterator<Map.Entry<SCAudioClip, NotificationData>> i
                    = playedClips.entrySet().iterator();

                while (i.hasNext())
                {
                    Map.Entry<SCAudioClip, NotificationData> e = i.next();

                    if (e.getValue() == data)
                    {
                        return e.getKey().isStarted();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Beeps the PC speaker.
     */
    private static class PCSpeakerClip
        extends AbstractSCAudioClip
    {
        /**
         * The beep method.
         */
        private Method beepMethod = null;

        /**
         * The toolkit.
         */
        private Object toolkit = null;

        /**
         * Initializes a new <tt>PCSpeakerClip</tt> instance.
         */
        public PCSpeakerClip()
        {
            super(null, NotificationActivator.getAudioNotifier());

            // load the method java.awt.Toolkit.getDefaultToolkit().beep();
            // use reflection to be sure it will not throw exception in Android
            try
            {
                Method getDefaultToolkitMethod =
                    Class.forName("java.awt.Toolkit")
                        .getMethod("getDefaultToolkit");
                toolkit = getDefaultToolkitMethod.invoke(null);
                beepMethod = toolkit.getClass().getMethod("beep");
            }
            catch(Throwable t)
            {
                logger.error("Cannot load awt.Toolkit", t);
            }
        }

        /**
         * Beeps the PC speaker.
         *
         * @return <tt>true</tt> if the playback was successful; otherwise,
         * <tt>false</tt>
         */
        @Override
        protected boolean runOnceInPlayThread()
        {
            try
            {
                if(beepMethod != null)
                    beepMethod.invoke(toolkit);

                return true;
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                    return false;
            }
        }
    }

    /**
     * Enumerates the types of devices on which <tt>SCAudioClip</tt>s may be
     * played back.
     */
    private static enum SCAudioClipDevice
    {
        NOTIFICATION,
        PC_SPEAKER,
        PLAYBACK
    }
}
