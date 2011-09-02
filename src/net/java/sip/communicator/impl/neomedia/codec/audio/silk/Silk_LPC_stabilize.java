/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

public class Silk_LPC_stabilize
{

    static final int LPC_STABILIZE_LPC_MAX_ABS_VALUE_Q16 =( ( Short.MAX_VALUE ) << 4 );

    /**
     * LPC stabilizer, for a single input data vector.
     * @param a_Q12 stabilized LPC vector [L]
     * @param a_Q16 LPC vector [L] 
     * @param bwe_Q16 Bandwidth expansion factor
     * @param L  Number of LPC parameters in the input vector
     */
    static void SKP_Silk_LPC_stabilize(
        short       []a_Q12,         /* O    stabilized LPC vector [L]                       */
        int         []a_Q16,         /* I    LPC vector [L]                                  */
        final int   bwe_Q16,       /* I    Bandwidth expansion factor                      */
        final int   L              /* I    Number of LPC parameters in the input vector    */
    )
    {
        int   maxabs, absval, sc_Q16;
        int     i, idx = 0;
        int   invGain_Q30=0;

        Silk_bwexpander_32.SKP_Silk_bwexpander_32( a_Q16, L, bwe_Q16 );

        /***************************/
        /* Limit range of the LPCs */
        /***************************/
        /* Limit the maximum absolute value of the prediction coefficients */
        while( true ) {
            /* Find maximum absolute value and its index */
            maxabs = Integer.MIN_VALUE;
            for( i = 0; i < L; i++ ) {
                absval = Math.abs( a_Q16[ i ] );
                if( absval > maxabs ) {
                    maxabs = absval;
                    idx    = i;
                }
            }
        
            if( maxabs >= LPC_STABILIZE_LPC_MAX_ABS_VALUE_Q16 ) {
                /* Reduce magnitude of prediction coefficients */
                sc_Q16 = ( Integer.MAX_VALUE / ( maxabs >> 4 ) );
                sc_Q16 = 65536 - sc_Q16;
                sc_Q16 = ( sc_Q16 /(idx + 1) );
                sc_Q16 = 65536 - sc_Q16;
                sc_Q16 = ( Silk_macros.SKP_SMULWB( sc_Q16, 32604 ) << 1 ); // 0.995 in Q16
                Silk_bwexpander_32.SKP_Silk_bwexpander_32( a_Q16, L, sc_Q16 );
            } else {
                break;
            }
        }

        /* Convert to 16 bit Q12 */
        for( i = 0; i < L; i++ ) {
            a_Q12[ i ] = (short)Silk_SigProc_FIX.SKP_RSHIFT_ROUND( a_Q16[ i ], 4 );
        }

        /**********************/
        /* Ensure stable LPCs */
        /**********************/
        int invGain_Q30_ptr[] = new int[1];
        invGain_Q30_ptr[0] = invGain_Q30;
        while( Silk_LPC_inv_pred_gain.SKP_Silk_LPC_inverse_pred_gain( invGain_Q30_ptr, a_Q12, L ) == 1 ) {
            invGain_Q30 = invGain_Q30_ptr[0];
            Silk_bwexpander.SKP_Silk_bwexpander( a_Q12, L, 65339 ); // 0.997 in Q16
        }
    }

    /**
     * 
     * @param a_QQ Stabilized LPC vector, Q(24-rshift) [L]
     * @param a_Q24 LPC vector [L]
     * @param QQ Q domain of output LPC vector 
     * @param L  Number of LPC parameters in the input vector
     */
    static void SKP_Silk_LPC_fit(
        short     []a_QQ,          /* O    Stabilized LPC vector, Q(24-rshift) [L]         */
        int       []a_Q24,         /* I    LPC vector [L]                                  */
        final int QQ,            /* I    Q domain of output LPC vector                   */
        final int L              /* I    Number of LPC parameters in the input vector    */
    )
    {
        int     i, rshift, idx = 0;
        int   maxabs, absval, sc_Q16;

        rshift = 24 - QQ;

        /***************************/
        /* Limit range of the LPCs */
        /***************************/
        /* Limit the maximum absolute value of the prediction coefficients */
        while( true ) {
            /* Find maximum absolute value and its index */
            maxabs = Integer.MIN_VALUE;
            for( i = 0; i < L; i++ ) {
                absval = Math.abs( a_Q24[ i ] );
                if( absval > maxabs ) {
                    maxabs = absval;
                    idx    = i;
                }
            }
        
            maxabs = ( maxabs >> rshift );
            if( maxabs >= Short.MAX_VALUE ) {
                /* Reduce magnitude of prediction coefficients */
                maxabs = Math.min( maxabs, 98369 ); // ( SKP_int32_MAX / ( 65470 >> 2 ) ) + SKP_int16_MAX = 98369
                sc_Q16 = 65470 - ( ( (65470 >> 2) * (maxabs - Short.MAX_VALUE) ) /
                                            Silk_SigProc_FIX.SKP_RSHIFT32(( maxabs * (idx + 1)), 2 ) );
                Silk_bwexpander_32.SKP_Silk_bwexpander_32( a_Q24, L, sc_Q16 );
            } else {
                break;
            }
        }

        /* Convert to 16 bit Q(24-rshift) */
        assert( rshift > 0  );
        assert( rshift < 31 );
        for( i = 0; i < L; i++ ) {
            a_QQ[ i ] = (short)Silk_SigProc_FIX.SKP_RSHIFT_ROUND( a_Q24[ i ], rshift );
        }
    }
}
