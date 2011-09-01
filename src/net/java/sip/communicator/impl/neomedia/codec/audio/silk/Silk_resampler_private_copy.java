/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Simple opy.
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_private_copy 
{
    /**
     * Simple copy.
     * @param SS Resampler state (unused).
     * @param out Output signal
     * @param out_offset offset of valid data.
     * @param in Input signal
     * @param in_offset offset of valid data.
     * @param inLen Number of input samples
     */
    static void SKP_Silk_resampler_private_copy(
        Object                        SS,            /* I/O: Resampler state (unused)                */
        short[]                        out,        /* O:    Output signal                             */
        int out_offset,
        short[]                        in,            /* I:    Input signal                            */
        int in_offset,
        int                            inLen       /* I:    Number of input samples                    */
    )
    {
        for(int k=0; k<inLen; k++)
            out[out_offset+k] = in[in_offset+k];
    }
}
