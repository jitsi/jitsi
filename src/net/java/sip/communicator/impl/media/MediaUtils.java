/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import javax.media.format.*;
import javax.sdp.*;
import java.util.*;
import net.java.sip.communicator.impl.media.codec.*;

/**
 * Implements static utility methods used by media classes.
 *
 * @author Emil Ivov
 */
public class MediaUtils
{
    /**
     * Returns the String encoding, as specified in AudioFormat and VideoFormat,
     * corresponding to the format specified in sdpFormatStr
     * @param sdpEncodingStr the SDP index that we'd like to convert to a JMF
     * format.
     * @return one of the AudioFormat.XXX or VideoFormat.XXX format strings.
     */
    public static String sdpToJmfEncoding(String sdpEncodingStr)
    {
        int sdpEncoding = -1;
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
            default:
                return null;
        }
    }

    /**
     * Returns a String containing the SDP encoding number corresponding to
     * <tt>jmfFormat</tt>.
     *
     * @param jmfEncoding one of the AudioFormat.XXX or VideoFormat.XXX format
     * strings.
     *
     * @return a string containing the SDP index corresponding to
     * <tt>jmfEncoding</tt>.
     */
    public static String jmfToSdpEncoding(String jmfEncoding)
    {
        if (jmfEncoding == null)
        {
            return null;
        }
        else if (jmfEncoding.equals(AudioFormat.ULAW_RTP))
        {
            return Integer.toString(SdpConstants.PCMU);
        }
        else if (jmfEncoding.equals(Constants.ALAW_RTP))
        {
            return Integer.toString(SdpConstants.PCMA);
        }
        else if (jmfEncoding.equals(AudioFormat.GSM_RTP))
        {
            return Integer.toString(SdpConstants.GSM);
        }
        else if (jmfEncoding.equals(AudioFormat.G723_RTP))
        {
            return Integer.toString(SdpConstants.G723);
        }
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
        {
            return Integer.toString(SdpConstants.DVI4_8000);
        }
        else if (jmfEncoding.equals(AudioFormat.DVI_RTP))
        {
            return Integer.toString(SdpConstants.DVI4_16000);
        }
        else if (jmfEncoding.equals(AudioFormat.ALAW))
        {
            return Integer.toString(SdpConstants.PCMA);
        }
        else if (jmfEncoding.equals(AudioFormat.G728_RTP))
        {
            return Integer.toString(SdpConstants.G728);
        }
        else if (jmfEncoding.equals(AudioFormat.G729_RTP))
        {
            return Integer.toString(SdpConstants.G729);
        }
        else if (jmfEncoding.equals(VideoFormat.H263_RTP))
        {
            return Integer.toString(SdpConstants.H263);
        }
        else if (jmfEncoding.equals(VideoFormat.JPEG_RTP))
        {
            return Integer.toString(SdpConstants.JPEG);
        }
        else if (jmfEncoding.equals(VideoFormat.H261_RTP))
        {
            return Integer.toString(SdpConstants.H261);
        }
        else if (jmfEncoding.equals(Constants.ILBC))
        {
            return Integer.toString(97);
        }
        else if (jmfEncoding.equals(Constants.ILBC_RTP))
        {
            return Integer.toString(97);
        }
        else if (jmfEncoding.equals(Constants.SPEEX))
        {
            return Integer.toString(110);
        }
        else if (jmfEncoding.equals(Constants.SPEEX_RTP))
        {
            return Integer.toString(110);
        }
        else
        {
            return null;
        }
    }

    /**
     * Converts the list of <tt>sdpEncodings</tt> to a list of jmf compatible
     * encoding strings as specified by the static vars in VideoFormat
     * and AudioFormat.
     *
     * @param sdpEncodings a list containing strings representing SDP format
     * codes.
     * @return a list of strings representing JMF compatible encoding names.
     */
    public static List sdpToJmfEncodings(List sdpEncodings)
    {
        ArrayList jmfEncodings = new ArrayList();
        if (sdpEncodings == null)
            return jmfEncodings;

        for (int i = 0; i < sdpEncodings.size(); i++)
        {
            String jmfEncoding =
                sdpToJmfEncoding((String)sdpEncodings.get(i));
            if (jmfEncoding != null)
            {
                jmfEncodings.add(jmfEncoding);
            }
        }
        return jmfEncodings;
    }

}
