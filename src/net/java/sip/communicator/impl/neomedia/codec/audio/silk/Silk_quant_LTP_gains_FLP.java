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
public class Silk_quant_LTP_gains_FLP 
{

    /**
     * 
     * @param B (Un-)quantized LTP gains 
     * @param cbk_index Codebook index
     * @param periodicity_index Periodicity index
     * @param W Error weights
     * @param mu Mu value (R/D tradeoff)
     * @param lowComplexity Flag for low complexity
     */ 
    static void SKP_Silk_quant_LTP_gains_FLP(
              float        B[],                                 /* I/O  (Un-)quantized LTP gains                */
              int          cbk_index[],                         /* O    Codebook index                          */
              int          []periodicity_index,                 /* O    Periodicity index                       */
              final float  W[],                                 /* I    Error weights                           */
              final float  mu,                                  /* I    Mu value (R/D tradeoff)                 */
              final int    lowComplexity                        /* I    Flag for low complexity                 */
    )
    {
//        SKP_int             j, k, temp_idx[ NB_SUBFR ], cbk_size;
//        const SKP_uint16    *cdf_ptr;
//        const SKP_int16     *cl_ptr;
//        const SKP_int16     *cbk_ptr_Q14;
//        const SKP_float     *b_ptr, *W_ptr;
//        SKP_float           rate_dist_subfr, rate_dist, min_rate_dist;
        int j,k,cbk_size;
        int[] temp_idx = new int[Silk_define.NB_SUBFR];
        int[] cdf_ptr; int cdf_ptr_offset;
        short[] cl_ptr; int cl_ptr_offset;
        short[] cbk_ptr_Q14; int cbk_ptr_Q14_offset;
        float b_ptr[];
        float W_ptr[]; int b_ptr_offset,W_ptr_offset;
        float rate_dist_subfr = 0, rate_dist, min_rate_dist;



        /***************************************************/
        /* Iterate over different codebooks with different */
        /* rates/distortions, and choose best */
        /***************************************************/
//        min_rate_dist = SKP_float_MAX;
        min_rate_dist = Float.MAX_VALUE;
        for( k = 0; k < 3; k++ ) {
            cdf_ptr     = Silk_tables_LTP.SKP_Silk_LTP_gain_CDF_ptrs[     k ];
            cl_ptr      = Silk_tables_LTP.SKP_Silk_LTP_gain_BITS_Q6_ptrs[ k ];
            cbk_ptr_Q14 = Silk_tables_LTP.SKP_Silk_LTP_vq_ptrs_Q14[       k ];
            cbk_size    = Silk_tables_LTP.SKP_Silk_LTP_vq_sizes[          k ];


            /* Setup pointer to first subframe */
            W_ptr = W;
            W_ptr_offset = 0;
            b_ptr = B;
            b_ptr_offset = 0;

            rate_dist = 0.0f;
            for( j = 0; j < Silk_define.NB_SUBFR; j++ ) {

                float [] rate_dist_subfr_ptr = new float[1];
                rate_dist_subfr_ptr[0] = rate_dist_subfr;
                
                Silk_VQ_nearest_neighbor_FLP.SKP_Silk_VQ_WMat_EC_FLP(
                    temp_idx,         /* O    index of best codebook vector                           */
                    j,
                    rate_dist_subfr_ptr,       /* O    best weighted quantization error + mu * rate            */
                    b_ptr,                  /* I    input vector to be quantized                            */
                    b_ptr_offset,
                    W_ptr,                  /* I    weighting matrix                                        */
                    W_ptr_offset,
                    cbk_ptr_Q14,            /* I    codebook                                                */
                    cl_ptr,                 /* I    code length for each codebook vector                    */
                    mu,                     /* I    tradeoff between weighted error and rate                */
                    cbk_size                /* I    number of vectors in codebook                           */
                );
                rate_dist_subfr = rate_dist_subfr_ptr[0];
//                Silk_VQ_nearest_neighbor_FLP.SKP_Silk_VQ_WMat_EC_FLP(
//                        &temp_idx[ j ],         /* O    index of best codebook vector                           */
//                        &rate_dist_subfr,       /* O    best weighted quantization error + mu * rate            */
//                        b_ptr,                  /* I    input vector to be quantized                            */
//                        W_ptr,                  /* I    weighting matrix                                        */
//                        cbk_ptr_Q14,            /* I    codebook                                                */
//                        cl_ptr,                 /* I    code length for each codebook vector                    */
//                        mu,                     /* I    tradeoff between weighted error and rate                */
//                        cbk_size                /* I    number of vectors in codebook                           */
//                    );


                rate_dist += rate_dist_subfr;

//                b_ptr += LTP_ORDER;
//                W_ptr += LTP_ORDER * LTP_ORDER;
                b_ptr_offset += Silk_define.LTP_ORDER;
                W_ptr_offset += Silk_define.LTP_ORDER * Silk_define.LTP_ORDER;
            }

            if( rate_dist < min_rate_dist ) {
                min_rate_dist = rate_dist;
//                SKP_memcpy( cbk_index, temp_idx, NB_SUBFR * sizeof( SKP_int ) );
//                *periodicity_index = k;
                System.arraycopy(temp_idx, 0, cbk_index, 0, Silk_define.NB_SUBFR);
                periodicity_index[0] = k;
            }

            /* Break early in low-complexity mode if rate distortion is below threshold */
            if( lowComplexity != 0 && ( rate_dist * 16384.0f < (float)Silk_tables_LTP.SKP_Silk_LTP_gain_middle_avg_RD_Q14 ) ) {
                break;
            }
        }

//        cbk_ptr_Q14 = SKP_Silk_LTP_vq_ptrs_Q14[ *periodicity_index ];
        cbk_ptr_Q14 = Silk_tables_LTP.SKP_Silk_LTP_vq_ptrs_Q14[periodicity_index[0]];
        
        for( j = 0; j < Silk_define.NB_SUBFR; j++ ) {
//            SKP_short2float_array( &B[ j * LTP_ORDER ],
//                &cbk_ptr_Q14[ cbk_index[ j ] * LTP_ORDER ],
//                LTP_ORDER );
            Silk_SigProc_FLP.SKP_short2float_array(B, j*Silk_define.LTP_ORDER, 
                    cbk_ptr_Q14, cbk_index[ j ] * Silk_define.LTP_ORDER, 
                    Silk_define.LTP_ORDER);
        }

        for( j = 0; j < Silk_define.NB_SUBFR * Silk_define.LTP_ORDER; j++ ) {
            B[ j ] *= Silk_define_FLP.Q14_CONVERSION_FAC;
        }
    }
}
