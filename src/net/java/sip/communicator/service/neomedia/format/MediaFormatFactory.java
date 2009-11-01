/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.format;

import java.util.*;

/**
 * Allows the creation of audio and video <tt>MediaFormat</tt> instances.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public interface MediaFormatFactory
{

    /**
     * The constant to be used as an argument representing a clock rate to
     * denote that a specific clock rate is not specified.
     */
    public static final double CLOCK_RATE_NOT_SPECIFIED = -1;

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt> with
     * default clock rate and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a
     * <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt> which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(String encoding);

    /**
     * Creates a <tt>MediaFormat</tt> for the specified RTP payload type with
     * default clock rate and set of format parameters. If
     * <tt>rtpPayloadType</tt> is known to this <tt>MediaFormatFactory</tt>,
     * returns a <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt>
     * or a <tt>VideoMediaFormat</tt> instance. Otherwise, returns
     * <tt>null</tt>.
     *
     * @param rtpPayloadType the RTP payload type of the <tt>MediaFormat</tt> to
     * create
     * @return a <tt>MediaFormat</tt> with the specified <tt>rtpPayloadType</tt>
     * which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>rtpPayloadType</tt> is known to
     * this <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(byte rtpPayloadType);

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt> with
     * the specified <tt>clockRate</tt> and a default set of format parameters.
     * If <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>,
     * returns a <tt>MediaFormat</tt> which either an <tt>AudioMediaFormat</tt>
     * or a <tt>VideoMediaFormat</tt> instance. Otherwise, returns
     * <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt> and
     * <tt>clockRate</tt> which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
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
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt>
     * which either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance if
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>;
     * otherwise, <tt>null</tt>
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
