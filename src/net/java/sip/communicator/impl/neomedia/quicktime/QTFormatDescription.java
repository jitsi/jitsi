/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

import java.awt.*;

/**
 * Describes the media format of media samples and of media sources, such as
 * devices and capture connections. Includes basic information about the media,
 * such as media type and format type (or codec type), as well as extended
 * information specific to each media type.
 *
 * @author Lubomir Marinov
 */
public class QTFormatDescription
    extends NSObject
{
    public static final String VideoEncodedPixelsSizeAttribute;

    static
    {
        VideoEncodedPixelsSizeAttribute = VideoEncodedPixelsSizeAttribute();
    }

    /**
     * Initializes a new <tt>QTFormatDescription</tt> instance which is to
     * represent a specific QTKit <tt>QTFormatDescription</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTFormatDescription</tt> object
     * which is to be represented by the new instance
     */
    public QTFormatDescription(long ptr)
    {
        super(ptr);
    }

    /**
     * Called by the garbage collector to release system resources and perform
     * other cleanup.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
    {
        release();
    }

    public Dimension sizeForKey(String key)
    {
        return sizeForKey(getPtr(), key);
    }

    private static native Dimension sizeForKey(long ptr, String key);

    private static native String VideoEncodedPixelsSizeAttribute();
}
