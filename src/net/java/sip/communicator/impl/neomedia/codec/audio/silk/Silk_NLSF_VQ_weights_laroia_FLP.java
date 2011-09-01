/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * R. Laroia, N. Phamdo and N. Farvardin, "Robust and Efficient Quantization of Speech LSP
 * Parameters Using Structured Vector Quantization", Proc. IEEE Int. Conf. Acoust., Speech,
 * Signal Processing, pp. 641-644, 1991.
 * 
 * @author Dingxin Xu
 */
public class Silk_NLSF_VQ_weights_laroia_FLP
{
    static float MIN_NDELTA = ( 1e-6f / Silk_SigProc_FLP.PI );

    /**
     * Laroia low complexity NLSF weights.
     * @param pXW Pointer to input vector weights  [D x 1]
     * @param pX Pointer to input vector           [D x 1]
     * @param D Input vector dimension
     */ 
    static void SKP_Silk_NLSF_VQ_weights_laroia_FLP( 
              float     []pXW,           /* 0: Pointer to input vector weights           [D x 1] */
        final float     []pX,            /* I: Pointer to input vector                   [D x 1] */ 
        final int       D                /* I: Input vector dimension                            */
    )
    {
        int   k;
        float tmp1, tmp2;
        
        /* Safety checks */
        assert( D > 0 );
        assert( ( D & 1 ) == 0 );
        
        /* First value */
        tmp1 = 1.0f / ( pX[ 0 ] > MIN_NDELTA ? pX[0]:MIN_NDELTA);
        tmp2 = 1.0f / ((pX[ 1 ] - pX[ 0 ])>MIN_NDELTA ?(pX[ 1 ] - pX[ 0 ]):MIN_NDELTA);
        pXW[ 0 ] = tmp1 + tmp2;
        
        /* Main loop */
        for( k = 1; k < D - 1; k += 2 ) {
            tmp1 = 1.0f / ((pX[ k + 1 ] - pX[ k ])>MIN_NDELTA ? (pX[ k + 1 ] - pX[ k ]):MIN_NDELTA);
            pXW[ k ] = tmp1 + tmp2;

            tmp2 = 1.0f / ((pX[ k + 2 ] - pX[ k + 1 ]) >MIN_NDELTA ?(pX[ k + 2 ] - pX[ k + 1 ]):MIN_NDELTA);
            pXW[ k + 1 ] = tmp1 + tmp2;
        }
        
        /* Last value */
        tmp1 = 1.0f / ( (1.0f - pX[ D - 1 ])> MIN_NDELTA ?(1.0f - pX[ D - 1 ]):MIN_NDELTA);
        pXW[ D - 1 ] = tmp1 + tmp2;
    }
}
