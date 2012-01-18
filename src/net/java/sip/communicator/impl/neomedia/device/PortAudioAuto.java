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
public class PortAudioAuto
{
    /**
     * The <tt>Logger</tt> used by the <tt>PortAudioAuto</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(PortAudioAuto.class);

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying PortAudio
     * <tt>CaptureDevice</tt>s
     */
    public static final String LOCATOR_PROTOCOL = "portaudio";

    /**
     * An array of the devices that can be used for playback.
     */
    public static CaptureDeviceInfo[] playbackDevices = null;

    /**
     * The default playback device.
     */
    public static CaptureDeviceInfo defaultPlaybackDevice = null;

    /**
     * The default capture device.
     */
    public static CaptureDeviceInfo defaultCaptureDevice = null;

    /**
     * Is PortAudio supported on current platform.
     */
    private static boolean supported = false;

    /**
     * Initializes a new <tt>PortAudioAuto</tt> instance which creates PortAudio
     * capture devices by enumerating all host devices with input channels.
     *
     * @throws Exception if anything wrong happens while creating the PortAudio
     * capture devices
     */
    PortAudioAuto()
        throws Exception
    {
        reinit();
        
        // now add it as available audio system to DeviceConfiguration
        DeviceConfiguration.addAudioSystem(
                DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO);

        supported = true;
    }
    
    /**
     * PortAudio device changed callback.
     */
    public void deviceChanged()
    {
        try
        {
            reinit();
        }
        catch(Exception e)
        {
            logger.warn("Error while reinitialize PortAudio devices", e);
        }
    }
    
    /**
     * Reinitializes PortAudio system.
     * @throws Exception if anything wrong happens while creating the PortAudio
     * capture devices
     */
    public void reinit()
        throws Exception
    {
        // if PortAudio has a problem initializing like missing native
        // components it will trow exception here and PortAudio rendering will
        // not be inited.
        int deviceCount = PortAudio.Pa_GetDeviceCount();

        int defaultInputDeviceIx = PortAudio.Pa_GetDefaultInputDevice();
        int defaultOutputDeviceIx = PortAudio.Pa_GetDefaultOutputDevice();
        
        playbackDevices = null;
        defaultPlaybackDevice = null;
        defaultCaptureDevice = null;

        /*
        // for windows we will search for directsound devices, to set them as
        // default, we need info for default devices host api
        PortAudio.PaHostApiTypeId defaultInputHostApi
            = PortAudio.PaHostApiTypeId.undefined;
        PortAudio.PaHostApiTypeId defaultOutputHostApi
            = PortAudio.PaHostApiTypeId.undefined;

        if(defaultInputDeviceIx != PortAudio.paNoDevice)
        {
            defaultInputHostApi = PortAudio.PaHostApiTypeId.valueOf(
                PortAudio.PaDeviceInfo_getHostApi(
                    PortAudio.Pa_GetDeviceInfo(defaultInputDeviceIx)));
        }

        if(defaultOutputDeviceIx != PortAudio.paNoDevice)
        {
            defaultOutputHostApi = PortAudio.PaHostApiTypeId.valueOf(
                PortAudio.PaDeviceInfo_getHostApi(
                    PortAudio.Pa_GetDeviceInfo(defaultOutputDeviceIx)));
        }
        */

        AudioFormat fmt = new AudioFormat(
            AudioFormat.LINEAR,
            Format.NOT_SPECIFIED,
            Format.NOT_SPECIFIED,
            Format.NOT_SPECIFIED,
            AudioFormat.LITTLE_ENDIAN,
            AudioFormat.SIGNED,
            Format.NOT_SPECIFIED /* frameSizeInBits */,
            Format.NOT_SPECIFIED /* frameRate */,
            Format.byteArray);
        
        Iterator<?> it = CaptureDeviceManager.getDeviceList(fmt).iterator();
        List<CaptureDeviceInfo> devicesToRemove = 
            new ArrayList<CaptureDeviceInfo>();
        
        while(it.hasNext())
        {
            CaptureDeviceInfo ifo = (CaptureDeviceInfo)it.next();

            if(!ifo.getLocator().getProtocol().equals("portaudio"))
            {
                continue;
            }
            else
            {
                devicesToRemove.add(ifo);
            }
        }
        
        for(CaptureDeviceInfo info : devicesToRemove)
        {
            CaptureDeviceManager.removeDevice(info);
        }
        if(devicesToRemove.size() > 0)
            CaptureDeviceManager.commit();
        
        Vector<CaptureDeviceInfo> playbackDevVector =
            new Vector<CaptureDeviceInfo>();
        int channels = 1;
        int sampleSizeInBits = 16;
        long sampleFormat = PortAudio.getPaSampleFormat(sampleSizeInBits);

        for (int deviceIndex = 0; deviceIndex < deviceCount; deviceIndex++)
        {
            long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);
            int maxInputChannels =
                PortAudio.PaDeviceInfo_getMaxInputChannels(deviceInfo);
            int maxOutputChannels =
                PortAudio.PaDeviceInfo_getMaxOutputChannels(deviceInfo);
            /*
            PortAudio.PaHostApiTypeId hostApi =
                PortAudio.PaHostApiTypeId.valueOf(
                    PortAudio.PaDeviceInfo_getHostApi(deviceInfo));
            */

            String devName = PortAudio.PaDeviceInfo_getName(deviceInfo);
            if (devName != null)
                devName = devName.trim();

            CaptureDeviceInfo jmfInfo
                = new CaptureDeviceInfo(
                        devName,
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

            /*
            // under windows we search for the first direct sound device
            // that we can use (output or input) and use it as default one
            if(OSUtils.IS_WINDOWS)
            {
                if(defaultInputHostApi != PortAudio.PaHostApiTypeId
                        .paDirectSound
                    && hostApi == PortAudio.PaHostApiTypeId.paDirectSound
                    && maxInputChannels > 0)
                {
                    defaultInputHostApi = hostApi;
                    defaultInputDeviceIx = deviceIndex;
                }

                if(defaultOutputHostApi
                        != PortAudio.PaHostApiTypeId.paDirectSound
                    && hostApi == PortAudio.PaHostApiTypeId.paDirectSound
                    && maxOutputChannels > 0)
                {
                    defaultOutputHostApi = hostApi;
                    defaultOutputDeviceIx = deviceIndex;
                }
            }
            */

            if(maxInputChannels > 0)
            {
                CaptureDeviceManager.addDevice(jmfInfo);

                if(logger.isDebugEnabled())
                    logger.debug("Adding capture device: [" + devName + "]");
            }
            if(maxOutputChannels > 0)
            {
                playbackDevVector.add(jmfInfo);

                if(logger.isDebugEnabled())
                    logger.debug("Adding playback device: [" + devName + "]");
            }

            if(deviceIndex == defaultInputDeviceIx)
            {
                defaultCaptureDevice = jmfInfo;

                if(logger.isDebugEnabled())
                    logger.debug("Default capture device: [" + devName + "]");
            }
            if(deviceIndex == defaultOutputDeviceIx)
            {
                defaultPlaybackDevice = jmfInfo;

                if(logger.isDebugEnabled())
                    logger.debug("Default playback device: [" + devName + "]");
            }
        }

        playbackDevices = playbackDevVector.toArray(new CaptureDeviceInfo[0]);
        CaptureDeviceManager.commit();
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
    public static double getSupportedSampleRate(
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

    /**
     * Determines whether PortAudio is supported.
     *
     * @return <tt>true</tt> if PortAudio is supported; otherwise,
     * <tt>false</tt>
     */
    public static boolean isSupported()
    {
        return supported;
    }
}
