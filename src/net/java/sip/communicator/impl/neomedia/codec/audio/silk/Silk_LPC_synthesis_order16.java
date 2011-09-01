/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 16th order AR filter.
 * Coefficients are in Q12.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_LPC_synthesis_order16
{
    /**
     * 16th order AR filter
     * @param in excitation signal
     * @param A_Q12 AR coefficients [16], between -8_Q0 and 8_Q0
     * @param Gain_Q26 gain
     * @param S state vector [16]
     * @param out output signal
     * @param len signal length, must be multiple of 16
     */
    static void SKP_Silk_LPC_synthesis_order16
    (
            short     []in,          /* I:   excitation signal */
            short     []A_Q12,       /* I:   AR coefficients [16], between -8_Q0 and 8_Q0 */
            final int Gain_Q26,     /* I:   gain */
            int       []S,                 /* I/O: state vector [16] */
            short     []out,               /* O:   output signal */
            final int len           /* I:   signal length, must be multiple of 16 */
    )
    {
        int   k;
        int SA, SB, out32_Q10, out32;
        for( k = 0; k < len; k++ ) {
            /* unrolled loop: prolog */
            /* multiply-add two prediction coefficients per iteration */
            SA = S[ 15 ];
            SB = S[ 14 ];
            S[ 14 ] = SA;
            out32_Q10 = Silk_macros.SKP_SMULWB(SA, A_Q12[ 0 ]);
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 1 ] );
            SA = S[ 13 ];
            S[ 13 ] = SB;

            /* unrolled loop: main loop */
            SB = S[ 12 ];
            S[ 12 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 2 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 3 ] );
            SA = S[ 11 ];
            S[ 11 ] = SB;

            SB = S[ 10 ];
            S[ 10 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 4 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 5 ] );
            
            SA = S[ 9 ];
            S[ 9 ] = SB;

            SB = S[ 8 ];
            S[ 8 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 6 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 7 ] );
            SA = S[ 7 ];
            S[ 7 ] = SB;

            SB = S[ 6 ];
            S[ 6 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 8 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 9 ] );
            
            SA = S[ 5 ];
            S[ 5 ] = SB;

            SB = S[ 4 ];
            S[ 4 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 10 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 11 ] );
            
            SA = S[ 3 ];
            S[ 3 ] = SB;

            SB = S[ 2 ];
            S[ 2 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 12 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 13 ] );
            SA = S[ 1 ];
            S[ 1 ] = SB;

            /* unrolled loop: epilog */
            SB = S[ 0 ];
            S[ 0 ] = SA;
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SA, A_Q12[ 14 ] );
            out32_Q10 = Silk_SigProc_FIX.SKP_SMLAWB_ovflw( out32_Q10, SB, A_Q12[ 15 ] );


            /* unrolled loop: end */
            /* apply gain to excitation signal and add to prediction */
            out32_Q10 = Silk_macros.SKP_ADD_SAT32( out32_Q10, Silk_macros.SKP_SMULWB( Gain_Q26, in[ k ] ) );

            /* scale to Q0 */
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32_Q10, 10 );

            /* saturate output */
            out[ k ] = ( short )Silk_SigProc_FIX.SKP_SAT16( out32 );

            /* move result into delay line */
            S[ 15 ] = Silk_SigProc_FIX.SKP_LSHIFT_SAT32( out32_Q10, 4 );
        }
    }
}
