/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.video4linux2.*;

/**
 * Discovers and registers <tt>CaptureDevice</tt>s which implement the Video for
 * Linux Two API Specification with JMF.
 *
 * @author Lubomir Marinov
 */
public class Video4Linux2Auto
{

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying
     * <tt>CaptureDevice</tt> which implement the Video for Linux Two API
     * Specification.
     */
    public static final String LOCATOR_PROTOCOL = "video4linux2";

    /**
     * Initializes a new <tt>Video4Linux2Auto</tt> instance which discovers and
     * registers <tt>CaptureDevice</tt>s which implement the Video for Linux Two
     * API Specification with JMF.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering <tt>CaptureDevice</tt>s which implement the Video for Linux
     * Two API Specification with JMF
     */
    public Video4Linux2Auto()
        throws Exception
    {
        String baseDeviceName = "/dev/video";
        boolean captureDeviceInfoIsAdded = discoverAndRegister(baseDeviceName);

        for (int deviceMinorNumber = 0;
                deviceMinorNumber <= 63;
                deviceMinorNumber++)
        {
            captureDeviceInfoIsAdded
                = discoverAndRegister(baseDeviceName + deviceMinorNumber)
                    || captureDeviceInfoIsAdded;
        }
        if (captureDeviceInfoIsAdded)
            CaptureDeviceManager.commit();
    }

    /**
     * Discovers and registers a <tt>CaptureDevice</tt> implementing the Video
     * for Linux Two API Specification with a specific device name with JMF.
     *
     * @param deviceName the device name of a candidate for a
     * <tt>CaptureDevice</tt> implementing the Video for Linux Two API
     * Specification to be discovered and registered with JMF
     * @return <tt>true</tt> if a <tt>CaptureDeviceInfo</tt> for the specified
     * <tt>CaptureDevice</tt> has been added to <tt>CaptureDeviceManager</tt>;
     * otherwise, <tt>false</tt>
     * @throws Exception if anything goes wrong while discovering and
     * registering the specified <tt>CaptureDevice</tt> with JMF
     */
    private boolean discoverAndRegister(String deviceName)
        throws Exception
    {
        int fd = Video4Linux2.open(deviceName, Video4Linux2.O_RDWR);
        boolean captureDeviceInfoIsAdded = false;

        if (-1 != fd)
        {
            try
            {
                long v4l2_capability = Video4Linux2.v4l2_capability_alloc();

                if (0 != v4l2_capability)
                {
                    try
                    {
                        if ((Video4Linux2.ioctl(
                                        fd,
                                        Video4Linux2.VIDIOC_QUERYCAP,
                                        v4l2_capability)
                                    != -1)
                                && ((Video4Linux2
                                            .v4l2_capability_getCapabilities(
                                                v4l2_capability)
                                        & Video4Linux2.V4L2_CAP_VIDEO_CAPTURE)
                                    == Video4Linux2.V4L2_CAP_VIDEO_CAPTURE))
                        {
                            captureDeviceInfoIsAdded
                                = register(deviceName, fd, v4l2_capability);
                        }
                    }
                    finally
                    {
                        Video4Linux2.free(v4l2_capability);
                    }
                }
            }
            finally
            {
                Video4Linux2.close(fd);
            }
        }
        return captureDeviceInfoIsAdded;
    }

    /**
     * Registers a <tt>CaptureDevice</tt> implementing the Video for Linux Two
     * API Specification with a specific device name, a specific <tt>open()</tt>
     * file descriptor and a specific <tt>v4l2_capability</tt> with JMF.
     *
     * @param deviceName name of the device (i.e. /dev/videoX)
     * @param fd file descriptor of the device
     * @param v4l2_capability device V4L2 capability
     * @return <tt>true</tt> if a <tt>CaptureDeviceInfo</tt> for the specified
     * <tt>CaptureDevice</tt> has been added to <tt>CaptureDeviceManager</tt>;
     * otherwise, <tt>false</tt>
     * @throws Exception if anything goes wrong while registering the specified
     * <tt>CaptureDevice</tt> with JMF
     */
    private boolean register(String deviceName, int fd, long v4l2_capability)
        throws Exception
    {
        long v4l2_format
            = Video4Linux2.v4l2_format_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);
        int pixelformat = 0;

        if (0 != v4l2_format)
        {
            try
            {
                if (Video4Linux2.ioctl(
                            fd,
                            Video4Linux2.VIDIOC_G_FMT,
                            v4l2_format)
                        != -1)
                {
                    long fmtPix
                        = Video4Linux2.v4l2_format_getFmtPix(v4l2_format);

                    pixelformat
                        = Video4Linux2.v4l2_pix_format_getPixelformat(fmtPix);
                    if ((Video4Linux2.V4L2_PIX_FMT_RGB24 != pixelformat)
                            || (FFmpeg.PIX_FMT_NONE
                                    == DataSource.getFFmpegPixFmt(pixelformat)))
                    {
                        Video4Linux2.v4l2_pix_format_setPixelformat(
                                fmtPix,
                                Video4Linux2.V4L2_PIX_FMT_RGB24);
                        if (Video4Linux2.ioctl(
                                    fd,
                                    Video4Linux2.VIDIOC_S_FMT,
                                    v4l2_format)
                                != -1)
                        {
                            pixelformat
                                = Video4Linux2.v4l2_pix_format_getPixelformat(
                                        fmtPix);
                        }
                    }
                }
            }
            finally
            {
                Video4Linux2.free(v4l2_format);
            }
        }

        Format format;

        if (Video4Linux2.V4L2_PIX_FMT_RGB24 == pixelformat)
            format = new RGBFormat();
        else
        {
            int ffmpegPixFmt = DataSource.getFFmpegPixFmt(pixelformat);

            if (FFmpeg.PIX_FMT_NONE != ffmpegPixFmt)
                format = new AVFrameFormat(ffmpegPixFmt, pixelformat);
            else
                return false;
        }

        String name = Video4Linux2.v4l2_capability_getCard(v4l2_capability);

        if ((name == null) || (name.length() == 0))
            name = deviceName;
        else
            name += " (" + deviceName + ")";
        CaptureDeviceManager.addDevice(
                new CaptureDeviceInfo(
                        name,
                        new MediaLocator(LOCATOR_PROTOCOL + ":" + deviceName),
                        new Format[] { format }));
        return true;
    }
}
