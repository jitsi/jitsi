/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.awt.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

/**
 * Implements a <tt>VideoFormat</tt> with format parameters (like
 * {@link VideoMediaFormatImpl}) (some of) which (could) distinguish payload
 * types.
 *
 * @author Lyubomir Marinov
 */
public class ParameterizedVideoFormat
    extends VideoFormat
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The format parameters of this <tt>ParameterizedVideoFormat</tt> instance.
     */
    private Map<String, String> fmtps;

    /**
     * Constructs a new <tt>ParametizedVideoFormat</tt>.
     *
     * @param encoding encoding
     * @param size video size
     * @param maxDataLength maximum data length
     * @param dataType data type
     * @param frameRate frame rate
     * @param fmtps format parameters
     */
    public ParameterizedVideoFormat(
            String encoding,
            Dimension size,
            int maxDataLength,
            Class<?> dataType,
            float frameRate,
            Map<String, String> fmtps)
    {
        super(encoding, size, maxDataLength, dataType, frameRate);

        this.fmtps
            = ((fmtps == null) || fmtps.isEmpty())
                ? MediaFormatImpl.EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(fmtps);
    }

    /**
     * Initializes a new <tt>ParameterizedVideoFormat</tt> with a specific
     * encoding  and a specific set of format parameters.
     *
     * @param encoding the encoding of the new instance
     * @param fmtps the format parameters of the new instance
     */
    public ParameterizedVideoFormat(String encoding, Map<String, String> fmtps)
    {
        super(encoding);

        this.fmtps
            = ((fmtps == null) || fmtps.isEmpty())
                ? MediaFormatImpl.EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(fmtps);
    }

    /**
     * Initializes a new <tt>ParameterizedVideoFormat</tt> with a specific
     * encoding  and a specific set of format parameters.
     *
     * @param encoding the encoding of the new instance
     * @param fmtps the format parameters of the new instance in the form of an
     * array of <tt>String</tt>s in which the key and the value of an
     * association are expressed as consecutive elements.
     */
    public ParameterizedVideoFormat(String encoding, String... fmtps)
    {
        this(encoding, toMap(fmtps));
    }

    /**
     * Initializes a new <tt>ParameterizedVideoFormat</tt> instance which has
     * the same properties as this instance.
     *
     * @return a new <tt>ParameterizedVideoFormat</tt> instance which has the
     * same properties as this instance
     */
    @Override
    public Object clone()
    {
        ParameterizedVideoFormat f
            = new ParameterizedVideoFormat(
                    getEncoding(),
                    getSize(),
                    getMaxDataLength(),
                    getDataType(),
                    getFrameRate(),
                    /*
                     * The formatParameters will be copied by
                     * ParameterizedVideoFormat#copy(Format) bellow.
                     */
                    null);

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

        if (f instanceof ParameterizedVideoFormat)
        {
            ParameterizedVideoFormat pvf = (ParameterizedVideoFormat) f;
            Map<String, String> pvfFmtps = pvf.getFormatParameters();

            fmtps
                = ((pvfFmtps == null) || pvfFmtps.isEmpty())
                    ? MediaFormatImpl.EMPTY_FORMAT_PARAMETERS
                    : new HashMap<String, String>(pvfFmtps);
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
        if (!super.equals(obj))
            return false;

        Map<String, String> objFmtps = null;

        if (obj instanceof ParameterizedVideoFormat)
            objFmtps = ((ParameterizedVideoFormat) obj).getFormatParameters();
        return
            VideoMediaFormatImpl.formatParametersAreEqual(
                    getEncoding(),
                    getFormatParameters(), objFmtps);
    }

    /**
     * Returns whether or not the format parameters match.
     *
     * @param format format to test
     * @return true if the format parameters match.
     */
    public boolean formatParametersMatch(Format format)
    {
        Map<String, String> formatFmtps = null;

        if (format instanceof ParameterizedVideoFormat)
            formatFmtps
                = ((ParameterizedVideoFormat) format).getFormatParameters();
        return
            VideoMediaFormatImpl.formatParametersMatch(
                    getEncoding(),
                    getFormatParameters(), formatFmtps);
    }

    /**
     * Returns the format parameters value for the specified name.
     *
     * @param name format parameters name
     * @return value for the specified format parameters name
     */
    public String getFormatParameter(String name)
    {
        return fmtps.get(name);
    }

    /**
     * Returns the format parameters <tt>Map</tt>.
     *
     * @return the format parameters <tt>Map</tt>.
     */
    public Map<String, String> getFormatParameters()
    {
        return new HashMap<String, String>(fmtps);
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

        ((ParameterizedVideoFormat) intersection).fmtps
            = fmtps.isEmpty()
                ? MediaFormatImpl.EMPTY_FORMAT_PARAMETERS
                : getFormatParameters();
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
        return super.matches(format) && formatParametersMatch(format);
    }

    /**
     * Initializes a new <tt>Map</tt> from an array in which the key and the
     * value of an association are expressed as consecutive elements.
     *
     * @param <T> the very type of the keys and the values to be associated in
     * the new <tt>Map</tt>
     * @param entries the associations to be created in the new <tt>Map</tt>
     * where the key and value of an association are expressed as consecutive
     * elements
     * @return a new <tt>Map</tt> with the associations specified by
     * <tt>entries</tt>
     */
    public static <T> Map<T, T> toMap(T... entries)
    {
        Map<T, T> map;

        if ((entries == null) || (entries.length == 0))
            map = null;
        else
        {
            map = new HashMap<T, T>();
            for (int i = 0; i < entries.length; i++)
                map.put(entries[i++], entries[i]);
        }
        return map;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();

        s.append(super.toString());

        // fmtps
        {
            s.append(", fmtps={");
            for (Map.Entry<String, String> fmtp : fmtps.entrySet())
            {
                s.append(fmtp.getKey());
                s.append('=');
                s.append(fmtp.getValue());
                s.append(',');
            }

            int lastIndex = s.length() - 1;

            if (s.charAt(lastIndex) == ',')
                s.setCharAt(lastIndex, '}');
            else
                s.append('}');
        }
        return s.toString();
    }
}
