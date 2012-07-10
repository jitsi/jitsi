/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

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
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null
            || StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        SCAudioClip audio = audioNotifService.createAudio(action.getDescriptor());

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
                return;
            }
        }
    }
}
