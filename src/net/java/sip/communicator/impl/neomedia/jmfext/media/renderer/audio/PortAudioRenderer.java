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
 * PortAudio renderer.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class PortAudioRenderer
    extends ControlsAdapter
    implements Renderer
{
    /**
     * logger
     */
    private static final Logger logger =
        Logger.getLogger(PortAudioRenderer.class);

    /**
     * Name of the renderer.
     */
    private static final String name = "PortAudio Renderer";

    /**
     * The supported input formats. The input formats are
     * changed after the device is set.
     */
    public static Format[] supportedInputFormats = new Format[]{};

    /**
     * The standard supported sample rates.
     */
    private static double[] supportedSampleRates =
        new double[]{8000, 16000, 22050, 44100, 48000};

    /**
     * The current input format of the renderer.
     */
    private AudioFormat inputFormat;

    /**
     * PortAudio output stream currently in use.
     */
    private OutputPortAudioStream stream = null;

    /**
     * Is renderer started.
     */
    boolean started = false;

    /**
     * Index of the device we use and must use when creating stream.
     */
    private static int deviceIndex = -1;

    /**
     * Lists the input formats supported by this Renderer.
     * @return  An array of Format objects that represent
     *          the input formats supported by this Renderer.
     */
    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
    }

    /**
     * Sets the Format of the input data.
     * @param format the format to set.
     * @return The Format that was set.
     */
    public Format setInputFormat(Format format)
    {
        if(!(format instanceof AudioFormat))
            return null;

        this.inputFormat = (AudioFormat) format;

        return inputFormat;
    }

    /**
     * Initiates the rendering process.
     * When start is called, the renderer begins rendering
     * any data available in its internal buffers.
     */
    public void start()
    {
        try
        {
            stream.start();
        }
        catch (PortAudioException paex)
        {
            logger.error("Starting portaudio stream failed", paex);
        }
    }

    /**
     * Halts the rendering process.
     */
    public void stop()
    {
        try
        {
            stream.stop();
        }
        catch (PortAudioException paex)
        {
            logger.error("Closing portaudio stream failed", paex);
        }
    }

    /**
     * Processes the data and renders it
     * to the output device represented by this Renderer.
     * @param buffer the input data.
     * @return BUFFER_PROCESSED_OK if the processing is successful.
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
     * Returns the name of this plug-in.
     * @return a <tt>String</tt> which contains the descriptive name of this
     * plug-in.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Opens the device and stream that we will use to render data.
     * @throws  ResourceUnavailableException If required resources cannot
     *          be opened/created.
     */
    public void open()
        throws ResourceUnavailableException
    {
        if (stream == null)
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
            catch (PortAudioException e)
            {
                throw new ResourceUnavailableException(e.getMessage());
            }
    }

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
     * Resets the state of the plug-in.
     */
    public void reset()
    {
    }

    /**
     * Used to set the device index used by the renderer common for all
     * instances of it. Change the format corresponding the device which
     * will be used.
     * @param locator the locator containing the device index.
     */
    public static void setDevice(MediaLocator locator)
    {
        deviceIndex = PortAudioUtils.getDeviceIndexFromLocator(locator);

        int outputChannels = 1;

        supportedInputFormats = new Format[supportedSampleRates.length];

        for(int i = 0; i < supportedSampleRates.length; i++)
        {
            double sampleRate = supportedSampleRates[i];

            supportedInputFormats[i] =
                new AudioFormat(
                    AudioFormat.LINEAR,
                      sampleRate,
                      16,
                      outputChannels,
                      AudioFormat.LITTLE_ENDIAN,
                      AudioFormat.SIGNED,
                      16,
                      Format.NOT_SPECIFIED,
                      Format.byteArray);
        }
    }
}
