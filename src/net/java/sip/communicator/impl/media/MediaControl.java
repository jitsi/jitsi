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
import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.sdp.*;

import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.media.MediaException;
import net.java.sip.communicator.util.*;

/**
 * This class is intended to provide a generic way to control media package.
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
{
    private final Logger logger = Logger.getLogger(MediaControl.class);

    /**
     * Our configuration helper.
     */
    private DeviceConfiguration deviceConfiguration = null;

    /**
     * A data source merging our audio and video data sources.
     */
    private DataSource avDataSource = null;

    /**
     * The audio <tt>DataSource</tt> which provides mute support.
     */
    private MutePushBufferDataSource muteAudioDataSource;

    /**
     * SDP Codes of all video formats that JMF supports.
     */
    private String[] supportedVideoEncodings = new String[]
        {
            // javax.media.format.VideoFormat.H263_RTP
            Integer.toString(SdpConstants.H263),
            // javax.media.format.VideoFormat.JPEG_RTP
            Integer.toString(SdpConstants.JPEG),
            // javax.media.format.VideoFormat.H261_RTP
            Integer.toString(SdpConstants.H261)
        };

    /**
     * SDP Codes of all audio formats that JMF supports.
     */
    private String[] supportedAudioEncodings = new String[]
        {
            // ILBC
            Integer.toString(97), 
            // javax.media.format.AudioFormat.G723_RTP
            Integer.toString(SdpConstants.G723),
            // javax.media.format.AudioFormat.GSM_RTP;
            Integer.toString(SdpConstants.GSM),
            // javax.media.format.AudioFormat.ULAW_RTP;
            Integer.toString(SdpConstants.PCMU),
            // javax.media.format.AudioFormat.DVI_RTP;
            Integer.toString(SdpConstants.DVI4_8000),
            // javax.media.format.AudioFormat.DVI_RTP;
            Integer.toString(SdpConstants.DVI4_16000),
            // javax.media.format.AudioFormat.ALAW;
            Integer.toString(SdpConstants.PCMA),
            Integer.toString(110),
            // javax.media.format.AudioFormat.G728_RTP;
            Integer.toString(SdpConstants.G728)
            // javax.media.format.AudioFormat.G729_RTP
            // g729 is not suppported by JMF
            //Integer.toString(SdpConstants.G729)
        };

    /**
     * The indicator which determines whether {@link #supportedAudioEncodings}
     * and {@link #supportedVideoEncodings} are already calculated to be
     * up-to-date with the current {@link #sourceProcessor} and the lock to
     * synchronize the access to the mentioned calculation.
     */
    private final boolean[] supportedEncodingsAreCalculated = new boolean[1];

    private static final String PROP_SDP_PREFERENCE
                            = "net.java.sip.communicator.impl.media.sdppref";

    /**
     * That's where we keep format preferences matching SDP formats to integers.
     * We keep preferences for both audio and video formats here in case we'd
     * ever need to compare them to one another. In most cases however both
     * would be decorelated and other components (such as the UI) should present
     * them separately.
     */
    private final Hashtable<String, Integer> encodingPreferences =
        new Hashtable<String, Integer>();

    /**
     * The processor that will be handling content coming from our capture data
     * sources.
     */
    private Processor sourceProcessor = null;

    /**
     * The list of readers currently using our processor.
     */
    private Vector processorReaders = new Vector();

    /**
     * An object that we use for.
     */
    private ProcessorUtility processorUtility = new ProcessorUtility();

    /**
     * The name of the property that could contain the name of a media file
     * to use instead of capture devices.
     */
    private static final String DEBUG_DATA_SOURCE_URL_PROPERTY_NAME
      = "net.java.sip.communicator.impl.media.DEBUG_DATA_SOURCE_URL";

    /**
     *
     */
    private static String[] customCodecs = new String[]
    {
    	FMJConditionals.FMJ_CODECS
    	   ? "net.sf.fmj.media.codec.audio.alaw.Encoder"
           : "net.java.sip.communicator.impl.media.codec.audio.alaw.JavaEncoder",
       	FMJConditionals.FMJ_CODECS
           ? "net.sf.fmj.media.codec.audio.alaw.DePacketizer"
           : "net.java.sip.communicator.impl.media.codec.audio.alaw.DePacketizer",
        FMJConditionals.FMJ_CODECS 
           ? "net.sf.fmj.media.codec.audio.alaw.Packetizer" 
           : "net.java.sip.communicator.impl.media.codec.audio.alaw.Packetizer",
        FMJConditionals.FMJ_CODECS 
           ? "net.sf.fmj.media.codec.audio.ulaw.Packetizer" 
           : "net.java.sip.communicator.impl.media.codec.audio.ulaw.Packetizer",
        "net.java.sip.communicator.impl.media.codec.audio.speex.JavaEncoder",
        "net.java.sip.communicator.impl.media.codec.audio.speex.JavaDecoder",
        "net.java.sip.communicator.impl.media.codec.audio.ilbc.JavaEncoder",
        "net.java.sip.communicator.impl.media.codec.audio.ilbc.JavaDecoder"
//        "net.java.sip.communicator.impl.media.codec.audio.g729.JavaDecoder",
//        "net.java.sip.communicator.impl.media.codec.audio.g729.JavaEncoder",
//        "net.java.sip.communicator.impl.media.codec.audio.g729.DePacketizer",
//        "net.java.sip.communicator.impl.media.codec.audio.g729.Packetizer"
    };

    /**
     * Custom Packages provided by Sip-Communicator
     */
    private static String[] customPackages = new String[]
    {    // datasource for low latency ALSA input
    "net.java.sip.communicator.impl"
    };

    /**
     * The default constructor.
     */
    public MediaControl()
    {
    }

    /** 
     * Returns the duration of the output data source. Usually this will be 
     * DURATION_UNKNOWN, but if the current data source is set to an audio
     * file, then this value will be of some use.
     * @return the output duration
     */
    public javax.media.Time getOutputDuration()
    {
        if (sourceProcessor == null)
            return Duration.DURATION_UNKNOWN;
        else return sourceProcessor.getDuration();
    }

    /**
     * Initializes the media control.
     *
     * @param deviceConfig the <tt>DeviceConfiguration</tt> that we should use
     * when retrieving device handlers.
     *
     * @throws MediaException if initialization fails.
     */
    public void initialize(DeviceConfiguration deviceConfig)
        throws MediaException
    {
        this.deviceConfiguration = deviceConfig;
        initializeFormatPreferences();

        // register our own datasources
        registerCustomPackages();

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
    }

    /**
     * Retrieves (from the configuration service) preferences specified for
     * various formats and assigns default ones to those that haven't been
     * mentioned.
     */
    private void initializeFormatPreferences()
    {
        //first init default preferences
        //video
        setEncodingPreference(SdpConstants.H263,      1000);
        setEncodingPreference(SdpConstants.JPEG,       950);
        setEncodingPreference(SdpConstants.H261,       800);

        //audio
        setEncodingPreference(SdpConstants.PCMU,       650);
        setEncodingPreference(SdpConstants.PCMA,       600);
        setEncodingPreference(97,                      500);
        setEncodingPreference(SdpConstants.GSM,        450);
        setEncodingPreference(110,                     350);
        setEncodingPreference(SdpConstants.DVI4_8000,  300);
        setEncodingPreference(SdpConstants.DVI4_16000, 250);
        setEncodingPreference(SdpConstants.G723,       150);
        setEncodingPreference(SdpConstants.G728,       100);

        //now override with those that are specified by the user.
        ConfigurationService confService
            = MediaActivator.getConfigurationService();

        List sdpPreferences = confService.getPropertyNamesByPrefix(
                        PROP_SDP_PREFERENCE, false);
        Iterator sdpPreferencesIter = sdpPreferences.iterator();
        while(sdpPreferencesIter.hasNext())
        {
            String pName = (String)sdpPreferencesIter.next();
            String prefStr = confService.getString(pName);
            String fmtName = pName.substring(pName.lastIndexOf('.'));
            int    preference = -1;
            int    fmt = -1;


            try
            {
                preference = Integer.parseInt(prefStr);
                fmt = Integer.parseInt(fmtName);
            }
            catch (NumberFormatException exc)
            {
                logger.warn("Failed to parse format ("
                            + fmtName +") or preference("
                            + prefStr + ").", exc);
                continue;
            }

            setEncodingPreference(fmt, preference);

            //now sort the arrays so that they are returned by order of
            //preference.
            sortEncodingsArray( this.supportedAudioEncodings);
            sortEncodingsArray( this.supportedVideoEncodings);
        }
    }

    /**
     * Compares the two formats for order.  Returns a negative integer,
     * zero, or a positive integer as the first format has been assigned a
     * preference higher, equal to, or greater than the one of the second.<p>
     *
     * @param enc1 the first format to compare for preference.
     * @param enc2 the second format to compare for preference.
     *
     * @return a negative integer, zero, or a positive integer as the first
     * format has been assigned a preference higher, equal to, or greater than
     * the one of the second.
     */
    private int compareEncodingPreferences(String enc1, String enc2)
    {
        Integer pref1 = encodingPreferences.get(enc1);
        int pref1IntValue = (pref1 == null) ? 0 : pref1.intValue();

        Integer pref2 = encodingPreferences.get(enc2);
        int pref2IntValue = (pref2 == null) ? 0 : pref2.intValue();

        return pref2IntValue - pref1IntValue;
    }

    /**
     * Sorts the <tt>encodingsArray</tt> according to user specified
     * preferences.
     *
     * @param encodingsArray the array of encodings that we'd like to sort
     * according to encoding preferences specifies by the user.
     */
    private void sortEncodingsArray(String[] encodingsArray)
    {
        Arrays.sort( encodingsArray, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                return compareEncodingPreferences((String)o1, (String)o2);
            }
        } );
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     *
     * @param encoding the SDP int of the encoding whose pref we're setting.
     * @param pref a positive int indicating the preference for that encoding.
     */
    private void setEncodingPreference(int encoding, int pref)
    {
        setEncodingPreference(Integer.toString(encoding), new Integer(pref));
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     *
     * @param encoding a string containing the SDP int of the encoding whose 
     * pref we're setting.
     * @param pref a positive int indicating the preference for that encoding.
     */
    private void setEncodingPreference(String encoding, Integer pref)
    {
        this.encodingPreferences.put(encoding, pref);
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
        // Init Capture devices
        DataSource audioDataSource = null;
        DataSource videoDataSource = null;
        CaptureDeviceInfo audioDeviceInfo = null;
        CaptureDeviceInfo videoDeviceInfo = null;

        // audio device
        audioDeviceInfo = deviceConfiguration.getAudioCaptureDevice();
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

        // video device
        videoDeviceInfo = deviceConfiguration.getVideoCaptureDevice();
        if (videoDeviceInfo != null)
        {
            videoDataSource = createDataSource(videoDeviceInfo.getLocator());
        }

        // Create the av data source
        if (audioDataSource != null && videoDataSource != null)
        {
            DataSource[] allDS = new DataSource[] {
                    audioDataSource,
                    videoDataSource
            };
            try
            {
                avDataSource = Manager.createMergingDataSource(allDS);
            }
            catch (IncompatibleSourceException exc)
            {
                logger.fatal(
                        "Failed to create a media data source!"
                        + "Media transmission won't be enabled!", exc);
                throw new InternalError("Failed to create a media data source!"
                        + "Media transmission won't be enabled!"
                        + exc.getMessage());
            }
        }
        else
        {
            if (audioDataSource != null)
            {
                avDataSource = audioDataSource;
            }
            if (videoDataSource != null)
            {
                avDataSource = videoDataSource;
            }
        }

        //avDataSource may be null (Bug report Vince Fourcade)
        if (avDataSource != null)
        {
            initProcessor(avDataSource);
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
            URL url = new URL(debugMediaSource);
            initDataSourceFromURL(url);
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
        logger.debug("Using a data source from url: " + dataSourceURL);
        MediaLocator locator = new MediaLocator(dataSourceURL);

        avDataSource = createDataSource(locator);

        //avDataSource may be null (Bug report Vince Fourcade)
        if (avDataSource != null)
        {
            initProcessor(avDataSource);
        }
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
        // register our custom codecs
        registerCustomCodecs();

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
                    + " trying to connec to to datasource!"
                    , ex);
                throw new MediaException(
                    "An internal error occurred while"
                    + " trying to connec to to datasource!"
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
                if(!System.getProperty("os.name")
                    .toLowerCase().contains("linux"))
                {
                    ((BufferControl)ctl).setBufferLength(60);//buffers in ms
                }
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
        sourceProcessor.setContentDescriptor(new ContentDescriptor(
            ContentDescriptor.RAW_RTP));

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
        logger.debug("We will be able to transmit in:");
        List<String> transmittableAudioEncodings = new ArrayList<String>();
        List<String> transmittableVideoEncodings = new ArrayList<String>();

        for (int i = 0; i < trackControls.length; i++)
        {
            Format[] formats = trackControls[i].getSupportedFormats();
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
                transmittableAudioEncodings
                    .toArray(new String[transmittableAudioEncodingCount]);

            // sort the supported encodings according to user preferences.
            this.sortEncodingsArray(supportedAudioEncodings);
        }
        //else
        {
            //just leave supportedAudioEncodings as  it was in the beginning
            //as it will be only receiving so it could say it supports
            //everything.
        }

        final int transmittableVideoEncodingCount =
            transmittableVideoEncodings.size();
        if (transmittableVideoEncodingCount > 0)
        {
            supportedVideoEncodings =
                transmittableVideoEncodings
                    .toArray(new String[transmittableVideoEncodingCount]);

            // sort the supported encodings according to user preferences.
            this.sortEncodingsArray(supportedVideoEncodings);
        }
        //else
        {
            //just leave supportedVideoEncodings as  it was in the beginning
            //as it will be only receiving so it could say it supports
            //everything.
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
            logger.error("Failed to close a capture date source.", exc);
            throw new MediaException("Failed to close a capture date source."
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
    private DataSource createDataSource(MediaLocator locator)
    {
        try {
            logger.info("Creating datasource for:"
                    + ((locator != null)
                        ? locator.toExternalForm()
                        : "null"));
            return Manager.createDataSource(locator);
        }
        catch (NoDataSourceException ex) {
            // The failure only concens us
            logger.error("Could not create data source for " +
                    locator.toExternalForm()
                    , ex);
            return null;
        }
        catch (IOException ex) {
            // The failure only concerns us
            logger.error("Could not create data source for " +
                    locator.toExternalForm()
                    , ex);
            return null;
        }
    }

    /**
     * Creates a processing data source using the <tt>encodingSets</tt> map
     * to determine the formats/encodings allowed for the various media types.
     *
     * @param encodingSets a hashtable mapping media types such as "audio" or
     * "video" to <tt>List</tt>a of encodings (ordered by preference) accepted
     * for the corresponding type.
     *
     * @return a processing data source set to generate flows in the encodings
     * specified by the encodingSets map.
     *
     * @throws MediaException if creating the data source fails for some reason.
     */
    public DataSource createDataSourceForEncodings(Hashtable encodingSets)
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
        boolean processorIsReady = true;
        if (sourceProcessor.getState() < Processor.Configured)
        {
            processorIsReady = processorUtility
                .waitForState(sourceProcessor, Processor.Configured);
        }
        if (!processorIsReady)
        {
            logger.error("Couldn't configure sourceProcessor");
            throw new MediaException("Couldn't configure sourceProcessor"
                                     , MediaException.INTERNAL_ERROR);
        }
        // Get the tracks from the sourceProcessor
        TrackControl[] tracks = sourceProcessor.getTrackControls();
        // Do we have atleast one track?
        if (tracks == null || tracks.length < 1)
        {
            logger.error("Couldn't find any tracks in sourceProcessor");
            throw new MediaException(
                "Couldn't find any tracks in sourceProcessor"
                , MediaException.INTERNAL_ERROR);
        }
        // Set the output content descriptor to RAW_RTP
        // This will limit the supported formats reported from
        // Track.getSupportedFormats to only valid RTP formats.
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.
            RAW_RTP);
        sourceProcessor.setContentDescriptor(cd);
        Format supported[];
        Format chosenFormat;
        boolean atLeastOneTrack = false;
        // Program the tracks.
        for (int i = 0; i < tracks.length; i++)
        {
            Format format = tracks[i].getFormat();
            if (tracks[i].isEnabled())
            {
                supported = tracks[i].getSupportedFormats();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Available encodings are:");
                    for (int j = 0; j < supported.length; j++)
                    {
                        logger.debug("track[" + (i + 1) + "] format[" +
                                     (j + 1) + "]="
                                     + supported[j].getEncoding());
                    }
                }
                
                // We've set the output content to the RAW_RTP.
                // So all the supported formats should work with RTP.
                // We'll pick one that matches those specified by the
                // constructor.
                if (supported.length > 0)
                {
                    if (supported[0] instanceof VideoFormat)
                    {
                        // For video formats, we should double check the
                        // sizes since not all formats work in all sizes.
                        int index = findFirstMatchingFormat(supported,
                            encodingSets);
                        if (index != -1)
                        {
                            chosenFormat = assertSize(
                                (VideoFormat)supported[index]);

                            tracks[i].setFormat(chosenFormat);
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
                            int index = findFirstMatchingFormat(supported,
                                encodingSets);
                            if (index != -1)
                            {
                                tracks[i].setFormat(supported[index]);
                                if (logger.isDebugEnabled())
                                {
                                    logger.debug("Track " + i +
                                                 " is set to transmit as: "
                                                 + supported[index]);
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
                else
                {
                    tracks[i].setEnabled(false);
                }
            }
            else
            {
                tracks[i].setEnabled(false);
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
        processorIsReady = processorUtility.waitForState(sourceProcessor
                                               , Controller.Realized);
        if (!processorIsReady)
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
        if (player == null
            || player.getState() < Player.Realized)
            return;
        Control cs[] = player.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);
        // Loop through the controls to find the Quality control for
        // the JPEG encoder.
        for (int i = 0; i < cs.length; i++)
        {
            if (cs[i] instanceof QualityControl && cs[i] instanceof Owned)
            {
                Object owner = ( (Owned) cs[i]).getOwner();
                // Check to see if the owner is a Codec.
                // Then check for the output format.
                if (owner instanceof Codec)
                {
                    Format fmts[] = ( (Codec) owner)
                        .getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++)
                    {
                        if (fmts[j].matches(jpegFmt))
                        {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
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
        Dimension size = sourceFormat.getSize();
        Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
        Format h263Fmt = new Format(VideoFormat.H263_RTP);
        if (sourceFormat.matches(jpegFmt))
        {
            // For JPEG, make sure width and height are divisible by 8.
            width = (size.width % 8 == 0)
                ? size.width
                : ( ( (size.width / 8)) * 8);
            height = (size.height % 8 == 0)
                ? size.height
                : (size.height / 8) * 8;
        }
        else if (sourceFormat.matches(h263Fmt))
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
     * Looks for the first encoding (amont the requested encodings elements)
     * that is also present in the <tt>availableFormats</tt> array and returns
     * the index of the corresponding <tt>Format</tt>.
     *
     * @param availableFormats an array of JMF <tt>Format</tt>s that we're
     * currently able to transmit.
     * @param requestedEncodings a table mapping media types (e.g. audio or
     * video) to a list of encodings that our interlocutor has sent in order of
     * preference.
     *
     * @return the index of the format corresponding to the first encoding that
     * had a marching format in the <tt>availableFormats</tt> array.
     */
    protected int findFirstMatchingFormat(Format[] availableFormats,
                                          Hashtable requestedEncodings)
    {
        if (availableFormats == null || requestedEncodings == null)
        {
            return -1;
        }

        Enumeration formatSets = requestedEncodings.elements();
        while(formatSets.hasMoreElements())
        {
            ArrayList currentSet = (ArrayList) formatSets.nextElement();
            for (int k = 0; k < currentSet.size(); k++)
            {
                for (int i = 0; i < availableFormats.length; i++)
                {
                    if (availableFormats[i].getEncoding()
                        .equals( (String)currentSet.get(k)))
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
     * could use for keeping the number of simulaneous active readers.
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
            sourceProcessor.stop();

            avDataSource.disconnect();
            try
            {
                initProcessor(avDataSource);
            }
            catch (Exception e)
            {
                logger.error("Error initing media processor.", e);
            }
        }

        processorReaders.remove(reader);
    }

    /**
     * Register in JMF the custom codecs we provide
     */
    private void registerCustomCodecs()
    {
        // use a set to check if the codecs are already 
        // registered in jmf.properties
        Set registeredPlugins = new HashSet();
        
        for ( Iterator plugins = PlugInManager
                .getPlugInList( null, 
                                null, 
                                PlugInManager.CODEC).iterator();
              plugins.hasNext(); )
        {
            registeredPlugins.add(plugins.next());
        }
        
        for (int i = 0; i < customCodecs.length; i++)
        {
            String className = customCodecs[i];
            
            if (registeredPlugins.contains(className))
            {
                logger.debug("Codec : " + className + " is already registered");
            }
            else
            {
                try
                {

                    Class pic = Class.forName(className);
                    Object instance = pic.newInstance();

                    boolean result =
                        PlugInManager.addPlugIn(
                            className,
                            ( (Codec) instance).getSupportedInputFormats(),
                            ( (Codec) instance).getSupportedOutputFormats(null),
                            PlugInManager.CODEC);
                    logger.debug("Codec : " + className +
                                 " is succsefully registered : " + result);
                }
                catch (Throwable ex)
                {
                    logger.debug("Codec : " + className +
                                 " is NOT succsefully registered");
                }
            }
        }

        try
        {
            PlugInManager.commit();
        }
        catch (IOException ex)
        {
            logger.error("Cannot commit to PlugInManager", ex);
        }
        
         
        // Register the custom codec formats with the RTP manager once at 
        // initialization. This is needed for the Sun JMF implementation. It 
        // causes the registration of the formats with the static FormatInfo 
        // instance of com.sun.media.rtp.RTPSessionMgr, which in turn makes the
        // formats available when the supported encodings arrays are generated 
        // in initProcessor(). In other JMF implementations this might not be 
        // needed, but should do no harm.
        
        //Commented as it fails to load alaw codec
//        RTPManager rtpManager = RTPManager.newInstance();
//        CallSessionImpl.registerCustomCodecFormats(rtpManager);
//        rtpManager.dispose();
    }


    /**
     * Register in JMF the custom packages we provide
     */
    private void registerCustomPackages()
    {
        Vector currentPackagePrefix = PackageManager.getProtocolPrefixList();

        for (int i = 0; i < customPackages.length; i++)
        {
            String className = customPackages[i];
            
            // linear search in a loop, but it doesn't have to scale since the 
            // list is always short
            if (!currentPackagePrefix.contains(className)) 
            {
                currentPackagePrefix.addElement(className);
                logger.debug("Adding package  : " + className);
            }            
        }

        PackageManager.setProtocolPrefixList(currentPackagePrefix);
        PackageManager.commitProtocolPrefixList();
        logger.debug("Registering new protocol prefix list : " 
                     + currentPackagePrefix);
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
}
