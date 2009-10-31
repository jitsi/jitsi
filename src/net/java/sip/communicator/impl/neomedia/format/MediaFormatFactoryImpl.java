/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.format;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.service.neomedia.MediaType;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaFormatFactory</tt> for the JMF <tt>Format</tt> types.
 *
 * @author Lubomir Marinov
 */
public class MediaFormatFactoryImpl
    implements MediaFormatFactory
{

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with a specific encoding
     * (name).
     *
     * @param encoding the encoding (name) of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name)
     * @see MediaFormatFactory#createAudioMediaFormat(String)
     */
    public MediaFormat createMediaFormat(String encoding)
    {
        return createMediaFormat(encoding, CLOCK_RATE_NOT_SPECIFIED);
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name) and clock rate.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name) and clock rate
     * @see MediaFormatFactory#createAudioMediaFormat(String, double)
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate)
    {
        List<MediaFormat> supportedMediaFormats
            = getSupportedMediaFormats(encoding, clockRate);

        return
            supportedMediaFormats.isEmpty()
                ? null
                : supportedMediaFormats.get(0);
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate and number of channels.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param channels the number of channels of the new instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate and number of channels
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, int)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels)
    {
        for (MediaFormat format : getSupportedMediaFormats(encoding, clockRate))
            if (format instanceof AudioMediaFormat)
            {
                AudioMediaFormat audioFormat = (AudioMediaFormat) format;

                if (audioFormat.getChannels() == channels)
                    return audioFormat;
            }
        return null;
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate and set of format-specific parameters.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param formatParams the set of format-specific parameters of the new
     * instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate and set of format-specific parameters
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, Map)
     */
    public MediaFormat createMediaFormat(
            String encoding,
            double clockRate,
            Map<String, String> formatParams)
    {
        MediaFormat mediaFormat = createMediaFormat(encoding, clockRate);

        if ((mediaFormat != null)
                && (formatParams != null)
                && !formatParams.isEmpty())
        {
            Map<String, String> formatParameters
                = new HashMap<String, String>();

            formatParameters.putAll(mediaFormat.getFormatParameters());
            formatParameters.putAll(formatParams);

            switch (mediaFormat.getMediaType())
            {
            case AUDIO:
                mediaFormat
                    = new AudioMediaFormatImpl(
                            ((AudioMediaFormatImpl) mediaFormat).getFormat(),
                            formatParameters);
                break;
            case VIDEO:
                VideoMediaFormatImpl videoMediaFormatImpl
                    = (VideoMediaFormatImpl) mediaFormat;

                mediaFormat
                    = new VideoMediaFormatImpl(
                            videoMediaFormatImpl.getFormat(),
                            videoMediaFormatImpl.getClockRate(),
                            formatParameters);
                break;
            default:
                mediaFormat = null;
            }
        }
        return mediaFormat;
    }

    /**
     * Creates a new <tt>AudioMediaFormat</tt> instance with specific encoding
     * (name), clock rate, number of channels and set of format-specific
     * parameters.
     *
     * @param encoding the encoding (name) of the new instance
     * @param clockRate the clock rate of the new instance
     * @param channels the number of channels of the new instance
     * @param formatParams the set of format-specific parameters of the new
     * instance
     * @return a new <tt>AudioMediaFormat</tt> instance with the specified
     * encoding (name), clock rate, number of channels and set of
     * format-specific parameters
     * @see MediaFormatFactory#createAudioMediaFormat(String, double, Map)
     */
    public AudioMediaFormat createAudioMediaFormat(
            String encoding,
            double clockRate,
            int channels,
            Map<String, String> formatParams)
    {
        AudioMediaFormat audioMediaFormat
            = createAudioMediaFormat(encoding, clockRate, channels);

        if ((audioMediaFormat != null)
                && (formatParams != null)
                && !formatParams.isEmpty())
        {
            Map<String, String> formatParameters
                = new HashMap<String, String>();

            formatParameters.putAll(audioMediaFormat.getFormatParameters());
            formatParameters.putAll(formatParams);

            audioMediaFormat
                = new AudioMediaFormatImpl(
                        ((AudioMediaFormatImpl) audioMediaFormat).getFormat(),
                        formatParameters);
        }
        return null;
    }

    private List<MediaFormat> getSupportedMediaFormats(
            String encoding,
            double clockRate)
    {
        EncodingConfiguration encodingConfiguration
            = NeomediaActivator
                .getMediaServiceImpl().getEncodingConfiguration();
        List<MediaFormat> supportedMediaFormats
            = getSupportedMediaFormats(
                encodingConfiguration
                    .getSupportedEncodings(MediaType.AUDIO),
                encoding,
                clockRate);

        if (supportedMediaFormats.isEmpty())
            supportedMediaFormats
                = getSupportedMediaFormats(
                    encodingConfiguration
                        .getSupportedEncodings(MediaType.VIDEO),
                    encoding,
                    clockRate);
        return supportedMediaFormats;
    }

    private List<MediaFormat> getSupportedMediaFormats(
            MediaFormat[] mediaFormats,
            String encoding,
            double clockRate)
    {
        List<MediaFormat> supportedMediaFormats = new ArrayList<MediaFormat>();

        for (MediaFormat mediaFormat : mediaFormats)
            if (mediaFormat.getEncoding().equals(encoding)
                    && ((CLOCK_RATE_NOT_SPECIFIED == clockRate)
                            || (mediaFormat.getClockRate() == clockRate)))
                supportedMediaFormats.add(mediaFormat);
        return supportedMediaFormats;
    }
}
