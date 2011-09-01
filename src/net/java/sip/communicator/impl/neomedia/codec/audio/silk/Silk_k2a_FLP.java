/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * step up function, converts reflection coefficients to 
 * prediction coefficients.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_k2a_FLP 
{
    /**
     * step up function, converts reflection coefficients to prediction coefficients.
     * 
     * @param A prediction coefficients [order].
     * @param rc reflection coefficients [order].
     * @param order prediction order.
     */
    static void SKP_Silk_k2a_FLP(
        float[]       A,                 /* O:   prediction coefficients [order]             */
        float[] rc,                /* I:   reflection coefficients [order]             */
        int       order               /* I:   prediction order                            */
    )
    {
        int   k, n;
        float[] Atmp = new float[Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC];

        for( k = 0; k < order; k++ )
        {
            for( n = 0; n < k; n++ )
            {
                Atmp[ n ] = A[ n ];
            }
            for( n = 0; n < k; n++ ) 
            {
                A[ n ] += Atmp[ k - n - 1 ] * rc[ k ];
            }
            A[ k ] = -rc[ k ];
        }
    }
}
