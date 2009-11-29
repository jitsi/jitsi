/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.media.*;
import javax.media.Controls; // disambiguation
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an audio mixer which manages the mixing of multiple audio streams
 * i.e. it is able to output a single audio stream which contains the audio of
 * multiple input audio streams.
 * <p>
 * The input audio streams are provided to the <tt>AudioMixer</tt> through
 * {@link #addInputDataSource(DataSource)} in the form of input
 * <tt>DataSource</tt>s giving access to one or more input
 * <tt>SourceStreams</tt>.
 * </p>
 * <p>
 * The output audio stream representing the mix of the multiple input audio
 * streams is provided by the <tt>AudioMixer</tt> in the form of a
 * <tt>AudioMixingPushBufferDataSource</tt> giving access to a
 * <tt>AudioMixingPushBufferStream</tt>. Such an output is obtained through
 * {@link #createOutputDataSource()}. The <tt>AudioMixer</tt> is able to provide
 * multiple output audio streams at one and the same time, though, each of them
 * containing the mix of a subset of the input audio streams.
 * </p>
 * 
 * @author Lubomir Marinov
 */
public class AudioMixer
{

    /**
     * The <tt>Logger</tt> used by the <tt>AudioMixer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(AudioMixer.class);

    /**
     * The default output <tt>AudioFormat</tt> in which <tt>AudioMixer</tt>,
     * <tt>AudioMixingPushBufferDataSource</tt> and
     * <tt>AudioMixingPushBufferStream</tt> output audio.
     */
    private static final AudioFormat DEFAULT_OUTPUT_FORMAT
        = new AudioFormat(
                AudioFormat.LINEAR,
                8000,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED);

    /**
     * The <tt>CaptureDevice</tt> capabilities provided by the
     * <tt>AudioMixingPushBufferDataSource</tt>s created by this
     * <tt>AudioMixer</tt>. JMF's
     * <tt>Manager.createMergingDataSource(DataSource[])</tt> requires the
     * interface implementation for audio if it is implemented for video and it
     * is indeed the case for our use case of
     * <tt>AudioMixingPushBufferDataSource</tt>.
     */
    protected final CaptureDevice captureDevice;

    /**
     * The number of output <tt>AudioMixingPushBufferDataSource</tt>s reading
     * from this <tt>AudioMixer</tt> which are connected. When the value is
     * greater than zero, this <tt>AudioMixer</tt> is connected to the input
     * <tt>DataSource</tt>s it manages.
     */
    private int connected;

    /**
     * The collection of input <tt>DataSource</tt>s this instance reads audio
     * data from.
     */
    private final List<InputDataSourceDesc> inputDataSources
        = new ArrayList<InputDataSourceDesc>();

    /**
     * The <tt>AudioMixingPushBufferDataSource</tt> which contains the mix of
     * <tt>inputDataSources</tt> excluding <tt>captureDevice</tt> and is thus
     * meant for playback on the local peer in a call.
     */
    private final AudioMixingPushBufferDataSource localOutputDataSource;

    /**
     * The number of output <tt>AudioMixingPushBufferDataSource</tt>s reading
     * from this <tt>AudioMixer</tt> which are started. When the value is
     * greater than zero, this <tt>AudioMixer</tt> is started and so are the
     * input <tt>DataSource</tt>s it manages.
     */
    private int started;

    /**
     * The output <tt>AudioMixerPushBufferStream</tt> through which this
     * instance pushes audio sample data to
     * <tt>AudioMixingPushBufferStream</tt>s to be mixed.
     */
    private AudioMixerPushBufferStream outputStream;

    /**
     * Initializes a new <tt>AudioMixer</tt> instance. Because JMF's
     * <tt>Manager.createMergingDataSource(DataSource[])</tt> requires the
     * implementation of <tt>CaptureDevice</tt> for audio if it is implemented
     * for video and it is indeed the cause for our use case of
     * <tt>AudioMixingPushBufferDataSource</tt>, the new <tt>AudioMixer</tt>
     * instance provides specified <tt>CaptureDevice</tt> capabilities to the
     * <tt>AudioMixingPushBufferDataSource</tt>s it creates. The specified
     * <tt>CaptureDevice</tt> is also added as the first input
     * <tt>DataSource</tt> of the new instance.
     * 
     * @param captureDevice the <tt>CaptureDevice</tt> capabilities to be
     * provided to the <tt>AudioMixingPushBufferDataSource</tt>s created by the
     * new instance and its first input <tt>DataSource</tt>
     */
    public AudioMixer(CaptureDevice captureDevice)
    {
        /*
         * AudioMixer provides PushBufferDataSources so it needs a way to push
         * them. It does the pushing by using the pushes of its CaptureDevice
         * i.e. it has to be a PushBufferDataSource.
         */
        if (captureDevice instanceof PullBufferDataSource)
            captureDevice
                = new PushBufferDataSourceAdapter(
                        (PullBufferDataSource) captureDevice);

        // Try to enable tracing on captureDevice.
        if (logger.isTraceEnabled())
            captureDevice
                = MediaDeviceImpl
                    .createTracingCaptureDevice(captureDevice, logger);

        this.captureDevice = captureDevice;

        this.localOutputDataSource = createOutputDataSource();
        addInputDataSource(
            (DataSource) this.captureDevice,
            this.localOutputDataSource);
    }

    /**
     * Adds a new input <tt>DataSource</tt> to the collection of input
     * <tt>DataSource</tt>s from which this instance reads audio. If the
     * specified <tt>DataSource</tt> indeed provides audio, the respective
     * contributions to the mix are always included.
     * 
     * @param inputDataSource a new <tt>DataSource</tt> to input audio to this
     * instance
     */
    public void addInputDataSource(DataSource inputDataSource)
    {
        addInputDataSource(inputDataSource, null);
    }

    /**
     * Adds a new input <tt>DataSource</tt> to the collection of input
     * <tt>DataSource</tt>s from which this instance reads audio. If the
     * specified <tt>DataSource</tt> indeed provides audio, the respective
     * contributions to the mix will be excluded from the mix output provided
     * through a specific <tt>AudioMixingPushBufferDataSource</tt>.
     * 
     * @param inputDataSource a new <tt>DataSource</tt> to input audio to this
     * instance
     * @param outputDataSource the <tt>AudioMixingPushBufferDataSource</tt> to
     * not include the audio contributions of <tt>inputDataSource</tt> in the
     * mix it outputs
     */
    void addInputDataSource(
            DataSource inputDataSource,
            AudioMixingPushBufferDataSource outputDataSource)
    {
        if (inputDataSource == null)
            throw new IllegalArgumentException("inputDataSource");

        synchronized (inputDataSources)
        {
            for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                if (inputDataSource.equals(inputDataSourceDesc.inputDataSource))
                    throw new IllegalArgumentException("inputDataSource");

            InputDataSourceDesc inputDataSourceDesc
                = new InputDataSourceDesc(
                        inputDataSource,
                        outputDataSource);
            boolean added = inputDataSources.add(inputDataSourceDesc);

            if (added)
            {
                if (logger.isTraceEnabled())
                    logger
                        .trace(
                            "Added input DataSource with hashCode "
                                + inputDataSource.hashCode());

                /*
                 * If the other inputDataSources have already been connected,
                 * connect to the new one as well.
                 */
                if (connected > 0)
                {
                    try
                    {
                        connect(
                            inputDataSourceDesc.getEffectiveInputDataSource(),
                            inputDataSourceDesc.inputDataSource);
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
                }

                // Update outputStream with any new inputStreams.
                if (outputStream != null)
                    getOutputStream();

                /*
                 * If the other inputDataSources have been started, start the
                 * new one as well.
                 */
                if (started > 0)
                    try
                    {
                        inputDataSourceDesc
                            .getEffectiveInputDataSource().start();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
            }
        }
    }

    /**
     * Notifies this <tt>AudioMixer</tt> that an output
     * <tt>AudioMixingPushBufferDataSource</tt> reading from it has been
     * connected. The first of the many
     * <tt>AudioMixingPushBufferDataSource</tt>s reading from this
     * <tt>AudioMixer</tt> which gets connected causes it to connect to the
     * input <tt>DataSource</tt>s it manages.
     * 
     * @throws IOException
     */
    void connect()
        throws IOException
    {
        synchronized (inputDataSources)
        {
            if (connected == 0)
            {
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                {
                    DataSource effectiveInputDataSource
                        = inputDataSourceDesc.getEffectiveInputDataSource();
                    DataSource inputDataSource
                        = inputDataSourceDesc.inputDataSource;

                    try
                    {
                        connect(effectiveInputDataSource, inputDataSource);
                    }
                    catch (IOException ioe)
                    {
                        logger
                            .error(
                                "Failed to connect to inputDataSource "
                                    + MediaStreamImpl.toString(inputDataSource),
                                ioe);
                        throw ioe;
                    }
                }
            }

            connected++;
        }
    }

    /**
     * Connects to a specific <tt>DataSource</tt> which this <tt>AudioMixer<tt>
     * will read audio from. The specified <tt>DataSource</tt> is known to exist
     * because of a specific <tt>DataSource</tt> added as an input to this
     * instance i.e. it may be an actual input <tt>DataSource</tt> added to this
     * instance or a <tt>DataSource</tt> transcoding an input
     * <tt>DataSource</tt> added to this instance.
     *
     * @param dataSource the <tt>DataSource</tt> to connect to
     * @param inputDataSource the <tt>DataSource</tt> which is the cause for
     * <tt>dataSource</tt> to exist in this <tt>AudioMixer</tt>
     * @throws IOException if anything wrong happens while connecting to
     * <tt>dataSource</tt>
     */
    protected void connect(DataSource dataSource, DataSource inputDataSource)
        throws IOException
    {
        dataSource.connect();
    }

    /**
     * Creates a new <tt>InputStreamDesc</tt> instance which is to describe a
     * specific input <tt>SourceStream</tt> originating from a specific input
     * <tt>DataSource</tt> given by its <tt>InputDataSourceDesc</tt>.
     *
     * @param inputStream the input <tt>SourceStream</tt> to be described by the
     * new instance
     * @param inputDataSourceDesc the input <tt>DataSource</tt> given by its
     * <tt>InputDataSourceDesc</tt> to be described by the new instance
     * @return a new <tt>InputStreamDesc</tt> instance which describes the
     * specified input <tt>SourceStream</tt> and <tt>DataSource</tt>
     */
    private InputStreamDesc createInputStreamDesc(
            SourceStream inputStream,
            InputDataSourceDesc inputDataSourceDesc)
    {
        return new InputStreamDesc(inputStream, inputDataSourceDesc);
    }

    /**
     * Creates a new <tt>AudioMixingPushBufferDataSource</tt> which gives
     * access to a single audio stream representing the mix of the audio streams
     * input into this <tt>AudioMixer</tt> through its input
     * <tt>DataSource</tt>s. The returned
     * <tt>AudioMixingPushBufferDataSource</tt> can also be used to include
     * new input <tt>DataSources</tt> in this <tt>AudioMixer</tt> but
     * have their contributions not included in the mix available through the
     * returned <tt>AudioMixingPushBufferDataSource</tt>.
     * 
     * @return a new <tt>AudioMixingPushBufferDataSource</tt> which gives access
     * to a single audio stream representing the mix of the audio streams input
     * into this <tt>AudioMixer</tt> through its input <tt>DataSource</tt>s
     */
    public AudioMixingPushBufferDataSource createOutputDataSource()
    {
        return new AudioMixingPushBufferDataSource(this);
    }

    /**
     * Creates a <tt>DataSource</tt> which attempts to transcode the tracks
     * of a specific input <tt>DataSource</tt> into a specific output
     * <tt>Format</tt>.
     * 
     * @param inputDataSource the <tt>DataSource</tt> from the tracks of which
     * data is to be read and transcoded into the specified output
     * <tt>Format</tt>
     * @param outputFormat the <tt>Format</tt> in which the tracks of
     * <tt>inputDataSource</tt> are to be transcoded
     * @return a new <tt>DataSource</tt> which attempts to transcode the tracks
     * of <tt>inputDataSource</tt> into <tt>outputFormat</tt>
     * @throws IOException
     */
    private DataSource createTranscodingDataSource(
            DataSource inputDataSource,
            Format outputFormat)
        throws IOException
    {
        TranscodingDataSource transcodingDataSource;

        if (inputDataSource instanceof TranscodingDataSource)
            transcodingDataSource = null;
        else
        {
            transcodingDataSource
                = new TranscodingDataSource(inputDataSource, outputFormat);

            if (connected > 0)
                transcodingDataSource.connect();
            if (started > 0)
                transcodingDataSource.start();
        }
        return transcodingDataSource;
    }

    /**
     * Notifies this <tt>AudioMixer</tt> that an output
     * <tt>AudioMixingPushBufferDataSource</tt> reading from it has been
     * disconnected. The last of the many
     * <tt>AudioMixingPushBufferDataSource</tt>s reading from this
     * <tt>AudioMixer</tt> which gets disconnected causes it to disconnect
     * from the input <tt>DataSource</tt>s it manages.
     */
    void disconnect()
    {
        synchronized (inputDataSources)
        {
            if (connected <= 0)
                return;

            connected--;

            if (connected == 0)
            {
                outputStream = null;

                for (InputDataSourceDesc inputDataSourceDesc
                        : inputDataSources)
                    inputDataSourceDesc
                        .getEffectiveInputDataSource().disconnect();
            }
        }
    }

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of the <tt>CaptureDevice</tt>
     * this <tt>AudioMixer</tt> provides through its output
     * <tt>AudioMixingPushBufferDataSource</tt>s.
     * 
     * @return the <tt>CaptureDeviceInfo</tt> of the <tt>CaptureDevice</tt> this
     * <tt>AudioMixer</tt> provides through its output
     * <tt>AudioMixingPushBufferDataSource</tt>s
     */
    CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return captureDevice.getCaptureDeviceInfo();
    }

    /**
     * Gets the content type of the data output by this <tt>AudioMixer</tt>.
     * 
     * @return the content type of the data output by this <tt>AudioMixer</tt>
     */
    String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Gets an <tt>InputStreamDesc</tt> from a specific existing list of
     * <tt>InputStreamDesc</tt>s which describes a specific
     * <tt>SourceStream</tt>. If such an <tt>InputStreamDesc</tt> does not
     * exist, returns <tt>null</tt>.
     *
     * @param inputStream the <tt>SourceStream</tt> to locate an
     * <tt>InputStreamDesc</tt> for in <tt>existingInputStreamDescs</tt>
     * @param existingInputStreamDescs the list of existing
     * <tt>InputStreamDesc</tt>s in which an <tt>InputStreamDesc</tt> for
     * <tt>inputStream</tt> is to be located
     * @return an <tt>InputStreamDesc</tt> from
     * <tt>existingInputStreamDescs</tt> which describes <tt>inputStream</tt> if
     * such an <tt>InputStreamDesc</tt> exists; otherwise, <tt>null</tt>
     */
    private InputStreamDesc getExistingInputStreamDesc(
            SourceStream inputStream,
            InputStreamDesc[] existingInputStreamDescs)
    {
        if (existingInputStreamDescs == null)
            return null;

        for (InputStreamDesc existingInputStreamDesc
                : existingInputStreamDescs)
        {
            SourceStream existingInputStream
                = existingInputStreamDesc.getInputStream();

            if (existingInputStream == inputStream)
                return existingInputStreamDesc;
            if ((existingInputStream instanceof BufferStreamAdapter<?>)
                    && (((BufferStreamAdapter<?>) existingInputStream)
                                .getStream()
                            == inputStream))
                return existingInputStreamDesc;
            if ((existingInputStream instanceof CachingPushBufferStream)
                    && (((CachingPushBufferStream) existingInputStream)
                                .getStream()
                            == inputStream))
                return existingInputStreamDesc;
        }
        return null;
    }

    /**
     * Gets the control of a specific <tt>Controls</tt> implementation of a
     * specific type if such a control is made available through
     * {@link Controls#getControls()}; otherwise, returns <tt>null</tt>.
     *
     * @param controlsImpl the implementation of <tt>Controls</tt> which is to
     * be queried for its list of controls so that the control of the specified
     * type can be looked for
     * @param controlType a <tt>String</tt> value which names the type of the
     * control to be retrieved
     * @return an <tt>Object</tt> which represents the control of
     * <tt>controlsImpl</tt> of the specified <tt>controlType</tt> if such a
     * control is made available through <tt>Controls#getControls()</tt>;
     * otherwise, <tt>null</tt>
     */
    public static Object getControl(Controls controlsImpl, String controlType)
    {
        Object[] controls = controlsImpl.getControls();

        if ((controls != null) && (controls.length > 0))
        {
            Class<?> controlClass;

            try
            {
                controlClass = Class.forName(controlType);
            }
            catch (ClassNotFoundException cnfe)
            {
                controlClass = null;
                logger
                    .warn(
                        "Failed to find control class " + controlType,
                        cnfe);
            }
            if (controlClass != null)
                for (Object control : controls)
                    if (controlClass.isInstance(control))
                        return control;
        }
        return null;
    }

    /**
     * Gets the duration of each one of the output streams produced by this
     * <tt>AudioMixer</tt>.
     * 
     * @return the duration of each one of the output streams produced by this
     * <tt>AudioMixer</tt>
     */
    Time getDuration()
    {
        return ((DataSource) captureDevice).getDuration();
    }

    /**
     * Gets the <tt>Format</tt> in which a specific <tt>DataSource</tt>
     * provides stream data.
     * 
     * @param dataSource the <tt>DataSource</tt> for which the <tt>Format</tt>
     * in which it provides stream data is to be determined
     * @return the <tt>Format</tt> in which the specified <tt>dataSource</tt>
     * provides stream data if it was determined; otherwise, <tt>null</tt>
     */
    private static Format getFormat(DataSource dataSource)
    {
        FormatControl formatControl
            = (FormatControl) dataSource.getControl(
                    FormatControl.class.getName());

        return (formatControl == null) ? null : formatControl.getFormat();
    }

    /**
     * Gets the <tt>Format</tt> in which a specific
     * <tt>SourceStream</tt> provides data.
     * 
     * @param stream
     *            the <tt>SourceStream</tt> for which the
     *            <tt>Format</tt> in which it provides data is to be
     *            determined
     * @return the <tt>Format</tt> in which the specified
     *         <tt>SourceStream</tt> provides data if it was determined;
     *         otherwise, <tt>null</tt>
     */
    private static Format getFormat(SourceStream stream)
    {
        if (stream instanceof PushBufferStream)
            return ((PushBufferStream) stream).getFormat();
        if (stream instanceof PullBufferStream)
            return ((PullBufferStream) stream).getFormat();
        return null;
    }

    /**
     * Gets an array of <tt>FormatControl</tt>s for the
     * <tt>CaptureDevice</tt> this <tt>AudioMixer</tt> provides through
     * its output <tt>AudioMixingPushBufferDataSource</tt>s.
     * 
     * @return an array of <tt>FormatControl</tt>s for the
     *         <tt>CaptureDevice</tt> this <tt>AudioMixer</tt> provides
     *         through its output <tt>AudioMixingPushBufferDataSource</tt>s
     */
    FormatControl[] getFormatControls()
    {
        /*
         * Setting the format of the captureDevice once we've started using it
         * is likely to wreak havoc so disable it.
         */
        FormatControl[] formatControls = captureDevice.getFormatControls();

        if (formatControls != null)
            for (int i = 0; i < formatControls.length; i++)
                formatControls[i]
                    = new ReadOnlyFormatControlDelegate(formatControls[i]);
        return formatControls;
    }

    /**
     * Gets the <tt>SourceStream</tt>s (in the form of
     * <tt>InputStreamDesc</tt>) of a specific <tt>DataSource</tt>
     * (provided in the form of <tt>InputDataSourceDesc</tt>) which produce
     * data in a specific <tt>AudioFormat</tt> (or a matching one).
     * 
     * @param inputDataSourceDesc
     *            the <tt>DataSource</tt> (in the form of
     *            <tt>InputDataSourceDesc</tt>) which is to be examined for
     *            <tt>SourceStreams</tt> producing data in the specified
     *            <tt>AudioFormat</tt>
     * @param outputFormat
     *            the <tt>AudioFormat</tt> in which the collected
     *            <tt>SourceStream</tt>s are to produce data
     * @param existingInputStreams
     * @param inputStreams
     *            the <tt>List</tt> of <tt>InputStreamDesc</tt> in which
     *            the discovered <tt>SourceStream</tt>s are to be returned
     * @return <tt>true</tt> if <tt>SourceStream</tt>s produced by the
     *         specified input <tt>DataSource</tt> and outputting data in the
     *         specified <tt>AudioFormat</tt> were discovered and reported
     *         in <tt>inputStreams</tt>; otherwise, <tt>false</tt>
     */
    private boolean getInputStreamsFromInputDataSource(
        InputDataSourceDesc inputDataSourceDesc,
        AudioFormat outputFormat,
        InputStreamDesc[] existingInputStreams,
        List<InputStreamDesc> inputStreams)
    {
        DataSource inputDataSource
            = inputDataSourceDesc.getEffectiveInputDataSource();
        SourceStream[] inputDataSourceStreams;

        if (inputDataSource instanceof PushBufferDataSource)
            inputDataSourceStreams
                = ((PushBufferDataSource) inputDataSource).getStreams();
        else if (inputDataSource instanceof PullBufferDataSource)
            inputDataSourceStreams
                = ((PullBufferDataSource) inputDataSource).getStreams();
        else if (inputDataSource instanceof TranscodingDataSource)
            inputDataSourceStreams
                = ((TranscodingDataSource) inputDataSource).getStreams();
        else
            inputDataSourceStreams = null;

        if (inputDataSourceStreams != null)
        {
            boolean added = false;

            for (SourceStream inputStream : inputDataSourceStreams)
            {
                Format inputFormat = getFormat(inputStream);

                if ((inputFormat != null)
                        && matches(inputFormat, outputFormat))
                {
                    InputStreamDesc inputStreamDesc
                        = getExistingInputStreamDesc(
                            inputStream,
                            existingInputStreams);

                    if (inputStreamDesc == null)
                        inputStreamDesc
                            = createInputStreamDesc(
                                    inputStream,
                                    inputDataSourceDesc);
                    if (inputStreams.add(inputStreamDesc))
                        added = true;
                }
            }
            return added;
        }

        Format inputFormat = getFormat(inputDataSource);

        if ((inputFormat != null) && !matches(inputFormat, outputFormat))
        {
            if (inputDataSource instanceof PushDataSource)
            {
                for (PushSourceStream inputStream
                        : ((PushDataSource) inputDataSource).getStreams())
                {
                    InputStreamDesc inputStreamDesc
                        = getExistingInputStreamDesc(
                            inputStream,
                            existingInputStreams);

                    if (inputStreamDesc == null)
                        inputStreamDesc
                            = createInputStreamDesc(
                                    new PushBufferStreamAdapter(
                                            inputStream,
                                            inputFormat),
                                    inputDataSourceDesc);
                    inputStreams.add(inputStreamDesc);
                }
                return true;
            }
            if (inputDataSource instanceof PullDataSource)
            {
                for (PullSourceStream inputStream
                        : ((PullDataSource) inputDataSource).getStreams())
                {
                    InputStreamDesc inputStreamDesc
                        = getExistingInputStreamDesc(
                            inputStream,
                            existingInputStreams);

                    if (inputStreamDesc == null)
                        inputStreamDesc
                            = createInputStreamDesc(
                                    new PullBufferStreamAdapter(
                                            inputStream,
                                            inputFormat),
                                    inputDataSourceDesc);
                    inputStreams.add(inputStreamDesc);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the <tt>SourceStream</tt>s (in the form of
     * <tt>InputStreamDesc</tt>) of the <tt>DataSource</tt>s from which
     * this <tt>AudioMixer</tt> reads data which produce data in a specific
     * <tt>AudioFormat</tt>. When an input <tt>DataSource</tt> does not
     * have such <tt>SourceStream</tt>s, an attempt is made to transcode its
     * tracks so that such <tt>SourceStream</tt>s can be retrieved from it
     * after transcoding.
     * 
     * @param outputFormat
     *            the <tt>AudioFormat</tt> in which the retrieved
     *            <tt>SourceStream</tt>s are to produce data
     * @param existingInputStreams
     * @return a new collection of <tt>SourceStream</tt>s (in the form of
     *         <tt>InputStreamDesc</tt>) retrieved from the input
     *         <tt>DataSource</tt>s of this <tt>AudioMixer</tt> and
     *         producing data in the specified <tt>AudioFormat</tt>
     * @throws IOException
     */
    private Collection<InputStreamDesc> getInputStreamsFromInputDataSources(
            AudioFormat outputFormat,
            InputStreamDesc[] existingInputStreams)
        throws IOException
    {
        List<InputStreamDesc> inputStreams = new ArrayList<InputStreamDesc>();

        synchronized (inputDataSources)
        {
            for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
            {
                boolean got
                    = getInputStreamsFromInputDataSource(
                            inputDataSourceDesc,
                            outputFormat,
                            existingInputStreams,
                            inputStreams);

                if (!got)
                {
                    DataSource transcodingDataSource
                        = createTranscodingDataSource(
                                inputDataSourceDesc
                                    .getEffectiveInputDataSource(),
                                outputFormat);

                    if (transcodingDataSource != null)
                    {
                        inputDataSourceDesc
                            .setTranscodingDataSource(transcodingDataSource);

                        getInputStreamsFromInputDataSource(
                            inputDataSourceDesc,
                            outputFormat,
                            existingInputStreams,
                            inputStreams);
                    }
                }
            }
        }
        return inputStreams;
    }

    /**
     * Gets the <tt>AudioMixingPushBufferDataSource</tt> containing the mix of
     * all input <tt>DataSource</tt>s excluding the <tt>CaptureDevice</tt> of
     * this <tt>AudioMixer</tt> and is thus meant for playback on the local peer
     * in a call.
     *
     * @return the <tt>AudioMixingPushBufferDataSource</tt> containing the mix
     * of all input <tt>DataSource</tt>s excluding the <tt>CaptureDevice</tt> of
     * this <tt>AudioMixer</tt> and is thus meant for playback on the local peer
     * in a call
     */
    public AudioMixingPushBufferDataSource getLocalOutputDataSource()
    {
        return localOutputDataSource;
    }

    /**
     * Gets the <tt>AudioFormat</tt> in which the input
     * <tt>DataSource</tt>s of this <tt>AudioMixer</tt> can produce data
     * and which is to be the output <tt>Format</tt> of this
     * <tt>AudioMixer</tt>.
     * 
     * @return the <tt>AudioFormat</tt> in which the input
     *         <tt>DataSource</tt>s of this <tt>AudioMixer</tt> can
     *         produce data and which is to be the output <tt>Format</tt> of
     *         this <tt>AudioMixer</tt>
     */
    private AudioFormat getOutputFormatFromInputDataSources()
    {
        String formatControlType = FormatControl.class.getName();
        AudioFormat outputFormat = null;

        synchronized (inputDataSources)
        {
            for (InputDataSourceDesc inputDataSource : inputDataSources)
            {
                FormatControl formatControl
                    = (FormatControl)
                        inputDataSource
                            .getEffectiveInputDataSource()
                                .getControl(formatControlType);

                if (formatControl != null)
                {
                    AudioFormat format
                        = (AudioFormat) formatControl.getFormat();

                    if (format != null)
                    {
                        // SIGNED
                        int signed = format.getSigned();

                        if ((AudioFormat.SIGNED == signed)
                                || (Format.NOT_SPECIFIED == signed))
                        {
                            // LITTLE_ENDIAN
                            int endian = format.getEndian();

                            if ((AudioFormat.LITTLE_ENDIAN == endian)
                                    || (Format.NOT_SPECIFIED == endian))
                            {
                                outputFormat = format;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (outputFormat == null)
            outputFormat = DEFAULT_OUTPUT_FORMAT;

        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Determined outputFormat of AudioMixer"
                        + " from inputDataSources to be "
                        + outputFormat);
        return outputFormat;
    }

    /**
     * Gets the <tt>AudioMixerPushBufferStream</tt>, first creating it if it
     * does not exist already, which reads data from the input
     * <tt>DataSource</tt>s of this <tt>AudioMixer</tt> and pushes it to
     * output <tt>AudioMixingPushBufferStream</tt>s for audio mixing.
     * 
     * @return the <tt>AudioMixerPushBufferStream</tt> which reads data from
     *         the input <tt>DataSource</tt>s of this
     *         <tt>AudioMixer</tt> and pushes it to output
     *         <tt>AudioMixingPushBufferStream</tt>s for audio mixing
     */
    AudioMixerPushBufferStream getOutputStream()
    {
        synchronized (inputDataSources)
        {
            AudioFormat outputFormat = getOutputFormatFromInputDataSources();

            setOutputFormatToInputDataSources(outputFormat);

            Collection<InputStreamDesc> inputStreams;

            try
            {
                inputStreams
                    = getInputStreamsFromInputDataSources(
                        outputFormat,
                        (outputStream == null)
                            ? null
                            : outputStream.getInputStreams());
            }
            catch (IOException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }

            if (inputStreams.size() <= 0)
                outputStream = null;
            else
            {
                if (outputStream == null)
                    outputStream = new AudioMixerPushBufferStream(outputFormat);
                outputStream.setInputStreams(inputStreams);
            }
            return outputStream;
        }
    }

    /**
     * Determines whether a specific <tt>Format</tt> matches a specific
     * <tt>Format</tt> in the sense of JMF <tt>Format</tt> matching.
     * Since this <tt>AudioMixer</tt> and the audio mixing functionality
     * related to it can handle varying characteristics of a certain output
     * <tt>Format</tt>, the only requirement for the specified
     * <tt>Format</tt>s to match is for both of them to have one and the
     * same encoding.
     * 
     * @param input
     *            the <tt>Format</tt> for which it is required to determine
     *            whether it matches a specific <tt>Format</tt>
     * @param pattern
     *            the <tt>Format</tt> against which the specified
     *            <tt>input</tt> is to be matched
     * @return <tt>true</tt> if the specified
     *         <tt>input<tt> matches the specified <tt>pattern</tt> in
     *         the sense of JMF <tt>Format</tt> matching; otherwise,
     *         <tt>false</tt>
     */
    private boolean matches(Format input, AudioFormat pattern)
    {
        return
            ((input instanceof AudioFormat) && input.isSameEncoding(pattern));
    }

    /**
     * Reads media from a specific <tt>PushBufferStream</tt> which belongs to
     * a specific <tt>DataSource</tt> into a specific output <tt>Buffer</tt>.
     * Allows extenders to tap into the reading and monitor and customize it.
     *
     * @param stream the <tt>PushBufferStream</tt> to read media from and known
     * to belong to the specified <tt>DataSOurce</tt>
     * @param buffer the output <tt>Buffer</tt> in which the media read from the
     * specified <tt>stream</tt> is to be written so that it gets returned to
     * the caller
     * @param dataSource the <tt>DataSource</tt> from which <tt>stream</tt>
     * originated
     * @throws IOException if anything wrong happens while reading from the
     * specified <tt>stream</tt>
     */
    protected void read(
            PushBufferStream stream,
            Buffer buffer,
            DataSource dataSource)
        throws IOException
    {
        stream.read(buffer);
    }

    /**
     * Reads an integer from a specific series of bytes starting the reading at
     * a specific offset in it.
     * 
     * @param input
     *            the series of bytes to read an integer from
     * @param inputOffset
     *            the offset in <tt>input</tt> at which the reading of the
     *            integer is to start
     * @return an integer read from the specified series of bytes starting at
     *         the specified offset in it
     */
    private static int readInt(byte[] input, int inputOffset)
    {
        return
            (input[inputOffset + 3] << 24)
                | ((input[inputOffset + 2] & 0xFF) << 16)
                | ((input[inputOffset + 1] & 0xFF) << 8)
                | (input[inputOffset] & 0xFF);
    }

    /**
     * Sets a specific <tt>AudioFormat</tt>, if possible, as the output
     * format of the input <tt>DataSource</tt>s of this
     * <tt>AudioMixer</tt> in an attempt to not have to perform explicit
     * transcoding of the input <tt>SourceStream</tt>s.
     * 
     * @param outputFormat
     *            the <tt>AudioFormat</tt> in which the input
     *            <tt>DataSource</tt>s of this <tt>AudioMixer</tt> are
     *            to be instructed to output
     */
    private void setOutputFormatToInputDataSources(AudioFormat outputFormat)
    {
        String formatControlType = FormatControl.class.getName();

        synchronized (inputDataSources)
        {
            for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
            {
                DataSource inputDataSource
                    = inputDataSourceDesc.getEffectiveInputDataSource();
                FormatControl formatControl
                    = (FormatControl)
                        inputDataSource.getControl(formatControlType);

                if (formatControl != null)
                {
                    Format inputFormat = formatControl.getFormat();

                    if ((inputFormat == null)
                            || !matches(inputFormat, outputFormat))
                    {
                        Format setFormat
                            = formatControl.setFormat(outputFormat);

                        if (setFormat == null)
                            logger
                                .error(
                                    "Failed to set format of inputDataSource to "
                                        + outputFormat);
                        else if (setFormat != outputFormat)
                            logger
                                .warn(
                                    "Failed to change format of inputDataSource from "
                                        + setFormat
                                        + " to "
                                        + outputFormat);
                        else if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Set format of inputDataSource to "
                                        + setFormat);
                    }
                }
            }
        }
    }

    /**
     * Starts the input <tt>DataSource</tt>s of this <tt>AudioMixer</tt>.
     * 
     * @throws IOException
     */
    void start()
        throws IOException
    {
        synchronized (inputDataSources)
        {
            if (started == 0)
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    inputDataSourceDesc.getEffectiveInputDataSource().start();

            started++;
        }
    }

    /**
     * Stops the input <tt>DataSource</tt>s of this <tt>AudioMixer</tt>.
     * 
     * @throws IOException
     */
    void stop()
        throws IOException
    {
        synchronized (inputDataSources)
        {
            if (started <= 0)
                return;

            started--;

            if (started == 0)
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    inputDataSourceDesc.getEffectiveInputDataSource().stop();
        }
    }

    /**
     * Represents a <tt>PushBufferStream</tt> which reads data from the
     * <tt>SourceStream</tt>s of the input <tt>DataSource</tt>s of this
     * <tt>AudioMixer</tt> and pushes it to
     * <tt>AudioMixingPushBufferStream</tt>s for audio mixing.
     */
    class AudioMixerPushBufferStream
        implements PushBufferStream
    {

        /**
         * The factor which scales a <tt>short</tt> value to an <tt>int</tt>
         * value.
         */
        private static final float INT_TO_SHORT_RATIO
            = Integer.MAX_VALUE / (float) Short.MAX_VALUE;

        /**
         * The factor which scales an <tt>int</tt> value to a <tt>short</tt>
         * value.
         */
        private static final float SHORT_TO_INT_RATIO
            = Short.MAX_VALUE / (float) Integer.MAX_VALUE;

        /**
         * The number of reads from an input stream with no returned samples
         * which do not get reported in tracing output. Once the number of such
         * reads from an input streams exceeds this limit, they get reported and
         * the counting is restarted.
         */
        private static final long TRACE_NON_CONTRIBUTING_READ_COUNT = 0;

        /**
         * The <tt>SourceStream</tt>s (in the form of
         * <tt>InputStreamDesc</tt> so that this instance can track back the
         * <tt>AudioMixingPushBufferDataSource</tt> which outputs the mixed
         * audio stream and determine whether the associated
         * <tt>SourceStream</tt> is to be included into the mix) from which
         * this instance reads its data.
         */
        private InputStreamDesc[] inputStreams;

        /**
         * The <tt>Object</tt> which synchronizes the access to
         * {@link #inputStreams}-related members.
         */
        private final Object inputStreamsSyncRoot = new Object();

        /**
         * The <tt>AudioFormat</tt> of the <tt>Buffer</tt> read during the last
         * read from one of the {@link #inputStreams}. Only used for debugging
         * purposes.
         */
        private AudioFormat lastReadInputFormat;

        /**
         * The <tt>AudioFormat</tt> of the data this instance outputs.
         */
        private final AudioFormat outputFormat;

        /**
         * The <tt>AudioMixingPushBufferStream</tt>s to which this instance
         * pushes data for audio mixing.
         */
        private final List<AudioMixingPushBufferStream> outputStreams
            = new ArrayList<AudioMixingPushBufferStream>();

        /**
         * The <tt>BufferTransferHandler</tt> through which this instance
         * gets notifications from its input <tt>SourceStream</tt>s that new
         * data is available for audio mixing.
         */
        private final BufferTransferHandler transferHandler
            = new BufferTransferHandler()
                    {
                        public void transferData(PushBufferStream stream)
                        {
                            AudioMixerPushBufferStream.this.transferData();
                        }
                    };

        /**
         * Initializes a new <tt>AudioMixerPushBufferStream</tt> instance to
         * output data in a specific <tt>AudioFormat</tt>.
         * 
         * @param outputFormat
         *            the <tt>AudioFormat</tt> in which the new instance is
         *            to output data
         */
        private AudioMixerPushBufferStream(AudioFormat outputFormat)
        {
            this.outputFormat = outputFormat;
        }

        /**
         * Adds a specific <tt>AudioMixingPushBufferStream</tt> to the
         * collection of such streams which this instance is to push the data
         * for audio mixing it reads from its input <tt>SourceStream</tt>s.
         * 
         * @param outputStream
         *            the <tt>AudioMixingPushBufferStream</tt> to be added
         *            to the collection of such streams which this instance is
         *            to push the data for audio mixing it reads from its input
         *            <tt>SourceStream</tt>s
         */
        void addOutputStream(AudioMixingPushBufferStream outputStream)
        {
            if (outputStream == null)
                throw new IllegalArgumentException("outputStream");

            synchronized (outputStreams)
            {
                if (!outputStreams.contains(outputStream))
                    outputStreams.add(outputStream);
            }
        }

        /**
         * Implements {@link SourceStream#endOfStream()}. Delegates to the input
         * <tt>SourceStreams</tt> of this instance.
         *
         * @return <tt>true</tt> if all input <tt>SourceStream</tt>s of this
         * instance have reached the end of their content; <tt>false</tt>,
         * otherwise
         */
        public boolean endOfStream()
        {
            synchronized (inputStreamsSyncRoot)
            {
                if (inputStreams != null)
                    for (InputStreamDesc inputStreamDesc : inputStreams)
                        if (!inputStreamDesc.getInputStream().endOfStream())
                            return false;
            }
            return true;
        }

        /**
         * Implements {@link SourceStream#getContentDescriptor()}. Returns a
         * <tt>ContentDescriptor</tt> which describes the content type of this
         * instance.
         *
         * @return a <tt>ContentDescriptor</tt> which describes the content type
         * of this instance
         */
        public ContentDescriptor getContentDescriptor()
        {
            return
                new ContentDescriptor(AudioMixer.this.getContentType());
        }

        /**
         * Implements {@link SourceStream#getContentLength()}. Delegates to the
         * input <tt>SourceStreams</tt> of this instance.
         *
         * @return the length of the content made available by this instance
         * which is the maximum length of the contents made available by its
         * input <tt>StreamSource</tt>s
         */
        public long getContentLength()
        {
            long contentLength = 0;

            synchronized (inputStreamsSyncRoot)
            {
                if (inputStreams != null)
                    for (InputStreamDesc inputStreamDesc : inputStreams)
                    {
                        long inputContentLength
                            = inputStreamDesc.getInputStream().getContentLength();

                        if (LENGTH_UNKNOWN == inputContentLength)
                            return LENGTH_UNKNOWN;
                        if (contentLength < inputContentLength)
                            contentLength = inputContentLength;
                    }
            }
            return contentLength;
        }

        /**
         * Implements {@link Controls#getControl(String)}. Invokes
         * {@link #getControls()} and then looks for a control of the specified
         * type in the returned array of controls.
         *
         * @param controlType a <tt>String</tt> value naming the type of the
         * control of this instance to be retrieved
         * @return an <tt>Object</tt> which represents the control of this
         * instance with the specified type
         */
        public Object getControl(String controlType)
        {
            return AudioMixer.getControl(this, controlType);
        }

        /*
         * Implements Controls#getControls(). Does nothing.
         */
        public Object[] getControls()
        {
            // TODO Auto-generated method stub
            return new Object[0];
        }

        /**
         * Implements {@link PushBufferStream#getFormat()}. Returns the
         * <tt>AudioFormat</tt> in which this instance was configured to output
         * its data.
         *
         * @return the <tt>AudioFormat</tt> in which this instance was
         * configured to output its data
         */
        public AudioFormat getFormat()
        {
            return outputFormat;
        }

        /**
         * Gets the <tt>SourceStream</tt>s (in the form of
         * <tt>InputStreamDesc</tt>s) from which this instance reads audio
         * samples.
         *
         * @return an array of <tt>InputStreamDesc</tt>s which describe the
         * input <tt>SourceStream</tt>s from which this instance reads audio
         * samples
         */
        InputStreamDesc[] getInputStreams()
        {
            synchronized (inputStreamsSyncRoot)
            {
                return (inputStreams == null) ? null : inputStreams.clone();
            }
        }

        /**
         * Implements {@link PushBufferStream#read(Buffer)}. Reads audio samples
         * from the input <tt>SourceStreams</tt> of this instance in various
         * formats, converts the read audio samples to one and the same format
         * and pushes them to the output <tt>AudioMixingPushBufferStream</tt>s
         * for the very audio mixing.
         *
         * @param buffer the <tt>Buffer</tt> in which the audio samples read
         * from the input <tt>SourceStream</tt>s are to be returned to the
         * caller
         * @throws IOException if any of the input <tt>SourceStream</tt>s throw
         * such an exception while reading from them or anything else goes wrong
         */
        public void read(Buffer buffer)
            throws IOException
        {
            InputStreamDesc[] inputStreams;

            synchronized (inputStreamsSyncRoot)
            {
                if (this.inputStreams != null)
                    inputStreams = this.inputStreams.clone();
                else
                    inputStreams = null;
            }

            int inputStreamCount
                = (inputStreams == null) ? 0 : inputStreams.length;

            if (inputStreamCount <= 0)
                return;

            AudioFormat outputFormat = getFormat();
            InputSampleDesc inputSampleDesc
                = new InputSampleDesc(
                        new int[inputStreamCount][],
                        inputStreams);
            int maxInputSampleCount;

            try
            {
                maxInputSampleCount
                    = readInputPushBufferStreams(
                        outputFormat,
                        inputSampleDesc);
            }
            catch (UnsupportedFormatException ufex)
            {
                IOException ioex = new IOException();
                ioex.initCause(ufex);
                throw ioex;
            }

            maxInputSampleCount
                = Math.max(
                        maxInputSampleCount,
                        readInputPullBufferStreams(
                            outputFormat,
                            maxInputSampleCount,
                            inputSampleDesc));

            buffer.setData(inputSampleDesc);
            buffer.setLength(maxInputSampleCount);

            /*
             * Convey the timeStamp so that it can be reported by the Buffers of
             * the AudioMixingPushBufferStreams when mixes are read from them.
             */
            long timeStamp = inputSampleDesc.getTimeStamp();

            if (timeStamp != Buffer.TIME_UNKNOWN)
                buffer.setTimeStamp(timeStamp);
        }

        /**
         * Reads audio samples from the input <tt>PullBufferStream</tt>s of this
         * instance and converts them to a specific output <tt>AudioFormat</tt>.
         * An attempt is made to read a specific maximum number of samples from
         * each of the <tt>PullBufferStream</tt>s but the very
         * <tt>PullBufferStream</tt> may not honor the request.
         *
         * @param outputFormat the <tt>AudioFormat</tt> in which the audio
         * samples read from the <tt>PullBufferStream</tt>s are to be converted
         * before being returned
         * @param outputSampleCount the maximum number of audio samples to be
         * read from each of the <tt>PullBufferStream</tt>s but the very
         * <tt>PullBufferStream</tt>s may not honor the request
         * @param inputSampleDesc an <tt>InputStreamDesc</tt> which specifies
         * the input streams to be read and the collection of audio samples in
         * which the read audio samples are to be returned
         * @return the maximum number of audio samples actually read from the
         * input <tt>PullBufferStream</tt>s of this instance
         * @throws IOException if anything goes wrong while reading the
         * specified input streams
         */
        private int readInputPullBufferStreams(
                AudioFormat outputFormat,
                int outputSampleCount,
                InputSampleDesc inputSampleDesc)
            throws IOException
        {
            InputStreamDesc[] inputStreams = inputSampleDesc.inputStreams;
            int maxInputSampleCount = 0;

            for (InputStreamDesc inputStream : inputStreams)
                if (inputStream.getInputStream() instanceof PullBufferStream)
                    throw
                        new UnsupportedOperationException(
                                AudioMixerPushBufferStream.class.getSimpleName()
                                    + ".readInputPullBufferStreams"
                                    + "(AudioFormat,int,InputSampleDesc)");
            return maxInputSampleCount;
        }

        /**
         * Reads audio samples from a specific <tt>PushBufferStream</tt> and
         * converts them to a specific output <tt>AudioFormat</tt>. An attempt
         * is made to read a specific maximum number of samples from the
         * specified <tt>PushBufferStream</tt> but the very
         * <tt>PushBufferStream</tt> may not honor the request.
         * 
         * @param inputStreamDesc an <tt>InputStreamDesc</tt> which specifies
         * the input <tt>PushBufferStream</tt> to read from
         * @param outputFormat the <tt>AudioFormat</tt> to which the samples
         * read from <tt>inputStream</tt> are to be converted before being
         * returned
         * @param sampleCount the maximum number of samples which the read
         * operation should attempt to read from <tt>inputStream</tt> but the
         * very <tt>inputStream</tt> may not honor the request
         * @return a <tt>Buffer</tt> which contains the array of <tt>int</tt>
         * audio samples read from the specified <tt>inputStream</tt>
         * @throws IOException if anything wrong happens while reading
         * <tt>inputStream</tt>
         * @throws UnsupportedFormatException if converting the samples read
         * from <tt>inputStream</tt> to <tt>outputFormat</tt> fails
         */
        private Buffer readInputPushBufferStream(
                InputStreamDesc inputStreamDesc,
                AudioFormat outputFormat,
                int sampleCount)
            throws IOException,
                   UnsupportedFormatException
        {
            PushBufferStream inputStream
                = (PushBufferStream) inputStreamDesc.getInputStream();
            AudioFormat inputStreamFormat
                = (AudioFormat) inputStream.getFormat();
            Buffer buffer = new Buffer();
        
            if (sampleCount != 0)
            {
                Class<?> inputDataType = inputStreamFormat.getDataType();
        
                if (Format.byteArray.equals(inputDataType))
                {
                    buffer.setData(
                        new byte[
                                sampleCount
                                    * (inputStreamFormat.getSampleSizeInBits()
                                            / 8)]);
                    buffer.setLength(0);
                    buffer.setOffset(0);
                }
                else
                    throw
                        new UnsupportedFormatException(
                                "!Format.getDataType().equals(byte[].class)",
                                inputStreamFormat);
            }

            AudioMixer.this.read(
                    inputStream,
                    buffer,
                    inputStreamDesc.getInputDataSource());

            /*
             * If the media is to be discarded, don't even bother with the
             * checks and the conversion.
             */
            if (buffer.isDiscard())
                return null;

            int inputLength = buffer.getLength();
        
            if (inputLength <= 0)
                return null;
        
            AudioFormat inputFormat = (AudioFormat) buffer.getFormat();

            if (inputFormat == null)
                inputFormat = inputStreamFormat;

            if (logger.isTraceEnabled()
                    && (lastReadInputFormat != null)
                    && !lastReadInputFormat.matches(inputFormat))
            {
                lastReadInputFormat = inputFormat;
                logger
                    .trace(
                        "Read inputSamples in different format "
                            + lastReadInputFormat);
            }

            int inputFormatSigned = inputFormat.getSigned();

            if ((inputFormatSigned != AudioFormat.SIGNED)
                    && (inputFormatSigned != Format.NOT_SPECIFIED))
                throw
                    new UnsupportedFormatException(
                            "AudioFormat.getSigned()",
                            inputFormat);

            int inputChannels = inputFormat.getChannels();
            int outputChannels = outputFormat.getChannels();

            if ((inputChannels != outputChannels)
                    && (inputChannels != Format.NOT_SPECIFIED)
                    && (outputChannels != Format.NOT_SPECIFIED))
            {
                logger
                    .error(
                        "Read inputFormat with channels "
                            + inputChannels
                            + " while expected outputFormat channels is "
                            + outputChannels);
                throw
                    new UnsupportedFormatException(
                            "AudioFormat.getChannels()",
                            inputFormat);
            }

            // Warn about different sampleRates.
            double inputSampleRate = inputFormat.getSampleRate();
            double outputSampleRate = outputFormat.getSampleRate();

            if (inputSampleRate != outputSampleRate)
                logger
                    .warn(
                        "Read inputFormat with sampleRate "
                            + inputSampleRate
                            + " while expected outputFormat sampleRate is "
                            + outputSampleRate);

            Object inputData = buffer.getData();
        
            if (inputData instanceof byte[])
            {
                int inputSampleSizeInBits = inputFormat.getSampleSizeInBits();
                byte[] inputSamples = (byte[]) inputData;
                int[] outputSamples;
                int outputSampleSizeInBits = outputFormat.getSampleSizeInBits();

                if (logger.isTraceEnabled()
                        && (inputSampleSizeInBits != outputSampleSizeInBits))
                    logger
                        .trace(
                            "Read inputFormat with sampleSizeInBits "
                                + inputSampleSizeInBits
                                + ". Will convert to sampleSizeInBits"
                                + outputSampleSizeInBits);

                switch (inputSampleSizeInBits)
                {
                case 16:
                    outputSamples = new int[inputSamples.length / 2];
                    for (int i = 0; i < outputSamples.length; i++)
                    {
                        int sample = ArrayIOUtils.readInt16(inputSamples, i * 2);
        
                        switch (outputSampleSizeInBits)
                        {
                        case 16:
                            break;
                        case 32:
                            sample = Math.round(sample * INT_TO_SHORT_RATIO);
                            break;
                        case 8:
                        case 24:
                        default:
                            throw
                                new UnsupportedFormatException(
                                        "AudioFormat.getSampleSizeInBits()",
                                        outputFormat);
                        }
        
                        outputSamples[i] = sample;
                    }
                    buffer.setData(outputSamples);
                    buffer.setFormat(outputFormat);
                    buffer.setLength(outputSamples.length);
                    buffer.setOffset(0);
                    return buffer;
                case 32:
                    outputSamples = new int[inputSamples.length / 4];
                    for (int i = 0; i < outputSamples.length; i++)
                    {
                        int sample = readInt(inputSamples, i * 4);
        
                        switch (outputSampleSizeInBits)
                        {
                        case 16:
                            sample = Math.round(sample * SHORT_TO_INT_RATIO);
                            break;
                        case 32:
                            break;
                        case 8:
                        case 24:
                        default:
                            throw
                                new UnsupportedFormatException(
                                        "AudioFormat.getSampleSizeInBits()",
                                        outputFormat);
                        }
        
                        outputSamples[i] = sample;
                    }
                    buffer.setData(outputSamples);
                    buffer.setFormat(outputFormat);
                    buffer.setLength(outputSamples.length);
                    buffer.setOffset(0);
                    return buffer;
                case 8:
                case 24:
                default:
                    throw
                        new UnsupportedFormatException(
                                "AudioFormat.getSampleSizeInBits()",
                                inputFormat);
                }
            }
            else if (inputData != null)
            {
                throw
                    new UnsupportedFormatException(
                            "Format.getDataType().equals("
                                + inputData.getClass()
                                + ")",
                            inputFormat);
            }
            return null;
        }

        /**
         * Reads audio samples from the input <tt>PushBufferStream</tt>s of
         * this instance and converts them to a specific output
         * <tt>AudioFormat</tt>.
         *
         * @param outputFormat the <tt>AudioFormat</tt> in which the audio
         * samples read from the <tt>PushBufferStream</tt>s are to be converted
         * before being returned
         * @param inputSampleDesc an <tt>InputSampleDesc</tt> which specifies
         * the input streams to be read and  the collection of audio samples in
         * which the read audio samples are to be returned
         * @return the maximum number of audio samples actually read from the
         * input <tt>PushBufferStream</tt>s of this instance
         * @throws IOException if anything wrong happens while reading the
         * specified input streams
         * @throws UnsupportedFormatException if any of the input streams
         * provides media in a format different than <tt>outputFormat</tt>
         */
        private int readInputPushBufferStreams(
                AudioFormat outputFormat,
                InputSampleDesc inputSampleDesc)
            throws IOException,
                   UnsupportedFormatException
        {
            InputStreamDesc[] inputStreams = inputSampleDesc.inputStreams;
            int[][] inputSamples = inputSampleDesc.inputSamples;
            int maxInputSampleCount = 0;
        
            for (int i = 0; i < inputStreams.length; i++)
            {
                InputStreamDesc inputStreamDesc = inputStreams[i];
                SourceStream inputStream = inputStreamDesc.getInputStream();
        
                if (inputStream instanceof PushBufferStream)
                {
                    Buffer inputStreamBuffer
                        = readInputPushBufferStream(
                                inputStreamDesc,
                                outputFormat,
                                maxInputSampleCount);
                    int[] inputStreamSamples
                        = (inputStreamBuffer == null)
                            ? null
                            : (int[]) inputStreamBuffer.getData();
                    int inputStreamSampleCount;
        
                    if (inputStreamSamples != null)
                    {
                        inputStreamSampleCount = inputStreamSamples.length;
                        if (inputStreamSampleCount != 0)
                        {
                            inputSamples[i] = inputStreamSamples;
        
                            if (maxInputSampleCount < inputStreamSampleCount)
                                maxInputSampleCount = inputStreamSampleCount;

                            /*
                             * Convey the timeStamp so that it can be set to the
                             * Buffers of the AudioMixingPushBufferStreams when
                             * mixes are read from them. Since the inputStreams
                             * will report different timeStamps, only use the
                             * first meaningful timestamp for now.
                             */
                            if (inputSampleDesc.getTimeStamp()
                                    == Buffer.TIME_UNKNOWN)
                                inputSampleDesc
                                    .setTimeStamp(
                                        inputStreamBuffer.getTimeStamp());
                        }
                        else if (logger.isTraceEnabled())
                            inputStreamDesc.nonContributingReadCount++;
                    }
                    else if (logger.isTraceEnabled())
                        inputStreamDesc.nonContributingReadCount++;

                    if (logger.isTraceEnabled()
                            && (TRACE_NON_CONTRIBUTING_READ_COUNT > 0)
                            && (inputStreamDesc.nonContributingReadCount
                                    >= TRACE_NON_CONTRIBUTING_READ_COUNT))
                    {
                        logger
                            .trace(
                                "Failed to read actual inputSamples more than "
                                    + inputStreamDesc
                                        .nonContributingReadCount
                                    + " times from inputStream with hash code "
                                    + inputStreamDesc
                                        .getInputStream().hashCode());
                        inputStreamDesc.nonContributingReadCount = 0;
                    }
                }
            }
            return maxInputSampleCount;
        }

        /**
         * Removes a specific <tt>AudioMixingPushBufferStream</tt> from the
         * collection of such streams which this instance pushes the data for
         * audio mixing it reads from its input <tt>SourceStream</tt>s.
         * 
         * @param outputStream
         *            the <tt>AudioMixingPushBufferStream</tt> to be removed
         *            from the collection of such streams which this instance
         *            pushes the data for audio mixing it reads from its input
         *            <tt>SourceStream</tt>s
         */
        void removeOutputStream(AudioMixingPushBufferStream outputStream)
        {
            synchronized (outputStreams)
            {
                if (outputStream != null)
                    outputStreams.remove(outputStream);
            }
        }

        /**
         * Pushes a copy of a specific set of input audio samples to a specific
         * <tt>AudioMixingPushBufferStream</tt> for audio mixing. Audio
         * samples read from input <tt>DataSource</tt>s which the
         * <tt>AudioMixingPushBufferDataSource</tt> owner of the specified
         * <tt>AudioMixingPushBufferStream</tt> has specified to not be
         * included in the output mix are not pushed to the
         * <tt>AudioMixingPushBufferStream</tt>.
         * 
         * @param outputStream
         *            the <tt>AudioMixingPushBufferStream</tt> to push the
         *            specified set of audio samples to
         * @param inputSampleDesc
         *            the set of audio samples to be pushed to
         *            <tt>outputStream</tt> for audio mixing
         * @param maxInputSampleCount
         *            the maximum number of audio samples available in
         *            <tt>inputSamples</tt>
         */
        private void setInputSamples(
            AudioMixingPushBufferStream outputStream,
            InputSampleDesc inputSampleDesc,
            int maxInputSampleCount)
        {
            int[][] inputSamples = inputSampleDesc.inputSamples;
            InputStreamDesc[] inputStreams = inputSampleDesc.inputStreams;

            inputSamples = inputSamples.clone();

            AudioMixingPushBufferDataSource outputDataSource
                = outputStream.getDataSource();

            for (int i = 0; i < inputSamples.length; i++)
            {
                InputStreamDesc inputStreamDesc = inputStreams[i];

                if (outputDataSource.equals(
                        inputStreamDesc.getOutputDataSource()))
                    inputSamples[i] = null;
            }

            outputStream
                .setInputSamples(
                    inputSamples,
                    maxInputSampleCount,
                    inputSampleDesc.getTimeStamp());
        }

        /**
         * Sets the <tt>SourceStream</tt>s (in the form of
         * <tt>InputStreamDesc</tt>) from which this instance is to read
         * audio samples and push them to the
         * <tt>AudioMixingPushBufferStream</tt>s for audio mixing.
         * 
         * @param inputStreams
         *            the <tt>SourceStream</tt>s (in the form of
         *            <tt>InputStreamDesc</tt>) from which this instance is
         *            to read audio samples and push them to the
         *            <tt>AudioMixingPushBufferStream</tt>s for audio mixing
         */
        private void setInputStreams(Collection<InputStreamDesc> inputStreams)
        {
            InputStreamDesc[] oldValue;
            InputStreamDesc[] newValue
                = inputStreams.toArray(
                        new InputStreamDesc[inputStreams.size()]);

            synchronized (inputStreamsSyncRoot)
            {
                oldValue = this.inputStreams;

                this.inputStreams = newValue;
            }

            boolean valueIsChanged = !Arrays.equals(oldValue, newValue);

            if (valueIsChanged)
            {
                setTransferHandler(oldValue, null);

                boolean skippedForTransferHandler = false;

                for (InputStreamDesc inputStreamDesc : newValue)
                {
                    SourceStream inputStream = inputStreamDesc.getInputStream();

                    if (!(inputStream instanceof PushBufferStream))
                        continue;
                    if (!skippedForTransferHandler)
                    {
                        skippedForTransferHandler = true;
                        continue;
                    }
                    if (!(inputStream instanceof CachingPushBufferStream))
                    {
                        PushBufferStream cachingInputStream
                            = new CachingPushBufferStream(
                                    (PushBufferStream) inputStream);

                        inputStreamDesc.setInputStream(cachingInputStream);
                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Created CachingPushBufferStream"
                                        + " with hashCode "
                                        + cachingInputStream.hashCode()
                                        + " for inputStream with hashCode "
                                        + inputStream.hashCode());
                    }
                }

                setTransferHandler(newValue, transferHandler);

                if (logger.isTraceEnabled())
                {
                    int oldValueLength
                        = (oldValue == null) ? 0 : oldValue.length;
                    int newValueLength
                        = (newValue == null) ? 0 : newValue.length;
                    int difference = newValueLength - oldValueLength;

                    if (difference > 0)
                        logger
                            .trace(
                                "Added "
                                    + difference
                                    + " inputStream(s) and the total is "
                                    + newValueLength);
                    else if (difference < 0)
                        logger
                            .trace(
                                "Removed "
                                    + difference
                                    + " inputStream(s) and the total is "
                                    + newValueLength);
                }
            }
        }

        /**
         * Implements
         * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}.
         * Because this instance pushes data to multiple output
         * <tt>AudioMixingPushBufferStreams</tt>, a single
         * <tt>BufferTransferHandler</tt> is not sufficient and thus this method
         * is unsupported and throws <tt>UnsupportedOperationException</tt>.
         *
         * @param transferHandler the <tt>BufferTransferHandler</tt> to be
         * notified by this <tt>PushBufferStream</tt> when media is available
         * for reading
         */
        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            throw
                new UnsupportedOperationException(
                        AudioMixerPushBufferStream.class.getSimpleName()
                            + ".setTransferHandler(BufferTransferHandler)");
        }

        /**
         * Sets a specific <tt>BufferTransferHandler</tt> to a specific
         * collection of <tt>SourceStream</tt>s (in the form of
         * <tt>InputStreamDesc</tt>) abstracting the differences among the
         * various types of <tt>SourceStream</tt>s.
         * 
         * @param inputStreams
         *            the input <tt>SourceStream</tt>s to which the
         *            specified <tt>BufferTransferHandler</tt> is to be set
         * @param transferHandler
         *            the <tt>BufferTransferHandler</tt> to be set to the
         *            specified <tt>inputStreams</tt>
         */
        private void setTransferHandler(
            InputStreamDesc[] inputStreams,
            BufferTransferHandler transferHandler)
        {
            if ((inputStreams == null) || (inputStreams.length <= 0))
                return;

            boolean transferHandlerIsSet = false;

            for (InputStreamDesc inputStreamDesc : inputStreams)
            {
                SourceStream inputStream = inputStreamDesc.getInputStream();

                if (inputStream instanceof PushBufferStream)
                {
                    BufferTransferHandler inputStreamTransferHandler;
                    PushBufferStream inputPushBufferStream
                        = (PushBufferStream) inputStream;

                    if (transferHandler == null)
                        inputStreamTransferHandler = null;
                    else if (transferHandlerIsSet)
                        inputStreamTransferHandler
                            = new BufferTransferHandler()
                                    {
                                        public void transferData(
                                            PushBufferStream stream)
                                        {
                                            /*
                                             * Do nothing because we don't want
                                             * the associated PushBufferStream
                                             * to cause the transfer of data
                                             * from this
                                             * AudioMixerPushBufferStream.
                                             */
                                        }
                                    };
                    else
                        inputStreamTransferHandler
                            = new StreamSubstituteBufferTransferHandler(
                                        transferHandler,
                                        inputPushBufferStream,
                                        this);

                    inputPushBufferStream.setTransferHandler(
                        inputStreamTransferHandler);

                    transferHandlerIsSet = true;
                }
            }
        }

        /**
         * Reads audio samples from the input <tt>SourceStream</tt>s of this
         * instance and pushes them to its output
         * <tt>AudioMixingPushBufferStream</tt>s for audio mixing.
         */
        protected void transferData()
        {
            Buffer buffer = new Buffer();

            try
            {
                read(buffer);
            }
            catch (IOException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }

            InputSampleDesc inputSampleDesc
                = (InputSampleDesc) buffer.getData();
            int[][] inputSamples = inputSampleDesc.inputSamples;
            int maxInputSampleCount = buffer.getLength();

            if ((inputSamples == null)
                    || (inputSamples.length == 0)
                    || (maxInputSampleCount <= 0))
                return;

            AudioMixingPushBufferStream[] outputStreams;

            synchronized (this.outputStreams)
            {
                outputStreams
                    = this.outputStreams.toArray(
                            new AudioMixingPushBufferStream[
                                    this.outputStreams.size()]);
            }
            for (AudioMixingPushBufferStream outputStream : outputStreams)
                setInputSamples(outputStream, inputSampleDesc, maxInputSampleCount);
        }
    }

    /**
     * Describes additional information about a specific input
     * <tt>DataSource</tt> of an <tt>AudioMixer</tt> so that the
     * <tt>AudioMixer</tt> can, for example, quickly discover the output
     * <tt>AudioMixingPushBufferDataSource</tt> in the mix of which the
     * contribution of the <tt>DataSource</tt> is to not be included.
     */
    private static class InputDataSourceDesc
    {

        /**
         * The <tt>DataSource</tt> for which additional information is
         * described by this instance.
         */
        public final DataSource inputDataSource;

        /**
         * The <tt>AudioMixingPushBufferDataSource</tt> in which the
         * mix contributions of the <tt>DataSource</tt> described by this
         * instance are to not be included.
         */
        public final AudioMixingPushBufferDataSource outputDataSource;

        /**
         * The <tt>DataSource</tt>, if any, which transcodes the tracks of
         * <tt>inputDataSource</tt> in the output <tt>Format</tt> of the
         * associated <tt>AudioMixer</tt>.
         */
        private DataSource transcodingDataSource;

        /**
         * Initializes a new <tt>InputDataSourceDesc</tt> instance which is
         * to describe additional information about a specific input
         * <tt>DataSource</tt> of an <tt>AudioMixer</tt>. Associates the
         * specified <tt>DataSource</tt> with the
         * <tt>AudioMixingPushBufferDataSource</tt> in which the mix
         * contributions of the specified input <tt>DataSource</tt> are to
         * not be included.
         * 
         * @param inputDataSource
         *            a <tt>DataSourc</tt> for which additional information
         *            is to be described by the new instance
         * @param outputDataSource
         *            the <tt>AudioMixingPushBufferDataSource</tt> in which
         *            the mix contributions of <tt>inputDataSource</tt> are
         *            to not be included
         */
        public InputDataSourceDesc(
            DataSource inputDataSource,
            AudioMixingPushBufferDataSource outputDataSource)
        {
            this.inputDataSource = inputDataSource;
            this.outputDataSource = outputDataSource;
        }

        /**
         * Gets the actual <tt>DataSource</tt> from which the associated
         * <tt>AudioMixer</tt> directly reads in order to retrieve the mix
         * contribution of the <tt>DataSource</tt> described by this
         * instance.
         * 
         * @return the actual <tt>DataSource</tt> from which the associated
         *         <tt>AudioMixer</tt> directly reads in order to retrieve
         *         the mix contribution of the <tt>DataSource</tt> described
         *         by this instance
         */
        public DataSource getEffectiveInputDataSource()
        {
            return
                (transcodingDataSource == null)
                    ? inputDataSource
                    : transcodingDataSource;
        }

        /**
         * Sets the <tt>DataSource</tt>, if any, which transcodes the tracks
         * of the input <tt>DataSource</tt> described by this instance in
         * the output <tt>Format</tt> of the associated
         * <tt>AudioMixer</tt>.
         * 
         * @param transcodingDataSource
         *            the <tt>DataSource</tt> which transcodes the tracks of
         *            the input <tt>DataSource</tt> described by this
         *            instance in the output <tt>Format</tt> of the
         *            associated <tt>AudioMixer</tt>
         */
        public void setTranscodingDataSource(DataSource transcodingDataSource)
        {
            this.transcodingDataSource = transcodingDataSource;
        }
    }

    /**
     * Describes a specific set of audio samples read from a specific set of
     * input streams specified by their <tt>InputStreamDesc</tt>s.
     */
    private static class InputSampleDesc
    {

        /**
         * The set of audio samples read from {@link #inputStreams}.
         */
        public final int[][] inputSamples;

        /**
         * The set of input streams from which {@link #inputSamples} were read.
         */
        public final InputStreamDesc[] inputStreams;

        /**
         * The time stamp of <tt>inputSamples</tt> to be reported in the
         * <tt>Buffer</tt>s of the <tt>AudioMixingPushBufferStream</tt>s when
         * mixes are read from them.
         */
        private long timeStamp = Buffer.TIME_UNKNOWN;

        /**
         * Initializes a new <tt>InputSampleDesc</tt> instance which is to
         * describe a specific set of audio samples read from a specific set of
         * input streams specified by their <tt>InputStreamDesc</tt>s.
         *
         * @param inputSamples the set of audio samples read from
         * <tt>inputStreams</tt>
         * @param inputStreams the set of input streams from which
         * <tt>inputSamples</tt> were read
         */
        public InputSampleDesc(
                int[][] inputSamples,
                InputStreamDesc[] inputStreams)
        {
            this.inputSamples = inputSamples;
            this.inputStreams = inputStreams;
        }

        /**
         * Gets the time stamp of <tt>inputSamples</tt> to be reported in the
         * <tt>Buffer</tt>s of the <tt>AudioMixingPushBufferStream</tt>s when
         * mixes are read from them.
         *
         * @return the time stamp of <tt>inputSamples</tt> to be reported in the
         * <tt>Buffer</tt>s of the <tt>AudioMixingPushBufferStream</tt>s when
         * mixes are read from them
         */
        public long getTimeStamp()
        {
            return timeStamp;
        }

        /**
         * Sets the time stamp of <tt>inputSamples</tt> to be reported in the
         * <tt>Buffer</tt>s of the <tt>AudioMixingPushBufferStream</tt>s when
         * mixes are read from them.
         *
         * @param timeStamp the time stamp of <tt>inputSamples</tt> to be
         * reported in the <tt>Buffer</tt>s of the
         * <tt>AudioMixingPushBufferStream</tt>s when mixes are read from them
         */
        public void setTimeStamp(long timeStamp)
        {
            if (this.timeStamp == Buffer.TIME_UNKNOWN)
                this.timeStamp = timeStamp;
            else
            {
                /*
                 * Setting the timeStamp more than once does not make sense
                 * because the inputStreams will report different timeStamps so
                 * only one should be picked up where the very reading from
                 * inputStreams takes place.
                 */
                throw new IllegalStateException("timeStamp");
            }
        }
    }

    /**
     * Describes additional information about a specific input audio
     * <tt>SourceStream</tt> of an <tt>AudioMixer</tt> so that the
     * <tt>AudioMixer</tt> can, for example, quickly discover the output
     * <tt>AudioMixingPushBufferDataSource</tt> in the mix of which the
     * contribution of the <tt>SourceStream</tt> is to not be included.
     */
    private static class InputStreamDesc
    {

        /**
         * The <tt>DataSource</tt> which created the
         * <tt>SourceStream</tt> described by this instance and additional
         * information about it.
         */
        private final InputDataSourceDesc inputDataSourceDesc;

        /**
         * The <tt>SourceStream</tt> for which additional information is
         * described by this instance.
         */
        private SourceStream inputStream;

        /**
         * The number of reads of this input stream which did not return any
         * samples.
         */
        long nonContributingReadCount;

        /**
         * Initializes a new <tt>InputStreamDesc</tt> instance which is to
         * describe additional information about a specific input audio
         * <tt>SourceStream</tt> of an <tt>AudioMixer</tt>. Associates
         * the specified <tt>SourceStream</tt> with the
         * <tt>DataSource</tt> which created it and additional information
         * about it.
         * 
         * @param inputStream a <tt>SourceStream</tt> for which additional
         * information is to be described by the new instance
         * @param inputDataSourceDesc the <tt>DataSource</tt> which created the
         * <tt>SourceStream</tt> to be described by the new instance and
         * additional information about it
         */
        public InputStreamDesc(
            SourceStream inputStream,
            InputDataSourceDesc inputDataSourceDesc)
        {
            this.inputStream = inputStream;
            this.inputDataSourceDesc = inputDataSourceDesc;
        }

        /**
         * Gets the input <tt>DataSource</tt> which caused {@link #inputStream}
         * to exist. If input <tt>DataSource</tt> is not transcoded for the
         * purposes of the audio mixing, it has directly provided
         * <tt>inputStream</tt>. Otherwise, it has been wrapped in a
         * <tt>TranscodingDataSource</tt> the latter has provided
         * <tt>inputStream</tt>.
         *
         * @return the input <tt>DataSource</tt> which caused
         * <tt>inputStream</tt> to exist
         */
        public DataSource getInputDataSource()
        {
            return inputDataSourceDesc.inputDataSource;
        }

        /**
         * Gets the <tt>SourceStream</tt> described by this instance
         * 
         * @return the <tt>SourceStream</tt> described by this instance
         */
        public SourceStream getInputStream()
        {
            return inputStream;
        }

        /**
         * Gets the <tt>AudioMixingPushBufferDataSource</tt> in which the
         * mix contribution of the <tt>SourceStream</tt> described by this
         * instance is to not be included.
         * 
         * @return the <tt>AudioMixingPushBufferDataSource</tt> in which the mix
         * contribution of the <tt>SourceStream</tt> described by this instance
         * is to not be included
         */
        public AudioMixingPushBufferDataSource getOutputDataSource()
        {
            return inputDataSourceDesc.outputDataSource;
        }

        /**
         * Sets the <tt>SourceStream</tt> to be described by this instance
         * 
         * @param inputStream the <tt>SourceStream</tt> to be described by this
         * instance
         */
        public void setInputStream(SourceStream inputStream)
        {
            this.inputStream = inputStream;
        }
    }
}
