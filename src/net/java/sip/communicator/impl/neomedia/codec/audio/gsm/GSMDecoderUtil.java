/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.gsm;

import org.rubycoder.gsm.*;


/**
 * GSMDecoderUtil class
 *
 * @author Martin Harvan
 * @author Damian Minkov
 */
public class GSMDecoderUtil
{
    private static GSMDecoder decoder = new GSMDecoder();
    private static final int GSM_BYTES = 33;
    private static final int PCM_INTS = 160;
    private static final int PCM_BYTES = 320;

    /**
     * Decode GSM data.
     *
     * @param bigEndian if the data are in big endian format
     * @param data the GSM data
     * @param offset offset
     * @param length length of the data
     * @param decoded decoded data array
     */
    public static void gsmDecode(boolean bigEndian,
                                 byte[] data,
                                 int offset,
                                 int length,
                                 byte[] decoded)
    {
        for (int i = 0; i < length / GSM_BYTES; i++)
        {
            int[] output = new int[PCM_INTS];
            byte[] input = new byte[GSM_BYTES];
            System.arraycopy(data, i * GSM_BYTES, input, 0, GSM_BYTES);
            try
            {
                decoder.decode(input, output);
            } catch (InvalidGSMFrameException e)
            {
                e.printStackTrace();
            }
            for (int j = 0; j < PCM_INTS; j++)
            {
                int index = j << 1;
                if (bigEndian)
                {
                    decoded[index + i * PCM_BYTES]
                            = (byte) ((output[j] & 0xff00) >> 8);
                    decoded[++index + (i * PCM_BYTES)]
                            = (byte) ((output[j] & 0x00ff));
                } else
                {
                    decoded[index + i * PCM_BYTES]
                            = (byte) ((output[j] & 0x00ff));
                    decoded[++index + (i * PCM_BYTES)]
                            = (byte) ((output[j] & 0xff00) >> 8);
                }
            }
        }
    }
}
