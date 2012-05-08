/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.pulseaudio;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.pulseaudio.*;
import net.java.sip.communicator.util.*;

public class DataSource
    extends AbstractPullBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    private static final int BUFFER_IN_TENS_OF_MILLIS = 10;

    private static final boolean DEBUG = logger.isDebugEnabled();

    private static final int FRAGSIZE_IN_TENS_OF_MILLIS = 2;

    private static final boolean SOFTWARE_GAIN;

    static {
        boolean softwareGain = true;

        try
        {
            String libraryVersion = PA.get_library_version();

            if (libraryVersion != null)
            {
                StringTokenizer st = new StringTokenizer(libraryVersion, ".");
                int major = Integer.parseInt(st.nextToken());
                int minor = Integer.parseInt(st.nextToken());

                if ((major >= 1) && (minor >= 0))
                {
                    softwareGain = false;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                                "Will control the volume"
                                    + " through the native PulseAudio API.");
                    }
                }
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
        SOFTWARE_GAIN = softwareGain;
    }

    private class PulseAudioStream
        extends AbstractPullBufferStream
    {
        private byte[] buffer;

        private int channels;

        private boolean corked = true;

        private long cvolume;

        private int fragsize;

        private final GainControl gainControl;

        private float gainControlLevel;

        private int length;

        private int offset;

        private final PulseAudioSystem pulseAudioSystem;

        private final PA.stream_request_cb_t readCallback
            = new PA.stream_request_cb_t()
            {
                public void callback(long s, int nbytes)
                {
                    readCallback(s, nbytes);
                }
            };

        private long stream;

        public PulseAudioStream(FormatControl formatControl)
        {
            super(DataSource.this, formatControl);

            pulseAudioSystem = PulseAudioSystem.getPulseAudioSystem();
            if (pulseAudioSystem == null)
                throw new IllegalStateException("pulseAudioSystem");

            gainControl
                = (GainControl)
                    NeomediaActivator.getMediaServiceImpl()
                            .getInputVolumeControl();
        }

        public void read(Buffer buffer)
            throws IOException
        {
            pulseAudioSystem.lockMainloop();
            try
            {
                if (stream == 0)
                    throw new IOException("stream");

                Object data = buffer.getData();
                byte[] bytes;

                if (data instanceof byte[])
                {
                    bytes = (byte[]) data;
                    if (bytes.length < fragsize)
                    {
                        bytes = new byte[fragsize];
                        buffer.setData(bytes);
                    }
                }
                else
                {
                    bytes = new byte[fragsize];
                    buffer.setData(bytes);
                }

                int toRead = fragsize;
                int offset = 0;
                int length = 0;

                while (toRead > 0)
                {
                    if (corked)
                        break;

                    if (this.length <= 0)
                    {
                        pulseAudioSystem.waitMainloop();
                        continue;
                    }

                    int toCopy = (toRead < this.length) ? toRead : this.length;

                    System.arraycopy(
                            this.buffer, this.offset,
                            bytes, offset,
                            toCopy);

                    this.offset += toCopy;
                    this.length -= toCopy;
                    if (this.length <= 0)
                    {
                        this.offset = 0;
                        this.length = 0;
                    }

                    toRead -= toCopy;
                    offset += toCopy;
                    length += toCopy;
                }

                buffer.setFlags(Buffer.FLAG_SYSTEM_TIME);
                buffer.setLength(length);
                buffer.setOffset(0);
                buffer.setTimeStamp(System.nanoTime());

                if (gainControl != null)
                {
                    if (SOFTWARE_GAIN || (cvolume == 0))
                    {
                        if (length > 0)
                        {
                            AbstractVolumeControl.applyGain(
                                    gainControl,
                                    bytes, 0, length);
                        }
                    }
                    else
                    {
                        float gainControlLevel = gainControl.getLevel();

                        if (this.gainControlLevel != gainControlLevel)
                        {
                            this.gainControlLevel = gainControlLevel;
                            setStreamVolume(stream, gainControlLevel);
                        }
                    }
                }
            }
            finally
            {
                pulseAudioSystem.unlockMainloop();
            }
        }

        private void readCallback(long stream, int length)
        {
            try
            {
                int peeked;

                if (corked)
                    peeked = 0;
                else
                {
                    int offset;

                    if ((buffer == null) || (buffer.length < length))
                    {
                        buffer = new byte[length];
                        this.offset = 0;
                        this.length = 0;
                        offset = 0;
                    }
                    else
                    {
                        offset = this.offset + this.length;
                        if (offset + length > buffer.length)
                        {
                            int overflow = this.length + length - buffer.length;

                            if (overflow > 0)
                            {
                                if (overflow >= this.length)
                                {
                                    if (DEBUG && logger.isDebugEnabled())
                                    {
                                        logger.debug(
                                                "Dropping "
                                                    + this.length
                                                    + " bytes!");
                                    }
                                    this.offset = 0;
                                    this.length = 0;
                                    offset = 0;
                                }
                                else
                                {
                                    if (DEBUG && logger.isDebugEnabled())
                                    {
                                        logger.debug(
                                                "Dropping "
                                                    + overflow
                                                    + " bytes!");
                                    }
                                    this.offset += overflow;
                                    this.length -= overflow;
                                }
                            }
                            if (this.length > 0)
                            {
                                for (int i = 0;
                                        i < this.length;
                                        i++, this.offset++)
                                {
                                    buffer[i] = buffer[this.offset];
                                }
                                this.offset = 0;
                                offset = this.length;
                            }
                        }
                    }

                    peeked = PA.stream_peek(stream, buffer, offset);
                }

                PA.stream_drop(stream);
                this.length += peeked;
            }
            finally
            {
                pulseAudioSystem.signalMainloop(false);
            }
        }

        @SuppressWarnings("unused")
        public void connect()
            throws IOException
        {
            pulseAudioSystem.lockMainloop();
            try
            {
                connectWithMainloopLock();
            }
            finally
            {
                pulseAudioSystem.unlockMainloop();
            }
        }

        private void connectWithMainloopLock()
            throws IOException
        {
            if (stream != 0)
                return;

            AudioFormat format = (AudioFormat) getFormat();
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
                            PulseAudioSystem.MEDIA_ROLE_PHONE);
                this.channels = channels;
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
                IOException ioe = new IOException();

                ioe.initCause(exception);
                throw ioe;
            }
            if (stream == 0)
                throw new IOException("stream");

            try
            {
                int bytesPerTenMillis
                    = (sampleRate / 100) * channels * (sampleSizeInBits / 8);

                fragsize = FRAGSIZE_IN_TENS_OF_MILLIS * bytesPerTenMillis;
                buffer = new byte[BUFFER_IN_TENS_OF_MILLIS * bytesPerTenMillis];

                long attr
                    = PA.buffer_attr_new(
                            -1,
                            -1,
                            -1,
                            -1,
                            fragsize);

                if (attr == 0)
                    throw new IOException("pa_buffer_attr_new");

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
                    PA.stream_connect_record(
                            stream,
                            getLocatorDev(),
                            attr,
                            PA.STREAM_ADJUST_LATENCY
                                | PA.STREAM_START_CORKED);

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
                            throw new IOException("stream.state");

                        PA.stream_set_read_callback(
                                stream,
                                readCallback);

                        if (!SOFTWARE_GAIN && (gainControl != null))
                        {
                            cvolume = PA.cvolume_new();

                            boolean freeCvolume = true;

                            try
                            {
                                float gainControlLevel = gainControl.getLevel();

                                setStreamVolume(stream, gainControlLevel);
                                this.gainControlLevel = gainControlLevel;
                                freeCvolume = false;
                            }
                            finally
                            {
                                if (freeCvolume)
                                {
                                    PA.cvolume_free(cvolume);
                                    cvolume = 0;
                                }
                            }
                        }

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

        private void cork(boolean b)
            throws IOException
        {
            try
            {
                PulseAudioSystem.corkStream(stream, b);
                corked = b;
            }
            finally
            {
                pulseAudioSystem.signalMainloop(false);
            }
        }

        public void disconnect()
            throws IOException
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
                        long cvolume = this.cvolume;

                        this.cvolume = 0;
                        this.stream = 0;

                        buffer = null;
                        corked = true;
                        fragsize = 0;
                        length = 0;
                        offset = 0;

                        pulseAudioSystem.signalMainloop(false);

                        if (cvolume != 0)
                            PA.cvolume_free(cvolume);
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

        private void setStreamVolume(long stream, float level)
        {
            int volume
                = PA.sw_volume_from_linear(
                        level
                            * (AbstractVolumeControl.MAX_VOLUME_PERCENT / 100));

            PA.cvolume_set(cvolume, channels, volume);

            long o
                = PA.context_set_source_output_volume(
                        pulseAudioSystem.getContext(),
                        PA.stream_get_index(stream),
                        cvolume,
                        null);

            if (o != 0)
                PA.operation_unref(o);
        }

        @Override
        public void start()
            throws IOException
        {
            pulseAudioSystem.lockMainloop();
            try
            {
                if (stream == 0)
                    connectWithMainloopLock();

                cork(false);
            }
            finally
            {
                pulseAudioSystem.unlockMainloop();
            }

            super.start();
        }

        @Override
        public void stop()
            throws IOException
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
            throws IOException
        {
            if (stream != 0)
                cork(true);

            super.stop();
        }
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
    }

    @Override
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new PulseAudioStream(formatControl);
    }

    protected void doDisconnect()
    {
        synchronized (getStreamSyncRoot())
        {
            Object[] streams = streams();

            if ((streams != null) && (streams.length != 0))
            {
                for (Object stream : streams)
                {
                    if (stream instanceof PulseAudioStream)
                    {
                        try
                        {
                            ((PulseAudioStream) stream).disconnect();
                        }
                        catch (IOException ioe)
                        {
                            // Well, what can we do?
                        }
                    }
                }
            }
        }

        super.doDisconnect();
    }

    private String getLocatorDev()
    {
        MediaLocator locator = getLocator();
        String locatorDev;

        if (locator == null)
            locatorDev = null;
        else
        {
            locatorDev = locator.getRemainder();
            if ((locatorDev != null) && (locatorDev.length() <= 0))
                locatorDev = null;
        }
        return locatorDev;
    }
}
