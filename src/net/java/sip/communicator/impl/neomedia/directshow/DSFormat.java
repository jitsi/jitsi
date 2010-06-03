/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.directshow;

/**
 * DirectShow video format.
 *
 * @author Sebastien Vincent
 */
public class DSFormat
{

    static
    {
        System.loadLibrary("jdirectshow");

        RGB24 = getRGB24PixelFormat();
        RGB32 = getRGB32PixelFormat();
        ARGB32 = getARGBPixelFormat();
        YUY2 = getYUY2PixelFormat();
        UYVY = getUYVYPixelFormat();
        NV12 = getNV12PixelFormat();
        Y411 = getY411PixelFormat();
        Y41P = getY41PPixelFormat();
        I420 = getI420PixelFormat();
    }

    /* supported formats */
    public static final long RGB24;
    public static final long RGB32;
    public static final long ARGB32;
    public static final long YUY2;
    public static final long UYVY;
    public static final long Y411;
    public static final long Y41P;
    public static final long NV12;
    public static final long I420;

    /**
     * Video width.
     */
    private int width = 0;

    /**
     * Video height.
     */
    private int height = 0;

    /**
     * Color space.
     */
    private long pixelFormat = -1;

    /**
     * Constructor.
     *
     * @param width video width
     * @param height video height
     * @param pixelFormat pixel format
     */
    public DSFormat(int width, int height, long pixelFormat)
    {
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
    }

    /**
     * Get video width.
     *
     * @return video width
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Get video height.
     *
     * @return video height
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Get color space.
     *
     * @return color space
     */
    public long getPixelFormat()
    {
        return pixelFormat;
    }

    /* RGB */
    public static native long getRGB24PixelFormat();
    public static native long getRGB32PixelFormat();
    public static native long getARGBPixelFormat();

    /* YUV */
    public static native long getAYUVPixelFormat();
    public static native long getYUY2PixelFormat();
    public static native long getUYVYPixelFormat();
    public static native long getIMC1PixelFormat();
    public static native long getIMC2PixelFormat();
    public static native long getIMC3PixelFormat();
    public static native long getIMC4PixelFormat();
    public static native long getYV12PixelFormat();
    public static native long getNV12PixelFormat();
    public static native long getIF09PixelFormat();
    public static native long getIYUVPixelFormat();
    public static native long getY211PixelFormat();
    public static native long getY411PixelFormat();
    public static native long getY41PPixelFormat();
    public static native long getYVU9PixelFormat();
    public static native long getYVYUPixelFormat();
    public static native long getI420PixelFormat();
}

