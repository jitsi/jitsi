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
     * The constant to be used as an argument representing number of channels to
     * denote that a specific number of channels is not specified.
     */
    public static final int CHANNELS_NOT_SPECIFIED = -1;

    /**
     * The constant to be used as an argument representing a clock rate to
     * denote that a specific clock rate is not specified.
     */
    public static final double CLOCK_RATE_NOT_SPECIFIED = -1;

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt> with
     * default clock rate and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a
     * <tt>MediaFormat</tt> which is either an <tt>AudioMediaFormat</tt> or a
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
     * returns a <tt>MediaFormat</tt> which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance.
     * Otherwise, returns <tt>null</tt>.
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
     * returns a <tt>MediaFormat</tt> which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance.
     * Otherwise, returns <tt>null</tt>.
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
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt> and a default set of format
     * parameters. If <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt> which is
     * either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and <tt>channels</tt> and a default set of format
     * parameters which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            int channels);

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters. If <tt>encoding</tt> is
     * known to this <tt>MediaFormatFactory</tt>, returns a <tt>MediaFormat</tt>
     * which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @param advancedAttrs advanced attributes received via SIP/SDP or
     * XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt> and set of format parameters which is either an
     * <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt> instance if
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>;
     * otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams,
            Map<String, String> advancedAttrs);

    /**
     * Creates a <tt>MediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters. If
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>, returns a
     * <tt>MediaFormat</tt> which is either an <tt>AudioMediaFormat</tt> or a
     * <tt>VideoMediaFormat</tt> instance. Otherwise, returns <tt>null</tt>.
     *
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @param advancedAttrs advanced attributes received via SIP/SDP or
     * XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams,
            Map<String, String> advancedAttrs);

    /**
     * Creates a <tt>MediaFormat</tt> either for the specified
     * <tt>rtpPayloadType</tt> or for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters. If
     * <tt>encoding</tt> is known to this <tt>MediaFormatFactory</tt>, ignores
     * <tt>rtpPayloadType</tt> and returns a <tt>MediaFormat</tt> which is
     * either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance. If <tt>rtpPayloadType</tt> is not
     * {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN} and <tt>encoding</tt> is
     * <tt>null</tt>, uses the encoding associated with <tt>rtpPayloadType</tt>.
     *
     * @param rtpPayloadType the RTP payload type to create a
     * <tt>MediaFormat</tt> for; {@link MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN} if
     * <tt>encoding</tt> is not <tt>null</tt>. If <tt>rtpPayloadType</tt> is not
     * <tt>MediaFormat#RTP_PAYLOAD_TYPE_UNKNOWN</tt> and <tt>encoding</tt> is
     * not <tt>null</tt>, <tt>rtpPayloadType</tt> is ignored
     * @param encoding the well-known encoding (name) to create a
     * <tt>MediaFormat</tt> for; <tt>null</tt>
     * @param clockRate the clock rate in Hz to create a <tt>MediaFormat</tt>
     * for
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo) if it makes sense for the <tt>MediaFormat</tt> with the specified
     * <tt>encoding</tt>; otherwise, ignored
     * @param formatParams any codec specific parameters which have been
     * received via SIP/SDP or XMPP/Jingle
     * @param advancedAttrs advanced attributes received via SIP/SDP or
     * XMPP/Jingle
     * @return a <tt>MediaFormat</tt> with the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt> and set of format parameters which
     * is either an <tt>AudioMediaFormat</tt> or a <tt>VideoMediaFormat</tt>
     * instance if <tt>encoding</tt> is known to this
     * <tt>MediaFormatFactory</tt>; otherwise, <tt>null</tt>
     */
    public MediaFormat createMediaFormat(
            byte rtpPayloadType,
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams,
            Map<String, String> advancedAttrs);
}
