/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a CoreVideo <tt>CVPixelBufferRef</tt>.
 *
 * @author Lubomir Marinov
 */
public class CVPixelBuffer
    extends CVImageBuffer
{

    /**
     * Initializes a new <tt>CVPixelBuffer</tt> instance which is to represent
     * a specific CoreVideo <tt>CVPixelBufferRef</tt>.
     *
     * @param ptr the CoreVideo <tt>CVPixelBufferRef</tt> to be represented by
     * the new instance
     */
    public CVPixelBuffer(long ptr)
    {
        super(ptr);
    }

    /**
     * Gets a <tt>byte</tt> array which represents the pixels of the associated
     * CoreVideo <tt>CVPixelBufferRef</tt>.
     *
     * @return a <tt>byte</tt> array which represents the pixels of the
     * associated CoreVideo <tt>CVPixelBufferRef</tt>
     */
    public byte[] getBytes()
    {
        return getBytes(getPtr());
    }

    /**
     * Gets a <tt>byte</tt> array which represents the pixels of a specific
     * CoreVideo <tt>CVPixelBufferRef</tt>.
     *
     * @param ptr the <tt>CVPixelBufferRef</tt> to get the pixel bytes of
     * @return a <tt>byte</tt> array which represents the pixels of the
     * specified CoreVideo <tt>CVPixelBufferRef</tt>
     */
    private static native byte[] getBytes(long ptr);

    /**
     * Gets the height in pixels of this <tt>CVPixelBuffer</tt>.
     *
     * @return the height in pixels of this <tt>CVPixelBuffer</tt>
     */
    public int getHeight()
    {
        return getHeight(getPtr());
    }

    /**
     * Gets the height in pixels of a specific CoreVideo
     * <tt>CVPixelBufferRef</tt>.
     *
     * @param ptr the CoreVideo <tt>CVPixelBufferRef</tt> to get the height in
     * pixels of
     * @return the height in pixels of the specified CoreVideo
     * <tt>CVPixelBufferRef</tt>
     */
    private static native int getHeight(long ptr);

    /**
     * Gets the width in pixels of this <tt>CVPixelBuffer</tt>.
     *
     * @return the width in pixels of this <tt>CVPixelBuffer</tt>
     */
    public int getWidth()
    {
        return getWidth(getPtr());
    }

    /**
     * Gets the width in pixels of a specific CoreVideo
     * <tt>CVPixelBufferRef</tt>.
     *
     * @param ptr the CoreVideo <tt>CVPixelBufferRef</tt> to get the width in
     * pixels of
     * @return the width in pixels of the specified CoreVideo
     * <tt>CVPixelBufferRef</tt>
     */
    private static native int getWidth(long ptr);
}
