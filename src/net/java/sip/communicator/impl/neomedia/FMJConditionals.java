/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import javax.media.format.*;

/**
 * Class to centralize workarounds or changes that need to be made for FMJ to work.
 * This is also a place to tweak which workarounds are used.
 *
 * @author Ken Larson
 */
public class FMJConditionals
{
    public static final boolean IS_FMJ = false;

    /**
     * Some SC codecs depend on internal Sun/IBM JMF classes.
     */
    public static final boolean FMJ_CODECS = IS_FMJ;

    /**
     * FMJ's filter graph builder does not give specific formats for the tracks,
     * but rather a general format with no encoding.
     */
    public static final AudioFormat FORCE_AUDIO_FORMAT = !IS_FMJ
        ? null
        : new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1);

    // to force ALAW/rtp, change the format to the following
//      new AudioFormat(
//          "ALAW/rtp",
//          8000,
//          8,
//          1,
//          -1,
//          AudioFormat.SIGNED);

    /**
     * JMF stores CUSTOM_CODEC_FORMATS statically, so they only need to be
     * registered once. FMJ does this dynamically (per instance), so it needs
     * to be done for every time we instantiate an RTP manager. This variable
     * determines whether we should register them every time (FMJ) or not (JMF).
     */
    public static final boolean REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
        = IS_FMJ;

    /**
     * Whether to use JMF's internal registry to avoid auto-detecting capture
     * devices each time, by tagging it with our own "author" property.
     */
    public static final boolean USE_JMF_INTERNAL_REGISTRY = !IS_FMJ;
}

