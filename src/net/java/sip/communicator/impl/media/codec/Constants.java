/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec;

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

    /**
     * mode    : Frame size for the encoding/decoding
     * 20 - 20 ms
     * 30 - 30 ms
     */
    public static int ILBC_MODE = 30;
}
