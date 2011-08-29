/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.gsm;

import org.rubycoder.gsm.*;

/**
 * GSMEncoderUtil class
 *
 * @author Martin Harvan
 * @author Damian Minkov
 */
public class GSMEncoderUtil {

    private static GSMEncoder encoder = new GSMEncoder();
    /**
     * number of bytes in GSM frame
     */
    private static final int GSM_BYTES = 33;

    /**
     * number of PCM bytes needed to encode
     */
    private static final int PCM_BYTES = 320;

    /**
     * number of PCM ints needed to encode
     */
    private static final int PCM_INTS = 160;

    public static void gsmEncode(
            boolean bigEndian,
            byte[] data,
            int offset,
            int length,
            byte[] decoded)
    {
        for (int i = offset; i < length / PCM_BYTES; i++)
        {
            int[] input = new int[PCM_INTS];
            byte[] output = new byte[GSM_BYTES];

            for (int j = 0; j < PCM_INTS; j++) {
                int index = j << 1;

                input[j] = data[i * PCM_BYTES + index++];

                input[j] <<= 8;
                input[j] |= data[i * PCM_BYTES + index++] & 0xFF;
            }
            encoder.encode(output, input);
            System.arraycopy(output, 0, decoded, i * GSM_BYTES, GSM_BYTES);
        }
    }
}
