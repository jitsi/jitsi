/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.mediamgr;

import java.util.*;
import javax.media.format.*;
import javax.sdp.*;
import net.java.sip.communicator.util.*;

//
// TODO: merge this MediaUtils with the one in impl/media
// There is a MediaUtils class in package impl.media
// which isn't exposed by the media service. It will
// be a good idea to find a mean to merge this two
// class somewhere
//
/**
 * Media utils
 * A collection of static methods related to media.
 *
 * @author Symphorien Wanko
 */
public abstract class MediaUtils
{

    /**
     * logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(MediaUtils.class);

    /**
     * <tt>getAudioEncoding</tt> return an <tt>Hashtable</tt>
     * with one entry in a format wich can be directly passed
     * to the media service for creating a <tt>RtpFlow</tt>
     * based on this encoding.
     *
     * @param payloadType the payload type
     * @return an hashtable ready to be passed to media service
     */
    public static Map<String, List<String>> getAudioEncoding(int payloadType)
    {
        Map<String, List<String>> ht = new Hashtable<String, List<String>>();
        List<String> list = new ArrayList<String>();
        ht.put("audio", list);
        // most of the format aren't handled by SC now
        // but itsn't a problem. it will facilitates
        // updates when more codecs will become available
        // in SC.
        switch (payloadType)
        {
            case SdpConstants.PCMU:
                list.add(AudioFormat.ULAW_RTP);
                return ht;
            case SdpConstants.GSM:
                list.add(AudioFormat.GSM_RTP);
                return ht;
            case SdpConstants.G723:
                list.add(AudioFormat.G723_RTP);
                return ht;
            case SdpConstants.DVI4_8000:
                list.add(AudioFormat.DVI_RTP);
                return ht;
            case SdpConstants.DVI4_16000:
                list.add(AudioFormat.DVI_RTP);
                return ht;
            //case SdpConstants.LPC:
            //    list.add(AudioFormat...);
            //    return ht;
            case SdpConstants.PCMA:
                list.add(AudioFormat.ALAW);
                return ht;
            //case SdpConstants.G722:
            //    list.add(AudioFormat...);
            //    return ht;
            //case SdpConstants.L16_1CH:
            //    list.add(AudioFormat...);
            //    return ht;
            //case SdpConstants.L16_2CH:
            //    list.add(AudioFormat...);
            //    return ht;
            //case SdpConstants.QCELP:
            //    list.add(AudioFormat...);
            //    return ht;
            //case SdpConstants.CN:
            //    list.add(AudioFormat...);
            //    return ht;
            //case SdpConstants.MPA:
            //    list.add(AudioFormat...);
            //    return ht;
            case SdpConstants.G728:
                list.add(AudioFormat.G728_RTP);
                return ht;
            case SdpConstants.DVI4_11025:
                list.add(AudioFormat.DVI_RTP);
                return ht;
            case SdpConstants.DVI4_22050:
                list.add(AudioFormat.DVI_RTP);
                return ht;
            case SdpConstants.G729:
                list.add(AudioFormat.G729_RTP);
                return ht;
            default:
                //throw new IllegalStateException("Unknown payload type");
                logger.warn("unknown payload type : " + payloadType);
                return null;
        }
    }

    /**
     * This method gives the name wich correspond to a payload type
     *
     * @param payloadType the type of the payload
     * @return the string corresponding to the payload
     */
    public static String getPayloadName(int payloadType)
    {
        // for update, seee http://tools.ietf.org/html/rfc3551#page-32
        switch (payloadType)
        {
            case SdpConstants.PCMU:
                return "PCMU";
            case SdpConstants.GSM:
                return "GSM";
            case SdpConstants.G723:
                return "G723";
            case SdpConstants.DVI4_8000:
                return "DVI4_8000";
            case SdpConstants.DVI4_16000:
                return "DVI4_16000";
            case SdpConstants.LPC:
                return "LPC";
            case SdpConstants.PCMA:
                return "PCMA";
            case SdpConstants.G722:
                return "G722";
            case SdpConstants.L16_1CH:
                return "L16_1CH";
            case SdpConstants.L16_2CH:
                return "L16_2CH";
            case SdpConstants.QCELP:
                return "QCELP";
            case SdpConstants.CN:
                return "CN";
            case SdpConstants.MPA:
                return "MPA";
            case SdpConstants.G728:
                return "G728";
            case SdpConstants.DVI4_11025:
                return "DVI4_11025";
            case SdpConstants.DVI4_22050:
                return "DVI4_22050";
            case SdpConstants.G729:
                return "G729";
            default:
                //throw new IllegalStateException("Unknown payload type");
                if (logger.isDebugEnabled())
                    logger.debug("unknown payload type : " + payloadType);
                return null;
        }
    }

    /**
     * Convert a <tt>SdpConstant</tt> to the corresponding payload type.
     *
     * @param sdpConstant the sdp constant to convert.
     * @return the payload type which match the provided sdp constant.
     */
    public static int getPayloadType(int sdpConstant)
    {
        switch (sdpConstant)
        {
            case SdpConstants.PCMU:
                return 0;
            case SdpConstants.GSM:
                return 3;
            case SdpConstants.G723:
                return 4;
            case SdpConstants.DVI4_8000:
                return 5;
            case SdpConstants.DVI4_16000:
                return 6;
            case SdpConstants.LPC:
                return 7;
            case SdpConstants.PCMA:
                return 8;
            case SdpConstants.G722:
                return 9;
            case SdpConstants.L16_1CH:
                return 10;
            case SdpConstants.L16_2CH:
                return 11;
            case SdpConstants.QCELP:
                return 12;
            case SdpConstants.CN:
                return 13;
            case SdpConstants.MPA:
                return 14;
            case SdpConstants.G728:
                return 15;
            case SdpConstants.DVI4_11025:
                return 16;
            case SdpConstants.DVI4_22050:
                return 17;
            case SdpConstants.G729:
                return 18;
            default:
                //throw new IllegalStateException("Unknown sdp constant");
                if (logger.isDebugEnabled())
                    logger.debug("unknown sdp constant : " + sdpConstant);
                return -1;
        }
    }
}
