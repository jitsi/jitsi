/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.conference.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
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
     * The actual <tt>MediaDeviceImpl</tt> wrapped by this instance for the
     * purposes of audio mixing and used by {@link #audioMixer} as its
     * <tt>CaptureDevice</tt>.
     */
    private final MediaDeviceImpl device;

    /**
     * The <tt>MediaDeviceSession</tt> of this <tt>AudioMixer</tt> with
     * {@link #device}.
     */
    private AudioMixerMediaDeviceSession deviceSession;

    /**
     * Initializes a new <tt>AudioMixerMediaDevice</tt> instance which is to
     * enable audio mixing on a specific <tt>MediaDeviceImpl</tt>.
     *
     * @param device the <tt>MediaDeviceImpl</tt> which the new instance is to
     * enable audio mixing on
     */
    public AudioMixerMediaDevice(MediaDeviceImpl device)
    {
        if (!MediaType.AUDIO.equals(device.getMediaType()))
            throw new IllegalArgumentException("device");

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
    public synchronized MediaDeviceSession createSession()
    {
        if (deviceSession == null)
            deviceSession = new AudioMixerMediaDeviceSession();
        return new MediaStreamMediaDeviceSession(deviceSession);
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
            audioMixer = new AudioMixer(device.createCaptureDevice())
            {
                @Override
                protected void read(
                        PushBufferStream stream,
                        Buffer buffer,
                        DataSource dataSource)
                    throws IOException
                {
                    super.read(stream, buffer, dataSource);

                    /*
                     * XXX The audio read from the specified stream has not been
                     * made available to the mixing yet. Slow code here is
                     * likely to degrade the performance of the whole mixer.
                     */

                    if (dataSource == captureDevice)
                    {
                        /*
                         * The audio of the very CaptureDevice to be contributed
                         * to the mix.
                         */
                    }
                    else if (dataSource
                            instanceof ReceiveStreamPushBufferDataSource)
                    {
                        ReceiveStream receiveStream
                            = ((ReceiveStreamPushBufferDataSource) dataSource)
                                .getReceiveStream();
                        /*
                         * The audio of a ReceiveStream to be contributed to the
                         * mix.
                         */
                    }
                }
            };
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
     * Represents the one and only <tt>MediaDeviceSession</tt> with the
     * <tt>MediaDevice</tt> of this <tt>AudioMixer</tt>
     */
    private class AudioMixerMediaDeviceSession
        extends MediaDeviceSession
    {

        /**
         * The list of <tt>MediaDeviceSession</tt>s of <tt>MediaStream</tt>s
         * which use this <tt>AudioMixer</tt>.
         */
        private final List<MediaStreamMediaDeviceSession>
            mediaStreamMediaDeviceSessions
                = new LinkedList<MediaStreamMediaDeviceSession>();

        /**
         * Initializes a new <tt>AudioMixingMediaDeviceSession</tt> which is to
         * represent the <tt>MediaDeviceSession</tt> of this <tt>AudioMixer</tt>
         * with its <tt>MediaDevice</tt>
         */
        public AudioMixerMediaDeviceSession()
        {
            super(AudioMixerMediaDevice.this);
        }

        /**
         * Adds a specific <tt>MediaStreamMediaDeviceSession</tt> to the mix
         * represented by this instance so that it knows when it is in use.
         *
         * @param mediaStreamMediaDeviceSession the
         * <tt>MediaStreamMediaDeviceSession</tt> to be added to the mix
         * represented by this instance
         */
        void addMediaStreamMediaDeviceSession(
                MediaStreamMediaDeviceSession mediaStreamMediaDeviceSession)
        {
            if (mediaStreamMediaDeviceSession == null)
                throw new NullPointerException("mediaStreamMediaDeviceSession");

            synchronized (mediaStreamMediaDeviceSessions)
            {
                if (!mediaStreamMediaDeviceSessions
                        .contains(mediaStreamMediaDeviceSession))
                    mediaStreamMediaDeviceSessions
                        .add(mediaStreamMediaDeviceSession);
            }
        }

        /**
         * Adds a <tt>ReceiveStream</tt> to this <tt>MediaDeviceSession</tt> to
         * be played back on the associated <tt>MediaDevice</tt> and a specific
         * <tt>DataSource</tt> is to be used to access its media data during the
         * playback.
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
            DataSource receiveStreamDataSourceForPlayback;

            if (captureDevice instanceof AudioMixingPushBufferDataSource)
                receiveStreamDataSourceForPlayback
                    = (AudioMixingPushBufferDataSource) captureDevice;
            else
                receiveStreamDataSourceForPlayback = receiveStreamDataSource;

            super
                .addReceiveStream(
                    receiveStream,
                    receiveStreamDataSourceForPlayback);
        }

        /**
         * Creates the <tt>DataSource</tt> that this instance is to read
         * captured media from. Since this is the <tt>MediaDeviceSession</tt> of
         * this <tt>AudioMixer</tt> with its <tt>MediaDevice</tt>, returns the
         * <tt>localOutputDataSource</tt> of the <tt>AudioMixer</tt> i.e. the
         * <tt>DataSource</tt> which represents the mix of all
         * <tt>ReceiveStream</tt>s and excludes the captured data from the
         * <tt>MediaDevice</tt> of the <tt>AudioMixer</tt>.
         *
         * @return the <tt>DataSource</tt> that this instance is to read
         * captured media from
         * @see MediaDeviceSession#createCaptureDevice()
         */
        @Override
        protected DataSource createCaptureDevice()
        {
            return getAudioMixer().getLocalOutputDataSource();
        }

        /**
         * Removes a specific <tt>MediaStreamMediaDeviceSession</tt> from the
         * mix represented by this instance. When the last
         * <tt>MediaStreamMediaDeviceSession</tt> is removed from this instance,
         * it is no longer in use and closes itself thus signaling to its
         * <tt>MediaDevice</tt> that it is no longer in use.
         *
         * @param mediaStreamMediaDeviceSession the
         * <tt>MediaStreamMediaDeviceSession</tt> to be removed from the mix
         * represented by this instance
         */
        void removeMediaStreamMediaDeviceSession(
                MediaStreamMediaDeviceSession mediaStreamMediaDeviceSession)
        {
            if (mediaStreamMediaDeviceSession != null)
                synchronized (mediaStreamMediaDeviceSessions)
                {
                    if (mediaStreamMediaDeviceSessions
                                .remove(mediaStreamMediaDeviceSession)
                            && mediaStreamMediaDeviceSessions.isEmpty())
                        close();
                }
        }
    }

    /**
     * Represents the work of a <tt>MediaStream</tt> with the
     * <tt>MediaDevice</tt> of an <tt>AudioMixer</tt> and the contribution of
     * that <tt>MediaStream</tt> to the mix.
     */
    private static class MediaStreamMediaDeviceSession
        extends MediaDeviceSession
    {

        /**
         * The <tt>MediaDeviceSession</tt> of the <tt>AudioMixer</tt> that this
         * instance exposes to a <tt>MediaStream</tt>. While there are multiple
         * <tt>MediaStreamMediaDeviceSession<tt>s each servicing a specific
         * <tt>MediaStream</tt>, they all share and delegate to one and the same
         * <tt>AudioMixerMediaDeviceSession</tt> so that they all contribute to
         * the mix.
         */
        private final AudioMixerMediaDeviceSession audioMixerMediaDeviceSession;

        /**
         * Initializes a new <tt>MediaStreamMediaDeviceSession</tt> which is to
         * represent the work of a <tt>MediaStream</tt> with the
         * <tt>MediaDevice</tt> of this <tt>AudioMixer</tt> and its contribution
         * to the mix.
         *
         * @param audioMixerMediaDeviceSession the <tt>MediaDeviceSession</tt>
         * of the <tt>AudioMixer</tt> with its <tt>MediaDevice</tt> which the
         * new instance is to delegate to in order to contribute to the mix
         */
        public MediaStreamMediaDeviceSession(
                AudioMixerMediaDeviceSession audioMixerMediaDeviceSession)
        {
            super(audioMixerMediaDeviceSession.getDevice());

            this.audioMixerMediaDeviceSession = audioMixerMediaDeviceSession;
            this.audioMixerMediaDeviceSession
                    .addMediaStreamMediaDeviceSession(this);
        }

        /**
         * Adds a <tt>ReceiveStream</tt> to this <tt>MediaDeviceSession</tt> to
         * be played back on the associated <tt>MediaDevice</tt> and a specific
         * <tt>DataSource</tt> is to be used to access its media data during the
         * playback.
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
            audioMixerMediaDeviceSession
                .addReceiveStream(receiveStream, receiveStreamDataSource);

            DataSource captureDevice = getCaptureDevice();

            /*
             * Unwrap wrappers of the captureDevice until
             * AudioMixingPushBufferDataSource is found.
             */
            if (captureDevice instanceof PushBufferDataSourceDelegate<?>)
                captureDevice
                    = ((PushBufferDataSourceDelegate<?>) captureDevice)
                        .getDataSource();
            if (captureDevice instanceof AudioMixingPushBufferDataSource)
                ((AudioMixingPushBufferDataSource) captureDevice)
                    .addInputDataSource(receiveStreamDataSource);
        }

        /**
         * Releases the resources allocated by this instance in the course of
         * its execution and prepares it to be garbage collected.
         *
         * @see MediaDeviceSession#close()
         */
        @Override
        public void close()
        {
            try
            {
                super.close();
            }
            finally
            {
                audioMixerMediaDeviceSession
                    .removeMediaStreamMediaDeviceSession(this);
            }
        }

        /**
         * Removes a <tt>ReceiveStream</tt> from this
         * <tt>MediaDeviceSession</tt> so that it no longer plays back on the
         * associated <tt>MediaDevice</tt>. Since this is the
         * <tt>MediaDeviceSession</tt> of a <tt>MediaStream</tt>, removes the
         * specified <tt>ReceiveStream</tt> from the mix.
         *
         * @param receiveStream the <tt>ReceiveStream</tt> to be removed from
         * this <tt>MediaDeviceSession</tt> and playback on the associated
         * <tt>MediaDevice</tt>
         * @see MediaDeviceSession#removeReceiveStream(ReceiveStream)
         */
        @Override
        public void removeReceiveStream(ReceiveStream receiveStream)
        {
            audioMixerMediaDeviceSession.removeReceiveStream(receiveStream);
        }
    }
}
