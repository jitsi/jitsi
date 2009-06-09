/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.conference;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.media.*;

/**
 * Represents an audio mixer which manages the mixing of multiple audio streams
 * i.e. it is able to output a single audio stream which contains the audio of
 * multiple input audio streams.
 * <p>
 * The input audio streams are provided to the <code>AudioMixer</code> through
 * {@link #addInputDataSource(DataSource)} in the form of input
 * <code>DataSource</code>s giving access to one or more input
 * <code>SourceStreams</code>.
 * </p>
 * <p>
 * The output audio stream representing the mix of the multiple input audio
 * streams is provided by the <code>AudioMixer</code> in the form of a
 * <code>AudioMixingPushBufferDataSource</code> giving access to a
 * <code>AudioMixingPushBufferStream</code>. Such an output is obtained through
 * {@link #createOutputDataSource()}. The <code>AudioMixer</code> is able to
 * provide multiple output audio streams at one and the same time, though, each
 * of them containing the mix of a subset of the input audio streams.
 * </p>
 * 
 * @author Lubomir Marinov
 */
public class AudioMixer
{

    /**
     * The default output <code>AudioFormat</code> in which
     * <code>AudioMixer</code>, <code>AudioMixingPushBufferDataSource</code> and
     * <code>AudioMixingPushBufferStream</code> output audio.
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
     * The <code>CaptureDevice</code> capabilities provided by the
     * <code>AudioMixingPushBufferDataSource</code>s created by this
     * <code>AudioMixer</code>. JMF's
     * <code>Manager.createMergingDataSource(DataSource[])</code> requires the
     * interface implementation for audio if it is implemented for video and it
     * is indeed the case for our use case of
     * <code>AudioMixingPushBufferDataSource</code>.
     */
    private final CaptureDevice captureDevice;

    /**
     * The number of output <code>AudioMixingPushBufferDataSource</code>s
     * reading from this <code>AudioMixer</code> which are connected. When the
     * value is greater than zero, this <code>AudioMixer</code> is connected to
     * the input <code>DataSource</code>s it manages.
     */
    private int connected;

    /**
     * The collection of input <code>DataSource</code>s this instance reads
     * audio data from.
     */
    private final List<InputDataSourceDesc> inputDataSources
        = new ArrayList<InputDataSourceDesc>();

    /**
     * The output <code>AudioMixerPushBufferStream</code> through which this
     * instance pushes audio sample data to
     * <code>AudioMixingPushBufferStream</code>s to be mixed.
     */
    private AudioMixerPushBufferStream outputStream;

    /**
     * Initializes a new <code>AudioMixer</code> instance. Because JMF's
     * <code>Manager.createMergingDataSource(DataSource[])</code> requires the
     * implementation of <code>CaptureDevice</code> for audio if it is
     * implemented for video and it is indeed the cause for our use case of
     * <code>AudioMixingPushBufferDataSource</code>, the new
     * <code>AudioMixer</code> instance provides specified
     * <code>CaptureDevice</code> capabilities to the
     * <code>AudioMixingPushBufferDataSource</code>s it creates. The specified
     * <code>CaptureDevice</code> is also added as the first input
     * <code>DataSource</code> of the new instance.
     * 
     * @param captureDevice the <code>CaptureDevice</code> capabilities to be
     *            provided to the <code>AudioMixingPushBufferDataSource</code>s
     *            created by the new instance and its first input
     *            <code>DataSource</code>
     */
    public AudioMixer(CaptureDevice captureDevice)
    {
        this.captureDevice = captureDevice;

        addInputDataSource((DataSource) captureDevice);
    }

    /**
     * Adds a new input <code>DataSource</code> to the collection of input
     * <code>DataSource</code>s from which this instance reads audio. If the
     * specified <code>DataSource</code> indeed provides audio, the respective
     * contributions to the mix are always included.
     * 
     * @param inputDataSource a new <code>DataSource</code> to input audio to
     *            this instance
     */
    public void addInputDataSource(DataSource inputDataSource)
    {
        addInputDataSource(inputDataSource, null);
    }

