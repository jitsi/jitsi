/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * compute inverse of LPC prediction gain, and
 * test if LPC coefficients are stable (all poles within unit circle)
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_LPC_inv_pred_gain_FLP 
{
    static final float RC_THRESHOLD =       0.9999f;
    
    /**
     *     compute inverse of LPC prediction gain, and                          
     *  test if LPC coefficients are stable (all poles within unit circle)   
     *  this code is based on SKP_Silk_a2k_FLP().
     * @param invGain inverse prediction gain, energy domain
     * @param A prediction coefficients [order]
     * @param A_offset offset of valid data.
     * @param order prediction order
     * @return returns 1 if unstable, otherwise 0
     */
    static int SKP_Silk_LPC_inverse_pred_gain_FLP(   /* O:   returns 1 if unstable, otherwise 0      */
        float[]       invGain,               /* O:   inverse prediction gain, energy domain  */
        float[]       A,                     /* I:   prediction coefficients [order]         */
        int A_offset,
        int           order                  /* I:   prediction order                        */
    )
    {
        int   k, n;
        double    rc, rc_mult1, rc_mult2;
        float[][] Atmp = new float[ 2 ][ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC ];
        float[] Aold, Anew;

        Anew = Atmp[ order & 1 ];
        for(int i_djinn=0; i_djinn<order; i_djinn++)
            Anew[i_djinn] = A[A_offset+i_djinn];

        invGain[0] = 1.0f;
        for( k = order - 1; k > 0; k-- ) 
        {
            rc = -Anew[ k ];
            if (rc > RC_THRESHOLD || rc < -RC_THRESHOLD)
            {
                return 1;
            }
            rc_mult1 = 1.0f - rc * rc;
            rc_mult2 = 1.0f / rc_mult1;
            invGain[0] *= (float)rc_mult1;
            /* swap pointers */
            Aold = Anew;
            Anew = Atmp[ k & 1 ];
            for( n = 0; n < k; n++ ) {
                Anew[ n ] = (float)( ( Aold[ n ] - Aold[ k - n - 1 ] * rc ) * rc_mult2 );
            }
        }
        rc = -Anew[ 0 ];
        if ( rc > RC_THRESHOLD || rc < -RC_THRESHOLD ) {
            return 1;
        }
        rc_mult1 = 1.0f - rc * rc;
        invGain[0] *= (float)rc_mult1;
        return 0;
    }
}
