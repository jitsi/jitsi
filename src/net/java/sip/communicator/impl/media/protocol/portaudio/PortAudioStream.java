/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

import java.io.*;
import java.nio.*;

import javax.media.*;
import javax.media.Buffer;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class PortAudioStream
    implements PullBufferStream
{
    private static final Logger logger =
        Logger.getLogger(PortAudioStream.class);

    /**
     * The locatro prefix used when creating or parsing MediaLocators.
     */
    public static final String LOCATOR_PREFIX = "portaudio:#";

    private final static ContentDescriptor cd =
        new ContentDescriptor(ContentDescriptor.RAW);

    private Control[] controls = new Control[0];

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

    private boolean started;
    private long stream = 0;

    private int seqNo = 0;

    private int sampleSize = 1;

    final private int deviceIndex;

    /**
     * Creates new stream.
     * @param locator the locator to extract the device index from it.
     */
    public PortAudioStream(MediaLocator locator)
    {
        this.deviceIndex = getDeviceIndexFromLocator(locator);
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
    void start()
    {
        synchronized (this)
        {
            this.started = true;
            try
            {
                PortAudio.Pa_StartStream(getStream());
            }
            catch (PortAudioException paex)
            {
                paex.printStackTrace();
            }
        }
    }

    void stop()
    {
        synchronized (this)
        {
            this.started = false;

            try
            {
                PortAudio.Pa_CloseStream(getStream());
                stream = 0;
            }
            catch (PortAudioException paex)
            {
                paex.printStackTrace();
            }
        }
    }

    private long getStream()
        throws PortAudioException
    {
        if (stream == 0)
        {
            long streamParameters
                = PortAudio.PaStreamParameters_new(
                        deviceIndex,
                        audioFormat.getChannels(),
                        PortAudio.SAMPLE_FORMAT_INT16);

            stream
                = PortAudio.Pa_OpenStream(
                        streamParameters,
                        0,
                        audioFormat.getSampleRate(),
                        PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
                        PortAudio.STREAM_FLAGS_NO_FLAG,
                        null);
            sampleSize =
                PortAudio.Pa_GetSampleSize(PortAudio.SAMPLE_FORMAT_INT16);
        }

        return stream;
    }

    /**
     * Query if the next read will block.
     * @return true if a read will block.
     */
    public boolean willReadBlock()
    {
        try
        {
            return PortAudio.Pa_GetStreamReadAvailable(getStream()) == 0;
        }
        catch (PortAudioException ex)
        {
            logger.error("Cannot get read available", ex);
            return true;
        }
    }

    /**
     * Block and read a buffer from the stream.
     * @param buffer should be non-null.
     * @throws IOException Thrown if an error occurs while reading.
     */
    public synchronized void read(Buffer buffer)
        throws IOException
    {
        if(!this.started)
            return;

        try
        {
            int canread =
                (int)PortAudio.Pa_GetStreamReadAvailable(getStream());
            if(canread < 1)
                canread = 512;

            byte[] bytebuff = new byte[canread*sampleSize];

            PortAudio.Pa_ReadStream(getStream(), bytebuff, canread);
    
            buffer.setTimeStamp(System.nanoTime());
            buffer.setData(bytebuff);
            buffer.setSequenceNumber(seqNo);
            buffer.setLength(bytebuff.length);
            buffer.setFlags(0);
            buffer.setHeader(null);
            seqNo++;
        }
        catch (PortAudioException e)
        {
            logger.error("", e);
        }
    }

    /**
     * Extracts the device index from the locator.
     * @param locator the locator containing the device index.
     * @return the extracted device index.
     */
    public static int getDeviceIndexFromLocator(MediaLocator locator)
    {
        return Integer.parseInt(locator.toExternalForm().replace(
                PortAudioStream.LOCATOR_PREFIX, ""));
    }
}
