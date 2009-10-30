/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.renderer.audio;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.protocol.portaudio.*;
import net.java.sip.communicator.util.*;

/**
 * Portaudio renderer.
 *
 * @author Damian Minkov
 */
public class PortAudioRenderer
    implements  Renderer
{
    private static final Logger logger =
        Logger.getLogger(PortAudioRenderer.class);

    private static final String name = "PortAudio Renderer";

    /**
     * The supported input formats. The Inputformats are
     * changed after the device is set.
     */
    public static Format[] supportedInputFormats = new Format[]{};

    /**
     * The standart supported sample rates.
     */
    private static double[] supportedSampleRates =
        new double[]{8000, 16000, 22050, 44100, 48000};

    private Object [] controls = new Object[0];
    private AudioFormat inputFormat;

    private long stream = 0;

    boolean started = false;

    private static int deviceIndex = -1;

    private static int frameSize;

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
        {
            return null;
        }

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
            PortAudio.Pa_StartStream(stream);
            started = true;
        }
        catch (PortAudioException e)
        {
            logger.error("Starting portaudio stream failed", e);
        }
    }

    /**
     * Halts the rendering process.
     */
    public synchronized void stop()
    {
        try
        {
            started = false;
            PortAudio.Pa_CloseStream(stream);
        }
        catch (PortAudioException e)
        {
            logger.error("Closing portaudio stream failed", e);
        }
    }

    /**
     * Processes the data and renders it
     * to the output device represented by this Renderer.
     * @param inputBuffer the input data.
     * @return BUFFER_PROCESSED_OK if the processing is successful.
     */
    public synchronized int process(Buffer inputBuffer)
    {
        byte[] buff = new byte[inputBuffer.getLength()];
        System.arraycopy(
            (byte[])inputBuffer.getData(),
            inputBuffer.getOffset(),
            buff,
            0,
            buff.length);

        try
        {
            PortAudio.Pa_WriteStream(
                        stream, buff, buff.length/frameSize);
        }
        catch (PortAudioException e)
        {
            logger.error("Error writing to device", e);
        }

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Returns the name of the pluging.
     * @return A String that contains the descriptive name of the plug-in.
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
        try
        {
            if (stream == 0)
            {
                // lets try three times to open the device
                // and wait 250 ms. between retries.
                stream = openStreamRetring(3, 250);
            }
        }
        catch (PortAudioException e)
        {
            throw new ResourceUnavailableException(e.getMessage());
        }
    }

    /**
     * When opening the device notifications maybe running so we will retry
     * opening the device.
     * @param numOfRetries the number of tries to open the requested device.
     * @param interval the interval to wait between retries in miliseconds.
     * @return the stream pointer.
     * @throws PortAudioException the exception to be thrown if device
     *         fail to open and after the last try.
     */
    private long openStreamRetring(int numOfRetries, int interval)
        throws PortAudioException
    {
        for(int i = 0; i < numOfRetries; i++)
        {
            try
            {
                 long streamParameters
                     = PortAudio.PaStreamParameters_new(
                             deviceIndex,
                             inputFormat.getChannels(),
                             PortAudio.SAMPLE_FORMAT_INT16);

                return PortAudio.Pa_OpenStream(
                             0,
                             streamParameters,
                             inputFormat.getSampleRate(),
                             PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
                             PortAudio.STREAM_FLAGS_CLIP_OFF
                                | PortAudio.STREAM_FLAGS_DITHER_OFF,
                             null);
             }
            catch (PortAudioException e)
            {
                if(i == numOfRetries - 1)
                    throw e;
                else
                    try
                    {Thread.sleep(interval);}catch(InterruptedException ex){}
            }
         }

        return 0;
     }

    /**
     *  Closes the plug-in.
     */
    public void close()
    {
    }

    /**
     * Resets the state of the plug-in.
     */
    public void reset()
    {
    }

    /**
     * Gives control information to the caller
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        return controls;
    }

    /**
     * Return required control from the Control[] array
     * if exists.
     * @param controlType the control we are interested in.
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> cls = Class.forName(controlType);
            Object cs[] = getControls();
            for(int i = 0; i < cs.length; i++)
            {
                if(cls.isInstance(cs[i]))
                {
                    return cs[i];
                }
            }
            return null;

        }
        catch (Exception e)
        {
            return null;
        }
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

        frameSize
            = PortAudio.Pa_GetSampleSize(PortAudio.SAMPLE_FORMAT_INT16)
                * outputChannels;
    }
}
