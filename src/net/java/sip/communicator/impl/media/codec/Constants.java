/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec;

import net.java.sip.communicator.util.*;

/**
 * Constants needed in codecs impl.
 * @author Damian Minkov
 */
public class Constants
{
    public static final String ALAW_RTP = "ALAW/rtp";
    public static final String SPEEX_RTP = "speex/rtp";
    public static final String SPEEX = "speex";
    public static final String ILBC_RTP = "ilbc/rtp";
    public static final String ILBC = "ilbc";

    public static final String H264_RTP = "h264/rtp";
    public static final String H264 = "h264";
    public static final int H264_RTP_SDP = 99;

    /**
     * mode    : Frame size for the encoding/decoding
     * 20 - 20 ms
     * 30 - 30 ms
     */
    public static int ILBC_MODE = 30;

    public static final int VIDEO_WIDTH;
    public static final int VIDEO_HEIGHT;

    static
    {

        /*
         * On Mac OS X, the Apple iSight camera reports two sizes 640x480 and
         * 320x240 if we use the default size 352x288 we must use source format
         * 640x480 in this situation we suffer from high cpu usage as every
         * frame is scaled, so we use the non standard format 320x240.
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
