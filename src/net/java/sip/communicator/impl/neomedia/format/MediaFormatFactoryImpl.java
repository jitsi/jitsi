/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaFormatFactory</tt> for the JMF <tt>Format</tt> types.
 *
 * @author Lubomir Marinov
 */
public class MediaFormatFactoryImpl
    implements MediaFormatFactory
{

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with a specific encoding
     * (name).
     *
     * @param encoding the encoding (name) of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name)
     * @see MediaFormatFactory#createAudioMediaFormat(String)
     */
    public AudioMediaFormat createAudioMediaFormat(String encoding)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if (format instanceof AudioMediaFormat)
                return (AudioMediaFormat) format;

        return new AudioMediaFormatImpl(encodingToJmfEncoding(encoding));
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name) and clock rate.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name) and clock rate
     * @see MediaFormatFactory#createAudioMediaFormat(String, double)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if ((format instanceof AudioMediaFormat)
                    && (format.getClockRate() == clockRate))
                return (AudioMediaFormat) format;

        return
            new AudioMediaFormatImpl(
                    encodingToJmfEncoding(encoding),
                    clockRate);
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate and number of channels.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param channels the number of channels of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate and number of channels
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, int)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if ((format instanceof AudioMediaFormat)
                    && (format.getClockRate() == clockRate))
            {
                AudioMediaFormat audioFormat = (AudioMediaFormat) format;

                if (audioFormat.getChannels() == channels)
                    return audioFormat;
            }

        return
            new AudioMediaFormatImpl(
                    encodingToJmfEncoding(encoding),
                    clockRate,
                    channels);
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate and set of format-specific parameters.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param formatParams the set of format-specific parameters of the new
     * instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate and set of format-specific parameters
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, Map)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if ((format instanceof AudioMediaFormat)
                    && (format.getClockRate() == clockRate)
                    && MediaFormatImpl
                            .formatParametersAreEqual(
                                format.getFormatParameters(),
                                formatParams))
                return (AudioMediaFormat) format;

        return
            new AudioMediaFormatImpl(
                    encodingToJmfEncoding(encoding),
                    clockRate,
                    formatParams);
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate, number of channels and set of format-specific
     * parameters.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param channels the number of channels of the new instance
     * @param formatParams the set of format-specific parameters of the new
     * instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate, number of channels and set of
     * format-specific parameters
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, Map)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if ((format instanceof AudioMediaFormat)
                    && (format.getClockRate() == clockRate))
            {
                AudioMediaFormat audioFormat = (AudioMediaFormat) format;

                if ((audioFormat.getChannels() == channels)
                        && MediaFormatImpl
                                .formatParametersAreEqual(
                                    format.getFormatParameters(),
                                    formatParams))
                    return audioFormat;
            }

        return
            new AudioMediaFormatImpl(
                    encodingToJmfEncoding(encoding),
                    clockRate,
                    channels,
                    formatParams);
    }

    /**
     * Creates a new <tt>VideoMediaFormat</tt> instance with a specific encoding
     * (name).
     *
     * @param encoding the encoding (name) of the new instance
     * @return a new <tt>VideoMediaFormat</tt> instance with the specified
     * encoding (name)
     * @see MediaFormatFactory#createVideoMediaFormat(String)
     */
    public VideoMediaFormat createVideoMediaFormat(String encoding)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if (format instanceof VideoMediaFormat)
                return (VideoMediaFormat) format;

        return new VideoMediaFormatImpl(encodingToJmfEncoding(encoding));
    }

    /**
     * Creates a new <tt>VideoMediaFormat</tt> instance with specific encoding
     * (name) and clock rate.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @return a new <tt>VideoMediaFormat</tt> instance with the specified
     * encoding (name) and clock rate
     * @see MediaFormatFactory#createVideoMediaFormat(String, double)
     */
    public VideoMediaFormat createVideoMediaFormat(
            String encoding,
            double clockRate)
    {
        for (MediaFormat format : MediaUtils.encodingToMediaFormats(encoding))
            if ((format instanceof VideoMediaFormat)
                    && (format.getClockRate() == clockRate))
                return (VideoMediaFormat) format;

        return
            new VideoMediaFormatImpl(
                    encodingToJmfEncoding(encoding),
                    clockRate);
    }

    /**
     * Gets the JMF-specific encoding corresponding to the specified well-known
     * encoding (name) as defined in RFC 3551 "RTP Profile for Audio and Video
     * Conferences with Minimal Control".
     * <p>
     * <b>Note</b>: This method is to be called only as a last resort because it
     * just appends "/rtp" to the specified <tt>encoding</tt> if it is not
     * appended yet.
     * </p>
     *
     * @param encoding the well-known encoding (name) as defined in RFC 3551
     * "RTP Profile for Audio and Video Conferences with Minimal Control" to get
     * the corresponding JMF-specific encoding of
     * @return the JMF-specific encoding corresponding to the specified
     * well-known encoding (name) as defined in RFC 3551 "RTP Profile for Audio
     * and Video Conferences with Minimal Control"
     */
    private static String encodingToJmfEncoding(String encoding)
    {
        if (encoding != null)
        {
            int encodingLength = encoding.length();

            if ((encodingLength < 4)
                    || !"/rtp"
                            .equalsIgnoreCase(
                                encoding.substring(encodingLength - 4)))
                encoding += "/rtp";
        }
        return encoding;
    }
}
