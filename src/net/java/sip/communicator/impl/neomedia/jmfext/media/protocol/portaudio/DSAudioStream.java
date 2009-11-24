/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio;

import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.streams.*;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * The stream used by jmf, wraps our InputPortAudioStream, which wraps
 * the actual PortAudio stream.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class DSAudioStream
    implements PullBufferStream
{
    private final static ContentDescriptor cd =
        new ContentDescriptor(ContentDescriptor.RAW);

    private Control[] controls = new Control[0];

    private final int deviceIndex;

    private InputPortAudioStream stream = null;

    private int seqNo = 0;

    /**
     * Creates new stream.
     * @param locator the locator to extract the device index from it.
     */
    public DSAudioStream(MediaLocator locator)
    {
        this.deviceIndex = PortAudioUtils.getDeviceIndexFromLocator(locator);
    }

    /**
     * Starts the stream operation
     * @throws PortAudioException if fail to start.
     */
    void start()
        throws PortAudioException
    {
        if(stream == null)
        {
            AudioFormat audioFormat =
                (AudioFormat)DataSource.getCaptureFormat();

            stream = PortAudioManager.getInstance().getInputStream(deviceIndex,
                audioFormat.getSampleRate(), audioFormat.getChannels());
        }

        stream.start();
    }

    /**
     * Stops the stream operation.
     * @throws PortAudioException if fail to stop.
     */
    void stop()
        throws PortAudioException
    {
        stream.stop();
        stream = null;
    }

    /**
     * Query if the next read will block.
     * @return true if a read will block.
     */
    public boolean willReadBlock()
    {
        return false;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public void read(Buffer buffer)
        throws IOException
    {
        if (stream == null)
            return;

        try
        {
            byte[] bytebuff = stream.read();

            buffer.setTimeStamp(System.nanoTime());

            buffer.setData(bytebuff);
            buffer.setLength(bytebuff.length);

            buffer.setFlags(0);
            buffer.setFormat(getFormat());
            buffer.setHeader(null);

            buffer.setSequenceNumber(seqNo);
            seqNo++;
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }
    }

    /**
     * Returns the supported format by this stream.
     * @return supported formats
     */
    public Format getFormat()
    {
        return DataSource.getCaptureFormat();
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
}
