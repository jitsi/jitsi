/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * 
 * @author Dingxin Xu
 */
public class Silk_NSQ 
{
    /**
     * 
     * @param psEncC Encoder State
     * @param psEncCtrlC Encoder Control
     * @param NSQ NSQ state
     * @param x prefiltered input signal
     * @param q quantized qulse signal
     * @param LSFInterpFactor_Q2 LSF interpolation factor in Q2
     * @param PredCoef_Q12 Short term prediction coefficients
     * @param LTPCoef_Q14 Long term prediction coefficients
     * @param AR2_Q13
     * @param HarmShapeGain_Q14
     * @param Tilt_Q14 Spectral tilt
     * @param LF_shp_Q14
     * @param Gains_Q16
     * @param Lambda_Q10
     * @param LTP_scale_Q14 LTP state scaling
     */
    static void SKP_Silk_NSQ(
        SKP_Silk_encoder_state     psEncC,             /* I/O  Encoder State                       */
        SKP_Silk_encoder_control   psEncCtrlC,         /* I    Encoder Control                     */
        SKP_Silk_nsq_state         NSQ,                /* I/O  NSQ state                           */
        short                      x[],                /* I    prefiltered input signal            */
        byte                       q[],                /* O    quantized qulse signal              */
        int                        LSFInterpFactor_Q2, /* I    LSF interpolation factor in Q2      */
        short                      PredCoef_Q12[],     /* I    Short term prediction coefficients  */
        short                      LTPCoef_Q14[],      /* I    Long term prediction coefficients   */
        short                      AR2_Q13[],          /* I                                        */
        int                        HarmShapeGain_Q14[],/* I                                        */
        int                        Tilt_Q14[],         /* I    Spectral tilt                       */
        int                        LF_shp_Q14[],       /* I                                        */
        int                        Gains_Q16[],        /* I                                        */
        int                        Lambda_Q10,         /* I                                        */
        int                        LTP_scale_Q14       /* I    LTP state scaling                   */
    )
    {
        int     k, lag, start_idx, subfr_length, LSF_interpolation_flag;
        short []A_Q12;
        short [] B_Q14;
        short [] AR_shp_Q13;
        int           A_Q12_offset, B_Q14_offset, AR_shp_Q13_offset;
        short   []pxq;
        int     pxq_offset;
        int     sLTP_Q16[] = new int[ 2 * Silk_define.MAX_FRAME_LENGTH ];
        short   sLTP[] = new short[ 2 * Silk_define.MAX_FRAME_LENGTH ];
        int     HarmShapeFIRPacked_Q14;
        int     offset_Q10;
        int     FiltState[] = new int[ Silk_define.MAX_LPC_ORDER ];
        int     x_sc_Q10[] = new int[ Silk_define.MAX_FRAME_LENGTH / Silk_define.NB_SUBFR ];

        subfr_length = psEncC.frame_length / Silk_define.NB_SUBFR;

        NSQ.rand_seed  =  psEncCtrlC.Seed;
        /* Set unvoiced lag to the previous one, overwrite later for voiced */
        lag             = NSQ.lagPrev;

        assert( NSQ.prev_inv_gain_Q16 != 0 );

        offset_Q10 = Silk_tables_other.SKP_Silk_Quantization_Offsets_Q10[ psEncCtrlC.sigtype ][ psEncCtrlC.QuantOffsetType ];

        if( LSFInterpFactor_Q2 == ( 1 << 2 ) ) {
            LSF_interpolation_flag = 0;
        } else {
            LSF_interpolation_flag = 1;
        }

        /* Setup pointers to start of sub frame */
        NSQ.sLTP_shp_buf_idx = psEncC.frame_length;
        NSQ.sLTP_buf_idx     = psEncC.frame_length;
        pxq                  = NSQ.xq;
        pxq_offset           = psEncC.frame_length;
//TODO: use a local copy of the parameter short x[], which is supposed to be input;
        short[] x_tmp = x.clone();
        int     x_tmp_offset = 0;
//TODO: use a local copy of the parameter byte[] q, which is supposed to be output;     
        byte[]  q_tmp = q.clone();
        int     q_tmp_offset = 0;
        
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            A_Q12      = PredCoef_Q12;
            A_Q12_offset = (( k >> 1 ) | ( 1 - LSF_interpolation_flag )) * Silk_define.MAX_LPC_ORDER;
            B_Q14      = LTPCoef_Q14;
            B_Q14_offset = k * Silk_define.LTP_ORDER;
            AR_shp_Q13 = AR2_Q13;
            AR_shp_Q13_offset = k * Silk_define.SHAPE_LPC_ORDER_MAX;

            /* Noise shape parameters */
            assert( HarmShapeGain_Q14[ k ] >= 0 );
            HarmShapeFIRPacked_Q14  = ( HarmShapeGain_Q14[ k ] >> 2 );
            HarmShapeFIRPacked_Q14 |= ( (int)( HarmShapeGain_Q14[ k ] >> 1 ) << 16 );


            if( psEncCtrlC.sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Voiced */
                lag = psEncCtrlC.pitchL[ k ];

                NSQ.rewhite_flag = 0;
                /* Re-whitening */
                if( ( k & ( 3 - ( LSF_interpolation_flag << 1 ) ) ) == 0 ) {
                    /* Rewhiten with new A coefs */
                    
                    start_idx = psEncC.frame_length - lag - psEncC.predictLPCOrder - Silk_define.LTP_ORDER / 2;
                    start_idx = Silk_SigProc_FIX.SKP_LIMIT_int( start_idx, 0, psEncC.frame_length - psEncC.predictLPCOrder ); /* Limit */
                    
                    Arrays.fill(FiltState, 0, psEncC.predictLPCOrder, 0);
                    Silk_MA.SKP_Silk_MA_Prediction( NSQ.xq, start_idx + k * ( psEncC.frame_length >> 2 ), 
                        A_Q12, A_Q12_offset, FiltState, sLTP, start_idx, psEncC.frame_length - start_idx, psEncC.predictLPCOrder );

                    NSQ.rewhite_flag = 1;
                    NSQ.sLTP_buf_idx = psEncC.frame_length;
                }
            }
            
            SKP_Silk_nsq_scale_states( NSQ, x_tmp, x_tmp_offset, x_sc_Q10, psEncC.subfr_length, sLTP, 
                    sLTP_Q16, k, LTP_scale_Q14, Gains_Q16, psEncCtrlC.pitchL );


            SKP_Silk_noise_shape_quantizer( NSQ, psEncCtrlC.sigtype, x_sc_Q10, q_tmp, q_tmp_offset, pxq, pxq_offset, 
                    sLTP_Q16, A_Q12, A_Q12_offset, B_Q14, B_Q14_offset,
                AR_shp_Q13, AR_shp_Q13_offset, lag, HarmShapeFIRPacked_Q14, Tilt_Q14[ k ], LF_shp_Q14[ k ], Gains_Q16[ k ], Lambda_Q10, 
                offset_Q10, psEncC.subfr_length, psEncC.shapingLPCOrder, psEncC.predictLPCOrder
            );

            x_tmp_offset          += psEncC.subfr_length;
            q_tmp_offset          += psEncC.subfr_length;
            pxq_offset        += psEncC.subfr_length;
        }

