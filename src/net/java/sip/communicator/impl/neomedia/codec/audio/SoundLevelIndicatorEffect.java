/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio;

import javax.media.*;
import javax.media.format.*;

/**
 * An effect that calculates the power of the signal and fire an event
 * with the current level.
 *
 * @author Damian Minkov
 */
public class SoundLevelIndicatorEffect
    implements Effect
{
    private Format[] supportedAudioFormats;

    /**
     * Input Format
     */
    private AudioFormat inputFormat;

    /**
     * Output Format
     */
    private AudioFormat outputFormat;

    private double levelRatio;

    private static int MAX_SOUND_LEVEL = Short.MAX_VALUE;
    private static int MIN_SOUND_LEVEL = Short.MIN_VALUE;

    private int lastLevel = 0;

    private SoundLevelIndicatorListener listener = null;

    /**
     * The minimum and maximum values of the scale
     * @param minLevel min level.
     * @param maxLevel max lavel.
     * @param listener the listener of the sound level changes.
     */
    public SoundLevelIndicatorEffect(int minLevel, int maxLevel,
        SoundLevelIndicatorListener listener)
    {
        this.listener = listener;

        levelRatio = MAX_SOUND_LEVEL/(maxLevel - minLevel);

        supportedAudioFormats = new Format[]{
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED,
                16,
                Format.NOT_SPECIFIED,
                Format.byteArray)
            };
    }

    /**
     * Lists all of the input formats that this codec accepts.
     * @return An array that contains the supported input <code>Formats</code>.
     */
    public Format[] getSupportedInputFormats()
    {
        return supportedAudioFormats;
    }

    /**
     * Lists the output formats that this codec can generate.
     * @param input The <code>Format</code> of the data to be used
     *        as input to the plug-in.
     * @return An array that contains the supported output <code>Formats</code>.
     */
    public Format[] getSupportedOutputFormats(Format input)
    {
        return supportedAudioFormats;
    }

    /**
     * Sets the format of the data to be input to this codec.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set.
     */
    public Format setInputFormat(Format format)
    {
        this.inputFormat = (AudioFormat)format;
        return inputFormat;
    }

    /**
     * Sets the format for the data this codec outputs.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set.
     */
    public Format setOutputFormat(Format format)
    {
        this.outputFormat = (AudioFormat)format;
        return outputFormat;
    }

    /**
     * Performs the media processing defined by this codec.
     * @param inputBuffer The <code>Buffer</code> that contains the media data
     *        to be processed.
     * @param outputBuffer The <code>Buffer</code> in which to store
     *        the processed media data.
     * @return <CODE>BUFFER_PROCESSED_OK</CODE> if the processing is successful.
     *         @see PlugIn
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        byte[] b = new byte[inputBuffer.getLength()];
        System.arraycopy(inputBuffer.getData(), inputBuffer.getOffset(), b, 0, b.length);
        outputBuffer.setData(b);

        outputBuffer.setData(b);
        outputBuffer.setFormat(inputBuffer.getFormat());
        outputBuffer.setLength(inputBuffer.getLength());
        outputBuffer.setOffset(inputBuffer.getOffset());
        outputBuffer.setHeader(inputBuffer.getHeader());
        outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
        outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        outputBuffer.setFlags(inputBuffer.getFlags());
        outputBuffer.setDiscard(inputBuffer.isDiscard());
        outputBuffer.setEOM(inputBuffer.isEOM());
        outputBuffer.setDuration(inputBuffer.getDuration());

        int newLevel = calculateCurrentSignalPower(
            b, 0, b.length, levelRatio);

        if(newLevel != lastLevel)
            listener.soundLevelChanged(newLevel);

        lastLevel = newLevel;

        return BUFFER_PROCESSED_OK;
    }


    /**
     * Estimates the signal power and use the levelRatio to
     * scale it to the needed levels.
     * @param buff the buffer with the data.
     * @param offset the offset that data starts.
     * @param len the length of the data
     * @param levelRatio the ratio for scaling to the needed levels
     * @return the power of the signal in dB SWL.
     */
    public static int calculateCurrentSignalPower(
        byte[] buff, int offset, int len, double levelRatio)
    {
        if(len == 0)
            return 0;

        int samplesNumber = len/2;
        int absoluteMeanSoundLevel = 0;
        // Do the processing
        for (int i = 0; i < samplesNumber; i++)
        {
            int tempL = buff[offset++];
            int tempH = buff[offset++];
            int soundLevel = tempH << 8 | (tempL & 255);

            if (soundLevel > MAX_SOUND_LEVEL)
            {
                soundLevel = MAX_SOUND_LEVEL;
            } else if (soundLevel < MIN_SOUND_LEVEL) {
                soundLevel = MIN_SOUND_LEVEL;
            }

            absoluteMeanSoundLevel += Math.abs(soundLevel);
        }

        return (int)(absoluteMeanSoundLevel/samplesNumber/levelRatio);
    }

    /**
     * Gets the name of this plug-in as a human-readable string.
     * @return A <code>String</code> that contains the descriptive name of the
     * plug-in.
     */
    public String getName()
    {
        return "SoundLevelIndicator Effect";
    }

    /**
     * Opens this effect.
     * @throws ResourceUnavailableException If all of the required resources
     * cannot be acquired.
     */
    public void open()
        throws ResourceUnavailableException
    {
    }

    /**
     * Closes this effect.
     */
    public void close()
    {
    }

    /**
     * Restes its state.
     */
    public void reset()
    {
    }

    /**
     * Obtain the collection of objects that
     * control the object that implements this interface.
     * <p>
     *
     * If no controls are supported, a zero length
     * array is returned.
     *
     * @return the collection of object controls
     */
    public Object[] getControls()
    {
        return new Control[0];
    }

    /**
     * Obtain the object that implements the specified
     * <code>Class</code> or <code>Interface</code>
     * The full class or interface name must be used.
     * <p>
     *
     * If the control is not supported then <code>null</code>
     * is returned.
     *
     * @param controlType the control type to return.
     * @return the object that implements the control,
     * or <code>null</code>.
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
     * Lister for the changes in the sound level.
     */
    public static interface SoundLevelIndicatorListener
    {
        /**
         * Called when the sound level is changing.
         * @param level the new level.
         */
        public void soundLevelChanged(int level);
    }
}
