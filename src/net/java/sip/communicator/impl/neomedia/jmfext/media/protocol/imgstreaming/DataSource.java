/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.awt.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;

/**
 * DataSource for our image streaming (which is used for 
 * Desktop streaming).
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class DataSource
    extends AbstractPushBufferCaptureDevice
{

    /**
     * Array of supported formats.
     */
    private static final Format formats[];

    /**
     * Resolutions supported for image. Listed in decreasing order of
     * preference.
     */
    private static final Dimension res[] = new Dimension[] {
        Toolkit.getDefaultToolkit().getScreenSize(), /* current desktop screen size */
        new Dimension(1024, 768),
        new Dimension(800, 600),
        new Dimension(720, 480),
        new Dimension(704, 576),
        new Dimension(352, 288),
        new Dimension(320, 240),
        new Dimension(176, 144),
        new Dimension(128, 96),
    };

    static
    {
        /* initialize supported format array */
        int i = 0;

        formats = new Format[res.length];
        for(Dimension dim : res)
        {
            formats[i]
                = new RGBFormat(
                        dim, /* resolution */
                        (int)(dim.getWidth() * dim.getHeight() * 4), /* max data length */
                        Format.byteArray, /* format */
                        -1.0f, /* frame rate */
                        32, /* color is coded in 24 bit/pixel */
                        1,
                        2,
                        3); /* color masks (red, green, blue) */
            i++;
        }
    }

    /**
     * Get supported formats.
     *
     * @return supported formats
     */
    public static Format[] getFormats()
    {
        return formats;
    }

    /**
     * Constructor.
     */
    public DataSource()
    {
    }

    /**
     * Constructor.
     *
     * @param locator associated <tt>MediaLocator</tt>
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
    }

    /**
     * Create a new <tt>PushBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PushBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPushBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPushBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new ImageStream(formatControl);
    }
}
