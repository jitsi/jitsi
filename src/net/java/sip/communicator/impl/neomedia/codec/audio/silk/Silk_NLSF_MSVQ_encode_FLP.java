/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * NLSF vector encoder.
 * 
 * @author Dingxin Xu
 */
public class Silk_NLSF_MSVQ_encode_FLP 
{
    /**
     * NLSF vector encoder.
     * @param NLSFIndices Codebook path vector [ CB_STAGES ]
     * @param pNLSF Quantized NLSF vector [ LPC_ORDER ]
     * @param psNLSF_CB_FLP Codebook object
     * @param pNLSF_q_prev Prev. quantized NLSF vector [LPC_ORDER]
     * @param pW NLSF weight vector [ LPC_ORDER ]
     * @param NLSF_mu Rate weight for the RD optimization
     * @param NLSF_mu_fluc_red Fluctuation reduction error weight 
     * @param NLSF_MSVQ_Survivors  Max survivors from each stage
     * @param LPC_order LPC order 
     * @param deactivate_fluc_red Deactivate fluctuation reduction
     */
    static void SKP_Silk_NLSF_MSVQ_encode_FLP(
              int                   []NLSFIndices,       /* O    Codebook path vector [ CB_STAGES ]      */
              float                 []pNLSF,             /* I/O  Quantized NLSF vector [ LPC_ORDER ]     */
        final SKP_Silk_NLSF_CB_FLP  psNLSF_CB_FLP,     /* I    Codebook object                         */
        final float                 []pNLSF_q_prev,      /* I    Prev. quantized NLSF vector [LPC_ORDER] */
        final float                 []pW,                /* I    NLSF weight vector [ LPC_ORDER ]        */
        final float                 NLSF_mu,            /* I    Rate weight for the RD optimization     */
        final float                 NLSF_mu_fluc_red,   /* I    Fluctuation reduction error weight      */
        final int                   NLSF_MSVQ_Survivors,/* I    Max survivors from each stage           */
        final int                   LPC_order,          /* I    LPC order                               */
        final int                   deactivate_fluc_red /* I    Deactivate fluctuation reduction        */
    )
    {
        int     i, s, k, cur_survivors, prev_survivors, input_index, cb_index, bestIndex;
        float   se, wsse, rateDistThreshold, bestRateDist;
        float   pNLSF_in[] = new float[ Silk_define.MAX_LPC_ORDER ];

        float   pRateDist[];
        float   pRate[];
        float   pRate_new[];
        int     pTempIndices[];
        int     pPath[];
        int     pPath_new[];
        float   pRes[];
        float   pRes_new[];
        if(Silk_define.LOW_COMPLEXITY_ONLY == 1) 
        {
            pRateDist =    new float[Silk_define.NLSF_MSVQ_TREE_SEARCH_MAX_VECTORS_EVALUATED_LC_MODE()];
            pRate =        new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE ];
            pRate_new =    new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE ];
            pTempIndices = new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE ];
            pPath =        new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * Silk_define.NLSF_MSVQ_MAX_CB_STAGES];
            pPath_new =    new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * Silk_define.NLSF_MSVQ_MAX_CB_STAGES];
            pRes =         new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * Silk_define.MAX_LPC_ORDER ];
            pRes_new =     new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * Silk_define.MAX_LPC_ORDER ];
        }else
        {
            pRateDist =    new float[Silk_define.NLSF_MSVQ_TREE_SEARCH_MAX_VECTORS_EVALUATED() ];
            pRate =        new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS ];
            pRate_new =    new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS ];
            pTempIndices = new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS ];
            pPath =        new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS * Silk_define.NLSF_MSVQ_MAX_CB_STAGES ];
            pPath_new =    new int[Silk_define.MAX_NLSF_MSVQ_SURVIVORS * Silk_define.NLSF_MSVQ_MAX_CB_STAGES ];
            pRes =         new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS * Silk_define.MAX_LPC_ORDER ];
            pRes_new =     new float[Silk_define.MAX_NLSF_MSVQ_SURVIVORS * Silk_define.MAX_LPC_ORDER ];
        }

        float[] pConstFloat;int pConstFloat_offset;
        float[] pFloat; int pFloat_offset;
        int[]   pConstInt; int pConstInt_offset;
        int[]   pInt; int pInt_offset;
        float[] pCB_element;int pCB_element_offset;
        SKP_Silk_NLSF_CBS_FLP pCurrentCBStage;

        assert( NLSF_MSVQ_Survivors <= Silk_define.MAX_NLSF_MSVQ_SURVIVORS );
        assert( ( Silk_define.LOW_COMPLEXITY_ONLY == 0 ) || ( NLSF_MSVQ_Survivors <= Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE ) );

        cur_survivors = NLSF_MSVQ_Survivors;



        /* Copy the input vector */
        System.arraycopy(pNLSF, 0, pNLSF_in, 0, LPC_order);

        /****************************************************/
        /* Tree search for the multi-stage vector quantizer */
        /****************************************************/

        /* Clear accumulated rates */
        Arrays.fill(pRate, 0, NLSF_MSVQ_Survivors, 0);
        
        /* Copy NLSFs into residual signal vector */
        System.arraycopy(pNLSF, 0, pRes, 0, LPC_order);

        /* Set first stage values */
        prev_survivors = 1;

        /* Loop over all stages */
        for( s = 0; s < psNLSF_CB_FLP.nStages; s++ ) {

            /* Set a pointer to the current stage codebook */
            pCurrentCBStage = psNLSF_CB_FLP.CBStages[ s ];

            /* Calculate the number of survivors in the current stage */
            cur_survivors = Math.min( NLSF_MSVQ_Survivors, prev_survivors * pCurrentCBStage.nVectors );

            if(Silk_define.NLSF_MSVQ_FLUCTUATION_REDUCTION == 0 ) {
                 /* Find a single best survivor in the last stage, if we */
                 /* do not need candidates for fluctuation reduction     */
                 if( s == psNLSF_CB_FLP.nStages - 1 ) {
                    cur_survivors = 1;
                 }
            }           
            /* Nearest neighbor clustering for multiple input data vectors */
            Silk_NLSF_VQ_rate_distortion_FLP.SKP_Silk_NLSF_VQ_rate_distortion_FLP( pRateDist, pCurrentCBStage, 
                    pRes, pW, pRate, NLSF_mu, prev_survivors, LPC_order );

            /* Sort the rate-distortion errors */
            Silk_sort_FLP.SKP_Silk_insertion_sort_increasing_FLP( pRateDist, 0, pTempIndices, prev_survivors * pCurrentCBStage.nVectors, cur_survivors );

            /* Discard survivors with rate-distortion values too far above the best one */
            rateDistThreshold = Silk_define.NLSF_MSVQ_SURV_MAX_REL_RD * pRateDist[ 0 ];
            while( pRateDist[ cur_survivors - 1 ] > rateDistThreshold && cur_survivors > 1 ) {
                cur_survivors--;
            }

            /* Update accumulated codebook contributions for the 'cur_survivors' best codebook indices */
            for( k = 0; k < cur_survivors; k++ ) { 
                if( s > 0 ) {
                    /* Find the indices of the input and the codebook vector */
                    if( pCurrentCBStage.nVectors == 8 ) {
                        input_index = ( pTempIndices[ k ] >> 3 );
                        cb_index    = pTempIndices[ k ] & 7;
                    } else {
                        input_index = pTempIndices[ k ] / pCurrentCBStage.nVectors;  
                        cb_index    = pTempIndices[ k ] - input_index * pCurrentCBStage.nVectors;
                    }
                } else {
                    /* Find the indices of the input and the codebook vector */
                    input_index = 0;
                    cb_index    = pTempIndices[ k ];
                }

                /* Subtract new contribution from the previous residual vector for each of 'cur_survivors' */
                pConstFloat = pRes;
                pConstFloat_offset = input_index * LPC_order;
                pCB_element = pCurrentCBStage.CB;
                pCB_element_offset = cb_index * LPC_order;
                pFloat      = pRes_new;
                pFloat_offset = k * LPC_order;
                for( i = 0; i < LPC_order; i++ ) {
                    pFloat[ pFloat_offset + i ] = pConstFloat[ pConstFloat_offset + i ] - pCB_element[ pCB_element_offset + i ];
                }

                /* Update accumulated rate for stage 1 to the current */
                pRate_new[ k ] = pRate[ input_index ] + pCurrentCBStage.Rates[ cb_index ];

                /* Copy paths from previous matrix, starting with the best path */
                pConstInt = pPath;
                pConstInt_offset = input_index * psNLSF_CB_FLP.nStages;
                pInt      = pPath_new;
                pInt_offset = k * psNLSF_CB_FLP.nStages;
                for( i = 0; i < s; i++ ) {
                    pInt[ pInt_offset + i ] = pConstInt[ pConstInt_offset + i ];
                }
                /* Write the current stage indices for the 'cur_survivors' to the best path matrix */
                pInt[ pInt_offset + s ] = cb_index;
            }

            if( s < psNLSF_CB_FLP.nStages - 1 ) {
                /* Copy NLSF residual matrix for next stage */
                System.arraycopy(pRes_new, 0, pRes, 0, cur_survivors * LPC_order);

                /* Copy rate vector for next stage */
                System.arraycopy(pRate_new, 0, pRate, 0, cur_survivors);

                /* Copy best path matrix for next stage */
                System.arraycopy(pPath_new, 0, pPath, 0, cur_survivors * psNLSF_CB_FLP.nStages);
            }

            prev_survivors = cur_survivors;
        }

        /* (Preliminary) index of the best survivor, later to be decoded */
        bestIndex = 0;

        if (Silk_define.NLSF_MSVQ_FLUCTUATION_REDUCTION == 1)
        {
            /******************************/
            /* NLSF fluctuation reduction */
            /******************************/
            if( deactivate_fluc_red != 1 ) {
            
                /* Search among all survivors, now taking also weighted fluctuation errors into account */
                bestRateDist = Float.MAX_VALUE;
                for( s = 0; s < cur_survivors; s++ ) {
                    /* Decode survivor to compare with previous quantized NLSF vector */
                    Silk_NLSF_MSVQ_decode_FLP.SKP_Silk_NLSF_MSVQ_decode_FLP( pNLSF, psNLSF_CB_FLP, 
                            pPath_new, s * psNLSF_CB_FLP.nStages, LPC_order );

                    /* Compare decoded NLSF vector with the previously quantized vector */ 
                    wsse = 0;
                    for( i = 0; i < LPC_order; i += 2 ) {
                        /* Compute weighted squared quantization error for index i */
                        se = pNLSF[ i ] - pNLSF_q_prev[ i ];
                        wsse += pW[ i ] * se * se;

                        /* Compute weighted squared quantization error for index i + 1 */
                        se = pNLSF[ i + 1 ] - pNLSF_q_prev[ i + 1 ];
                        wsse += pW[ i + 1 ] * se * se;
                    }

                    /* Add the fluctuation reduction penalty to the rate distortion error */
                    wsse = pRateDist[s] + wsse * NLSF_mu_fluc_red;

                    /* Keep index of best survivor */
                    if( wsse < bestRateDist ) {
                        bestRateDist = wsse;
                        bestIndex = s;
                    }
                }
            }
        }

        /* Copy best path to output argument */
        System.arraycopy(pPath_new, bestIndex * psNLSF_CB_FLP.nStages, NLSFIndices, 0, psNLSF_CB_FLP.nStages);

        /* Decode and stabilize the best survivor */
        Silk_NLSF_MSVQ_decode_FLP.SKP_Silk_NLSF_MSVQ_decode_FLP( pNLSF, psNLSF_CB_FLP, NLSFIndices, 0, LPC_order );
    }
}
