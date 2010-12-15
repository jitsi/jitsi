/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.audiolevel;

import net.java.sip.communicator.impl.neomedia.*;

/**
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class AudioLevelCalculator
{

    /**
     * The decrease percentage. The decrease cannot be done with more than this
     * value in percents.
     */
    private static final double DEC_LEVEL = 0.2;

    /**
     * The increase percentage. The increase cannot be done with more than this
     * value in percents.
     */
    private static final double INC_LEVEL = 0.4;

    /**
     * The maximum level we can get as a result after compute.
     */
    private static final int MAX_AUDIO_LEVEL = Short.MAX_VALUE;

    /**
     * The maximum sound pressure level which matches the maximum of the sound
     * meter.
     */
    private static final double MAX_SOUND_PRESSURE_LEVEL
        = 127 /* HUMAN TINNITUS (RINGING IN THE EARS) BEGINS */;

    /**
     * The minimum level we can get as a result after compute.
     */
    private static final int MIN_AUDIO_LEVEL = Short.MIN_VALUE;

    /**
     * Modifies a specific <tt>level</tt> value so that its multiple uses for
     * the purposes of a sound meter will result in a smoother, animation-like
     * display of its changes.
     *
     * @param level the level to animate
     * @param minLevel the minimum value of the range to which <tt>level</tt>
     * belongs
     * @param maxLevel the maximum value of the range to which <tt>level</tt>
     * belongs
     * @param lastLevel the last level which has been previously calculated
     * @return the value which represents <tt>level</tt> with animation taken
     * into account
     */
    private static int animateLevel(
            int level,
            int minLevel, int maxLevel, int lastLevel)
    {
        // we don't allow to quick level changes
        // the speed ot fthe change is controlled by
        int diff = lastLevel - level;

        if(diff >= 0)
        {
            int maxDiff = (int)(maxLevel*DEC_LEVEL);

            if(diff > maxDiff)
                level = lastLevel - maxDiff;
        }
        else
        {
            int maxDiff = (int)(maxLevel*INC_LEVEL);

            if(diff > maxDiff)
                level = lastLevel + maxDiff;
        }
        return level;
    }

    /**
     * Estimates the signal power and use the levelRatio to scale it to the
     * needed levels.
     *
     * @param samples the samples of the signal to calculate the signal power
     * level of
     * @param offset the offset that data starts.
     * @param length the length of the data
     * @param minLevel the minimum value of the result
     * @param maxLevel the maximum value of the result
     * @param lastLevel the last level we calculated.
     * @return the power of the signal in dB SWL.
     */
    public static int calculateSignalPowerLevel(
        byte[] samples, int offset, int length,
        int minLevel, int maxLevel, int lastLevel)
    {
        if(length == 0)
            return 0;

        int samplesNumber = length/2;
        int absoluteMeanSoundLevel = 0;
        // magic ratio which scales good visually our levels
        double levelRatio = MAX_AUDIO_LEVEL/(maxLevel - minLevel)/16;

        // Do the processing
        for (int i = 0; i < samplesNumber; i++)
        {
            int tempL = samples[offset++];
            int tempH = samples[offset++];
            int soundLevel = tempH << 8 | (tempL & 255);

            if (soundLevel > MAX_AUDIO_LEVEL)
                soundLevel = MAX_AUDIO_LEVEL;
            else if (soundLevel < MIN_AUDIO_LEVEL)
                soundLevel = MIN_AUDIO_LEVEL;

            absoluteMeanSoundLevel += Math.abs(soundLevel);
        }

        int result
            = (int)(absoluteMeanSoundLevel/samplesNumber/levelRatio);

        result = ensureLevelRange(result, minLevel, maxLevel);
        result = animateLevel(result, minLevel, maxLevel, lastLevel);
        return result;
    }

    /**
     * Calculates the sound pressure level of a signal with specific
     * <tt>samples</tt> and makes sure that it is expressed as a value in the
     * range between <tt>minLevel</tt> and <tt>maxLevel</tt>.
     *
     * @param samples the samples of the signal to calculate the sound pressure
     * level of
     * @param offset the offset in <tt>samples</tt> in which the samples start
     * @param length the length in bytes of the samples in <tt>samples<tt>
     * starting at <tt>offset</tt>
     * @param minLevel the minimum value of the level to be returned
     * @param maxLevel the maximum value of the level to be returned
     * @param lastLevel the last level which has been previously calculated
     * @return the sound pressure level of the specified signal as a value in
     * the range between <tt>minLevel</tt> and <tt>maxLevel</tt>
     */
    public static int calculateSoundPressureLevel(
        byte[] samples, int offset, int length,
        int minLevel, int maxLevel, int lastLevel)
    {
        double rms = 0;
        int sampleCount = 0;

        while (offset < length)
        {
            double sample = ArrayIOUtils.readShort(samples, offset);

            sample /= Short.MAX_VALUE;
            rms += sample * sample;
            sampleCount++;

            offset += 2;
        }
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        double db;

        if (rms > 0)
            db = 20 * Math.log10(rms / 0.00002);
        else
            db = -MAX_SOUND_PRESSURE_LEVEL;

        return ensureLevelRange((int) db, minLevel, maxLevel);
    }

    /**
     * Ensures that a specific <tt>level</tt> value is in a specific range
     * between <tt>minLevel</tt> and <tt>maxLevel</tt>.
     *
     * @param level the level to check
     * @param minLevel the minimum value of the specified range
     * @param maxLevel the maximum value of the specified range
     * @return <tt>level</tt> if its value is between <tt>minLevel</tt> and
     * <tt>maxLevel</tt>, <tt>minLevel</tt> if <tt>level</tt> has a value which
     * is less than <tt>minLevel</tt>, or <tt>maxLevel</tt> if <tt>level</tt>
     * has a value which is greater than <tt>maxLevel</tt>
     */
    private static int ensureLevelRange(int level, int minLevel, int maxLevel)
    {
        if (level < minLevel)
            return minLevel;
        else if(level > maxLevel)
            return maxLevel;
        else
            return level;
    }
}
