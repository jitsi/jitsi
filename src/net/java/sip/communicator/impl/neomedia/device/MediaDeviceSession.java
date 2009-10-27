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
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Represents the use of a specific <tt>MediaDevice</tt> by a
 * <tt>MediaStream</tt>.
 * 
 * @author Lubomir Marinov
 */
public class MediaDeviceSession
    implements ControllerListener
{

    /**
     * The <tt>Logger</tt> used by the <tt>MediaDeviceSession</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaDeviceSession.class);

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
     * The <tt>Player</tt>s rendering <tt>ReceiveStream</tt>s on the
     * <tt>MediaDevice</tt> represented by this instance. Associated with
     * <tt>DataSource</tt> because different <tt>ReceiveStream</tt>s may be
     * added with one and the same <tt>DataSource</tt> so it has to be clear
     * when a new <tt>Player</tt> is to be created and when it is to be
     * disposed.
     */
    private final Map<DataSource, Player> players
        = new HashMap<DataSource, Player>();

    /**
     * The JMF <tt>Processor</tt> which transcodes {@link #captureDevice} into
     * the format of this instance.
     */
    private Processor processor;

    /**
     * The <tt>ReceiveStream</tt>s rendered by this instance on its associated
     * <tt>MediaDevice</tt>. Mapped to <tt>DataSource</tt> because extenders may
     * choose to override their <tt>DataSource</tt> and use one and the same
     * instance for different <tt>ReceiveStream</tt>s (e.g. audio mixing).
     */
    private final Map<ReceiveStream, DataSource> receiveStreams
        = new HashMap<ReceiveStream, DataSource>();

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
            addReceiveStream(receiveStream, receiveStreamDataSource);
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
    protected void addReceiveStream(
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

        synchronized (players)
        {
            Player player = players.get(receiveStreamDataSource);

            if (player == null)
            {
                Throwable exception = null;

                try
                {
                    player = Manager.createPlayer(receiveStreamDataSource);
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
                    logger
                        .error(
                            "Failed to create player for new receive stream "
                                + receiveStream,
                            exception);
                else
                {
                    player.addControllerListener(this);
                    player.realize();

                    players.put(receiveStreamDataSource, player);
                }
            }
        }
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
        if (processor != null)
        {
            processor.stop();
            if (processor.getState() == Processor.Realized)
            {
                DataSource dataOutput = processor.getDataOutput();

                if (dataOutput != null)
                    dataOutput.disconnect();
            }
            processor.deallocate();
            processor.close();
        }
    }

    /**
     * Notifies this <tt>ControllerListener</tt> that the <tt>Controller</tt>
     * which it is registered with has generated an event.
     *
     * @param event the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     * @see ControllerListener#controllerUpdate(ControllerEvent)
     */
    public void controllerUpdate(ControllerEvent event)
    {
        if (event instanceof RealizeCompleteEvent)
        {
            Player player = (Player) event.getSourceController();

            if (player != null)
            {
                player.start();

                realizeComplete(player);
            }
        }
    }

    /**
     * Releases the resources allocated by a specific <tt>Player</tt> in the
     * course of its execution and prepares it to be garbage collected.
     *
     * @param player the <tt>Player</tt> to dispose of
     */
    protected void disposePlayer(Player player)
    {
        synchronized (players)
        {
            Iterator<Map.Entry<DataSource, Player>> playerIter
                = players.entrySet().iterator();

            while (playerIter.hasNext())
                if (playerIter.next().getValue().equals(player))
                {
                    playerIter.remove();
                    break;
                }

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
            for (Player player : getPlayers())
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
            if (match.isSameEncoding(format))
                return match;
        return null;
    }

    /**
     * Gets the <tt>DataSource</tt> that this instance uses to read captured
     * media from. If it does not exist yet, it is created.
     *
     * @return the <tt>DataSource</tt> that this instance uses to read captured
     * media from
     */
    protected DataSource getCaptureDevice()
    {
        if (captureDevice == null)
            captureDevice = getDevice().createOutputDataSource();
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
                captureDevice.connect();
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }
            catch (NullPointerException npe)
            {
                /*
                 * TODO The old media says it happens when the operating system
                 * does not support the operation.
                 */
                exception = npe;
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
     * Gets the <tt>MediaFormat</tt> in which this instance captures media from
     * its associated <tt>MediaDevice</tt>.
     *
     * @return the <tt>MediaFormat</tt> in which this instance captures media
     * from its associated <tt>MediaDevice</tt>
     */
    public MediaFormat getFormat()
    {
        Processor processor = getProcessor();

        if (processor != null)
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

        return (processor == null) ? null : processor.getDataOutput();
    }

    /**
     * Gets the <tt>Player</tt>s rendering <tt>ReceiveStream</tt>s for this
     * instance on its associated <tt>MediaDevice</tt>. The returned
     * <tt>List</tt> is a copy of the internal storage and, consequently,
     * modifications to it do not affect this instance.
     *
     * @return a new <tt>List</tt> of <tt>Player</tt>s rendering
     * <tt>ReceiveStream</tt>s for this instance on its associated
     * <tt>MediaDevice</tt>
     */
    protected List<Player> getPlayers()
    {
        synchronized (players)
        {
            return new ArrayList<Player>(players.values());
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
                else if (waitForState(processor, Processor.Configured))
                {
                    try
                    {
                        exception = null;
                        processor
                            .setContentDescriptor(
                                new ContentDescriptor(
                                        ContentDescriptor.RAW_RTP));
                    }
                    catch (NotConfiguredError nce)
                    {
                        // TODO
                        exception = nce;
                        processor = null;
                    }

                    if (exception != null)
                        logger
                            .error(
                                "Failed to set ContentDescriptor to Processor.",
                                exception);
                    else
                        this.processor = processor;
                }
                else
                    processor = null;
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

        if (processor != null)
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
     * Notifies this instance that a specific <tt>Player</tt> of remote content
     * has generated a <tt>RealizeCompleteEvent</tt>. Allows extenders to carry
     * out additional processing on the <tt>Player</tt>.
     *
     * @param player the <tt>Player</tt> which is the source of a
     * <tt>RealizeCompleteEvent</tt>
     */
    protected void realizeComplete(Player player)
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
    public void removeReceiveStream(ReceiveStream receiveStream)
    {
        DataSource receiveStreamDataSource
            = receiveStreams.remove(receiveStream);

        if ((receiveStreamDataSource != null)
                && !receiveStreams.containsValue(receiveStreamDataSource))
            synchronized (players)
            {
                Player player = players.get(receiveStreamDataSource);

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

        Processor processor = getProcessor();

        if (processor != null)
        {
            if ((processor.getState() < Processor.Configured)
                    && !waitForState(processor, Processor.Configured))
            {
                // TODO
                return;
            }

            for (TrackControl trackControl : processor.getTrackControls())
            {
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
                                = findFirstMatchingFormat(
                                    supportedFormats,
                                    mediaFormatImpl.getFormat());
                        }
                    }
                    break;
                case VIDEO:
                    if (supportedFormats[0] instanceof VideoFormat)
                    {
                        supportedFormat
                            = findFirstMatchingFormat(
                                supportedFormats,
                                mediaFormatImpl.getFormat());

                        if (supportedFormat != null)
                            supportedFormat
                                = assertSize((VideoFormat) supportedFormat);
                    }
                    break;
                }

                if (supportedFormat == null)
                    trackControl.setEnabled(false);
                else
                    trackControl.setFormat(supportedFormat);
            }
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
            throw new IllegalArgumentException("direction");

        if (MediaDirection.SENDRECV.equals(direction)
                || MediaDirection.SENDONLY.equals(direction))
        {
            Processor processor = getProcessor();

            if ((processor != null)
                    && (processor.getState() != Processor.Started))
                processor.start();
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
            throw new IllegalArgumentException("direction");

        if (MediaDirection.SENDRECV.equals(direction)
                || MediaDirection.SENDONLY.equals(direction))
            if ((processor != null)
                    && (processor.getState() == Processor.Started))
                processor.start();
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
}