    /**
     * Adds a new input <code>DataSource</code> to the collection of input
     * <code>DataSource</code>s from which this instance reads audio. If the
     * specified <code>DataSource</code> indeed provides audio, the respective
     * contributions to the mix will be excluded from the mix output provided
     * through a specific <code>AudioMixingPushBufferDataSource</code>.
     * 
     * @param inputDataSource a new <code>DataSource</code> to input audio to
     *            this instance
     * @param outputDataSource the <code>AudioMixingPushBufferDataSource</code>
     *            to not include the audio contributions of
     *            <code>inputDataSource</code> in the mix it outputs
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
     * Notifies this <code>AudioMixer</code> that an output
     * <code>AudioMixingPushBufferDataSource</code> reading from it has been
     * connected. The first of the many
     * <code>AudioMixingPushBufferDataSource</code>s reading from this
     * <code>AudioMixer</code> which gets connected causes it to connect to the
     * input <code>DataSource</code>s it manages.
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
     * Creates a new <code>AudioMixingPushBufferDataSource</code> which gives
     * access to a single audio stream representing the mix of the audio streams
     * input into this <code>AudioMixer</code> through its input
     * <code>DataSource</code>s. The returned
     * <code>AudioMixingPushBufferDataSource</code> can also be used to include
     * new input <code>DataSources</code> in this <code>AudioMixer</code> but
     * have their contributions not included in the mix available through the
     * returned <code>AudioMixingPushBufferDataSource</code>.
     * 
     * @return a new <code>AudioMixingPushBufferDataSource</code> which gives
     *         access to a single audio stream representing the mix of the audio
     *         streams input into this <code>AudioMixer</code> through its input
     *         <code>DataSource</code>s
     */
    public AudioMixingPushBufferDataSource createOutputDataSource()
    {
        return new AudioMixingPushBufferDataSource(this);
    }

