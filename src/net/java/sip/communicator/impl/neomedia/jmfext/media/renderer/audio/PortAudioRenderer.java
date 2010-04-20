/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.audio;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.streams.*;
import net.java.sip.communicator.util.*;

/**
 * Implements an audio <tt>Renderer</tt> which uses PortAudio.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class PortAudioRenderer
    extends ControlsAdapter
    implements Renderer
{
    /**
     * The <tt>Logger</tt> used by the <tt>PortAudioRenderer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PortAudioRenderer.class);

    /**
     * The human-readable name of the <tt>PortAudioRenderer</tt> JMF plug-in.
     */
    private static final String PLUGIN_NAME = "PortAudio Renderer";

    /**
     * The list of the standard sample rates supported by
     * <tt>PortAudioRenderer</tt>.
     */
    private static final double[] SUPPORTED_SAMPLE_RATES
        = new double[] { 8000, 16000, 22050, 44100, 48000 };

    /**
     * The PortAudio index of the device to be used by the
     * <tt>PortAudioRenderer</tt> instance which are to be opened.
     */
    private static int deviceIndex = -1;

    /**
     * The supported input formats. Changed when {@link #deviceIndex} is set.
     */
    public static Format[] supportedInputFormats = new Format[0];

    /**
     * The JMF <tt>Format</tt> in which this <tt>PortAudioRenderer</tt> is
     * currently configured to read the audio data to be rendered.
     */
    private AudioFormat inputFormat = null;

    /**
     * The output PortAudio stream represented by this instance.
     */
    private OutputPortAudioStream stream = null;

    /**
     *  Closes the plug-in.
     */
    public void close()
    {
        if (stream != null)
        {
            stop();

            try
            {
                stream.close();
            }
            catch (PortAudioException paex)
            {
                logger.error("Failed to close stream", paex);
            }
        }
    }

    /**
     * Gets the descriptive/human-readble name of this JMF plug-in.
     *
     * @return the descriptive/human-readable name of this JMF plug-in
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Gets the list of JMF <tt>Format</tt>s of audio data which this
     * <tt>Renderer</tt> is capable of rendering.
     *
     * @return an array of JMF <tt>Format</tt>s of audio data which this
     * <tt>Renderer</tt> is capable of rendering
     */
    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
    }

    /**
     * Opens the PortAudio device and output stream represented by this instance
     * which are to be used to render audio.
     *
     * @throws ResourceUnavailableException if the PortAudio device or output
     * stream cannot be created or opened
     */
    public void open()
        throws ResourceUnavailableException
    {
        if (stream == null)
        {
            try
            {
                stream
                    = PortAudioManager
                        .getInstance()
                            .getOutputStream(
                                deviceIndex,
                                inputFormat.getSampleRate(),
                                inputFormat.getChannels());
            }
            catch (PortAudioException paex)
            {
                throw new ResourceUnavailableException(paex.getMessage());
            }
        }
    }

    /**
     * Renders the audio data contained in a specific <tt>Buffer</tt> onto the
     * PortAudio device represented by this <tt>Renderer</tt>.
     *
     * @param buffer the <tt>Buffer</tt> which contains the audio data to be
     * rendered
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the specified <tt>buffer</tt> has
     * been successfully processed
     */
    public int process(Buffer buffer)
    {
        try
        {
            stream
                .write(
                    (byte[]) buffer.getData(),
                    buffer.getOffset(),
                    buffer.getLength());
        }
        catch (PortAudioException paex)
        {
            logger.error("Error writing to device", paex);
        }
        return BUFFER_PROCESSED_OK;
    }

    /**
     * Resets the state of the plug-in.
     */
    public void reset()
    {
    }

    /**
     * Sets the PortAudio index of the device to be used by the
     * <tt>PortAudioRenderer</tt> instances which are to be opened later on.
     * Changes the <tt>supportedInputFormats</tt> property of all
     * <tt>PortAudioRenderer</tt> instances.
     *
     * @param locator the <tt>MediaLocator</tt> specifying the PortAudio device
     * index
     */
    public static void setDevice(MediaLocator locator)
    {
        deviceIndex = PortAudioUtils.getDeviceIndexFromLocator(locator);

        int outputChannels = 1;

        supportedInputFormats = new Format[SUPPORTED_SAMPLE_RATES.length];
        for(int i = 0; i < SUPPORTED_SAMPLE_RATES.length; i++)
        {
            supportedInputFormats[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_SAMPLE_RATES[i],
                        16,
                        outputChannels,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        16,
                        Format.NOT_SPECIFIED,
                        Format.byteArray);
        }
    }

    /**
     * Sets the JMF <tt>Format</tt> of the audio data to be rendered by this
     * <tt>Renderer</tt>.
     *
     * @param format the JMF <tt>Format</tt> of the audio data to be redered by
     * this instance
     * @return <tt>null</tt> if the specified <tt>format</tt> is not compatible
     * with this <tt>Renderer</tt>; otherwise, the JMF <tt>Format</tt> which has
     * been successfully set
     */
    public Format setInputFormat(Format format)
    {
        if(!(format instanceof AudioFormat))
            return null;

        this.inputFormat = (AudioFormat) format;
        return this.inputFormat;
    }

    /**
     * Starts the rendering process. Any audio data available in the internal
     * resources associated with this <tt>PortAudioRenderer</tt> will begin
     * being rendered.
     */
    public void start()
    {
        try
        {
            stream.start();
        }
        catch (PortAudioException paex)
        {
            logger.error("Starting PortAudio stream failed", paex);
        }
    }

    /**
     * Stops the rendering process.
     */
    public void stop()
    {
        try
        {
            stream.stop();
        }
        catch (PortAudioException paex)
        {
            logger.error("Closing PortAudio stream failed", paex);
        }
    }
}
