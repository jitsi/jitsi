/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio.streams;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.portaudio.*;

/**
 * The input audio stream.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class InputPortAudioStream
{

    /**
     * The audio data to be read from this <tt>InputPortAudioStream</tt> upon
     * the next {@link #read(Buffer)} request.
     */
    private byte[] bufferData = null;

    /**
     * The number of bytes in {@link #bufferData} which represent valid audio
     * data to be read from this <tt>InputPortAudioStream</tt> upon the next
     * {@link #read(Buffer)} request.
     */
    private int bufferLength = 0;

    /**
     * The time stamp of the audio data in {@link #bufferData} represented in
     * accord with {@link Buffer#FLAG_SYSTEM_TIME}.
     */
    private long bufferTimeStamp = 0;

    /**
     * Our parent stream, the actual source of data.
     */
    private final MasterPortAudioStream parentStream;

    /**
     * Is this stream started.
     */
    private boolean started = false;

    /**
     * This is unprotected field which will stop any further reading,
     * as read is synchronized sometimes there maybe some delay
     * before we are stopped, as reading is too aggressive stopping thread may
     * even wait more than 20 seconds.
     */
    private boolean stopping = false;

    /**
     * Creates new input stream (slave) with master input stream.
     *
     * @param parentStream the parent/master input stream.
     */
    public InputPortAudioStream(MasterPortAudioStream parentStream)
    {
        this.parentStream = parentStream;
    }

    private Object readSync = new Object();
    /**
     * Reads audio data from this <tt>InputPortAudioStream</tt> into a specific
     * <tt>Buffer</tt> blocking until audio data is indeed available.
     *
     * @param buffer the <tt>Buffer</tt> into which the audio data read from
     * this <tt>InputPortAudioStream</tt> is to be returned
     * @throws PortAudioException if an error occurs while reading
     */
    public void read(Buffer buffer)
        throws PortAudioException
    {
        if (stopping || !started)
        {
            buffer.setLength(0);
            return;
        }
        
        synchronized (readSync)
        {
            while (true)
            {
                if (bufferData == null) {
                    // parent read returns false if read is already active, wait
                    // until notified by setBuffer below.
                    if (!parentStream.read(buffer)) {
                        try {
                            readSync.wait();
                        } catch (InterruptedException e) {
                            continue;
                        }
                        continue;  // bufferData was set by setBuffer
                    }
                    // parent read returned true, break loop below
                }
                else
                {
                    /*
                     * If buffer conatins a data area then check if the type and 
                     * length fits. If yes then use it, otherwise allocate a new
                     * area and set it in buffer.
                     * 
                     * In case we can re-use the data area: copy the data, don't
                     * just set the bufferData into buffer because bufferData 
                     * points to a data area in another buffer instance.
                     */
                    Object data = buffer.getData();
                    byte[] tmpArray;
                    if (data instanceof byte[] && ((byte[])data).length >= bufferLength) {
                        tmpArray = (byte[])data;
                    }
                    else
                    {
                        tmpArray = new byte[bufferLength];
                        buffer.setData(tmpArray);
                    }
                    System.arraycopy(bufferData, 0, tmpArray, 0, bufferLength);
                    buffer.setFlags(Buffer.FLAG_SYSTEM_TIME);
                    buffer.setLength(bufferLength);
                    buffer.setOffset(0);
                    buffer.setTimeStamp(bufferTimeStamp);
                }
                break;
            }
            /*
             * The bufferData of this InputPortAudioStream has been consumed so
             * make sure a new piece of audio data will be read the next time.
             */
            bufferData = null;
        }
    }

    /**
     * Sets the audio data to be read from this <tt>InputPortAudioStream</tt>
     * upon the next request. Used by {@link #parentStream} in order to provide
     * all audio samples to all its slaves and not only to the one which caused
     * the actual read from PortAudio.
     *
     * @param bufferData the audio data to be read from this
     * <tt>InputPortAudioStream</tt> upon the next request
     * @param bufferLength the number of bytes in <tt>bufferData</tt> which
     * represent valid audio data to be read from this
     * <tt>InputPortAudioStream</tt> upon the next request
     * @param bufferTimeStamp the time stamp of the audio data in
     * <tt>bufferData</tt> represented in accord with
     * {@link Buffer#FLAG_SYSTEM_TIME}
     */
    void setBuffer(byte[] bufferData, int bufferLength, long bufferTimeStamp)
    {
        synchronized (readSync)
        {
            this.bufferData = bufferData;
            this.bufferLength = bufferLength;
            this.bufferTimeStamp = bufferTimeStamp;
            readSync.notifyAll();
        }
    }

    /**
     * Starts the stream. Also starts the parent stream
     * if its not already started.
     *
     * @throws PortAudioException if an error occurs while starting this
     * <tt>InputPortAudioStream</tt>
     */
    public synchronized void start()
        throws PortAudioException
    {
        if(!started)
        {
            parentStream.start(this);
            started = true;
        }
    }

    /**
     * Stops the stream. Also stops the parent if we are the last slave
     * stream that use it.
     *
     * @throws PortAudioException if an error occurs while stopping this
     * <tt>InputPortAudioStream</tt>
     */
    public synchronized void stop()
        throws PortAudioException
    {
        if(started)
        {
            stopping = true;
            parentStream.stop(this);
            started = false;
            stopping = false;
        }
    }
}
