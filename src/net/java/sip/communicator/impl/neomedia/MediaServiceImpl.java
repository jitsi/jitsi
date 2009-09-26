/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;

/**
 * @author Lubomir Marinov
 */
public class MediaServiceImpl
    implements MediaService
{

    /**
     * The value of the <tt>devices</tt> property of <tt>MediaServiceImpl</tt>
     * when no <tt>MediaDevice</tt>s are available. Explicitly defined in order
     * to reduce unnecessary allocations.
     */
    private static final List<MediaDevice> EMPTY_DEVICES
        = Collections.emptyList();

    /**
     * The <tt>net.java.sip.communicator.impl.media.MediaServiceImpl</tt> this
     * instance delegates to for functionality it already supports in order to
     * keep this instance as compatible with it as possible. 
     */
    private final net.java.sip.communicator.impl.media.MediaServiceImpl mediaServiceImpl;

    /**
     * Initializes a new <tt>MediaServiceImpl</tt> instance which is to delegate
     * to a specific
     * <tt>net.java.sip.communicator.impl.media.MediaServiceImpl</tt> for
     * functionality it already supports in order to keep the new instance as
     * compatible with the specified <tt>mediaServiceImpl</tt> as possible.
     *
     * @param mediaServiceImpl the
     * <tt>net.java.sip.communicator.impl.media.MediaServiceImpl</tt> the new
     * instance is to delegate to
     */
    public MediaServiceImpl(
        net.java.sip.communicator.impl.media.MediaServiceImpl mediaServiceImpl)
    {
        if (mediaServiceImpl == null)
            throw new NullPointerException("mediaServiceImpl");

        this.mediaServiceImpl = mediaServiceImpl;
    }

    /*
     * Implements MediaService#createMediaStream(StreamConnector, MediaDevice).
     */
    public MediaStream createMediaStream(
            StreamConnector connector,
            MediaDevice device)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Implements MediaService#getDefaultDevice(MediaType).
     */
    public MediaDevice getDefaultDevice(MediaType mediaType)
    {
        CaptureDeviceInfo captureDeviceInfo;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfo
                = mediaServiceImpl
                    .getDeviceConfiguration().getAudioCaptureDevice();
            break;
        case VIDEO:
            captureDeviceInfo
                = mediaServiceImpl
                    .getDeviceConfiguration().getVideoCaptureDevice();
            break;
        default:
            captureDeviceInfo = null;
            break;
        }

        return
            (captureDeviceInfo == null)
                ? null
                : new CaptureMediaDevice(captureDeviceInfo, mediaType);
    }

    /*
     * Implements MediaService#getDevices(MediaType).
     */
    public List<MediaDevice> getDevices(MediaType mediaType)
    {
        CaptureDeviceInfo[] captureDeviceInfos;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfos
                = mediaServiceImpl
                    .getDeviceConfiguration().getAvailableAudioCaptureDevices();
            break;
        case VIDEO:
            captureDeviceInfos
                = mediaServiceImpl
                    .getDeviceConfiguration().getAvailableVideoCaptureDevices();
            break;
        default:
            captureDeviceInfos = null;
            break;
        }

        List<MediaDevice> captureDevices;

        if ((captureDeviceInfos == null) || (captureDeviceInfos.length == 0))
            captureDevices = EMPTY_DEVICES;
        else
        {
            captureDevices
                = new ArrayList<MediaDevice>(captureDeviceInfos.length);
            for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
                captureDevices
                    .add(new CaptureMediaDevice(captureDeviceInfo, mediaType));
        }
        return captureDevices;
    }
}
