/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>SoundNotificationHandlerImpl</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class SoundNotificationHandlerImpl
    implements SoundNotificationHandler
{
    /**
     * The audio clip that manages to play the sound.
     */
    private SCAudioClip audio;

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
     * @param action the action to act upon.
     */
    public void start(SoundNotificationAction action)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null
            || StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        //stop any previous audio notification
        if(audio != null)
        {
            stop();
        }

        audio = audioNotifService.createAudio(action.getDescriptor());

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        if(action.getLoopInterval() > -1)
            audio.playInLoop(action.getLoopInterval());
        else
            audio.play();
    }

    /**
     * Stops the sound.
     */
    public void stop()
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null || audio == null)
            return;

        audio.stop();
        audioNotifService.destroyAudio(audio);
        audio = null;
    }
}