        /* Save scalars for this layer */
        NSQ.sLF_AR_shp_Q12                 = NSQ.sLF_AR_shp_Q12;
        NSQ.prev_inv_gain_Q16              = NSQ.prev_inv_gain_Q16;
        NSQ.lagPrev                        = psEncCtrlC.pitchL[ Silk_define.NB_SUBFR - 1 ];
    /* Save quantized speech and noise shaping signals */
        System.arraycopy(NSQ.xq, psEncC.frame_length, NSQ.xq, 0, psEncC.frame_length);
        System.arraycopy(NSQ.sLTP_shp_Q10, psEncC.frame_length, NSQ.sLTP_shp_Q10, 0, psEncC.frame_length);
        
//TODO: copy back the q_tmp to the output parameter q;        
        System.arraycopy(q_tmp, 0, q, 0, q.length);
    }
    
    /**
     * SKP_Silk_noise_shape_quantizer.
     * @param NSQ NSQ state
     * @param sigtype Signal type
     * @param x_sc_Q10
     * @param q
     * @param q_offset
     * @param xq
     * @param xq_offset
     * @param sLTP_Q16 LTP state
     * @param a_Q12 Short term prediction coefs
     * @param a_Q12_offset
     * @param b_Q14 Long term prediction coefs
     * @param b_Q14_offset
     * @param AR_shp_Q13 Noise shaping AR coefs
     * @param AR_shp_Q13_offset
     * @param lag Pitch lag
     * @param HarmShapeFIRPacked_Q14
     * @param Tilt_Q14 Spectral tilt
     * @param LF_shp_Q14
     * @param Gain_Q16
     * @param Lambda_Q10
     * @param offset_Q10
     * @param length Input length
     * @param shapingLPCOrder Noise shaping AR filter order
     * @param predictLPCOrder Prediction filter order
     */
    static void SKP_Silk_noise_shape_quantizer(
        SKP_Silk_nsq_state  NSQ,               /* I/O  NSQ state                       */
            int             sigtype,            /* I    Signal type                     */
        final int           x_sc_Q10[],         /* I                                    */
        byte                q[],                /* O                                    */
        int                 q_offset,
        short               xq[],               /* O                                    */
        int                 xq_offset,
        int                 sLTP_Q16[],         /* I/O  LTP state                       */
        final short         a_Q12[],            /* I    Short term prediction coefs     */
        int                 a_Q12_offset,
        final short         b_Q14[],            /* I    Long term prediction coefs      */
        int                 b_Q14_offset,
        final short         AR_shp_Q13[],       /* I    Noise shaping AR coefs          */
        int                 AR_shp_Q13_offset,
        int                 lag,                /* I    Pitch lag                       */
        int                 HarmShapeFIRPacked_Q14, /* I                                */
        int                 Tilt_Q14,           /* I    Spectral tilt                   */
        int                 LF_shp_Q14,         /* I                                    */
        int                 Gain_Q16,           /* I                                    */
        int                 Lambda_Q10,         /* I                                    */
        int                 offset_Q10,         /* I                                    */
        int                 length,             /* I    Input length                    */
        int                 shapingLPCOrder,    /* I    Noise shaping AR filter order   */
        int                 predictLPCOrder     /* I    Prediction filter order         */
    )
    {
        int     i, j;
        int   LTP_pred_Q14, LPC_pred_Q10, n_AR_Q10, n_LTP_Q14;
        int   n_LF_Q10, r_Q10, q_Q0, q_Q10;
        int   thr1_Q10, thr2_Q10, thr3_Q10;
        int   dither;
        int   exc_Q10, LPC_exc_Q10, xq_Q10;
        int   tmp, sLF_AR_shp_Q10;
        int   []psLPC_Q14;
        int   psLPC_Q14_offset;
        int   []shp_lag_ptr, pred_lag_ptr;
        int   shp_lag_ptr_offset, pred_lag_ptr_offset;

        shp_lag_ptr  = NSQ.sLTP_shp_Q10;
        shp_lag_ptr_offset = NSQ.sLTP_shp_buf_idx - lag + Silk_define.HARM_SHAPE_FIR_TAPS / 2;
        pred_lag_ptr = sLTP_Q16;
        pred_lag_ptr_offset = NSQ.sLTP_buf_idx - lag + Silk_define.LTP_ORDER / 2;
        
        /* Setup short term AR state */
        psLPC_Q14     = NSQ.sLPC_Q14;
        psLPC_Q14_offset = Silk_define.MAX_LPC_ORDER - 1;

        /* Quantization thresholds */
        thr1_Q10 = ( -1536 - (Lambda_Q10 >> 1));
        thr2_Q10 = ( -512 - (Lambda_Q10 >> 1));
        thr2_Q10 = ( thr2_Q10 + (Silk_macros.SKP_SMULBB( offset_Q10, Lambda_Q10 ) >> 10 ));
        thr3_Q10 = (  512 + (Lambda_Q10 >> 1));

        for( i = 0; i < length; i++ ) {
            /* Generate dither */
            NSQ.rand_seed = Silk_SigProc_FIX.SKP_RAND( NSQ.rand_seed );

            /* dither = rand_seed < 0 ? 0xFFFFFFFF : 0; */
            dither = ( NSQ.rand_seed >> 31 );
                    
            /* Short-term prediction */
            assert( ( predictLPCOrder  & 1 ) == 0 );    /* check that order is even */
//            SKP_assert( ( (SKP_int64)a_Q12 & 3 ) == 0 );    /* check that array starts at 4-byte aligned address */
            assert( predictLPCOrder >= 10 );            /* check that unrolling works */
            /* Partially unrolled */
            LPC_pred_Q10 = Silk_macros.SKP_SMULWB(               psLPC_Q14[  psLPC_Q14_offset+0 ], a_Q12[ a_Q12_offset+0 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-1 ], a_Q12[ a_Q12_offset+1 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-2 ], a_Q12[ a_Q12_offset+2 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-3 ], a_Q12[ a_Q12_offset+3 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-4 ], a_Q12[ a_Q12_offset+4 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-5 ], a_Q12[ a_Q12_offset+5 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-6 ], a_Q12[ a_Q12_offset+6 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-7 ], a_Q12[ a_Q12_offset+7 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-8 ], a_Q12[ a_Q12_offset+8 ] );
            LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-9 ], a_Q12[ a_Q12_offset+9 ] );

            for( j = 10; j < predictLPCOrder; j ++ ) {
                LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-j ], a_Q12[ a_Q12_offset+j ] );
            }
            /* Long-term prediction */
            if( sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Unrolled loop */
                LTP_pred_Q14 = Silk_macros.SKP_SMULWB(               pred_lag_ptr[ pred_lag_ptr_offset +0 ], b_Q14[ b_Q14_offset+0 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -1 ], b_Q14[ b_Q14_offset+1 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -2 ], b_Q14[ b_Q14_offset+2 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -3 ], b_Q14[ b_Q14_offset+3 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset -4 ], b_Q14[ b_Q14_offset+4 ] );
                pred_lag_ptr_offset++;
            } else {
                LTP_pred_Q14 = 0;
            }

            /* Noise shape feedback */
            assert( ( shapingLPCOrder       & 1 ) == 0 );   /* check that order is even */
