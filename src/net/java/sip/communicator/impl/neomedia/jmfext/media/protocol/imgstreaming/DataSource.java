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

import net.java.sip.communicator.impl.neomedia.codec.video.*;
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
    extends AbstractPullBufferCaptureDevice
{

    /**
     * Array of supported formats.
     */
    private static final Format[] formats
        = new Format[]
                {
                    new AVFrameFormat(
                            Toolkit.getDefaultToolkit().getScreenSize(),
                            Format.NOT_SPECIFIED,
                            FFmpeg.PIX_FMT_ARGB),
                     new RGBFormat(
                            Toolkit.getDefaultToolkit().getScreenSize(), // size
                            Format.NOT_SPECIFIED, // maxDataLength
                            Format.byteArray, // dataType
                            Format.NOT_SPECIFIED, // frameRate
                            32, // bitsPerPixel
                            2, // red
                            3, // green
                            4) // blue
                };

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
     * Create a new <tt>PullBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * in the list of streams of this <tt>PullBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PullBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PullBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPullBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new ImageStream(formatControl);
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
}
