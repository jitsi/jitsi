/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Core decoder. Performs inverse NSQ operation LTP + LPC
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_decode_core 
{
    /**
     * Core decoder. Performs inverse NSQ operation LTP + LPC.
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param xq Decoded speech.
     * @param xq_offset offset of the valid data.
     * @param q Pulse signal.
     */
    static void SKP_Silk_decode_core(
            SKP_Silk_decoder_state      psDec,                          /* I/O  Decoder state               */
            SKP_Silk_decoder_control    psDecCtrl,                      /* I    Decoder control             */
            short[]                     xq,                             /* O    Decoded speech              */
            int                            xq_offset,
            int[]                       q                               /* I    Pulse signal                */
    )
    {
        int     i, k, lag = 0, start_idx, NLSF_interpolation_flag, sigtype, LTP_scale_Q14;
        short[] A_Q12;
        short[] B_Q14;
        int     B_Q14_offset;
        
        short[] pxq;
        int     pxq_offset;
        short[] A_Q12_tmp = new short[Silk_define.MAX_LPC_ORDER];
        
        short[]   sLTP = new short[ Silk_define.MAX_FRAME_LENGTH ];
        
        int   Gain_Q16;
        int[] pred_lag_ptr;
        int   pred_lag_ptr_offset;
        int[] pexc_Q10;
        int   pexc_Q10_offset;
        int[] pres_Q10;
        int   pres_Q10_offset;
        int   LTP_pred_Q14;
        int   LPC_pred_Q10;
        
        int   rand_seed, offset_Q10, dither;
        int[]   vec_Q10 = new int[ Silk_define.MAX_FRAME_LENGTH / Silk_define.NB_SUBFR ];
        int   inv_gain_Q16, inv_gain_Q32, gain_adj_Q16;
        int[] FiltState = new int[ Silk_define.MAX_LPC_ORDER ];
        int j;

        Silk_typedef.SKP_assert( psDec.prev_inv_gain_Q16 != 0 );
        
        offset_Q10 = Silk_tables_other.SKP_Silk_Quantization_Offsets_Q10[ psDecCtrl.sigtype ][ psDecCtrl.QuantOffsetType ];

        if( psDecCtrl.NLSFInterpCoef_Q2 < ( 1 << 2 ) ) {
            NLSF_interpolation_flag = 1;
        } else {
            NLSF_interpolation_flag = 0;
        }


        /* Decode excitation */
        rand_seed = psDecCtrl.Seed;
        for( i = 0; i < psDec.frame_length; i++ ) {
            rand_seed = Silk_SigProc_FIX.SKP_RAND( rand_seed );
            /* dither = rand_seed < 0 ? 0xFFFFFFFF : 0; */
            dither = rand_seed>>31;
        
            psDec.exc_Q10[ i ] = (q[ i ] << 10) + offset_Q10;
            psDec.exc_Q10[ i ] = ( psDec.exc_Q10[ i ] ^ dither ) - dither;

            rand_seed += q[ i ];
        }


        pexc_Q10 = psDec.exc_Q10;
        pexc_Q10_offset = 0;
        
        pres_Q10 = psDec.res_Q10;
        pres_Q10_offset = 0;
        
        pxq      = psDec.outBuf;
        pxq_offset = psDec.frame_length;
        
        psDec.sLTP_buf_idx = psDec.frame_length;
        /* Loop over subframes */
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            A_Q12 = psDecCtrl.PredCoef_Q12[ k >> 1 ];

            /* Preload LPC coeficients to array on stack. Gives small performance gain */        
            System.arraycopy(A_Q12, 0, A_Q12_tmp, 0, psDec.LPC_order);
            
            B_Q14         = psDecCtrl.LTPCoef_Q14;
            B_Q14_offset  = k*Silk_define.LTP_ORDER;
            
            Gain_Q16      = psDecCtrl.Gains_Q16[ k ];
            LTP_scale_Q14 = psDecCtrl.LTP_scale_Q14;
            sigtype       = psDecCtrl.sigtype;

            inv_gain_Q16  = Silk_typedef.SKP_int32_MAX/(Gain_Q16>>1);
            inv_gain_Q16  = Math.min(inv_gain_Q16, Silk_typedef.SKP_int16_MAX);
            
            /* Calculate Gain adjustment factor */
            gain_adj_Q16 = 1 << 16;
            if( inv_gain_Q16 != psDec.prev_inv_gain_Q16 ) {
                gain_adj_Q16 =  Silk_Inlines.SKP_DIV32_varQ( inv_gain_Q16, psDec.prev_inv_gain_Q16, 16 );
            }

            /* Avoid abrupt transition from voiced PLC to unvoiced normal decoding */
            if( psDec.lossCnt !=0 && psDec.prev_sigtype == Silk_define.SIG_TYPE_VOICED &&
                psDecCtrl.sigtype == Silk_define.SIG_TYPE_UNVOICED && k < ( Silk_define.NB_SUBFR >> 1 ) ) {
                
                Arrays.fill(B_Q14, B_Q14_offset, B_Q14_offset+Silk_define.LTP_ORDER, (short)0);
                
                B_Q14[ B_Q14_offset + Silk_define.LTP_ORDER/2 ] = ( short )1 << 12; /* 0.25 */
            
                sigtype = Silk_define.SIG_TYPE_VOICED;
                psDecCtrl.pitchL[ k ] = psDec.lagPrev;
                LTP_scale_Q14 = 1 << 14;
            }

            if( sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Voiced */
                
                lag = psDecCtrl.pitchL[ k ];
                /* Re-whitening */
                if( ( k & ( 3 - (NLSF_interpolation_flag << 1))) == 0 ) {
                    /* Rewhiten with new A coefs */
                    start_idx = psDec.frame_length - lag - psDec.LPC_order - Silk_define.LTP_ORDER / 2;
                    start_idx = Silk_SigProc_FIX.SKP_LIMIT_int( start_idx, 0, psDec.frame_length - psDec.LPC_order );

                    Arrays.fill(FiltState, 0, psDec.LPC_order, 0);
                    
                    Silk_MA.SKP_Silk_MA_Prediction( psDec.outBuf, start_idx + k * ( psDec.frame_length >> 2 ), 
                            A_Q12, 0, FiltState, sLTP,  start_idx, psDec.frame_length - start_idx, psDec.LPC_order );

                    
                    /* After rewhitening the LTP state is unscaled */
                    inv_gain_Q32 = ( inv_gain_Q16 << 16 );
                    
                    if( k == 0 ) {
                        /* Do LTP downscaling */
                        inv_gain_Q32 = ( Silk_macros.SKP_SMULWB( inv_gain_Q32, psDecCtrl.LTP_scale_Q14 )<< 2 );
                    }
                    for( i = 0; i < (lag + Silk_define.LTP_ORDER/2); i++ ) {
                        psDec.sLTP_Q16[ psDec.sLTP_buf_idx - i - 1 ] = Silk_macros.SKP_SMULWB( inv_gain_Q32, sLTP[ psDec.frame_length - i - 1 ] );
                    }
                } else {
                    /* Update LTP state when Gain changes */
                    if( gain_adj_Q16 != 1 << 16 ) {
                        for( i = 0; i < ( lag + Silk_define.LTP_ORDER / 2 ); i++ ) {
                            psDec.sLTP_Q16[ psDec.sLTP_buf_idx - i - 1 ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, psDec.sLTP_Q16[ psDec.sLTP_buf_idx - i - 1 ] );
                        }
                    }
                }
            }
            
            /* Scale short term state */
            for( i = 0; i < Silk_define.MAX_LPC_ORDER; i++ ) {
                psDec.sLPC_Q14[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, psDec.sLPC_Q14[ i ] );
            }

            /* Save inv_gain */
            Silk_typedef.SKP_assert( inv_gain_Q16 != 0 );
            psDec.prev_inv_gain_Q16 = inv_gain_Q16;

            /* Long-term prediction */
            if( sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Setup pointer */
                pred_lag_ptr = psDec.sLTP_Q16;
                pred_lag_ptr_offset = psDec.sLTP_buf_idx - lag + Silk_define.LTP_ORDER / 2;
                
                for( i = 0; i < psDec.subfr_length; i++ ) {
                    /* Unrolled loop */
                    LTP_pred_Q14 = Silk_macros.SKP_SMULWB(               pred_lag_ptr[ pred_lag_ptr_offset +0 ], B_Q14[ B_Q14_offset + 0 ] );
                    LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -1 ], B_Q14[ B_Q14_offset + 1 ] );
                    LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -2 ], B_Q14[ B_Q14_offset + 2 ] );
                    LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -3 ], B_Q14[ B_Q14_offset + 3 ] );
                    LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -4 ], B_Q14[ B_Q14_offset + 4 ] );
                    pred_lag_ptr_offset++;
                
                    /* Generate LPC residual */ 
                    pres_Q10[ pres_Q10_offset + i  ] = ( pexc_Q10[ pexc_Q10_offset + i ] + Silk_SigProc_FIX.SKP_RSHIFT_ROUND( LTP_pred_Q14, 4 ) );
                
                    /* Update states */
                    psDec.sLTP_Q16[ psDec.sLTP_buf_idx ] = ( pres_Q10[ pres_Q10_offset +  i ]<< 6 );
                    psDec.sLTP_buf_idx++;
                }
            } else {
                System.arraycopy(pexc_Q10, pexc_Q10_offset + 0, pres_Q10, pres_Q10_offset + 0, psDec.subfr_length);
            }


            for( i = 0; i < psDec.subfr_length; i++ ) {
                /* Partially unrolled */
                LPC_pred_Q10 = Silk_macros.SKP_SMULWB(               psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  1 ], A_Q12_tmp[ 0 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  2 ], A_Q12_tmp[ 1 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  3 ], A_Q12_tmp[ 2 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  4 ], A_Q12_tmp[ 3 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  5 ], A_Q12_tmp[ 4 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  6 ], A_Q12_tmp[ 5 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  7 ], A_Q12_tmp[ 6 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  8 ], A_Q12_tmp[ 7 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i -  9 ], A_Q12_tmp[ 8 ] );
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i - 10 ], A_Q12_tmp[ 9 ] );

                for( j = 10; j < psDec.LPC_order; j ++ ) {
                    LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i - j - 1 ], A_Q12_tmp[ j ] );
                }

                /* Add prediction to LPC residual */
                vec_Q10[ i ] = ( pres_Q10[ pres_Q10_offset + i ]+ LPC_pred_Q10 );
                
                /* Update states */
                psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i ] = ( vec_Q10[ i ]<< 4 );
            }
            /* Scale with Gain */
            for( i = 0; i < psDec.subfr_length; i++ ) {
                pxq[ pxq_offset+i ] = ( short )Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( Silk_macros.SKP_SMULWW( vec_Q10[ i ], Gain_Q16 ), 10 ) );
            }

            /* Update LPC filter state */
            System.arraycopy(psDec.sLPC_Q14, psDec.subfr_length, psDec.sLPC_Q14, 0, Silk_define.MAX_LPC_ORDER);
            
            pexc_Q10_offset += psDec.subfr_length;
            pres_Q10_offset += psDec.subfr_length;
            pxq_offset      += psDec.subfr_length;
        }
        
        /* Copy to output */
        System.arraycopy(psDec.outBuf, psDec.frame_length, xq, xq_offset+0, psDec.frame_length);
    }
}
