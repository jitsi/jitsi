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

import net.java.sip.communicator.impl.neomedia.*;
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
        MediaFormat mediaFormat = MediaUtils.getMediaFormat(format);

        if (mediaFormat == null)
        {
            if (format instanceof AudioFormat)
                mediaFormat = new AudioMediaFormatImpl((AudioFormat) format);
            else if (format instanceof VideoFormat)
                mediaFormat = new VideoMediaFormatImpl((VideoFormat) format);
        }
        return mediaFormat;
    }

    /**
     * Creates a new <tt>MediaFormat</tt> instance for a specific JMF
     * <tt>Format</tt> and assigns it specific clock rate and set of
     * format-specific parameters.
     *
     * @param format the JMF <tt>Format</tt> the new instance is to provide an
     * implementation of <tt>MediaFormat</tt> for
     * @param clockRate the clock rate of the new instance
     * @param formatParameters the set of format-specific parameters of the new
     * instance
     * @return a new <tt>MediaFormat</tt> instance for the specified JMF
     * <tt>Format</tt> and with the specified clock rate and set of
     * format-specific parameters
     */
    public static MediaFormatImpl<? extends Format> createInstance(
            Format format,
            double clockRate,
            Map<String, String> formatParameters)
    {
        if (format instanceof AudioFormat)
        {
            AudioFormat audioFormat = (AudioFormat) format;
            AudioFormat clockRateAudioFormat
                = new AudioFormat(
                        audioFormat.getEncoding(),
                        clockRate,
                        audioFormat.getSampleSizeInBits(),
                        audioFormat.getChannels());

            return
                new AudioMediaFormatImpl(
                        (AudioFormat)
                            clockRateAudioFormat.intersects(audioFormat),
                        formatParameters);
        }
        if (format instanceof VideoFormat)
            return
                new VideoMediaFormatImpl(
                        (VideoFormat) format,
                        clockRate,
                        formatParameters);
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
    static boolean formatParametersAreEqual(
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
    protected MediaFormatImpl(T format)
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
    protected MediaFormatImpl(T format, Map<String, String> formatParameters)
    {
        if (format == null)
            throw new NullPointerException("format");

        this.format = format;
        this.formatParameters
            = ((formatParameters == null) || formatParameters.isEmpty())
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(formatParameters);
    }

    /**
     * Implements MediaFormat#equals(Object) and actually compares the
     * encapsulated JMF <tt>Format</tt> instances.
     *
     * @param mediaFormat the object that we'd like to compare <tt>this</tt> one
     * to.
     *
     * @return <tt>true</tt> if the JMF <tt>Format</tt> instances encapsulated
     * by this class are equal and <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object mediaFormat)
    {
        if (this == mediaFormat)
            return true;

        if (!getClass().isInstance(mediaFormat))
            return false;

        @SuppressWarnings("unchecked")
        MediaFormatImpl<T> mediaFormatImpl = (MediaFormatImpl<T>) mediaFormat;

        return format.equals(mediaFormatImpl.format)
                && formatParametersAreEqual(
                        getFormatParameters(),
                        mediaFormatImpl.getFormatParameters());
    }

    /**
     * Implements MediaFormat#getEncoding() and returns the encoding of the JMF
     * <tt>Format</tt> that we are encapsulating here but it is the RFC-known
     * encoding and not the internal JMF encoding.
     *
     * @return the RFC-known encoding of the JMF <tt>Format</tt> that we are
     * encapsulating
     */
    public String getEncoding()
    {
        String jmfEncoding = getJMFEncoding();
        String encoding = MediaUtils.jmfEncodingToEncoding(jmfEncoding);

        if (encoding == null)
        {
            encoding = jmfEncoding;

            int encodingLength = encoding.length();

            if (encodingLength > 3)
            {
                int rtpPos = encodingLength - 4;

                if ("/rtp".equalsIgnoreCase(encoding.substring(rtpPos)))
                    encoding = encoding.substring(0, rtpPos);
            }
        }
        return encoding;
    }

    /**
     * Returns the JMF <tt>Format</tt> instance that we are wrapping here.
     *
     * @return a reference to that JMF <tt>Format</tt> instance that this class
     * is wrapping.
     */
    public T getFormat()
    {
        return format;
    }

    /**
     * Implements MediaFormat#getFormatParameters(). Returns a copy of the
     * format properties of this instance. Modifications to the returned Map do
     * no affect the format properties of this instance.
     *
     * @return a copy of the format properties of this instance. Modifications
     * to the returned Map do no affect the format properties of this instance.
     */
    public Map<String, String> getFormatParameters()
    {
        return (formatParameters == EMPTY_FORMAT_PARAMETERS)
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(formatParameters);
    }

    /**
     * Gets the encoding of the JMF <tt>Format</tt> represented by this
     * instance as it is known to JMF (in contrast to its RFC name).
     *
     * @return the encoding of the JMF <tt>Format</tt> represented by this
     * instance as it is known to JMF (in contrast to its RFC name)
     */
    public String getJMFEncoding()
    {
        return format.getEncoding();
    }

    /**
     * Gets the RTP payload type (number) of this <tt>MediaFormat</tt> as it is
     * known in RFC 3551 "RTP Profile for Audio and Video Conferences with
     * Minimal Control".
     *
     * @return the RTP payload type of this <tt>MediaFormat</tt> if it is known
     * in RFC 3551 "RTP Profile for Audio and Video Conferences with Minimal
     * Control"; otherwise, {@link #RTP_PAYLOAD_TYPE_UNKNOWN}
     * @see MediaFormat#getRTPPayloadType()
     */
    public byte getRTPPayloadType()
    {
        return MediaUtils.getRTPPayloadType(getJMFEncoding(), getClockRate());
    }

    /**
     * Overrides Object#hashCode() because Object#equals(Object) is overridden.
     *
     * @return a hash code value for this <tt>MediaFormat</tt>.
     */
    @Override
    public int hashCode()
    {
        return (super.hashCode() | getFormatParameters().hashCode());
    }

    /**
     * Returns a <tt>String</tt> representation of the clock rate associated
     * with this <tt>MediaFormat</tt> making sure that the value appears as
     * an integer (i.e. its long-casted value is equal to its original one)
     * unless it is actually a non integer.
     *
     * @return a <tt>String</tt> representation of the clock rate associated
     * with this <tt>MediaFormat</tt>.
     */
    public String getClockRateString()
    {
        double clockRate = getClockRate();
        long   clockRateL = (long)clockRate;
        if ( clockRateL == clockRate)
            return Long.toString(clockRateL);
        else
            return Double.toString(clockRate);
    }

    /**
     * Returns a <tt>String</tt> representation of this <tt>MediaFormat</tt>
     * containing, among other things, its encoding and clockrate values.
     *
     * @return a <tt>String</tt> representation of this <tt>MediaFormat</tt>.
     */
    @Override
    public String toString()
    {
        return getEncoding() + "/" + (getClockRateString())
            + " PayloadType="+getRTPPayloadType();
    }
}
