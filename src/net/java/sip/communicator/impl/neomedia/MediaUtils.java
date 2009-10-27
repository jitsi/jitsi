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
     * <tt>MediaFormat</tt>s.
     */
    private static final Map<String, MediaFormat[]>
        rtpPayloadTypeStr2MediaFormats
            = new HashMap<String, MediaFormat[]>();

    /**
     * Returned by {@link #sdpToJmfEncoding(String)} if it does not know the
     * given encoding.
     */
    public static final int UNKNOWN_ENCODING = -1;

    static
    {
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.PCMU,
            MediaType.AUDIO,
            AudioFormat.ULAW_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.GSM,
            MediaType.AUDIO,
            AudioFormat.GSM_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G723,
            MediaType.AUDIO,
            AudioFormat.G723_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.DVI4_8000,
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.DVI4_16000,
            MediaType.AUDIO,
            AudioFormat.DVI_RTP,
            16000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.PCMA,
            MediaType.AUDIO,
            Constants.ALAW_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            97,
            MediaType.AUDIO,
            Constants.ILBC_RTP);
        mapRtpPayloadTypeToMediaFormats(
            98,
            MediaType.AUDIO,
            Constants.ILBC_RTP);
        mapRtpPayloadTypeToMediaFormats(
            110,
            MediaType.AUDIO,
            Constants.SPEEX_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G728,
            MediaType.AUDIO,
            AudioFormat.G728_RTP,
            8000);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.G729,
            MediaType.AUDIO,
            AudioFormat.G729_RTP,
            8000);

        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.H263,
            MediaType.VIDEO,
            VideoFormat.H263_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.JPEG,
            MediaType.VIDEO,
            VideoFormat.JPEG_RTP);
        mapRtpPayloadTypeToMediaFormats(
            SdpConstants.H261,
            MediaType.VIDEO,
            VideoFormat.H261_RTP);
        mapRtpPayloadTypeToMediaFormats(
            Constants.H264_RTP_SDP,
            MediaType.VIDEO,
            Constants.H264_RTP);
    }

    /**
     * Adds a new mapping of a specific RTP payload type to a list of
     * <tt>MediaFormat</tt>s of a specific <tt>MediaType</tt>, with a specific
     * JMF encoding and, optionally, with specific clock rates.
     *
     * @param rtpPayloadType the RTP payload type to be associated with a list
     * of <tt>MediaFormat</tt>s
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to
     * be associated with <tt>rtpPayloadType</tt>
     * @param jmfEncoding the JMF encoding of the <tt>MediaFormat</tt>s to be
     * associated with <tt>rtpPayloadType</tt>
     * @param clockRates the optional list of clock rates of the
     * <tt>MediaFormat</tt>s to be associated with <tt>rtpPayloadType</tt>
     */
    private static void mapRtpPayloadTypeToMediaFormats(
            int rtpPayloadType,
            MediaType mediaType,
            String jmfEncoding,
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
                        = MediaFormatImpl.createInstance(format, clockRate);

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

        if (mediaFormatCount > 0)
            rtpPayloadTypeStr2MediaFormats
                .put(
                    Integer.toString(rtpPayloadType),
                    mediaFormats.toArray(new MediaFormat[mediaFormatCount]));
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
            = rtpPayloadTypeStr2MediaFormats.get(rtpPayloadTypeStr);

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
            = rtpPayloadTypeStr2MediaFormats.get(rtpPayloadTypeStr);

        return
            (mediaFormats == null)
                ? EMPTY_MEDIA_FORMATS
                : mediaFormats.clone();
    }

    /**
     * Returns the SDP encoding number corresponding to <tt>jmfFormat</tt>.
     * 
     * @param jmfEncoding one of the AudioFormat.XXX or VideoFormat.XXX format
     *            strings.
     * 
     * @return the SDP index corresponding to <tt>jmfEncoding</tt>. Returns
     *         {@link #UNKNOWN_ENCODING} if the encoding is not supported or
     *         <code>jmfEncoding</code> is <code>null</code>.
     */
    public static int jmfToSdpEncoding(String jmfEncoding)
    {
        if (jmfEncoding == null)
            return UNKNOWN_ENCODING;
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
            return UNKNOWN_ENCODING;
    }

    /**
     * Converts the list of <tt>sdpEncodings</tt> to a list of jmf compatible
     * encoding strings as specified by the static vars in VideoFormat and
     * AudioFormat.
     * 
     * @param sdpEncodings a list containing strings representing SDP format
     *            codes.
     * @return a list of strings representing JMF compatible encoding names.
     */
    public static List<String> sdpToJmfEncodings(List<String> sdpEncodings)
    {
        List<String> jmfEncodings = new ArrayList<String>();

        if (sdpEncodings != null)
            for (String sdpEncoding : sdpEncodings)
            {
                String jmfEncoding = rtpPayloadTypeToJmfEncoding(sdpEncoding);

                if (jmfEncoding != null)
                    jmfEncodings.add(jmfEncoding);
            }
        return jmfEncodings;
    }
}
