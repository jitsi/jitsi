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
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;

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
     * The default output <tt>AudioFormat</tt> in which <tt>AudioMixer</tt>,
     * <tt>AudioMixingPushBufferDataSource</tt> and
     * <tt>AudioMixingPushBufferStream</tt> output audio.
     */
    private static final AudioFormat DEFAULT_OUTPUT_FORMAT
        = new AudioFormat(
                AudioFormat.LINEAR,
                44100,
                16,
                2,
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
    private final CaptureDevice captureDevice;

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

        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
            if (inputDataSource.equals(inputDataSourceDesc.inputDataSource))
                throw new IllegalArgumentException("inputDataSource");

        inputDataSources.add(
            new InputDataSourceDesc(
                    inputDataSource,
                    outputDataSource));
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
        if (connected == 0)
            for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                inputDataSourceDesc.getEffectiveInputDataSource().connect();

        connected++;
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
        if (connected <= 0)
            return;

        connected--;

        if (connected == 0)
        {
            outputStream = null;

            for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
                inputDataSourceDesc.getEffectiveInputDataSource().disconnect();
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
     * Gets the duration of each one of the output streams produced by this
     * <tt>AudioMixer</tt>.
     * 
     * @return the duration of each one of the output streams produced by this
     * <tt>AudioMixer</tt>
     */
    Time getDuration()
    {
        Time duration = null;

        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
        {
            Time inputDuration
                = inputDataSourceDesc
                        .getEffectiveInputDataSource().getDuration();

            if (Duration.DURATION_UNBOUNDED.equals(inputDuration)
                    || Duration.DURATION_UNKNOWN.equals(inputDuration))
                return inputDuration;

            if ((duration == null)
                    || (duration.getNanoseconds() < inputDuration.getNanoseconds()))
                duration = inputDuration;
        }
        return (duration == null) ? Duration.DURATION_UNKNOWN : duration;
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
        return captureDevice.getFormatControls();
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
     * @param inputStreams
     *            the <tt>List</tt> of <tt>InputStreamDesc</tt> in which
     *            the discovered <tt>SourceStream</tt>s are to be returned
     * @return <tt>true</tt> if <tt>SourceStream</tt>s produced by the
     *         specified input <tt>DataSource</tt> and outputing data in the
     *         specified <tt>AudioFormat</tt> were discovered and reported
     *         in <tt>inputStreams</tt>; otherwise, <tt>false</tt>
     */
    private boolean getInputStreamsFromInputDataSource(
        InputDataSourceDesc inputDataSourceDesc,
        AudioFormat outputFormat,
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
                        && matches(inputFormat, outputFormat)
                        && inputStreams.add(
                                new InputStreamDesc(
                                        inputStream,
                                        inputDataSourceDesc)))
                    added = true;
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
                    inputStreams.add(
                        new InputStreamDesc(
                                new PushBufferStreamAdapter(
                                        inputStream,
                                        inputFormat),
                                inputDataSourceDesc));
                return true;
            }
            if (inputDataSource instanceof PullDataSource)
            {
                for (PullSourceStream inputStream
                        : ((PullDataSource) inputDataSource).getStreams())
                    inputStreams.add(
                        new InputStreamDesc(
                                new PullBufferStreamAdapter(
                                        inputStream,
                                        inputFormat),
                                inputDataSourceDesc));
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
     * @return a new collection of <tt>SourceStream</tt>s (in the form of
     *         <tt>InputStreamDesc</tt>) retrieved from the input
     *         <tt>DataSource</tt>s of this <tt>AudioMixer</tt> and
     *         producing data in the specified <tt>AudioFormat</tt>
     * @throws IOException
     */
    private Collection<InputStreamDesc> getInputStreamsFromInputDataSources(
            AudioFormat outputFormat)
        throws IOException
    {
        List<InputStreamDesc> inputStreams = new ArrayList<InputStreamDesc>();

        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
        {
            boolean got
                = getInputStreamsFromInputDataSource(
                        inputDataSourceDesc,
                        outputFormat,
                        inputStreams);

            if (!got)
            {
                DataSource transcodingDataSource
                    = createTranscodingDataSource(
                            inputDataSourceDesc.getEffectiveInputDataSource(),
                            outputFormat);

                if (transcodingDataSource != null)
                {
                    inputDataSourceDesc.setTranscodingDataSource(
                        transcodingDataSource);

                    getInputStreamsFromInputDataSource(
                        inputDataSourceDesc,
                        outputFormat,
                        inputStreams);
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
        // TODO Auto-generated method stub
        return DEFAULT_OUTPUT_FORMAT;
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
        AudioFormat outputFormat = getOutputFormatFromInputDataSources();

        setOutputFormatToInputDataSources(outputFormat);

        Collection<InputStreamDesc> inputStreams;

        try
        {
            inputStreams = getInputStreamsFromInputDataSources(outputFormat);
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

        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
        {
            DataSource inputDataSource
                = inputDataSourceDesc.getEffectiveInputDataSource();
            FormatControl formatControl
                = (FormatControl) inputDataSource.getControl(formatControlType);

            if (formatControl != null)
            {
                Format inputFormat = formatControl.getFormat();

                if ((inputFormat == null)
                        || !matches(inputFormat, outputFormat))
                    formatControl.setFormat(outputFormat);
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
        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
            inputDataSourceDesc.getEffectiveInputDataSource().start();
    }

    /**
     * Stops the input <tt>DataSource</tt>s of this <tt>AudioMixer</tt>.
     * 
     * @throws IOException
     */
    void stop()
        throws IOException
    {
        for (InputDataSourceDesc inputDataSourceDesc : inputDataSources)
            inputDataSourceDesc.getEffectiveInputDataSource().stop();
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
         * The <tt>SourceStream</tt>s (in the form of
         * <tt>InputStreamDesc</tt> so that this instance can track back the
         * <tt>AudioMixingPushBufferDataSource</tt> which outputs the mixed
         * audio stream and determine whether the associated
         * <tt>SourceStream</tt> is to be included into the mix) from which
         * this instance reads its data.
         */
        private InputStreamDesc[] inputStreams;

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

        /*
         * Implements SourceStream#endOfStream(). Delegates to the input
         * SourceStreams of this instance.
         */
        public boolean endOfStream()
        {
            if (inputStreams != null)
                for (InputStreamDesc inputStreamDesc : inputStreams)
                    if (!inputStreamDesc.getInputStream().endOfStream())
                        return false;
            return true;
        }

        /*
         * Implements SourceStream#getContentDescriptor(). Returns a
         * ContentDescriptor which describes the content type of this instance.
         */
        public ContentDescriptor getContentDescriptor()
        {
            return
                new ContentDescriptor(AudioMixer.this.getContentType());
        }

        /*
         * Implements SourceStream#getContentLength(). Delegates to the input
         * SourceStreams of this instance.
         */
        public long getContentLength()
        {
            long contentLength = 0;

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
            return contentLength;
        }

        /*
         * Implements Controls#getControl(String). Does nothing.
         */
        public Object getControl(String controlType)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * Implements Controls#getControls(). Does nothing.
         */
        public Object[] getControls()
        {
            // TODO Auto-generated method stub
            return new Object[0];
        }

        /*
         * Implements PushBufferStream#getFormat(). Returns the output
         * AudioFormat in which this instance was configured to output its data.
         */
        public AudioFormat getFormat()
        {
            return outputFormat;
        }

        /*
         * Implements PushBufferStream#read(Buffer). Reads audio samples from
         * the input SourceStreams of this instance in various formats, converts
         * the read audio samples to one and the same format and pushes them to
         * the output AudioMixingPushBufferStreams for the very audio mixing.
         */
        public void read(Buffer buffer)
            throws IOException
        {
            int inputStreamCount
                = (inputStreams == null) ? 0 : inputStreams.length;

            if (inputStreamCount <= 0)
                return;

            AudioFormat outputFormat = getFormat();
            int[][] inputSamples = new int[inputStreamCount][];
            int maxInputSampleCount;

            try
            {
                maxInputSampleCount
                    = readInputPushBufferStreams(outputFormat, inputSamples);
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
                            inputSamples));

            buffer.setData(inputSamples);
            buffer.setLength(maxInputSampleCount);
        }

        /**
         * Reads audio samples from a specific <tt>PushBufferStream</tt> and
         * converts them to a specific output <tt>AudioFormat</tt>. An
         * attempt is made to read a specific maximum number of samples from the
         * specified <tt>PushBufferStream</tt> but the very
         * <tt>PushBufferStream</tt> may not honor the request.
         * 
         * @param inputStream
         *            the <tt>PushBufferStream</tt> to read from
         * @param outputFormat
         *            the <tt>AudioFormat</tt> to which the samples read
         *            from <tt>inputStream</tt> are to converted before
         *            being returned
         * @param sampleCount
         *            the maximum number of samples which the read operation
         *            should attempt to read from <tt>inputStream</tt> but
         *            the very <tt>inputStream</tt> may not honor the
         *            request
         * @return
         * @throws IOException
         * @throws UnsupportedFormatException
         */
        private int[] read(
                PushBufferStream inputStream,
                AudioFormat outputFormat,
                int sampleCount)
            throws IOException,
                   UnsupportedFormatException
        {
            Buffer buffer = new Buffer();
        
            if (sampleCount != 0)
            {
                AudioFormat inputFormat = (AudioFormat) inputStream.getFormat();
                Class<?> inputDataType = inputFormat.getDataType();
        
                if (Format.byteArray.equals(inputDataType))
                {
                    buffer.setData(
                        new byte[
                                sampleCount
                                    * (inputFormat.getSampleSizeInBits() / 8)]);
                    buffer.setLength(0);
                    buffer.setOffset(0);
                }
                else
                    throw
                        new UnsupportedFormatException(
                                "!Format.getDataType().equals(byte[].class)",
                                inputFormat);
            }
        
            inputStream.read(buffer);
        
            int inputLength = buffer.getLength();
        
            if (inputLength <= 0)
                return null;
        
            AudioFormat inputFormat = (AudioFormat) buffer.getFormat();
        
            if (inputFormat.getSigned() != AudioFormat.SIGNED)
                throw
                    new UnsupportedFormatException(
                            "AudioFormat.getSigned()",
                            inputFormat);
            if (inputFormat.getChannels() != outputFormat.getChannels())
                throw
                    new UnsupportedFormatException(
                            "AudioFormat.getChannels()",
                            inputFormat);
        
            Object inputData = buffer.getData();
        
            if (inputData instanceof byte[])
            {
                byte[] inputSamples = (byte[]) inputData;
                int[] outputSamples;
                int outputSampleSizeInBits = outputFormat.getSampleSizeInBits();
        
                switch (inputFormat.getSampleSizeInBits())
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
                    return outputSamples;
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
                    return outputSamples;
                case 8:
                case 24:
                default:
                    throw
                        new UnsupportedFormatException(
                                "AudioFormat.getSampleSizeInBits()",
                                inputFormat);
                }
            }
            else if (inputData instanceof short[])
            {
                throw
                    new UnsupportedFormatException(
                            "Format.getDataType().equals(short[].class)",
                            inputFormat);
            }
            else if (inputData instanceof int[])
            {
                throw
                    new UnsupportedFormatException(
                            "Format.getDataType().equals(int[].class)",
                            inputFormat);
            }
            return null;
        }

        /**
         * Reads audio samples from the input <tt>PullBufferStream</tt>s of
         * this instance and converts them to a specific output
         * <tt>AudioFormat</tt>. An attempt is made to read a specific
         * maximum number of samples from each of the
         * <tt>PullBufferStream</tt>s but the very
         * <tt>PullBufferStream</tt> may not honor the request.
         * 
         * @param outputFormat
         *            the <tt>AudioFormat</tt> in which the audio samples
         *            read from the <tt>PullBufferStream</tt>s are to be
         *            converted before being returned
         * @param outputSampleCount
         *            the maximum number of audio samples to be read from each
         *            of the <tt>PullBufferStream</tt>s but the very
         *            <tt>PullBufferStream</tt> may not honor the request
         * @param inputSamples
         *            the collection of audio samples in which the read audio
         *            samples are to be returned
         * @return the maximum number of audio samples actually read from the
         *         input <tt>PullBufferStream</tt>s of this instance
         * @throws IOException
         */
        private int readInputPullBufferStreams(
                AudioFormat outputFormat,
                int outputSampleCount,
                int[][] inputSamples)
            throws IOException
        {
            int maxInputSampleCount = 0;
        
            for (InputStreamDesc inputStreamDesc : inputStreams)
                if (inputStreamDesc.getInputStream() instanceof PullBufferStream)
                    throw
                        new UnsupportedOperationException(
                                AudioMixerPushBufferStream.class.getSimpleName()
                                    + ".readInputPullBufferStreams(AudioFormat,int,int[][])");
            return maxInputSampleCount;
        }

        /**
         * Reads audio samples from the input <tt>PushBufferStream</tt>s of
         * this instance and converts them to a specific output
         * <tt>AudioFormat</tt>.
         * 
         * @param outputFormat
         *            the <tt>AudioFormat</tt> in which the audio samples
         *            read from the <tt>PushBufferStream</tt>s are to be
         *            converted before being returned
         * @param inputSamples
         *            the collection of audio samples in which the read audio
         *            samples are to be returned
         * @return the maximum number of audio samples actually read from the
         *         input <tt>PushBufferStream</tt>s of this instance
         * @throws IOException
         * @throws UnsupportedFormatException
         */
        private int readInputPushBufferStreams(
                AudioFormat outputFormat,
                int[][] inputSamples)
            throws IOException,
                   UnsupportedFormatException
        {
            int maxInputSampleCount = 0;
        
            for (int i = 0; i < inputStreams.length; i++)
            {
                SourceStream inputStream = inputStreams[i].getInputStream();
        
                if (inputStream instanceof PushBufferStream)
                {
                    int[] inputStreamSamples
                        = read(
                                (PushBufferStream) inputStream,
                                outputFormat,
                                maxInputSampleCount);
        
                    if (inputStreamSamples != null)
                    {
                        int inputStreamSampleCount = inputStreamSamples.length;
        
                        if (inputStreamSampleCount != 0)
                        {
                            inputSamples[i] = inputStreamSamples;
        
                            if (maxInputSampleCount < inputStreamSampleCount)
                                maxInputSampleCount = inputStreamSampleCount;
                        }
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
         * @param inputSamples
         *            the set of audio samples to be pushed to
         *            <tt>outputStream</tt> for audio mixing
         * @param maxInputSampleCount
         *            the maximum number of audio samples available in
         *            <tt>inputSamples</tt>
         */
        private void setInputSamples(
            AudioMixingPushBufferStream outputStream,
            int[][] inputSamples,
            int maxInputSampleCount)
        {
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

            outputStream.setInputSamples(inputSamples, maxInputSampleCount);
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
            InputStreamDesc[] oldValue = this.inputStreams;
            InputStreamDesc[] newValue
                = inputStreams.toArray(
                        new InputStreamDesc[inputStreams.size()]);
            boolean valueIsChanged = !Arrays.equals(oldValue, newValue);

            if (valueIsChanged)
                setTransferHandler(oldValue, null);
            this.inputStreams = newValue;
            if (valueIsChanged)
            {
                boolean skippedForTransferHandler = false;

                for (InputStreamDesc inputStreamDesc : newValue)
                {
                   SourceStream inputStream = inputStreamDesc.getInputStream();

                   if (inputStream instanceof PushBufferStream)
                   {
                       if (!skippedForTransferHandler)
                           skippedForTransferHandler = true;
                       else if (!(inputStream instanceof CachingPushBufferStream))
                           inputStreamDesc.setInputStream(
                               new CachingPushBufferStream(
                                       (PushBufferStream) inputStream));
                   }
                }

                setTransferHandler(newValue, transferHandler);
            }
        }

        /*
         * Implements PushBufferStream#setTransferHandler(BufferTransferHandler).
         * Because this instance pushes data to multiple output
         * AudioMixingPushBufferStreams, a single BufferTransferHandler is not
         * sufficient and thus this method is unsupported.
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

            int[][] inputSamples = (int[][]) buffer.getData();
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
                setInputSamples(outputStream, inputSamples, maxInputSampleCount);
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
