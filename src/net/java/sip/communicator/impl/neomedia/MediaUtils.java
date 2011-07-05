/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.sdp.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.ScreenDevice;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Implements static utility methods used by media classes.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class MediaUtils
{

    /**
     * The constant which stands for an empty array of <tt>MediaFormat</tt>s.
     * Explicitly defined in order to reduce unnecessary allocations.
     */
    public static final MediaFormat[] EMPTY_MEDIA_FORMATS = new MediaFormat[0];

    /**
     * The maximum sample rate for audio that we advertise.
     */
    public static final double MAX_AUDIO_SAMPLE_RATE = 32000;

    /**
     * The <tt>Map</tt> of JMF-specific encodings to well-known encodings as
     * defined in RFC 3551.
     */
    private static final Map<String, String> jmfEncodingToEncodings
        = new HashMap<String, String>();

    /**
     * The <tt>MediaFormat</tt>s which do not have RTP payload types assigned by
     * RFC 3551 and are thus referred to as having dynamic RTP payload types.
     */
    private static final List<MediaFormat> rtpPayloadTypelessMediaFormats
        = new ArrayList<MediaFormat>();

    /**
     * The <tt>Map</tt> of RTP payload types (expressed as <tt>String</tt>s) to
     * <tt>MediaFormat</tt>s.
     */
    private static final Map<String, MediaFormat[]>
        rtpPayloadTypeStrToMediaFormats
            = new HashMap<String, MediaFormat[]>();

    static
    {
        addMediaFormats(
            (byte) SdpConstants.PCMU,
            "PCMU",
            MediaType.AUDIO,
            AudioFormat.ULAW_RTP,
            8000);

        /*
         * Some codecs depend on JMF native libraries which are only available
         * on 32-bit Linux and 32-bit Windows.
         */
        if(OSUtils.IS_LINUX32 || OSUtils.IS_WINDOWS32)
        {
            Map<String, String> g723FormatParams
                = new HashMap<String, String>();
            g723FormatParams.put("annexa", "no");
            g723FormatParams.put("bitrate", "6.3");
            addMediaFormats(
                    (byte) SdpConstants.G723,
                    "G723",
                    MediaType.AUDIO,
                    AudioFormat.G723_RTP,
                    g723FormatParams,
                    null,
                    8000);

            addMediaFormats(
                    (byte) SdpConstants.GSM,
                    "GSM",
                    MediaType.AUDIO,
                    AudioFormat.GSM_RTP,
                    8000);
        }

        addMediaFormats(
            (byte) SdpConstants.DVI4_8000,
            "DVI4",
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            8000);
        addMediaFormats(
            (byte) SdpConstants.DVI4_16000,
            "DVI4",
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            16000);
        addMediaFormats(
            (byte) SdpConstants.PCMA,
            "PCMA",
            MediaType.AUDIO,
            Constants.ALAW_RTP,
            8000);
        addMediaFormats(
            MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
            "iLBC",
            MediaType.AUDIO,
            Constants.ILBC_RTP,
            8000);
        addMediaFormats(
            MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
            "speex",
            MediaType.AUDIO,
            Constants.SPEEX_RTP,
            8000,
            16000,
            32000);
        addMediaFormats(
            (byte) SdpConstants.G722,
            "G722",
            MediaType.AUDIO,
            Constants.G722_RTP,
            8000);
        addMediaFormats(
            (byte) SdpConstants.G728,
            "G728",
            MediaType.AUDIO,
            AudioFormat.G728_RTP,
            8000);
        if (EncodingConfiguration.G729)
            addMediaFormats(
                (byte) SdpConstants.G729,
                "G729",
                MediaType.AUDIO,
                AudioFormat.G729_RTP,
                8000);
        addMediaFormats(
            MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
            "telephone-event",
            MediaType.AUDIO,
            Constants.TELEPHONE_EVENT,
            8000);

        /* We don't really support these.
        addMediaFormats((byte) SdpConstants.JPEG,
                        "JPEG",
                        MediaType.VIDEO,
                        VideoFormat.JPEG_RTP);
        addMediaFormats(
            (byte) SdpConstants.H263,
            "H263",
            MediaType.VIDEO,
            VideoFormat.H263_RTP);
        addMediaFormats(
            (byte) SdpConstants.H261,
            "H261",
            MediaType.VIDEO,
            VideoFormat.H261_RTP);
         */

        /* H264 */
        Map<String, String> h264FormatParams
            = new HashMap<String, String>();
        String packetizationMode = "packetization-mode";
        Map<String, String> h264AdvancedAttributes
            = new HashMap<String, String>();

        /*
         * Disable PLI since we use the periodic intra-refresh feature of
         * FFmpeg/x264.
         */
        //h264AdvancedAttributes.put("rtcp-fb", "nack pli");

        ScreenDevice screen
            = NeomediaActivator.getMediaServiceImpl().getDefaultScreenDevice();
        java.awt.Dimension res = (screen == null) ? null : screen.getSize();

        h264AdvancedAttributes.put("imageattr", createImageAttr(null, res));

        // packetization-mode=1
        h264FormatParams.put(packetizationMode, "1");
        addMediaFormats(
            MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
            "H264",
            MediaType.VIDEO,
            Constants.H264_RTP,
            h264FormatParams,
            h264AdvancedAttributes);
        // packetization-mode=0
        /*
         * XXX At the time of this writing,
         * EncodingConfiguration#compareEncodingPreferences(MediaFormat,
         * MediaFormat) is incomplete and considers two MediaFormats to be equal
         * if they have an equal number of format parameters (given that the
         * encodings and clock rates are equal, of course). Either fix the
         * method in question or don't add a format parameter for packetization
         * mode 0 (which is equivalent to having packetization-mode explicitly
         * defined as 0 anyway, according to the respective RFC).
         */
        h264FormatParams.remove(packetizationMode);
        addMediaFormats(
            MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
            "H264",
            MediaType.VIDEO,
            Constants.H264_RTP,
            h264FormatParams,
            h264AdvancedAttributes);

        /* H263+ */
        Map<String, String> h263FormatParams
            = new HashMap<String, String>();
        Map<String, String> h263AdvancedAttributes
            = new LinkedHashMap<String, String>();

        // maximum resolution we can receive is the size of our screen device
        if(res != null)
            h263FormatParams.put("CUSTOM", res.width + "," + res.height + ",2");
        h263FormatParams.put("VGA", "2");
        h263FormatParams.put("CIF", "1");
        h263FormatParams.put("QCIF", "1");

        addMediaFormats(
                MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN,
                "H263-1998",
                MediaType.VIDEO,
                Constants.H263P_RTP,
                h263FormatParams,
                h263AdvancedAttributes);
    }

    /**
     * Adds a new mapping of a specific RTP payload type to a list of
     * <tt>MediaFormat</tt>s of a specific <tt>MediaType</tt>, with a specific
     * JMF encoding and, optionally, with specific clock rates.
     *
     * @param rtpPayloadType the RTP payload type to be associated with a list
     * of <tt>MediaFormat</tt>s
     * @param encoding the well-known encoding (name) corresponding to
     * <tt>rtpPayloadType</tt> (in contrast to the JMF-specific encoding
     * specified by <tt>jmfEncoding</tt>)
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to
     * be associated with <tt>rtpPayloadType</tt>
     * @param jmfEncoding the JMF encoding of the <tt>MediaFormat</tt>s to be
     * associated with <tt>rtpPayloadType</tt>
     * @param clockRates the optional list of clock rates of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayloadType</tt>
     */
    private static void addMediaFormats(
            byte rtpPayloadType,
            String encoding,
            MediaType mediaType,
            String jmfEncoding,
            double... clockRates)
    {
        addMediaFormats(
            rtpPayloadType,
            encoding,
            mediaType,
            jmfEncoding,
            null,
            null,
            clockRates);
    }

    /**
     * Adds a new mapping of a specific RTP payload type to a list of
     * <tt>MediaFormat</tt>s of a specific <tt>MediaType</tt>, with a specific
     * JMF encoding and, optionally, with specific clock rates.
     *
     * @param rtpPayloadType the RTP payload type to be associated with a list
     * of <tt>MediaFormat</tt>s
     * @param encoding the well-known encoding (name) corresponding to
     * <tt>rtpPayloadType</tt> (in contrast to the JMF-specific encoding
     * specified by <tt>jmfEncoding</tt>)
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to
     * be associated with <tt>rtpPayloadType</tt>
     * @param jmfEncoding the JMF encoding of the <tt>MediaFormat</tt>s to be
     * associated with <tt>rtpPayloadType</tt>
     * @param formatParameters the set of format-specific parameters of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayloadType</tt>
     * @param advancedAttributes the set of advanced attributes of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayload</tt>
     * @param clockRates the optional list of clock rates of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayloadType</tt>
     */
    @SuppressWarnings("unchecked")
    private static void addMediaFormats(
            byte rtpPayloadType,
            String encoding,
            MediaType mediaType,
            String jmfEncoding,
            Map<String, String> formatParameters,
            Map<String, String> advancedAttributes,
            double... clockRates)
    {
        int clockRateCount = clockRates.length;
        List<MediaFormat> mediaFormats
            = new ArrayList<MediaFormat>(clockRateCount);

        if (clockRateCount > 0)
        {
            for (double clockRate : clockRates)
            {
                Format format;

                switch (mediaType)
                {
                case AUDIO:
                    format = new AudioFormat(jmfEncoding);
                    break;
                case VIDEO:
                    format
                        = new ParameterizedVideoFormat(
                                jmfEncoding,
                                formatParameters);
                    break;
                default:
                    throw new IllegalArgumentException("mediaType");
                }

                MediaFormat mediaFormat
                    = MediaFormatImpl.createInstance(
                            format,
                            clockRate,
                            formatParameters,
                            advancedAttributes);

                if (mediaFormat != null)
                    mediaFormats.add(mediaFormat);
            }
        }
        else
        {
            Format format;
            double clockRate;

            switch (mediaType)
            {
            case AUDIO:
                AudioFormat audioFormat = new AudioFormat(jmfEncoding);

                format = audioFormat;
                clockRate = audioFormat.getSampleRate();
                break;
            case VIDEO:
                format
                    = new ParameterizedVideoFormat(
                            jmfEncoding,
                            formatParameters);
                clockRate = VideoMediaFormatImpl.DEFAULT_CLOCK_RATE;
                break;
            default:
                throw new IllegalArgumentException("mediaType");
            }

            MediaFormat mediaFormat
                = MediaFormatImpl.createInstance(
                        format,
                        clockRate,
                        formatParameters,
                        advancedAttributes);

            if (mediaFormat != null)
                mediaFormats.add(mediaFormat);
        }

        if (mediaFormats.size() > 0)
        {
            if (MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN == rtpPayloadType)
                rtpPayloadTypelessMediaFormats.addAll(mediaFormats);
            else
                rtpPayloadTypeStrToMediaFormats.put(
                        Byte.toString(rtpPayloadType),
                        mediaFormats.toArray(EMPTY_MEDIA_FORMATS));

            jmfEncodingToEncodings.put(
                    ((MediaFormatImpl<? extends Format>) mediaFormats.get(0))
                        .getJMFEncoding(),
                    encoding);
        }
    }

    /**
     * Gets a <tt>MediaFormat</tt> predefined in <tt>MediaUtils</tt> which
     * represents a specific JMF <tt>Format</tt>. If there is no such
     * representing <tt>MediaFormat</tt> in <tt>MediaUtils</tt>, returns
     * <tt>null</tt>.
     *
     * @param format the JMF <tt>Format</tt> to get the <tt>MediaFormat</tt>
     * representation for
     * @return a <tt>MediaFormat</tt> predefined in <tt>MediaUtils</tt> which
     * represents <tt>format</tt> if any; <tt>null</tt> if there is no such
     * representing <tt>MediaFormat</tt> in <tt>MediaUtils</tt>
     */
    @SuppressWarnings("unchecked")
    public static MediaFormat getMediaFormat(Format format)
    {
        double clockRate;

        if (format instanceof AudioFormat)
            clockRate = ((AudioFormat) format).getSampleRate();
        else if (format instanceof VideoFormat)
            clockRate = VideoMediaFormatImpl.DEFAULT_CLOCK_RATE;
        else
            clockRate = Format.NOT_SPECIFIED;

        byte rtpPayloadType = getRTPPayloadType(format.getEncoding(), clockRate);

        if (MediaFormatImpl.RTP_PAYLOAD_TYPE_UNKNOWN != rtpPayloadType)
        {
            for (MediaFormat mediaFormat : getMediaFormats(rtpPayloadType))
            {
                MediaFormatImpl<? extends Format> mediaFormatImpl
                    = (MediaFormatImpl<? extends Format>) mediaFormat;

                if (format.matches(mediaFormatImpl.getFormat()))
                    return mediaFormat;
            }
        }
        return null;
    }

    /**
     * Gets the <tt>MediaFormat</tt> known to <tt>MediaUtils</tt> and having the
     * specified well-known <tt>encoding</tt> (name) and <tt>clockRate</tt>.
     *
     * @param encoding the well-known encoding (name) of the
     * <tt>MediaFormat</tt> to get
     * @param clockRate the clock rate of the <tt>MediaFormat</tt> to get
     * @return the <tt>MediaFormat</tt> known to <tt>MediaUtils</tt> and having
     * the specified <tt>encoding</tt> and <tt>clockRate</tt>
     */
    public static MediaFormat getMediaFormat(String encoding, double clockRate)
    {
        return getMediaFormat(encoding, clockRate, null);
    }

    /**
     * Gets the <tt>MediaFormat</tt> known to <tt>MediaUtils</tt> and having the
     * specified well-known <tt>encoding</tt> (name), <tt>clockRate</tt> and
     * matching format parameters.
     *
     * @param encoding the well-known encoding (name) of the
     * <tt>MediaFormat</tt> to get
     * @param clockRate the clock rate of the <tt>MediaFormat</tt> to get
     * @param fmtps the format parameters of the <tt>MediaFormat</tt> to get
     * @return the <tt>MediaFormat</tt> known to <tt>MediaUtils</tt> and having
     * the specified <tt>encoding</tt> (name), <tt>clockRate</tt> and matching
     * format parameters
     */
    public static MediaFormat getMediaFormat(
            String encoding, double clockRate,
            Map<String, String> fmtps)
    {
        for (MediaFormat format : getMediaFormats(encoding))
            if ((format.getClockRate() == clockRate)
                    && format.formatParametersMatch(fmtps))
                return format;
        return null;
    }

    /**
     * Gets the index of a specific <tt>MediaFormat</tt> instance within the
     * internal storage of <tt>MediaUtils</tt>. Since the index is in the
     * internal storage which may or may not be one and the same for the various
     * <tt>MediaFormat</tt> instances and which may or may not be searched for
     * the purposes of determining the index, the index is not to be used as a
     * way to determine whether <tt>MediaUtils</tt> knows the specified
     * <tt>mediaFormat</tt>
     *
     * @param mediaFormat the <tt>MediaFormat</tt> to determine the index of
     * @return the index of the specified <tt>mediaFormat</tt> in the internal
     * storage of <tt>MediaUtils</tt>
     */
    public static int getMediaFormatIndex(MediaFormat mediaFormat)
    {
        return rtpPayloadTypelessMediaFormats.indexOf(mediaFormat);
    }

    /**
     * Gets the <tt>MediaFormat</tt>s predefined in <tt>MediaUtils</tt> with a
     * specific well-known encoding (name) as defined by RFC 3551 "RTP Profile
     * for Audio and Video Conferences with Minimal Control".
     *
     * @param encoding the well-known encoding (name) to get the corresponding
     * <tt>MediaFormat</tt>s of
     * @return a <tt>List</tt> of <tt>MediaFormat</tt>s corresponding to the
     * specified encoding (name)
     */
    @SuppressWarnings("unchecked")
    public static List<MediaFormat> getMediaFormats(String encoding)
    {
        String jmfEncoding = null;

        for (Map.Entry<String, String> jmfEncodingToEncoding
                : jmfEncodingToEncodings.entrySet())
            if (jmfEncodingToEncoding.getValue().equals(encoding))
            {
                jmfEncoding = jmfEncodingToEncoding.getKey();
                break;
            }

        List<MediaFormat> mediaFormats = new ArrayList<MediaFormat>();

        if (jmfEncoding != null)
        {
            for (MediaFormat[] rtpPayloadTypeMediaFormats
                    : rtpPayloadTypeStrToMediaFormats.values())
                for (MediaFormat rtpPayloadTypeMediaFormat
                            : rtpPayloadTypeMediaFormats)
                    if (((MediaFormatImpl<? extends Format>)
                                    rtpPayloadTypeMediaFormat)
                                .getJMFEncoding().equals(jmfEncoding))
                        mediaFormats.add(rtpPayloadTypeMediaFormat);


            if (mediaFormats.size() < 1)
            {
                for (MediaFormat rtpPayloadTypelessMediaFormat
                        : rtpPayloadTypelessMediaFormats)
                    if (((MediaFormatImpl<? extends Format>)
                                    rtpPayloadTypelessMediaFormat)
                                .getJMFEncoding().equals(jmfEncoding))
                        mediaFormats.add(rtpPayloadTypelessMediaFormat);
            }
        }
        return mediaFormats;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s known to <tt>MediaUtils</tt> and being of
     * the specified <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to
     * get
     * @return the <tt>MediaFormat</tt>s known to <tt>MediaUtils</tt> and being
     * of the specified <tt>mediaType</tt>
     */
    public static MediaFormat[] getMediaFormats(MediaType mediaType)
    {
        List<MediaFormat> mediaFormats = new ArrayList<MediaFormat>();

        for (MediaFormat[] formats : rtpPayloadTypeStrToMediaFormats.values())
            for (MediaFormat format : formats)
                if (format.getMediaType().equals(mediaType))
                    mediaFormats.add(format);
        for (MediaFormat format : rtpPayloadTypelessMediaFormats)
            if (format.getMediaType().equals(mediaType))
                mediaFormats.add(format);
        return mediaFormats.toArray(EMPTY_MEDIA_FORMATS);
    }

    /**
     * Gets the <tt>MediaFormat</tt>s (expressed as an array) corresponding to
     * a specific RTP payload type.
     *
     * @param rtpPayloadType the RTP payload type to retrieve the
     * corresponding <tt>MediaFormat</tt>s for
     * @return an array of <tt>MediaFormat</tt>s corresponding to the specified
     * RTP payload type
     */
    public static MediaFormat[] getMediaFormats(byte rtpPayloadType)
    {
        MediaFormat[] mediaFormats
            = rtpPayloadTypeStrToMediaFormats.get(Byte.toString(rtpPayloadType));

        return
            (mediaFormats == null)
                ? EMPTY_MEDIA_FORMATS
                : mediaFormats.clone();
    }

    /**
     * Gets the well-known encoding (name) as defined in RFC 3551 "RTP Profile
     * for Audio and Video Conferences with Minimal Control" corresponding to a
     * given JMF-specific encoding.
     *
     * @param jmfEncoding the JMF encoding to get the corresponding well-known
     * encoding of
     * @return the well-known encoding (name) as defined in RFC 3551 "RTP
     * Profile for Audio and Video Conferences with Minimal Control"
     * corresponding to <tt>jmfEncoding</tt> if any; otherwise, <tt>null</tt>
     */
    public static String jmfEncodingToEncoding(String jmfEncoding)
    {
        return jmfEncodingToEncodings.get(jmfEncoding);
    }

    /**
     * Gets the RTP payload type corresponding to a specific JMF encoding and
     * clock rate.
     *
     * @param jmfEncoding the JMF encoding as returned by
     * {@link Format#getEncoding()} or the respective <tt>AudioFormat</tt> and
     * <tt>VideoFormat</tt> encoding constants to get the corresponding RTP
     * payload type of
     * @param clockRate the clock rate to be taken into account in the search
     * for the RTP payload type if the JMF encoding does not uniquely identify
     * it
     * @return the RTP payload type corresponding to the specified JMF encoding
     * and clock rate if known in RFC 3551 "RTP Profile for Audio and Video
     * Conferences with Minimal Control"; otherwise,
     * {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN}
     */
    public static byte getRTPPayloadType(String jmfEncoding, double clockRate)
    {
        if (jmfEncoding == null)
            return MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN;
        else if (jmfEncoding.equals(AudioFormat.ULAW_RTP))
            return SdpConstants.PCMU;
        else if (jmfEncoding.equals(Constants.ALAW_RTP))
            return SdpConstants.PCMA;
        else if (jmfEncoding.equals(AudioFormat.GSM_RTP))
            return SdpConstants.GSM;
        else if (jmfEncoding.equals(AudioFormat.G723_RTP))
            return SdpConstants.G723;
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP)
                    && (clockRate == 8000))
            return SdpConstants.DVI4_8000;
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP)
                    && (clockRate == 16000))
            return SdpConstants.DVI4_16000;
        else if (jmfEncoding.equals(AudioFormat.ALAW))
            return SdpConstants.PCMA;
        else if (jmfEncoding.equals(Constants.G722))
            return SdpConstants.G722;
        else if (jmfEncoding.equals(Constants.G722_RTP))
            return SdpConstants.G722;
        else if (jmfEncoding.equals(AudioFormat.G728_RTP))
            return SdpConstants.G728;
        else if (jmfEncoding.equals(AudioFormat.G729_RTP))
            return SdpConstants.G729;
        else if (jmfEncoding.equals(VideoFormat.H263_RTP))
            return SdpConstants.H263;
        else if (jmfEncoding.equals(VideoFormat.JPEG_RTP))
            return SdpConstants.JPEG;
        else if (jmfEncoding.equals(VideoFormat.H261_RTP))
            return SdpConstants.H261;
        else
            return MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN;
    }


    /**
     * Creates value of an imgattr.
     *
     * http://tools.ietf.org/html/draft-ietf-mmusic-image-attributes-04
     *
     * @param sendSize maximum size peer can send
     * @param maxRecvSize maximum size peer can display
     * @return string that represent imgattr that can be encoded via SIP/SDP or
     * XMPP/Jingle
     */
    public static String createImageAttr(java.awt.Dimension sendSize,
            java.awt.Dimension maxRecvSize)
    {
        StringBuffer img = new StringBuffer();

        /* send width */
        if(sendSize != null)
        {
            /* single value => send [x=width,y=height] */
            /*img.append("send [x=");
            img.append((int)sendSize.getWidth());
            img.append(",y=");
            img.append((int)sendSize.getHeight());
            img.append("]");*/
            /* send [x=[min-max],y=[min-max]] */
            img.append("send [x=[0-");
            img.append((int)sendSize.getWidth());
            img.append("],y=[0-");
            img.append((int)sendSize.getHeight());
            img.append("]]");
            /*
            else
            {
                // range
                img.append(" send [x=[");
                img.append((int)minSendSize.getWidth());
                img.append("-");
                img.append((int)maxSendSize.getWidth());
                img.append("],y=[");
                img.append((int)minSendSize.getHeight());
                img.append("-");
                img.append((int)maxSendSize.getHeight());
                img.append("]]");
            }
            */
        }
        else
        {
            /* can send "all" sizes */
            img.append("send *");
        }

        /* receive size */
        if(maxRecvSize != null)
        {
            /* basically we can receive any size up to our
             * screen display size
             */

            /* recv [x=[min-max],y=[min-max]] */
            img.append(" recv [x=[0-");
            img.append((int)maxRecvSize.getWidth());
            img.append("],y=[0-");
            img.append((int)maxRecvSize.getHeight());
            img.append("]]");
        }
        else
        {
            /* accept all sizes */
            img.append(" recv *");
        }

        return img.toString();
    }
}
