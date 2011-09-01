/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * first-order allpass filter.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_allpass_int_FLP 
{
    /**
     * first-order allpass filter.
     * @param in input signal [len].
     * @param in_offset offset of valid data.
     * @param S  I/O: state [1].
     * @param S_offset offset of valid data.
     * @param A coefficient (0 <= A < 1).
     * @param out output signal [len]. 
     * @param out_offset offset of valid data.
     * @param len number of samples.
     */
//TODO:float or double ???    
    static void SKP_Silk_allpass_int_FLP
    (
        float[]           in,        /* I:   input signal [len]          */
        int in_offset,
        float[]           S,         /* I/O: state [1]                   */
        int S_offset,
        float             A,         /* I:   coefficient (0 <= A < 1)    */
        float[]           out,       /* O:   output signal [len]         */
        int out_offset,
        final int         len        /* I:   number of samples           */
    )
    {
        float Y2, X2, S0;
        int k;

        S0 = S[ S_offset ];
        for ( k = len-1; k >= 0; k-- ) 
        {
            Y2        = in[in_offset] - S0;
            X2        = Y2 * A;
            out[out_offset++]  = S0 + X2;
            S0        = in[in_offset++] + X2;
        }
        S[ S_offset ] = S0;
    }
}
