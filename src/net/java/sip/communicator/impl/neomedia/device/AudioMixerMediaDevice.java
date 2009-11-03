/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.conference.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements a <tt>MediaDevice</tt> which performs audio mixing using
 * {@link AudioMixer}.
 *
 * @author Lubomir Marinov
 */
public class AudioMixerMediaDevice
    extends AbstractMediaDevice
{

    /**
     * The <tt>AudioMixer</tt> which performs audio mixing in this
     * <tt>MediaDevice</tt> (and rather the session that it represents).
     */
    private AudioMixer audioMixer;

    /**
     * The actual <tt>AudioMediaDeviceImpl</tt> wrapped by this instance for the
     * purposes of audio mixing and used by {@link #audioMixer} as its
     * <tt>CaptureDevice</tt>.
     */
    private final AudioMediaDeviceImpl device;

    /**
     * Initializes a new <tt>AudioMixerMediaDevice</tt> instance which is to
     * enable audio mixing on a specific <tt>AudioMediaDeviceImpl</tt>.
     *
     * @param device the <tt>AudioMediaDeviceImpl</tt> which the new instance is
     * to enable audio mixing on
     */
    public AudioMixerMediaDevice(AudioMediaDeviceImpl device)
    {

        /*
         * AudioMixer is initialized with a CaptureDevice so we have to be sure
         * that the wrapped device can provide one.
         */
        if (!device.getDirection().allowsSending())
            throw new IllegalArgumentException("device");

        this.device = device;
    }

    /**
     * Creates a <tt>DataSource</tt> instance for this <tt>MediaDevice</tt>
     * which gives access to the captured media.
     *
     * @return a <tt>DataSource</tt> instance which gives access to the media
     * captured by this <tt>MediaDevice</tt>
     * @see AbstractMediaDevice#createOutputDataSource()
     */
    AudioMixingPushBufferDataSource createOutputDataSource()
    {
        return getAudioMixer().createOutputDataSource();
    }

    /**
     * Creates a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>.
     *
     * @return a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>
     * @see AbstractMediaDevice#createSession()
     */
    @Override
    public MediaDeviceSession createSession()
    {
        return new AudioMixerMediaDeviceSession();
    }

    /**
     * Gets the <tt>AudioMixer</tt> which performs audio mixing in this
     * <tt>MediaDevice</tt> (and rather the session it represents). If it still
     * does not exist, it is created.
     *
     * @return the <tt>AudioMixer</tt> which performs audio mixing in this
     * <tt>MediaDevice</tt> (and rather the session it represents)
     */
    private AudioMixer getAudioMixer()
    {
        if (audioMixer == null)
            audioMixer = new AudioMixer(device.getCaptureDevice());
        return audioMixer;
    }

    /**
     * Returns the <tt>MediaDirection</tt> supported by this device.
     *
     * @return {@link MediaDirection#SENDONLY} if this is a read-only device,
     * {@link MediaDirection#RECVONLY} if this is a write-only device or
     * {@link MediaDirection#SENDRECV} if this <tt>MediaDevice</tt> can both
     * capture and render media
     * @see MediaDevice#getDirection()
     */
    public MediaDirection getDirection()
    {
        return device.getDirection();
    }

    /**
     * Gets the <tt>MediaFormat</tt> in which this <t>MediaDevice</tt> captures
     * media.
     *
     * @return the <tt>MediaFormat</tt> in which this <tt>MediaDevice</tt>
     * captures media
     * @see MediaDevice#getFormat()
     */
    public MediaFormat getFormat()
    {
        return device.getFormat();
    }

    /**
     * Gets the <tt>MediaType</tt> that this device supports.
     *
     * @return {@link MediaType#AUDIO} if this is an audio device or
     * {@link MediaType#VIDEO} if this is a video device
     * @see MediaDevice#getMediaType()
     */
    public MediaType getMediaType()
    {
        return device.getMediaType();
    }

    /**
     * Gets a list of <tt>MediaFormat</tt>s supported by this
     * <tt>MediaDevice</tt>.
     *
     * @return the list of <tt>MediaFormat</tt>s supported by this device
     * @see MediaDevice#getSupportedFormats()
     */
    public List<MediaFormat> getSupportedFormats()
    {
        return device.getSupportedFormats();
    }

    /**
     * Represents the <tt>MediaDeviceSession</tt> specific to one of the many
     * possible <tt>MediaStream</tt>s using this <tt>MediaDevice</tt> for audio
     * mixing.
     */
    private class AudioMixerMediaDeviceSession
        extends MediaDeviceSession
    {

        /**
         * Initializes a new <tt>AudioMixingMediaDeviceSession</tt> which is to
         * represent the <tt>MediaDeviceSession</tt> of one of the many possible
         * <tt>MediaStream</tt>s using this <tt>MediaDevice</tt> for audio
         * mixing.
         */
        public AudioMixerMediaDeviceSession()
        {
            super(AudioMixerMediaDevice.this);
        }

        /**
         * Adds a <tt>ReceiveStream</tt> to this <tt>MediaDeviceSession</tt> to
         * be played back on the associated <tt>MediaDevice</tt> and a specific
         * <tt>DataSource</tt> is to be used to access its media data during the
         * playback. The <tt>DataSource</tt> is explicitly specified in order to
         * allow extenders to override the <tt>DataSource</tt> of the
         * <tt>ReceiveStream</tt> (e.g. create a clone of it).
         *
         * @param receiveStream the <tt>ReceiveStream</tt> to be played back by
         * this <tt>MediaDeviceSession</tt> on its associated
         * <tt>MediaDevice</tt>
         * @param receiveStreamDataSource the <tt>DataSource</tt> to be used for
         * accessing the media data of <tt>receiveStream</tt> during its
         * playback
         * @see MediaDeviceSession#addReceiveStream(ReceiveStream, DataSource)
         */
        @Override
        protected void addReceiveStream(
                ReceiveStream receiveStream,
                DataSource receiveStreamDataSource)
        {
            DataSource captureDevice = getCaptureDevice();
            AudioMixingPushBufferDataSource audioMixingDataSource;
            DataSource receiveStreamDataSourceForPlayback;

            if (captureDevice instanceof AudioMixingPushBufferDataSource)
            {
                audioMixingDataSource
                    = (AudioMixingPushBufferDataSource) captureDevice;
                receiveStreamDataSourceForPlayback
                    = getAudioMixer().getLocalOutputDataSource();
            }
            else
            {
                audioMixingDataSource = null;
                receiveStreamDataSourceForPlayback = receiveStreamDataSource;
            }

            super
                .addReceiveStream(
                    receiveStream,
                    receiveStreamDataSourceForPlayback);

            if (audioMixingDataSource != null)
                audioMixingDataSource
                    .addInputDataSource(receiveStreamDataSource);
        }
    }
}
