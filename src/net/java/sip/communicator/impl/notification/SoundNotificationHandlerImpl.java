/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.awt.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.gui.*;
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
    /**
     * The indicator which determines whether this
     * <tt>SoundNotificationHandler</tt> is currently muted i.e. the sounds are
     * off.
     */
    private boolean mute;

    private Map<SCAudioClip, NotificationData> playedClips
        = new WeakHashMap<SCAudioClip, NotificationData>();

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
        if(SCAudioClipDevice.PLAYBACK.equals(device))
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
            audio = new PCSpeakerClip();
            break;
        }

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        playedClips.put(audio, data);

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
            if (!played)
                playedClips.remove(audio);
        }
    }

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param isMute mute or not currently playing sounds
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
            Iterator<Map.Entry<SCAudioClip, NotificationData>> i
                = playedClips.entrySet().iterator();

            while (i.hasNext())
            {
                Map.Entry<SCAudioClip, NotificationData> e = i.next();

                if (e.getValue() == data)
                {
                    try
                    {
                        e.getKey().stop();
                    }
                    finally
                    {
                        i.remove();
                    }
                }
            }
        }
    }

    /**
     * Beeps the PC speaker.
     */
    private static class PCSpeakerClip
        extends AbstractSCAudioClip
    {
        /**
         * Initializes a new <tt>PCSpeakerClip</tt> instance.
         */
        public PCSpeakerClip()
        {
            super(null, NotificationActivator.getAudioNotifier());
        }

        /**
         * Beeps the PC speaker.
         *
         * @return <tt>true</tt> if the playback was successful; otherwise,
         * <tt>false</tt>
         */
        protected boolean runOnceInPlayThread()
        {
            try
            {
                Toolkit.getDefaultToolkit().beep();
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
