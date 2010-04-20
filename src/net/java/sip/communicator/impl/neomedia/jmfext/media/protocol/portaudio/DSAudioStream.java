/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.streams.*;

/**
 * The stream used by jmf, wraps our InputPortAudioStream, which wraps
 * the actual PortAudio stream.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class DSAudioStream
    extends ControlsAdapter
    implements PullBufferStream
{
    private static final ContentDescriptor cd
        = new ContentDescriptor(ContentDescriptor.RAW);

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
            AudioFormat audioFormat = DataSource.getCaptureFormat();

            stream
                = PortAudioManager
                    .getInstance()
                        .getInputStream(
                            deviceIndex,
                            audioFormat.getSampleRate(),
                            audioFormat.getChannels());
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
        {
            buffer.setLength(0);
            return;
        }

        try
        {
            stream.read(buffer);
        }
        catch (PortAudioException paex)
        {
            IOException ioex = new IOException();

            ioex.initCause(paex);
            throw ioex;
        }

        buffer.setFormat(getFormat());
        buffer.setHeader(null);

        buffer.setSequenceNumber(seqNo++);
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
}
