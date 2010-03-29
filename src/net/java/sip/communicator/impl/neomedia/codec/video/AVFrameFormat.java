/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

/**
 * Implements a <tt>VideoFormat</tt> for a <tt>Buffer</tt> carrying
 * <tt>AVFrame</tt> as its <tt>data</tt>. While the <tt>AVFrameFormat</tt> class
 * is not strictly necessary and <tt>VideoFormat</tt> could have be directly
 * used, it is conceived as an appripriate way to avoid possible matching with
 * other <tt>VideoFormat</tt>s and a very obvious one.
 *
 * @author Lubomir Marinov
 */
public class AVFrameFormat
    extends VideoFormat
{
    private int pixFmt;

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance with unspecified size
     * and frame rate.
     */
    public AVFrameFormat()
    {
        this(null, NOT_SPECIFIED);
    }

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance with specific size and
     * frame rate.
     */
    public AVFrameFormat(Dimension size, float frameRate)
    {
        super("AVFrame", size, NOT_SPECIFIED, AVFrame.class, frameRate);

        this.pixFmt = FFmpeg.PIX_FMT_YUV420P;
    }

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance which has the same
     * properties as this instance.
     *
     * @return a new <tt>AVFrameFormat</tt> instance which has the same
     * properties as this instance
     */
    @Override
    public Object clone()
    {
        AVFrameFormat f = new AVFrameFormat(size, frameRate);

        f.copy(this);
        return f;
    }

    @Override
    protected void copy(Format f)
    {
        super.copy(f);

        if (f instanceof AVFrameFormat)
        {
            AVFrameFormat avFrameFormat = (AVFrameFormat) f;

            pixFmt = avFrameFormat.pixFmt;
        }
    }

    /**
     * Determines whether a specific <tt>Object</tt> represents a value that is
     * equal to the value represented by this instance.
     *
     * @param obj the <tt>Object</tt> to be determined whether it represents a
     * value that is equal to the value represented by this instance
     * @return <tt>true</tt> if the specified <tt>obj</tt> represents a value
     * that is equal to the value represented by this instance; otherwise,
     * <tt>false</tt>
     */
    @Override
    public boolean equals(Object obj)
    {
        if ((obj instanceof AVFrameFormat) && super.equals(obj))
        {
            AVFrameFormat avFrameFormat = (AVFrameFormat) obj;

            return (pixFmt == avFrameFormat.pixFmt);
        }
        else
            return false;
    }

    public int getPixFmt()
    {
        return pixFmt;
    }
}
