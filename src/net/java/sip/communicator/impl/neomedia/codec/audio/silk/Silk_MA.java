/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Variable order MA filter.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_MA 
{
    /**
     * Variable order MA filter.
     * @param in input signal.
     * @param in_offset offset of valid data.
     * @param B MA coefficients, Q13 [order+1].
     * @param S state vector [order].
     * @param out output signal.
     * @param out_offset offset of valid data.
     * @param len signal length.
     * @param order filter order.
     */
    static void SKP_Silk_MA(
            short[]    in,            /* I:   input signal                                */
            int        in_offset,
            short[]    B,             /* I:   MA coefficients, Q13 [order+1]              */
            int[]      S,             /* I/O: state vector [order]                        */
            short[]    out,           /* O:   output signal                               */
            int        out_offset,
            final int  len,            /* I:   signal length                               */
            final int  order           /* I:   filter order                                */
    )
    {
        int   k, d, in16;
        int out32;
        
        for( k = 0; k < len; k++ ) {
            in16 = in[ in_offset + k ];
            out32 = Silk_macros.SKP_SMLABB(S[0], in16, B[ 0 ]);
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32, 13 );
            
            for( d = 1; d < order; d++ ) {
                S[ d - 1 ] = Silk_macros.SKP_SMLABB( S[ d ], in16, B[ d ] );
            }
            S[ order - 1 ] = Silk_macros.SKP_SMULBB( in16, B[ order ] );

            /* Limit */
            out[ out_offset + k ] = (short)Silk_SigProc_FIX.SKP_SAT16( out32 );
        }
    }
    
    /**
     * Variable order MA prediction error filter.
     * @param in Input signal.
     * @param in_offset offset of valid data.
     * @param B MA prediction coefficients, Q12 [order].
     * @param B_offset 
     * @param S State vector [order].
     * @param out Output signal.
     * @param out_offset offset of valid data.
     * @param len Signal length.
     * @param order Filter order.
     */
    static void SKP_Silk_MA_Prediction(
            short[]      in,            /* I:   Input signal                                */
            int          in_offset,
            short[]      B,             /* I:   MA prediction coefficients, Q12 [order]     */
            int          B_offset,
            int[]        S,             /* I/O: State vector [order]                        */
            short[]      out,           /* O:   Output signal                               */
            int          out_offset,
            final int    len,            /* I:   Signal length                               */
            final int    order           /* I:   Filter order                                */
        )
    {
        int   k, d, in16;
        int out32;

        for( k = 0; k < len; k++ ) {
            in16 = in[ in_offset + k ];
            out32 = ( in16 << 12 ) - S[ 0 ];
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32, 12 );
            
            for( d = 0; d < order - 1; d++ ) {
                S[ d ] = Silk_SigProc_FIX.SKP_SMLABB_ovflw( S[ d + 1 ], in16, B[ B_offset + d ] );
            }
            S[ order - 1 ] = Silk_macros.SKP_SMULBB( in16, B[ B_offset + order - 1 ] );

            /* Limit */
            out[ out_offset + k ] = (short)Silk_SigProc_FIX.SKP_SAT16( out32 );
        }
    }
    
    /**
     * 
     * @param in input signal.
     * @param in_offset offset of valid data.
     * @param B MA prediction coefficients, Q13 [order].
     * @param S state vector [order].
     * @param out output signal.
     * @param out_offset offset of valid data.
     * @param len signal length.
     * @param order filter order.
     */
    static void SKP_Silk_MA_Prediction_Q13(
            short      []in,            /* I:   input signal                                */
            int          in_offset,
            short      []B,             /* I:   MA prediction coefficients, Q13 [order]     */
            int        []S,             /* I/O: state vector [order]                        */
            short      []out,           /* O:   output signal                               */
            int          out_offset,
            int          len,            /* I:   signal length                               */
            int          order           /* I:   filter order                                */
        )
    {
        int   k, d, in16;
        int out32;
        for( k = 0; k < len; k++ ) {
            in16 = in[ in_offset + k ];
            out32 = ( in16 << 13 ) - S[ 0 ];
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32, 13 );
            
            for( d = 0; d < order - 1; d++ ) {
                S[ d ] = Silk_macros.SKP_SMLABB( S[ d + 1 ], in16, B[ d ] );
            }
            S[ order - 1 ] = Silk_macros.SKP_SMULBB( in16, B[ order - 1 ] );

            /* Limit */
            out[ out_offset + k ] = ( short )Silk_SigProc_FIX.SKP_SAT16( out32 );
        }
    }
    
    /**
     * 
     * @param in Input signal.
     * @param in_offset offset of valid data.
     * @param B MA prediction coefficients, Q12 [order].
     * @param S State vector [order].
     * @param out Output signal.
     * @param out_offset offset of valid data.
     * @param len Signal length.
     * @param Order Filter order.
     */
    static void SKP_Silk_LPC_analysis_filter(
            short      []in,            /* I:   Input signal                                */
            int        in_offset,
            short      []B,             /* I:   MA prediction coefficients, Q12 [order]     */
            short      []S,             /* I/O: State vector [order]                        */
            short      []out,           /* O:   Output signal                               */
            int        out_offset,
            final int  len,            /* I:   Signal length                               */
            final int  Order           /* I:   Filter order                                */
        )
    {
        int   k, j, idx, Order_half = ( Order >> 1 );
        int out32_Q12, out32;
        short SA, SB;
        /* Order must be even */
        Silk_typedef.SKP_assert( 2 * Order_half == Order );

        /* S[] values are in Q0 */
        for( k = 0; k < len; k++ ) {
            SA = S[ 0 ];
            out32_Q12 = 0;
            for( j = 0; j < ( Order_half - 1 ); j++ ) {
                idx = Silk_macros.SKP_SMULBB( 2, j ) + 1;
                /* Multiply-add two prediction coefficients for each loop */
                SB = S[ idx ];
                S[ idx ] = SA;
                out32_Q12 = Silk_macros.SKP_SMLABB( out32_Q12, SA, B[ idx - 1 ] );
                out32_Q12 = Silk_macros.SKP_SMLABB( out32_Q12, SB, B[ idx ] );
                SA = S[ idx + 1 ];
                S[ idx + 1 ] = SB;
            }

            /* Unrolled loop: epilog */
            SB = S[ Order - 1 ];
            S[ Order - 1 ] = SA;
            out32_Q12 = Silk_macros.SKP_SMLABB( out32_Q12, SA, B[ Order - 2 ] );
            out32_Q12 = Silk_macros.SKP_SMLABB( out32_Q12, SB, B[ Order - 1 ] );

            /* Subtract prediction */
            out32_Q12 = Silk_macros.SKP_SUB_SAT32( ( (int)in[ in_offset + k ] << 12 ), out32_Q12 );

            /* Scale to Q0 */
            out32 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32_Q12, 12 );

            /* Saturate output */
            out[ out_offset + k ] = ( short )Silk_SigProc_FIX.SKP_SAT16( out32 );

            /* Move input line */
            S[ 0 ] = in[ in_offset + k ];
        }
    }
}