//            SKP_assert( ( (SKP_int64)AR_shp_Q13 & 3 ) == 0 );   /* check that array starts at 4-byte aligned address */
            assert( shapingLPCOrder >= 12 );                /* check that unrolling works */
            /* Partially unrolled */
            n_AR_Q10 = Silk_macros.SKP_SMULWB(           psLPC_Q14[   psLPC_Q14_offset+0 ], AR_shp_Q13[AR_shp_Q13_offset+0 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-1 ], AR_shp_Q13[AR_shp_Q13_offset+1 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-2 ], AR_shp_Q13[AR_shp_Q13_offset+2 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-3 ], AR_shp_Q13[AR_shp_Q13_offset+3 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-4 ], AR_shp_Q13[AR_shp_Q13_offset+4 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-5 ], AR_shp_Q13[AR_shp_Q13_offset+5 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-6 ], AR_shp_Q13[AR_shp_Q13_offset+6 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-7 ], AR_shp_Q13[AR_shp_Q13_offset+7 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-8 ], AR_shp_Q13[AR_shp_Q13_offset+8 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-9 ], AR_shp_Q13[AR_shp_Q13_offset+9 ] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[ psLPC_Q14_offset-10 ], AR_shp_Q13[AR_shp_Q13_offset+10] );
            n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[ psLPC_Q14_offset-11 ], AR_shp_Q13[AR_shp_Q13_offset+11] );

            for( j = 12; j < shapingLPCOrder; j ++ ) {
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[ psLPC_Q14_offset-j ], AR_shp_Q13[ AR_shp_Q13_offset+j ] );
            }
            n_AR_Q10 = ( n_AR_Q10 >> 1 );   /* Q11 -> Q10 */
            n_AR_Q10  = Silk_macros.SKP_SMLAWB( n_AR_Q10, NSQ.sLF_AR_shp_Q12, Tilt_Q14 );

            n_LF_Q10   = ( Silk_macros.SKP_SMULWB( NSQ.sLTP_shp_Q10[ NSQ.sLTP_shp_buf_idx - 1 ], LF_shp_Q14 ) << 2 ); 
            n_LF_Q10   = Silk_macros.SKP_SMLAWT( n_LF_Q10, NSQ.sLF_AR_shp_Q12, LF_shp_Q14 );

            assert( lag > 0 || sigtype == Silk_define.SIG_TYPE_UNVOICED );

            /* Long-term shaping */
            if( lag > 0 ) {
                /* Symmetric, packed FIR coefficients */
                n_LTP_Q14 = Silk_macros.SKP_SMULWB((shp_lag_ptr[ shp_lag_ptr_offset+0 ] + shp_lag_ptr[ shp_lag_ptr_offset-2 ] ), 
                        HarmShapeFIRPacked_Q14 );
                n_LTP_Q14 = Silk_macros.SKP_SMLAWT(n_LTP_Q14, shp_lag_ptr[ shp_lag_ptr_offset-1 ],HarmShapeFIRPacked_Q14 );
                shp_lag_ptr_offset++;
                n_LTP_Q14 = ( n_LTP_Q14 << 6 );
            } else {
                n_LTP_Q14 = 0;
            }

            /* Input minus prediction plus noise feedback  */
            //r = x[ i ] - LTP_pred - LPC_pred + n_AR + n_Tilt + n_LF + n_LTP;
            tmp   = ( LTP_pred_Q14 - n_LTP_Q14 );                       /* Add Q14 stuff */
            tmp   = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( tmp, 4 );                                 /* round to Q10  */
            tmp   = ( tmp + LPC_pred_Q10 );                             /* add Q10 stuff */ 
            tmp   = ( tmp - n_AR_Q10 );                                 /* subtract Q10 stuff */ 
            tmp   = ( tmp - n_LF_Q10 );                                 /* subtract Q10 stuff */ 
            r_Q10 = ( x_sc_Q10[ i ] - tmp );

            /* Flip sign depending on dither */
            r_Q10 = ( r_Q10 ^ dither ) - dither;
            r_Q10 = ( r_Q10 - offset_Q10 );
            r_Q10 = Silk_SigProc_FIX.SKP_LIMIT_32( r_Q10, -64 << 10, 64 << 10 );

            /* Quantize */
            if( r_Q10 < thr1_Q10 ) {
                q_Q0 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( ( r_Q10 + (Lambda_Q10 >> 1) ), 10 );
                q_Q10 = ( q_Q0 << 10 );
            } else if( r_Q10 < thr2_Q10 ) {
                q_Q0 = -1;
                q_Q10 = -1024;
            } else if( r_Q10 > thr3_Q10 ) {
                q_Q0 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( ( r_Q10 - (Lambda_Q10 >> 1) ), 10 );
                q_Q10 = ( q_Q0 << 10 );
            } else {
                q_Q0 = 0;
                q_Q10 = 0;
            }
            q[ q_offset + i ] = (  byte)q_Q0; /* No saturation needed because max is 64 */

            /* Excitation */
            exc_Q10 = ( q_Q10 + offset_Q10 );
            exc_Q10 = ( exc_Q10 ^ dither ) - dither;

            /* Add predictions */
            LPC_exc_Q10 = ( exc_Q10 + Silk_SigProc_FIX.SKP_RSHIFT_ROUND( LTP_pred_Q14, 4 ) );
            xq_Q10      = ( LPC_exc_Q10 + LPC_pred_Q10 );
            
            /* Scale XQ back to normal level before saving */
            xq[ xq_offset + i ] = (  short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( Silk_macros.SKP_SMULWW( xq_Q10, Gain_Q16 ), 10 ) );
            
            
            /* Update states */
            psLPC_Q14_offset++;
            psLPC_Q14[psLPC_Q14_offset] = ( xq_Q10 << 4 );
            sLF_AR_shp_Q10 = ( xq_Q10 - n_AR_Q10 );
            NSQ.sLF_AR_shp_Q12 = ( sLF_AR_shp_Q10 << 2 );

            NSQ.sLTP_shp_Q10[ NSQ.sLTP_shp_buf_idx ] = ( sLF_AR_shp_Q10 - n_LF_Q10 );
            sLTP_Q16[ NSQ.sLTP_buf_idx ] = ( LPC_exc_Q10 << 6 );
            NSQ.sLTP_shp_buf_idx++;
            NSQ.sLTP_buf_idx++;

            /* Make dither dependent on quantized signal */
            NSQ.rand_seed += q[ q_offset + i ];
        }
        /* Update LPC synth buffer */
        System.arraycopy(NSQ.sLPC_Q14, length, NSQ.sLPC_Q14, 0, Silk_define.MAX_LPC_ORDER);
    }

    /**
     * 
     * @param NSQ NSQ state
     * @param x input in Q0 
     * @param x_offset
     * @param x_sc_Q10 input scaled with 1/Gain
     * @param length length of input
     * @param sLTP re-whitened LTP state in Q0
     * @param sLTP_Q16 LTP state matching scaled input
     * @param subfr subframe number
     * @param LTP_scale_Q14
     * @param Gains_Q16
     * @param pitchL
     */
    static void SKP_Silk_nsq_scale_states(
            SKP_Silk_nsq_state NSQ,               /* I/O NSQ state                        */
            final short        x[],                /* I input in Q0                        */
            int                x_offset,
            int                x_sc_Q10[],         /* O input scaled with 1/Gain           */
            int                length,             /* I length of input                    */
            short              sLTP[],             /* I re-whitened LTP state in Q0        */
            int                sLTP_Q16[],         /* O LTP state matching scaled input    */
            int                subfr,              /* I subframe number                    */
            final int          LTP_scale_Q14,      /* I                                    */
            final int          Gains_Q16[], /* I                                 */
            final int          pitchL[]  /* I                                    */
        )
    {
        int   i, scale_length, lag;
        int   inv_gain_Q16, gain_adj_Q16, inv_gain_Q32;

        inv_gain_Q16 = ( Integer.MAX_VALUE / ( Gains_Q16[ subfr ] >> 1) );
        inv_gain_Q16 = ( inv_gain_Q16 < Short.MAX_VALUE ? inv_gain_Q16:Short.MAX_VALUE );
        lag          = pitchL[ subfr ];

        /* After rewhitening the LTP state is un-scaled */
        if( NSQ.rewhite_flag !=0 ) {
            inv_gain_Q32 = ( inv_gain_Q16 << 16 );
            if( subfr == 0 ) {
                /* Do LTP downscaling */
                inv_gain_Q32 = ( Silk_macros.SKP_SMULWB( inv_gain_Q32, LTP_scale_Q14 ) << 2 );
            }
            for( i = NSQ.sLTP_buf_idx - lag - Silk_define.LTP_ORDER / 2; i < NSQ.sLTP_buf_idx; i++ ) {
                sLTP_Q16[ i ] = Silk_macros.SKP_SMULWB( inv_gain_Q32, sLTP[ i ] );
            }
        }

        /* Prepare for Worst case. Next frame starts with max lag voiced */
        scale_length = length * Silk_define.NB_SUBFR;                                           /* approx max lag */
        scale_length = scale_length - Silk_macros.SKP_SMULBB( Silk_define.NB_SUBFR - (subfr + 1), length ); /* subtract samples that will be too old in next frame */
        scale_length = Silk_SigProc_FIX.SKP_max_int( scale_length, lag + Silk_define.LTP_ORDER );                /* make sure to scale whole pitch period if voiced */

        /* Adjust for changing gain */
        if( inv_gain_Q16 != NSQ.prev_inv_gain_Q16 ) {
            gain_adj_Q16 =  Silk_Inlines.SKP_DIV32_varQ( inv_gain_Q16, NSQ.prev_inv_gain_Q16, 16 );

            for( i = NSQ.sLTP_shp_buf_idx - scale_length; i < NSQ.sLTP_shp_buf_idx; i++ ) {
                NSQ.sLTP_shp_Q10[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, NSQ.sLTP_shp_Q10[ i ] );
            }

            /* Scale LTP predict state */
            if( NSQ.rewhite_flag == 0 ) {
                for( i = NSQ.sLTP_buf_idx - lag - Silk_define.LTP_ORDER / 2; i < NSQ.sLTP_buf_idx; i++ ) {
                    sLTP_Q16[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, sLTP_Q16[ i ] );
                }
            }
            NSQ.sLF_AR_shp_Q12 = Silk_macros.SKP_SMULWW( gain_adj_Q16, NSQ.sLF_AR_shp_Q12 );

            /* scale short term state */
            for( i = 0; i < Silk_define.MAX_LPC_ORDER; i++ ) {
                NSQ.sLPC_Q14[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, NSQ.sLPC_Q14[ i ] );
            }
        }

        /* Scale input */
        for( i = 0; i < length; i++ ) {
            x_sc_Q10[ i ] = ( Silk_macros.SKP_SMULBB( x[ x_offset + i ], ( short )inv_gain_Q16 ) >> 6 );
        }

        /* save inv_gain */
        assert( inv_gain_Q16 != 0 );
        NSQ.prev_inv_gain_Q16 = inv_gain_Q16;
    }
}
