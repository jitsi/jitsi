/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Fourth order ARMA filter.
 * Internally operates as two biquad filters in sequence.
 * Coeffients are stored in a packed format:
 * { B1_Q14[1], B2_Q14[1], -A1_Q14[1], -A1_Q14[2], -A2_Q14[1], -A2_Q14[2], gain_Q16 }
 * where it is assumed that B*_Q14[0], B*_Q14[2], A*_Q14[0] are all 16384.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_private_ARMA4 
{
    /**
     * 
     * @param S State vector [ 4 ].
     * @param S_offset offset of valid data.
     * @param out Output signal.
     * @param out_offset  offset of valid data.
     * @param in Input signal.
     * @param in_offset offset of valid data.
     * @param Coef ARMA coefficients [ 7 ]. 
     * @param Coef_offset offset of valid data.
     * @param len Signal length.
     */
    static void SKP_Silk_resampler_private_ARMA4(
        int[]                        S,            /* I/O: State vector [ 4 ]                        */
        int S_offset,
        short[]                        out,        /* O:    Output signal                            */
        int out_offset,
        short[]                        in,            /* I:    Input signal                            */
        int in_offset,
        short[]                        Coef,        /* I:    ARMA coefficients [ 7 ]                 */
        int Coef_offset,
        int                            len            /* I:    Signal length                            */
    )
    {
        int k;
        int in_Q8, out1_Q8, out2_Q8, X;

        for( k = 0; k < len; k++ )
        {
            in_Q8  = (int)in[ in_offset+k ] << 8;

            /* Outputs of first and second biquad */
            out1_Q8 = in_Q8 + ( S[ S_offset ] << 2 );
            out2_Q8 = out1_Q8 + ( S[ S_offset+2 ] << 2 );

            /* Update states, which are stored in Q6. Coefficients are in Q14 here */
            X      = Silk_macros.SKP_SMLAWB( S[ S_offset+1 ], in_Q8,   Coef[ Coef_offset ] );
            S[ S_offset ] = Silk_macros.SKP_SMLAWB( X,      out1_Q8, Coef[ Coef_offset+2 ] );

            X      = Silk_macros.SKP_SMLAWB( S[ S_offset+3 ], out1_Q8, Coef[ Coef_offset+1 ] );
            S[ S_offset+2 ] = Silk_macros.SKP_SMLAWB( X,      out2_Q8, Coef[ Coef_offset+4 ] );

            S[ S_offset+1 ] = Silk_macros.SKP_SMLAWB( in_Q8 >> 2, out1_Q8, Coef[ Coef_offset+3 ] );
            S[ S_offset+3 ] = Silk_macros.SKP_SMLAWB( out1_Q8 >> 2 , out2_Q8, Coef[ Coef_offset+5 ] );

            /* Apply gain and store to output. The coefficient is in Q16 */
            out[ out_offset+k ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_macros.SKP_SMLAWB( 128, out2_Q8, Coef[ Coef_offset+6 ] ) >> 8  );
        }
    }
}
