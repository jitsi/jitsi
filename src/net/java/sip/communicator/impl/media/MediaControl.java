/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.util.*;
import java.beans.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.service.media.MediaException;
import net.java.sip.communicator.util.*;

/**
 * Provides a generic way to control media package.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Jean Lorchat
 * @author Ryan Ricard
 * @author Ken Larson
 * @author Lubomir Marinov
 */
public class MediaControl
    extends PropertyChangeNotifier
{
    private static final Logger logger = Logger.getLogger(MediaControl.class);

    /**
     * Our device configuration helper.
     */
    private DeviceConfiguration deviceConfiguration = null;

    /**
     * Our encoding configuration helper.
     */
    private EncodingConfiguration encodingConfiguration = null;

    /**
     * A data source merging our audio and video data sources.
     */
    private DataSource avDataSource = null;

    /**
     * The audio <code>DataSource</code> which provides mute support.
     */
    private MutePushBufferDataSource muteAudioDataSource;

    /**
     * The current video <code>DataSource</code>. If present, it's available in
     * {@link #avDataSource} either directly or as a merged
     * <code>DataSource</code>.
     */
    private SourceCloneable videoDataSource;

    /**
     * The property which represents the current video <code>DataSource</code>.
     */
    public static final String VIDEO_DATA_SOURCE = "VIDEO_DATA_SOURCE";

    /**
     * SDP Codes of all video formats that JMF supports for current datasource.
     */
    private String[] supportedVideoEncodings = new String[0];

    /**
     * SDP Codes of all audio formats that JMF supports for current datasource.
     */
    private String[] supportedAudioEncodings = new String[0];

    /**
     * The indicator which determines whether {@link #supportedAudioEncodings}
     * and {@link #supportedVideoEncodings} are already calculated to be
     * up-to-date with the current {@link #sourceProcessor} and the lock to
     * synchronize the access to the mentioned calculation.
     */
    private final boolean[] supportedEncodingsAreCalculated = new boolean[1];

    /**
     * The processor that will be handling content coming from our capture data
     * sources.
     */
    private Processor sourceProcessor = null;

    /**
     * The list of readers currently using our processor.
     */
    private final List<Object> processorReaders = new Vector<Object>();

    /**
     * An object that we use for.
     */
    private final ProcessorUtility processorUtility = new ProcessorUtility();

    /**
     * The name of the property that could contain the name of a media file
     * to use instead of capture devices.
     */
    private static final String DEBUG_DATA_SOURCE_URL_PROPERTY_NAME
        = "net.java.sip.communicator.impl.media.DEBUG_DATA_SOURCE_URL";

    /**
     * The indicator which determines whether the streaming of local video
     * through this <code>MediaControl</code> is allowed. The setting does not
     * reflect the availability of actual video capture devices, it just
     * expresses the desire of the user to have the local video streamed in the
     * case the system is actually able to do so.
     */
    private boolean localVideoAllowed = false;

    /**
     * The default constructor.
     */
    public MediaControl()
    {
        //com.sun.media.util.Registry.set("allowLogging", Boolean.TRUE);
    }

    /**
     * Returns the duration of the output data source. Usually this will be
     * DURATION_UNKNOWN, but if the current data source is set to an audio
     * file, then this value will be of some use.
     * @return the output duration
     */
    public javax.media.Time getOutputDuration()
    {
        return (sourceProcessor == null) ? Duration.DURATION_UNKNOWN
                                         : sourceProcessor.getDuration();
    }

    /**
     * Initializes the media control.
     *
     * @param deviceConfig the <tt>DeviceConfiguration</tt> that we should use
     * when retrieving device handlers.
     *
     * @throws MediaException if initialization fails.
     */
    public void initialize( DeviceConfiguration deviceConfig,
                            EncodingConfiguration encodingConfig)
        throws MediaException
    {
        this.deviceConfiguration = deviceConfig;
        this.encodingConfiguration = encodingConfig;

        // init the encodings settings
        encodingConfig.initializeFormatPreferences();
        encodingConfig.registerCustomPackages();

        String debugDataSourceURL
            = MediaActivator.getConfigurationService().getString(
                DEBUG_DATA_SOURCE_URL_PROPERTY_NAME);

        if(debugDataSourceURL == null)
        {
            initCaptureDevices();
        }
        else
        {
            initDebugDataSource(debugDataSourceURL);
        }

        deviceConfiguration.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt)
            {
                if(evt.getPropertyName().equals(
                    DeviceConfiguration.AUDIO_CAPTURE_DEVICE))
                {
                    try
                    {
                        initCaptureDevices();
                    }
                    catch (MediaException e)
                    {
                        logger.error(
                            "Cannot init capture devices after change", e);
                    }
                }
            }
        });
    }

    /**
     * Opens all detected capture devices making them ready to capture.
     * <p>
     * The method is kept private because it relies on
     * {@link #deviceConfiguration} which is (publicly) set only through
     * {@link #initialize(DeviceConfiguration)}.
     * </p>
     *
     * @throws MediaException if opening the devices fails.
     */
    private void initCaptureDevices()
        throws MediaException
    {
        disposeBeforeInitCaptureDevices();

        // Init audio device
        CaptureDeviceInfo audioDeviceInfo
            = deviceConfiguration.getAudioCaptureDevice();
        DataSource audioDataSource = null;

        muteAudioDataSource = null;
        if (audioDeviceInfo != null)
        {
            audioDataSource = createDataSource(audioDeviceInfo.getLocator());

            /* Provide mute support for the audio (if possible). */
            if (audioDataSource instanceof PushBufferDataSource)
                audioDataSource =
                    muteAudioDataSource =
                        new MutePushBufferDataSource(
                            (PushBufferDataSource) audioDataSource);
        }

        // Init video device
        CaptureDeviceInfo videoDeviceInfo
            = localVideoAllowed
                ? deviceConfiguration.getVideoCaptureDevice()
                : null;
        DataSource videoDataSource = null;

        DataSource cloneableVideoDataSource = null;
        try
        {
            if (videoDeviceInfo != null)
            {
                videoDataSource = createDataSource(videoDeviceInfo.getLocator());

                // we will check video sizes and will set the most appropriate one
                selectVideoSize(videoDataSource);

                cloneableVideoDataSource =
                    Manager.createCloneableDataSource(videoDataSource);
                if (cloneableVideoDataSource != null)
                    videoDataSource = cloneableVideoDataSource;
            }
        }
        finally
        {
            setVideoDataSource((SourceCloneable) cloneableVideoDataSource);
        }

        // Create the audio/video data source.
        if (audioDataSource != null && videoDataSource != null)
        {
            try
            {
                avDataSource
                    = Manager.createMergingDataSource(new DataSource[] {
                          audioDataSource,
                          videoDataSource
                      });
            }
            catch (IncompatibleSourceException exc)
            {
                logger.fatal(
                        "Failed to create a media data source!"
                            + "Media transmission won't be enabled!",
                        exc);
                throw new InternalError(
                        "Failed to create a media data source!"
                            + "Media transmission won't be enabled!"
                            + exc.getMessage());
            }
        }
        else if (audioDataSource != null)
            avDataSource = audioDataSource;
        else if (videoDataSource != null)
            avDataSource = videoDataSource;
        else
            avDataSource = null;

        //avDataSource may be null (Bug report Vince Fourcade)
        if (avDataSource != null)
            initProcessor(avDataSource);
        else
            sourceProcessor = null;
    }

    /**
     * Allows this instance to dispose of any state which is reinitialized by
     * {@link #initCaptureDevices()}. For example, a vital requirement is to
     * invoke {@link Controller#close()} on <code>sourceProcessor</code>
     * regardless of the fact that it is soon to not be referenced at all or it
     * will keep unnecessary threads alive and they will in turn keep just about
     * anything created to an associated <code>Call</code>.
     */
    private void disposeBeforeInitCaptureDevices()
    {
        if (avDataSource != null)
        {
            /*
             * As reported by Carlos Alexandre, stopping before disconnecting
             * resolves a slow disconnect on Linux.
             */
            try
            {
                avDataSource.stop();
            }
            catch (IOException ex)
            {
                /*
                 * We cannot do much about the exception because we're not
                 * really interested in the stopping but rather in calling
                 * DataSource#disconnect() anyway.
                 */
                logger.error("Failed to properly stop avDataSource.", ex);
            }

            avDataSource.disconnect();
        }
        if (sourceProcessor != null)
        {
            sourceProcessor.stop();
            if (sourceProcessor.getState() == Processor.Realized)
            {
                DataSource dataOutput = sourceProcessor.getDataOutput();

                if (dataOutput != null)
                    dataOutput.disconnect();
            }
            sourceProcessor.deallocate();
            sourceProcessor.close();
        }
    }

    /**
     * Opens the source pointed to by the <tt>debugMediaSource</tt> URL and
     * prepares to use it instead of capture devices.
     *
     * @param debugMediaSource an url (e.g. file:/home/user/movie.mov) pointing
     * to a media file to use instead of capture devices.
     *
     * @throws MediaException if opening the devices fails.
     */
    public void initDebugDataSource(String debugMediaSource)
        throws MediaException
    {
        try
        {
            initDataSourceFromURL(new URL(debugMediaSource));
        }
        catch (MalformedURLException e)
        {
            logger.fatal("Failed to Create the Debug Media Data Source!",e);
        }
    }

    /**
     * Opens the source pointed to by the <tt>dataSourceURL</tt> URL and
     * prepares to use it instead of capture devices
     *
     * @param dataSourceURL an URL (e.g. file:/home/user/outgoing_message.wav)
     * pointing to a media file to use instead of capture devices
     *
     * @throws MediaException if opening the devices fails
     */
    public void initDataSourceFromURL(URL dataSourceURL)
        throws MediaException
    {
        if (logger.isDebugEnabled())
            logger.debug("Using a data source from url: " + dataSourceURL);

        avDataSource = createDataSource(new MediaLocator(dataSourceURL));

        //avDataSource may be null (Bug report Vince Fourcade)
        if (avDataSource != null)
            initProcessor(avDataSource);
    }


    /**
     * Initialize the processor that we will be using for transmission. The
     * method also updates the list of supported formats limiting it to the
     * formats supported by <tt>dataSource</tt>
     * @param dataSource the source to use for our source processor.
     * @throws MediaException if connecting the data source or initializing the
     * processor fails.
     */
    private void initProcessor(DataSource dataSource)
        throws MediaException
    {
        // this is the second stage of initing encoding configs
        encodingConfiguration.registerCustomCodecs();

        try
        {
            try
            {
                dataSource.connect();
            }
            //Thrown when operation is not supported by the OS
            catch (NullPointerException ex)
            {
                logger.error(
                    "An internal error occurred while"
                        + " trying to connec to the datasource!"
                    , ex);
                throw new MediaException(
                    "An internal error occurred while"
                        + " trying to connec to the datasource!"
                    , MediaException.INTERNAL_ERROR
                    , ex);
            }

            // 1. Changing buffer size. The default buffer size (for javasound)
            // is 125 milliseconds - 1/8 sec. On MacOS this leads to exception and
            // no audio capture. 30 value of buffer fix the problem and is ok
            // when using some pstn gateways
            // 2. Changing to 60. When it is 30 there are some issues
            // with asterisk and nat(we don't start to send stream and so
            // asterisk rtp part doesn't notice that we are behind nat)
            // 3. Do not set buffer length on linux as it completely breaks
            // audio capture.
            Control ctl = (Control)
                dataSource.getControl("javax.media.control.BufferControl");

            if(ctl != null)
            {
                if(!OSUtils.IS_LINUX)
                    ((BufferControl)ctl).setBufferLength(60);//buffers in ms
            }

            sourceProcessor = Manager.createProcessor(dataSource);

            if (!processorUtility.waitForState(sourceProcessor,
                                               Processor.Configured))
            {
                throw new MediaException(
                    "Media manager could not configure processor\n"
                        + "for the specified data source",
                    MediaException.INTERNAL_ERROR);
            }

        }
        catch (NoProcessorException ex)
        {
            logger.error(
                "Media manager could not create a processor\n"
                    + "for the specified data source"
                , ex
                );
            throw new MediaException(
                "Media manager could not create a processor\n"
                    + "for the specified data source"
                , MediaException.INTERNAL_ERROR
                , ex);
        }
        catch (IOException ex)
        {
            logger.error(
                "Media manager could not connect "
                    + "to the specified data source"
                , ex);
            throw new MediaException("Media manager could not connect "
                                         + "to the specified data source"
                                     , MediaException.INTERNAL_ERROR
                                     , ex);
        }
        sourceProcessor.setContentDescriptor(
            new ContentDescriptor(ContentDescriptor.RAW_RTP));

        /*
         * The lists of the supported audio and video encodings will have to be
         * calculated again in order to get them up-to-date with the current
         * sourceProcessor.
         */
        synchronized (supportedEncodingsAreCalculated)
        {
            supportedEncodingsAreCalculated[0] = false;
        }
    }

    /**
     * Calculates the audio and video encodings supported by the current
     * {@link #sourceProcessor}.
     */
    private void calculateSupportedEncodings()
    {
        //check out the formats that our processor supports and update our
        //supported formats arrays.
        TrackControl[] trackControls = sourceProcessor.getTrackControls();
        if (logger.isDebugEnabled())
            logger.debug("We will be able to transmit in:");
        List<String> transmittableAudioEncodings = new ArrayList<String>();
        List<String> transmittableVideoEncodings = new ArrayList<String>();

        for (TrackControl trackControl : trackControls)
        {
            Format[] formats = trackControl.getSupportedFormats();
            for (int j = 0; j < formats.length; j++)
            {
                Format format = formats[j];
                String encoding = format.getEncoding();

                int sdpInt = MediaUtils.jmfToSdpEncoding(encoding);
                if (sdpInt != MediaUtils.UNKNOWN_ENCODING)
                {
                    String sdp = String.valueOf(sdpInt);

                    if (format instanceof AudioFormat)
                    {
                        if (!transmittableAudioEncodings.contains(sdp))
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Audio=[" + (j + 1) + "]="
                                    + encoding + "; sdp=" + sdp);
                            }
                            transmittableAudioEncodings.add(sdp);
                        }
                    }
                    else if (format instanceof VideoFormat)
                    {
                        if (!transmittableVideoEncodings.contains(sdp))
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Video=[" + (j + 1) + "]="
                                    + encoding + "; sdp=" + sdp);
                            }
                            transmittableVideoEncodings.add(sdp);
                        }
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug("unknown encoding format " + encoding);
                }
            }
        }

        //now update the supported encodings arrays.
        final int transmittableAudioEncodingCount =
            transmittableAudioEncodings.size();
        if (transmittableAudioEncodingCount > 0)
        {
            supportedAudioEncodings =
                encodingConfiguration.updateEncodings(transmittableAudioEncodings);
        }
        else
        {
            //just leave supportedAudioEncodings as  it was in the beginning
            //as it will be only receiving so it could say it supports
            //everything.
            supportedAudioEncodings =
                encodingConfiguration.getSupportedAudioEncodings();
        }

        final int transmittableVideoEncodingCount =
            transmittableVideoEncodings.size();
        if (transmittableVideoEncodingCount > 0)
        {
            supportedVideoEncodings =
                encodingConfiguration.updateEncodings(transmittableVideoEncodings);
        }
        else
        {
            //just leave supportedVideoEncodings as  it was in the beginning
            //as it will be only receiving so it could say it supports
            //everything.
            supportedVideoEncodings =
                encodingConfiguration.getSupportedVideoEncodings();
        }
    }

    /**
     * Ensures {@link #supportedAudioEncodings} and
     * {@link #supportedVideoEncodings} are up-to-date with the current
     * {@link #sourceProcessor} i.e. calculates them if necessary.
     */
    private void ensureSupportedEncodingsAreCalculated()
    {
        synchronized (supportedEncodingsAreCalculated)
        {
            if (!supportedEncodingsAreCalculated[0])
            {
                if (sourceProcessor != null)
                {
                    calculateSupportedEncodings();
                }
                supportedEncodingsAreCalculated[0] = true;
            }
        }
    }

    /**
     * Closes all currently used capture devices and data sources so that they
     * would be usable by other applications.
     *
     * @throws MediaException if closing the devices fails with an IO
     * Exception.
     */
    public void closeCaptureDevices()
        throws MediaException
    {
        try {
            if(avDataSource != null)
                avDataSource.stop();
        } catch (IOException exc) {
            logger.error("Failed to close a capture data source.", exc);
            throw new MediaException("Failed to close a capture data source."
                                     , MediaException.INTERNAL_ERROR
                                     , exc);
        }
    }

    /**
     * Returns a JMF DataSource object over the device that <tt>locator</tt>
     * points to.
     * @param locator the MediaLocator of the device/movie that we'd like to
     * transmit from.
     * @return a connected <tt>DataSource</tt> for the media specified by the
     * locator.
     */
    public static DataSource createDataSource(MediaLocator locator)
    {
        try {
            if (logger.isInfoEnabled())
                logger.info("Creating datasource for:"
                    + ((locator != null)
                        ? locator.toExternalForm()
                        : "null"));
            return Manager.createDataSource(locator);
        }
        catch (NoDataSourceException ex) {
            // The failure only concens us
            logger.error("Could not create data source for " +
                    ((locator != null)
                            ? locator.toExternalForm()
                            : "null")
                    , ex);
            return null;
        }
        catch (IOException ex) {
            // The failure only concerns us
            logger.error("Could not create data source for " +
                    ((locator != null)
                            ? locator.toExternalForm()
                            : "null")
                    , ex);
            return null;
        }
    }

    /**
     * Creates a processing data source using the <tt>encodingSets</tt> map to
     * determine the formats/encodings allowed for the various media types.
     *
     * @param encodingSets a hashtable mapping media types such as "audio" or
     *            "video" to <tt>List</tt>a of encodings (ordered by preference)
     *            accepted for the corresponding type.
     *
     * @return a processing data source set to generate flows in the encodings
     *         specified by the encodingSets map.
     *
     * @throws MediaException if creating the data source fails for some reason.
     */
    public DataSource createDataSourceForEncodings(
        Map<String, List<String>> encodingSets)
        throws MediaException
    {
        if (sourceProcessor == null)
        {
            logger.error("Processor is null.");
            throw new MediaException("The source Processor has not been "
                                         + "initialized."
                                     , MediaException.INTERNAL_ERROR);
        }
        // Wait for the sourceProcessor to configure
        if (sourceProcessor.getState() < Processor.Configured)
        {
            if (!processorUtility.waitForState(sourceProcessor,
                    Processor.Configured))
            {
                logger.error("Couldn't configure sourceProcessor");
                throw new MediaException("Couldn't configure sourceProcessor"
                                         , MediaException.INTERNAL_ERROR);
            }
        }
        // Get the tracks from the sourceProcessor
        TrackControl[] tracks = sourceProcessor.getTrackControls();
        // Do we have at least one track?
        if ((tracks == null) || (tracks.length < 1))
        {
            logger.error("Couldn't find any tracks in sourceProcessor");
            throw new MediaException(
                "Couldn't find any tracks in sourceProcessor"
                , MediaException.INTERNAL_ERROR);
        }
        if (    logger.isDebugEnabled()
             && (sourceProcessor.getState() > Processor.Configured))
            if (logger.isDebugEnabled())
                logger.debug(
                "sourceProcessor is in state "
                    + sourceProcessor.getState()
                    + " which is > Processor.Configured"
                    + " and then TrackControl.setFormat(Format) may not work.");
        boolean atLeastOneTrack = false;
        // Go through all the tracks in our device processor (we would normally
        // have one per device or in other words one for video and another one
        // for audio) processor and set them to transmit in whatever format we
        // promised via sdp.
        for (int i = 0; i < tracks.length; i++)
        {
            if (!tracks[i].isEnabled())
            {
                tracks[i].setEnabled(false);//is this really necessary?
                continue;
            }
            Format[] supported = tracks[i].getSupportedFormats();
            if (supported.length == 0)
            {
                if (logger.isDebugEnabled())
                    logger.debug("No available encodings.");
                tracks[i].setEnabled(false);
                continue;
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Available encodings are:");
                for (int j = 0; j < supported.length; j++)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("track[" + i + "] format[" + j + "]="
                                 + supported[j].getEncoding());
                }
            }

            // We've set the output content to the RAW_RTP.
            // So all the supported formats should work with RTP.
            // We'll pick one that matches those specified by the
            // constructor.
            if (supported[0] instanceof VideoFormat)
            {
                // For video formats, we should double check the
                // sizes since not all formats work in all sizes.
                int index = findFirstMatchingFormat(supported,encodingSets);
                if (index != -1)
                {
                    Format chosenFormat = assertSize(
                        (VideoFormat)supported[index]);

                    tracks[i].setFormat(chosenFormat);
                    if (logger.isDebugEnabled())
                        logger.debug("Track " + i + " is set to transmit "
                                 + "as: " + chosenFormat);
                    atLeastOneTrack = true;
                }
                else
                {
                    tracks[i].setEnabled(false);
                }
            }
            else
            {
                if (FMJConditionals.FORCE_AUDIO_FORMAT != null)
                {
                    tracks[i].setFormat(FMJConditionals.FORCE_AUDIO_FORMAT);
                    atLeastOneTrack = true;
                }
                else
                {
                    int index = findFirstMatchingFormat(supported,encodingSets);
                    if (index != -1)
                    {
                        Format setFormat
                            = tracks[i].setFormat(supported[index]);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug( "Track " + i
                                 + " is set to transmit as: " + setFormat);
                        }
                        atLeastOneTrack = true;
                    }
                    else
                    {
                        tracks[i].setEnabled(false);
                    }
                }
            }
        }
        if (!atLeastOneTrack)
        {
            logger.error(
                "Couldn't set any of the tracks to a valid RTP format");
            throw new MediaException(
                "Couldn't set any of the tracks to a valid RTP format"
                , MediaException.INTERNAL_ERROR);
        }
        // Realize the sourceProcessor. This will internally create a flow
        // graph and attempt to create an output datasource
        if (!processorUtility.waitForState(sourceProcessor,
                Controller.Realized))
        {
            logger.error("Couldn't realize sourceProcessor");
            throw new MediaException("Couldn't realize sourceProcessor"
                                     , MediaException.INTERNAL_ERROR);
        }
        // Set the JPEG quality.
        /** @todo set JPEG quality through a property */
        setJpegQuality(sourceProcessor, 1f);
        // Get the output data source of the sourceProcessor
        return sourceProcessor.getDataOutput();
    }

    /**
     * Setting the encoding quality to the specified value on the JPEG encoder.
     * 0.5 is a good default.
     *
     * @param player the player that we're setting the quality on.
     * @param val a float between 0 (for minimum quality) and 1 (for maximum
     * quality).
     */
    private void setJpegQuality(Player player, float val)
    {
        if ((player == null)
                || (player.getState() < Player.Realized))
            return;
        Control cs[] = player.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);
        // Loop through the controls to find the Quality control for
        // the JPEG encoder.
        for (Control c : cs)
        {
            if (c instanceof QualityControl && c instanceof Owned)
            {
                Object owner = ((Owned) c).getOwner();
                // Check to see if the owner is a Codec.
                // Then check for the output format.
                if (owner instanceof Codec)
                {
                    Format fmts[] = ((Codec) owner)
                        .getSupportedOutputFormats(null);
                    for (Format fmt : fmts)
                    {
                        if (fmt.matches(jpegFmt))
                        {
                            qc = (QualityControl) c;
                            qc.setQuality(val);
                            if (logger.isDebugEnabled())
                                logger.debug("Setting quality to "
                                         + val + " on " + qc);
                            break;
                        }
                    }
                }
                if (qc != null)
                {
                    break;
                }
            }
        }
    }

    /**
     * For JPEG and H263, we know that they only work for particular
     * sizes.  So we'll perform extra checking here to make sure they
     * are of the right sizes.
     *
     * @param sourceFormat the original format that we'd like to check for
     * size.
     *
     * @return the modified <tt>VideoFormat</tt> set to the size we support.
     */
    private VideoFormat assertSize(VideoFormat sourceFormat)
    {
        int width, height;

        // JPEG
        if (sourceFormat.matches(new Format(VideoFormat.JPEG_RTP)))
        {
            Dimension size = sourceFormat.getSize();

            // For JPEG, make sure width and height are divisible by 8.
            width = (size.width % 8 == 0)
                ? size.width
                : ( ( (size.width / 8)) * 8);
            height = (size.height % 8 == 0)
                ? size.height
                : (size.height / 8) * 8;
        }
        // H.263
        else if (sourceFormat.matches(new Format(VideoFormat.H263_RTP)))
        {
            // For H.263, we only support some specific sizes.
            //if (size.width < 128)
//            {
//                width = 128;
//                height = 96;
//            }
            //else if (size.width < 176)
//            {
//                width = 176;
//                height = 144;
//            }
            //else
//            {
                width = 352;
                height = 288;

//            }
        }
        else
        {
            // We don't know this particular format.  We'll just
            // leave it alone then.
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
     * Looks for the first encoding (among the requested encodings elements)
     * that is also present in the <tt>availableFormats</tt> array and returns
     * the index of the corresponding <tt>Format</tt>.
     *
     * @param availableFormats an array of JMF <tt>Format</tt>s that we're
     *            currently able to transmit.
     * @param requestedEncodings a table mapping media types (e.g. audio or
     *            video) to a list of encodings that our interlocutor has sent
     *            in order of preference.
     *
     * @return the index of the format corresponding to the first encoding that
     *         had a marching format in the <tt>availableFormats</tt> array.
     */
    protected int findFirstMatchingFormat(
            Format[] availableFormats,
            Map<String, List<String>> requestedEncodings)
    {
        if (availableFormats == null || requestedEncodings == null)
        {
            return -1;
        }

        for (List<String> requestedEncodingSet : requestedEncodings.values())
        {
            for (String requestedEncoding : requestedEncodingSet)
            {
                for (int i = 0; i < availableFormats.length; i++)
                {
                    String availableEncoding = availableFormats[i].getEncoding();

                    if (availableEncoding.equals(requestedEncoding))
                    {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns an array of Strings containing video formats in the order of
     * preference.
     * @return an array of Strings containing video formats in the order of
     * preference.
     */
    public String[] getSupportedVideoEncodings()
    {
        ensureSupportedEncodingsAreCalculated();
        return this.supportedVideoEncodings;
    }

    /**
     * Returns an array of Strings containing audio formats in the order of
     * preference.
     * @return an array of Strings containing audio formats in the order of
     * preference.
     */
    public String[] getSupportedAudioEncodings()
    {
        ensureSupportedEncodingsAreCalculated();
        return this.supportedAudioEncodings;
    }

    /**
     * Starts reading media from the source data sources. If someone is
     * already reading, then simply add the reader to the list of readers so
     * that we don't pull the plug from underneath their feet.
     *
     * @param reader a reference to the object calling this method, that we
     * could use for keeping the number of simultaneous active readers.
     */
    public void startProcessingMedia(Object reader)
    {
        if( sourceProcessor.getState() !=  Processor.Started )
            sourceProcessor.start();

        if(!processorReaders.contains(reader))
            processorReaders.add(reader);
    }

    /**
     * Stops reading media from the source data sources. If there is someone
     * else still reading, then we simply remove the local reference to the
     * reader and wait for the last reader to call stopProcessing before we
     * really stop the processor.
     *
     * @param reader a reference to the object calling this method, that we
     * could use for keeping the number of simultaneous active readers.
     */
    public void stopProcessingMedia(Object reader)
    {
        if(sourceProcessor == null)
            return;

        if( sourceProcessor.getState() ==  Processor.Started )
        {
            try
            {
                initCaptureDevices();
            }
            catch (Exception e)
            {
                logger.error("Error initing media processor.", e);
            }
        }

        processorReaders.remove(reader);
    }

    /**
     * Determines whether the audio of this instance is mute.
     *
     * @return <tt>true</tt> if the audio of this instance is mute; otherwise,
     *         <tt>false</tt>
     */
    public boolean isMute()
    {
        return (muteAudioDataSource != null) && muteAudioDataSource.isMute();
    }

    /**
     * Sets the mute state of the audio of this instance.
     *
     * @param mute <tt>true</tt> to mute the audio of this instance;
     *            <tt>false</tt>, otherwise
     */
    public void setMute(boolean mute)
    {
        if (muteAudioDataSource != null)
            muteAudioDataSource.setMute(mute);
    }

    /**
     * Selects the nearest size supported by the capture device, to make drivers
     * scale the images.
     *
     * @param videoDS the video <code>DataSource</code>
     */
    private void selectVideoSize(DataSource videoDS)
    {
        selectVideoSize(videoDS, Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);
    }

    public static Dimension selectVideoSize(DataSource videoDS,
        final int preferredWidth, final int preferredHeight)
    {
        if(videoDS == null)
            return null;

        FormatControl formatControl =
            (FormatControl) videoDS.getControl(FormatControl.class.getName());

        if (formatControl == null)
            return null;

        Format[] formats = formatControl.getSupportedFormats();
        final int count = formats.length;

        if (count < 1)
            return null;

        Format selectedFormat = null;

        if (count == 1)
            selectedFormat = formats[0];
        else
        {
            class FormatInfo
            {
                public final VideoFormat format;

                public final double difference;

                public FormatInfo(VideoFormat format)
                {
                    this.format = format;

                    Dimension size = format.getSize();

                    int width = size.width;
                    double xScale =
                        (width == preferredWidth)
                            ? 1
                            : (preferredWidth / (double) width);

                    int height = size.height;
                    double yScale =
                        (height == preferredHeight)
                            ? 1
                            : (preferredHeight / (double) height);

                    difference = Math.abs(1 - Math.min(xScale, yScale));
                }
            }

            FormatInfo[] infos = new FormatInfo[count];

            for (int i = 0; i < count; i++)
            {
                FormatInfo info =
                    infos[i] = new FormatInfo((VideoFormat) formats[i]);

                if (info.difference == 0)
                {
                    selectedFormat = info.format;
                    break;
                }
            }
            if (selectedFormat == null)
            {
                Arrays.sort(infos, new Comparator<FormatInfo>()
                {
                    public int compare(FormatInfo info0, FormatInfo info1)
                    {
                        return Double.compare(info0.difference,
                            info1.difference);
                    }
                });
                selectedFormat = infos[0].format;
            }
        }

        formatControl.setFormat(selectedFormat);
        return ((VideoFormat) selectedFormat).getSize();
    }

    /**
     * Sets the <code>DataSource</code> to be used by this instance to capture
     * video. The <code>DataSource</code> is to be provided in the form of a
     * <code>SourceCloneable</code> so that it can give access to the local
     * video both as a stand-alone <code>DataSource</code> and a merged one with
     * audio. If the setting changes the state of this instance, registered
     * <code>PropertyChangeListener</code>s are notified about the change of the
     * value of the property {@link #VIDEO_DATA_SOURCE}.
     *
     * @param videoDataSource a <code>SourceCloneable</code> representing the
     *            <code>DataSource</code> to be used by this instance to capture
     *            video.
     */
    private void setVideoDataSource(SourceCloneable videoDataSource)
    {
        Object oldValue = this.videoDataSource;

        this.videoDataSource = videoDataSource;

        firePropertyChange(VIDEO_DATA_SOURCE, oldValue, this.videoDataSource);
    }

    /**
     * Creates a <code>DataSource</code> which gives access to the local video
     * this instance captures and controls.
     *
     * @return a <code>DataSource</code> which gives access to the local video
     *         this instance captures and controls; <tt>null</tt> if video is
     *         not utilized by this instance
     */
    public DataSource createLocalVideoDataSource()
    {
        return (videoDataSource == null)
                ? null
                : videoDataSource.createClone();
    }

    /**
     * Sets the indicator which determines whether the streaming of local video
     * through this <code>MediaControl</code> is allowed. The setting does not
     * reflect the availability of actual video capture devices, it just
     * expresses the desire of the user to have the local video streamed in the
     * case the system is actually able to do so.
     *
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     *            this <code>MediaControl</code>; <tt>false</tt> to disallow it
     */
    public void setLocalVideoAllowed(boolean allowed)
        throws MediaException
    {
        if (localVideoAllowed != allowed)
        {
            localVideoAllowed = allowed;

            initCaptureDevices();
        }
    }

    /**
     * Gets the indicator which determines whether the streaming of local video
     * through this <code>MediaControl</code> is allowed. The setting does not
     * reflect the availability of actual video capture devices, it just
     * expresses the desire of the user to have the local video streamed in the
     * case the system is actually able to do so.
     *
     * @return <tt>true</tt> if the streaming of local video for this
     *         <code>MediaControl</code> is allowed; <tt>false</tt>, otherwise
     */
    public boolean isLocalVideoAllowed()
    {
        return localVideoAllowed;
    }
}
