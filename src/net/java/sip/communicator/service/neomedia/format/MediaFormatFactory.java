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
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, a default clock rate for the specified
     * <tt>encoding</tt>, a single audio channel, and no format parameters.
     *
     * @param encoding the encoding of the format to create.
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public AudioMediaFormat createAudioMediaFormat(String encoding);

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
    public AudioMediaFormat createAudioMediaFormat(
                    String encoding, double clockRate);

    /**
     * Creates an <tt>AudioMediaFormat</tt> for the specified <tt>encoding</tt>,
     * <tt>clockRate</tt>, <tt>channels</tt>, and no format parameters.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the rate in Hz of the audio format
     * @param channels the number of availabe channels (1 for mono,
     *        2 for stereo)
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
     * @param formatParams any codec specific params that have being received
     * via SIP/SDP or XMPP/Jingle.
     *
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public AudioMediaFormat createAudioMediaFormat(
                    String encoding, double clockRate,
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

    /**
     * Creates a <tt>VideoMediaFormat</tt> for the specified <tt>encoding</tt>.
     *
     * @param encoding the encoding of the format to create.
     *
     * @return a newly created <tt>VideoMediaFormat</tt> with the specified
     * encoding.
     */
    public VideoMediaFormat createVideoMediaFormat(String encoding);

    /**
     * Creates an <tt>VideoMediaFormat</tt> for the specified <tt>encoding</tt>,
     * and <tt>frameRate</tt>.
     *
     * @param encoding the encoding of the format to create.
     * @param clockRate the the frame rate
     * @return a newly created <tt>AudioMediaFormat</tt> with the specified
     * parameters.
     */
    public VideoMediaFormat createVideoMediaFormat(
                    String encoding, double clockRate);
}
