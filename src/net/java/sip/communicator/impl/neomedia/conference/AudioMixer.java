/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * The <tt>BufferControl</tt> of this instance and, respectively, its
     * <tt>AudioMixingPushBufferDataSource</tt>s.
     */
    private BufferControl bufferControl;

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
        {
            captureDevice
                = new PushBufferDataSourceAdapter(
                        (PullBufferDataSource) captureDevice);
        }

        // Try to enable tracing on captureDevice.
        if (logger.isTraceEnabled())
        {
            captureDevice
                = MediaDeviceImpl
                    .createTracingCaptureDevice(captureDevice, logger);
        }

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
            throw new NullPointerException("inputDataSource");

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
                        inputDataSourceDesc.connect(this);
                    }
                    catch (IOException ioex)
                    {
                        throw new UndeclaredThrowableException(ioex);
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
                {
                    try
                    {
                        inputDataSourceDesc.start();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
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
     * @throws IOException if input/output error occurred
     */
    void connect()
        throws IOException
    {
        synchronized (inputDataSources)
        {
            if (connected == 0)
            {
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    try
                    {
                        inputDataSourceDesc.connect(this);
                    }
                    catch (IOException ioe)
                    {
                        logger
                            .error(
                                "Failed to connect to inputDataSource "
                                    + MediaStreamImpl
                                        .toString(
                                            inputDataSourceDesc
                                                .inputDataSource),
                                ioe);
                        throw ioe;
                    }

                /*
                 * Since the media of the input streams is to be mixed, their
                 * bufferLengths have to be equal. After a DataSource is
                 * connected, its BufferControl is available and its
                 * bufferLength may change so make sure that the bufferLengths
                 * of the input streams are equal.
                 */
                if (outputStream != null)
                    outputStream.equalizeInputStreamBufferLength();
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
     * Notifies this <tt>AudioMixer</tt> that a specific input
     * <tt>DataSource</tt> has finished its connecting procedure. Primarily
     * meant for input <tt>DataSource</tt> which have their connecting executed
     * in a separate thread as are, for example, input <tt>DataSource</tt>s
     * which are being transcoded.
     *
     * @param inputDataSource the <tt>InputDataSourceDesc</tt> of the input
     * <tt>DataSource</tt> which has finished its connecting procedure
     * @throws IOException if anything wrong happens while including
     * <tt>inputDataSource</tt> into the mix
     */
    void connected(InputDataSourceDesc inputDataSource)
        throws IOException
    {
        synchronized (inputDataSources)
        {
            if (inputDataSources.contains(inputDataSource)
                    && (connected > 0))
            {
                if (started > 0)
                    inputDataSource.start();
                if (outputStream != null)
                    getOutputStream();
            }
        }
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
     * Creates a <tt>DataSource</tt> which attempts to transcode the tracks of a
     * specific input <tt>DataSource</tt> into a specific output
     * <tt>Format</tt>.
     *
     * @param inputDataSourceDesc the <tt>InputDataSourceDesc</tt> describing
     * the input <tt>DataSource</tt> to be transcoded into the specified output
     * <tt>Format</tt> and to receive the transcoding <tt>DataSource</tt>
     * @param outputFormat the <tt>Format</tt> in which the tracks of the input
     * <tt>DataSource</tt> are to be transcoded
     * @return <tt>true</tt> if a new transcoding <tt>DataSource</tt> has been
     * created for the input <tt>DataSource</tt> described by
     * <tt>inputDataSourceDesc</tt>; otherwise, <tt>false</tt>
     * @throws IOException if an error occurs while creating the transcoding
     * <tt>DataSource</tt>, connecting to it or staring it
     */
    private boolean createTranscodingDataSource(
            InputDataSourceDesc inputDataSourceDesc,
            Format outputFormat)
        throws IOException
    {
        if (inputDataSourceDesc.createTranscodingDataSource(outputFormat))
        {
            if (connected > 0)
                inputDataSourceDesc.connect(this);
            if (started > 0)
                inputDataSourceDesc.start();
            return true;
        }
        else
            return false;
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
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    inputDataSourceDesc.disconnect();

                /*
                 * XXX Make the outputStream to release the inputStreams.
                 * Otherwise, the PushBufferStream ones which have been wrapped
                 * into CachingPushBufferStream may remaing waiting.
                 */
                outputStream.setInputStreams(null);
                outputStream = null;
            }
        }
    }

    /**
     * Gets the <tt>BufferControl</tt> of this instance and, respectively, its
     * <tt>AudioMixingPushBufferDataSource</tt>s.
     *
     * @return the <tt>BufferControl</tt> of this instance and, respectively,
     * its <tt>AudioMixingPushBufferDataSource</tt>s if such a control is
     * available for the <tt>CaptureDevice</tt> of this instance; otherwise,
     * <tt>null</tt>
     */
    BufferControl getBufferControl()
    {
        if ((bufferControl == null) && (captureDevice instanceof Controls))
        {
            BufferControl captureDeviceBufferControl
                = (BufferControl)
                    ((Controls) captureDevice)
                        .getControl(BufferControl.class.getName());

            if (captureDeviceBufferControl != null)
                bufferControl
                    = new ReadOnlyBufferControlDelegate(
                            captureDeviceBufferControl);
        }
        return bufferControl;
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

        if (!OSUtils.IS_ANDROID && (formatControls != null))
            for (int i = 0; i < formatControls.length; i++)
                formatControls[i]
                    = new ReadOnlyFormatControlDelegate(formatControls[i]);
        return formatControls;
    }

    /**
     * Gets the <tt>SourceStream</tt>s (in the form of <tt>InputStreamDesc</tt>)
     * of a specific <tt>DataSource</tt> (provided in the form of
     * <tt>InputDataSourceDesc</tt>) which produce data in a specific
     * <tt>AudioFormat</tt> (or a matching one).
     *
     * @param inputDataSourceDesc the <tt>DataSource</tt> (in the form of
     * <tt>InputDataSourceDesc</tt>) which is to be examined for
     * <tt>SourceStreams</tt> producing data in the specified
     * <tt>AudioFormat</tt>
     * @param outputFormat the <tt>AudioFormat</tt> in which the collected
     * <tt>SourceStream</tt>s are to produce data
     * @param existingInputStreams the <tt>InputStreamDesc</tt> instances which
     * already exist and which are used to avoid creating multiple
     * <tt>InputStreamDesc</tt>s for input <tt>SourceStream</tt>s which already
     * have ones
     * @param inputStreams the <tt>List</tt> of <tt>InputStreamDesc</tt> in
     * which the discovered <tt>SourceStream</tt>s are to be returned
     * @return <tt>true</tt> if <tt>SourceStream</tt>s produced by the specified
     * input <tt>DataSource</tt> and outputting data in the specified
     * <tt>AudioFormat</tt> were discovered and reported in
     * <tt>inputStreams</tt>; otherwise, <tt>false</tt>
     */
    private boolean getInputStreamsFromInputDataSource(
        InputDataSourceDesc inputDataSourceDesc,
        AudioFormat outputFormat,
        InputStreamDesc[] existingInputStreams,
        List<InputStreamDesc> inputStreams)
    {
        SourceStream[] inputDataSourceStreams
            = inputDataSourceDesc.getStreams();

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

        DataSource inputDataSource
            = inputDataSourceDesc.getEffectiveInputDataSource();

        if (inputDataSource == null)
            return false;

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
     * Gets the <tt>SourceStream</tt>s (in the form of <tt>InputStreamDesc</tt>)
     * of the <tt>DataSource</tt>s from which this <tt>AudioMixer</tt> reads
     * data which produce data in a specific <tt>AudioFormat</tt>. When an input
     * <tt>DataSource</tt> does not have such <tt>SourceStream</tt>s, an attempt
     * is made to transcode its tracks so that such <tt>SourceStream</tt>s can
     * be retrieved from it after transcoding.
     *
     * @param outputFormat the <tt>AudioFormat</tt> in which the retrieved
     * <tt>SourceStream</tt>s are to produce data
     * @param existingInputStreams the <tt>SourceStream</tt>s which are already
     * known to this <tt>AudioMixer</tt>
     * @return a new collection of <tt>SourceStream</tt>s (in the form of
     * <tt>InputStreamDesc</tt>) retrieved from the input <tt>DataSource</tt>s
     * of this <tt>AudioMixer</tt> and producing data in the specified
     * <tt>AudioFormat</tt>
     * @throws IOException if anything wrong goes while retrieving the input
     * <tt>SourceStream</tt>s from the input <tt>DataSource</tt>s
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

                if (!got
                        && createTranscodingDataSource(
                                inputDataSourceDesc,
                                outputFormat))
                    getInputStreamsFromInputDataSource(
                        inputDataSourceDesc,
                        outputFormat,
                        existingInputStreams,
                        inputStreams);
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
                DataSource effectiveInputDataSource
                    = inputDataSource.getEffectiveInputDataSource();

                if (effectiveInputDataSource == null)
                    continue;

                FormatControl formatControl
                    = (FormatControl)
                        effectiveInputDataSource.getControl(formatControlType);

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
            AudioFormat outputFormat
                = (outputStream == null)
                    ? getOutputFormatFromInputDataSources()
                    : outputStream.getFormat();

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
            catch (IOException ioex)
            {
                throw new UndeclaredThrowableException(ioex);
            }

            if (outputStream == null)
                outputStream
                    = new AudioMixerPushBufferStream(this, outputFormat);
            outputStream.setInputStreams(inputStreams);
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
     * Removes <tt>DataSource</tt>s accepted by a specific
     * <tt>DataSourceFilter</tt> from the list of input <tt>DataSource</tt>s of
     * this <tt>AudioMixer</tt> from which it reads audio to be mixed.
     *
     * @param dataSourceFilter the <tt>DataSourceFilter</tt> which selects the
     * <tt>DataSource</tt>s to be removed from the list of input
     * <tt>DataSource</tt>s of this <tt>AudioMixer</tt> from which it reads
     * audio to be mixed
     */
    public void removeInputDataSources(DataSourceFilter dataSourceFilter)
    {
        synchronized (inputDataSources)
        {
            Iterator<InputDataSourceDesc> inputDataSourceIter
                = inputDataSources.iterator();
            boolean removed = false;

            while (inputDataSourceIter.hasNext())
            {
                if (dataSourceFilter
                        .accept(inputDataSourceIter.next().inputDataSource))
                {
                    inputDataSourceIter.remove();
                    removed = true;
                }
            }
            if (removed && (outputStream != null))
                getOutputStream();
        }
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
                FormatControl formatControl
                    = (FormatControl)
                        inputDataSourceDesc.getControl(formatControlType);

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
     * @param outputStream the <tt>AudioMixerPushBufferStream</tt> which
     * requests this <tt>AudioMixer</tt> to start. If <tt>outputStream</tt> is
     * the current one and only <tt>AudioMixerPushBufferStream</tt> of this
     * <tt>AudioMixer</tt>, this <tt>AudioMixer</tt> starts if it hasn't started
     * yet. Otherwise, the request is ignored.
     * @throws IOException if any of the input <tt>DataSource</tt>s of this
     * <tt>AudioMixer</tt> throws such an exception while attempting to start it
     */
    void start(AudioMixerPushBufferStream outputStream)
        throws IOException
    {
        synchronized (inputDataSources)
        {
            /*
             * AudioMixer has only one outputStream at a time and only its
             * current outputStream known when it has to start (and stop).
             */
            if (this.outputStream != outputStream)
                return;

            if (started == 0)
            {
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    inputDataSourceDesc.start();
            }

            started++;
        }
    }

    /**
     * Stops the input <tt>DataSource</tt>s of this <tt>AudioMixer</tt>.
     *
     * @param outputStream the <tt>AudioMixerPushBufferStream</tt> which
     * requests this <tt>AudioMixer</tt> to stop. If <tt>outputStream</tt> is
     * the current one and only <tt>AudioMixerPushBufferStream</tt> of this
     * <tt>AudioMixer</tt>, this <tt>AudioMixer</tt> stops. Otherwise, the
     * request is ignored.
     * @throws IOException if any of the input <tt>DataSource</tt>s of this
     * <tt>AudioMixer</tt> throws such an exception while attempting to stop it
     */
    void stop(AudioMixerPushBufferStream outputStream)
        throws IOException
    {
        synchronized (inputDataSources)
        {
            /*
             * AudioMixer has only one outputStream at a time and only its
             * current outputStream known when it has to stop (and start).
             */
            if (this.outputStream != outputStream)
                return;

            if (started <= 0)
                return;

            started--;

            if (started == 0)
            {
                for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                    inputDataSourceDesc.stop();
            }
        }
    }
}
