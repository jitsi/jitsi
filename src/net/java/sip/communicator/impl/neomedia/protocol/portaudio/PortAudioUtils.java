/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol.portaudio;

import javax.media.*;

/**
 * Common used util methods concerning PortAudio.
 * @author Damian Minkov
 */
public class PortAudioUtils
{
    /**
     * The locator prefix used when creating or parsing <tt>MediaLocator</tt>s.
     */
    public static final String LOCATOR_PREFIX = "portaudio:#";

    /**
     * Extracts the device index from the locator.
     * @param locator the locator containing the device index.
     * @return the extracted device index.
     */
    public static int getDeviceIndexFromLocator(MediaLocator locator)
    {
        return Integer.parseInt(locator.toExternalForm().replace(
                LOCATOR_PREFIX, ""));
    }

    /**
     * Returns the PortAudio sample format.
     * @param sampleSizeInBits the size in bits.
     * @return the portaudio sampleformat.
     */
    public static long getPortAudioSampleFormat(int sampleSizeInBits)
    {
        switch(sampleSizeInBits)
        {
            case 8 : return PortAudio.SAMPLE_FORMAT_INT8;
            case 16 : return PortAudio.SAMPLE_FORMAT_INT16;
            case 24 : return PortAudio.SAMPLE_FORMAT_INT24;
            case 32 : return PortAudio.SAMPLE_FORMAT_INT32;
            default : return PortAudio.SAMPLE_FORMAT_INT16;
        }
    }
}
