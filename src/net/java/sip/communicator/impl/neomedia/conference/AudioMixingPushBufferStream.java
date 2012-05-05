/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a <tt>PushBufferStream</tt> containing the result of the audio
 * mixing of <tt>DataSource</tt>s.
 *
 * @author Lyubomir Marinov
 */
public class AudioMixingPushBufferStream
    extends ControlsAdapter
    implements PushBufferStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>AudioMixingPushBufferStream</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMixingPushBufferStream.class);

    /**
     * The <tt>AudioMixerPushBufferStream</tt> which reads data from the input
     * <tt>DataSource</tt>s and pushes it to this instance to be mixed.
     */
    private final AudioMixerPushBufferStream audioMixerStream;

    /**
     * The <tt>AudioMixingPushBufferDataSource</tt> which created and owns this
     * instance and defines the input data which is to not be mixed in the
     * output of this <tt>PushBufferStream</tt>.
     */
    private final AudioMixingPushBufferDataSource dataSource;

    /**
     * The collection of input audio samples still not mixed and read through
     * this <tt>AudioMixingPushBufferStream</tt>.
     */
    private int[][] inputSamples;

    /**
     * The maximum number of per-stream audio samples available through
     * <tt>inputSamples</tt>.
     */
    private int maxInputSampleCount;

    /**
     * The <tt>Object</tt> which synchronizes the access to the data to be read
     * from this <tt>PushBufferStream</tt> i.e. to {@link #inputSamples},
     * {@link #maxInputSampleCount} and {@link #timeStamp}.
     */
    private final Object readSyncRoot = new Object();

    /**
     * The time stamp of {@link #inputSamples} to be reported in the specified
     * <tt>Buffer</tt> when data is read from this instance.
     */
    private long timeStamp;

    /**
     * The <tt>BufferTransferHandler</tt> through which this
     * <tt>PushBufferStream</tt> notifies its clients that new data is available
     * for reading.
     */
    private BufferTransferHandler transferHandler;

    /**
     * Initializes a new <tt>AudioMixingPushBufferStream</tt> mixing the
     * input data of a specific <tt>AudioMixerPushBufferStream</tt> and
     * excluding from the mix the audio contributions of a specific
     * <tt>AudioMixingPushBufferDataSource</tt>.
     *
     * @param audioMixerStream the <tt>AudioMixerPushBufferStream</tt> reading
     * data from input <tt>DataSource</tt>s and to push it to the new
     * <tt>AudioMixingPushBufferStream</tt>
     * @param dataSource the <tt>AudioMixingPushBufferDataSource</tt> which has
     * requested the initialization of the new instance and which defines the
     * input data to not be mixed in the output of the new instance
     */
    AudioMixingPushBufferStream(
        AudioMixerPushBufferStream audioMixerStream,
        AudioMixingPushBufferDataSource dataSource)
    {
        this.audioMixerStream = audioMixerStream;
        this.dataSource = dataSource;
    }

    /**
     * Implements {@link SourceStream#endOfStream()}. Delegates to the wrapped
     * <tt>AudioMixerPushBufferStream</tt> because this instance is just a facet
     * to it.
     *
     * @return <tt>true</tt> if this <tt>SourceStream</tt> has reached the end
     * of the media it makes available; otherwise, <tt>false</tt>
     */
    public boolean endOfStream()
    {
        /*
         * TODO If the inputSamples haven't been consumed yet, don't report the
         * end of this stream even if the wrapped stream has reached its end.
         */
        return audioMixerStream.endOfStream();
    }

    /**
     * Implements {@link SourceStream#getContentDescriptor()}. Delegates to the
     * wrapped <tt>AudioMixerPushBufferStream</tt> because this instance is just
     * a facet to it.
     *
     * @return a <tt>ContentDescriptor</tt> which describes the content being
     * made available by this <tt>SourceStream</tt>
     */
    public ContentDescriptor getContentDescriptor()
    {
        return audioMixerStream.getContentDescriptor();
    }

    /**
     * Implements {@link SourceStream#getContentLength()}. Delegates to the
     * wrapped <tt>AudioMixerPushBufferStream</tt> because this instance is just
     * a facet to it.
     *
     * @return the length of the media being made available by this
     * <tt>SourceStream</tt>
     */
    public long getContentLength()
    {
        return audioMixerStream.getContentLength();
    }

    /**
     * Gets the <tt>AudioMixingPushBufferDataSource</tt> which created and owns
     * this instance and defines the input data which is to not be mixed in the
     * output of this <tt>PushBufferStream</tt>.
     *
     * @return the <tt>AudioMixingPushBufferDataSource</tt> which created and
     * owns this instance and defines the input data which is to not be mixed in
     * the output of this <tt>PushBufferStream</tt>
     */
    public AudioMixingPushBufferDataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * Implements {@link PushBufferStream#getFormat()}. Delegates to the wrapped
     * <tt>AudioMixerPushBufferStream</tt> because this instance is just a facet
     * to it.
     *
     * @return the <tt>Format</tt> of the audio being made available by this
     * <tt>PushBufferStream</tt>
     */
    public AudioFormat getFormat()
    {
        return audioMixerStream.getFormat();
    }

    /**
     * Gets the maximum possible value for an audio sample of a specific
     * <tt>AudioFormat</tt>.
     *
     * @param outputFormat the <tt>AudioFormat</tt> of which to get the maximum
     * possible value for an audio sample
     * @return the maximum possible value for an audio sample of the specified
     * <tt>AudioFormat</tt>
     * @throws UnsupportedFormatException if the specified <tt>outputFormat</tt>
     * is not supported by the underlying implementation
     */
    private static int getMaxOutputSample(AudioFormat outputFormat)
        throws UnsupportedFormatException
    {
        switch(outputFormat.getSampleSizeInBits())
        {
        case 8:
            return Byte.MAX_VALUE;
        case 16:
            return Short.MAX_VALUE;
        case 32:
            return Integer.MAX_VALUE;
        case 24:
        default:
            throw
                new UnsupportedFormatException(
                        "Format.getSampleSizeInBits()",
                        outputFormat);
        }
    }

    /**
     * Mixes as in audio mixing a specified collection of audio sample sets and
     * returns the resulting mix audio sample set in a specific
     * <tt>AudioFormat</tt>.
     *
     * @param inputSamples the collection of audio sample sets to be mixed into
     * one audio sample set in the sense of audio mixing
     * @param outputFormat the <tt>AudioFormat</tt> in which the resulting mix
     * audio sample set is to be produced
     * @param outputSampleCount the size of the resulting mix audio sample set
     * to be produced
     * @return the resulting audio sample set of the audio mixing of the
     * specified input audio sample sets
     */
    private static int[] mix(
        int[][] inputSamples,
        AudioFormat outputFormat,
        int outputSampleCount)
    {
        int[] outputSamples = new int[outputSampleCount];

        /*
         * The trivial case of performing audio mixing the audio of a single
         * stream. Then there is nothing to mix and the input becomes the
         * output.
         */
        if (inputSamples.length == 1)
        {
            int[] inputStreamSamples = inputSamples[0];

            if (inputStreamSamples != null)
            {
                System.arraycopy(
                        inputStreamSamples, 0,
                        outputSamples, 0,
                        inputStreamSamples.length);
            }
            return outputSamples;
        }

        int maxOutputSample;

        try
        {
            maxOutputSample = getMaxOutputSample(outputFormat);
        }
        catch (UnsupportedFormatException ufex)
        {
            throw new UnsupportedOperationException(ufex);
        }

        for (int[] inputStreamSamples : inputSamples)
        {
            if (inputStreamSamples == null)
                continue;

            int inputStreamSampleCount = inputStreamSamples.length;

            if (inputStreamSampleCount <= 0)
                continue;

            for (int i = 0; i < inputStreamSampleCount; i++)
            {
                int inputStreamSample = inputStreamSamples[i];
                int outputSample = outputSamples[i];

                outputSamples[i]
                    = inputStreamSample
                            + outputSample
                            - Math.round(
                                    inputStreamSample
                                        * (outputSample
                                                / (float) maxOutputSample));
            }
        }
        return outputSamples;
    }

    /**
     * Implements {@link PushBufferStream#read(Buffer)}. If
     * <tt>inputSamples</tt> are available, mixes them and writes the mix to the
     * specified <tt>Buffer</tt> performing the necessary data type conversions.
     *
     * @param buffer the <tt>Buffer</tt> to receive the data read from this
     * instance
     * @throws IOException if anything wrong happens while reading from this
     * instance
     */
    public void read(Buffer buffer)
        throws IOException
    {
        int[][] inputSamples;
        int maxInputSampleCount;
        long timeStamp;

        synchronized (readSyncRoot)
        {
            inputSamples = this.inputSamples;
            maxInputSampleCount = this.maxInputSampleCount;
            timeStamp = this.timeStamp;

            this.inputSamples = null;
            this.maxInputSampleCount = 0;
            this.timeStamp = Buffer.TIME_UNKNOWN;
        }

        int inputSampleCount = (inputSamples == null) ? 0 : inputSamples.length;

        if ((inputSampleCount == 0)
                || (maxInputSampleCount <= 0))
        {
            buffer.setLength(0);
            return;
        }

        AudioFormat outputFormat = getFormat();
        int[] outputSamples
            = mix(inputSamples, outputFormat, maxInputSampleCount);

        Class<?> outputDataType = outputFormat.getDataType();

        if (Format.byteArray.equals(outputDataType))
        {
            byte[] outputData;

            switch (outputFormat.getSampleSizeInBits())
            {
            case 16:
                outputData = new byte[outputSamples.length * 2];
                for (int i = 0; i < outputSamples.length; i++)
                    ArrayIOUtils.writeInt16(outputSamples[i], outputData, i * 2);
                break;
            case 32:
                outputData = new byte[outputSamples.length * 4];
                for (int i = 0; i < outputSamples.length; i++)
                    writeInt(outputSamples[i], outputData, i * 4);
                break;
            case 8:
            case 24:
            default:
                throw
                    new UnsupportedOperationException(
                            "AudioMixingPushBufferStream.read(Buffer)");
            }

            buffer.setData(outputData);
            buffer.setFormat(outputFormat);
            buffer.setLength(outputData.length);
            buffer.setOffset(0);
            buffer.setTimeStamp(timeStamp);
        }
        else
            throw
                new UnsupportedOperationException(
                        "AudioMixingPushBufferStream.read(Buffer)");
    }

    /**
     * Sets the collection of audio sample sets to be mixed in the sense of
     * audio mixing by this stream when data is read from it. Triggers a push to
     * the clients of this stream.
     *
     * @param inputSamples the collection of audio sample sets to be mixed by
     * this stream when data is read from it
     * @param maxInputSampleCount the maximum number of per-stream audio samples
     * available through <tt>inputSamples</tt>
     * @param timeStamp the time stamp of <tt>inputSamples</tt> to be reported
     * in the specified <tt>Buffer</tt> when data is read from this instance
     */
    void setInputSamples(
            int[][] inputSamples,
            int maxInputSampleCount,
            long timeStamp)
    {
        synchronized (readSyncRoot)
        {
            this.inputSamples = inputSamples;
            this.maxInputSampleCount = maxInputSampleCount;
        }

        BufferTransferHandler transferHandler = this.transferHandler;

        if (transferHandler != null)
            transferHandler.transferData(this);
    }

    /**
     * Implements
     * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}. Sets
     * the <tt>BufferTransferHandler</tt> which is to be notified by this
     * instance when it has media available for reading.
     *
     * @param transferHandler the <tt>BufferTransferHandler</tt> to be notified
     * by this instance when it has media available for reading
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        this.transferHandler = transferHandler;
    }

    /**
     * Starts the pushing of data out of this stream.
     *
     * @throws IOException if starting the pushing of data out of this stream
     * fails
     */
    synchronized void start()
        throws IOException
    {
        audioMixerStream.addOutputStream(this);
        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Started "
                        + getClass().getSimpleName()
                        + " with hashCode "
                        + hashCode());
    }

    /**
     * Stops the pushing of data out of this stream.
     *
     * @throws IOException if stopping the pushing of data out of this stream
     * fails
     */
    synchronized void stop()
        throws IOException
    {
        audioMixerStream.removeOutputStream(this);
        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Stopped "
                        + getClass().getSimpleName()
                        + " with hashCode "
                        + hashCode());
    }

    /**
     * Converts an integer to a series of bytes and writes the result into a
     * specific output array of bytes starting the writing at a specific offset
     * in it.
     *
     * @param input the integer to be written out as a series of bytes
     * @param output the output to receive the conversion of the specified
     * integer to a series of bytes
     * @param outputOffset the offset in <tt>output</tt> at which the writing of
     * the result of the conversion is to be started
     */
    private static void writeInt(int input, byte[] output, int outputOffset)
    {
        output[outputOffset] = (byte) (input & 0xFF);
        output[outputOffset + 1] = (byte) ((input >>> 8) & 0xFF);
        output[outputOffset + 2] = (byte) ((input >>> 16) & 0xFF);
        output[outputOffset + 3] = (byte) (input >> 24);
    }
}
