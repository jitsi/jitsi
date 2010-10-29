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
     * <tt>MediaFormat</tt> which is either an <tt>AudioMediaFormat</tt> or a
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
     * returns a <tt>MediaFormat</tt> which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance.
     * Otherwise, returns <tt>null</tt>.
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
     * returns a <tt>MediaFormat</tt> which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance.
     * Otherwise, returns <tt>null</tt>.
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
        return createMediaFormat(encoding, clockRate, 1);
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt> and a default set of format
     * parameters. If <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt> which is
     * either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of availabe channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt> and a default set of format
     * parameters which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(String, double, int)
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            int channels)
    {
        for (MediaFormat format : getSupportedMediaFormats(encoding, clockRate))
            if ((CHANNELS_NOT_SPECIFIED != channels)
                    && (format instanceof AudioMediaFormat))
            {
                AudioMediaFormat audioFormat = (AudioMediaFormat) format;
                int audioFormatChannels = audioFormat.getChannels();

                if ((CHANNELS_NOT_SPECIFIED == audioFormatChannels)
                        || (audioFormatChannels == channels))
                    return audioFormat;
            } else
                return format;
        return null;
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt>
     * which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
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
     * @see MediaFormatFactory#createMediaFormat(String, double, Map, Map)
     */
    public MediaFormat createMediaFormat( String              encoding,
                                          double              clockRate,
                                          Map<String, String> formatParams,
                                          Map<String, String> advancedParams)
    {
        return createMediaFormat( encoding, clockRate, 1, formatParams,
                        advancedParams);
    }

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters. If
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>, returns a
     * <tt>MediaFormat</tt> which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of availabe channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @param advancedParams any parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     * @see MediaFormatFactory#createMediaFormat(String, double, int, Map, Map)
     */
    public MediaFormat createMediaFormat(String              encoding,
                                         double              clockRate,
                                         int                 channels,
                                         Map<String, String> formatParams,
                                         Map<String, String> advancedParams)
    {
        MediaFormat mediaFormat
            = createMediaFormat(encoding, clockRate, channels);

        if ((mediaFormat != null)
                && (formatParams != null)
                && !formatParams.isEmpty())
        {
            Map<String, String> formatParameters
                = new HashMap<String, String>();
            Map<String, String> advancedParameters
                = new HashMap<String, String>();

            //formatParameters.putAll(mediaFormat.getFormatParameters());
            formatParameters.putAll(formatParams);

            if(advancedParams != null)
            {
                advancedParameters.putAll(advancedParams);
            }

            switch (mediaFormat.getMediaType())
            {
            case AUDIO:
                mediaFormat
                    = new AudioMediaFormatImpl(
                            ((AudioMediaFormatImpl) mediaFormat).getFormat(),
                            formatParameters, advancedParameters);
                break;
            case VIDEO:
                VideoMediaFormatImpl videoMediaFormatImpl
                    = (VideoMediaFormatImpl) mediaFormat;

                mediaFormat
                    = new VideoMediaFormatImpl(
                            videoMediaFormatImpl.getFormat(),
                            videoMediaFormatImpl.getClockRate(),
                            formatParameters,
                            advancedParameters);
                break;
            default:
                mediaFormat = null;
            }
        }
        return mediaFormat;
    }

    /**
     * Creates a <tt>MediaFormat</tt> either for the specified
     * <tt>rtpPayloadType</tt> or for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters. If
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>, ignores
     * <tt>rtpPayloadType</tt> and returns a <tt>MediaFormat</tt> which is
     * either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. If <tt>rtpPayloadType</tt> is not
     * {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN} and <tt>encoding</tt> is
     * <tt>null</tt>, uses the encoding associated with <tt>rtpPayloadType</tt>.
     *
     * @param rtpPayloadType the RTP payload type to create a
     * <tt>MediaFormat</tt> for; {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN} if
     * <tt>encoding</tt> is not <tt>null</tt>. If <tt>rtpPayloadType</tt> is not
     * <tt>MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN</tt> and <tt>encoding</tt> is
     * not <tt>null</tt>, <tt>rtpPayloadType</tt> is ignored
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for; <tt>null</tt>
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of availabe channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @param advancedParams any parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(
            byte rtpPayloadType,
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams,
            Map<String, String> advancedParams)
    {

        /*
         * If rtpPayloadType is specified, use it only to figure out encoding
         * and/or clockRate in case either one of them is unknown.
         */
        if ((MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN != rtpPayloadType)
                && ((encoding == null)
                        || (CLOCK_RATE_NOT_SPECIFIED == clockRate)))
        {
            MediaFormat[] rtpPayloadTypeMediaFormats
                = MediaUtils.getMediaFormats(rtpPayloadType);

            if (rtpPayloadTypeMediaFormats.length > 0)
            {
                if (encoding == null)
                    encoding = rtpPayloadTypeMediaFormats[0].getEncoding();

                // Assign or check the clock rate.
                if (CLOCK_RATE_NOT_SPECIFIED == clockRate)
                    clockRate = rtpPayloadTypeMediaFormats[0].getClockRate();
                else
                {
                    boolean clockRateIsValid = false;

                    for (MediaFormat rtpPayloadTypeMediaFormat
                            : rtpPayloadTypeMediaFormats)
                        if (rtpPayloadTypeMediaFormat
                                    .getEncoding().equals(encoding)
                                && (rtpPayloadTypeMediaFormat.getClockRate()
                                            == clockRate))
                        {
                            clockRateIsValid = true;
                            break;
                        }

                    if (!clockRateIsValid)
                        return null;
                }
            }
        }

        return createMediaFormat(encoding, clockRate, channels, formatParams,
                advancedParams);
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

        // uses equalsIgnoreCase, as some clients transmit some of the codecs
        // starting with capital letters
        for (MediaFormat mediaFormat : mediaFormats)
            if (mediaFormat.getEncoding().equalsIgnoreCase(encoding)
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
