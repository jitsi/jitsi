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
    /**
     * The supported audio formats by this effect.
     */
    private Format[] supportedAudioFormats;

    /**
     * Input Format
     */
    private AudioFormat inputFormat;

    /**
     * Output Format
     */
    private AudioFormat outputFormat;

    /**
     * The ratio we will scale the levels, to come to desired
     * boundaries.
     */
    private double levelRatio;

    /**
     * The maximum level we can get as a result after compute.
     */
    private static int MAX_SOUND_LEVEL = Short.MAX_VALUE;

    /**
     * The minimum level we can get as a result after compute.
     */
    private static int MIN_SOUND_LEVEL = Short.MIN_VALUE;

    /**
     * This is the last level we fired.
     */
    private int lastLevel = 0;

    /**
     * The maimum level that we can fire.
     */
    private int maxOutputLevel = 0;

    /**
     * The minimum level that we can fire.
     */
    private int minOutputLevel = 0;

    /**
     * The listener for the levels.
     */
    private SoundLevelIndicatorListener listener = null;

    /**
     * The dispatcher of the events, handle the calculation and the
     * evnent firing in different thread.
     */
    private SliEventDispatcher eventDispatcher = null;

    /**
     * The increase percentage. The increase cannot be done with more than this
     * value in percents.
     */
    private final static double INC_LEVEL = 0.4;

    /**
     * The decrease percentage. The decrease cannot be done with more than this
     * value in percents.
     */
    private final static double DEC_LEVEL = 0.2;

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
        this.maxOutputLevel = maxLevel;
        this.minOutputLevel = minLevel;

        // magic ratio which scales good visually our levels
        levelRatio = MAX_SOUND_LEVEL/(maxLevel - minLevel)/16;

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
     * Calculates the ration we will use.
     * @param minLevel
     * @param maxLevel
     * @return
     */
    private static int getLevelRatio(int minLevel, int maxLevel)
    {
        // magic ratio which scales good visually our levels
        return MAX_SOUND_LEVEL/(maxLevel - minLevel)/16;
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
        return new Format[]{
            new AudioFormat(
                AudioFormat.LINEAR,
                ((AudioFormat)input).getSampleRate(),
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
        System.arraycopy(
            inputBuffer.getData(), inputBuffer.getOffset(), b, 0, b.length);
        outputBuffer.setData(b);

        eventDispatcher.addData(b);

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

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Estimates the signal power and use the levelRatio to
     * scale it to the needed levels.
     * @param buff the buffer with the data.
     * @param offset the offset that data starts.
     * @param len the length of the data
     * @param maxOutLevel the maximum value of the result
     * @param minOutLevel the minimum value of the result
     * @param lastLevel the last level we calculated.
     * @return the power of the signal in dB SWL.
     */
    public static int calculateCurrentSignalPower(
        byte[] buff, int offset, int len, 
        int maxOutLevel, int minOutLevel, int lastLevel)
    {
        return calculateCurrentSignalPower(buff, offset, len,
            getLevelRatio(minOutLevel, maxOutLevel), 
            maxOutLevel, minOutLevel, lastLevel);
    }

    /**
     * Estimates the signal power and use the levelRatio to
     * scale it to the needed levels. 
     * @param buff the buffer with the data.
     * @param offset the offset that data starts.
     * @param len the length of the data
     * @param levelRatio the ratio for scaling to the needed levels
     * @param maxOutLevel the maximum value of the result
     * @param minOutLevel the minimum value of the result
     * @param lastLevel the last level we calculated.
     * @return the power of the signal in dB SWL.
     */
    public static int calculateCurrentSignalPower(
        byte[] buff, int offset, int len, double levelRatio,
        int maxOutLevel, int minOutLevel, int lastLevel)
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
        
        int result =
            (int)(absoluteMeanSoundLevel/samplesNumber/levelRatio);

        if(result > maxOutLevel)
            result = maxOutLevel;
        int result2 = result;
        // we don't allow to quick level changes
        // the speed ot fthe change is controlled by
        //
        int diff = lastLevel - result;
        if(diff >= 0)
        {
            int maxDiff = (int)(maxOutLevel*DEC_LEVEL);
            if(diff > maxDiff)
                result2 = lastLevel - maxDiff;
            else
                result2 = result;
        }
        else
        {
            int maxDiff = (int)(maxOutLevel*INC_LEVEL);
            if(diff > maxDiff)
                result2 = lastLevel + maxDiff;
            else
                result2 = result;
        }

        return result2;
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
        if(eventDispatcher == null)
            eventDispatcher = new SliEventDispatcher();

        new Thread(eventDispatcher).start();
    }

    /**
     * Closes this effect.
     */
    public void close()
    {
        synchronized(eventDispatcher)
        {
            eventDispatcher.stopped = true;
            eventDispatcher.notifyAll();
        }
        eventDispatcher = null;
    }

    /**
     * Resets its state.
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

    /**
     * We use different thread to compute and deliver sound levels
     * so we wont delay the media processing thread.
     */
    private class SliEventDispatcher
        implements Runnable
    {
        /**
         * start/stop indicator.
         */
        boolean stopped = true;

        /**
         * The data to process.
         */
        byte[] data = null;

        public void run()
        {
            stopped = false;
            while(!stopped)
            {
                byte[] dataToProcess = null;
                synchronized(this)
                {
                    if(data == null)
                        try {
                            wait();
                        } catch (InterruptedException ie) {}

                    dataToProcess = data;
                    data = null;
                }

                if(dataToProcess != null)
                {
                    int newLevel = calculateCurrentSignalPower(
                        dataToProcess, 0, dataToProcess.length,
                        levelRatio, maxOutputLevel, minOutputLevel, lastLevel);

                    if(newLevel != lastLevel)
                        listener.soundLevelChanged(newLevel);

                    lastLevel = newLevel;
                }
            }
        }

        /**
         * Adds data to be processed.
         * @param data
         */
        synchronized void addData(byte[] data)
        {
            this.data = data;
            notifyAll();
        }
    }
}
