/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.format.*;
import javax.sdp.*;

import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * Implements static utility methods used by media classes.
 * 
 * @author Emil Ivov
 */
public class MediaUtils
{

    /**
     * Returned by {@link #sdpToJmfEncoding(String)} if it does not know the
     * given encoding.
     */
    public static final int UNKNOWN_ENCODING = -1;

    /**
     * Returns the String encoding, as specified in AudioFormat and VideoFormat,
     * corresponding to the format specified in sdpFormatStr
     * 
     * @param sdpEncodingStr the SDP index that we'd like to convert to a JMF
     *            format.
     * @return one of the AudioFormat.XXX or VideoFormat.XXX format strings.
     */
    public static String sdpToJmfEncoding(String sdpEncodingStr)
    {
        int sdpEncoding = UNKNOWN_ENCODING;

        try
        {
            sdpEncoding = Integer.parseInt(sdpEncodingStr);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }

        switch (sdpEncoding)
        {
        case SdpConstants.PCMU:
            return AudioFormat.ULAW_RTP;

        case SdpConstants.GSM:
            return AudioFormat.GSM_RTP;

        case SdpConstants.G723:
            return AudioFormat.G723_RTP;

        case SdpConstants.DVI4_8000:
            return AudioFormat.DVI_RTP;

        case SdpConstants.DVI4_16000:
            return AudioFormat.DVI_RTP;

        case SdpConstants.PCMA:
            return Constants.ALAW_RTP;

        case 97:
            return Constants.ILBC_RTP;

        case 98:
            return Constants.ILBC_RTP;

        case 110:
            return Constants.SPEEX_RTP;

        case SdpConstants.G728:
            return AudioFormat.G728_RTP;

        case SdpConstants.G729:
            return AudioFormat.G729_RTP;

        case SdpConstants.H263:
            return VideoFormat.H263_RTP;

        case SdpConstants.JPEG:
            return VideoFormat.JPEG_RTP;

        case SdpConstants.H261:
            return VideoFormat.H261_RTP;

        case Constants.H264_RTP_SDP:
            return Constants.H264_RTP;

        default:
            return null;
        }
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
        {
            return UNKNOWN_ENCODING;
        }
        else if (jmfEncoding.equals(AudioFormat.ULAW_RTP))
        {
            return SdpConstants.PCMU;
        }
        else if (jmfEncoding.equals(Constants.ALAW_RTP))
        {
            return SdpConstants.PCMA;
        }
        else if (jmfEncoding.equals(AudioFormat.GSM_RTP))
        {
            return SdpConstants.GSM;
        }
        else if (jmfEncoding.equals(AudioFormat.G723_RTP))
        {
            return SdpConstants.G723;
        }
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
        {
            return SdpConstants.DVI4_8000;
        }
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
        {
            return SdpConstants.DVI4_16000;
        }
        else if (jmfEncoding.equals(AudioFormat.ALAW))
        {
            return SdpConstants.PCMA;
        }
        else if (jmfEncoding.equals(AudioFormat.G728_RTP))
        {
            return SdpConstants.G728;
        }
        else if (jmfEncoding.equals(AudioFormat.G729_RTP))
        {
            return SdpConstants.G729;
        }
        else if (jmfEncoding.equals(VideoFormat.H263_RTP))
        {
            return SdpConstants.H263;
        }
        else if (jmfEncoding.equals(VideoFormat.JPEG_RTP))
        {
            return SdpConstants.JPEG;
        }
        else if (jmfEncoding.equals(VideoFormat.H261_RTP))
        {
            return SdpConstants.H261;
        }
        else if (jmfEncoding.equals(Constants.H264_RTP))
        {
            return Constants.H264_RTP_SDP;
        }
        else if (jmfEncoding.equals(Constants.ILBC))
        {
            return 97;
        }
        else if (jmfEncoding.equals(Constants.ILBC_RTP))
        {
            return 97;
        }
        else if (jmfEncoding.equals(Constants.SPEEX))
        {
            return 110;
        }
        else if (jmfEncoding.equals(Constants.SPEEX_RTP))
        {
            return 110;
        }
        else
        {
            return UNKNOWN_ENCODING;
        }
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
        {
            for (String sdpEncoding : sdpEncodings)
            {
                String jmfEncoding = sdpToJmfEncoding(sdpEncoding);

                if (jmfEncoding != null)
                {
                    jmfEncodings.add(jmfEncoding);
                }
            }
        }
        return jmfEncodings;
    }
}
