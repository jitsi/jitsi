/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.audio;

import java.io.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.*;
import net.java.sip.communicator.impl.neomedia.pulseaudio.*;

public class PulseAudioRenderer
    extends AbstractRenderer<AudioFormat>
{
    /**
     * The human-readable <tt>PlugIn</tt> name of the
     * <tt>PulseAudioRenderer</tt> instances.
     */
    private static final String PLUGIN_NAME = "PulseAudio Renderer";

    private static final Format[] SUPPORTED_INPUT_FORMATS
        = new Format[]
        {
            new AudioFormat(
                    AudioFormat.LINEAR,
                    Format.NOT_SPECIFIED /* sampleRate */,
                    16,
                    Format.NOT_SPECIFIED /* channels */,
                    AudioFormat.LITTLE_ENDIAN,
                    AudioFormat.SIGNED,
                    Format.NOT_SPECIFIED /* frameSizeInBits */,
                    Format.NOT_SPECIFIED /* frameRate */,
                    Format.byteArray)
        };

    private boolean corked = true;

    private final String mediaRole;

    private final PulseAudioSystem pulseAudioSystem;

    private long stream;

    private final PA.stream_request_cb_t writeCallback
        = new PA.stream_request_cb_t()
        {
            public void callback(long s, int nbytes)
            {
                pulseAudioSystem.signalMainloop(false);
            }
        };

    /**
     * Initializes a new <tt>PulseAudioRenderer</tt> instance.
     */
    public PulseAudioRenderer()
    {
        this(null);
    }

    public PulseAudioRenderer(String mediaRole)
    {
        pulseAudioSystem = PulseAudioSystem.getPulseAudioSystem();
        if (pulseAudioSystem == null)
            throw new IllegalStateException("pulseAudioSystem");

        this.mediaRole = (mediaRole == null) ? "phone" : mediaRole;
    }

    public void close()
    {
        pulseAudioSystem.lockMainloop();
        try
        {
            long stream = this.stream;

            if (stream != 0)
            {
                try
                {
                    stopWithMainloopLock();
                }
                finally
                {
                    this.stream = 0;
                    corked = true;
                    pulseAudioSystem.signalMainloop(false);
    
                    PA.stream_disconnect(stream);
                    PA.stream_unref(stream);
                }
            }
        }
        finally
        {
            pulseAudioSystem.unlockMainloop();
        }
    }

    private void cork(boolean b)
    {
        try
        {
            PulseAudioSystem.corkStream(stream, b);
            corked = b;
        }
        catch (IOException ioe)
        {
            throw new UndeclaredThrowableException(ioe);
        }
        finally
        {
            pulseAudioSystem.signalMainloop(false);
        }
    }

    public String getName()
    {
        return PLUGIN_NAME;
    }

    public Format[] getSupportedInputFormats()
    {
        return SUPPORTED_INPUT_FORMATS.clone();
    }

    public void open()
        throws ResourceUnavailableException
    {
        pulseAudioSystem.lockMainloop();
        try
        {
            openWithMainloopLock();
        }
        finally
        {
            pulseAudioSystem.unlockMainloop();
        }
    }

    private void openWithMainloopLock()
        throws ResourceUnavailableException
    {
        if (stream != 0)
            return;

        AudioFormat format = this.inputFormat;
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int sampleSizeInBits = format.getSampleSizeInBits();

        if ((sampleRate == Format.NOT_SPECIFIED)
                && (MediaUtils.MAX_AUDIO_SAMPLE_RATE
                        != Format.NOT_SPECIFIED))
            sampleRate = (int) MediaUtils.MAX_AUDIO_SAMPLE_RATE;
        if (channels == Format.NOT_SPECIFIED)
            channels = 1;
        if (sampleSizeInBits == Format.NOT_SPECIFIED)
            sampleSizeInBits = 16;

        long stream = 0;
        Throwable exception = null;

        try
        {
            stream
                = pulseAudioSystem.createStream(
                        sampleRate,
                        channels,
                        getClass().getName(),
                        mediaRole);
        }
        catch (IllegalStateException ise)
        {
            exception = ise;
        }
        catch (RuntimeException re)
        {
            exception = re;
        }
        if (exception != null)
        {
            ResourceUnavailableException rue
                = new ResourceUnavailableException();

            rue.initCause(exception);
            throw rue;
        }
        if (stream == 0)
            throw new ResourceUnavailableException("stream");

        try
        {
            long attr
                = PA.buffer_attr_new(
                        -1,
                        2 /* millis / 10 */
                            * (sampleRate / 100)
                            * channels
                            * (sampleSizeInBits / 8),
                        -1,
                        -1,
                        -1);

            if (attr == 0)
                throw new ResourceUnavailableException("pa_buffer_attr_new");

            try
            {
                Runnable stateCallback
                    = new Runnable()
                    {
                        public void run()
                        {
                            pulseAudioSystem.signalMainloop(false);
                        }
                    };

                PA.stream_set_state_callback(
                        stream,
                        stateCallback);
                PA.stream_connect_playback(
                        stream,
                        null,
                        attr,
                        PA.STREAM_ADJUST_LATENCY
                            | PA.STREAM_START_CORKED,
                        0,
                        0);

                try
                {
                    if (attr != 0)
                    {
                        PA.buffer_attr_free(attr);
                        attr = 0;
                    }

                    int state
                        = pulseAudioSystem.waitForStreamState(
                                stream,
                                PA.STREAM_READY);

                    if (state != PA.STREAM_READY)
                        throw new ResourceUnavailableException("stream.state");

                    PA.stream_set_write_callback(
                            stream,
                            writeCallback);

                    this.stream = stream;
                }
                finally
                {
                    if (this.stream == 0)
                        PA.stream_disconnect(stream);
                }
            }
            finally
            {
                if (attr != 0)
                    PA.buffer_attr_free(attr);
            }
        }
        finally
        {
            if (this.stream == 0)
                PA.stream_unref(stream);
        }
    }

    public int process(Buffer buffer)
    {
        if (buffer.isDiscard())
            return BUFFER_PROCESSED_OK;
        if (buffer.getLength() <= 0)
            return BUFFER_PROCESSED_OK;

        int ret;

        pulseAudioSystem.lockMainloop();
        try
        {
            ret = processWithMainloopLock(buffer);
        }
        finally
        {
            pulseAudioSystem.unlockMainloop();
        }
        if ((ret != BUFFER_PROCESSED_FAILED) && (buffer.getLength() > 0))
            ret |= INPUT_BUFFER_NOT_CONSUMED;

        return ret;
    }

    private int processWithMainloopLock(Buffer buffer)
    {
        if ((stream == 0) || corked)
            return BUFFER_PROCESSED_FAILED;

        int writableSize = PA.stream_writable_size(stream);
        int ret;

        if (writableSize <= 0)
        {
            pulseAudioSystem.waitMainloop();
            ret = BUFFER_PROCESSED_OK;
        }
        else
        {
            int offset = buffer.getOffset();
            int length = buffer.getLength();
            int writtenSize
                = PA.stream_write(
                        stream,
                        (byte[]) buffer.getData(),
                        offset,
                        (length < writableSize) ? length : writableSize,
                        null,
                        0,
                        PA.SEEK_RELATIVE);

            if (writtenSize < 0)
                ret = BUFFER_PROCESSED_FAILED;
            else
            {
                ret = BUFFER_PROCESSED_OK;
                buffer.setLength(length - writtenSize);
                buffer.setOffset(offset + writtenSize);
            }
        }

        return ret;
    }

    public void start()
    {
        pulseAudioSystem.lockMainloop();
        try
        {
            if (stream == 0)
            {
                try
                {
                    openWithMainloopLock();
                }
                catch (ResourceUnavailableException rue)
                {
                    throw new UndeclaredThrowableException(rue);
                }
            }

            cork(false);
        }
        finally
        {
            pulseAudioSystem.unlockMainloop();
        }
    }

    public void stop()
    {
        pulseAudioSystem.lockMainloop();
        try
        {
            stopWithMainloopLock();
        }
        finally
        {
            pulseAudioSystem.unlockMainloop();
        }
    }

    private void stopWithMainloopLock()
    {
        if (stream != 0)
            cork(true);
    }
}
