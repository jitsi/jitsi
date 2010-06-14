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
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a <tt>PushBufferStream</tt> which reads data from the
 * <tt>SourceStream</tt>s of the input <tt>DataSource</tt>s of the associated
 * <tt>AudioMixer</tt> and pushes it to <tt>AudioMixingPushBufferStream</tt>s
 * for audio mixing.
 * <p>
 * Pretty much private to <tt>AudioMixer</tt> but extracted into its own file
 * for the sake of clarity.
 * </p>
 *
 * @author Lubomir Marinov
 */
class AudioMixerPushBufferStream
    extends ControlsAdapter
    implements PushBufferStream
{
    /**
     * The <tt>Logger</tt> used by the <tt>AudioMixerPushBufferStream</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMixerPushBufferStream.class);

    /**
     * The factor which scales a <tt>short</tt> value to an <tt>int</tt> value.
     */
    private static final float INT_TO_SHORT_RATIO
        = Integer.MAX_VALUE / (float) Short.MAX_VALUE;

    /**
     * The factor which scales an <tt>int</tt> value to a <tt>short</tt> value.
     */
    private static final float SHORT_TO_INT_RATIO
        = Short.MAX_VALUE / (float) Integer.MAX_VALUE;

    /**
     * The number of reads from an input stream with no returned samples which
     * do not get reported in tracing output. Once the number of such reads from
     * an input stream exceeds this limit, it gets reported and the counting is
     * restarted.
     */
    private static final long TRACE_NON_CONTRIBUTING_READ_COUNT = 0;

    /**
     * The <tt>AudioMixer</tt> which created this
     * <tt>AudioMixerPushBufferStream</tt>.
     */
    private final AudioMixer audioMixer;

    /**
     * The <tt>SourceStream</tt>s (in the form of <tt>InputStreamDesc</tt> so
     * that this instance can track back the
     * <tt>AudioMixingPushBufferDataSource</tt> which outputs the mixed audio
     * stream and determine whether the associated <tt>SourceStream</tt> is to
     * be included into the mix) from which this instance reads its data.
     */
    private InputStreamDesc[] inputStreams;

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #inputStreams}-related members.
     */
    private final Object inputStreamsSyncRoot = new Object();

    /**
     * The <tt>AudioFormat</tt> of the <tt>Buffer</tt> read during the last read
     * from one of the {@link #inputStreams}. Only used for debugging purposes.
     */
    private AudioFormat lastReadInputFormat;

    /**
     * The <tt>AudioFormat</tt> of the data this instance outputs.
     */
    private final AudioFormat outputFormat;

    /**
     * The <tt>AudioMixingPushBufferStream</tt>s to which this instance pushes
     * data for audio mixing.
     */
    private final List<AudioMixingPushBufferStream> outputStreams
        = new ArrayList<AudioMixingPushBufferStream>();

    /**
     * The <tt>BufferTransferHandler</tt> through which this instance gets
     * notifications from its input <tt>SourceStream</tt>s that new data is
     * available for audio mixing.
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
     * Initializes a new <tt>AudioMixerPushBufferStream</tt> instance to output
     * data in a specific <tt>AudioFormat</tt> for a specific
     * <tt>AudioMixer</tt>.
     *
     * @param audioMixer the <tt>AudioMixer</tt> which creates this instance and
     * for which it is to output data
     * @param outputFormat the <tt>AudioFormat</tt> in which the new instance is
     * to output data
     */
    public AudioMixerPushBufferStream(
            AudioMixer audioMixer,
            AudioFormat outputFormat)
    {
        this.audioMixer = audioMixer;
        this.outputFormat = outputFormat;
    }

    /**
     * Adds a specific <tt>AudioMixingPushBufferStream</tt> to the collection of
     * such streams to which this instance is to push the data for audio mixing
     * it reads from its input <tt>SourceStream</tt>s.
     *
     * @param outputStream the <tt>AudioMixingPushBufferStream</tt> to add to
     * the collection of such streams to which this instance is to push the data
     * for audio mixing it reads from its input <tt>SourceStream</tt>s
     * @throws IOException if <tt>outputStream</tt> was the first
     * <tt>AudioMixingPushBufferStream</tt> and the <tt>AudioMixer</tt> failed
     * to start
     */
    void addOutputStream(AudioMixingPushBufferStream outputStream)
        throws IOException
    {
        if (outputStream == null)
            throw new IllegalArgumentException("outputStream");

        synchronized (outputStreams)
        {
            if (!outputStreams.contains(outputStream)
                    && outputStreams.add(outputStream)
                    && (outputStreams.size() == 1))
            {
                boolean started = false;

                try
                {
                    audioMixer.start(this);
                    started = true;
                }
                finally
                {
                    if (!started)
                        outputStreams.remove(outputStream);
                }
            }
        }
    }

    /**
     * Implements {@link SourceStream#endOfStream()}. Delegates to the input
     * <tt>SourceStreams</tt> of this instance.
     *
     * @return <tt>true</tt> if all input <tt>SourceStream</tt>s of this
     * instance have reached the end of their content; <tt>false</tt>, otherwise
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
     * Attempts to equalize the length in milliseconds of the buffering
     * performed by the <tt>inputStreams</tt> in order to always read and mix
     * one and the same length in milliseconds.
     */
    void equalizeInputStreamBufferLength()
    {
        synchronized (inputStreamsSyncRoot)
        {
            if ((inputStreams == null) || (inputStreams.length < 1))
                return;

            /*
             * The first inputStream is expected to be from the CaptureDevice
             * and no custom BufferControl is provided for it so the
             * bufferLength is whatever it says.
             */
            BufferControl bufferControl = getBufferControl(inputStreams[0]);
            long bufferLength
                = (bufferControl == null)
                    ? CachingPushBufferStream.DEFAULT_BUFFER_LENGTH
                    : bufferControl.getBufferLength();

            for (int i = 1; i < inputStreams.length; i++)
            {
                BufferControl inputStreamBufferControl
                    = getBufferControl(inputStreams[i]);

                if (inputStreamBufferControl != null)
                    inputStreamBufferControl.setBufferLength(bufferLength);
            }
        }
    }

    /**
     * Gets the <tt>BufferControl<tt> of a specific input stream. The returned
     * <tt>BufferControl</tt> may be available through its input
     * <tt>DataSource</tt>, its transcoding <tt>DataSource</tt> if any or the
     * very input stream.
     *
     * @param inputStreamDesc an <tt>InputStreamDesc</tt> which describes the
     * input stream and its originating <tt>DataSource</tt>s from which the
     * <tt>BufferControl</tt> is to be retrieved
     * @return the <tt>BufferControl</tt> of the specified input stream found in
     * its input <tt>DataSource</tt>, its transcoding <tt>DataSource</tt> if any
     * or the very input stream if such a control exists; otherwise,
     * <tt>null</tt>
     */
    private BufferControl getBufferControl(InputStreamDesc inputStreamDesc)
    {
        InputDataSourceDesc inputDataSourceDesc
            = inputStreamDesc.inputDataSourceDesc;

        // Try the DataSource which directly provides the specified inputStream.
        DataSource effectiveInputDataSource
            = inputDataSourceDesc.getEffectiveInputDataSource();
        String bufferControlType = BufferControl.class.getName();

        if (effectiveInputDataSource != null)
        {
            BufferControl bufferControl
                = (BufferControl)
                    effectiveInputDataSource.getControl(bufferControlType);

            if (bufferControl != null)
                return bufferControl;
        }

        /*
         * If transcoding is taking place and the transcodingDataSource does not
         * have a BufferControl, try the inputDataSource which is being
         * transcoded.
         */
        DataSource inputDataSource = inputDataSourceDesc.inputDataSource;

        if ((inputDataSource != null)
                && (inputDataSource != effectiveInputDataSource))
        {
            BufferControl bufferControl
                = (BufferControl) inputDataSource.getControl(bufferControlType);

            if (bufferControl != null)
                return bufferControl;
        }

        // If everything else has failed, try the very inputStream.
        return
            (BufferControl)
                inputStreamDesc.getInputStream().getControl(bufferControlType);
    }

    /**
     * Implements {@link SourceStream#getContentDescriptor()}. Returns a
     * <tt>ContentDescriptor</tt> which describes the content type of this
     * instance.
     *
     * @return a <tt>ContentDescriptor</tt> which describes the content type of
     * this instance
     */
    public ContentDescriptor getContentDescriptor()
    {
        return new ContentDescriptor(audioMixer.getContentType());
    }

    /**
     * Implements {@link SourceStream#getContentLength()}. Delegates to the
     * input <tt>SourceStreams</tt> of this instance.
     *
     * @return the length of the content made available by this instance which
     * is the maximum length of the contents made available by its input
     * <tt>StreamSource</tt>s
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
     * Implements {@link PushBufferStream#getFormat()}. Returns the
     * <tt>AudioFormat</tt> in which this instance was configured to output its
     * data.
     *
     * @return the <tt>AudioFormat</tt> in which this instance was configured to
     * output its data
     */
    public AudioFormat getFormat()
    {
        return outputFormat;
    }

    /**
     * Gets the <tt>SourceStream</tt>s (in the form of
     * <tt>InputStreamDesc</tt>s) from which this instance reads audio samples.
     *
     * @return an array of <tt>InputStreamDesc</tt>s which describe the input
     * <tt>SourceStream</tt>s from which this instance reads audio samples
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
     * formats, converts the read audio samples to one and the same format and
     * pushes them to the output <tt>AudioMixingPushBufferStream</tt>s for the
     * very audio mixing.
     *
     * @param buffer the <tt>Buffer</tt> in which the audio samples read from
     * the input <tt>SourceStream</tt>s are to be returned to the caller
     * @throws IOException if any of the input <tt>SourceStream</tt>s throws
     * such an exception while reading from them or anything else goes wrong
     */
    public void read(Buffer buffer)
        throws IOException
    {
        InputStreamDesc[] inputStreams;

        synchronized (inputStreamsSyncRoot)
        {
            inputStreams
                = (this.inputStreams == null)
                    ? null
                    : this.inputStreams.clone();
        }

        int inputStreamCount = (inputStreams == null) ? 0 : inputStreams.length;

        if (inputStreamCount <= 0)
            return;

        AudioFormat outputFormat = getFormat();
        InputSampleDesc inputSampleDesc
            = new InputSampleDesc(new int[inputStreamCount][], inputStreams);
        int maxInputSampleCount;

        try
        {
            maxInputSampleCount
                = readInputPushBufferStreams(outputFormat, inputSampleDesc);
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
     * instance and converts them to a specific output <tt>AudioFormat</tt>. An
     * attempt is made to read a specific maximum number of samples from each of
     * the <tt>PullBufferStream</tt>s but the very <tt>PullBufferStream</tt> may
     * not honor the request.
     *
     * @param outputFormat the <tt>AudioFormat</tt> in which the audio samples
     * read from the <tt>PullBufferStream</tt>s are to be converted before being
     * returned
     * @param outputSampleCount the maximum number of audio samples to be read
     * from each of the <tt>PullBufferStream</tt>s but the very
     * <tt>PullBufferStream</tt>s may not honor the request
     * @param inputSampleDesc an <tt>InputStreamDesc</tt> which specifies the
     * input streams to be read and the collection of audio samples in which the
     * read audio samples are to be returned
     * @return the maximum number of audio samples actually read from the input
     * <tt>PullBufferStream</tt>s of this instance
     * @throws IOException if anything goes wrong while reading the specified
     * input streams
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
     * converts them to a specific output <tt>AudioFormat</tt>. An attempt is
     * made to read a specific maximum number of samples from the specified
     * <tt>PushBufferStream</tt> but the very <tt>PushBufferStream</tt> may not
     * honor the request.
     *
     * @param inputStreamDesc an <tt>InputStreamDesc</tt> which specifies the
     * input <tt>PushBufferStream</tt> to read from
     * @param outputFormat the <tt>AudioFormat</tt> to which the samples read
     * from <tt>inputStream</tt> are to be converted before being returned
     * @param sampleCount the maximum number of samples which the read operation
     * should attempt to read from <tt>inputStream</tt> but the very
     * <tt>inputStream</tt> may not honor the request
     * @return a <tt>Buffer</tt> which contains the array of <tt>int</tt> audio
     * samples read from the specified <tt>inputStream</tt>
     * @throws IOException if anything wrong happens while reading
     * <tt>inputStream</tt>
     * @throws UnsupportedFormatException if converting the samples read from
     * <tt>inputStream</tt> to <tt>outputFormat</tt> fails
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

        audioMixer
            .read(
                inputStream,
                buffer,
                inputStreamDesc.inputDataSourceDesc.inputDataSource);

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
     * Reads audio samples from the input <tt>PushBufferStream</tt>s of this
     * instance and converts them to a specific output <tt>AudioFormat</tt>.
     *
     * @param outputFormat the <tt>AudioFormat</tt> in which the audio samples
     * read from the <tt>PushBufferStream</tt>s are to be converted before being
     * returned
     * @param inputSampleDesc an <tt>InputSampleDesc</tt> which specifies the
     * input streams to be read and  the collection of audio samples in which
     * the read audio samples are to be returned
     * @return the maximum number of audio samples actually read from the input
     * <tt>PushBufferStream</tt>s of this instance
     * @throws IOException if anything wrong happens while reading the specified
     * input streams
     * @throws UnsupportedFormatException if any of the input streams provides
     * media in a format different than <tt>outputFormat</tt>
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
                                .setTimeStamp(inputStreamBuffer.getTimeStamp());
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
     * Reads an integer from a specific series of bytes starting the reading at
     * a specific offset in it.
     *
     * @param input the series of bytes to read an integer from
     * @param inputOffset the offset in <tt>input</tt> at which the reading of
     * the integer is to start
     * @return an integer read from the specified series of bytes starting at
     * the specified offset in it
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
     * Removes a specific <tt>AudioMixingPushBufferStream</tt> from the
     * collection of such streams to which this instance pushes the data for
     * audio mixing it reads from its input <tt>SourceStream</tt>s.
     *
     * @param outputStream the <tt>AudioMixingPushBufferStream</tt> to remove
     * from the collection of such streams to which this instance pushes the
     * data for audio mixing it reads from its input <tt>SourceStream</tt>s
     * @throws IOException if <tt>outputStream</tt> was the last
     * <tt>AudioMixingPushBufferStream</tt> and the <tt>AudioMixer</tt> failed
     * to stop
     */
    void removeOutputStream(AudioMixingPushBufferStream outputStream)
        throws IOException
    {
        synchronized (outputStreams)
        {
            if ((outputStream != null)
                    && outputStreams.remove(outputStream)
                    && outputStreams.isEmpty())
                audioMixer.stop(this);
        }
    }

    /**
     * Pushes a copy of a specific set of input audio samples to a specific
     * <tt>AudioMixingPushBufferStream</tt> for audio mixing. Audio samples read
     * from input <tt>DataSource</tt>s which the
     * <tt>AudioMixingPushBufferDataSource</tt> owner of the specified
     * <tt>AudioMixingPushBufferStream</tt> has specified to not be included in
     * the output mix are not pushed to the
     * <tt>AudioMixingPushBufferStream</tt>.
     *
     * @param outputStream the <tt>AudioMixingPushBufferStream</tt> to push the
     * specified set of audio samples to
     * @param inputSampleDesc the set of audio samples to be pushed to
     * <tt>outputStream</tt> for audio mixing
     * @param maxInputSampleCount the maximum number of audio samples available
     * in <tt>inputSamples</tt>
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
        boolean outputDataSourceIsMute = outputDataSource.isMute();

        for (int i = 0; i < inputSamples.length; i++)
        {
            InputStreamDesc inputStreamDesc = inputStreams[i];

            if (outputDataSource.equals(inputStreamDesc.getOutputDataSource()))
            {
                inputSamples[i] = null;
            }
            else if (outputDataSourceIsMute
                    && (inputStreamDesc.inputDataSourceDesc.inputDataSource
                            == audioMixer.captureDevice))
            {
                inputSamples[i] = null;
            }
        }

        outputStream
            .setInputSamples(
                inputSamples,
                maxInputSampleCount,
                inputSampleDesc.getTimeStamp());
    }

    /**
     * Sets the <tt>SourceStream</tt>s (in the form of <tt>InputStreamDesc</tt>)
     * from which this instance is to read audio samples and push them to the
     * <tt>AudioMixingPushBufferStream</tt>s for audio mixing.
     *
     * @param inputStreams the <tt>SourceStream</tt>s (in the form of
     * <tt>InputStreamDesc</tt>) from which this instance is to read audio
     * samples and push them to the <tt>AudioMixingPushBufferStream</tt>s for
     * audio mixing
     */
    void setInputStreams(Collection<InputStreamDesc> inputStreams)
    {
        InputStreamDesc[] oldValue;
        InputStreamDesc[] newValue
            = inputStreams.toArray(new InputStreamDesc[inputStreams.size()]);

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
            equalizeInputStreamBufferLength();

            if (logger.isTraceEnabled())
            {
                int oldValueLength = (oldValue == null) ? 0 : oldValue.length;
                int newValueLength = (newValue == null) ? 0 : newValue.length;
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
     * <tt>BufferTransferHandler</tt> is not sufficient and thus this method is
     * unsupported and throws <tt>UnsupportedOperationException</tt>.
     *
     * @param transferHandler the <tt>BufferTransferHandler</tt> to be notified
     * by this <tt>PushBufferStream</tt> when media is available for reading
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        throw
            new UnsupportedOperationException(
                    AudioMixerPushBufferStream.class.getSimpleName()
                        + ".setTransferHandler(BufferTransferHandler)");
    }

    /**
     * Sets a specific <tt>BufferTransferHandler</tt> to a specific collection
     * of <tt>SourceStream</tt>s (in the form of <tt>InputStreamDesc</tt>)
     * abstracting the differences among the various types of
     * <tt>SourceStream</tt>s.
     *
     * @param inputStreams the input <tt>SourceStream</tt>s to which the
     * specified <tt>BufferTransferHandler</tt> is to be set
     * @param transferHandler the <tt>BufferTransferHandler</tt> to be set to
     * the specified <tt>inputStreams</tt>
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
                    inputStreamTransferHandler = new BufferTransferHandler()
                    {
                        public void transferData(PushBufferStream stream)
                        {
                            /*
                             * Do nothing because we don't want the associated
                             * PushBufferStream to cause the transfer of data
                             * from this AudioMixerPushBufferStream.
                             */
                        }
                    };
                else
                    inputStreamTransferHandler
                        = new StreamSubstituteBufferTransferHandler(
                                    transferHandler,
                                    inputPushBufferStream,
                                    this);

                inputPushBufferStream
                    .setTransferHandler(inputStreamTransferHandler);

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

        InputSampleDesc inputSampleDesc = (InputSampleDesc) buffer.getData();
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
}
