/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Defines the types of <tt>CVPixelBuffer</tt>s to be output by
 * <tt>QTCaptureDecompressedVideoOutput</tt>.
 *
 * @author Lubomir Marinov
 */
public final class CVPixelFormatType
{

    /** 24 bit RGB */
    public static final int kCVPixelFormatType_24RGB = 0x00000018;

    /** 32 bit ARGB */
    public static final int kCVPixelFormatType_32ARGB = 0x00000020;

    /** Planar Component Y'CbCr 8-bit 4:2:0. */
    public static final int kCVPixelFormatType_420YpCbCr8Planar = 0x79343230;

    /**
     * Prevents the initialization of <tt>CVPixelFormatType</tt> instances.
     */
    private CVPixelFormatType()
    {
    }
}
