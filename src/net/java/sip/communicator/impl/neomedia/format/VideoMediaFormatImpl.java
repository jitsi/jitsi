/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.awt.*;

import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>VideoMediaFormat</tt> for the JMF <tt>VideoFormat</tt>
 *
 * @author Lubomir Marinov
 */
public class VideoMediaFormatImpl
    extends MediaFormatImpl<VideoFormat>
    implements VideoMediaFormat
{

    /**
     * The default value of the <tt>clockRate</tt> property of
     * <tt>VideoMediaFormatImpl</tt>.
     */
    private static final double DEFAULT_CLOCK_RATE = 90000;

    /**
     * The clock rate of this <tt>VideoMediaFormat</tt>.
     */
    private final double clockRate;

    /**
     * Initializes a new <tt>VideoMediaFormatImpl</tt> instance with a specific
     * encoding.
     *
     * @param encoding the encoding of the new <tt>VideoMediaFormatImpl</tt>
     * instance
     */
    public VideoMediaFormatImpl(String encoding)
    {
        this(encoding, DEFAULT_CLOCK_RATE);
    }

    /**
     * Initializes a new <tt>VideoMediaFormatImpl</tt> instance with a specific
     * encoding and a specific clock rate.
     *
     * @param encoding the encoding of the new <tt>VideoMediaFormatImpl</tt>
     * instance
     * @param clockRate the clock rate of the new <tt>VideoMediaFormatImpl</tt>
     * instance
     */
    public VideoMediaFormatImpl(String encoding, double clockRate)
    {
        this(new VideoFormat(encoding), clockRate);
    }

    /**
     * Initializes a new <tt>VideoMediaFormatImpl</tt> instance which is to
     * provide an implementation of <tt>VideoMediaFormat</tt> for a specific
     * JMF <tt>VideoFormat</tt>.
     *
     * @param format the JMF <tt>VideoFormat</tt> the new instance is to wrap
     * and provide an implementation of <tt>VideoMediaFormat</tt> for
     */
    public VideoMediaFormatImpl(VideoFormat format)
    {
        this(format, DEFAULT_CLOCK_RATE);
    }

    /**
     * Initializes a new <tt>VideoMediaFormatImpl</tt> instance which is to
     * provide an implementation of <tt>VideoMediaFormat</tt> for a specific
     * JMF <tt>VideoFormat</tt> and to have a specific clock rate.
     *
     * @param format the JMF <tt>VideoFormat</tt> the new instance is to wrap
     * and provide an implementation of <tt>VideoMediaFormat</tt> for
     * @param clockRate the clock rate of the new <tt>VideoMediaFormatImpl</tt>
     * instance
     */
    public VideoMediaFormatImpl(VideoFormat format, double clockRate)
    {
        super(format);

        this.clockRate = clockRate;
    }

    /*
     * Overrides MediaFormatImpl#equals(Object).
     */
    public boolean equals(Object mediaFormat)
    {
        if (this == mediaFormat)
            return true;

        if (!super.equals(mediaFormat))
            return false;

        VideoMediaFormatImpl videoMediaFormatImpl
            = (VideoMediaFormatImpl) mediaFormat;

        return (getClockRate() == videoMediaFormatImpl.getClockRate());
    }

    /*
     * Implements MediaFormat#getClockRate().
     */
    public double getClockRate()
    {
        return clockRate;
    }

    /*
     * Implements MediaFormat#getMediaType(). Returns MediaType#VIDEO.
     */
    public final MediaType getMediaType()
    {
        return MediaType.VIDEO;
    }

    /*
     * Implements VideoMediaFormat#getSize(). Delegates to VideoFormat.
     */
    public Dimension getSize()
    {
        return format.getSize();
    }

    /*
     * Overrides MediaFormatImpl#hashCode() because Object#equals(Object) is
     * overridden.
     */
    public int hashCode()
    {
        return (super.hashCode() | (int) getClockRate()); 
    }
}
