/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.audiolevel.*;
import net.java.sip.communicator.impl.neomedia.conference.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>MediaDevice</tt> which performs audio mixing using
 * {@link AudioMixer}.
 *
 * @author Lubomir Marinov
 * @author Emil Ivov
 */
public class AudioMixerMediaDevice
    extends AbstractMediaDevice
{
    /**
     * The <tt>Logger</tt> used by <tt>AudioMixerMediaDevice</tt> and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMixerMediaDevice.class);

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
     * The <tt>MediaDeviceSession</tt> of this <tt>AudioMixer</tt> with
     * {@link #device}.
     */
    private AudioMixerMediaDeviceSession deviceSession;

    /**
     * The dispatcher that delivers to listeners calculations of the local
     * audio level.
     */
    private AudioLevelEventDispatcher localAudioLevelEventDispatcher
        = new AudioLevelEventDispatcher();

    /**
     * The <tt>Map</tt> where we store audio level dispatchers and the
     * streams they are interested in.
     */
    private Map<ReceiveStream, AudioLevelEventDispatcher>
        streamAudioLevelListeners = new Hashtable<ReceiveStream,
                                              AudioLevelEventDispatcher>();

    /**
     * The <tt>List</tt> where we store all listeners interested in changes
     * of the local audio level.
     */
    private List<SimpleAudioLevelListener> localUserAudioLevelListeners
        = new ArrayList<SimpleAudioLevelListener>();

    /**
     * The listener that we'll register with the level dispatcher of the local
     * stream and that will notify all the listeners (if any) registered in
     * <tt>localUserAudioLevelListeners</tt>.
     */
    private SimpleAudioLevelListener localUserAudioLevelDelegator
        = new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    fireLocalUserAudioLevelException(level);
                }
            };

    /**
     * The <tt>List</tt> of RTP extensions supported by this device (at the time
     * of writing this list is only filled for audio devices and is
     * <tt>null</tt> otherwise).
     */
    private List<RTPExtension> rtpExtensions = null;

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
            throw
                new IllegalArgumentException("device must be able to capture");

        this.device = device;
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
        DataSource effectiveCaptureDevice = captureDevice;

        /*
         * Unwrap wrappers of the captureDevice until
         * AudioMixingPushBufferDataSource is found.
         */
        if (captureDevice instanceof PushBufferDataSourceDelegate<?>)
            captureDevice
                = ((PushBufferDataSourceDelegate<?>) captureDevice)
                    .getDataSource();

        /*
         * AudioMixingPushBufferDataSource is definitely not a CaptureDevice
         * and does not need the special connecting defined by
         * AbstractMediaDevice and MediaDeviceImpl.
         */
        if (captureDevice instanceof AudioMixingPushBufferDataSource)
            effectiveCaptureDevice.connect();
        else
            device.connect(effectiveCaptureDevice);
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
     * Returns a <tt>List</tt> containing (at the time of writing) a single
     * extension descriptor indicating <tt>SENDRECV</tt> for mixer-to-client
     * audio levels.
     *
     * @return a <tt>List</tt> containing the <tt>CSRC_AUDIO_LEVEL_URN</tt>
     * extension descriptor.
     */
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
                logger.info("Aha! Someone messed with the source!", e);
                return null;
            }

            rtpExtensions.add(new RTPExtension(
                               csrcAudioLevelURN, MediaDirection.SENDRECV));
        }

        return rtpExtensions;
    }

    /**
     * Gets the <tt>AudioMixer</tt> which performs audio mixing in this
     * <tt>MediaDevice</tt> (and rather the session it represents). If it still
     * does not exist, it is created.
     *
     * @return the <tt>AudioMixer</tt> which performs audio mixing in this
     * <tt>MediaDevice</tt> (and rather the session it represents)
     */
    private synchronized AudioMixer getAudioMixer()
    {
        if (audioMixer == null)
            audioMixer = new AudioMixer(device.createCaptureDevice())
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
                        AudioMixerMediaDevice.this.connect(dataSource);
                    else
                        super.connect(dataSource, inputDataSource);
                }

                @Override
                protected void read( PushBufferStream stream,
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
                        if(localAudioLevelEventDispatcher == null)
                        {
                            localAudioLevelEventDispatcher

                            synchronized (localAudioLevelListeners)
                            {
                                if(localAudioLevelEventDispatcher == null)
                            localAudioLevelEventDispatcher
                            }
                            new Thread(localAudioLevelEventDispatcher).start();
                        }
                        localAudioLevelEventDispatcher.addData(buffer);
                    }
                    else if (dataSource
                            instanceof ReceiveStreamPushBufferDataSource)
                    {
                        /*
                         * The audio of a ReceiveStream to be contributed to the
                         * mix.
                         */
                        ReceiveStream receiveStream
                            = ((ReceiveStreamPushBufferDataSource) dataSource)
                                .getReceiveStream();
                        AudioLevelEventDispatcher stEvDispatch;

                        synchronized (streamAudioLevelListeners)
                        {
                            stEvDispatch
                                = streamAudioLevelListeners.get(receiveStream);
                        }

                        if(stEvDispatch != null)
                            stEvDispatch.addData(buffer);
                    }
                }
            };
        return audioMixer;
    }

    /**
     * Notifies all currently registered <tt>SimpleAudioLevelListener</tt>s
     * that our local media now has audio level <tt>level</tt>.
     *
     * @param level the new audio level of the local media stream.
     */
    private void fireLocalUserAudioLevelException(int level)
    {
        synchronized(this.localUserAudioLevelListeners)
        {
            //these events are going to happen veeery often (~50 times per sec)
            //and we'd like to avoid creating an iterator every time
            for(int i = 0; i < localUserAudioLevelListeners.size(); i++)
            {
                localUserAudioLevelListeners.get(i)
                    .audioLevelChanged(level);
            }
        }
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
         * Adds <tt>l</tt> to the list of listeners that are being notified of
         * new local audio levels as they change. If <tt>l</tt> is added
         * multiple times it would only be registered once.
         *
         * @param l the listener we'd like to add.
         */
        public void addLocalUserAudioLevelListener(SimpleAudioLevelListener l)
        {
            synchronized(localUserAudioLevelListeners)
            {
                if(! localUserAudioLevelListeners.contains(l))
                    localUserAudioLevelListeners.add(l);
            }
        }

        /**
         * Removes <tt>l</tt> from the list of listeners that are being
         * notified of local audio levels.If <tt>l</tt> is not in the list,
         * the method has no effect.
         *
         * @param l the listener we'd like to remove.
         */
        public void removeLocalUserAudioLevelListener(
                                                SimpleAudioLevelListener l)
        {
            synchronized(localUserAudioLevelListeners)
            {
                localUserAudioLevelListeners.remove(l);
            }
        }

        /**
         * Sets <tt>l</tt> as the list of listeners that will receive
         * notifications of audio level event changes in the data arriving from
         * <tt>stream</tt>.
         *
         * @param stream the stream that <tt>l</tt> would like to register as
         * an audio level listener for.
         * @param l the listener we'd like to register for notifications from
         * <tt>stream</tt>.
         */
        public void putStreamAudioLevelListener(ReceiveStream            stream,
                                                SimpleAudioLevelListener l)
        {
            synchronized(streamAudioLevelListeners)
            {
                AudioLevelEventDispatcher dispatcher
                    = streamAudioLevelListeners.get(stream);

                if ( dispatcher == null )
                {
                    //this is not a replacement but a registration for a stream
                    //that was not listened to so far. create it and "put" it
                    dispatcher = new AudioLevelEventDispatcher();
                    streamAudioLevelListeners.put(stream, dispatcher);
                }

                dispatcher.setAudioLevelListener(l);
            }
        }

        /**
         * Removes listeners registered for audio level changes with the
         * specified receive  <tt>stream</tt>.
         *
         * @param stream the stream whose listeners we'd like to get rid of.
         */
        public void removeStreamAudioLevelListener(ReceiveStream stream)
        {
            synchronized(streamAudioLevelListeners)
            {
                AudioLevelEventDispatcher dispatcher =
                    streamAudioLevelListeners.remove(stream);

                dispatcher.setAudioLevelListener(null);
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
        protected void addReceiveStream( ReceiveStream receiveStream,
                                         DataSource receiveStreamDataSource)
        {
            DataSource captureDevice = getCaptureDevice();
            DataSource receiveStreamDataSourceForPlayback;

            if (captureDevice instanceof AudioMixingPushBufferDataSource)
                receiveStreamDataSourceForPlayback
                    = (AudioMixingPushBufferDataSource) captureDevice;
            else
                receiveStreamDataSourceForPlayback = receiveStreamDataSource;

            super.addReceiveStream( receiveStream,
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
        extends AudioMediaDeviceSession
        implements PropertyChangeListener
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
         * We use this field to keep a reference to the listener that we've
         * registered with the audio mixer for local audio level notifications.
         * We use this reference so that we could unregister it if someone
         * resets it or sets it to <tt>null</tt>.
         */
        private SimpleAudioLevelListener localAudioLevelListener = null;

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

            this.audioMixerMediaDeviceSession.addPropertyChangeListener(this);
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
        protected void addReceiveStream( ReceiveStream receiveStream,
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

        /**
         * Adds a specific <tt>SoundLevelListener</tt> to the list of
         * listeners interested in and notified about changes in local sound level
         * related information.
         * @param l the <tt>SoundLevelListener</tt> to add
         */
        @Override
        public void setLocalUserAudioLevelListener(SimpleAudioLevelListener l)
        {
            if (localAudioLevelListener != null)
            {
                    audioMixerMediaDeviceSession
                        .removeLocalUserAudioLevelListener(
                                        localAudioLevelListener);
                    localAudioLevelListener = null;
            }

            if( l != null)
            {
                this.localAudioLevelListener = l;
                audioMixerMediaDeviceSession.addLocalUserAudioLevelListener(l);
            }
        }

        /**
         * Returns the list of SSRC identifiers that are directly contributing
         * to the media flows that we are sending out. Note that since this is
         * a pseudo device we would simply be delegating the call to the
         * corresponding method of the master mixer device session.
         *
         * @return a <tt>long[]</tt> array of SSRC identifiers that are
         * currently contributing to the mixer encapsulated by this device
         * session.
         */
        @Override
        public long[] getRemoteSSRCList()
        {
            return audioMixerMediaDeviceSession.getRemoteSSRCList();
        }

        /**
         * Adds <tt>listener</tt> to the list of
         * <tt>SimpleAudioLevelListener</tt>s registered with the mixer session
         * that this "slave session" encapsulates. This class does not keep a
         * reference to <tt>listener</tt>.
         *
         * @param listener the <tt>SimpleAudioLevelListener</tt> that we are to
         * pass to the mixer device session or <tt>null</tt> if we are trying
         * to unregister it.
         */
        @Override
        public void setStreamAudioLevelListener(
                                            SimpleAudioLevelListener listener)
        {
            if( listener != null)
                audioMixerMediaDeviceSession.putStreamAudioLevelListener(
                        getReceiveStream(), listener);
            else
                audioMixerMediaDeviceSession
                    .removeStreamAudioLevelListener(getReceiveStream());
        }

        /**
         * The method relays <tt>PropertyChangeEvent</tt>s indicating a change
         * in the SSRC_LIST in the encapsulated mixer device so that the
         * <tt>MediaStream</tt> that uses this device session can update its
         * CSRC list.
         *
         * @param evt that <tt>PropertyChangeEvent</tt> whose old and new value
         * we will be relaying to the stream.
         */
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (MediaDeviceSession.SSRC_LIST.equals(evt.getPropertyName()))
            {
                firePropertyChange(MediaDeviceSession.SSRC_LIST,
                                evt.getOldValue(), evt.getNewValue());
            }
        }

    }
}
