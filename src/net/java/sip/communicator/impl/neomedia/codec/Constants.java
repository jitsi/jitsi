/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec;

import net.java.sip.communicator.util.*;

/**
 * Allows start import of <tt>net.java.sip.communicator.impl.neomedia.codec</tt>
 * in order to get the constants define in
 * <tt>net.java.sip.communicator.impl.neomedia.codec.Constants</tt> without star
 * import of <tt>net.java.sip.communicator.impl.neomedia.codec</tt>.
 *
 * @author Lubomir Marinov
 */
public class Constants
{
    public static final String ALAW_RTP = "ALAW/rtp";
    public static final String G722 = "g722";
    public static final String G722_RTP = "g722/rtp";
    public static final String ILBC = "ilbc";
    public static final String ILBC_RTP = "ilbc/rtp";
    public static final String SILK = "SILK";
    public static final String SILK_RTP = "SILK/rtp";
    public static final String SPEEX = "speex";
    public static final String SPEEX_RTP = "speex/rtp";

    public static final String H264 = "h264";
    public static final String H264_RTP = "h264/rtp";
    public static final String H263P = "H263-1998";
    public static final String H263P_RTP = "h263-1998/rtp";

    /**
     * Pseudo format representing DTMF tones sent over RTP.
     */
    public static final String TELEPHONE_EVENT = "telephone-event";

    /**
     * mode    : Frame size for the encoding/decoding
     * 20 - 20 ms
     * 30 - 30 ms
     */
    public static int ILBC_MODE = 30;

    /**
     * Default video width.
     */
    public static final int VIDEO_WIDTH;

    /**
     * Default video height.
     */
    public static final int VIDEO_HEIGHT;

    static
    {

        /*
         * On Mac OS X, the Apple iSight camera reports two sizes 640x480 and
         * 320x240 if we use the default size 352x288 we must use source format
         * 640x480 in this situation we suffer from high cpu usage as every
         * frame is scaled, so we use the non-standard format 320x240.
         */
        if (OSUtils.IS_MAC)
        {
            VIDEO_WIDTH = 320;
            VIDEO_HEIGHT = 240;
        }
        else
        {
            VIDEO_WIDTH = 352;
            VIDEO_HEIGHT = 288;
        }
    }
}
