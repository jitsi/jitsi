/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

import net.java.sip.communicator.util.*;

/**
 * Manages PortAudio stream creation and setting necessary properties when using
 * them.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class PortAudioManager
{
    /**
     * Echo cancel enabled by default.
     */
    private static boolean enabledEchoCancel = true;

    /**
     * Denoise enabled by default.
     */
    private static boolean enabledDeNoise = true;

    /**
     * The number of milliseconds of echo to cancel. The default value is 256
     * ms.
     */
    private static long filterLengthInMillis = 256;

    /**
     * The default value for suggested latency used to open devices.
     * The suggested latency is later calculated dependent the OS we use.
     * If its not calculated this is the default value.
     * Currently -1 (unspecified).
     */
    private static double suggestedLatency = PortAudio.LATENCY_UNSPECIFIED;

    /**
     * Enables or disables echo cancel.
     *
     * @param enabled should we enable or disable echo cancellation
     * @param filterLengthInMillis the number of milliseconds of echo to cancel.
     * Should generally correspond to 100-500 ms.
     */
    public static void setEchoCancel(boolean enabled, long filterLengthInMillis)
    {
        PortAudioManager.enabledEchoCancel = enabled;
        PortAudioManager.filterLengthInMillis = filterLengthInMillis;
    }

    /**
     * Enables or disables noise suppression.
     * @param enabled should we enable or disable noise suppression.
     */
    public static void setDeNoise(boolean enabled)
    {
        PortAudioManager.enabledDeNoise = enabled;
    }

    /**
     * Returns the default values of the latency to be used when
     * opening new streams.
     * @return the latency.
     */
    public static double getSuggestedLatency()
    {
        if(suggestedLatency != PortAudio.LATENCY_UNSPECIFIED)
            return suggestedLatency;

        if (OSUtils.IS_MAC || OSUtils.IS_LINUX)
            return PortAudio.LATENCY_HIGH;
        else if (OSUtils.IS_WINDOWS)
            return 0.1d;
        else
            return PortAudio.LATENCY_UNSPECIFIED;
    }

    /**
     * Changes the suggested latency.
     * @param aSuggestedLatency the suggestedLatency to set.
     */
    public static void setSuggestedLatency(double aSuggestedLatency)
    {
        suggestedLatency = aSuggestedLatency;
    }

    /**
     * Is echo cancel enabled.
     * @return true if echo cancel is enabled, false otherwise.
     */
    public static boolean isEnabledEchoCancel()
    {
        return enabledEchoCancel;
    }

    /**
     * Is noise reduction enabled.
     * @return true if noise reduction is enabled, false otherwise.
     */
    public static boolean isEnabledDeNoise()
    {
        return enabledDeNoise;
    }

    /**
     * Gets the number of milliseconds of echo to cancel.
     *
     * @return the number of milliseconds of echo to cancel
     */
    public static long getFilterLengthInMillis()
    {
        return filterLengthInMillis;
    }
}
