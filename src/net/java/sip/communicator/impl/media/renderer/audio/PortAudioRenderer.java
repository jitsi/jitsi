/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.renderer.audio;

import javax.media.*;
import javax.media.format.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.media.protocol.portaudio.*;

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
     * Will be inited after the device is set.
     */
    private static AudioFormat audioFormat = null;

    /**
     * The supported input formats. The Inputformats are
     * changed after the device is set.
     */
    public static Format[] supportedInputFormats = new Format[]{};

    private Object [] controls = new Object[0];
    private AudioFormat inputFormat;

    private long stream = 0;

    boolean started = false;

    private WriterThread writerThread = null;
    private final Object bufferSync = new Object();
    private byte[] buffer = null;

    private static int deviceIndex = -1;

    private static int frameSize;

    /**
     * Lists the input formats supported by this Renderer.
     * @return  An array of Format objects that represent
     *          the input formats supported by this Renderer.
     */
    public Format[] getSupportedInputFormats()
    {
        return new Format[]{audioFormat};
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
    public void stop()
    {
        synchronized(bufferSync)
        {
            try
            {
                started = false;
                PortAudio.Pa_CloseStream(stream);
                writerThread = null;
            }
            catch (PortAudioException e)
            {
                logger.error("Closing portaudio stream failed", e);
            }
        }
    }

    /**
     * Processes the data and renders it
     * to the output device represented by this Renderer.
     * @param inputBuffer the input data.
     * @return BUFFER_PROCESSED_OK if the processing is successful.
     */
    public int process(Buffer inputBuffer)
    {
        synchronized(bufferSync)
        {
            byte[] buff = new byte[inputBuffer.getLength()];
                        System.arraycopy(
                            (byte[])inputBuffer.getData(),
                            inputBuffer.getOffset(),
                            buff,
                            0,
                            buff.length);

            buffer = buff;
            bufferSync.notifyAll();
        }

        if(writerThread == null)
        {
            writerThread = new WriterThread();
            writerThread.start();
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
                long streamParameters
                    = PortAudio.PaStreamParameters_new(
                            deviceIndex,
                            audioFormat.getChannels(),
                            PortAudio.SAMPLE_FORMAT_INT16);

                stream
                    = PortAudio.Pa_OpenStream(
                            0,
                            streamParameters,
                            audioFormat.getSampleRate(),
                            PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
                            PortAudio.STREAM_FLAGS_NO_FLAG,
                            null);
            }
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
        deviceIndex = PortAudioStream.getDeviceIndexFromLocator(locator);

        long device = PortAudio.Pa_GetDeviceInfo(deviceIndex);
        int outputChannels = 1;

        double defaultSampleRate =
            PortAudio.PaDeviceInfo_getDefaultSampleRate(device);

        audioFormat =
            new AudioFormat(
                    AudioFormat.LINEAR,
                      defaultSampleRate,
                      16,
                      outputChannels,
                      AudioFormat.LITTLE_ENDIAN,
                      AudioFormat.SIGNED,
                      16,
                      Format.NOT_SPECIFIED,
                      Format.byteArray);
        supportedInputFormats = new Format[]{audioFormat};

        frameSize
            = PortAudio.Pa_GetSampleSize(PortAudio.SAMPLE_FORMAT_INT16)
                * outputChannels;
    }

    /**
     * Writes data to the portaudio stream. If there is no data for
     * particular time write some silence so we wont hear some cracks in
     * the sound.
     */
    private class WriterThread
        extends Thread
    {

        public WriterThread()
        {
            setName("Portaudio Renderer");
        }

        public void run()
        {
            while(true)
            {
                try
                {
                    synchronized(bufferSync)
                    {
                        // put some silence if there is no data
                        if(buffer == null)
                        {
                            buffer = new byte[1024];
                        }

                        PortAudio.Pa_WriteStream(
                            stream, buffer, buffer.length/frameSize);

                        buffer = null;
                        bufferSync.wait(90);
                    }

                    if(!started)
                        return;
                }
                catch (Exception e)
                {
                    logger.error("writing to stream", e);
                }
            }
        }
    }
}
