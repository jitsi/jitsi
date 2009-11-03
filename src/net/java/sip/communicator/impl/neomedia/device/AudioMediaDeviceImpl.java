/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.*;

/**
 * Extends <tt>MediaDeviceImpl</tt> with audio-specific functionality.
 *
 * @author Lubomir Marinov
 */
public class AudioMediaDeviceImpl
    extends MediaDeviceImpl
{

    /**
     * Initializes a new <tt>AudioMediaDeviceImpl</tt> instance with
     * <tt>MediaDirection</tt> which does not allow sending i.e. the new
     * instance cannot be used to capture audio.
     */
    public AudioMediaDeviceImpl()
    {
        super(MediaType.AUDIO);
    }

    /**
     * Initializes a new <tt>AudioMediaDeviceImpl</tt> instance which is to
     * provide an implementation of <tt>MediaDevice</tt> for a specific audio
     * <tt>CaptureDevice</tt>.
     *
     * @param captureDevice the audio <tt>CaptureDevice</tt> the new instance is
     * to provide an implementation of <tt>MediaDevice</tt> for
     */
    public AudioMediaDeviceImpl(CaptureDevice captureDevice)
    {
        super(captureDevice, MediaType.AUDIO);
    }

    /**
     * Initializes a new <tt>AudioMediaDeviceImpl</tt> instance which is to
     * provide an implementation of <tt>MediaDevice</tt> for an audio
     * <tt>CaptureDevice</tt> with a specific <tt>CaptureDeviceInfo</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> of the audio
     * <tt>CaptureDevice</tt> the new instance is to provide an implementation
     * of <tt>MediaDevice</tt> for
     */
    public AudioMediaDeviceImpl(CaptureDeviceInfo captureDeviceInfo)
    {
        super(captureDeviceInfo, MediaType.AUDIO);
    }

    /**
     * Determines whether this <tt>MediaDevice</tt> will provide silence instead
     * of actual captured data next time it is read.
     *
     * @return <tt>true</tt> if this <tt>MediaDevice</tt> will provide silence
     * instead of actual captured data next time it is read; <tt>false</tt>,
     * otherwise
     */
    public boolean isMute()
    {
        CaptureDevice captureDevice = getCaptureDevice(false);

        if (captureDevice instanceof MutePushBufferDataSource)
            return ((MutePushBufferDataSource) captureDevice).isMute();

        /*
         * If there is no underlying CaptureDevice, this instance is mute
         * because it cannot capture any audio.
         */
        return !getDirection().allowsSending();
    }

    /**
     * Sets the JMF <tt>CaptureDevice</tt> this instance wraps and provides a
     * <tt>MediaDevice</tt> implementation for. Tries to enable muting.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> this instance is to
     * wrap and provide a <tt>MediaDevice</tt> implementation for
     * @see MediaDeviceImpl#setCaptureDevice(CaptureDevice)
     */
    @Override
    protected void setCaptureDevice(CaptureDevice captureDevice)
    {
        if (captureDevice instanceof PushBufferDataSource)
            captureDevice
                = new MutePushBufferDataSource(
                        (PushBufferDataSource) captureDevice);

        super.setCaptureDevice(captureDevice);
    }

    /**
     * Sets the indicator which determines whether this <tt>MediaDevice</tt>
     * will start providing silence instead of actual captured data next time it
     * is read.
     *
     * @param mute <tt>true</tt> to have this <tt>MediaDevice</tt> start
     * providing silence instead of actual captured data next time it is read;
     * otherwise, <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        CaptureDevice captureDevice = getCaptureDevice();

        if (captureDevice instanceof MutePushBufferDataSource)
            ((MutePushBufferDataSource) captureDevice).setMute(mute);
    }
}
