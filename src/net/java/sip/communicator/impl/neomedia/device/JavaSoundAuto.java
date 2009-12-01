/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;

import javax.sound.sampled.*;

import net.java.sip.communicator.util.*;

/**
 * Detects javasound and registers capture devices.
 *
 * @author Damian Minkov
 */
public class JavaSoundAuto
{
    /**
     * The <tt>Logger</tt> used by the <tt>JavaSoundAuto</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(JavaSoundAuto.class);

    /**
     * Creates <tt>JavaSoundAuto</tt> and checks is javasound supported
     * on current operating system.
     */
    @SuppressWarnings("unchecked") //legacy JMF code.
    public JavaSoundAuto()
    {
        boolean supported = false;
        
        try
        {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                    null,
                    AudioSystem.NOT_SPECIFIED);
            supported = AudioSystem.isLineSupported(info);
        } catch (Throwable thr)
        {
            supported = false;
            logger.error("Failed detecting java sound audio", thr);
        }

        logger.info("JavaSound Capture Supported = " + supported);

        if (supported)
        {
            // It's there, start to register JavaSound with CaptureDeviceManager
            Vector<CaptureDeviceInfo> devices
                = (Vector) CaptureDeviceManager.getDeviceList(null).clone();

            // remove the old javasound capturers
            String name;
            Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
            while (enumeration.hasMoreElements())
            {
                CaptureDeviceInfo cdi = enumeration.nextElement();
                name = cdi.getName();
                if (name.startsWith("JavaSound"))
                    CaptureDeviceManager.removeDevice(cdi);
            }

            // collect javasound capture device info from JavaSoundSourceStream
            // and register them with CaptureDeviceManager
            CaptureDeviceInfo[] cdi
                =  com.sun.media.protocol.javasound.JavaSoundSourceStream
                    .listCaptureDeviceInfo();
            if ( cdi != null )
            {
                for (int i = 0; i < cdi.length; i++)
                    CaptureDeviceManager.addDevice(cdi[i]);

                try
                {
                    CaptureDeviceManager.commit();
                    logger.info("JavaSoundAuto: Committed ok");
                } catch (java.io.IOException ioe)
                {
                    logger.error("JavaSoundAuto: error committing cdm");
                }
            }

            // now add it as available audio system to DeviceConfiguration
            DeviceConfiguration.addAudioSystem(
                DeviceConfiguration.AUDIO_SYSTEM_JAVASOUND);
        }
    }
}
