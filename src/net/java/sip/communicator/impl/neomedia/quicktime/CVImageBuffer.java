/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a CoreVideo <tt>CVImageBufferRef</tt>.
 *
 * @author Lyubomir Marinov
 */
public class CVImageBuffer
{
    static
    {
        System.loadLibrary("jnquicktime");
    }

    /**
     * The CoreVideo <tt>CVImageBufferRef</tt> represented by this instance.
     */
    private long ptr;

    /**
     * Initializes a new <tt>CVImageBuffer</tt> instance which is to represent
     * a specific CoreVideo <tt>CVImageBufferRef</tt>.
     *
     * @param ptr the CoreVideo <tt>CVImageBufferRef</tt> to be represented by
     * the new instance
     */
    public CVImageBuffer(long ptr)
    {
        setPtr(ptr);
    }

    /**
     * Gets the CoreVideo <tt>CVImageBufferRef</tt> represented by this
     * instance.
     *
     * @return the CoreVideo <tt>CVImageBufferRef</tt> represented by this
     * instance
     */
    protected long getPtr()
    {
        return ptr;
    }

    /**
     * Sets the CoreVideo <tt>CVImageBufferRef</tt> represented by this
     * instance.
     *
     * @param ptr the CoreVideo <tt>CVImageBufferRef</tt> to be represented by
     * this instance
     */
    protected void setPtr(long ptr)
    {
        if (ptr == 0)
            throw new IllegalArgumentException("ptr");

        this.ptr = ptr;
    }
}
