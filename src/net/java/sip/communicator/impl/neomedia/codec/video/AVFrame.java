/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

/**
 * Represents a pointer to a native FFmpeg <tt>AVFrame</tt> object.
 *
 * @author Lubomir Marinov
 */
public class AVFrame
{
    /**
     * The pointer to the native FFmpeg <tt>AVFrame</tt> object represented by
     * this instance.
     */
    private final long ptr;

    /**
     * Initializes a new <tt>AVFrame</tt> instance which is to represent a
     * specific pointer to a native FFmpeg <tt>AVFrame</tt> object.
     *
     * @param ptr the pointer to the native FFmpeg <tt>AVFrame</tt> object to be
     * represented by the new instance
     */
    public AVFrame(long ptr)
    {
        if (ptr == 0)
            throw new IllegalArgumentException("ptr");

        this.ptr = ptr;
    }

    /**
     * Gets the pointer to the native FFmpeg <tt>AVFrame</tt> object represented
     * by this instance.
     *
     * @return the pointer to the native FFmpeg <tt>AVFrame</tt> object
     * represented by this instance
     */
    public long getPtr()
    {
        return ptr;
    }
}
