/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Split signal into two decimated bands using first-order allpass filters.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_ana_filt_bank_1
{
    /* Coefficients for 2-band filter bank based on first-order allpass filters */
    // old
    static short[] A_fb1_20 = {  5394 << 1 };
    static short[] A_fb1_21 = { (short)(20623 << 1) };        /* wrap-around to negative number is intentional */

    /**
     * Split signal into two decimated bands using first-order allpass filters.
     * @param in Input signal [N].
     * @param in_offset offset of valid data.
     * @param S State vector [2].
     * @param S_offset offset of valid data.
     * @param outL Low band [N/2].
     * @param outL_offset offset of valid data.
     * @param outH High band [N/2].
     * @param outH_offset offset of valid data.
     * @param scratch Scratch memory [3*N/2].
     * @param N Number of input samples.
     */
    static void SKP_Silk_ana_filt_bank_1
    (
        short[]      in,        /* I:   Input signal [N]        */
        int in_offset,
        int[]            S,         /* I/O: State vector [2]        */
        int S_offset,
        short[]            outL,      /* O:   Low band [N/2]          */
        int outL_offset,
        short[]            outH,      /* O:   High band [N/2]         */
        int outH_offset,
        int[]            scratch,   /* I:   Scratch memory [3*N/2]  */   // todo: remove - no longer used
        final int      N           /* I:   Number of input samples */
    )
    {
        int      k, N2 = N >> 1;
        int    in32, X, Y, out_1, out_2;

        /* Internal variables and state are in Q10 format */
        for( k = 0; k < N2; k++ ) 
        {
            /* Convert to Q10 */
            in32 = in[ in_offset + 2 * k ] << 10;

            /* All-pass section for even input sample */
            Y      = in32 - S[ S_offset + 0 ];
            X      = Silk_macros.SKP_SMLAWB( Y, Y, A_fb1_21[ 0 ] );
            out_1  = S[ S_offset + 0 ] + X;
            S[ S_offset + 0 ] = in32 + X;

            /* Convert to Q10 */
            in32 = in[ in_offset + 2 * k + 1 ] << 10;

            /* All-pass section for odd input sample, and add to output of previous section */
            Y      = in32 - S[ S_offset + 1 ];
            X      = Silk_macros.SKP_SMULWB( Y, A_fb1_20[ 0 ] );
            out_2  = S[ S_offset + 1 ] + X;
            S[ S_offset + 1 ] = in32 + X;

            /* Add/subtract, convert back to int16 and store to output */
            outL[ outL_offset + k ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out_2 + out_1, 11 ) );
            outH[ outH_offset + k ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out_2 - out_1, 11 ) );
        }
    }
}
