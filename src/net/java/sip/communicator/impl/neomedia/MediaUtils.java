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
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements static utility methods used by media classes.
 * 
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class MediaUtils
{

    /**
     * The constant which stands for an empty array of <tt>MediaFormat</tt>s.
     * Explicitly defined in order to reduce unnecessary allocations.
     */
    private static final MediaFormat[] EMPTY_MEDIA_FORMATS = new MediaFormat[0];

    /**
     * The <tt>Map</tt> of RTP payload types (expressed as <tt>String</tt>s) to
     * their well-known encoding names (in contrast to the JMF-specific
     * encodings).
     */
    private static final Map<String, String> rtpPayloadTypeStrToEncodings
        = new HashMap<String, String>();

    /**
     * The <tt>Map</tt> of RTP payload types (expressed as <tt>String</tt>s) to
     * <tt>MediaFormat</tt>s.
     */
    private static final Map<String, MediaFormat[]>
        rtpPayloadTypeStrToMediaFormats
            = new HashMap<String, MediaFormat[]>();

    static
    {
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.PCMU,
            "PCMU",
            MediaType.AUDIO,
            AudioFormat.ULAW_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.GSM,
            "GSM",
            MediaType.AUDIO,
            AudioFormat.GSM_RTP,
            8000);

        Map<String, String> g723FormatProperties
            = new HashMap<String, String>();
        g723FormatProperties.put("annexa", "no");
        g723FormatProperties.put("bitrate", "6.3");
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G723,
            "G723",
            MediaType.AUDIO,
            AudioFormat.G723_RTP,
            g723FormatProperties,
            8000);

        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.DVI4_8000,
            "DVI4",
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.DVI4_16000,
            "DVI4",
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            16000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.PCMA,
            "PCMA",
            MediaType.AUDIO,
            Constants.ALAW_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            97,
            null,
            MediaType.AUDIO,
            Constants.ILBC_RTP);
        mapRtpPayloadTypeToMediaFormats(
            98,
            null,
            MediaType.AUDIO,
            Constants.ILBC_RTP);
        mapRtpPayloadTypeToMediaFormats(
            110,
            null,
            MediaType.AUDIO,
            Constants.SPEEX_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G728,
            "G728",
            MediaType.AUDIO,
            AudioFormat.G728_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G729,
            "G729",
            MediaType.AUDIO,
            AudioFormat.G729_RTP,
            8000);

        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.H263,
            "H263",
            MediaType.VIDEO,
            VideoFormat.H263_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.JPEG,
            "JPEG",
            MediaType.VIDEO,
            VideoFormat.JPEG_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.H261,
            "H261",
            MediaType.VIDEO,
            VideoFormat.H261_RTP);

        Map<String, String> h264FormatProperties
            = new HashMap<String, String>();
        h264FormatProperties.put("packetization-mode", "1");
        mapRtpPayloadTypeToMediaFormats(
            Constants.H264_RTP_SDP,
            null,
            MediaType.VIDEO,
            Constants.H264_RTP,
            h264FormatProperties);
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
    public static List<MediaFormat> encodingToMediaFormats(String encoding)
    {
        List<MediaFormat> mediaFormats = new ArrayList<MediaFormat>();

        for (Map.Entry<String, String> rtpPayloadTypeStrToEncoding
                : rtpPayloadTypeStrToEncodings.entrySet())
            if (rtpPayloadTypeStrToEncoding.getValue().equals(encoding))
                for (MediaFormat mediaFormat
                        : rtpPayloadTypeToMediaFormats(
                            rtpPayloadTypeStrToEncoding.getKey()))
                    mediaFormats.add(mediaFormat);
        return mediaFormats;
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
        int rtpPayloadType = jmfEncodingToRtpPayloadType(jmfEncoding);
        String encoding = null;

        if (MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN != rtpPayloadType)
            encoding
                = rtpPayloadTypeStrToEncodings
                    .get(Integer.toString(rtpPayloadType));
        return encoding;
    }

    /**
     * Gets the RTP payload type corresponding to a specific JMF encoding.
     *
     * @param jmfEncoding the JMF encoding as returned by
     * {@link Format#getEncoding()} or the respective <tt>AudioFormat</tt> and
     * <tt>VideoFormat</tt> encoding constants to get the corresponding RTP
     * payload type of
     * @return the RTP payload type corresponding to the specified JMF encoding
     * if known in RFC 3551 "RTP Profile for Audio and Video Conferences with
     * Minimal Control"; otherwise, {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN}
     */
    public static int jmfEncodingToRtpPayloadType(String jmfEncoding)
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
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
            return SdpConstants.DVI4_8000;
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
            return SdpConstants.DVI4_16000;
        else if (jmfEncoding.equals(AudioFormat.ALAW))
            return SdpConstants.PCMA;
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
        else if (jmfEncoding.equals(Constants.H264_RTP))
            return Constants.H264_RTP_SDP;
        else if (jmfEncoding.equals(Constants.ILBC))
            return 97;
        else if (jmfEncoding.equals(Constants.ILBC_RTP))
            return 97;
        else if (jmfEncoding.equals(Constants.SPEEX))
            return 110;
        else if (jmfEncoding.equals(Constants.SPEEX_RTP))
            return 110;
        else
            return MediaFormat.RTP_PAYLOAD_TYPE_UNKNOWN;
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
    private static void mapRtpPayloadTypeToMediaFormats(
            int rtpPayloadType,
            String encoding,
            MediaType mediaType,
            String jmfEncoding,
            double... clockRates)
    {
        mapRtpPayloadTypeToMediaFormats(
            rtpPayloadType,
            encoding,
            mediaType,
            jmfEncoding,
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
     * @param clockRates the optional list of clock rates of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayloadType</tt>
     */
    private static void mapRtpPayloadTypeToMediaFormats(
            int rtpPayloadType,
            String encoding,
            MediaType mediaType,
            String jmfEncoding,
            Map<String, String> formatParameters,
            double... clockRates)
    {
        int clockRateCount = clockRates.length;
        List<MediaFormat> mediaFormats
            = new ArrayList<MediaFormat>(clockRateCount);

        if (clockRateCount > 0)
            for (double clockRate : clockRates)
            {
                Format format;

                switch (mediaType)
                {
                    case AUDIO:
                        format = new AudioFormat(jmfEncoding);
                        break;
                    case VIDEO:
                        format = new VideoFormat(jmfEncoding);
                        break;
                    default:
                        throw new IllegalArgumentException("mediaType");
                }

                if (format != null)
                {
                    MediaFormat mediaFormat
                        = MediaFormatImpl
                            .createInstance(
                                format,
                                clockRate,
                                formatParameters);

                    if (mediaFormat != null)
                        mediaFormats.add(mediaFormat);
                }
            }
        else
        {
            Format format;

            switch (mediaType)
            {
                case AUDIO:
                    format = new AudioFormat(jmfEncoding);
                    break;
                case VIDEO:
                    format = new VideoFormat(jmfEncoding);
                    break;
                default:
                    throw new IllegalArgumentException("mediaType");
            }

            if (format != null)
            {
                MediaFormat mediaFormat
                    = MediaFormatImpl.createInstance(format);

                if (mediaFormat != null)
                    mediaFormats.add(mediaFormat);
            }
        }

        int mediaFormatCount = mediaFormats.size();
        String rtpPayloadTypeStr = Integer.toString(rtpPayloadType);

        if (mediaFormatCount > 0)
            rtpPayloadTypeStrToMediaFormats
                .put(
                    rtpPayloadTypeStr,
                    mediaFormats.toArray(new MediaFormat[mediaFormatCount]));

        /*
         * If there's also a well-known encoding name for the specified RTP
         * payload type, remember it as well.
         */
        if (encoding != null)
            rtpPayloadTypeStrToEncodings.put(rtpPayloadTypeStr, encoding);
    }

    /**
     * Returns the JMF encoding as specified in <tt>AudioFormat</tt> and
     * <tt>VideoFormat</tt> corresponding to the specified RTP payload type.
     * 
     * @param rtpPayloadTypeStr the RTP payload type as <tt>String</tt> to get
     * the respective JMF encoding of
     * @return the JMF encoding corresponding to the specified RTP payload type
     */
    public static String rtpPayloadTypeToJmfEncoding(String rtpPayloadTypeStr)
    {
        MediaFormat[] mediaFormats
            = rtpPayloadTypeStrToMediaFormats.get(rtpPayloadTypeStr);

        return
            ((mediaFormats != null) && (mediaFormats.length > 0))
                ? ((MediaFormatImpl<? extends Format>) mediaFormats[0])
                        .getJMFEncoding()
                : null;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s (expressed as an array) corresponding to
     * a specific RTP payload type (expressed as a <tt>String</tt>).
     *
     * @param rtpPayloadTypeStr the RTP payload type to retrieve the
     * corresponding <tt>MediaFormat</tt>s for
     * @return an array of <tt>MediaFormat</tt>s corresponding to the specified
     * RTP payload type
     */
    public static MediaFormat[] rtpPayloadTypeToMediaFormats(
            String rtpPayloadTypeStr)
    {
        MediaFormat[] mediaFormats
            = rtpPayloadTypeStrToMediaFormats.get(rtpPayloadTypeStr);

        return
            (mediaFormats == null)
                ? EMPTY_MEDIA_FORMATS
                : mediaFormats.clone();
    }

    /**
     * Converts a list of RTP payload types (specified as <tt>String</tt>s)  to
     * a list of JMF-specific encodings (as returned by
     * {@link Format#getEncoding()} or as specified by the respective
     * <tt>AudioFormat</tt> and <tt>VideoFormat</tt> constants).
     * 
     * @param rtpPayloadTypeStrings the list of RTP payload types (specified as
     * <tt>String</tt>s) to be converted to a list of JMF-specific encodings
     * @return a new list of JMF-specific encodings corresponding to the RTP
     * payload types specified as <tt>String</tt>s by
     * <tt>rtpPayloadTypeStrings</tt>
     */
    public static List<String> rtpPayloadTypesToJmfEncodings(
            List<String> rtpPayloadTypeStrings)
    {
        List<String> jmfEncodings = new ArrayList<String>();

        if (rtpPayloadTypeStrings != null)
            for (String rtpPayloadTypeStr : rtpPayloadTypeStrings)
            {
                String jmfEncoding
                    = rtpPayloadTypeToJmfEncoding(rtpPayloadTypeStr);

                if (jmfEncoding != null)
                    jmfEncodings.add(jmfEncoding);
            }
        return jmfEncodings;
    }
}
