/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.conference.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaDeviceImpl</tt> with audio-specific functionality.
 *
 * @author Lubomir Marinov
 */
public class AudioMediaDeviceImpl
    extends MediaDeviceImpl
{

    /**
     * The <tt>Logger</tt> used by the <tt>AudioMediaDeviceImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMediaDeviceImpl.class);

    /**
     * The <tt>AudioMixer</tt> which enables sharing an exclusive
     * <tt>CaptureDevice</tt> such as JavaSound between multiple
     * <tt>CaptureDevice</tt> users.
     */
    private AudioMixer captureDeviceSharing;

    /**
     * The <tt>List</tt> of RTP extensions supported by this device (at the time
     * of writing this list is only filled for audio devices and is
     * <tt>null</tt> otherwise).
     */
    private List<RTPExtension> rtpExtensions = null;

    /**
     * Initializes a new <tt>AudioMediaDeviceImpl</tt> instance which represents
     * a <tt>MediaDevice</tt> with <tt>MediaType</tt> <tt>AUDIO</tt> and a
     * <tt>MediaDirection</tt> which does not allow sending.
     */
    public AudioMediaDeviceImpl()
    {
        super(MediaType.AUDIO);
    }

    /**
     * Initializes a new <tt>AudioMediaDeviceImpl</tt> which is to provide an
     * implementation of <tt>MediaDevice</tt> with <tt>MediaType</tt>
     * <tt>AUDIO</tt> to a <tt>CaptureDevice</tt> with a specific
     * <tt>CaptureDeviceInfo</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> of the
     * <tt>CaptureDevice</tt> to which the new instance is to provide an
     * implementation of <tt>MediaDevice</tt>
     */
    public AudioMediaDeviceImpl(CaptureDeviceInfo captureDeviceInfo)
    {
        super(captureDeviceInfo, MediaType.AUDIO);
    }

    /**
     * Connects to a specific <tt>CaptureDevice</tt> given in the form of a
     * <tt>DataSource</tt>.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to be connected to
     * @throws IOException if anything wrong happens while connecting to the
     * specified <tt>captureDevice</tt>
     * @see AbstractMediaDevice#connect(DataSource)
     */
    @Override
    public void connect(DataSource captureDevice)
        throws IOException
    {
        super.connect(captureDevice);

        /*
         * 1. Changing buffer size. The default buffer size (for JavaSound) is
         * 125 milliseconds - 1/8 sec. On Mac OS X this leads to an exception
         * and no audio capture. A value of 30 for the buffer fixes the problem
         * and is OK when using some PSTN gateways.
         *
         * 2. Changing to 60. When it is 30 there are some issues with Asterisk
         * and NAT (we don't start to send stream and so Asterisk RTP part
         * doesn't notice that we are behind NAT).
         *
         * 3. Do not set buffer length on Linux as it completely breaks audio
         * capture.
         */
        if(!OSUtils.IS_LINUX)
        {
            BufferControl bufferControl
                = (BufferControl)
                    captureDevice.getControl(BufferControl.class.getName());

            if (bufferControl != null)
                bufferControl.setBufferLength(60 /* millis */);
        }
    }

    /**
     * Creates the JMF <tt>CaptureDevice</tt> this instance represents and
     * provides an implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance represents and
     * provides an implementation of <tt>MediaDevice</tt> for; <tt>null</tt>
     * if the creation fails
     */
    @Override
    synchronized CaptureDevice createCaptureDevice()
    {
        CaptureDevice captureDevice = null;

        if (getDirection().allowsSending())
        {
            if (captureDeviceSharing == null)
            {
                String protocol = getCaptureDeviceInfoLocatorProtocol();
                boolean createCaptureDeviceIfNull = true;

                if ("javasound".equalsIgnoreCase(protocol)
                        || PortAudioAuto.LOCATOR_PROTOCOL.equalsIgnoreCase(
                                protocol))
                {
                    captureDevice = super.createCaptureDevice();
                    createCaptureDeviceIfNull = false;
                    if (captureDevice != null)
                    {
                        captureDeviceSharing
                            = createCaptureDeviceSharing(captureDevice);
                        captureDevice
                            = captureDeviceSharing.createOutputDataSource();
                    }
                }
                if ((captureDevice == null) && createCaptureDeviceIfNull)
                    captureDevice = super.createCaptureDevice();
            }
            else
                captureDevice = captureDeviceSharing.createOutputDataSource();
        }
        return captureDevice;
    }

    /**
     * Creates a new <tt>AudioMixer</tt> which is to enable the sharing of a
     * specific explicit <tt>CaptureDevice</tt>
     *
     * @param captureDevice an exclusive <tt>CaptureDevice</tt> for which
     * sharing is to be enabled
     * @return a new <tt>AudioMixer</tt> which enables the sharing of the
     * specified exclusive <tt>captureDevice</tt>
     */
    private AudioMixer createCaptureDeviceSharing(CaptureDevice captureDevice)
    {
        return
            new AudioMixer(captureDevice)
            {
                @Override
                protected void connect(
                        DataSource dataSource,
                        DataSource inputDataSource)
                    throws IOException
                {
                    /*
                     * CaptureDevice needs special connecting as defined by
                     * AbstractMediaDevice and, especially, MediaDeviceImpl.
                     */
                    if (inputDataSource == captureDevice)
                        AudioMediaDeviceImpl.this.connect(dataSource);
                    else
                        super.connect(dataSource, inputDataSource);
                }
            };
    }

    /**
     * Initializes a new <tt>Renderer</tt> instance which is to play back media
     * on this <tt>MediaDevice</tt>.
     *
     * @return a new <tt>Renderer</tt> instance which is to play back media on
     * this <tt>MediaDevice</tt> or <tt>null</tt> if a suitable
     * <tt>Renderer</tt> is to be chosen irrespective of this
     * <tt>MediaDevice</tt>
     */
    @Override
    Renderer createRenderer()
    {
        String protocol = getCaptureDeviceInfoLocatorProtocol();
        String className;

        if ("javasound".equalsIgnoreCase(protocol))
            className = "net.sf.fmj.media.renderer.audio.JavaSoundRenderer";
        else if (PortAudioAuto.LOCATOR_PROTOCOL.equalsIgnoreCase(protocol))
        {
            className
                = "net.java.sip.communicator.impl.neomedia.jmfext.media"
                    + ".renderer.audio.PortAudioRenderer";
        }
        else
            className = null;

        if (className != null)
        {
            try
            {
                return (Renderer) Class.forName(className).newInstance();
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                    logger.warn(
                            "Failed to initialize a new "
                                + className
                                + " instance",
                            t);
            }
        }

        return super.createRenderer();
    }

    private String getCaptureDeviceInfoLocatorProtocol()
    {
        CaptureDeviceInfo cdi = getCaptureDeviceInfo();

        if (cdi != null)
        {
            MediaLocator locator = cdi.getLocator();

            if (locator != null)
                return locator.getProtocol();
        }

        return null;
    }
    /**
     * Returns a <tt>List</tt> containing (at the time of writing) a single
     * extension descriptor indicating <tt>RECVONLY</tt> support for
     * mixer-to-client audio levels.
     *
     * @return a <tt>List</tt> containing the <tt>CSRC_AUDIO_LEVEL_URN</tt>
     * extension descriptor.
     */
    @Override
    public List<RTPExtension> getSupportedExtensions()
    {
        if ( rtpExtensions == null)
        {
            rtpExtensions = new ArrayList<RTPExtension>(1);

            URI csrcAudioLevelURN;
            try
            {
                csrcAudioLevelURN = new URI(RTPExtension.CSRC_AUDIO_LEVEL_URN);
            }
            catch (URISyntaxException e)
            {
                // can't happen since CSRC_AUDIO_LEVEL_URN is a valid URI and
                // never changes.
                if (logger.isInfoEnabled())
                    logger.info("Aha! Someone messed with the source!", e);
                return null;
            }

            rtpExtensions.add(new RTPExtension(
                               csrcAudioLevelURN, MediaDirection.RECVONLY));
        }

        return rtpExtensions;
    }
}
