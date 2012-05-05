/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.notify;

import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;

import javax.swing.*;

import net.java.sip.communicator.service.audionotifier.*;

/**
 * Implementation of SCAudioClip.
 *
 * @author Yana Stamcheva
 */
public class JavaSoundClipImpl
    extends SCAudioClipImpl
    implements ActionListener
               
{
    private static Constructor<AudioClip> acConstructor = null;

    private final Timer playAudioTimer = new Timer(1000, null);

    private final AudioClip audioClip;

    private final AudioNotifierService audioNotifier;

    /**
     * Creates the audio clip and initialize the listener used from the
     * loop timer.
     *
     * @param url the url pointing to the audio file
     * @param audioNotifier the audio notify service
     * @throws IOException cannot audio clip with supplied url.
     */
    public JavaSoundClipImpl(URL url, AudioNotifierService audioNotifier)
        throws IOException
    {
        this.audioClip = createAppletAudioClip(url.openStream());
        this.audioNotifier = audioNotifier;

        this.playAudioTimer.addActionListener(this);
    }

    /**
     * Plays this audio.
     */
    public void play()
    {
        if ((audioClip != null) && !audioNotifier.isMute())
            audioClip.play();
    }

    /**
     * Plays this audio in loop.
     *
     * @param interval the loop interval
     */
    public void playInLoop(int interval)
    {
        if(!audioNotifier.isMute())
        {
            if(interval == 0)
                audioClip.loop();
            else
            {
                //first play the audio and then start the timer and wait
                audioClip.play();
                playAudioTimer.setDelay(interval);
                playAudioTimer.setRepeats(true);

                playAudioTimer.start();
            }
        }

        setLoopInterval(interval);
        setIsLooping(true);
    }

    /**
     * Stops this audio.
     */
    public void stop()
    {
        if (audioClip != null)
            audioClip.stop();

        if (isLooping())
        {
            playAudioTimer.stop();
            setIsLooping(false);
        }
    }

    /**
     * Stops this audio without setting the isLooping property in the case of
     * a looping audio. The AudioNotifier uses this method to stop the audio
     * when setMute(true) is invoked. This allows us to restore all looping
     * audios when the sound is restored by calling setMute(false).
     */
    public void internalStop()
    {
        if (audioClip != null)
            audioClip.stop();

        if (isLooping())
            playAudioTimer.stop();
    }

    /**
     * Creates an AppletAudioClip.
     *
     * @param inputstream the audio input stream
     * @throws IOException
     */
    private static AudioClip createAppletAudioClip(InputStream inputstream)
        throws IOException
    {
        if (acConstructor == null)
        {
            try
            {
                acConstructor
                    = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Constructor<AudioClip>>()
                            {
                                public Constructor<AudioClip> run()
                                    throws ClassNotFoundException,
                                           NoSuchMethodException,
                                           SecurityException
                                {
                                    return createAcConstructor();
                                }
                            });
            }
            catch (PrivilegedActionException paex)
            {
                throw
                    new IOException(
                            "Failed to get AudioClip constructor: "
                                + paex.getException());
            }
        }

        try
        {
            return acConstructor.newInstance(inputstream);
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to construct the AudioClip: " + ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Constructor<AudioClip> createAcConstructor()
        throws ClassNotFoundException,
               NoSuchMethodException,
               SecurityException
    {
        Class<?> class1;
        try
        {
            class1
                = Class.forName(
                    "com.sun.media.sound.JavaSoundAudioClip",
                    true,
                    ClassLoader.getSystemClassLoader());
        }
        catch (ClassNotFoundException cnfex)
        {
            class1
                = Class.forName("sun.audio.SunAudioClip", true, null);
        }
        return
            (Constructor<AudioClip>) class1.getConstructor(InputStream.class);
    }

    /**
     * Plays an audio clip. Used in the playAudioTimer to play an audio in loop.
     * @param e the event.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (audioClip != null)
        {
            audioClip.stop();
            audioClip.play();
        }
    }
}
