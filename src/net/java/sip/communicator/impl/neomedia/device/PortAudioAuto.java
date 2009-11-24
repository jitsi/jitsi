/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio.*;
import net.java.sip.communicator.util.*;

import com.sun.media.util.*;

/**
 * Creates PortAudio capture devices by enumerating all host devices that have
 * input channels.
 *
 * @author Damian Minkov
 */
public class PortAudioAuto
{
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

    PortAudioAuto() throws Exception
    {
        // if PortAudio has a problem initializing like missing native
        // components it will trow exception here and PortAudio rendering will
        // not be inited.
        PortAudioManager.getInstance();

        // enable jmf logging, so we can track codec chains and formats
        if(Logger.getLogger(PortAudioAuto.class).isDebugEnabled())
            Registry.set("allowLogging", true);

        int deviceCount = PortAudio.Pa_GetDeviceCount();
        int deviceIndex = 0;

        int defaultInputDeviceIx = PortAudio.Pa_GetDefaultInputDevice();
        int defaultOutputDeviceIx = PortAudio.Pa_GetDefaultOutputDevice();

        Vector<CaptureDeviceInfo> playbackDevVector =
            new Vector<CaptureDeviceInfo>();

        for (; deviceIndex < deviceCount; deviceIndex++)
        {
            long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);
            int maxInputChannels =
                PortAudio.PaDeviceInfo_getMaxInputChannels(deviceInfo);
            int maxOutputChannels =
                PortAudio.PaDeviceInfo_getMaxOutputChannels(deviceInfo);

            CaptureDeviceInfo jmfInfo =
                    new CaptureDeviceInfo(
                        PortAudio.PaDeviceInfo_getName(deviceInfo),
                        new MediaLocator(
                            PortAudioUtils.LOCATOR_PREFIX + deviceIndex),
                        new Format[]{DataSource.getCaptureFormat()});

            if(maxInputChannels > 0)
            {
                CaptureDeviceManager.addDevice(jmfInfo);
            }

            if(maxOutputChannels > 0)
            {
                playbackDevVector.add(jmfInfo);
            }

            if(deviceIndex == defaultInputDeviceIx)
                defaultCaptureDevice = jmfInfo;

            if(deviceIndex == defaultOutputDeviceIx)
                defaultPlaybackDevice = jmfInfo;
        }

        playbackDevices = playbackDevVector.toArray(new CaptureDeviceInfo[0]);

        CaptureDeviceManager.commit();

        // now add it as available audio system to DeviceConfiguration
        DeviceConfiguration.addAudioSystem(
            DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO);
    }
}
