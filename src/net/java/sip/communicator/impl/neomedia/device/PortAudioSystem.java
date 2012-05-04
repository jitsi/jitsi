/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.util.*;

/**
 * Creates PortAudio capture devices by enumerating all host devices that have
 * input channels.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class PortAudioSystem
    extends AudioSystem
{
    /**
     * The <tt>Logger</tt> used by the <tt>PortAudioSystem</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PortAudioSystem.class);

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying PortAudio
     * <tt>CaptureDevice</tt>s
     */
    public static final String LOCATOR_PROTOCOL = "portaudio";

    private Runnable devicesChangedCallback;

    /**
     * Initializes a new <tt>PortAudioSystem</tt> instance which creates PortAudio
     * capture devices by enumerating all host devices with input channels.
     *
     * @throws Exception if anything wrong happens while creating the PortAudio
     * capture devices
     */
    PortAudioSystem()
        throws Exception
    {
        super(
                LOCATOR_PROTOCOL,
                FEATURE_DENOISE
                    | FEATURE_ECHO_CANCELLATION
                    | FEATURE_NOTIFY_AND_PLAYBACK_DEVICES
                    | FEATURE_REINITIALIZE);
    }

    protected void doInitialize()
        throws Exception
    {
        /*
         * If PortAudio fails to initialize because of, for example, a missing
         * native counterpart, it will throw an exception here and the PortAudio
         * Renderer will not be initialized.
         */
        int deviceCount = PortAudio.Pa_GetDeviceCount();
        int channels = 1;
        int sampleSizeInBits = 16;
        long sampleFormat = PortAudio.getPaSampleFormat(sampleSizeInBits);
        int defaultInputDeviceIndex = PortAudio.Pa_GetDefaultInputDevice();
        int defaultOutputDeviceIndex = PortAudio.Pa_GetDefaultOutputDevice();
        List<CaptureDeviceInfo> captureDevices
            = new LinkedList<CaptureDeviceInfo>();
        List<CaptureDeviceInfo> playbackDevices
            = new LinkedList<CaptureDeviceInfo>();

        for (int deviceIndex = 0; deviceIndex < deviceCount; deviceIndex++)
        {
            long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);
            String name = PortAudio.PaDeviceInfo_getName(deviceInfo);

            if (name != null)
                name = name.trim();

            int maxInputChannels
                = PortAudio.PaDeviceInfo_getMaxInputChannels(deviceInfo);
            int maxOutputChannels
                = PortAudio.PaDeviceInfo_getMaxOutputChannels(deviceInfo);

            CaptureDeviceInfo cdi
                = new CaptureDeviceInfo(
                        name,
                        new MediaLocator(LOCATOR_PROTOCOL + ":#" + deviceIndex),
                        new Format[]
                        {
                            new AudioFormat(
                                    AudioFormat.LINEAR,
                                    (maxInputChannels > 0)
                                        ? getSupportedSampleRate(
                                                true,
                                                deviceIndex,
                                                channels,
                                                sampleFormat)
                                        : PortAudio.DEFAULT_SAMPLE_RATE,
                                    sampleSizeInBits,
                                    channels,
                                    AudioFormat.LITTLE_ENDIAN,
                                    AudioFormat.SIGNED,
                                    Format.NOT_SPECIFIED /* frameSizeInBits */,
                                    Format.NOT_SPECIFIED /* frameRate */,
                                    Format.byteArray)
                        });

            if (maxInputChannels > 0)
            {
                if (deviceIndex == defaultInputDeviceIndex)
                {
                    captureDevices.add(0, cdi);
                    if (logger.isDebugEnabled())
                        logger.debug("Added default capture device: " + name);
                }
                else
                {
                    captureDevices.add(cdi);
                    if (logger.isDebugEnabled())
                        logger.debug("Added capture device: " + name);
                }
            }
            if (maxOutputChannels > 0)
            {
                if (deviceIndex == defaultOutputDeviceIndex)
                {
                    playbackDevices.add(0, cdi);
                    if (logger.isDebugEnabled())
                        logger.debug("Added default playback device: " + name);
                }
                else
                {
                    playbackDevices.add(cdi);
                    if (logger.isDebugEnabled())
                        logger.debug("Added playback device: " + name);
                }
            }
        }

        setCaptureDevices(captureDevices);
        setPlaybackDevices(playbackDevices);

        if (devicesChangedCallback == null)
        {
            devicesChangedCallback
                = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            PortAudio.Pa_UpdateAvailableDeviceList();
                            initialize();
                        }
                        catch (Throwable t)
                        {
                            if (t instanceof ThreadDeath)
                                throw (ThreadDeath) t;

                            logger.warn(
                                    "Failed to reinitialize PortAudio devices",
                                    t);
                        }
                    }
                };
            PortAudio.setDevicesChangedCallback(devicesChangedCallback);
        }
    }

    /**
     * Gets a sample rate supported by a PortAudio device with a specific device
     * index with which it is to be registered with JMF.
     *
     * @param input <tt>true</tt> if the supported sample rate is to be retrieved for
     * the PortAudio device with the specified device index as an input device
     * or <tt>false</tt> for an output device
     * @param deviceIndex the device index of the PortAudio device for which a
     * supported sample rate is to be retrieved
     * @param channelCount number of channel
     * @param sampleFormat sample format
     * @return a sample rate supported by the PortAudio device with the
     * specified device index with which it is to be registered with JMF
     */
    private static double getSupportedSampleRate(
            boolean input,
            int deviceIndex,
            int channelCount,
            long sampleFormat)
    {
        long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);
        double supportedSampleRate;

        if (deviceInfo != 0)
        {
            double defaultSampleRate
                = PortAudio.PaDeviceInfo_getDefaultSampleRate(deviceInfo);

            if (defaultSampleRate >= MediaUtils.MAX_AUDIO_SAMPLE_RATE)
                supportedSampleRate = defaultSampleRate;
            else
            {
                long streamParameters
                    = PortAudio.PaStreamParameters_new(
                            deviceIndex,
                            channelCount,
                            sampleFormat,
                            PortAudio.LATENCY_UNSPECIFIED);

                if (streamParameters == 0)
                    supportedSampleRate = defaultSampleRate;
                else
                {
                    try
                    {
                        long inputParameters;
                        long outputParameters;

                        if (input)
                        {
                            inputParameters = streamParameters;
                            outputParameters = 0;
                        }
                        else
                        {
                            inputParameters = 0;
                            outputParameters = streamParameters;
                        }

                        boolean formatIsSupported
                            = PortAudio.Pa_IsFormatSupported(
                                    inputParameters,
                                    outputParameters,
                                    PortAudio.DEFAULT_SAMPLE_RATE);

                        supportedSampleRate
                            = formatIsSupported
                                ? PortAudio.DEFAULT_SAMPLE_RATE
                                : defaultSampleRate;
                    }
                    finally
                    {
                        PortAudio.PaStreamParameters_free(streamParameters);
                    }
                }
            }
        }
        else
            supportedSampleRate = PortAudio.DEFAULT_SAMPLE_RATE;
        return supportedSampleRate;
    }

    @Override
    public String toString()
    {
        return "PortAudio";
    }
}
