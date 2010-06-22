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
 * used, it is conceived as an appropriate way to avoid possible matching with
 * other <tt>VideoFormat</tt>s and a very obvious one.
 *
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class AVFrameFormat
    extends VideoFormat
{

    /**
     * The encoding of the <tt>AVFrameFormat</tt> instances.
     */
    public static final String AVFRAME = "AVFrame";

    /**
     * The native FFmpeg format represented by this instance.
     */
    private int pixFmt;

    /**
     * Native format of the capture device. Because some native capture device
     * formats can correspond to the same FFmpeg format, JMF's stream is unable
     * to differentiate them. So having the native format here
     * allow JMF stream to know it directly.
     */
    private int devicePixFmt;

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance with unspecified size,
     * frame rate and FFmpeg colorspace.
     */
    public AVFrameFormat()
    {
        this(NOT_SPECIFIED, NOT_SPECIFIED);
    }

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance with a specific FFmpeg
     * colorspace and unspecified size and frame rate.
     *
     * @param pixFmt the FFmpeg colorspace to be represented by the new instance
     * @param devicePixFmt the capture device colorspace to be represented by
     * the new instance
     */
    public AVFrameFormat(int pixFmt, int devicePixFmt)
    {
        this(null, NOT_SPECIFIED, pixFmt, devicePixFmt);
    }

    /**
     * Initializes a new <tt>AVFrameFormat</tt> instance with specific size,
     * frame rate and FFmpeg colorspace.
     *
     * @param size the <tt>Dimension</tt> of the new instance
     * @param frameRate the frame rate of the new instance
     * @param pixFmt the FFmpeg colorspace to be represented by the new instance
     * @param devicePixFmt the capture device colorspace to be represented by
     * the new instance
     */
    public AVFrameFormat(Dimension size, float frameRate, int pixFmt,
            int devicePixFmt)
    {
        super(AVFRAME, size, NOT_SPECIFIED, AVFrame.class, frameRate);

        this.pixFmt = pixFmt;
        this.devicePixFmt = devicePixFmt;
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
        AVFrameFormat f = new AVFrameFormat(size, frameRate, pixFmt,
                devicePixFmt);

        f.copy(this);
        return f;
    }

    /**
     * Copies the properties of the specified <tt>Format</tt> into this
     * instance.
     *
     * @param f the <tt>Format</tt> the properties of which are to be copied
     * into this instance
     */
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

    /**
     * Gets the native FFmpeg format represented by this instance.
     *
     * @return the native FFmpeg format represented by this instance
     */
    public int getPixFmt()
    {
        return pixFmt;
    }

    /**
     * Gets the native capture device format represented by this instance.
     *
     * @return native capture device format reprensented by this instance
     */
    public int getDevicePixFmt()
    {
        return devicePixFmt;
    }

    /**
     * Finds the attributes shared by two matching <tt>Format</tt>s. If the
     * specified <tt>Format</tt> does not match this one, the result is
     * undefined.
     *
     * @param format the matching <tt>Format</tt> to intersect with this one
     * @return a <tt>Format</tt> with its attributes set to the attributes
     * common to this instance and the specified <tt>format</tt>
     */
    @Override
    public Format intersects(Format format)
    {
        Format intersection = super.intersects(format);

        if (intersection == null)
            return null;
        if (!(format instanceof AVFrameFormat))
            return intersection;

        ((AVFrameFormat) intersection).pixFmt
            = (pixFmt == NOT_SPECIFIED)
                ? ((AVFrameFormat) format).pixFmt
                : pixFmt;
        return intersection;
    }

    /**
     * Determines whether a specific format matches this instance i.e. whether
     * their attributes match according to the definition of "match" given by
     * {@link Format#matches(Format)}.
     *
     * @param format the <tt>Format</tt> to compare to this instance
     * @return <tt>true</tt> if the specified <tt>format</tt> matches this one;
     * otherwise, <tt>false</tt>
     */
    @Override
    public boolean matches(Format format)
    {
        if (!super.matches(format))
            return false;
        if (!(format instanceof AVFrameFormat))
            return true;

        AVFrameFormat avFrameFormat = (AVFrameFormat) format;

        return
            (pixFmt == NOT_SPECIFIED
                || (avFrameFormat.pixFmt == NOT_SPECIFIED)
                || (pixFmt == avFrameFormat.pixFmt));
    }
}
