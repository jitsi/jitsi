/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_schur_FLP 
{
    /**
     * 
     * @param refl_coef reflection coefficients (length order)
     * @param ref1_coef_offset offset of valid data.
     * @param auto_corr autotcorreation sequence (length order+1)
     * @param auto_corr_offset offset of valid data.
     * @param order order
     */
    static void SKP_Silk_schur_FLP(  
            float       refl_coef[],        /* O    reflection coefficients (length order)      */
            int ref1_coef_offset,
            float auto_corr[],        /* I    autotcorreation sequence (length order+1)   */
            int auto_corr_offset,
            int         order               /* I    order                                       */
    )
    {
        int k, n;
        float[][] C = new float[Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC + 1][2];
        float Ctmp1, Ctmp2, rc_tmp;

        /* copy correlations */
        for (k = 0; k < order + 1; k++)
        {
            C[k][0] = C[k][1] = auto_corr[auto_corr_offset + k];
        }

        for (k = 0; k < order; k++)
        {
            /* get reflection coefficient */
            rc_tmp = -C[k + 1][0] / Math.max(C[0][1], 1e-9f);

            /* save the output */
            refl_coef[ref1_coef_offset + k] = rc_tmp;

            /* update correlations */
            for (n = 0; n < order - k; n++)
            {
                Ctmp1 = C[n + k + 1][0];
                Ctmp2 = C[n][1];
                C[n + k + 1][0] = Ctmp1 + Ctmp2 * rc_tmp;
                C[n][1] = Ctmp2 + Ctmp1 * rc_tmp;
            }
        }
    }
}
