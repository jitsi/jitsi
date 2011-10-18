/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaFormat</tt> for the JMF <tt>Format</tt>.
 *
 * @param <T> the type of the wrapped <tt>Format</tt>
 *
 * @author Lyubomir Marinov
 */
public abstract class MediaFormatImpl<T extends Format>
    implements MediaFormat
{
    /**
     * The name of the <tt>clockRate</tt> property of <tt>MediaFormatImpl</tt>.
     */
    public static final String CLOCK_RATE_PNAME = "clockRate";

    /**
     * The value of the <tt>formatParameters</tt> property of
     * <tt>MediaFormatImpl</tt> when no codec-specific parameters have been
     * received via SIP/SDP or XMPP/Jingle. Explicitly defined in order to
     * reduce unnecessary allocations.
     */
    static final Map<String, String> EMPTY_FORMAT_PARAMETERS
        = Collections.emptyMap();

    /**
     * The value of the <tt>codecSettings</tt> property of
     * <tt>MediaFormatImpl</tt> when no codec-specific settings.
     * Explicitly defined in order to reduce unnecessary allocations.
     */
    static final Map<String, String> EMPTY_CODEC_SETTINGS
        = Collections.emptyMap();

    /**
     * The name of the <tt>encoding</tt> property of <tt>MediaFormatImpl</tt>.
     */
    public static final String ENCODING_PNAME = "encoding";

    /**
     * The name of the <tt>formatParameters</tt> property of
     * <tt>MediaFormatImpl</tt>.
     */
    public static final String FORMAT_PARAMETERS_PNAME = "fmtps";

    /**
     * The additional codec settings.
     */
    private Map<String, String> codecSettings = EMPTY_CODEC_SETTINGS;

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
     * @param advancedAttrs advanced attributes of the new instance
     * @return a new <tt>MediaFormat</tt> instance for the specified JMF
     * <tt>Format</tt> and with the specified clock rate and set of
     * format-specific parameters
     */
    public static MediaFormatImpl<? extends Format> createInstance(
            Format format,
            double clockRate,
            Map<String, String> formatParameters,
            Map<String, String> advancedAttrs)
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
                        formatParameters,
                        advancedAttrs);
        }
        else if (format instanceof VideoFormat)
            return
                new VideoMediaFormatImpl(
                        (VideoFormat) format,
                        clockRate,
                        -1,
                        formatParameters,
                        advancedAttrs);
        else
            return null;
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
     * @param fmtps1 the first set of format parameters to be tested for
     * equality
     * @param fmtps2 the second set of format parameters to be tested for
     * equality
     * @return <tt>true</tt> if the specified sets of format parameters are
     * equal; <tt>false</tt>, otherwise
     */
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
        if (fmtps1 == null)
            return (fmtps2 == null) || fmtps2.isEmpty();
        if (fmtps2 == null)
            return (fmtps1 == null) || fmtps1.isEmpty();
        if (fmtps1.size() == fmtps2.size())
        {
            for (Map.Entry<String, String> fmtp1 : fmtps1.entrySet())
            {
                String key1 = fmtp1.getKey();

                if (!fmtps2.containsKey(key1))
                    return false;

                String value1 = fmtp1.getValue();
                String value2 = fmtps2.get(key1);

                /*
                 * Since the values are strings, allow null to be equal to the
                 * empty string.
                 */
                if ((value1 == null) || (value1.length() == 0))
                {
                    if ((value2 != null) && (value2.length() != 0))
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
     * {@inheritDoc}
     * <p>
     * The default implementation of <tt>MediaFormatImpl</tt> always returns
     * <tt>true</tt> because format parameters in general do not cause the
     * distinction of payload types.
     * </p>
     */
    public boolean formatParametersMatch(Map<String, String> fmtps)
    {
        return true;
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
     * The advanced parameters of this instance which have been received
     * via SIP/SDP or XMPP/Jingle.
     */
    private final Map<String, String> advancedParameters;

    /**
     * Initializes a new <tt>MediaFormatImpl</tt> instance which is to provide
     * an implementation of <tt>MediaFormat</tt> for a specific <tt>Format</tt>.
     *
     * @param format the JMF <tt>Format</tt> the new instance is to provide an
     * implementation of <tt>MediaFormat</tt> for
     */
    protected MediaFormatImpl(T format)
    {
        this(format, null, null);
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
     * @param advancedParameters any parameters that have been
     * received via SIP/SDP or XMPP/Jingle
     */
    protected MediaFormatImpl(
            T format,
            Map<String, String> formatParameters,
            Map<String, String> advancedParameters)
    {
        if (format == null)
            throw new NullPointerException("format");

        this.format = format;
        this.formatParameters
            = ((formatParameters == null) || formatParameters.isEmpty())
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(formatParameters);
        this.advancedParameters
            = ((advancedParameters == null) || advancedParameters.isEmpty())
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(advancedParameters);
    }

    /**
     * Implements MediaFormat#equals(Object) and actually compares the
     * encapsulated JMF <tt>Format</tt> instances.
     *
     * @param mediaFormat the object that we'd like to compare <tt>this</tt> one
     * to.
     8*
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

        return getFormat().equals(mediaFormatImpl.getFormat())
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
     * Implements MediaFormat#getAdvancedParameters(). Returns a copy of the
     * attribute properties of this instance. Modifications to the returned Map
     * do no affect the format properties of this instance.
     *
     * @return a copy of the attribute properties of this instance.
     * Modifications to the returned Map do no affect the format properties of
     * this instance.
     */
    public Map<String, String> getAdvancedAttributes()
    {
        return (advancedParameters == EMPTY_FORMAT_PARAMETERS)
                ? EMPTY_FORMAT_PARAMETERS
                : new HashMap<String, String>(advancedParameters);
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
        /*
         * XXX We've experienced a case of JMF's VideoFormat#hashCode()
         * returning different values for instances which are reported equal by
         * VideoFormat#equals(Object) which is inconsistent with the protocol
         * covering the two methods in question and causes problems,
         * for example, with Map. While jmfEncoding is more generic than format,
         * it still provides a relatively good distribution given that we do not
         * have a lot of instances with one and the same jmfEncoding.
         */
        return getJMFEncoding().hashCode() | getFormatParameters().hashCode();
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
        long clockRateL = (long) clockRate;

        if (clockRateL == clockRate)
            return Long.toString(clockRateL);
        else
            return Double.toString(clockRate);
    }

    /**
     * Sets additional codec settings.
     *
     * @param settings additional settings represented by a map.
     */
    public void setAdditionalCodecSettings(Map<String, String> settings)
    {
        codecSettings = settings;
    }

    /**
     * Returns additional codec settings.
     *
     * @return additional settings represented by a map.
     */
    public Map<String, String> getAdditionalCodecSettings()
    {
        return codecSettings;
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
        StringBuffer str = new StringBuffer();

        str.append("rtpmap:");
        str.append(getRTPPayloadType());
        str.append(' ');
        str.append(getEncoding());
        str.append('/');
        str.append(getClockRateString());

        /*
         * If the number of channels is 1, it does not have to be mentioned
         * because it is the default.
         */
        if (MediaType.AUDIO.equals(getMediaType()))
        {
            int channels = ((AudioFormat) getFormat()).getChannels();

            if (channels != 1)
            {
                str.append('/');
                str.append(channels);
            }
        }

        Map<String, String> formatParameters = getFormatParameters();

        if (!formatParameters.isEmpty())
        {
            str.append(" fmtp:");

            boolean prependSeparator = false;

            for (Map.Entry<String, String> formatParameter
                    : formatParameters.entrySet())
            {
                if (prependSeparator)
                    str.append(';');
                else
                    prependSeparator = true;
                str.append(formatParameter.getKey());
                str.append('=');
                str.append(formatParameter.getValue());
            }
        }

        return str.toString();
    }
}
