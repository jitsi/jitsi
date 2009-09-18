/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.device;

import javax.media.*;
import net.java.sip.communicator.impl.media.protocol.portaudio.*;

/**
 * Creates Portaudio capture devices.
 *
 * @author Damian Minkov
 */
public class PortAudioAuto
{
    PortAudioAuto() throws Exception
    {
        Format[] formats = new Format[1];

        formats[0] = PortAudioStream.audioFormat;

        CaptureDeviceInfo jmfInfo =
            new CaptureDeviceInfo("portaudio:1",
                    new MediaLocator("portaudio:#" + 1), formats);

        CaptureDeviceManager.addDevice(jmfInfo);

        CaptureDeviceManager.commit();
    }
}
