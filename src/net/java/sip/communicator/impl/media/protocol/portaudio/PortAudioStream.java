/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class PortAudioStream
    implements PullBufferStream
{
    private static final Logger logger =
        Logger.getLogger(PortAudioStream.class);

    /**
     * The indicator which determines whether
     * <tt>PortAudioStream#read(Buffer)</tt> will try to workaround a crash
     * experienced on Linux with Alsa and PulseAudio when <tt>Pa_ReadStream</tt>
     * is invoked to read more than the internal <tt>framesPerBuffer</tt>.
     */
    private static final boolean USE_FRAMES_PER_BUFFER_WORKAROUND;

    private static AudioFormat audioFormat = new AudioFormat(
                    AudioFormat.LINEAR,
                      44100,
                      16,
                      1,
                      AudioFormat.LITTLE_ENDIAN,
                      AudioFormat.SIGNED,
                      16,
                      Format.NOT_SPECIFIED,
                      Format.byteArray);

    private final static ContentDescriptor cd =
        new ContentDescriptor(ContentDescriptor.RAW);

    static
    {
        String osName = System.getProperty("os.name");

        USE_FRAMES_PER_BUFFER_WORKAROUND = false;
//            = (osName != null) && osName.contains("Linux");
    }

    private Control[] controls = new Control[0];

    private long stream = 0;

    private int seqNo = 0;

    private int frameSize;

    private final int deviceIndex;

    /**
     * Creates new stream.
     * @param locator the locator to extract the device index from it.
     */
    public PortAudioStream(MediaLocator locator)
    {
        this.deviceIndex = PortAudioUtils.getDeviceIndexFromLocator(locator);
    }

    /**
     * Return the formats supported by the datasource stream
     * corresponding the maximum input channels.
     *
     * @return the supported formats.
     */
    public static Format[] getFormats()
    {
        return new Format[]{audioFormat};
    }

    /**
     * Returns the supported format by this stream.
     * @return supported formats
     */
    public Format getFormat()
    {
        return audioFormat;
    }

    /**
     * We are providing access to raw data
     * @return RAW content descriptor.
     */
    public ContentDescriptor getContentDescriptor()
    {
        return cd;
    }

    /**
     * We are streaming.
     * @return unknown content length.
     */
    public long getContentLength()
    {
        return LENGTH_UNKNOWN;
    }

    /**
     * The stream never ends.
     * @return true if the end of the stream has been reached.
     */
    public boolean endOfStream()
    {
        return false;
    }

    /**
     * Gives control information to the caller
     * @return no controls currently supported.
     */
    public Object[] getControls()
    {
        return controls;
    }

    /**
     * Return required control from the Control[] array
     * if exists
     *
     * @param controlType the control class name.
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> cls = Class.forName(controlType);
            Object cs[] = getControls();
            for(int i = 0; i < cs.length; i++)
            {
                if(cls.isInstance(cs[i]))
                {
                    return cs[i];
                }
            }
            return null;

        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Starts the stream operation
     */
    synchronized void start()
        throws PortAudioException
    {
        PortAudio.Pa_StartStream(getStream());
    }

    synchronized void stop()
        throws PortAudioException
    {
        if (stream != 0)
        {
            PortAudio.Pa_CloseStream(stream);
            stream = 0;
        }
    }

    private long getStream()
        throws PortAudioException
    {
        if (stream == 0)
        {
            int channels = audioFormat.getChannels();
            long streamParameters
                = PortAudio.PaStreamParameters_new(
                        deviceIndex,
                        channels,
                        PortAudio.SAMPLE_FORMAT_INT16);

            stream
                = PortAudio.Pa_OpenStream(
                        streamParameters,
                        0,
                        audioFormat.getSampleRate(),
                        PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
                        PortAudio.STREAM_FLAGS_NO_FLAG,
                        null);
            frameSize
                = PortAudio.Pa_GetSampleSize(PortAudio.SAMPLE_FORMAT_INT16)
                    * channels;
        }

        return stream;
    }

    /**
     * Query if the next read will block.
     * @return true if a read will block.
     */
    public synchronized boolean willReadBlock()
    {
        return
            (stream != 0)
                ? PortAudio.Pa_GetStreamReadAvailable(stream) == 0
                : false;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public synchronized void read(Buffer buffer)
        throws IOException
    {
        if (stream == 0)
            return;

        try
        {
            byte[] bytebuff = null;
            int readAvailableFrames;

            if (USE_FRAMES_PER_BUFFER_WORKAROUND)
            {
                Format bufferFormat = buffer.getFormat();

                /*
                 * If we've managed to read at least once with a certain buffer
                 * length, there does not seem to be a reason why we will not be
                 * able to do it again.
                 */
                if ((bufferFormat != null)
                        && Format.byteArray.equals(bufferFormat.getDataType()))
                    bytebuff = (byte[]) buffer.getData();
            }

            int bytebuffLength;

            if (bytebuff == null)
            {
                readAvailableFrames
                    = (int) PortAudio.Pa_GetStreamReadAvailable(stream);
                if (readAvailableFrames < 1)
                    readAvailableFrames = 512;

                bytebuff = new byte[readAvailableFrames * frameSize];
                bytebuffLength = bytebuff.length;
            }
            else
            {
                readAvailableFrames = bytebuff.length / frameSize;
                bytebuffLength = readAvailableFrames * frameSize;
            }

            PortAudio.Pa_ReadStream(stream, bytebuff, readAvailableFrames);

            buffer.setTimeStamp(System.nanoTime());
            buffer.setData(bytebuff);
            buffer.setSequenceNumber(seqNo);
            buffer.setLength(bytebuffLength);
            buffer.setFlags(0);
            buffer.setHeader(null);
            seqNo++;
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }
    }
}
