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
 *
 * @author Damian Minkov
 */
public class PortAudioRenderer
    implements  Renderer
{
    private static final Logger logger =
        Logger.getLogger(PortAudioRenderer.class);

    private static final String name = "PortAudio Renderer";
    public static Format[] supportedInputFormats = new Format[]
    {
         new AudioFormat(
                    AudioFormat.LINEAR,
                      44100,
                      16,
                      1,
                      AudioFormat.LITTLE_ENDIAN,
                      AudioFormat.SIGNED,
                      16,
                      Format.NOT_SPECIFIED,
                      Format.byteArray)
    };
    protected Object [] controls = new Object[0];
    private AudioFormat inputFormat;

    private long stream = 0;

    public PortAudioRenderer()
    {
        try
        {
            PortAudio.initialize();
        }
        catch (PortAudioException e)
        {
            logger.error("Cannot Initialize portaudio", e);
        }
    }

    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
    }

    public Format setInputFormat(Format format)
    {
        if(!(format instanceof AudioFormat))
        {
            return null;
        }

        this.inputFormat = (AudioFormat) format;

        return inputFormat;
    }

    public void start()
    {
        try
        {            
            PortAudio.Pa_StartStream(stream);
        }
        catch (PortAudioException e)
        {
            logger.error("Starting portaudio stream failed", e);
        }
    }

    public void stop()
    {
        try
        {
            PortAudio.Pa_CloseStream(stream);
        }
        catch (PortAudioException e)
        {
            logger.error("Closing portaudio stream failed", e);
        }
    }

    public int process(Buffer inputBuffer)
    {
        try
        {
            byte[] buff = new byte[inputBuffer.getLength()];
            System.arraycopy(
                (byte[])inputBuffer.getData(),
                inputBuffer.getOffset(),
                buff,
                0,
                buff.length);
            PortAudio.Pa_WriteStream(stream, buff, buff.length/2);
        }
        catch (PortAudioException e)
        {
            logger.error("Error write to device!", e);
        }

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Returns the name of the pluging.
     */
    public String getName()
    {
        return name;
    }

    public void open()
        throws ResourceUnavailableException
    {
        try
        {
            if (stream == 0)
            {
                int deviceCount = PortAudio.Pa_GetDeviceCount();
                int deviceIndex = 0;

                for (; deviceIndex < deviceCount; deviceIndex++)
                {
                    long deviceInfo = PortAudio.Pa_GetDeviceInfo(deviceIndex);

                    if ((PortAudio.PaDeviceInfo_getMaxOutputChannels(deviceInfo) == 2)
                        && (PortAudio.PaDeviceInfo_getMaxInputChannels(deviceInfo) == 2)
                        && PortAudio.PaDeviceInfo_getName(deviceInfo)
                                    .contains("Analog"))
                        break;
                }

                long streamParameters
                    = PortAudio.PaStreamParameters_new(
                            deviceIndex,
                            1,
                            PortAudio.SAMPLE_FORMAT_INT16);

                stream
                    = PortAudio.Pa_OpenStream(
                            0,
                            streamParameters,
                            44100,
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
     *
     */
    public void close()
    {
    }

    /**
     * 
     */
    public void reset()
    {
    }

    /**
     * Gives control information to the caller
     *
     */
    public Object[] getControls()
    {
        return controls;
    }

    /**
     * Return required control from the Control[] array
     * if exists, that is
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
}
