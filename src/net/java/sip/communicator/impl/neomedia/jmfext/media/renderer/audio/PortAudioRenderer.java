/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.audio;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.util.*;

/**
 * Implements an audio <tt>Renderer</tt> which uses PortAudio.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class PortAudioRenderer
    extends ControlsAdapter
    implements Renderer
{
    /**
     * The <tt>Logger</tt> used by the <tt>PortAudioRenderer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PortAudioRenderer.class);

    /**
     * The constant which represents an empty array with
     * <tt>Format</tt> element type. Explicitly defined in order to
     * reduce unnecessary allocations.
     */
    private static final Format[] EMPTY_SUPPORTED_INPUT_FORMATS
        = new Format[0];

    /**
     * The human-readable name of the <tt>PortAudioRenderer</tt> JMF plug-in.
     */
    private static final String PLUGIN_NAME = "PortAudio Renderer";

    /**
     * The list of JMF <tt>Format</tt>s of audio data which
     * <tt>PortAudioRenderer</tt> instances are capable of rendering.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    /**
     * The list of the sample rates supported by <tt>PortAudioRenderer</tt> as
     * input.
     */
    private static final double[] SUPPORTED_INPUT_SAMPLE_RATES
        = new double[] { 8000, 11025, 16000, 22050, 32000, 44100, 48000 };

    static
    {
        int count = SUPPORTED_INPUT_SAMPLE_RATES.length;

        SUPPORTED_INPUT_FORMATS = new Format[count];
        for (int i = 0; i < count; i++)
        {
            SUPPORTED_INPUT_FORMATS[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_INPUT_SAMPLE_RATES[i],
                        16 /* sampleSizeInBits */,
                        Format.NOT_SPECIFIED /* channels */,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.byteArray);
        }
    }

    /**
     * The <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device to be used by <tt>PortAudioRenderer</tt> instances which
     * are to be opened later on and which don't have a specified
     * <tt>MediaLocator</tt> at the time of opening.
     */
    private static MediaLocator defaultLocator;

    /**
     * The audio samples left unwritten by a previous call to
     * {@link #process(Buffer)}. As {@link #bytesPerBuffer} number of
     * bytes are always written, the number of the unwritten audio samples is
     * always less than that.
     */
    private byte[] bufferLeft;

    /**
     * The number of bytes in {@link #bufferLeft} representing unwritten audio
     * samples.
     */
    private int bufferLeftLength = 0;

    /**
     * The number of bytes to write to the native PortAudio stream represented
     * by this instance with a single invocation. Based on
     * {@link #framesPerBuffer}.
     */
    private int bytesPerBuffer;

    /**
     * The number of frames to write to the native PortAudio stream represented
     * by this instance with a single invocation.
     */
    private int framesPerBuffer;

    /**
     * The JMF <tt>Format</tt> in which this <tt>PortAudioRenderer</tt> is
     * currently configured to read the audio data to be rendered.
     */
    private AudioFormat inputFormat;

    /**
     * The <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device used by this instance for rendering.
     */
    private MediaLocator locator;

    private long outputParameters = 0;

    /**
     * The indicator which determines whether this <tt>Renderer</tt> is started.
     */
    private boolean started = false;

    /**
     * The output PortAudio stream represented by this instance.
     */
    private long stream = 0;

    /**
     * The indicator which determines whether {@link #stream} is busy and should
     * not, for example, be closed.
     */
    private boolean streamIsBusy = false;

    /**
     * Array of supported input formats.
     */
    private Format[] supportedInputFormats;

    /**
     * The <tt>GainControl</tt> used to control volume/gain of current played
     * media.
     */
    private GainControl gainControl = null;

    /**
     * Initializes a new <tt>PortAudioRenderer</tt> instance.
     */
    public PortAudioRenderer()
    {
        this(true);
    }

    /**
     * Initializes a new <tt>PortAudioRenderer</tt> instance.
     * @param enableVolumeControl whether we enable volume control or not.
     */
    public PortAudioRenderer(boolean enableVolumeControl)
    {
        if(enableVolumeControl)
            this.gainControl = (GainControl)NeomediaActivator
                    .getMediaServiceImpl().getOutputVolumeControl();
    }

    /**
     * Applies the gain specified by {@link #gainControl} to the signal defined
     * by the <tt>length</tt> number of samples given in <tt>buffer</tt>
     * starting at <tt>offset</tt>.
     *
     * @param buffer the samples of the signal to apply the gain to
     * @param offset the start of the samples of the signal in <tt>buffer</tt>
     * @param length the number of samples of the signal given in
     * <tt>buffer</tt>
     */
    private void applyGain(byte[] buffer, int offset, int length)
    {
        if (gainControl.getMute())
            Arrays.fill(buffer, offset, offset + length, (byte) 0);
        else
        {
            // Assign the maximum of 200% to the volume scale.
            float level = gainControl.getLevel() * 2;

            if (level != 1)
            {
                for (int i = offset, toIndex = offset + length;
                        i < toIndex;
                        i += 2)
                {
                    int i1 = i + 1;
                    short s = (short) ((buffer[i] & 0xff) | (buffer[i1] << 8));

                    /* Clip, don't wrap. */
                    int si = s;

                    si = (int) (si * level);
                    if (si > Short.MAX_VALUE)
                        s = Short.MAX_VALUE;
                    else if (si < Short.MIN_VALUE)
                        s = Short.MIN_VALUE;
                    else
                        s = (short) si;

                    buffer[i] = (byte) s;
                    buffer[i1] = (byte) (s >> 8);
                }
            }
        }
    }

    /**
     * Closes this <tt>PlugIn</tt>.
     */
    public synchronized void close()
    {
        try
        {
            stop();
        }
        finally
        {
            if (stream != 0)
            {
                try
                {
                    PortAudio.Pa_CloseStream(stream);
                    stream = 0;
                }
                catch (PortAudioException paex)
                {
                    logger.error("Failed to close PortAudio stream.", paex);
                }
            }
            if ((stream == 0) && (outputParameters != 0))
            {
                PortAudio.PaStreamParameters_free(outputParameters);
                outputParameters = 0;
            }
        }
    }

    /**
     * Gets the <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device used by this instance for rendering.
     *
     * @return the <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device used by this instance for rendering
     */
    public MediaLocator getLocator()
    {
        return (this.locator == null) ? defaultLocator : this.locator;
    }

    /**
     * Gets the descriptive/human-readable name of this JMF plug-in.
     *
     * @return the descriptive/human-readable name of this JMF plug-in
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Gets the list of JMF <tt>Format</tt>s of audio data which this
     * <tt>Renderer</tt> is capable of rendering.
     *
     * @return an array of JMF <tt>Format</tt>s of audio data which this
     * <tt>Renderer</tt> is capable of rendering
     */
    public Format[] getSupportedInputFormats()
    {
        if (supportedInputFormats == null)
        {
            MediaLocator locator = getLocator();
            int deviceIndex;

            if ((locator == null)
                    || ((deviceIndex = DataSource.getDeviceIndex(locator))
                            == PortAudio.paNoDevice))
                supportedInputFormats = SUPPORTED_INPUT_FORMATS;
            else
            {
                long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);

                if (deviceInfo == 0)
                    supportedInputFormats = SUPPORTED_INPUT_FORMATS;
                else
                {
                    int minOutputChannels = 1;
                    /*
                     * The maximum output channels may be a lot and checking all
                     * of them will take a lot of time. Besides, we currently
                     * support at most 2.
                     */
                    int maxOutputChannels
                        = Math.min(
                                PortAudio.PaDeviceInfo_getMaxOutputChannels(
                                        deviceInfo),
                                2);
                    List<Format> supportedInputFormats
                        = new ArrayList<Format>(SUPPORTED_INPUT_FORMATS.length);

                    for (Format supportedInputFormat : SUPPORTED_INPUT_FORMATS)
                    {
                        getSupportedInputFormats(
                                supportedInputFormat,
                                deviceIndex,
                                minOutputChannels,
                                maxOutputChannels,
                                supportedInputFormats);
                    }
                    this.supportedInputFormats
                        = supportedInputFormats.isEmpty()
                            ? EMPTY_SUPPORTED_INPUT_FORMATS
                            : supportedInputFormats.toArray(
                                    EMPTY_SUPPORTED_INPUT_FORMATS);
                }
            }
        }
        return
            (supportedInputFormats.length == 0)
                ? EMPTY_SUPPORTED_INPUT_FORMATS
                : supportedInputFormats.clone();
    }

    private void getSupportedInputFormats(
            Format format,
            int deviceIndex,
            int minOutputChannels, int maxOutputChannels,
            List<Format> supportedInputFormats)
    {
        AudioFormat audioFormat = (AudioFormat) format;
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        long sampleFormat = PortAudio.getPaSampleFormat(sampleSizeInBits);
        double sampleRate = audioFormat.getSampleRate();

        for (int channels = minOutputChannels;
                channels <= maxOutputChannels;
                channels++)
        {
            long outputParameters
                = PortAudio.PaStreamParameters_new(
                        deviceIndex,
                        channels,
                        sampleFormat,
                        PortAudio.LATENCY_UNSPECIFIED);

            if (outputParameters != 0)
            {
                try
                {
                    if (PortAudio.Pa_IsFormatSupported(
                            0,
                            outputParameters,
                            sampleRate))
                    {
                        supportedInputFormats.add(
                                new AudioFormat(
                                        audioFormat.getEncoding(),
                                        sampleRate,
                                        sampleSizeInBits,
                                        channels,
                                        audioFormat.getEndian(),
                                        audioFormat.getSigned(),
                                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                                        Format.NOT_SPECIFIED /* frameRate */,
                                        audioFormat.getDataType()));
                    }
                }
                finally
                {
                    PortAudio.PaStreamParameters_free(outputParameters);
                }
            }
        }
    }

    /**
     * Opens the PortAudio device and output stream represented by this instance
     * which are to be used to render audio.
     *
     * @throws ResourceUnavailableException if the PortAudio device or output
     * stream cannot be created or opened
     */
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (stream == 0)
        {
            MediaLocator locator = getLocator();

            if (locator == null)
                throw new ResourceUnavailableException("locator not set");

            int deviceIndex = DataSource.getDeviceIndex(locator);
            AudioFormat inputFormat = this.inputFormat;

            if (inputFormat == null)
                throw new ResourceUnavailableException("inputFormat not set");

            int channels = inputFormat.getChannels();

            if (channels == Format.NOT_SPECIFIED)
                channels = 1;

            long sampleFormat
                = PortAudio.getPaSampleFormat(
                    inputFormat.getSampleSizeInBits());
            double sampleRate = inputFormat.getSampleRate();

            framesPerBuffer
                = (int)
                    ((sampleRate * PortAudio.DEFAULT_MILLIS_PER_BUFFER)
                        / (channels * 1000));

            try
            {
                outputParameters
                    = PortAudio.PaStreamParameters_new(
                            deviceIndex,
                            channels,
                            sampleFormat,
                            PortAudio.getSuggestedLatency());

                stream
                    = PortAudio.Pa_OpenStream(
                            0 /* inputParameters */,
                            outputParameters,
                            sampleRate,
                            framesPerBuffer,
                            PortAudio.STREAM_FLAGS_CLIP_OFF
                                | PortAudio.STREAM_FLAGS_DITHER_OFF,
                            null /* streamCallback */);
            }
            catch (PortAudioException paex)
            {
                throw new ResourceUnavailableException(paex.getMessage());
            }
            finally
            {
                if ((stream == 0) && (outputParameters != 0))
                {
                    PortAudio.PaStreamParameters_free(outputParameters);
                    outputParameters = 0;
                }
            }
            if (stream == 0)
                throw new ResourceUnavailableException("Pa_OpenStream");

            bytesPerBuffer
                = PortAudio.Pa_GetSampleSize(sampleFormat)
                    * channels
                    * framesPerBuffer;
        }
    }

    /**
     * Renders the audio data contained in a specific <tt>Buffer</tt> onto the
     * PortAudio device represented by this <tt>Renderer</tt>.
     *
     * @param buffer the <tt>Buffer</tt> which contains the audio data to be
     * rendered
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the specified <tt>buffer</tt> has
     * been successfully processed
     */
    public int process(Buffer buffer)
    {
        synchronized (this)
        {
            if (!started || (stream == 0))
                return BUFFER_PROCESSED_OK;
            else
                streamIsBusy = true;
        }
        try
        {
            process(
                (byte[]) buffer.getData(),
                buffer.getOffset(),
                buffer.getLength());
        }
        catch (PortAudioException paex)
        {
            logger.error("Failed to process Buffer.", paex);
        }
        finally
        {
            synchronized (this)
            {
                streamIsBusy = false;
                notifyAll();
            }
        }
        return BUFFER_PROCESSED_OK;
    }

    private void process(byte[] buffer, int offset, int length)
        throws PortAudioException
    {

        /*
         * If there are audio samples left unwritten from a previous write,
         * prepend them to the specified buffer. If it's possible to write them
         * now, do it.
         */
        if ((bufferLeft != null) && (bufferLeftLength > 0))
        {
            int numberOfBytesInBufferLeftToBytesPerBuffer
                = bytesPerBuffer - bufferLeftLength;
            int numberOfBytesToCopyToBufferLeft
                = (numberOfBytesInBufferLeftToBytesPerBuffer < length)
                    ? numberOfBytesInBufferLeftToBytesPerBuffer
                    : length;

            System
                .arraycopy(
                    buffer,
                    offset,
                    bufferLeft,
                    bufferLeftLength,
                    numberOfBytesToCopyToBufferLeft);
            offset += numberOfBytesToCopyToBufferLeft;
            length -= numberOfBytesToCopyToBufferLeft;
            bufferLeftLength += numberOfBytesToCopyToBufferLeft;

            if (bufferLeftLength == bytesPerBuffer)
            {
                PortAudio.Pa_WriteStream(stream, bufferLeft, framesPerBuffer);
                bufferLeftLength = 0;
            }
        }

        // Write the audio samples from the specified buffer.
        int numberOfWrites = length / bytesPerBuffer;

        if (numberOfWrites > 0)
        {
            // if we have some volume setting apply them
            if (gainControl != null)
                applyGain(buffer, offset, length);

            PortAudio.Pa_WriteStream(
                    stream,
                    buffer, offset, framesPerBuffer,
                    numberOfWrites);

            int bytesWritten = numberOfWrites * bytesPerBuffer;

            offset += bytesWritten;
            length -= bytesWritten;
        }

        // If anything was left unwritten, remember it for next time.
        if (length > 0)
        {
            if (bufferLeft == null)
                bufferLeft = new byte[bytesPerBuffer];
            System.arraycopy(buffer, offset, bufferLeft, 0, length);
            bufferLeftLength = length;
        }
    }

    /**
     * Resets this <tt>PlugIn</tt>.
     */
    public void reset()
    {
    }

    /**
     * Sets the <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device to be used by <tt>PortAudioRenderer</tt> instances which
     * are to be opened later on and which don't have a specified
     * <tt>MediaLocator</tt> at the time of opening.
     *
     * @param defaultLocator the <tt>MediaLocator</tt> which specifies the
     * device index of the PortAudio device to be used by
     * <tt>PortAudioRenderer</tt> instances which are to be opened later on and
     * which don't have a specified <tt>MediaLocator</tt> at the time of opening
     */
    public static void setDefaultLocator(MediaLocator defaultLocator)
    {
        if (PortAudioRenderer.defaultLocator == null)
        {
            if (defaultLocator == null)
                return;
        }
        else if (PortAudioRenderer.defaultLocator.equals(defaultLocator))
            return;

        PortAudioRenderer.defaultLocator = defaultLocator;
    }

    /**
     * Sets the JMF <tt>Format</tt> of the audio data to be rendered by this
     * <tt>Renderer</tt>.
     *
     * @param format the JMF <tt>Format</tt> of the audio data to be redered by
     * this instance
     * @return <tt>null</tt> if the specified <tt>format</tt> is not compatible
     * with this <tt>Renderer</tt>; otherwise, the JMF <tt>Format</tt> which has
     * been successfully set
     */
    public Format setInputFormat(Format format)
    {
        Format matchingFormat = null;

        for (Format supportedInputFormat : getSupportedInputFormats())
        {
            if (supportedInputFormat.matches(format))
            {
                matchingFormat = supportedInputFormat.intersects(format);
                break;
            }
        }
        if (matchingFormat == null)
            return null;

        inputFormat = (AudioFormat) matchingFormat;
        return inputFormat;
    }

    /**
     * Sets the <tt>MediaLocator</tt> which specifies the device index of the
     * PortAudio device to be used by this instance for rendering.
     *
     * @param locator a <tt>MediaLocator</tt> which specifies the device index
     * of the PortAudio device to be used by this instance for rendering
     */
    public void setLocator(MediaLocator locator)
    {
        if (this.locator == null)
        {
            if (locator == null)
                return;
        }
        else if (this.locator.equals(locator))
            return;

        this.locator = locator;
        supportedInputFormats = null;
    }

    /**
     * Starts the rendering process. Any audio data available in the internal
     * resources associated with this <tt>PortAudioRenderer</tt> will begin
     * being rendered.
     */
    public synchronized void start()
    {
        if (!started && (stream != 0))
        {
            try
            {
                PortAudio.Pa_StartStream(stream);
                started = true;
            }
            catch (PortAudioException paex)
            {
                logger.error("Failed to start PortAudio stream.", paex);
            }
        }
    }

    /**
     * Stops the rendering process.
     */
    public synchronized void stop()
    {
        boolean interrupted = false;

        while (streamIsBusy)
        {
            try
            {
                wait();
            }
            catch (InterruptedException iex)
            {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();

        if (started && (stream != 0))
        {
            try
            {
                PortAudio.Pa_StopStream(stream);
                started = false;

                bufferLeft = null;
            }
            catch (PortAudioException paex)
            {
                logger.error("Failed to close PortAudio stream.", paex);
            }
        }
    }

    /**
     * Implements {@link javax.media.Controls#getControls()}. Gets the controls
     * available for the owner of this instance. The current implementation
     * returns an empty array because it has no available controls.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for the owner of this instance
     */
    public Object[] getControls()
    {
        return new Object[]{gainControl};
    }
}
