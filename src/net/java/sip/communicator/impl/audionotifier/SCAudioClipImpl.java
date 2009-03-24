/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.audionotifier;

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
public class SCAudioClipImpl implements SCAudioClip
{
    private static Constructor<AudioClip> acConstructor = null;

    private Timer playAudioTimer = new Timer(1000, null);

    private AudioClip audioClip;

    private boolean isInvalid;

    private boolean isLooping;

    private int loopInterval;

    private ActionListener audioListener;

    private AudioNotifierService audioNotifier;

    /**
     * Creates the audio clip and initialize the listener used from the
     * loop timer.
     *
     * @param url the url pointing to the audio file
     */
    public SCAudioClipImpl(URL url, AudioNotifierService audioNotifier)
        throws IOException
    {
        InputStream inputstream;

        inputstream = url.openStream();
        this.createAppletAudioClip(inputstream);

        this.audioListener = new PlayAudioListener(audioClip);
        this.playAudioTimer.addActionListener(audioListener);

        this.audioNotifier = audioNotifier;
    }

    /**
     * Plays this audio.
     */
    public void play()
    {
        if (audioClip != null && !audioNotifier.isMute())
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

        this.loopInterval = interval;

        this.isLooping = true;
    }

    /**
     * Stops this audio.
     */
    public void stop()
    {
        if (audioClip != null)
            audioClip.stop();

        if(isLooping)
        {
            playAudioTimer.stop();
            this.isLooping = false;
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

        if(isLooping)
        {
            playAudioTimer.stop();
        }
    }

    /**
     * Creates an AppletAudioClip.
     *
     * @param inputstream the audio input stream
     * @throws IOException
     */
    private void createAppletAudioClip(InputStream inputstream)
        throws IOException
    {
        if(acConstructor == null)
        {
            try
            {
                acConstructor = AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Constructor<AudioClip>>()
                {
                    @SuppressWarnings("unchecked")
                    public Constructor<AudioClip> run()
                        throws  NoSuchMethodException,
                                SecurityException,
                                ClassNotFoundException
                    {

                        Class<?> class1 = null;
                        try
                        {
                            class1 = Class.forName(
                                    "com.sun.media.sound.JavaSoundAudioClip",
                                    true, ClassLoader.getSystemClassLoader());
                        }
                        catch(ClassNotFoundException classnotfoundexception)
                        {
                            class1 = Class.forName(
                                "sun.audio.SunAudioClip", true, null);
                        }
                        Class<?> aclass[] = new Class[1];
                        aclass[0] = Class.forName("java.io.InputStream");
                        return (Constructor<AudioClip>) class1.getConstructor(aclass);
                    }
                });
            }
            catch(PrivilegedActionException privilegedactionexception)
            {
                throw new IOException("Failed to get AudioClip constructor: "
                    + privilegedactionexception.getException());
            }
        }

        try
        {
            Object aobj[] = {
                inputstream
            };
            audioClip = acConstructor.newInstance(aobj);
        }
        catch(Exception exception)
        {
            throw new IOException("Failed to construct the AudioClip: "
                + exception);
        }
    }

    /**
     * Plays an audio clip. Used in the playAudioTimer to play an audio in loop.
     */
    private static class PlayAudioListener implements ActionListener
    {
        private AudioClip audio;

        public PlayAudioListener(AudioClip audio)
        {
            this.audio = audio;
        }
        public void actionPerformed(ActionEvent e)
        {
            audio.stop();
            audio.play();
        }
    }

    /**
     * Returns TRUE if this audio is invalid, FALSE otherwise.
     *
     * @return TRUE if this audio is invalid, FALSE otherwise
     */
    public boolean isInvalid()
    {
        return isInvalid;
    }

    /**
     * Marks this audio as invalid or not.
     *
     * @param isInvalid TRUE to mark this audio as invalid, FALSE otherwise
     */
    public void setInvalid(boolean isInvalid)
    {
        this.isInvalid = isInvalid;
    }

    /**
     * Returns TRUE if this audio is currently playing in loop, FALSE otherwise.
     * @return TRUE if this audio is currently playing in loop, FALSE otherwise.
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
}
