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
    /**
     * ARGB32 format type.
     */
    public static final int ARGB32 = 0;

    /**
     * RGB32 format type (first byte is ignored).
     */
    public static final int RGB32 = 1;

    /**
     * RBG24 format type.
     */
    public static final int RGB24 = 2;

    /**
     * Unknown format type.
     */
    public static final int UNKNOWN = 3;

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
    private int colorSpace = UNKNOWN;

    /**
     * Constructor.
     *
     * @param width video width
     * @param height video height
     * @param colorSpace color space
     */
    public DSFormat(int width, int height, int colorSpace)
    {
        this.width = width;
        this.height = height;
        this.colorSpace = colorSpace;
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
    public int getColorSpace()
    {
        return colorSpace;
    }
}

