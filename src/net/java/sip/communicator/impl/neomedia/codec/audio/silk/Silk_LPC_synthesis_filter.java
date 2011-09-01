/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * even order AR filter.
 * Coefficients are in Q12
 *
 * @author Jing Dai
 */
public class Silk_LPC_synthesis_filter 
{
    /**
     * even order AR filter.
     * @param in excitation signal
     * @param A_Q12 AR coefficients [Order], between -8_Q0 and 8_Q0
     * @param Gain_Q26 gain
     * @param S state vector [Order]
     * @param out output signal
     * @param len signal length
     * @param Order filter order, must be even
     */
    static void SKP_Silk_LPC_synthesis_filter
    (
            short     []in,        /* I:   excitation signal */
            short     []A_Q12,     /* I:   AR coefficients [Order], between -8_Q0 and 8_Q0 */
            final int Gain_Q26,   /* I:   gain */
            int       []S,               /* I/O: state vector [Order] */
            short     []out,             /* O:   output signal */
            final int len,        /* I:   signal length */
            final int Order         /* I:   filter order, must be even */
    )
    {
        int   k, j, idx;
        int   Order_half = ( Order >> 1 );
        int SA, SB, out32_Q10, out32;

        /* Order must be even */
        Silk_typedef.SKP_assert( 2 * Order_half == Order );

        /* S[] values are in Q14 */
        for( k = 0; k < len; k++ ) {
            SA = S[ Order - 1 ];
            out32_Q10 = 0;
            for( j = 0; j < ( Order_half - 1 ); j++ ) {
                idx = Silk_macros.SKP_SMULBB( 2, j ) + 1;
                SB = S[ Order - 1 - idx ];
                S[ Order - 1 - idx ] = SA;
                out32_Q10 = Silk_macros.SKP_SMLAWB( out32_Q10, SA, A_Q12[ ( j << 1 ) ] );
                out32_Q10 = Silk_macros.SKP_SMLAWB( out32_Q10, SB, A_Q12[ ( j << 1 ) + 1 ] );
                SA = S[ Order - 2 - idx ];
                S[ Order - 2 - idx ] = SB;
            }

            /* unrolled loop: epilog */
            SB = S[ 0 ];
            S[ 0 ] = SA;
            out32_Q10 = Silk_macros.SKP_SMLAWB( out32_Q10, SA, A_Q12[ Order - 2 ] );
            out32_Q10 = Silk_macros.SKP_SMLAWB( out32_Q10, SB, A_Q12[ Order - 1 ] );
            /* apply gain to excitation signal and add to prediction */
            out32_Q10 = Silk_macros.SKP_ADD_SAT32( out32_Q10, Silk_macros.SKP_SMULWB( Gain_Q26, in[ k ] ) );

            /* scale to Q0 */
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32_Q10, 10 );

            /* saturate output */
            out[ k ] = ( short )Silk_SigProc_FIX.SKP_SAT16( out32 );

            /* move result into delay line */
            S[ Order - 1 ] = Silk_SigProc_FIX.SKP_LSHIFT_SAT32( out32_Q10, 4 );
        }
    }
}
