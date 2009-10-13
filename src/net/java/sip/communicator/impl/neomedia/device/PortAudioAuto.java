/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.media.protocol.portaudio.*;

/**
 * Creates PortAudio capture devices by enumerating all the host devices that
 * have input channels.
 *
 * @author Damian Minkov
 */
public class PortAudioAuto
{
    /**
     * An array of the devices that can be used for playback.
     */
    public static CaptureDeviceInfo[] playbackDevices = null;

    PortAudioAuto() throws Exception
    {
        // if PortAudio has a problem initializing like missing native
        // components it will trow exception here and PortAudio rendering will
        // not be inited.
        PortAudio.initialize();

        int deviceCount = PortAudio.Pa_GetDeviceCount();
        int deviceIndex = 0;

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
                            PortAudioStream.LOCATOR_PREFIX + deviceIndex),
                        PortAudioStream.getFormats());

            if(maxInputChannels > 0)
            {
                CaptureDeviceManager.addDevice(jmfInfo);
            }

            if(maxOutputChannels > 0)
            {
                playbackDevVector.add(jmfInfo);
            }
        }

        playbackDevices = playbackDevVector.toArray(new CaptureDeviceInfo[0]);

        CaptureDeviceManager.commit();

        // now add it as available audio system to DeviceConfiguration
        DeviceConfiguration.addAudioSystem(
            DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO);
    }
}
