/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * Represents a buffer of native memory with a specific size/capacity which
 * either contains a specific number of bytes of valid data or is free for
 * consumption.
 *
 * @author Lubomir Marinov
 */
public class ByteBuffer
{

    /**
     * The maximum number of bytes which can be written into the native memory
     * represented by this instance.
     */
    public final int capacity;

    /**
     * The indicator which determines whether this instance is free to be
     * written bytes into.
     */
    private boolean free;

    /**
     * The number of bytes of valid data that the native memory represented by
     * this instance contains.
     */
    private int length;

    /**
     * The pointer to the native memory represented by this instance.
     */
    public final long ptr;

    /**
     * Initializes a new <tt>ByteBuffer</tt> instance with a specific
     * <tt>capacity</tt>.
     *
     * @param capacity the maximum number of bytes which can be written into the
     * native memory represented by the new instance
     */
    public ByteBuffer(int capacity)
    {
        this.capacity = capacity;
        this.ptr = FFmpeg.av_malloc(this.capacity);

        this.free = true;
        this.length = 0;

        if (this.ptr == 0)
        {
            throw
                new OutOfMemoryError(
                        getClass().getSimpleName()
                            + " with capacity "
                            + this.capacity);
        }
    }

    /**
     * Gets the number of bytes of valid data that the native memory represented
     * by this instance contains.
     *
     * @return the number of bytes of valid data that the native memory
     * represented by this instance contains
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Determines whether this instance is free to be written bytes into.
     *
     * @return <tt>true</tt> if this instance is free to be written bytes into
     * or <tt>false</tt> is the native memory represented by this instance is
     * already is use
     */
    public boolean isFree()
    {
        return free;
    }

    /**
     * Sets the indicator which determines whether this instance is free to be
     * written bytes into.
     *
     * @param free <tt>true</tt> if this instance is to be made available for
     * writing bytes into; otherwise, <tt>false</tt>
     */
    public void setFree(boolean free)
    {
        this.free = free;
        if (this.free)
            setLength(0);
    }

    /**
     * Sets the number of bytes of valid data that the native memory represented
     * by this instance contains.
     *
     * @param length the number of bytes of valid data that the native memory
     * represented by this instance contains
     */
    public void setLength(int length)
    {
        this.length = length;
    }
}
