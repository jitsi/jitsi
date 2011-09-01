/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Rate-Distortion calculations for multiple input data vectors
 *
 * @author Dingxin Xu
 */
public class Silk_NLSF_VQ_rate_distortion_FLP 
{     
    /**
     * Rate-Distortion calculations for multiple input data vectors.
     * @param pRD Rate-distortion values [psNLSF_CBS_FLP->nVectors*N]
     * @param psNLSF_CBS_FLP NLSF codebook stage struct
     * @param in Input vectors to be quantized
     * @param w Weight vector
     * @param rate_acc Accumulated rates from previous stage
     * @param mu Weight between weighted error and rate 
     * @param N Number of input vectors to be quantized 
     * @param LPC_order  LPC order 
     */
    static void SKP_Silk_NLSF_VQ_rate_distortion_FLP(
              float             []pRD,               /* O   Rate-distortion values [psNLSF_CBS_FLP->nVectors*N] */
        final SKP_Silk_NLSF_CBS_FLP psNLSF_CBS_FLP,    /* I   NLSF codebook stage struct                          */
        final float             []in,                /* I   Input vectors to be quantized                       */
        final float             []w,                 /* I   Weight vector                                       */
        final float             []rate_acc,          /* I   Accumulated rates from previous stage               */
        final float             mu,                 /* I   Weight between weighted error and rate              */
        final int               N,                  /* I   Number of input vectors to be quantized             */
        final int               LPC_order           /* I   LPC order                                           */
    )
    {
        float[] pRD_vec;
        int     pRD_vec_offset;
        int   i, n;

        /* Compute weighted quantization errors for all input vectors over one codebook stage */
        Silk_NLSF_VQ_sum_error_FLP.SKP_Silk_NLSF_VQ_sum_error_FLP( pRD, in, w, psNLSF_CBS_FLP.CB, 
                N, psNLSF_CBS_FLP.nVectors, LPC_order );

        /* Loop over input vectors */
        pRD_vec = pRD;
        pRD_vec_offset = 0;
        for( n = 0; n < N; n++ ) {
            /* Add rate cost to error for each codebook vector */
            for( i = 0; i < psNLSF_CBS_FLP.nVectors; i++ ) {
                pRD_vec[ pRD_vec_offset + i ] += mu * ( rate_acc[n] + psNLSF_CBS_FLP.Rates[ i ] );
            }
            pRD_vec_offset += psNLSF_CBS_FLP.nVectors;
        }
    }
}
