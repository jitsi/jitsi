/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.service.neomedia.MediaType;
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
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt> with
     * default clock rate and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a
     * <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt> which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(String)
     */
    public MediaFormat createMediaFormat(String encoding)
    {
        return createMediaFormat(encoding, CLOCK_RATE_NOT_SPECIFIED);
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified RTP payload type with
     * default clock rate and set of format parameters. If
     * <tt>rtpPayloadType</tt> is known to this <tt>MediaFormatFactory</tt>,
     * returns a <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt>
     * or a <tt>VideoMediaFormat</tt> instance. Otherwise, returns
     * <tt>null</tt>.
     *
     * @param rtpPayloadType the RTP payload type of the <tt>MediaFormat</tt> to
     * create
     * @return a <tt>MediaFormat</tt> with the specified <tt>rtpPayloadType</tt>
     * which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>rtpPayloadType</tt> is known to
     * this <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(byte)
     */
    public MediaFormat createMediaFormat(byte rtpPayloadType)
    {

        /*
         * We know which are the MediaFormat instance with the specified
         * rtpPayloadType but we cannot directly return them because they do not
         * reflect the user configuration with respect to being enabled and
         * disabled.
         */
        for (MediaFormat rtpPayloadTypeMediaFormat
                : MediaUtils.getMediaFormats(rtpPayloadType))
        {
            MediaFormat mediaFormat
                = createMediaFormat(
                    rtpPayloadTypeMediaFormat.getEncoding(),
                    rtpPayloadTypeMediaFormat.getClockRate());
            if (mediaFormat != null)
                return mediaFormat;
        }
        return null;
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt> with
     * the specified <tt>clockRate</tt> and a default set of format parameters.
     * If <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>,
     * returns a <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt>
     * or a <tt>VideoMediaFormat</tt> instance. Otherwise, returns
     * <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt> and
     * <tt>clockRate</tt> which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(String, double)
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate)
    {
        List<MediaFormat> supportedMediaFormats
            = getSupportedMediaFormats(encoding, clockRate);

        return
            supportedMediaFormats.isEmpty()
                ? null
                : supportedMediaFormats.get(0);
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
        for (MediaFormat format : getSupportedMediaFormats(encoding, clockRate))
            if (format instanceof AudioMediaFormat)
            {
                AudioMediaFormat audioFormat = (AudioMediaFormat) format;

                if (audioFormat.getChannels() == channels)
                    return audioFormat;
            }
        return null;
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt>
     * which either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance if
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>;
     * otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(String, double, Map)
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams)
    {
        MediaFormat mediaFormat = createMediaFormat(encoding, clockRate);

        if ((mediaFormat != null)
                && (formatParams != null)
                && !formatParams.isEmpty())
        {
            Map<String, String> formatParameters
                = new HashMap<String, String>();

            formatParameters.putAll(mediaFormat.getFormatParameters());
            formatParameters.putAll(formatParams);

            switch (mediaFormat.getMediaType())
            {
            case AUDIO:
                mediaFormat
                    = new AudioMediaFormatImpl(
                            ((AudioMediaFormatImpl) mediaFormat).getFormat(),
                            formatParameters);
                break;
            case VIDEO:
                VideoMediaFormatImpl videoMediaFormatImpl
                    = (VideoMediaFormatImpl) mediaFormat;

                mediaFormat
                    = new VideoMediaFormatImpl(
                            videoMediaFormatImpl.getFormat(),
                            videoMediaFormatImpl.getClockRate(),
                            formatParameters);
                break;
            default:
                mediaFormat = null;
            }
        }
        return mediaFormat;
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
        AudioMediaFormat audioMediaFormat
            = createAudioMediaFormat(encoding, clockRate, channels);

        if ((audioMediaFormat != null)
                && (formatParams != null)
                && !formatParams.isEmpty())
        {
            Map<String, String> formatParameters
                = new HashMap<String, String>();

            formatParameters.putAll(audioMediaFormat.getFormatParameters());
            formatParameters.putAll(formatParams);

            audioMediaFormat
                = new AudioMediaFormatImpl(
                        ((AudioMediaFormatImpl) audioMediaFormat).getFormat(),
                        formatParameters);
        }
        return null;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s among the specified <tt>mediaFormats</tt>
     * which have the specified <tt>encoding</tt> and, optionally,
     * <tt>clockRate</tt>.
     *
     * @param mediaFormats the <tt>MediaFormat</tt>s from which to filter out
     * only the ones which have the specified <tt>encoding</tt> and, optionally,
     * <tt>clockRate</tt>
     * @param encoding the well-known encoding (name) of the
     * <tt>MediaFormat</tt>s to be retrieved
     * @param clockRate the clock rate of the <tt>MediaFormat</tt>s to be
     * retrieved; {@link #CLOCK_RATE_NOT_SPECIFIED} if any clock rate is
     * acceptable
     * @return a <tt>List</tt> of the <tt>MediaFormat</tt>s among
     * <tt>mediaFormats</tt> which have the specified <tt>encoding</tt> and,
     * optionally, <tt>clockRate</tt>
     */
    private List<MediaFormat> getMatchingMediaFormats(
            MediaFormat[] mediaFormats,
            String encoding,
            double clockRate)
    {
        List<MediaFormat> supportedMediaFormats = new ArrayList<MediaFormat>();

        for (MediaFormat mediaFormat : mediaFormats)
            if (mediaFormat.getEncoding().equals(encoding)
                    && ((CLOCK_RATE_NOT_SPECIFIED == clockRate)
                            || (mediaFormat.getClockRate() == clockRate)))
                supportedMediaFormats.add(mediaFormat);
        return supportedMediaFormats;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s supported by this
     * <tt>MediaFormatFactory</tt> and the <tt>MediaService</tt> associated with
     * it and having the specified <tt>encoding</tt> and, optionally,
     * <tt>clockRate</tt>.
     *
     * @param encoding the well-known encoding (name) of the
     * <tt>MediaFormat</tt>s to be retrieved
     * @param clockRate the clock rate of the <tt>MediaFormat</tt>s to be
     * retrieved; {@link #CLOCK_RATE_NOT_SPECIFIED} if any clock rate is
     * acceptable
     * @return a <tt>List</tt> of the <tt>MediaFormat</tt>s supported by the
     * <tt>MediaService</tt> associated with this <tt>MediaFormatFactory</tt>
     * and having the specified encoding and, optionally, clock rate
     */
    private List<MediaFormat> getSupportedMediaFormats(
            String encoding,
            double clockRate)
    {
        EncodingConfiguration encodingConfiguration
            = NeomediaActivator
                .getMediaServiceImpl().getEncodingConfiguration();
        List<MediaFormat> supportedMediaFormats
            = getMatchingMediaFormats(
                encodingConfiguration
                    .getSupportedEncodings(MediaType.AUDIO),
                encoding,
                clockRate);

        if (supportedMediaFormats.isEmpty())
            supportedMediaFormats
                = getMatchingMediaFormats(
                    encodingConfiguration
                        .getSupportedEncodings(MediaType.VIDEO),
                    encoding,
                    clockRate);
        return supportedMediaFormats;
    }
}
