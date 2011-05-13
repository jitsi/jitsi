/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.awt.*;
import java.util.*;

import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>VideoMediaFormat</tt> for the JMF <tt>VideoFormat</tt>.
 *
 * @author Lyubomir Marinov
 */
public class VideoMediaFormatImpl
    extends MediaFormatImpl<VideoFormat>
    implements VideoMediaFormat
{

    /**
     * The default value of the <tt>clockRate</tt> property of
     * <tt>VideoMediaFormatImpl</tt>.
     */
    public static final double DEFAULT_CLOCK_RATE = 90000;

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
    VideoMediaFormatImpl(String encoding)
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
    VideoMediaFormatImpl(String encoding, double clockRate)
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
    VideoMediaFormatImpl(VideoFormat format)
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
    VideoMediaFormatImpl(VideoFormat format, double clockRate)
    {
        this(format, clockRate, null, null);
    }

    /**
     * Initializes a new <tt>VideoMediaFormatImpl</tt> instance which is to
     * provide an implementation of <tt>VideoMediaFormat</tt> for a specific
     * JMF <tt>VideoFormat</tt> and to have specific clock rate and set of
     * format-specific parameters.
     *
     * @param format the JMF <tt>VideoFormat</tt> the new instance is to wrap
     * and provide an implementation of <tt>VideoMediaFormat</tt> for
     * @param clockRate the clock rate of the new <tt>VideoMediaFormatImpl</tt>
     * instance
     * @param formatParameters the set of format-specific parameters of the new
     * instance
     * @param advancedParameters set of advanced parameters of the new instance
     */
    VideoMediaFormatImpl(
            VideoFormat format,
            double clockRate,
            Map<String, String> formatParameters,
            Map<String, String> advancedParameters)
    {
        super(format, formatParameters, advancedParameters);

        this.clockRate = clockRate;
    }

    /**
     * Implements <tt>MediaFormat#equals(Object)</tt> and actually compares the
     * encapsulated JMF <tt>Format</tt> instances.
     *
     * @param mediaFormat the object that we'd like to compare <tt>this</tt> one
     * to
     * @return <tt>true</tt> if the JMF <tt>Format</tt> instances encapsulated
     * by this instance and their other characteristics are equal;
     * <tt>false</tt>, otherwise.
     * @see MediaFormatImpl#equals(Object)
     */
    @Override
    public boolean equals(Object mediaFormat)
    {
        if (this == mediaFormat)
            return true;

        if (!super.equals(mediaFormat))
            return false;

        VideoMediaFormatImpl videoMediaFormatImpl
            = (VideoMediaFormatImpl) mediaFormat;

        double clockRate = getClockRate();
        double videoMediaFormatImplClockRate
            = videoMediaFormatImpl.getClockRate();

        if (MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED == clockRate)
            clockRate = DEFAULT_CLOCK_RATE;
        if (MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED
                == videoMediaFormatImplClockRate)
            videoMediaFormatImplClockRate = DEFAULT_CLOCK_RATE;

        return (clockRate == videoMediaFormatImplClockRate);
    }

    /**
     * {@inheritDoc}
     *
     * @see MediaFormatImpl#formatParametersAreEqual(Map, Map)
     */
    @Override
    protected boolean formatParametersAreEqual(
            Map<String, String> fmtps1,
            Map<String, String> fmtps2)
    {
        return formatParametersAreEqual(getEncoding(), fmtps1, fmtps2);
    }

    /**
     * Determines whether a specific set of format parameters is equal to
     * another set of format parameters in the sense that they define an equal
     * number of parameters and assign them equal values. Since the values are
     * <tt>String</tt>s, presumes that a value of <tt>null</tt> is equal to the
     * empty <tt>String</tt>.
     * <p>
     * The two <tt>Map</tt> instances of format parameters to be checked for
     * equality are presumed to be modifiable in the sense that if the lack of a
     * format parameter in a given <tt>Map</tt> is equivalent to it having a
     * specific value, an association of the format parameter to the value in
     * question may be added to or removed from the respective <tt>Map</tt>
     * instance for the purposes of determining equality.
     * </p>
     *
     * @param encoding the encoding (name) related to the two sets of format
     * parameters to be tested for equality
     * @param fmtps1 the first set of format parameters to be tested for
     * equality
     * @param fmtps2 the second set of format parameters to be tested for
     * equality
     * @return <tt>true</tt> if the specified sets of format parameters are
     * equal; <tt>false</tt>, otherwise
     */
    public static boolean formatParametersAreEqual(
            String encoding,
            Map<String, String> fmtps1, Map<String, String> fmtps2)
    {
        return
            MediaFormatImpl.formatParametersAreEqual(encoding, fmtps1, fmtps2);
    }

    /**
     * Determines whether the format parameters of this <tt>MediaFormat</tt>
     * match a specific set of format parameters.
     *
     * @param fmtps the set of format parameters to match to the format
     * parameters of this <tt>MediaFormat</tt>
     * @return <tt>true</tt> if this <tt>MediaFormat</tt> considers
     * <tt>fmtps</tt> matching its format parameters; otherwise, <tt>false</tt>
     */
    @Override
    public boolean formatParametersMatch(Map<String, String> fmtps)
    {
        return
            formatParametersMatch(getEncoding(), getFormatParameters(), fmtps)
                && super.formatParametersMatch(fmtps);
    }

    /**
     * Determines whether two sets of format parameters match in the context of
     * a specific encoding.
     *
     * @param encoding the encoding (name) related to the two sets of format
     * parameters to be matched.
     * @param fmtps1 the first set of format parameters which is to be matched
     * against <tt>fmtps2</tt>
     * @param fmtps2 the second set of format parameters which is to be matched
     * against <tt>fmtps1</tt>
     * @return <tt>true</tt> if the two sets of format parameters match in the
     * context of the specified <tt>encoding</tt>; otherwise, <tt>false</tt>
     */
    public static boolean formatParametersMatch(
            String encoding,
            Map<String, String> fmtps1 , Map<String, String> fmtps2)
    {
        return true;
    }

    /**
     * Gets the clock rate associated with this <tt>MediaFormat</tt>.
     *
     * @return the clock rate associated with this <tt>MediaFormat</tt>
     * @see MediaFormat#getClockRate()
     */
    public double getClockRate()
    {
        return clockRate;
    }

    /**
     * Gets the type of this <tt>MediaFormat</tt> which is
     * {@link MediaType#VIDEO} for <tt>AudioMediaFormatImpl</tt> instances.
     *
     * @return the <tt>MediaType</tt> that this format represents and which is
     * <tt>MediaType.VIDEO</tt> for <tt>AudioMediaFormatImpl</tt> instances
     * @see MediaFormat#getMediaType()
     */
    public final MediaType getMediaType()
    {
        return MediaType.VIDEO;
    }

    /**
     * Gets the size of the image that this <tt>VideoMediaFormat</tt> describes.
     *
     * @return a {@link Dimension} instance indicating the image size (in
     * pixels) of this <tt>VideoMediaFormat</tt>
     * @see VideoMediaFormat#getSize()
     */
    public Dimension getSize()
    {
        return format.getSize();
    }

    /**
     * Overrides <tt>MediaFormatImpl#hashCode()</tt> because
     * <tt>Object#equals(Object)</tt> is overridden.
     *
     * @return a hash code value for this <tt>VideoMediaFormatImpl</tt>
     * @see MediaFormatImpl#hashCode()
     */
    @Override
    public int hashCode()
    {
        double clockRate = getClockRate();

        /*
         * The implementation of #equals(Object) of this instance assumes that
         * MediaFormatFactory#CLOCK_RATE_NOT_SPECIFIED and #DEFAULT_CLOCK_RATE
         * are equal.
         */
        if (MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED == clockRate)
            clockRate = DEFAULT_CLOCK_RATE;
        return (super.hashCode() | Double.valueOf(clockRate).hashCode());
    }
}
