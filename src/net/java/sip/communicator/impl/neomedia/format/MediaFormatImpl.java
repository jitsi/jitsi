/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaFormat</tt> for the JMF <tt>Format</tt>.
 *
 * @param <T> the type of the wrapped <tt>Format</tt>
 * 
 * @author Lubomir Marinov
 */
public abstract class MediaFormatImpl<T extends Format>
    implements MediaFormat
{

    /**
     * The value of the <tt>formatParameters</tt> property of
     * <tt>MediaFormatImpl</tt> when no codec-specific parameters have been
     * received via SIP/SDP or XMPP/Jingle. Explicitly defined in order to
     * reduce unnecessary allocations.
     */
    private static final Map<String, String> EMPTY_FORMAT_PARAMETERS
        = Collections.emptyMap();

    /**
     * Creates a new <tt>MediaFormat</tt> instance for a specific JMF
     * <tt>Format</tt>.
     *
     * @param format the JMF <tt>Format</tt> the new instance is to provide an
     * implementation of <tt>MediaFormat</tt> for
     * @return a new <tt>MediaFormat</tt> instance for the specified JMF
     * <tt>Format</tt>
     */
    public static MediaFormat createInstance(Format format)
    {
        if (format instanceof AudioFormat)
            return new AudioMediaFormatImpl((AudioFormat) format);
        if (format instanceof VideoFormat)
            return new VideoMediaFormatImpl((VideoFormat) format);
        return null;
    }

    /**
     * Determines whether a specific set of format parameters is equal to
     * another set of format parameters in the sense that they define an equal
     * number of parameters and assign them equal values. Since the values are
     * <tt>String</tt>s, presumes that a value of <tt>null</tt> is equal to the
     * empty <tt>String</tt>.
     * 
     * @param formatParameters1 the first set of format parameters to be tested
     * for equality 
     * @param formatParameters2 the second set of format parameters to be tested
     * for equality
     * @return <tt>true</tt> if the specified sets of format parameters are
     * equal; <tt>false</tt>, otherwise
     */
    private static boolean formatParametersAreEqual(
            Map<String, String> formatParameters1,
            Map<String, String> formatParameters2)
    {
        if (formatParameters1 == null)
            return
                (formatParameters2 == null)
                    || (formatParameters2.size() == 0);
        if (formatParameters2 == null)
            return
                (formatParameters1 == null)
                    || (formatParameters1.size() == 0);
        if (formatParameters1.size() == formatParameters2.size())
        {
            for (Map.Entry<String, String> formatParameter1
                    : formatParameters1.entrySet())
            {
                String key1 = formatParameter1.getKey();

                if (!formatParameters2.containsKey(key1))
                    return false;

                String value1 = formatParameter1.getValue();
                String value2 = formatParameters2.get(key1);

                /*
                 * Since the values are strings, allow null to be equal to the
                 * empty string.
                 */
                if ((value1 == null) || (value1.length() == 0))
                {
                    if ((value2 != null) && (value2.length() > 0))
                        return false;
                }
                else if (!value1.equals(value2))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    /**
     * The JMF <tt>Format</tt> this instance wraps and provides an
     * implementation of <tt>MediaFormat</tt> for.
     */
    protected final T format;

    /**
     * The codec-specific parameters of this instance which have been received
     * via SIP/SDP or XMPP/Jingle.
     */
    private final Map<String, String> formatParameters;

    /**
     * Initializes a new <tt>MediaFormatImpl</tt> instance which is to provide
     * an implementation of <tt>MediaFormat</tt> for a specific <tt>Format</tt>.
     *
     * @param format the JMF <tt>Format</tt> the new instance is to provide an
     * implementation of <tt>MediaFormat</tt> for
     */
    public MediaFormatImpl(T format)
    {
        this(format, null);
    }

    /**
     * Initializes a new <tt>MediaFormatImpl</tt> instance which is to provide
     * an implementation of <tt>MediaFormat</tt> for a specific <tt>Format</tt>
     * and which is to have a specific set of codec-specific parameters.
     *
     * @param format the JMF <tt>Format</tt> the new instance is to provide an
     * implementation of <tt>MediaFormat</tt> for
     * @param formatParameters any codec-specific parameters that have been
     * received via SIP/SDP or XMPP/Jingle
     */
    public MediaFormatImpl(T format, Map<String, String> formatParameters)
    {
        if (format == null)
            throw new NullPointerException("format");

        this.format = format;
        this.formatParameters
            = ((formatParameters == null) || formatParameters.isEmpty())
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(formatParameters);
    }

    /*
     * Implements MediaFormat#equals(Object).
     */
    public boolean equals(Object mediaFormat)
    {
        if (this == mediaFormat)
            return true;

        if (!getClass().isInstance(mediaFormat))
            return false;

        @SuppressWarnings("unchecked")
        MediaFormatImpl<T> mediaFormatImpl = (MediaFormatImpl<T>) mediaFormat;

        return
            format.equals(mediaFormatImpl.format)
                && formatParametersAreEqual(
                        getFormatParameters(),
                        mediaFormatImpl.getFormatParameters());
    }

    /*
     * Implements MediaFormat#getEncoding().
     */
    public String getEncoding()
    {
        return format.getEncoding();
    }

    /*
     * Implements MediaFormat#getFormatParameters(). Returns a copy of the
     * format properties of this instance. Modifications to the returned Map do
     * no affect the format properties of this instance.
     */
    public Map<String, String> getFormatParameters()
    {
        return
            (formatParameters == EMPTY_FORMAT_PARAMETERS)
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(formatParameters);
    }

    /*
     * Overrides Object#hashCode() because Object#equals(Object) is overridden.
     */
    public int hashCode()
    {
        return (super.hashCode() | getFormatParameters().hashCode()); 
    }
}
