/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Dingxin Xu
 */
public class Silk_VQ_nearest_neighbor_FLP 
{
    /**
     * entropy constrained MATRIX-weighted VQ, for a single input data vector.
     * @param ind Index of best codebook vector
     * @param rate_dist Best weighted quant. error + mu * rate
     * @param in Input vector to be quantized
     * @param W Weighting matrix 
     * @param cb Codebook
     * @param cl_Q6 Code length for each codebook vector
     * @param mu Tradeoff between WSSE and rate
     * @param L Number of vectors in codebook
     */
    static void SKP_Silk_VQ_WMat_EC_FLP(
              int                   []ind,               /* O    Index of best codebook vector           */
              int                   ind_offset,
              float                 []rate_dist,         /* O    Best weighted quant. error + mu * rate  */
        final float                 []in,                /* I    Input vector to be quantized            */
              int                   in_offset,
        final float                 []W,                 /* I    Weighting matrix                        */
              int                   W_offset,
        final short                 []cb,                /* I    Codebook                                */
        final short                 []cl_Q6,             /* I    Code length for each codebook vector    */
        final float                 mu,                 /* I    Tradeoff between WSSE and rate          */
        final int                   L                   /* I    Number of vectors in codebook           */
    )
    {
//        SKP_int   k;
//        SKP_float sum1;
//        SKP_float diff[ 5 ];
//        const SKP_int16 *cb_row;
        int k;
        float sum1;
        float diff[] = new float[5];
        final short []cb_row;
        int cb_row_offset = 0;

        /* Loop over codebook */
//        *rate_dist = SKP_float_MAX;
        rate_dist[0] = Float.MAX_VALUE;
        
        cb_row = cb;
        cb_row_offset = 0;
        
        for( k = 0; k < L; k++ ) {
            /* Calc difference between in vector and cbk vector */
            diff[ 0 ] = in[ in_offset + 0 ] - cb_row[ 0 ] * Silk_define_FLP.Q14_CONVERSION_FAC;
            diff[ 1 ] = in[ in_offset + 1 ] - cb_row[ 1 ] * Silk_define_FLP.Q14_CONVERSION_FAC;
            diff[ 2 ] = in[ in_offset + 2 ] - cb_row[ 2 ] * Silk_define_FLP.Q14_CONVERSION_FAC;
            diff[ 3 ] = in[ in_offset + 3 ] - cb_row[ 3 ] * Silk_define_FLP.Q14_CONVERSION_FAC;
            diff[ 4 ] = in[ in_offset + 4 ] - cb_row[ 4 ] * Silk_define_FLP.Q14_CONVERSION_FAC;

            /* Weighted rate */
            sum1 = mu * cl_Q6[ k ] / 64.0f;

            /* Add weighted quantization error, assuming W is symmetric */
            /* first row of W */
            sum1 += diff[ 0 ] * ( W[ W_offset + 0 ] * diff[ 0 ] + 
                         2.0f * ( W[ W_offset + 1 ] * diff[ 1 ] + 
                                  W[ W_offset + 2 ] * diff[ 2 ] + 
                                  W[ W_offset + 3 ] * diff[ 3 ] + 
                                  W[ W_offset + 4 ] * diff[ 4 ] ) );

            /* second row of W */
            sum1 += diff[ 1 ] * ( W[ W_offset + 6 ] * diff[ 1 ] + 
                         2.0f * ( W[ W_offset + 7 ] * diff[ 2 ] + 
                                  W[ W_offset + 8 ] * diff[ 3 ] + 
                                  W[ W_offset + 9 ] * diff[ 4 ] ) );

            /* third row of W */
            sum1 += diff[ 2 ] * ( W[ W_offset + 12 ] * diff[ 2 ] + 
                        2.0f *  ( W[ W_offset + 13 ] * diff[ 3 ] + 
                                  W[ W_offset + 14 ] * diff[ 4 ] ) );

            /* fourth row of W */
            sum1 += diff[ 3 ] * ( W[ W_offset + 18 ] * diff[ 3 ] + 
                         2.0f * ( W[ W_offset + 19 ] * diff[ 4 ] ) );

            /* last row of W */
            sum1 += diff[ 4 ] * ( W[ W_offset + 24 ] * diff[ 4 ] );

            /* find best */
            if( sum1 < rate_dist[0] ) {
                rate_dist[0] = sum1;
                ind[ind_offset + 0] = k;
            }

            /* Go to next cbk vector */
            cb_row_offset += Silk_define.LTP_ORDER;
        }
    }
}
