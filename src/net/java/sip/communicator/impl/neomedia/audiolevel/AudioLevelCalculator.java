/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.audiolevel;

/**
 * @author Damian Minkov
 */
public class AudioLevelCalculator
{
    /**
     * The maximum level we can get as a result after compute.
     */
    private static int MAX_AUDIO_LEVEL = Short.MAX_VALUE;

    /**
     * The minimum level we can get as a result after compute.
     */
    private static int MIN_AUDIO_LEVEL = Short.MIN_VALUE;

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
     * Calculates the ratio we will use.
     *
     * @param minLevel the minimum level
     * @param maxLevel the maximum level
     *
     * @return a ratio that could be used to fine tune levels so that they
     * render in an user friendly manner.
     */
    private static int calculateLevelRatio(int minLevel, int maxLevel)
    {
        // magic ratio which scales good visually our levels
        return MAX_AUDIO_LEVEL/(maxLevel - minLevel)/16;

    }

    /**
     * Estimates the signal power and use the levelRatio to
     * scale it to the needed levels.
     *
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
        if(len == 0)
            return 0;

        int samplesNumber = len/2;
        int absoluteMeanSoundLevel = 0;
        double levelRatio = calculateLevelRatio(minOutLevel, maxOutLevel);
        // Do the processing
        for (int i = 0; i < samplesNumber; i++)
        {
            int tempL = buff[offset++];
            int tempH = buff[offset++];
            int soundLevel = tempH << 8 | (tempL & 255);

            if (soundLevel > MAX_AUDIO_LEVEL)
            {
                soundLevel = MAX_AUDIO_LEVEL;
            } else if (soundLevel < MIN_AUDIO_LEVEL) {
                soundLevel = MIN_AUDIO_LEVEL;
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
}
