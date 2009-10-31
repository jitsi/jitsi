/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.format;

import java.util.*;

/**
 * The MediaFormatFactory allows creating instances of Audio and Video formats.
 *
 * @author Emil Ivov
 */
public interface MediaFormatFactory
{

    /**
     * The constant to be used as an argument representing a clock rate to
     * denote that a specific clock rate is not specified.
     */
    public static final double CLOCK_RATE_NOT_SPECIFIED = -1;

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, a default clock rate for the specified
     * <tt>encoding</tt>, a single audio channel, and no format parameters.
     *
     * @param encoding the encoding of the format to create.
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public MediaFormat createMediaFormat(String encoding);

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, a single audio channel, and no format parameters.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the rate in Hz of the audio format
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate);

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt>, and no format parameters.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the rate in Hz of the audio format
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo)
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public AudioMediaFormat createAudioMediaFormat(
                    String encoding, double clockRate, int channels);

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>formatParams</tt> parameters and a single audio
     * channel.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the rate in Hz of the audio format
     * @param formatParams any codec specific params that have been received via
     * SIP/SDP or XMPP/Jingle.
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams);

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt>, and <tt>formatParams</tt>
     * parameters.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the rate in Hz of the audio format
     * @param channels the number of availabe channels (1 for mono,
     *        2 for stereo)
     * @param formatParams any codec specific params that have being received
     * via SIP/SDP or XMPP/Jingle.
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public AudioMediaFormat createAudioMediaFormat(
                    String encoding, double clockRate, int channels,
                    Map<String, String> formatParams);
}
