/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>AudioMediaFormat</tt> for the JMF <tt>AudioFormat</tt>.
 *
 * @author Lubomir Marinov
 */
public class AudioMediaFormatImpl
    extends MediaFormatImpl<AudioFormat>
    implements AudioMediaFormat
{

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance which is to
     * provide an implementation of <tt>AudioMediaFormat</tt> for a specific
     * JMF <tt>AudioFormat</tt>.
     *
     * @param format the JMF <tt>AudioFormat</tt> the new instance is to wrap
     * and provide an implementation of <tt>AudioMediaFormat</tt> for
     */
    public AudioMediaFormatImpl(AudioFormat format)
    {
        super(format);
    }

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance with the
     * specified encoding and a single audio channel.
     *
     * @param encoding the encoding of the new <tt>AudioMediaFormatImpl</tt>
     * instance
     */
    public AudioMediaFormatImpl(String encoding)
    {
        super(new AudioFormat(encoding));
    }

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance with the
     * specified encoding and clock rate and a single audio channel.
     *
     * @param encoding the encoding of the new <tt>AudioMediaFormatImpl</tt>
     * instance
     * @param clockRate the clock (i.e. sample) rate of the new
     * <tt>AudioMediaFormatImpl</tt> instance
     */
    public AudioMediaFormatImpl(String encoding, double clockRate)
    {
        this(encoding, clockRate, 1);
    }

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance with the
     * specified encoding, clock rate and number of audio channels.
     *
     * @param encoding the encoding of the new <tt>AudioMediaFormatImpl</tt>
     * instance
     * @param clockRate the clock (i.e. sample) rate of the new
     * <tt>AudioMediaFormatImpl</tt> instance
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo)
     */
    public AudioMediaFormatImpl(String encoding, double clockRate, int channels)
    {
        this(encoding, clockRate, channels, null);
    }

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance with the
     * specified encoding, clock rate and format parameters and a single audio
     * channel.
     *
     * @param encoding the encoding of the new <tt>AudioMediaFormatImpl</tt>
     * instance
     * @param clockRate the clock (i.e. sample) rate of the new
     * <tt>AudioMediaFormatImpl</tt> instance
     * @param formatParameters any codec-specific parameters that have been
     * received via SIP/SDP or XMPP/Jingle.
     */
    public AudioMediaFormatImpl(
            String encoding,
            double clockRate,
            Map<String, String> formatParameters)
    {
        this(encoding, clockRate, 1, formatParameters);
    }

    /**
     * Initializes a new <tt>AudioMediaFormatImpl</tt> instance with the
     * specified encoding, clock rate, number of audio channels and format
     * parameters.
     *
     * @param encoding the encoding of the new <tt>AudioMediaFormatImpl</tt>
     * instance
     * @param clockRate the clock (i.e. sample) rate of the new
     * <tt>AudioMediaFormatImpl</tt> instance
     * @param channels the number of available channels (1 for mono, 2 for
     * stereo)
     * @param formatParameters any codec-specific parameters that have been
     * received via SIP/SDP or XMPP/Jingle
     */
    public AudioMediaFormatImpl(
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParameters)
    {
        super(
            new AudioFormat(
                    encoding,
                    clockRate,
                    AudioFormat.NOT_SPECIFIED,
                    channels),
            formatParameters);
    }

    /*
     * Implements AudioMediaFormat#getChannels(). Delegates to AudioFormat.
     */
    public int getChannels()
    {
        return format.getChannels();
    }

    /*
     * Implements MediaFormat#getClockRate(). Returns
     * AudioFormat#getSampleRate().
     */
    public double getClockRate()
    {
        return format.getSampleRate();
    }

    /*
     * Implements MediaFormat#getMediaType(). Returns MediaType#AUDIO.
     */
    public final MediaType getMediaType()
    {
        return MediaType.AUDIO;
    }
}
