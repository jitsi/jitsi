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

    /**
     * The RGB24 constant.
     */
    public static final long RGB24;

    /**
     * The RGB32 constant.
     */
    public static final long RGB32;

    /**
     * The ARGB32 constant.
     */
    public static final long ARGB32;

    /**
     * The YUY2 constant.
     */
    public static final long YUY2;

    /**
     * The UYVY constant.
     */
    public static final long UYVY;

    /**
     * The Y411 constant.
     */
    public static final long Y411;

    /**
     * The Y41P constant.
     */
    public static final long Y41P;

    /**
     * The NV12 constant.
     */
    public static final long NV12;

    /**
     * The I420 constant.
     */
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
    /**
     * Get the RGB24 native pixel format
     *
     * @return RGB24 native format value
     */
    public static native long getRGB24PixelFormat();

    /**
     * Get the RGB32 native pixel format
     *
     * @return RGB32 native format value
     */
    public static native long getRGB32PixelFormat();

    /**
     * Get the ARGB32 native pixel format
     *
     * @return ARGB32 native format value
     */
    public static native long getARGBPixelFormat();

    /* YUV */

    /**
     * Get the AYUV native pixel format
     *
     * @return AYUV native format value
     */
    public static native long getAYUVPixelFormat();

    /**
     * Get the YUY2 native pixel format
     *
     * @return YUY2 native format value
     */
    public static native long getYUY2PixelFormat();

    /**
     * Get the UYVY native pixel format
     *
     * @return UYVY native format value
     */
    public static native long getUYVYPixelFormat();

    /**
     * Get the IMC1 native pixel format
     *
     * @return IMC1 native format value
     */
    public static native long getIMC1PixelFormat();

    /**
     * Get the IMC2 native pixel format
     *
     * @return IMC2 native format value
     */
    public static native long getIMC2PixelFormat();

    /**
     * Get the IMC3 native pixel format
     *
     * @return IMC3 native format value
     */
    public static native long getIMC3PixelFormat();

    /**
     * Get the IMC4 native pixel format
     *
     * @return IMC4 native format value
     */
    public static native long getIMC4PixelFormat();

    /**
     * Get the YV12 native pixel format
     *
     * @return YV12 native format value
     */
    public static native long getYV12PixelFormat();

    /**
     * Get the NV12 native pixel format
     *
     * @return NV12 native format value
     */
    public static native long getNV12PixelFormat();

    /**
     * Get the IF09 native pixel format
     *
     * @return IF09 native format value
     */
    public static native long getIF09PixelFormat();

    /**
     * Get the IYUV native pixel format
     *
     * @return IYUV native format value
     */
    public static native long getIYUVPixelFormat();

    /**
     * Get the Y211 native pixel format
     *
     * @return Y211 native format value
     */
    public static native long getY211PixelFormat();

    /**
     * Get the Y411 native pixel format
     *
     * @return Y411 native format value
     */
    public static native long getY411PixelFormat();

    /**
     * Get the Y41P native pixel format
     *
     * @return Y41P native format value
     */
    public static native long getY41PPixelFormat();

    /**
     * Get the YVU9 native pixel format
     *
     * @return YVU9 native format value
     */
    public static native long getYVU9PixelFormat();

    /**
     * Get the YVYU native pixel format
     *
     * @return YVYU native format value
     */
    public static native long getYVYUPixelFormat();

    /**
     * Get the I420 native pixel format
     *
     * @return I420 native format value
     */
    public static native long getI420PixelFormat();
}

