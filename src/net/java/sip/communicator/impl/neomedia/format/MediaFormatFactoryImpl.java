/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaFormatFactory</tt> for the JMF <tt>Format</tt> types.
 *
 * @author Lubomir Marinov
 */
public class MediaFormatFactoryImpl
    implements MediaFormatFactory
{

    /*
     * Implements MediaFormatFactory#createAudioMediaFormat(String).
     */
    public AudioMediaFormat createAudioMediaFormat(String encoding)
    {
        return new AudioMediaFormatImpl(encoding);
    }

    /*
     * Implements MediaFormatFactory#createAudioMediaFormat(String, double).
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate)
    {
        return new AudioMediaFormatImpl(encoding, clockRate);
    }

    /*
     * Implements MediaFormatFactory#createAudioMediaFormat(String, double,
     * int).
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels)
    {
        return new AudioMediaFormatImpl(encoding, clockRate, channels);
    }

    /*
     * Implements MediaFormatFactory#createAudioMediaFormat(String, double,
     * Map<String, String>).
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams)
    {
        return new AudioMediaFormatImpl(encoding, clockRate, formatParams);
    }

    /*
     * Implements MediaFormatFactory#createAudioMediaFormat(String, double, int,
     * Map<String, String>).
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams)
    {
        return
            new AudioMediaFormatImpl(
                    encoding,
                    clockRate,
                    channels,
                    formatParams);
    }

    /*
     * Implements MediaFormatFactory#createVideoMediaFormat(String).
     */
    public VideoMediaFormat createVideoMediaFormat(String encoding)
    {
        return new VideoMediaFormatImpl(encoding);
    }

    /*
     * Implements MediaFormatFactory#createVideoMediaFormat(String, double).
     */
    public VideoMediaFormat createVideoMediaFormat(
            String encoding,
            double clockRate)
    {
        return new VideoMediaFormatImpl(encoding, clockRate);
    }
}
