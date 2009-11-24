/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio.streams;

import net.java.sip.communicator.impl.neomedia.portaudio.PortAudioException;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio.*;

/**
 * The input audio stream.
 *
 * @author Damian Minkov
 */
public class InputPortAudioStream
{
    private final MasterPortAudioStream parentStream;

    private boolean started = false;

    /**
     * The buffer to return.
     */
    protected byte[] buffer = null;

    /**
     * Creates new input stream (slave) with master input stream.
     * @param st the parent(master) input stream.
     */
    public InputPortAudioStream(MasterPortAudioStream st)
    {
        this.parentStream = st;
    }

    /**
     * Block and read a buffer from the stream if there is no buffer.
     *
     * @return the bytes that a read from underlying stream.
     * @throws PortAudioException if an error occurs while reading.
     */
    public byte[] read()
        throws PortAudioException
    {
        synchronized(parentStream)
        {
            byte[] res = buffer;

            if(res == null)
                res = parentStream.read();
            buffer = null;
            return res;
        }
    }

    /**
     * Starts the stream. Also starts the parent stream
     * if its not already started.
     * @throws PortAudioException if stream cannot be started.
     */
    public synchronized void start()
        throws PortAudioException
    {
        if(started)
            return;

        parentStream.start(this);
        started = true;
    }

    /**
     * Stops the stream. Also stops the parent if we are the last slave
     * stream that use it.
     * @throws PortAudioException
     */
    public synchronized void stop()
        throws PortAudioException
    {
        if(!started)
            return;

        parentStream.stop(this);
        started = false;
    }

    /**
     * The parent can set a buffer that was requested and read by other
     * slave stream.
     * @param buffer the buffer to set.
     */
    public void setBuffer(byte[] buffer)
    {
        this.buffer = buffer;
    }
}
