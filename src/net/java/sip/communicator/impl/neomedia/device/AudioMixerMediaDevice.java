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
import java.beans.*;

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
 * @author Lyubomir Marinov
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
     * The listener that we'll register with the level dispatcher of the local
     * stream and that will notify all the listeners (if any) registered in
     * <tt>localUserAudioLevelListeners</tt>.
     */
    private final SimpleAudioLevelListener localUserAudioLevelDelegator
        = new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    lastMeasuredLocalUserAudioLevel = level;
                    fireLocalUserAudioLevelException(level);
                }
            };

    /**
     * The dispatcher that delivers to listeners calculations of the local
     * audio level.
     */
    private final AudioLevelEventDispatcher localUserAudioLevelDispatcher
        = new AudioLevelEventDispatcher();

    /**
     * The <tt>List</tt> where we store all listeners interested in changes of
     * the local audio level and the number of times each one of them has been
     * added. We wrap listeners because we may have multiple subscriptions with
     * the same listener and we would only store it once. If one of the multiple
     * subscriptions of a particular listener is removed, however, we wouldn't
     * want to reset the listener to <tt>null</tt> as there are others still
     * interested, and hence the <tt>referenceCount</tt> in the wrapper.
     * <p>
     * <b>Note</b>: <tt>localUserAudioLevelListeners</tt> is a copy-on-write
     * storage and access to it is synchronized by
     * {@link #localUserAudioLevelListenersSyncRoot}.
     * </p>
     */
    private List<SimpleAudioLevelListenerWrapper>
        localUserAudioLevelListeners
            = new ArrayList<SimpleAudioLevelListenerWrapper>();

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #localUserAudioLevelListeners}.
     */
    private final Object localUserAudioLevelListenersSyncRoot = new Object();

    /**
     * The levels map that we use to cache last measured audio levels for all
     * streams associated with this mixer.
     */
    private final AudioLevelMap audioLevelCache = new AudioLevelMap();

    /**
     * The most recently measured level of the locally captured audio stream.
     */
    private int lastMeasuredLocalUserAudioLevel = 0;

    /**
     * The <tt>List</tt> of RTP extensions supported by this device (at the time
     * of writing this list is only filled for audio devices and is
     * <tt>null</tt> otherwise).
     */
    private List<RTPExtension> rtpExtensions = null;

    /**
     * The <tt>Map</tt> where we store audio level dispatchers and the
     * streams they are interested in.
     */
    private final Map<ReceiveStream, AudioLevelEventDispatcher>
        streamAudioLevelListeners
            = new HashMap<ReceiveStream, AudioLevelEventDispatcher>();

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
    public AudioMixingPushBufferDataSource createOutputDataSource()
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
     * Notifies all currently registered <tt>SimpleAudioLevelListener</tt>s
     * that our local media now has audio level <tt>level</tt>.
     *
     * @param level the new audio level of the local media stream.
     */
    private void fireLocalUserAudioLevelException(int level)
    {
        List<SimpleAudioLevelListenerWrapper> localUserAudioLevelListeners;

        synchronized(localUserAudioLevelListenersSyncRoot)
        {
            localUserAudioLevelListeners = this.localUserAudioLevelListeners;
        }

        {
            /*
             * XXX These events are going to happen veeery often (~50 times per
             * sec) and we'd like to avoid creating an iterator every time.
             */
            int localUserAudioLevelListenerCount
                = localUserAudioLevelListeners.size();

            for(int i = 0; i < localUserAudioLevelListenerCount; i++)
                localUserAudioLevelListeners.get(i).listener.audioLevelChanged(
                        level);
        }
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
                        synchronized(localUserAudioLevelListenersSyncRoot)
                        {
                            if (localUserAudioLevelListeners.isEmpty())
                                return;
                        }

                        localUserAudioLevelDispatcher.addData(buffer);

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
                        AudioLevelEventDispatcher streamEventDispatcher;

                        synchronized (streamAudioLevelListeners)
                        {
                            streamEventDispatcher
                                = streamAudioLevelListeners.get(receiveStream);
                        }

                        if(streamEventDispatcher != null
                            && !buffer.isDiscard()
                            && buffer.getLength() > 0
                            && buffer.getData() != null)
                        {
                            if(! streamEventDispatcher.isRunning())
                                new Thread(streamEventDispatcher,
                                   "StreamAudioLevelDispatcher (Mixer Edition)")
                                        .start();
                            streamEventDispatcher.addData(buffer);
                        }
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
     * Returns a <tt>List</tt> containing (at the time of writing) a single
     * extension descriptor indicating <tt>SENDRECV</tt> for mixer-to-client
     * audio levels.
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
                               csrcAudioLevelURN, MediaDirection.SENDRECV));
        }

        return rtpExtensions;
    }

    /**
     * Gets a list of <tt>MediaFormat</tt>s supported by this
     * <tt>MediaDevice</tt>. Currently does nothing.
     * @param preset does nothing for audio.
     * @return the list of <tt>MediaFormat</tt>s supported by this device
     * @see MediaDevice#getSupportedFormats()
     */
    public List<MediaFormat> getSupportedFormats(QualityPreset preset)
    {
        return device.getSupportedFormats();
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
        return this.getSupportedFormats(null);
    }

    /**
     * Removes the <tt>DataSource</tt> accepted by a specific
     * <tt>DataSourceFilter</tt> from the list of input <tt>DataSource</tt> of
     * the <tt>AudioMixer</tt> of this <tt>AudioMixerMediaDevice</tt> from
     * which it reads audio to be mixed.
     *
     * @param dataSourceFilter the <tt>DataSourceFilter</tt> which selects the
     * <tt>DataSource</tt>s to be removed
     */
    void removeInputDataSources(DataSourceFilter dataSourceFilter)
    {
        AudioMixer audioMixer = this.audioMixer;

        if (audioMixer != null)
            audioMixer.removeInputDataSources(dataSourceFilter);
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
         * Adds <tt>l</tt> to the list of listeners that are being notified of
         * new local audio levels as they change. If <tt>l</tt> is added
         * multiple times it would only be registered once.
         *
         * @param l the listener we'd like to add.
         */
        public void addLocalUserAudioLevelListener(SimpleAudioLevelListener l)
        {
            // If the listener is null, we have nothing more to do here.
            if (l == null)
                return;

            synchronized(localUserAudioLevelListenersSyncRoot)
            {
                //if this is the first listener that we are seeing then we also
                //need to create the dispatcher.
                if (localUserAudioLevelListeners.isEmpty())
                {
                    localUserAudioLevelDispatcher
                        .setAudioLevelListener(localUserAudioLevelDelegator);

                    new Thread(localUserAudioLevelDispatcher,
                          "Local User Audio Level Dispatcher (Mixer Edition)")
                                .start();
                }

                //check if this listener has already been added.
                SimpleAudioLevelListenerWrapper wrapper
                    = new SimpleAudioLevelListenerWrapper(l);
                int index = localUserAudioLevelListeners.indexOf(wrapper);

                if( index != -1)
                {
                    wrapper = localUserAudioLevelListeners.get(index);
                    wrapper.referenceCount++;
                }
                else
                {
                    /*
                     * XXX localUserAudioLevelListeners must be a copy-on-write
                     * storage so that firing events to its
                     * SimpleAudioLevelListeners can happen outside a block
                     * synchronized by localUserAudioLevelListenersSyncRoot and
                     * thus reduce the chances for a deadlock (which was,
                     * otherwise, observed in practice).
                     */
                    localUserAudioLevelListeners
                        = new ArrayList<SimpleAudioLevelListenerWrapper>(
                                localUserAudioLevelListeners);
                    localUserAudioLevelListeners.add(wrapper);
                }
            }
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
         * Adds a specific <tt>DataSource</tt> providing remote audio to the mix
         * produced by the associated <tt>MediaDevice</tt>.
         *
         * @param playbackDataSource the <tt>DataSource</tt> providing remote
         * audio to be added to the mix produced by the associated
         * <tt>MediaDevice</tt>
         */
        void addPlaybackDataSource(DataSource playbackDataSource)
        {
            /*
             * We don't play back the contributions of the conference members
             * separately, we have a single playback of the mix of all
             * contributions but ours.
             */
            setPlaybackDataSource(
                (AudioMixingPushBufferDataSource) getCaptureDevice());
        }

        /**
         * Adds a specific <tt>ReceiveStream</tt> to the list of
         * <tt>ReceiveStream</tt>s known to this instance to be contributing
         * audio to the mix produced by its associated <tt>AudioMixer</tt>.
         *
         * @param receiveStream the <tt>ReceiveStream</tt> to be added to the
         * list of <tt>ReceiveStream</tt>s known to this instance to be
         * contributing audio to the mix produced by its associated
         * <tt>AudioMixer</tt>
         */
        void addReceiveStream(ReceiveStream receiveStream)
        {
            addSSRC(receiveStream.getSSRC());
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
         * Sets <tt>l</tt> as the list of listeners that will receive
         * notifications of audio level event changes in the data arriving from
         * <tt>stream</tt>.
         *
         * @param stream the stream that <tt>l</tt> would like to register as
         * an audio level listener for.
         * @param listener the listener we'd like to register for notifications
         * from <tt>stream</tt>.
         */
        public void putStreamAudioLevelListener(
                ReceiveStream stream,
                SimpleAudioLevelListener listener)
        {
            synchronized(streamAudioLevelListeners)
            {
                AudioLevelEventDispatcher dispatcher
                    = streamAudioLevelListeners.get(stream);

                if (dispatcher == null)
                {
                    //this is not a replacement but a registration for a stream
                    //that was not listened to so far. create it and "put" it
                    dispatcher = new AudioLevelEventDispatcher();
                    dispatcher.setMapCache(audioLevelCache, stream.getSSRC());
                    streamAudioLevelListeners.put(stream, dispatcher);
                }

                dispatcher.setAudioLevelListener(listener);
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
            synchronized(localUserAudioLevelListenersSyncRoot)
            {
                //check if this listener has already been added.
                int index
                    = localUserAudioLevelListeners.indexOf(
                            new SimpleAudioLevelListenerWrapper(l));

                if( index != -1)
                {
                    SimpleAudioLevelListenerWrapper wrapper
                        = localUserAudioLevelListeners.get(index);

                    if(wrapper.referenceCount > 1)
                        wrapper.referenceCount--;
                    else
                    {
                        /*
                         * XXX localUserAudioLevelListeners must be a
                         * copy-on-write storage so that firing events to its
                         * SimpleAudioLevelListeners can happen outside a block
                         * synchronized by localUserAudioLevelListenersSyncRoot
                         * and thus reduce the chances for a deadlock (whic
                         * was, otherwise, observed in practice).
                         */
                        localUserAudioLevelListeners
                            = new ArrayList<SimpleAudioLevelListenerWrapper>(
                                    localUserAudioLevelListeners);
                        localUserAudioLevelListeners.remove(wrapper);
                    }
                }

                //if this was the last listener then we also need to remove the
                //dispatcher
                if (localUserAudioLevelListeners.isEmpty())
                {
                    localUserAudioLevelDispatcher.stop();
                    localUserAudioLevelDispatcher.setAudioLevelListener(null);
                }
            }
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
            {
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
         * Removes a specific <tt>DataSource</tt> providing remote audio from
         * the mix produced by the associated <tt>AudioMixer</tt>.
         *
         * @param playbackDataSource the <tt>DataSource</tt> providing remote
         * audio to be removed from the mix produced by the associated
         * <tt>AudioMixer</tt>
         */
        void removePlaybackDataSource(final DataSource playbackDataSource)
        {
            removeInputDataSources(new DataSourceFilter()
            {
                public boolean accept(DataSource dataSource)
                {
                    return dataSource.equals(playbackDataSource);
                }
            });
        }

        /**
         * Removes a specific <tt>ReceiveStream</tt> from the list of
         * <tt>ReceiveStream</tt>s known to this instance to be contributing
         * audio to the mix produced by its associated <tt>AudioMixer</tt>.
         *
         * @param receiveStream the <tt>ReceiveStream</tt> to be removed from
         * the list of <tt>ReceiveStream</tt>s known to this instance to be
         * contributing audio to the mix produced by its associated
         * <tt>AudioMixer</tt>
         */
        void removeReceiveStream(ReceiveStream receiveStream)
        {
            removeSSRC(receiveStream.getSSRC());

            //make sure we no longer cache levels for that stream.
            audioLevelCache.removeLevel(receiveStream.getSSRC());
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

                if(dispatcher != null)
                {
                    dispatcher.setAudioLevelListener(null);
                }
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
        private SimpleAudioLevelListener localUserAudioLevelListener = null;

        /**
         * We use this field to keep a reference to the listener that we've
         * registered with the audio mixer for stream audio level notifications.
         * We use this reference so because at the time we get it from the
         * <tt>MediaStream</tt> it might be too early to register it with the
         * mixer as it is like that we don't have a receive stream yet. If
         * that's the case, we hold on to the listener and register it only
         * when we get the <tt>ReceiveStream</tt>.
         */
        private SimpleAudioLevelListener streamAudioLevelListener = null;

        /**
         * The <tt>Object</tt> that we use to lock operations on
         * <tt>streamAudioLevelListener</tt>.
         */
        private final Object streamAudioLevelListenerLock = new Object();

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
         * Creates a new <tt>Player</tt> to render the
         * <tt>playbackDataSource</tt> of this instance on the associated
         * <tt>MediaDevice</tt>.
         *
         * @return a new <tt>Player</tt> to render the
         * <tt>playbackDataSource</tt> of this instance on the associated
         * <tt>MediaDevice</tt> or <tt>null</tt> if the
         * <tt>playbackDataSource</tt> of this instance is not to be rendered
         * @see MediaDeviceSession#createPlayer()
         */
        @Override
        protected Player createPlayer()
        {
            /*
             * We don't want the contribution of each conference member played
             * back separately, we want the one and only mix of all
             * contributions but ours to be played back once for all of them.
             */
            return null;
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
         * Notifies this instance that the value of its
         * <tt>playbackDataSource</tt> property has changed from a specific
         * <tt>oldValue</tt> to a specific <tt>newValue</tt>.
         *
         * @param oldValue the <tt>DataSource</tt> which used to be the value of
         * the <tt>playbackDataSource</tt> property of this instance
         * @param newValue the <tt>DataSource</tt> which is the value of the
         * <tt>playbackDataSource</tt> property of this instance
         * @see MediaDeviceSession#playbackDataSourceChanged(DataSource,
         * DataSource)
         */
        @Override
        protected void playbackDataSourceChanged(
                DataSource oldValue,
                DataSource newValue)
        {
            super.playbackDataSourceChanged(oldValue, newValue);

            if (oldValue != null)
                audioMixerMediaDeviceSession.removePlaybackDataSource(oldValue);
            if (newValue != null)
            {
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
                        .addInputDataSource(newValue);

                audioMixerMediaDeviceSession.addPlaybackDataSource(newValue);
            }
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
                firePropertyChange(
                    MediaDeviceSession.SSRC_LIST,
                    evt.getOldValue(),
                    evt.getNewValue());
        }

        /**
         * Notifies this instance that the value of its <tt>receiveStream</tt>
         * property has changed from a specific <tt>oldValue</tt> to a specific
         * <tt>newValue</tt>.
         *
         * @param oldValue the <tt>ReceiveStream</tt> which used to be the value
         * of the <tt>receiveStream</tt> property of this instance
         * @param newValue the <tt>ReceiveStream</tt> which is the value of the
         * <tt>receiveStream</tt> property of this instance
         * @see MediaDeviceSession#receiveStreamChanged(ReceiveStream,
         * ReceiveStream)
         */
        @Override
        protected void receiveStreamChanged(
                ReceiveStream oldValue,
                ReceiveStream newValue)
        {
            super.receiveStreamChanged(oldValue, newValue);

            if (oldValue != null)
                audioMixerMediaDeviceSession.removeReceiveStream(oldValue);
            if (newValue != null)
            {
                /*
                 * If someone registered a stream level listener, we can now add
                 * it since we have the stream that it's supposed to listen to.
                 */
                synchronized(streamAudioLevelListenerLock)
                {
                    if(this.streamAudioLevelListener != null)
                        audioMixerMediaDeviceSession
                            .putStreamAudioLevelListener(
                                newValue,
                                streamAudioLevelListener);
                }

                audioMixerMediaDeviceSession.addReceiveStream(newValue);
            }
        }

        /**
         * Override it here cause we won't register effects to that stream
         * cause we already have one.
         *
         * @param processor the processor.
         */
        @Override
        protected void registerLocalUserAudioLevelEffect(Processor processor)
        {
        }

        /**
         * Adds a specific <tt>SoundLevelListener</tt> to the list of listeners
         * interested in and notified about changes in local sound level related
         * information.
         * @param l the <tt>SoundLevelListener</tt> to add
         */
        @Override
        public void setLocalUserAudioLevelListener(SimpleAudioLevelListener l)
        {
            if (localUserAudioLevelListener != null)
            {
                    audioMixerMediaDeviceSession
                        .removeLocalUserAudioLevelListener(
                                        localUserAudioLevelListener);
                    localUserAudioLevelListener = null;
            }

            if( l != null)
            {
                this.localUserAudioLevelListener = l;

                // add the listener only if we are not muted
                // this happens when holding a conversation, stream is muted
                // and when recreated listener is again set
                if(!isMute())
                    audioMixerMediaDeviceSession
                            .addLocalUserAudioLevelListener(l);
            }
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
            synchronized(streamAudioLevelListenerLock)
            {
                this.streamAudioLevelListener = listener;

                ReceiveStream receiveStream = getReceiveStream();

                if( listener != null)
                {
                    //if we already have a ReceiveStream - register the listener
                    //with the mixer. Otherwise - wait till we get one.
                    if( receiveStream != null)
                        audioMixerMediaDeviceSession
                            .putStreamAudioLevelListener(
                                receiveStream, listener);
                }
                else if( receiveStream != null)
                    audioMixerMediaDeviceSession
                        .removeStreamAudioLevelListener(receiveStream);
            }
        }

        /**
         * Returns the last audio level that was measured by the underlying
         * mixer for the specified <tt>csrc</tt>.
         *
         * @param csrc the CSRC ID whose last measured audio level we'd like to
         * retrieve.
         *
         * @return the audio level that was last measured by the underlying
         * mixer for the specified <tt>csrc</tt> or <tt>-1</tt> if the
         * <tt>csrc</tt> does not belong to neither of the conference
         * participants.
         */
        @Override
        public int getLastMeasuredAudioLevel(long csrc)
        {
            return
                ((AudioMixerMediaDevice) getDevice()).audioLevelCache.getLevel(
                        csrc);
        }

        /**
         * Returns the last audio level that was measured by the underlying
         * mixer for local user.
         *
         * @return the audio level that was last measured for the local user.
         */
        @Override
        public int getLastMeasuredLocalUserAudioLevel()
        {
            return
                ((AudioMixerMediaDevice) getDevice())
                    .lastMeasuredLocalUserAudioLevel;
        }

        /**
         * Sets the indicator which determines whether this
         * <tt>MediaDeviceSession</tt> is set to output "silence" instead of the
         * actual media fed from its <tt>CaptureDevice</tt>.
         * If we are muted we just remove the local level listener from the
         * session.
         *
         * @param mute <tt>true</tt> to set this <tt>MediaDeviceSession</tt> to
         *             output "silence" instead of the actual media fed from its
         *             <tt>CaptureDevice</tt>; otherwise, <tt>false</tt>
         */
        @Override
        public void setMute(boolean mute)
        {
            if(super.isMute() == mute)
                return;

            super.setMute(mute);

            if(mute)
            {
                audioMixerMediaDeviceSession.removeLocalUserAudioLevelListener(
                        this.localUserAudioLevelListener);
            }
            else
            {
                audioMixerMediaDeviceSession.addLocalUserAudioLevelListener(
                        this.localUserAudioLevelListener);
            }
        }
    }

    /**
     * A very lightweight wrapper that allows us to track the number of times
     * that a particular listener was added.
     */
    private static class SimpleAudioLevelListenerWrapper
    {
        /** The listener being wrapped by this wrapper. */
        public final SimpleAudioLevelListener listener;

        /** The number of times this listener has been added. */
        int referenceCount;

        /**
         * Creates a wrapper of the <tt>l</tt> listener.
         *
         * @param l the listener we'd like to wrap;
         */
        public SimpleAudioLevelListenerWrapper(SimpleAudioLevelListener l)
        {
            this.listener = l;
            this.referenceCount = 1;
        }

        /**
         * Returns <tt>true</tt> if <tt>obj</tt> is a wrapping the same listener
         * as ours.
         *
         * @param obj the wrapper we'd like to compare to this instance
         *
         * @return <tt>true</tt> if <tt>obj</tt> is a wrapping the same listener
         * as ours.
         */
        @Override
        public boolean equals(Object obj)
        {
            return (obj instanceof SimpleAudioLevelListenerWrapper)
                && ((SimpleAudioLevelListenerWrapper)obj).listener == listener;
        }

        /**
         * Returns a hash code value for this instance for the benefit of
         * hashtables.
         *
         * @return a hash code value for this instance for the benefit of
         * hashtables
         */
        @Override
        public int hashCode()
        {
            /*
             * Equality is based on the listener field only so its hashCode is
             * enough. Besides, it's the only immutable of this instance i.e.
             * the only field appropriate for the calculation of the hashCode.
             */
            return listener.hashCode();
        }
    }
}