    /**
     * Creates a <code>DataSource</code> which attempts to transcode the tracks
     * of a specific input <code>DataSource</code> into a specific output
     * <code>Format</code>.
     * 
     * @param inputDataSource
     *            the <code>DataSource</code> from the tracks of which data is
     *            to be read and transcoded into the specified output
     *            <code>Format</code>
     * @param outputFormat
     *            the <code>Format</code> in which the tracks of
     *            <code>inputDataSource</code> are to be transcoded
     * @return a new <code>DataSource</code> which attempts to transcode the
     *         tracks of <code>inputDataSource</code> into
     *         <code>outputFormat</code>
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
     * Notifies this <code>AudioMixer</code> that an output
     * <code>AudioMixingPushBufferDataSource</code> reading from it has been
     * disconnected. The last of the many
     * <code>AudioMixingPushBufferDataSource</code>s reading from this
     * <code>AudioMixer</code> which gets disconnected causes it to disconnect
     * from the input <code>DataSource</code>s it manages.
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
     * Gets the <code>CaptureDeviceInfo</code> of the <code>CaptureDevice</code>
     * this <code>AudioMixer</code> provides through its output
     * <code>AudioMixingPushBufferDataSource</code>s.
     * 
     * @return the <code>CaptureDeviceInfo</code> of the
     *         <code>CaptureDevice</code> this <code>AudioMixer</code> provides
     *         through its output <code>AudioMixingPushBufferDataSource</code>s
     */
    CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return captureDevice.getCaptureDeviceInfo();
    }

    /**
     * Gets the content type of the data output by this <code>AudioMixer</code>.
     * 
     * @return the content type of the data output by this
     *         <code>AudioMixer</code>
     */
    String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Gets the duration of each one of the output streams produced by this
     * <code>AudioMixer</code>.
     * 
     * @return the duration of each one of the output streams produced by this
     *         <code>AudioMixer</code>
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
     * Gets the <code>Format</code> in which a specific <code>DataSource</code>
     * provides stream data.
     * 
     * @param dataSource
     *            the <code>DataSource</code> for which the <code>Format</code>
     *            in which it provides stream data is to be determined
     * @return the <code>Format</code> in which the specified
     *         <code>dataSource</code> provides stream data if it was
     *         determined; otherwise, <tt>null</tt>
     */
    private static Format getFormat(DataSource dataSource)
    {
        FormatControl formatControl
            = (FormatControl) dataSource.getControl(
                    FormatControl.class.getName());

        return (formatControl == null) ? null : formatControl.getFormat();
    }

    /**
     * Gets the <code>Format</code> in which a specific
     * <code>SourceStream</code> provides data.
     * 
     * @param stream
     *            the <code>SourceStream</code> for which the
     *            <code>Format</code> in which it provides data is to be
     *            determined
     * @return the <code>Format</code> in which the specified
     *         <code>SourceStream</code> provides data if it was determined;
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
     * Gets an array of <code>FormatControl</code>s for the
     * <code>CaptureDevice</code> this <code>AudioMixer</code> provides through
     * its output <code>AudioMixingPushBufferDataSource</code>s.
     * 
     * @return an array of <code>FormatControl</code>s for the
     *         <code>CaptureDevice</code> this <code>AudioMixer</code> provides
     *         through its output <code>AudioMixingPushBufferDataSource</code>s
     */
    FormatControl[] getFormatControls()
    {
        return captureDevice.getFormatControls();
    }

    /**
     * Gets the <code>SourceStream</code>s (in the form of
     * <code>InputStreamDesc</code>) of a specific <code>DataSource</code>
     * (provided in the form of <code>InputDataSourceDesc</code>) which produce
     * data in a specific <code>AudioFormat</code> (or a matching one).
     * 
     * @param inputDataSourceDesc
     *            the <code>DataSource</code> (in the form of
     *            <code>InputDataSourceDesc</code>) which is to be examined for
     *            <code>SourceStreams</code> producing data in the specified
     *            <code>AudioFormat</code>
     * @param outputFormat
     *            the <code>AudioFormat</code> in which the collected
     *            <code>SourceStream</code>s are to produce data
     * @param inputStreams
     *            the <code>List</code> of <code>InputStreamDesc</code> in which
     *            the discovered <code>SourceStream</code>s are to be returned
     * @return <tt>true</tt> if <code>SourceStream</code>s produced by the
     *         specified input <code>DataSource</code> and outputing data in the
     *         specified <code>AudioFormat</code> were discovered and reported
     *         in <code>inputStreams</code>; otherwise, <tt>false</tt>
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
     * Gets the <code>SourceStream</code>s (in the form of
     * <code>InputStreamDesc</code>) of the <code>DataSource</code>s from which
     * this <code>AudioMixer</code> reads data which produce data in a specific
     * <code>AudioFormat</code>. When an input <code>DataSource</code> does not
     * have such <code>SourceStream</code>s, an attempt is made to transcode its
     * tracks so that such <code>SourceStream</code>s can be retrieved from it
     * after transcoding.
     * 
     * @param outputFormat
     *            the <code>AudioFormat</code> in which the retrieved
     *            <code>SourceStream</code>s are to produce data
     * @return a new collection of <code>SourceStream</code>s (in the form of
     *         <code>InputStreamDesc</code>) retrieved from the input
     *         <code>DataSource</code>s of this <code>AudioMixer</code> and
     *         producing data in the specified <code>AudioFormat</code>
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
     * Gets the <code>AudioFormat</code> in which the input
     * <code>DataSource</code>s of this <code>AudioMixer</code> can produce data
     * and which is to be the output <code>Format</code> of this
     * <code>AudioMixer</code>.
     * 
     * @return the <code>AudioFormat</code> in which the input
     *         <code>DataSource</code>s of this <code>AudioMixer</code> can
     *         produce data and which is to be the output <code>Format</code> of
     *         this <code>AudioMixer</code>
     */
    private AudioFormat getOutputFormatFromInputDataSources()
    {
        // TODO Auto-generated method stub
        return DEFAULT_OUTPUT_FORMAT;
    }

    /**
     * Gets the <code>AudioMixerPushBufferStream</code>, first creating it if it
     * does not exist already, which reads data from the input
     * <code>DataSource</code>s of this <code>AudioMixer</code> and pushes it to
     * output <code>AudioMixingPushBufferStream</code>s for audio mixing.
     * 
     * @return the <code>AudioMixerPushBufferStream</code> which reads data from
     *         the input <code>DataSource</code>s of this
     *         <code>AudioMixer</code> and pushes it to output
     *         <code>AudioMixingPushBufferStream</code>s for audio mixing
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
     * Determines whether a specific <code>Format</code> matches a specific
     * <code>Format</code> in the sense of JMF <code>Format</code> matching.
     * Since this <code>AudioMixer</code> and the audio mixing functionality
     * related to it can handle varying characteristics of a certain output
     * <code>Format</code>, the only requirement for the specified
     * <code>Format</code>s to match is for both of them to have one and the
     * same encoding.
     * 
     * @param input
     *            the <code>Format</code> for which it is required to determine
     *            whether it matches a specific <code>Format</code>
     * @param pattern
     *            the <code>Format</code> against which the specified
     *            <code>input</code> is to be matched
     * @return <tt>true</tt> if the specified
     *         <code>input<code> matches the specified <code>pattern</code> in
     *         the sense of JMF <code>Format</code> matching; otherwise,
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
     *            the offset in <code>input</code> at which the reading of the
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
     * Reads a short integer from a specific series of bytes starting the
     * reading at a specific offset in it.
     * 
     * @param input
     *            the series of bytes to read the short integer from
     * @param inputOffset
     *            the offset in <code>input</code> at which the reading of the
     *            short integer is to start
     * @return a short integer in the form of
     *         <tt>int</code> read from the specified series of bytes starting at the specified offset in it
     */
    private static int readShort(byte[] input, int inputOffset)
    {
        return
            (short)
                ((input[inputOffset + 1] << 8)
                    | (input[inputOffset] & 0x00FF));
    }

    /**
     * Sets a specific <code>AudioFormat</code>, if possible, as the output
     * format of the input <code>DataSource</code>s of this
     * <code>AudioMixer</code> in an attempt to not have to perform explicit
     * transcoding of the input <code>SourceStream</code>s.
     * 
     * @param outputFormat
     *            the <code>AudioFormat</code> in which the input
     *            <code>DataSource</code>s of this <code>AudioMixer</code> are
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
     * Starts the input <code>DataSource</code>s of this <code>AudioMixer</code>.
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
     * Stops the input <code>DataSource</code>s of this <code>AudioMixer</code>.
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
     * Represents a <code>PushBufferStream</code> which reads data from the
     * <code>SourceStream</code>s of the input <code>DataSource</code>s of this
     * <code>AudioMixer</code> and pushes it to
     * <code>AudioMixingPushBufferStream</code>s for audio mixing.
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
         * The <code>SourceStream</code>s (in the form of
         * <code>InputStreamDesc</code> so that this instance can track back the
         * <code>AudioMixingPushBufferDataSource</code> which outputs the mixed
         * audio stream and determine whether the associated
         * <code>SourceStream</code> is to be included into the mix) from which
         * this instance reads its data.
         */
        private InputStreamDesc[] inputStreams;

        /**
         * The <code>AudioFormat</code> of the data this instance outputs.
         */
        private final AudioFormat outputFormat;

        /**
         * The <code>AudioMixingPushBufferStream</code>s to which this instance
         * pushes data for audio mixing.
         */
        private final List<AudioMixingPushBufferStream> outputStreams
            = new ArrayList<AudioMixingPushBufferStream>();

        /**
         * The <code>BufferTransferHandler</code> through which this instance
         * gets notifications from its input <code>SourceStream</code>s that new
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
         * Initializes a new <code>AudioMixerPushBufferStream</code> instance to
         * output data in a specific <code>AudioFormat</code>.
         * 
         * @param outputFormat
         *            the <code>AudioFormat</code> in which the new instance is
         *            to output data
         */
        private AudioMixerPushBufferStream(AudioFormat outputFormat)
        {
            this.outputFormat = outputFormat;
        }

        /**
         * Adds a specific <code>AudioMixingPushBufferStream</code> to the
         * collection of such streams which this instance is to push the data
         * for audio mixing it reads from its input <code>SourceStream</code>s.
         * 
         * @param outputStream
         *            the <code>AudioMixingPushBufferStream</code> to be added
         *            to the collection of such streams which this instance is
         *            to push the data for audio mixing it reads from its input
         *            <code>SourceStream</code>s
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
         * Reads audio samples from a specific <code>PushBufferStream</code> and
         * converts them to a specific output <code>AudioFormat</code>. An
         * attempt is made to read a specific maximum number of samples from the
         * specified <code>PushBufferStream</code> but the very
         * <code>PushBufferStream</code> may not honor the request.
         * 
         * @param inputStream
         *            the <code>PushBufferStream</code> to read from
         * @param outputFormat
         *            the <code>AudioFormat</code> to which the samples read
         *            from <code>inputStream</code> are to converted before
         *            being returned
         * @param sampleCount
         *            the maximum number of samples which the read operation
         *            should attempt to read from <code>inputStream</code> but
         *            the very <code>inputStream</code> may not honor the
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
                        int sample = readShort(inputSamples, i * 2);
        
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
         * Reads audio samples from the input <code>PullBufferStream</code>s of
         * this instance and converts them to a specific output
         * <code>AudioFormat</code>. An attempt is made to read a specific
         * maximum number of samples from each of the
         * <code>PullBufferStream</code>s but the very
         * <code>PullBufferStream</code> may not honor the request.
         * 
         * @param outputFormat
         *            the <code>AudioFormat</code> in which the audio samples
         *            read from the <code>PullBufferStream</code>s are to be
         *            converted before being returned
         * @param outputSampleCount
         *            the maximum number of audio samples to be read from each
         *            of the <code>PullBufferStream</code>s but the very
         *            <code>PullBufferStream</code> may not honor the request
         * @param inputSamples
         *            the collection of audio samples in which the read audio
         *            samples are to be returned
         * @return the maximum number of audio samples actually read from the
         *         input <code>PullBufferStream</code>s of this instance
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
         * Reads audio samples from the input <code>PushBufferStream</code>s of
         * this instance and converts them to a specific output
         * <code>AudioFormat</code>.
         * 
         * @param outputFormat
         *            the <code>AudioFormat</code> in which the audio samples
         *            read from the <code>PushBufferStream</code>s are to be
         *            converted before being returned
         * @param inputSamples
         *            the collection of audio samples in which the read audio
         *            samples are to be returned
         * @return the maximum number of audio samples actually read from the
         *         input <code>PushBufferStream</code>s of this instance
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
         * Removes a specific <code>AudioMixingPushBufferStream</code> from the
         * collection of such streams which this instance pushes the data for
         * audio mixing it reads from its input <code>SourceStream</code>s.
         * 
         * @param outputStream
         *            the <code>AudioMixingPushBufferStream</code> to be removed
         *            from the collection of such streams which this instance
         *            pushes the data for audio mixing it reads from its input
         *            <code>SourceStream</code>s
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
         * <code>AudioMixingPushBufferStream</code> for audio mixing. Audio
         * samples read from input <code>DataSource</code>s which the
         * <code>AudioMixingPushBufferDataSource</code> owner of the specified
         * <code>AudioMixingPushBufferStream</code> has specified to not be
         * included in the output mix are not pushed to the
         * <code>AudioMixingPushBufferStream</code>.
         * 
         * @param outputStream
         *            the <code>AudioMixingPushBufferStream</code> to push the
         *            specified set of audio samples to
         * @param inputSamples
         *            the set of audio samples to be pushed to
         *            <code>outputStream</code> for audio mixing
         * @param maxInputSampleCount
         *            the maximum number of audio samples available in
         *            <code>inputSamples</code>
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
         * Sets the <code>SourceStream</code>s (in the form of
         * <code>InputStreamDesc</code>) from which this instance is to read
         * audio samples and push them to the
         * <code>AudioMixingPushBufferStream</code>s for audio mixing.
         * 
         * @param inputStreams
         *            the <code>SourceStream</code>s (in the form of
         *            <code>InputStreamDesc</code>) from which this instance is
         *            to read audio samples and push them to the
         *            <code>AudioMixingPushBufferStream</code>s for audio mixing
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
         * Sets a specific <code>BufferTransferHandler</code> to a specific
         * collection of <code>SourceStream</code>s (in the form of
         * <code>InputStreamDesc</code>) abstracting the differences among the
         * various types of <code>SourceStream</code>s.
         * 
         * @param inputStreams
         *            the input <code>SourceStream</code>s to which the
         *            specified <code>BufferTransferHandler</code> is to be set
         * @param transferHandler
         *            the <code>BufferTransferHandler</code> to be set to the
         *            specified <code>inputStreams</code>
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
         * Reads audio samples from the input <code>SourceStream</code>s of this
         * instance and pushes them to its output
         * <code>AudioMixingPushBufferStream</code>s for audio mixing.
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
     * <code>DataSource</code> of an <code>AudioMixer</code> so that the
     * <code>AudioMixer</code> can, for example, quickly discover the output
     * <code>AudioMixingPushBufferDataSource</code> in the mix of which the
     * contribution of the <code>DataSource</code> is to not be included.
     */
    private static class InputDataSourceDesc
    {

        /**
         * The <code>DataSource</code> for which additional information is
         * described by this instance.
         */
        public final DataSource inputDataSource;

        /**
         * The <code>AudioMixingPushBufferDataSource</code> in which the
         * mix contributions of the <code>DataSource</code> described by this
         * instance are to not be included.
         */
        public final AudioMixingPushBufferDataSource outputDataSource;

        /**
         * The <code>DataSource</code>, if any, which transcodes the tracks of
         * <code>inputDataSource</code> in the output <code>Format</code> of the
         * associated <code>AudioMixer</code>.
         */
        private DataSource transcodingDataSource;

        /**
         * Initializes a new <code>InputDataSourceDesc</code> instance which is
         * to describe additional information about a specific input
         * <code>DataSource</code> of an <code>AudioMixer</code>. Associates the
         * specified <code>DataSource</code> with the
         * <code>AudioMixingPushBufferDataSource</code> in which the mix
         * contributions of the specified input <code>DataSource</code> are to
         * not be included.
         * 
         * @param inputDataSource
         *            a <code>DataSourc</code> for which additional information
         *            is to be described by the new instance
         * @param outputDataSource
         *            the <code>AudioMixingPushBufferDataSource</code> in which
         *            the mix contributions of <code>inputDataSource</code> are
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
         * Gets the actual <code>DataSource</code> from which the associated
         * <code>AudioMixer</code> directly reads in order to retrieve the mix
         * contribution of the <code>DataSource</code> described by this
         * instance.
         * 
         * @return the actual <code>DataSource</code> from which the associated
         *         <code>AudioMixer</code> directly reads in order to retrieve
         *         the mix contribution of the <code>DataSource</code> described
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
         * Sets the <code>DataSource</code>, if any, which transcodes the tracks
         * of the input <code>DataSource</code> described by this instance in
         * the output <code>Format</code> of the associated
         * <code>AudioMixer</code>.
         * 
         * @param transcodingDataSource
         *            the <code>DataSource</code> which transcodes the tracks of
         *            the input <code>DataSource</code> described by this
         *            instance in the output <code>Format</code> of the
         *            associated <code>AudioMixer</code>
         */
        public void setTranscodingDataSource(DataSource transcodingDataSource)
        {
            this.transcodingDataSource = transcodingDataSource;
        }
    }

    /**
     * Describes additional information about a specific input audio
     * <code>SourceStream</code> of an <code>AudioMixer</code> so that the
     * <code>AudioMixer</code> can, for example, quickly discover the output
     * <code>AudioMixingPushBufferDataSource</code> in the mix of which the
     * contribution of the <code>SourceStream</code> is to not be included.
     */
    private static class InputStreamDesc
    {

        /**
         * The <code>DataSource</code> which created the
         * <code>SourceStream</code> described by this instance and additional
         * information about it.
         */
        private final InputDataSourceDesc inputDataSourceDesc;

        /**
         * The <code>SourceStream</code> for which additional information is
         * described by this instance.
         */
        private SourceStream inputStream;

        /**
         * Initializes a new <code>InputStreamDesc</code> instance which is to
         * describe additional information about a specific input audio
         * <code>SourceStream</code> of an <code>AudioMixer</code>. Associates
         * the specified <code>SourceStream</code> with the
         * <code>DataSource</code> which created it and additional information
         * about it.
         * 
         * @param inputStream
         *            a <code>SourceStream</code> for which additional
         *            information is to be described by the new instance
         * @param inputDataSourceDesc
         *            the <code>DataSource</code> which created the
         *            <code>SourceStream</code> to be described by the new
         *            instance and additional information about it
         */
        public InputStreamDesc(
            SourceStream inputStream,
            InputDataSourceDesc inputDataSourceDesc)
        {
            this.inputStream = inputStream;
            this.inputDataSourceDesc = inputDataSourceDesc;
        }

        /**
         * Gets the <code>SourceStream</code> described by this instance
         * 
         * @return the <code>SourceStream</code> described by this instance
         */
        public SourceStream getInputStream()
        {
            return inputStream;
        }

        /**
         * Gets the <code>AudioMixingPushBufferDataSource</code> in which the
         * mix contribution of the <code>SourceStream</code> described by this
         * instance is to not be included.
         * 
         * @return the <code>AudioMixingPushBufferDataSource</code> in which the
         *         mix contribution of the <code>SourceStream</code> described
         *         by this instance is to not be included
         */
        public AudioMixingPushBufferDataSource getOutputDataSource()
        {
            return inputDataSourceDesc.outputDataSource;
        }

        /**
         * Sets the <code>SourceStream</code> to be described by this instance
         * 
         * @param inputStream
         *            the <code>SourceStream</code> to be described by this
         *            instance
         */
        public void setInputStream(SourceStream inputStream)
        {
            this.inputStream = inputStream;
        }
    }
}
