/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.notify;

import java.io.*;
import java.net.*;

import javax.sound.sampled.*;

import net.java.sip.communicator.impl.neomedia.protocol.portaudio.*;
import net.java.sip.communicator.impl.neomedia.protocol.portaudio.streams.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of SCAudioClip using PortAudio.
 *
 * @author Damian Minkov
 */
public class PortAudioClipImpl
    extends SCAudioClipImpl
{
    private static final Logger logger
        = Logger.getLogger(PortAudioClipImpl.class);

    private final AudioNotifierServiceImpl audioNotifier;

    private boolean started = false;

    private final URL url;

    /**
     * Creates the audio clip and initialize the listener used from the
     * loop timer.
     *
     * @param url the url pointing to the audio file
     * @param audioNotifier the audio notify service
     * @throws IOException cannot audio clip with supplied url.
     */
    public PortAudioClipImpl(URL url, AudioNotifierServiceImpl audioNotifier)
        throws IOException
    {
        this.audioNotifier = audioNotifier;
        this.url = url;
    }

    /**
     * Plays this audio.
     */
    public void play()
    {
        if ((url != null) && !audioNotifier.isMute())
        {
            started = true;
            new Thread(new PlayThread()).start();
        }
    }

    /**
     * Plays this audio in loop.
     *
     * @param interval the loop interval
     */
    public void playInLoop(int interval)
    {
        setLoopInterval(interval);
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
     * a looping audio. The AudioNotifier uses this method to stop the audio
     * when setMute(true) is invoked. This allows us to restore all looping
     * audios when the sound is restored by calling setMute(false).
     */
    public void internalStop()
    {
        if (url != null)
            started = false;
    }

    private class PlayThread
        implements Runnable
    {
        byte[] buffer = new byte[1024];
        private OutputPortAudioStream portAudioStream = null;

        public void run()
        {
            try
            {
                while(true)
                {
                    AudioInputStream audioStream =
                        AudioSystem.getAudioInputStream(url);
                    AudioFormat audioStreamFormat = audioStream.getFormat();

                    if (portAudioStream == null)
                    {
                        int deviceIndex =
                            PortAudioUtils.getDeviceIndexFromLocator(
                                        audioNotifier.getDeviceConfiguration().
                                        getAudioNotifyDevice().getLocator());

                        portAudioStream = PortAudioManager.getInstance().
                            getOutputStream(
                                deviceIndex,
                                audioStreamFormat.getSampleRate(),
                                audioStreamFormat.getChannels(),
                                PortAudioUtils.getPortAudioSampleFormat(
                                    audioStreamFormat.getSampleSizeInBits()));
                        portAudioStream.start();
                    }

                    if(!started)
                    {
                        portAudioStream.stop();
                        return;
                    }

                    while(audioStream.read(buffer) != -1)
                    {
                        portAudioStream.write(buffer);
                    }

                    if(!isLooping())
                    {
                        portAudioStream.stop();
                        break;
                    }
                    else
                    {
                        Thread.sleep(getLoopInterval());
                    }
                }
            }
            catch (PortAudioException e)
            {
                logger.error(
                    "Cannot open portaudio device for notifications", e);
            }
            catch (IOException e)
            {
                logger.error("Error reading from audio resource", e);
            }
            catch (InterruptedException e)
            {
                logger.error("Cannot wait the interval between plays", e);
            }
            catch (UnsupportedAudioFileException e)
            {
                logger.error("Unknown file format", e);
            }
        }
    }
}
