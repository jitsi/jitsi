/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.notify;

import java.io.*;
import java.net.*;

import javax.sound.sampled.*;

import net.java.sip.communicator.impl.media.protocol.portaudio.*;
import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of SCAudioClip using PortAudio.
 *
 * @author Damian Minkov
 */
public class PortAudioClipImpl
    extends SCAudioClipImpl
    implements PropertyChangeListener
{
    private static final Logger logger
        = Logger.getLogger(PortAudioClipImpl.class);

    private final AudioNotifierServiceImpl audioNotifier;

    private Thread playThread = new Thread(new PlayThread());

    private boolean started = false;

    private long portAudioStream = 0;

    private URL url = null;

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

        audioNotifier.getDeviceConfiguration().addPropertyChangeListener(this);
    }

    /**
     * Plays this audio.
     */
    public void play()
    {
        if ((url != null) && !audioNotifier.isMute())
        {
            started = true;
            playThread.start();
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

        if(!audioNotifier.isMute())
        {
            started = true;
            playThread.start();
        }
    }

    /**
     * Stops this audio.
     */
    public void stop()
    {
        if (url != null)
            started = false;
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

    /**
     * Notified when device configuration has changed.
     * @param evt the event of the change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
            DeviceConfiguration.AUDIO_NOTIFY_DEVICE))
        {
            // make the stream 0, to be sure next time the new
            // device will be used
            portAudioStream = 0;
        }
    }

    private class PlayThread
        implements Runnable
    {
        byte[] buffer = new byte[1024];

        public void run()
        {
            try
            {
                while(true)
                {
                    if (portAudioStream == 0)
                    {
                        int deviceIndex =
                            PortAudioStream.getDeviceIndexFromLocator(
                                        audioNotifier.getDeviceConfiguration().
                                        getAudioNotifyDevice().getLocator());
                        long devInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);
                        int maxOutChannels =
                            PortAudio.PaDeviceInfo_getMaxOutputChannels(devInfo);
                        if(maxOutChannels > 2)
                            maxOutChannels = 2;

                        double sampleRate =
                            PortAudio.PaDeviceInfo_getDefaultSampleRate(devInfo);

                        long streamParameters
                            = PortAudio.PaStreamParameters_new(
                                    deviceIndex,
                                    maxOutChannels,
                                    PortAudio.SAMPLE_FORMAT_INT16);

                        portAudioStream
                            = PortAudio.Pa_OpenStream(
                                    0,
                                    streamParameters,
                                    sampleRate,
                                    PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
                                    PortAudio.STREAM_FLAGS_NO_FLAG,
                                    null);

                        PortAudio.Pa_StartStream(portAudioStream);
                    }

                    AudioInputStream audioStream =
                        AudioSystem.getAudioInputStream(url);

                    if(!started)
                    {
                        PortAudio.Pa_CloseStream(portAudioStream);
                        return;
                    }

                    int read = 0;
                    while((read = audioStream.read(buffer)) != -1)
                    {
                        PortAudio.Pa_WriteStream(
                            portAudioStream, 
                            buffer,
                            buffer.length/audioStream.getFormat().getFrameSize());
                    }

                    if(!isLooping())
                    {
                        PortAudio.Pa_CloseStream(portAudioStream);
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
