/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.Dimension;
import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.audio.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Represents the use of a specific <tt>MediaDevice</tt> by a
 * <tt>MediaStream</tt>.
 *
 * @author Lubomir Marinov
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class MediaDeviceSession
    extends PropertyChangeNotifier
{

    /**
     * The <tt>Logger</tt> used by the <tt>MediaDeviceSession</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaDeviceSession.class);

    /**
     * The name of the <tt>MediaDeviceSession</tt> instance property the value
     * of which represents the output <tt>DataSource</tt> of the
     * <tt>MediaDeviceSession</tt> instance which provides the captured (RTP)
     * data to be sent by <tt>MediaStream</tt> to <tt>MediaStreamTarget</tt>.
     */
    public static final String OUTPUT_DATA_SOURCE = "OUTPUT_DATA_SOURCE";

    /**
     * The name of the property that corresponds to the array of SSRC
     * identifiers that we store in this <tt>MediaDeviceSession</tt> instance
     * and that we update upon adding and removing <tt>ReceiveStream</tt>
     */
    public static final String SSRC_LIST = "SSRC_LIST";

    /**
     * The JMF <tt>DataSource</tt> of {@link #device} through which this
     * instance accesses the media captured by it.
     */
    private DataSource captureDevice;

    /**
     * The indicator which determines whether {@link DataSource#connect()} has
     * been successfully executed on {@link #captureDevice}.
     */
    private boolean captureDeviceIsConnected;

    /**
     * The <tt>MediaDevice</tt> used by this instance to capture and play back
     * media.
     */
    private final AbstractMediaDevice device;

    /**
     * The last JMF <tt>Format</tt> set to this instance by a call to its
     * {@link #setFormat(MediaFormat) and to be set as the output format of
     * {@link #processor}.
     */
    private Format format;

    /**
     * The indicator which determines whether this <tt>MediaDeviceSession</tt>
     * is set to output "silence" instead of the actual media captured from
     * {@link #captureDevice}.
     */
    private boolean mute = false;

    /**
     * The <tt>ControllerListener</tt> which listens to the <tt>Player</tt>
     * instances in {@link #players} for <tt>ControllerEvent</tt>s.
     */
    private ControllerListener playerControllerListener;

    /**
     * The <tt>Processor</tt>s rendering <tt>ReceiveStream</tt>s on the
     * <tt>MediaDevice</tt> represented by this instance. Associated with
     * <tt>DataSource</tt> because different <tt>ReceiveStream</tt>s may be
     * added with one and the same <tt>DataSource</tt> so it has to be clear
     * when a new <tt>Processor</tt> is to be created and when it is to be
     * disposed. The <tt>Processor</tt> is used as a Player.
     */
    private final Map<DataSource, Processor> players
        = new HashMap<DataSource, Processor>();

    /**
     * The JMF <tt>Processor</tt> which transcodes {@link #captureDevice} into
     * the format of this instance.
     */
    private Processor processor;

    /**
     * The <tt>ControllerListener</tt> which listens to {@link #processor} for
     * <tt>ControllerEvent</tt>s.
     */
    private ControllerListener processorControllerListener;

    /**
     * The indicator which determines whether {@link #processor} has received
     * a <tt>ControllerClosedEvent</tt> at an unexpected time in its execution.
     * A value of <tt>false</tt> does not mean that <tt>processor</tt> exists
     * or that it is not closed, it just means that if <tt>processor</tt> failed
     * to be initialized or it received a <tt>ControllerClosedEvent</tt>, it was
     * at an expected time of its execution and that the fact in question was
     * reflected, for example, by setting <tt>processor</tt> to <tt>null</tt>.
     * If there is no <tt>processorIsPrematurelyClosed</tt> field and
     * <tt>processor</tt> is set to <tt>null</tt> or left existing after the
     * receipt of <tt>ControllerClosedEvent</tt>, it will either lead to not
     * firing a <tt>PropertyChangeEvent</tt> for <tt>OUTPUT_DATA_SOURCE</tt>
     * when it has actually changed and, consequently, cause the
     * <tt>SendStream</tt>s of <tt>MediaStreamImpl</tt> to not be recreated or
     * it will be impossible to detect that <tt>processor</tt> cannot have its
     * format set and will thus be left broken even for subsequent calls to
     * {@link #setFormat(MediaFormat)}.
     */
    private boolean processorIsPrematurelyClosed;

    /**
     * The <tt>ReceiveStream</tt>s rendered by this instance on its associated
     * <tt>MediaDevice</tt>. Mapped to <tt>DataSource</tt> because extenders may
     * choose to override their <tt>DataSource</tt> and use one and the same
     * instance for different <tt>ReceiveStream</tt>s (e.g. audio mixing).
     */
    private final Map<ReceiveStream, DataSource> receiveStreams
        = new HashMap<ReceiveStream, DataSource>();

    /**
     * The list of SSRC identifiers representing the parties that we are
     * currently handling receive streams from.
     */
    private long[] ssrcList = null;

    /**
     * The <tt>MediaDirection</tt> in which this <tt>MediaDeviceSession</tt> has
     * been started.
     */
    private MediaDirection startedDirection = MediaDirection.INACTIVE;

    /**
     * The <tt>MediaStream</tt> that creates us.
     */
    private MediaStream parentStream = null;

    /**
     * A list of listeners registered for local user sound level events.
     */
    private final List<SoundLevelListener> localSoundLevelListeners
        = new Vector<SoundLevelListener>();

    /**
     * A list of listeners registered for stream user sound level events.
     */
    private final List<SoundLevelListener> streamSoundLevelListeners
        = new Vector<SoundLevelListener>();

    /**
     * Initializes a new <tt>MediaDeviceSession</tt> instance which is to
     * represent the use of a specific <tt>MediaDevice</tt> by a
     * <tt>MediaStream</tt>.
     *
     * @param device the <tt>MediaDevice</tt> the use of which by a
     * <tt>MediaStream</tt> is to be represented by the new instance
     */
    protected MediaDeviceSession(AbstractMediaDevice device)
    {
        checkDevice(device);

        this.device = device;
    }

    /**
     * Adds a <tt>ReceiveStream</tt> to this <tt>MediaDeviceSession</tt> to be
     * played back on the associated <tt>MediaDevice</tt>.
     *
     * @param receiveStream the <tt>ReceiveStream</tt> to be played back by this
     * <tt>MediaDeviceSession</tt> on its associated <tt>MediaDevice</tt>
     */
    public void addReceiveStream(ReceiveStream receiveStream)
    {
        DataSource receiveStreamDataSource = receiveStream.getDataSource();

        if (receiveStreamDataSource != null)
        {
            if (receiveStreamDataSource instanceof PushBufferDataSource)
                receiveStreamDataSource
                    = new ReceiveStreamPushBufferDataSource(
                            receiveStream,
                            (PushBufferDataSource) receiveStreamDataSource,
                            true);
            else
                logger
                    .warn(
                        "Adding ReceiveStream with DataSource"
                            + " not of type PushBufferDataSource but "
                            + receiveStreamDataSource.getClass().getSimpleName()
                            + " which may prevent the ReceiveStream from"
                            + " properly transferring to another MediaDevice"
                            + " if such a need arises.");

            addReceiveStream(receiveStream, receiveStreamDataSource);
        }
    }

    /**
     * Adds a <tt>ReceiveStream</tt> to this <tt>MediaDeviceSession</tt> to be
     * played back on the associated <tt>MediaDevice</tt> and a specific
     * <tt>DataSource</tt> is to be used to access its media data during the
     * playback. The <tt>DataSource</tt> is explicitly specified in order to
     * allow extenders to override the <tt>DataSource</tt> of the
     * <tt>ReceiveStream</tt> (e.g. create a clone of it).
     *
     * @param receiveStream the <tt>ReceiveStream</tt> to be played back by this
     * <tt>MediaDeviceSession</tt> on its associated <tt>MediaDevice</tt>
     * @param receiveStreamDataSource the <tt>DataSource</tt> to be used for
     * accessing the media data of <tt>receiveStream</tt> during its playback
     */
    protected synchronized void addReceiveStream(
            ReceiveStream receiveStream,
            DataSource receiveStreamDataSource)
    {

        /*
         * Though we check for null in #addReceiveStream(ReceiveStream), we have
         * to check again because we may have been overridden.
         */
        if (receiveStreamDataSource == null)
            return;

        receiveStreams.put(receiveStream, receiveStreamDataSource);
        if (logger.isTraceEnabled())
            logger.trace(
                    "Added ReceiveStream with ssrc " + receiveStream.getSSRC());

        addSSRC(receiveStream.getSSRC());

        synchronized (players)
        {
            Processor player = players.get(receiveStreamDataSource);

            if (player == null)
            {
                Throwable exception = null;

                try
                {
                    player = Manager.createProcessor(receiveStreamDataSource);
                }
                catch (IOException ioe)
                {
                    exception = ioe;
                }
                catch (NoPlayerException npe)
                {
                    exception = npe;
                }

                if (exception != null)
                    logger.error(
                            "Failed to create player"
                                + " for ReceiveStream with ssrc "
                                + receiveStream.getSSRC(),
                            exception);
                else if (!waitForState(player, Processor.Configured))
                    logger.error(
                            "Failed to configure player"
                                + " for ReceiveStream with ssrc "
                                + receiveStream.getSSRC());
                else
                {
                    boolean status = waitForState(player, Processor.Configured);

                    if(status){
                    //here we add sound level indicator for every incoming
                    //stream
                    try
                    {
                        TrackControl tcs[] = player.getTrackControls();
                        if (tcs != null)
                        {
                            for (TrackControl tc : tcs)
                            {
                                if (tc.getFormat() instanceof AudioFormat)
                                {
                                    // Assume there is only one audio track
                                    addSoundLevelIndicator(parentStream,tc);
                                    break;
                                }
                            }
                        }
                    }
                    catch (UnsupportedPlugInException ex)
                    {
                        logger.error("The processor does not support effects", ex);
                    }}

                    // to use the processor as player we must set its
                    // content descriptor to null
                    player.setContentDescriptor(null);

                    if (waitForState(player, Processor.Realized))
                    {
                        player.start();

                        realizeComplete(player);

                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Created Player with hashCode "
                                        + player.hashCode()
                                        + " for ReceiveStream with ssrc "
                                        + receiveStream.getSSRC());

                        players.put(receiveStreamDataSource, player);
                    }
                    else
                        logger
                            .error(
                                "Failed to realize player"
                                    + " for ReceiveStream with ssrc "
                                    + receiveStream.getSSRC());
                }
            }
        }
    }

    /**
     * Creates sound level indicator effect and add it to the codec chain of the
     * <tt>TrackControl</tt> and assumes there is only one audio track.
     *
     * @param tc the track control.
     * @throws UnsupportedPlugInException
     */
    private void addLocalSoundLevelIndicator(TrackControl tc)
            throws UnsupportedPlugInException
    {
        SoundLevelIndicatorEffect slie = new SoundLevelIndicatorEffect(
            SoundLevelChangeEvent.MIN_LEVEL,
            SoundLevelChangeEvent.MAX_LEVEL,
            new SoundLevelIndicatorEffect.SoundLevelIndicatorListener()
            {
                public void soundLevelChanged(int level)
                {
                    fireLocalUserSoundLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        tc.setCodecChain(new Codec[]{slie});
    }

    /**
     * Adds sound level indicator to the track and fire events to the listeners
     * from the <tt>MediaStream</tt>.
     * @param mediaStream the media stream
     * @param tc the TrackControl
     * @throws UnsupportedPlugInException
     */
    private void addSoundLevelIndicator(MediaStream mediaStream, TrackControl tc)
        throws UnsupportedPlugInException
    {
        if(mediaStream == null || !(mediaStream instanceof AudioMediaStreamImpl))
            return;

        final AudioMediaStreamImpl aStream = (AudioMediaStreamImpl)mediaStream;

        if(streamSoundLevelListeners.size() == 0)
            return;

        SoundLevelIndicatorEffect slie = new SoundLevelIndicatorEffect(
            SoundLevelChangeEvent.MIN_LEVEL,
            SoundLevelChangeEvent.MAX_LEVEL,
            new SoundLevelIndicatorEffect.SoundLevelIndicatorListener()
            {
                public void soundLevelChanged(int level)
                {
                    fireStreamSoundLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        tc.setCodecChain(new Codec[]{slie});
    }

    /**
     * For JPEG and H263, we know that they only work for particular sizes.  So
     * we'll perform extra checking here to make sure they are of the right
     * sizes.
     *
     * @param sourceFormat the original format to check the size of
     * @return the modified <tt>VideoFormat</tt> set to the size we support
     */
    private static VideoFormat assertSize(VideoFormat sourceFormat)
    {
        int width, height;

        // JPEG
        if (sourceFormat.matches(new Format(VideoFormat.JPEG_RTP)))
        {
            Dimension size = sourceFormat.getSize();

            // For JPEG, make sure width and height are divisible by 8.
            width
                = (size.width % 8 == 0)
                    ? size.width
                    : ((size.width / 8) * 8);
            height
                = (size.height % 8 == 0)
                    ? size.height
                    : ((size.height / 8) * 8);
        }
        // H.263
        else if (sourceFormat.matches(new Format(VideoFormat.H263_RTP)))
        {
            // For H.263, we only support some specific sizes.
//            if (size.width < 128)
//            {
//                width = 128;
//                height = 96;
//            }
//            else if (size.width < 176)
//            {
//                width = 176;
//                height = 144;
//            }
//            else
//            {
                width = 352;
                height = 288;
//            }
        }
        else
        {
            // We don't know this particular format.  We'll just leave it alone then.
            return sourceFormat;
        }

        VideoFormat result = new VideoFormat(null,
                                             new Dimension(width, height),
                                             Format.NOT_SPECIFIED,
                                             null,
                                             Format.NOT_SPECIFIED);
        return (VideoFormat) result.intersects(sourceFormat);
    }

    /**
     * Asserts that a specific <tt>MediaDevice</tt> is acceptable to be set as
     * the <tt>MediaDevice</tt> of this instance. Allows extenders to override
     * and customize the check.
     *
     * @param device the <tt>MediaDevice</tt> to be checked for suitability to
     * become the <tt>MediaDevice</tt> of this instance
     */
    protected void checkDevice(AbstractMediaDevice device)
    {
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     */
    public void close()
    {
        disposePlayers();

        disconnectCaptureDevice();
        closeProcessor();
    }

    /**
     * Makes sure {@link #processor} is closed.
     */
    private void closeProcessor()
    {
        if (processor != null)
        {
            if (processorControllerListener != null)
                processor.removeControllerListener(processorControllerListener);

            processor.stop();
            if (logger.isTraceEnabled())
                logger
                    .trace(
                        "Stopped Processor with hashCode "
                            + processor.hashCode());

            if (processor.getState() == Processor.Realized)
            {
                DataSource dataOutput = processor.getDataOutput();

                if (dataOutput != null)
                    dataOutput.disconnect();
            }
            processor.deallocate();
            processor.close();
            processorIsPrematurelyClosed = false;

            /*
             * Once the processor uses the captureDevice, the captureDevice has
             * to be reconnected on its next use.
             */
            disconnectCaptureDevice();
        }
    }

    /**
     * Creates the <tt>DataSource</tt> that this instance is to read captured
     * media from.
     *
     * @return the <tt>DataSource</tt> that this instance is to read captured
     * media from
     */
    protected DataSource createCaptureDevice()
    {
        DataSource captureDevice = getDevice().createOutputDataSource();

        // Try to enable muting.
        if (captureDevice instanceof PushBufferDataSource)
        {
            MutePushBufferDataSource mutePushBufferDataSource
                = new MutePushBufferDataSource(
                        (PushBufferDataSource) captureDevice);

            mutePushBufferDataSource.setMute(mute);
            captureDevice = mutePushBufferDataSource;
        }

        return captureDevice;
    }

    /**
     * Makes sure {@link #captureDevice} is disconnected.
     */
    private void disconnectCaptureDevice()
    {
        if (captureDevice != null)
        {
            /*
             * As reported by Carlos Alexandre, stopping before disconnecting
             * resolves a slow disconnect on Linux.
             */
            try
            {
                captureDevice.stop();
            }
            catch (IOException ioe)
            {
                /*
                 * We cannot do much about the exception because we're not
                 * really interested in the stopping but rather in calling
                 * DataSource#disconnect() anyway.
                 */
                logger
                    .error(
                        "Failed to properly stop captureDevice "
                            + captureDevice,
                        ioe);
            }

            captureDevice.disconnect();
            captureDeviceIsConnected = false;
        }
    }

    /**
     * Releases the resources allocated by a specific <tt>Processor</tt> in the
     * course of its execution and prepares it to be garbage collected.
     * The <tt>Processor</tt> is used as a <tt>Player</tt>.
     *
     * @param player the <tt>Processor</tt> to dispose of
     */
    protected void disposePlayer(Processor player)
    {
        synchronized (players)
        {
            Iterator<Map.Entry<DataSource, Processor>> playerIter
                = players.entrySet().iterator();

            while (playerIter.hasNext())
                if (playerIter.next().getValue().equals(player))
                {
                    playerIter.remove();
                    break;
                }

            if (playerControllerListener != null)
                player.removeControllerListener(playerControllerListener);
            player.stop();
            player.deallocate();
            player.close();
        }
    }

    /**
     * Releases the resources allocated by {@link #players} in the course of
     * their execution and prepares them to be garbage collected.
     */
    private void disposePlayers()
    {
        synchronized (players)
        {
            for (Processor player : getPlayers())
                disposePlayer(player);
        }
    }

    /**
     * Finds the first <tt>Format</tt> instance in a specific list of
     * <tt>Format</tt>s which matches a specific <tt>Format</tt>. The
     * implementation considers a pair of <tt>Format</tt>s matching if they have
     * the same encoding.
     *
     * @param formats the array of <tt>Format</tt>s to be searched for a match
     * to the specified <tt>format</tt>
     * @param format the <tt>Format</tt> to search for a match in the specified
     * <tt>formats</tt>
     * @return the first element of <tt>formats</tt> which matches
     * <tt>format</tt> i.e. is of the same encoding
     */
    private static Format findFirstMatchingFormat(
            Format[] formats,
            Format format)
    {
        for (Format match : formats)
        {
            /*
             * TODO Is the encoding enough? We've been explicitly told what
             * format to use so it may be that its non-encoding attributes which
             * have been specified are also necessary.
             */
            if (match.isSameEncoding(format))
                return match;
        }
        return null;
    }

    /**
     * Gets the <tt>DataSource</tt> that this instance uses to read captured
     * media from. If it does not exist yet, it is created.
     *
     * @return the <tt>DataSource</tt> that this instance uses to read captured
     * media from
     */
    protected synchronized DataSource getCaptureDevice()
    {
        if (captureDevice == null)
            captureDevice = createCaptureDevice();
        return captureDevice;
    }

    /**
     * Gets {@link #captureDevice} in a connected state. If this instance is not
     * connected to <tt>captureDevice</tt> yet, first tries to connect to it.
     * Returns <tt>null</tt> if this instance fails to create
     * <tt>captureDevice</tt> or to connect to it.
     *
     * @return {@link #captureDevice} in a connected state; <tt>null</tt> if
     * this instance fails to create <tt>captureDevice</tt> or to connect to it
     */
    private DataSource getConnectedCaptureDevice()
    {
        DataSource captureDevice = getCaptureDevice();

        if ((captureDevice != null) && !captureDeviceIsConnected)
        {
            Throwable exception = null;

            try
            {
                getDevice().connect(captureDevice);
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }

            if (exception == null)
                captureDeviceIsConnected = true;
            else
                captureDevice = null;
        }
        return captureDevice;
    }

    /**
     * Gets the <tt>MediaDevice</tt> associated with this instance and the work
     * of a <tt>MediaStream</tt> with which is represented by it.
     *
     * @return the <tt>MediaDevice</tt> associated with this instance and the
     * work of a <tt>MediaStream</tt> with which is represented by it
     */
    public AbstractMediaDevice getDevice()
    {
        return device;
    }

    /**
     * Gets the <tt>MediaDirection</tt> in which this instance has been started.
     * For example, a <tt>MediaDirection</tt> which returns <tt>true</tt> for
     * <tt>allowsSending()</tt> signals that this instance is capturing media
     * from its <tt>MediaDevice</tt>.
     *
     * @return the <tt>MediaDirection</tt> in which this instance has been
     * started
     */
    public MediaDirection getStartedDirection()
    {
        return startedDirection;
    }

    /**
     * Gets the <tt>MediaFormat</tt> in which this instance captures media from
     * its associated <tt>MediaDevice</tt>.
     *
     * @return the <tt>MediaFormat</tt> in which this instance captures media
     * from its associated <tt>MediaDevice</tt>
     */
    public MediaFormat getFormat()
    {
        Processor processor = getProcessor();

        if ((processor != null)
                && (this.processor == processor)
                && !processorIsPrematurelyClosed)
        {
            MediaType mediaType = getMediaType();

            for (TrackControl trackControl : processor.getTrackControls())
            {
                if (!trackControl.isEnabled())
                    continue;

                MediaFormat format
                    = MediaFormatImpl.createInstance(trackControl.getFormat());

                if ((format != null) && format.getMediaType().equals(mediaType))
                    return format;
            }
        }
        return null;
    }

    /**
     * Gets the <tt>MediaType</tt> of the media captured and played back by this
     * instance. It is the same as the <tt>MediaType</tt> of its associated
     * <tt>MediaDevice</tt>.
     *
     * @return the <tt>MediaType</tt> of the media captured and played back by
     * this instance as reported by {@link MediaDevice#getMediaType()} of its
     * associated <tt>MediaDevice</tt>
     */
    private MediaType getMediaType()
    {
        return getDevice().getMediaType();
    }

    /**
     * Gets the output <tt>DataSource</tt> of this instance which provides the
     * captured (RTP) data to be sent by <tt>MediaStream</tt> to
     * <tt>MediaStreamTarget</tt>.
     *
     * @return the output <tt>DataSource</tt> of this instance which provides
     * the captured (RTP) data to be sent by <tt>MediaStream</tt> to
     * <tt>MediaStreamTarget</tt>
     */
    public DataSource getOutputDataSource()
    {
        Processor processor = getProcessor();
        DataSource outputDataSource;

        if ((processor == null)
                || ((processor.getState() < Processor.Realized)
                        && !waitForState(processor, Processor.Realized)))
            outputDataSource = null;
        else
        {
            outputDataSource = processor.getDataOutput();
            if (logger.isTraceEnabled() && (outputDataSource != null))
                logger
                    .trace(
                        "Processor with hashCode "
                            + processor.hashCode()
                            + " provided "
                            + MediaStreamImpl.toString(outputDataSource));

            /*
             * Whoever wants the outputDataSource, they expect it to be started
             * in accord with the previously-set direction.
             */
            startProcessorInAccordWithDirection(processor);
        }
        return outputDataSource;
    }

    /**
     * Gets the <tt>Processors</tt>s rendering <tt>ReceiveStream</tt>s for this
     * instance on its associated <tt>MediaDevice</tt>. The returned
     * <tt>List</tt> is a copy of the internal storage and, consequently,
     * modifications to it do not affect this instance.
     * The <tt>Processor</tt>s are used as a <tt>Player</tt>s.
     *
     * @return a new <tt>List</tt> of <tt>Processor</tt>s rendering
     * <tt>ReceiveStream</tt>s for this instance on its associated
     * <tt>MediaDevice</tt>
     */
    protected List<Processor> getPlayers()
    {
        synchronized (players)
        {
            return new ArrayList<Processor>(players.values());
        }
    }

    /**
     * Gets the JMF <tt>Processor</tt> which transcodes the <tt>MediaDevice</tt>
     * of this instance into the format of this instance.
     *
     * @return the JMF <tt>Processor</tt> which transcodes the
     * <tt>MediaDevice</tt> of this instance into the format of this instance
     */
    private Processor getProcessor()
    {
        if (processor == null)
        {
            DataSource captureDevice = getConnectedCaptureDevice();

            if (captureDevice != null)
            {
                Processor processor = null;
                Throwable exception = null;

                try
                {
                    processor = Manager.createProcessor(captureDevice);
                }
                catch (IOException ioe)
                {
                    // TODO
                    exception = ioe;
                }
                catch (NoProcessorException npe)
                {
                    // TODO
                    exception = npe;
                }

                if (exception != null)
                    logger
                        .error(
                            "Failed to create Processor for " + captureDevice,
                            exception);
                else
                {
                    if (processorControllerListener == null)
                        processorControllerListener = new ControllerListener()
                        {

                            /**
                             * Notifies this <tt>ControllerListener</tt> that
                             * the <tt>Controller</tt> which it is registered
                             * with has generated an event.
                             *
                             * @param event the <tt>ControllerEvent</tt>
                             * specifying the <tt>Controller</tt> which is the
                             * source of the event and the very type of the
                             * event
                             * @see ControllerListener#controllerUpdate(
                             * ControllerEvent)
                             */
                            public void controllerUpdate(ControllerEvent event)
                            {
                                processorControllerUpdate(event);
                            }
                        };
                    processor
                        .addControllerListener(processorControllerListener);

                    if (waitForState(processor, Processor.Configured))
                    {
                        this.processor = processor;
                        processorIsPrematurelyClosed = false;
                    }
                    else
                    {
                        if (processorControllerListener != null)
                            processor
                                .removeControllerListener(
                                    processorControllerListener);
                        processor = null;
                    }
                }
            }
        }
        return processor;
    }

    /**
     * Gets a list of the <tt>MediaFormat</tt>s in which this instance is
     * capable of capturing media from its associated <tt>MediaDevice</tt>.
     *
     * @return a new list of <tt>MediaFormat</tt>s in which this instance is
     * capable of capturing media from its associated <tt>MediaDevice</tt>
     */
    public List<MediaFormat> getSupportedFormats()
    {
        Processor processor = getProcessor();
        Set<Format> supportedFormats = new HashSet<Format>();

        if ((processor != null)
                && (this.processor == processor)
                && !processorIsPrematurelyClosed)
        {
            MediaType mediaType = getMediaType();

            for (TrackControl trackControl : processor.getTrackControls())
            {
                if (!trackControl.isEnabled())
                    continue;

                for (Format supportedFormat : trackControl.getSupportedFormats())
                    switch (mediaType)
                    {
                    case AUDIO:
                        if (supportedFormat instanceof AudioFormat)
                            supportedFormats.add(supportedFormat);
                        break;
                    case VIDEO:
                        if (supportedFormat instanceof VideoFormat)
                            supportedFormats.add(supportedFormat);
                        break;
                    }
            }
        }

        List<MediaFormat> supportedMediaFormats
            = new ArrayList<MediaFormat>(supportedFormats.size());

        for (Format format : supportedFormats)
            supportedMediaFormats.add(MediaFormatImpl.createInstance(format));
        return supportedMediaFormats;
    }

    /**
     * Determines whether this <tt>MediaDeviceSession</tt> is set to output
     * "silence" instead of the actual media fed from its
     * <tt>CaptureDevice</tt>.
     *
     * @return <tt>true</tt> if this <tt>MediaDeviceSession</tt> is set to
     * output "silence" instead of the actual media fed from its
     * <tt>CaptureDevice</tt>; otherwise, <tt>false</tt>
     */
    public boolean isMute()
    {
        DataSource captureDevice = this.captureDevice;

        if (captureDevice == null)
            return mute;
        if (captureDevice instanceof MutePushBufferDataSource)
            return ((MutePushBufferDataSource) captureDevice).isMute();
        return false;
    }

    /**
     * Gets notified about <tt>ControllerEvent</tt>s generated by
     * {@link #processor}.
     *
     * @param event the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     */
    private void processorControllerUpdate(ControllerEvent event)
    {
        if (event instanceof ConfigureCompleteEvent)
        {
            Processor processor = (Processor) event.getSourceController();

            if (processor != null)
            {
                try
                {
                    processor
                        .setContentDescriptor(
                            new ContentDescriptor(
                                    ContentDescriptor.RAW_RTP));
                }
                catch (NotConfiguredError nce)
                {
                    logger
                        .error(
                            "Failed to set ContentDescriptor to Processor.",
                            nce);
                }

                if (format != null)
                    setFormat(processor, format);

                if(parentStream instanceof AudioMediaStreamImpl
                    && localSoundLevelListeners.size() > 0)
                {
                    // here we add sound level indicator for captured media
                    // from the microphone if there are interested listeners
                    try
                    {
                        TrackControl tcs[] = processor.getTrackControls();

                        if (tcs != null)
                            for (TrackControl tc : tcs)
                                if (tc.getFormat() instanceof AudioFormat)
                                {
                                    addLocalSoundLevelIndicator(tc);
                                    break;
                                }
                    }
                    catch (UnsupportedPlugInException ex)
                    {
                        logger
                            .error(
                                "Unsupported sound level indicator effect",
                                ex);
                    }
                }
            }
        }
        else if (event instanceof ControllerClosedEvent)
        {
            Processor processor = (Processor) event.getSourceController();

            /*
             * If everything goes according to plan, we should've removed the
             * ControllerListener from the processor by now.
             */
            logger.warn(event);

            // TODO Should the access to processor be synchronized?
            if ((processor != null) && (this.processor == processor))
                processorIsPrematurelyClosed = true;
        }
    }

    /**
     * Notifies this instance that a specific <tt>Processor</tt> of
     * remote content has generated a <tt>RealizeCompleteEvent</tt>.
     * Allows extenders to carry out additional processing on the
     * <tt>Processor</tt>. The <tt>Processor</tt> is used as a <tt>Player</tt>.
     *
     * @param player the <tt>Processor</tt> which is the source of a
     * <tt>RealizeCompleteEvent</tt>
     */
    protected void realizeComplete(Processor player)
    {
    }

    /**
     * Removes a <tt>ReceiveStream</tt> from this <tt>MediaDeviceSession</tt> so
     * that it no longer plays back on the associated <tt>MediaDevice</tt>.
     *
     * @param receiveStream the <tt>ReceiveStream</tt> to be removed from this
     * <tt>MediaDeviceSession</tt> and playback on the associated
     * <tt>MediaDevice</tt>
     */
    public synchronized void removeReceiveStream(ReceiveStream receiveStream)
    {
        DataSource receiveStreamDataSource
            = receiveStreams.remove(receiveStream);

        removeSSRC(receiveStream.getSSRC());

        if ((receiveStreamDataSource != null)
                && !receiveStreams.containsValue(receiveStreamDataSource))
            synchronized (players)
            {
                Processor player = players.get(receiveStreamDataSource);

                if (player != null)
                    disposePlayer(player);
            }
    }

    /**
     * Sets the <tt>MediaFormat</tt> in which this <tt>MediaDeviceSession</tt>
     * outputs the media captured by its <tt>MediaDevice</tt>.
     *
     * @param format the <tt>MediaFormat</tt> in which this
     * <tt>MediaDeviceSession</tt> is to output the media captured by its
     * <tt>MediaDevice</tt>
     */
    public void setFormat(MediaFormat format)
    {
        MediaType mediaType = getMediaType();

        if (!mediaType.equals(format.getMediaType()))
            throw new IllegalArgumentException("format");

        /*
         * We need javax.media.Format and we know how to convert MediaFormat to
         * it only for MediaFormatImpl so assert early.
         */
        MediaFormatImpl<? extends Format> mediaFormatImpl
            = (MediaFormatImpl<? extends Format>) format;

        this.format = mediaFormatImpl.getFormat();

        /*
         * If the processor is after Configured, setting a different format will
         * silently fail. Recreate the processor in order to be able to set the
         * different format.
         */
        if (processor != null)
        {
            int processorState = processor.getState();

            if (processorState == Processor.Configured)
                setFormat(processor, this.format);
            else if (processorIsPrematurelyClosed
                        || ((processorState > Processor.Configured)
                                && !format.equals(getFormat())))
                setProcessor(null);
        }
    }

    /**
     * Sets the JMF <tt>Format</tt> in which a specific <tt>Processor</tt> is to
     * output media data.
     *
     * @param processor the <tt>Processor</tt> to set the output <tt>Format</tt>
     * of
     * @param format the JMF <tt>Format</tt> to set to <tt>processor</tt>
     */
    private void setFormat(Processor processor, Format format)
    {
        TrackControl[] trackControls = processor.getTrackControls();
        MediaType mediaType = getMediaType();

        for (int trackIndex = 0;
                trackIndex < trackControls.length;
                trackIndex++)
        {
            TrackControl trackControl = trackControls[trackIndex];

            if (!trackControl.isEnabled())
                continue;

            Format[] supportedFormats = trackControl.getSupportedFormats();

            if ((supportedFormats == null) || (supportedFormats.length < 1))
            {
                trackControl.setEnabled(false);
                continue;
            }

            Format supportedFormat = null;

            switch (mediaType)
            {
            case AUDIO:
                if (supportedFormats[0] instanceof AudioFormat)
                {
                    if (FMJConditionals.FORCE_AUDIO_FORMAT != null)
                        trackControl
                            .setFormat(FMJConditionals.FORCE_AUDIO_FORMAT);
                    else
                    {
                        supportedFormat
                            = findFirstMatchingFormat(supportedFormats, format);

                        /*
                         * We've failed to find a supported format so try to use
                         * whatever we've been told and, if it fails, the caller
                         * will at least know why.
                         */
                        if (supportedFormat == null)
                            supportedFormat = format;
                    }
                }
                break;
            case VIDEO:
                if (supportedFormats[0] instanceof VideoFormat)
                {
                    supportedFormat
                        = findFirstMatchingFormat(supportedFormats, format);

                    /*
                     * We've failed to find a supported format so try to use
                     * whatever we've been told and, if it fails, the caller
                     * will at least know why.
                     */
                    if (supportedFormat == null)
                        supportedFormat = format;

                    if (supportedFormat != null)
                        supportedFormat
                            = assertSize((VideoFormat) supportedFormat);
                }
                break;
            }

            if (supportedFormat == null)
                trackControl.setEnabled(false);
            else if (!supportedFormat.equals(trackControl.getFormat()))
            {
                Format setFormat = trackControl.setFormat(supportedFormat);

                if (setFormat == null)
                    logger
                        .error(
                            "Failed to set format of track "
                                + trackIndex
                                + " to "
                                + supportedFormat
                                + ". Processor is in state "
                                + processor.getState());
                else if (setFormat != supportedFormat)
                    logger
                        .warn(
                            "Failed to change format of track "
                                + trackIndex
                                + " from "
                                + setFormat
                                + " to "
                                + supportedFormat
                                + ". Processor is in state "
                                + processor.getState());
                else if (logger.isTraceEnabled())
                    logger
                        .trace(
                            "Set format of track "
                                + trackIndex
                                + " to "
                                + setFormat);
            }
        }
    }

    /**
     * Sets the indicator which determines whether this
     * <tt>MediaDeviceSession</tt> is set to output "silence" instead of the
     * actual media fed from its <tt>CaptureDevice</tt>.
     *
     * @param mute <tt>true</tt> to set this <tt>MediaDeviceSession</tt> to
     * output "silence" instead of the actual media fed from its
     * <tt>CaptureDevice</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        if (this.mute != mute)
        {
            this.mute = mute;

            DataSource captureDevice = this.captureDevice;

            if (captureDevice instanceof MutePushBufferDataSource)
                ((MutePushBufferDataSource) captureDevice).setMute(this.mute);
        }
    }

    /**
     * Sets the JMF <tt>Processor</tt> which is to transcode
     * {@link #captureDevice} into the format of this instance.
     *
     * @param processor the JMF <tt>Processor</tt> which is to transcode
     * {@link #captureDevice} into the format of this instance
     */
    private void setProcessor(Processor processor)
    {
        if (this.processor != processor)
        {
            closeProcessor();

            this.processor = processor;

            /*
             * Since the processor has changed, its output DataSource known to
             * the public has also changed.
             */
            firePropertyChange(OUTPUT_DATA_SOURCE, null, null);
        }
    }

    /**
     * Starts the processing of media in this instance in a specific direction.
     *
     * @param direction a <tt>MediaDirection</tt> value which represents the
     * direction of the processing of media to be started. For example,
     * {@link MediaDirection#SENDRECV} to start both capture and playback of
     * media in this instance or {@link MediaDirection#SENDONLY} to only start
     * the capture of media in this instance
     */
    public void start(MediaDirection direction)
    {
        if (direction == null)
            throw new NullPointerException("direction");

        startedDirection = startedDirection.or(direction);

        if (startedDirection.allowsSending())
        {
            Processor processor = getProcessor();

            if (processor != null)
                startProcessorInAccordWithDirection(processor);
        }
    }

    /**
     * Starts a specific <tt>Processor</tt> if this <tt>MediaDeviceSession</tt>
     * has been started and the specified <tt>Processor</tt> is not started.
     *
     * @param processor the <tt>Processor</tt> to start
     */
    private void startProcessorInAccordWithDirection(Processor processor)
    {
        if (startedDirection.allowsSending()
                && (processor.getState() != Processor.Started))
        {
            processor.start();
            if (logger.isTraceEnabled())
                logger
                    .trace(
                        "Started Processor with hashCode "
                            + processor.hashCode());
        }
    }

    /**
     * Stops the processing of media in this instance in a specific direction.
     *
     * @param direction a <tt>MediaDirection</tt> value which represents the
     * direction of the processing of media to be stopped. For example,
     * {@link MediaDirection#SENDRECV} to stop both capture and playback of
     * media in this instance or {@link MediaDirection#SENDONLY} to only stop
     * the capture of media in this instance
     */
    public void stop(MediaDirection direction)
    {
        if (direction == null)
            throw new NullPointerException("direction");

        switch (startedDirection)
        {
        case SENDRECV:
            if (direction.allowsReceiving())
                startedDirection
                    = direction.allowsSending()
                        ? MediaDirection.INACTIVE
                        : MediaDirection.SENDONLY;
            else if (direction.allowsSending())
                startedDirection = MediaDirection.RECVONLY;
            break;
        case SENDONLY:
            if (direction.allowsSending())
                startedDirection = MediaDirection.INACTIVE;
            break;
        case RECVONLY:
            if (direction.allowsReceiving())
                startedDirection = MediaDirection.INACTIVE;
            break;
        case INACTIVE:
            /*
             * This MediaDeviceSession is already inactive so there's nothing to
             * stop.
             */
            break;
        default:
            throw new IllegalArgumentException("direction");
        }

        if (startedDirection.allowsSending())
            if ((processor != null)
                    && (processor.getState() > Processor.Configured))
            {
                processor.stop();
                if (logger.isTraceEnabled())
                    logger
                        .trace(
                            "Stopped Processor with hashCode "
                                + processor.hashCode());
            }
    }

    /**
     * Waits for the specified JMF <tt>Processor</tt> to enter the specified
     * <tt>state</tt> and returns <tt>true</tt> if <tt>processor</tt> has
     * successfully entered <tt>state</tt> or <tt>false</tt> if <tt>process</tt>
     * has failed to enter <tt>state</tt>.
     *
     * @param processor the JMF <tt>Processor</tt> to wait on
     * @param state the state as defined by the respective <tt>Processor</tt>
     * state constants to wait <tt>processor</tt> to enter
     * @return <tt>true</tt> if <tt>processor</tt> has successfully entered
     * <tt>state</tt>; otherwise, <tt>false</tt>
     */
    private static boolean waitForState(Processor processor, int state)
    {
        return new ProcessorUtility().waitForState(processor, state);
    }

    /**
     * Sets the parent <tt>MediaStream</tt> that creates us.
     *
     * @param parentStream the parentStream to set
     */
    public void setParentStream(MediaStream parentStream)
    {
        this.parentStream = parentStream;
    }

    /**
     * Retrurns the parent <tt>MediaStream</tt> that creates us.
     * @return the parentStream that created us.
     */
    public MediaStream getParentStream()
    {
        return this.parentStream;
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public void addLocalUserSoundLevelListener(SoundLevelListener l)
    {
        synchronized(localSoundLevelListeners)
        {
            if (!localSoundLevelListeners.contains(l))
                localSoundLevelListeners.add(l);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public void removeLocalUserSoundLevelListener(SoundLevelListener l)
    {
        synchronized(localSoundLevelListeners)
        {
            localSoundLevelListeners.remove(l);
        }
    }

    /**
     * Creates and dispatches a <tt>SoundLevelEvent</tt> notifying
     * registered listeners that the local user sound level has changed.
     *
     * @param level the new level
     */
    void fireLocalUserSoundLevelEvent(int level)
    {
        // If no local ssrc give up
        if(parentStream.getLocalSourceID() == 0)
            return;

        Map<Long,Integer> lev = new HashMap<Long, Integer>();
        lev.put(parentStream.getLocalSourceID(), level);
        SoundLevelChangeEvent soundLevelEvent
            = new SoundLevelChangeEvent(parentStream,lev);

        List<SoundLevelListener> listeners;

        synchronized (localSoundLevelListeners)
        {
            listeners =
                new ArrayList<SoundLevelListener>(localSoundLevelListeners);
        }

        for (Iterator<SoundLevelListener> listenerIter
                = listeners.iterator(); listenerIter.hasNext();)
        {
            SoundLevelListener listener = listenerIter.next();

            listener.soundLevelChanged(soundLevelEvent);
        }
    }

    /**
     * Adds <tt>listener</tt> to the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param listener the <tt>SoundLevelListener</tt> to register with this
     * <tt>AudioMediaStream</tt>
     * @see AudioMediaStream#addSoundLevelListener(SoundLevelListener)
     */
    public void addSoundLevelListener(SoundLevelListener listener)
    {
        synchronized(streamSoundLevelListeners)
        {
            if (!streamSoundLevelListeners.contains(listener))
                streamSoundLevelListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param listener the <tt>SoundLevelListener</tt> to no longer be notified
     * by this <tt>AudioMediaStream</tt> about changes in the sound levels of
     * the conference participants that the remote party may be mixing
     * @see AudioMediaStream#removeSoundLevelListener(SoundLevelListener)
     */
    public void removeSoundLevelListener(SoundLevelListener listener)
    {
        synchronized(streamSoundLevelListeners)
        {
            streamSoundLevelListeners.remove(listener);
        }
    }

    /**
     * Fires a <tt>StreamSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param level the new sound level
     */
    public void fireStreamSoundLevelEvent(int level)
    {
        // If no remote ssrc give up
        if(parentStream.getRemoteSourceID() == 0)
            return;

        Map<Long,Integer> lev = new HashMap<Long, Integer>();
        lev.put(parentStream.getLocalSourceID(), level);
        SoundLevelChangeEvent event
            = new SoundLevelChangeEvent(parentStream, lev);

        SoundLevelListener[] ls;

        synchronized(streamSoundLevelListeners)
        {
            ls = streamSoundLevelListeners.toArray(
                new SoundLevelListener[streamSoundLevelListeners.size()]);
        }

        for (SoundLevelListener listener : ls)
        {
            listener.soundLevelChanged(event);
        }
    }

    /**
     * Returns the list of SSRC identifiers that this device session is handling
     * streams from. In this case (i.e. the case of a device session handling
     * a single remote party) we would rarely (if ever) have more than a single
     * SSRC identifier returned. However, we would also be using the same method
     * to query a device session operating over a mixer in which case we would
     * have the SSRC IDs of all parties currently contributing to the mixing.
     *
     * @return a <tt>long[]</tt> array of SSRC identifiers that this device
     * session is handling streams from.
     */
    public long[] getRemoteSSRCList()
    {
        return ssrcList;
    }

    /**
     * Adds <tt>ssrc</tt> to the array of SSRC identifiers representing parties
     * that this <tt>MediaDeviceSession</tt> is currently receiving streams
     * from.
     *
     * @param ssrc the new SSRC identifier that we'd like to add to the array of
     * <tt>ssrc</tt> identifiers stored by this session.
     */
    private void addSSRC(long ssrc)
    {
        //init if necessary
        if ( ssrcList == null)
        {
            setSsrcList(new long[]{ssrc});
            return;
        }

        //check whether we already have this ssrc
        for ( long i : ssrcList)
        {
            if ( i == ssrc)
                return;
        }

        //resize the array and add the new ssrc to the end.
        long[] newSsrcList = new long[ssrcList.length + 1];

        System.arraycopy(ssrcList, 0, newSsrcList, 0, ssrcList.length);
        newSsrcList[newSsrcList.length - 1] = ssrc;

        setSsrcList(newSsrcList);
    }

    /**
     * Removes <tt>ssrc</tt> from the array of SSRC identifiers representing
     * parties that this <tt>MediaDeviceSession</tt> is currently receiving
     * streams from.
     *
     * @param ssrc the SSRC identifier that we'd like to remove from the array
     * of <tt>ssrc</tt> identifiers stored by this session.
     */
    private void removeSSRC(long ssrc)
    {
        //find the ssrc
        int index = -1;

        if (ssrcList == null || ssrcList.length == 0)
        {
            //list is already empty so there's nothing to do.
            return;
        }

        for (int i = 0; i < ssrcList.length; i++)
        {
            if (ssrcList[i] == ssrc)
            {
                index = i;
                break;
            }
        }

        if (index < 0 || index >= ssrcList.length)
        {
            //the ssrc we are trying to remove is not in the list so there's
            //nothing to do.
            return;
        }

        //if we get here and the list has a single element this would mean we
        //simply need to empty it as the only element is the one we are removing
        if (ssrcList.length == 1)
        {
            setSsrcList(null);
            return;
        }

        long[] newSsrcList = new long[ssrcList.length];

        System.arraycopy(ssrcList, 0, newSsrcList, 0, index);
        if (index < ssrcList.length - 1)
        {
            System.arraycopy(ssrcList,    index + 1,
                             newSsrcList, index,
                             ssrcList.length - index - 1);
        }

        setSsrcList(newSsrcList);
    }

    /**
     * Sets the list of SSRC identifiers that this device stores to
     * <tt>newSsrcList</tt> and fires a <tt>PropertyChangeEvent</tt> for the
     * <tt>SSRC_LIST</tt> property.
     *
     * @param newSsrcList that SSRC array that we'd like to replace the existing
     * SSRC list with.
     */
    private void setSsrcList(long[] newSsrcList)
    {
        long[] oldSsrcList = ssrcList;
        ssrcList = newSsrcList;

        firePropertyChange(SSRC_LIST, oldSsrcList, newSsrcList);
    }
}
