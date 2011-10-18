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
    private String soundFileDescriptor;

    /**
     * By default we don't play sounds in loop.
     */
    private int loopInterval = -1;

    /**
     * The audio clip that manages to play the sound.
     */
    private SCAudioClip audio;

    /**
     * Indicates if this handler is enabled.
     */
    private boolean isEnabled = true;

    /**
     * Creates an instance of <tt>SoundNotificationHandlerImpl</tt> by
     * specifying the sound file descriptor.
     * 
     * @param soundDescriptor the sound file descriptor
     */
    public SoundNotificationHandlerImpl(String soundDescriptor)
    {
        this.soundFileDescriptor = soundDescriptor;
    }

    /**
     * Creates an instance of <tt>SoundNotificationHandlerImpl</tt> by
     * specifying the sound file descriptor and the loop interval.
     * 
     * @param soundDescriptor the sound file descriptor
     * @param loopInterval the loop interval
     */
    public SoundNotificationHandlerImpl( String soundDescriptor,
                                            int loopInterval)
    {
        this.soundFileDescriptor = soundDescriptor;
        this.loopInterval = loopInterval;
    }

    /**
     * Returns the loop interval. This is the interval of milliseconds to wait
     * before repeating the sound, when playing a sound in loop. By default this
     * method returns -1.
     * 
     * @return the loop interval 
     */
    public int getLoopInterval()
    {
        return loopInterval;
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     */
    public void start()
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService == null
            || StringUtils.isNullOrEmpty(soundFileDescriptor, true))
            return;

        audio = audioNotifService.createAudio(soundFileDescriptor);

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        if(loopInterval > -1)
            audio.playInLoop(loopInterval);
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

        if(audioNotifService == null)
            return;

        if(audio == null)
            return;

        audio.stop();

        audioNotifService.destroyAudio(audio);
    }

    /**
     * Returns the descriptor pointing to the sound to be played.
     * 
     * @return the descriptor pointing to the sound to be played.
     */
    public String getDescriptor()
    {
        return soundFileDescriptor;
    }

    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for the sound action type is
     * disabled no sounds will be played when the <tt>fireNotification</tt>
     * method is called.
     * 
     * @return TRUE if this notification action handler is enabled and FALSE
     * otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Enables or disables this notification handler. While the notification
     * handler for the sound action type is disabled no sounds will be played
     * when the <tt>fireNotification</tt> method is called.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
}
