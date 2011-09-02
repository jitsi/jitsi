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
class NSQ_del_dec_struct
{
    int RandState[] = new int[ Silk_define.DECISION_DELAY ];
    int Q_Q10[]     = new int[ Silk_define.DECISION_DELAY ];
    int Xq_Q10[]    = new int[ Silk_define.DECISION_DELAY ];
    int Pred_Q16[]  = new int[ Silk_define.DECISION_DELAY ];
    int Shape_Q10[] = new int[ Silk_define.DECISION_DELAY ];
    int Gain_Q16[]  = new int[ Silk_define.DECISION_DELAY ];
    int sLPC_Q14[]  = new int[ Silk_define.MAX_FRAME_LENGTH / Silk_define.NB_SUBFR + Silk_define.NSQ_LPC_BUF_LENGTH() ];
    int LF_AR_Q12;
    int Seed;
    int SeedInit;
    int RD_Q10;
    public void FieldsInit()
    {
        Arrays.fill(this.RandState, 0);
        Arrays.fill(this.Q_Q10, 0);
        Arrays.fill(this.Xq_Q10, 0);
        Arrays.fill(this.Pred_Q16, 0);
        Arrays.fill(this.Shape_Q10, 0);
        Arrays.fill(this.Gain_Q16, 0);
        Arrays.fill(this.sLPC_Q14, 0);
        this.LF_AR_Q12 = 0;
        this.Seed = 0;
        this.SeedInit = 0;
        this.RD_Q10 = 0;
    }
}

/**
 * 
 * @author Dingxin Xu
 */
class NSQ_sample_struct implements Cloneable
{
    int Q_Q10;
    int RD_Q10;
    int xq_Q14;
    int LF_AR_Q12;
    int sLTP_shp_Q10;
    int LPC_exc_Q16;    
    public Object clone() 
    {
        NSQ_sample_struct clone = null;
        try {
            clone = (NSQ_sample_struct) super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return clone;
    }
}

/**
 * 
 * @author Dingxin Xu
 */
public class Silk_NSQ_del_dec 
{
    static void SKP_Silk_NSQ_del_dec(
        SKP_Silk_encoder_state    psEncC,             /* I/O  Encoder State                       */
        SKP_Silk_encoder_control  psEncCtrlC,         /* I    Encoder Control                     */
        SKP_Silk_nsq_state        NSQ,                /* I/O  NSQ state                           */
        short                     x[],                /* I    Prefiltered input signal            */
        byte                      q[],                /* O    Quantized pulse signal              */
        int                       LSFInterpFactor_Q2, /* I    LSF interpolation factor in Q2      */
        short                     PredCoef_Q12[],     /* I    Prediction coefs                    */
        short                     LTPCoef_Q14[],      /* I    LT prediction coefs                 */
        short                     AR2_Q13[],          /* I                                        */
        int                       HarmShapeGain_Q14[],/* I                                        */
        int                       Tilt_Q14[],         /* I    Spectral tilt                       */
        int                       LF_shp_Q14[],       /* I                                        */
        int                       Gains_Q16[],        /* I                                        */
        int                       Lambda_Q10,         /* I                                        */
        int                       LTP_scale_Q14       /* I    LTP state scaling                   */
    )
    {
        int     i, k, lag, start_idx, LSF_interpolation_flag, Winner_ind, subfr;
        int     last_smple_idx, smpl_buf_idx, decisionDelay, subfr_length;
        short[] A_Q12;
        short[] B_Q14;
        short[] AR_shp_Q13;
        int           A_Q12_offset, B_Q14_offset, AR_shp_Q13_offset;
        short[] pxq;
        int     pxq_offset;
        int   sLTP_Q16[] = new int[ 2 * Silk_define.MAX_FRAME_LENGTH ];
        short   sLTP[] = new short[ 2 * Silk_define.MAX_FRAME_LENGTH ];
        int   HarmShapeFIRPacked_Q14;
        int     offset_Q10;
        int   FiltState[] = new int[ Silk_define.MAX_LPC_ORDER ], RDmin_Q10;
        int   x_sc_Q10[] = new int[ Silk_define.MAX_FRAME_LENGTH / Silk_define.NB_SUBFR ];
        NSQ_del_dec_struct psDelDec[] = new NSQ_del_dec_struct[ Silk_define.DEL_DEC_STATES_MAX ];
        /*
         * psDelDec is an array of references, which has to be created manually.
         */
        {
            for(int psDelDecIni_i=0; psDelDecIni_i<Silk_define.DEL_DEC_STATES_MAX; psDelDecIni_i++)
            {
                psDelDec[psDelDecIni_i] = new NSQ_del_dec_struct();
            }
        }
        NSQ_del_dec_struct psDD;

        subfr_length = psEncC.frame_length / Silk_define.NB_SUBFR;

        /* Set unvoiced lag to the previous one, overwrite later for voiced */
        lag = NSQ.lagPrev;

        assert( NSQ.prev_inv_gain_Q16 != 0 );

      //TODO: use a local copy of the parameter short x[], which is supposed to be input;
        short[] x_tmp = x.clone();
        int     x_tmp_offset = 0;
//TODO: use a local copy of the parameter byte[] q, which is supposed to be output;     
        byte[]  q_tmp = q.clone();
        int     q_tmp_offset = 0;
        
        /* Initialize delayed decision states */
//        SKP_memset( psDelDec, 0, psEncC.nStatesDelayedDecision * sizeof( NSQ_del_dec_struct ) );
//TODO:
        for(int inx=0; inx<psEncC.nStatesDelayedDecision; inx++)
        {
            psDelDec[inx].FieldsInit();
        }
        for( k = 0; k < psEncC.nStatesDelayedDecision; k++ ) {
            psDD                 = psDelDec[ k ];
            psDD.Seed           = ( k + psEncCtrlC.Seed ) & 3;
            psDD.SeedInit       = psDD.Seed;
            psDD.RD_Q10         = 0;
            psDD.LF_AR_Q12      = NSQ.sLF_AR_shp_Q12;
            psDD.Shape_Q10[ 0 ] = NSQ.sLTP_shp_Q10[ psEncC.frame_length - 1 ];
//            SKP_memcpy( psDD.sLPC_Q14, NSQ.sLPC_Q14, NSQ_LPC_BUF_LENGTH * sizeof( SKP_int32 ) );
            System.arraycopy(NSQ.sLPC_Q14, 0, psDD.sLPC_Q14, 0, Silk_define.NSQ_LPC_BUF_LENGTH());
        }

        offset_Q10   = Silk_tables_other.SKP_Silk_Quantization_Offsets_Q10[ psEncCtrlC.sigtype ][ psEncCtrlC.QuantOffsetType ];
        smpl_buf_idx = 0; /* index of oldest samples */

        decisionDelay = ( Silk_define.DECISION_DELAY < subfr_length ? Silk_define.DECISION_DELAY:subfr_length );

        /* For voiced frames limit the decision delay to lower than the pitch lag */
        if( psEncCtrlC.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
                decisionDelay = ( decisionDelay < (psEncCtrlC.pitchL[ k ] - Silk_define.LTP_ORDER / 2 - 1) ? 
                        decisionDelay:(psEncCtrlC.pitchL[ k ] - Silk_define.LTP_ORDER / 2 - 1));
            }
        }

        if( LSFInterpFactor_Q2 == ( 1 << 2 ) ) {
            LSF_interpolation_flag = 0;
        } else {
            LSF_interpolation_flag = 1;
        }

        /* Setup pointers to start of sub frame */
        pxq                   = NSQ.xq;
        pxq_offset            = psEncC.frame_length;
        NSQ.sLTP_shp_buf_idx = psEncC.frame_length;
        NSQ.sLTP_buf_idx     = psEncC.frame_length;
        subfr = 0;
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            A_Q12      = PredCoef_Q12;
            A_Q12_offset =  ( ( k >> 1 ) | ( 1 - LSF_interpolation_flag ) ) * Silk_define.MAX_LPC_ORDER ;
            B_Q14      = LTPCoef_Q14;
            B_Q14_offset = k * Silk_define.LTP_ORDER;
            AR_shp_Q13 = AR2_Q13;
            AR_shp_Q13_offset = k * Silk_define.SHAPE_LPC_ORDER_MAX;

            NSQ.rewhite_flag = 0;
            if( psEncCtrlC.sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Voiced */
                lag = psEncCtrlC.pitchL[ k ];

                /* Re-whitening */
                if( ( k & ( 3 - ( LSF_interpolation_flag << 1 ) ) ) == 0 ) {
                    if( k == 2 ) {
                        /* RESET DELAYED DECISIONS */
                        /* Find winner */
                        RDmin_Q10 = psDelDec[ 0 ].RD_Q10;
                        Winner_ind = 0;
                        for( i = 1; i < psEncC.nStatesDelayedDecision; i++ ) {
                            if( psDelDec[ i ].RD_Q10 < RDmin_Q10 ) {
                                RDmin_Q10 = psDelDec[ i ].RD_Q10;
                                Winner_ind = i;
                            }
                        }
                        for( i = 0; i < psEncC.nStatesDelayedDecision; i++ ) {
                            if( i != Winner_ind ) {
                                psDelDec[ i ].RD_Q10 += ( Integer.MAX_VALUE >> 4 );
                                assert( psDelDec[ i ].RD_Q10 >= 0 );
                            }
                        }
                        
                        /* Copy final part of signals from winner state to output and long-term filter states */
                        psDD = psDelDec[ Winner_ind ];
                        last_smple_idx = smpl_buf_idx + decisionDelay;
                        for( i = 0; i < decisionDelay; i++ ) {
                            last_smple_idx = ( last_smple_idx - 1 ) & Silk_define.DECISION_DELAY_MASK;
//                            q[   i - decisionDelay ] = ( SKP_int )SKP_RSHIFT( psDD.Q_Q10[ last_smple_idx ], 10 );
                            q_tmp[   q_tmp_offset + i - decisionDelay ] = (byte) ( psDD.Q_Q10[ last_smple_idx ] >> 10 );

//                            pxq[ i - decisionDelay ] = ( SKP_int16 )SKP_SAT16( SKP_RSHIFT_ROUND( 
//                                SKP_SMULWW( psDD.Xq_Q10[ last_smple_idx ], 
//                                psDD.Gain_Q16[ last_smple_idx ] ), 10 ) );
                            pxq[ pxq_offset + i - decisionDelay ] = (short) Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( 
                                    Silk_macros.SKP_SMULWW( psDD.Xq_Q10[ last_smple_idx ], 
                                    psDD.Gain_Q16[ last_smple_idx ] ), 10 ) );
                            NSQ.sLTP_shp_Q10[ NSQ.sLTP_shp_buf_idx - decisionDelay + i ] = psDD.Shape_Q10[ last_smple_idx ];
                        }

                        subfr = 0;
                    }

                    /* Rewhiten with new A coefs */
                    start_idx = psEncC.frame_length - lag - psEncC.predictLPCOrder - Silk_define.LTP_ORDER / 2;
                    start_idx = Silk_SigProc_FIX.SKP_LIMIT_int( start_idx, 0, psEncC.frame_length - psEncC.predictLPCOrder );
                    
//                    SKP_memset( FiltState, 0, psEncC.predictLPCOrder * sizeof( SKP_int32 ) );
                    Arrays.fill(FiltState, 0, psEncC.predictLPCOrder, 0);
                    Silk_MA.SKP_Silk_MA_Prediction( NSQ.xq, start_idx + k * psEncC.subfr_length, 
                        A_Q12, A_Q12_offset, FiltState, sLTP, start_idx, psEncC.frame_length - start_idx, psEncC.predictLPCOrder );

                    NSQ.sLTP_buf_idx = psEncC.frame_length;
                    NSQ.rewhite_flag = 1;
                }
            }

            /* Noise shape parameters */
            assert( HarmShapeGain_Q14[ k ] >= 0 );
            HarmShapeFIRPacked_Q14  =                        ( HarmShapeGain_Q14[ k ] >> 2 );
            HarmShapeFIRPacked_Q14 |= ( ( HarmShapeGain_Q14[ k ] >> 1 ) << 16 );

            SKP_Silk_nsq_del_dec_scale_states( NSQ, psDelDec, x_tmp, x_tmp_offset, x_sc_Q10, 
                subfr_length, sLTP, sLTP_Q16, k, psEncC.nStatesDelayedDecision, smpl_buf_idx,
                LTP_scale_Q14, Gains_Q16, psEncCtrlC.pitchL );

            int smpl_buf_idx_ptr[] = new int[1];
            smpl_buf_idx_ptr[0] = smpl_buf_idx;
            SKP_Silk_noise_shape_quantizer_del_dec( NSQ, psDelDec, psEncCtrlC.sigtype, x_sc_Q10, q_tmp, q_tmp_offset, pxq, pxq_offset,
                    sLTP_Q16, A_Q12, A_Q12_offset, B_Q14, B_Q14_offset, AR_shp_Q13, AR_shp_Q13_offset, lag, HarmShapeFIRPacked_Q14, Tilt_Q14[ k ], 
                    LF_shp_Q14[ k ], Gains_Q16[ k ], Lambda_Q10, offset_Q10, psEncC.subfr_length, subfr++, psEncC.shapingLPCOrder, psEncC.predictLPCOrder, 
                psEncC.nStatesDelayedDecision, smpl_buf_idx_ptr, decisionDelay );
            smpl_buf_idx = smpl_buf_idx_ptr[0];
            
            x_tmp_offset   += psEncC.subfr_length;
            q_tmp_offset   += psEncC.subfr_length;
            pxq_offset += psEncC.subfr_length;
        }

        /* Find winner */
        RDmin_Q10 = psDelDec[ 0 ].RD_Q10;
        Winner_ind = 0;
        for( k = 1; k < psEncC.nStatesDelayedDecision; k++ ) {
            if( psDelDec[ k ].RD_Q10 < RDmin_Q10 ) {
                RDmin_Q10 = psDelDec[ k ].RD_Q10;
                Winner_ind = k;
            }
        }
        
        /* Copy final part of signals from winner state to output and long-term filter states */
        psDD = psDelDec[ Winner_ind ];
        psEncCtrlC.Seed = psDD.SeedInit;
        last_smple_idx = smpl_buf_idx + decisionDelay;
        for( i = 0; i < decisionDelay; i++ ) {
            last_smple_idx = ( last_smple_idx - 1 ) & Silk_define.DECISION_DELAY_MASK;
            q_tmp[q_tmp_offset + i - decisionDelay] = ( byte )( psDD.Q_Q10[ last_smple_idx ] >> 10 );
            pxq[ pxq_offset + i - decisionDelay ] = ( short )Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( 
                Silk_macros.SKP_SMULWW( psDD.Xq_Q10[ last_smple_idx ], psDD.Gain_Q16[ last_smple_idx ] ), 10 ) );
            NSQ.sLTP_shp_Q10[ NSQ.sLTP_shp_buf_idx - decisionDelay + i ] = psDD.Shape_Q10[ last_smple_idx ];
            sLTP_Q16[          NSQ.sLTP_buf_idx     - decisionDelay + i ] = psDD.Pred_Q16[  last_smple_idx ];

        }
//        SKP_memcpy( NSQ.sLPC_Q14, &psDD.sLPC_Q14[ psEncC.subfr_length ], NSQ_LPC_BUF_LENGTH * sizeof( SKP_int32 ) );
        System.arraycopy(psDD.sLPC_Q14, psEncC.subfr_length, NSQ.sLPC_Q14, 0, Silk_define.NSQ_LPC_BUF_LENGTH());
        
        /* Update states */
        NSQ.sLF_AR_shp_Q12    = psDD.LF_AR_Q12;
        NSQ.prev_inv_gain_Q16 = NSQ.prev_inv_gain_Q16;
        NSQ.lagPrev           = psEncCtrlC.pitchL[ Silk_define.NB_SUBFR - 1 ];

        /* Save quantized speech and noise shaping signals */
//        SKP_memcpy( NSQ.xq,           &NSQ.xq[           psEncC.frame_length ], psEncC.frame_length * sizeof( SKP_int16 ) );
//        SKP_memcpy( NSQ.sLTP_shp_Q10, &NSQ.sLTP_shp_Q10[ psEncC.frame_length ], psEncC.frame_length * sizeof( SKP_int32 ) );
        System.arraycopy(NSQ.xq, psEncC.frame_length, NSQ.xq, 0, psEncC.frame_length);
        System.arraycopy(NSQ.sLTP_shp_Q10, psEncC.frame_length, NSQ.sLTP_shp_Q10, 0, psEncC.frame_length);
//TODO: copy back the q_tmp to the output parameter q;        
        System.arraycopy(q_tmp, 0, q, 0, q.length);        
    }

    /**
     * Noise shape quantizer for one subframe.
     * @param NSQ NSQ state
     * @param psDelDec Delayed decision states
     * @param sigtype Signal type
     * @param x_Q10
     * @param q
     * @param q_offset
     * @param xq
     * @param xq_offset
     * @param sLTP_Q16 LTP filter state
     * @param a_Q12 Short term prediction coefs
     * @param a_Q12_offset
     * @param b_Q14 Long term prediction coefs
     * @param b_Q14_offset
     * @param AR_shp_Q13 Noise shaping coefs
     * @param AR_shp_Q13_offset
     * @param lag Pitch lag
     * @param HarmShapeFIRPacked_Q14
     * @param Tilt_Q14 Spectral tilt
     * @param LF_shp_Q14
     * @param Gain_Q16
     * @param Lambda_Q10
     * @param offset_Q10
     * @param length Input length
     * @param subfr Subframe number
     * @param shapingLPCOrder Shaping LPC filter order
     * @param predictLPCOrder Prediction LPC filter order
     * @param nStatesDelayedDecision Number of states in decision tree
     * @param smpl_buf_idx Index to newest samples in buffers
     * @param decisionDelay
     */
    static void SKP_Silk_noise_shape_quantizer_del_dec(
        SKP_Silk_nsq_state  NSQ,                   /* I/O  NSQ state                           */
        NSQ_del_dec_struct  psDelDec[],             /* I/O  Delayed decision states             */
        int                 sigtype,                /* I    Signal type                         */
        final int           x_Q10[],                /* I                                        */
        byte                q[],                    /* O                                        */
        int                 q_offset,
        short               xq[],                   /* O                                        */
        int                 xq_offset,
        int                 sLTP_Q16[],             /* I/O  LTP filter state                    */
        final short         a_Q12[],                /* I    Short term prediction coefs         */
        int                 a_Q12_offset,
        final short         b_Q14[],                /* I    Long term prediction coefs          */
        int                 b_Q14_offset,
        final short         AR_shp_Q13[],           /* I    Noise shaping coefs                 */
        int                 AR_shp_Q13_offset,
        int                 lag,                    /* I    Pitch lag                           */
        int                 HarmShapeFIRPacked_Q14, /* I                                        */
        int                 Tilt_Q14,               /* I    Spectral tilt                       */
        int                 LF_shp_Q14,             /* I                                        */
        int                 Gain_Q16,               /* I                                        */
        int                 Lambda_Q10,             /* I                                        */
        int                 offset_Q10,             /* I                                        */
        int                 length,                 /* I    Input length                        */
        int                 subfr,                  /* I    Subframe number                     */
        int                 shapingLPCOrder,        /* I    Shaping LPC filter order            */
        int                 predictLPCOrder,        /* I    Prediction LPC filter order         */
        int                 nStatesDelayedDecision, /* I    Number of states in decision tree   */
        int                 []smpl_buf_idx,          /* I    Index to newest samples in buffers  */
        int                 decisionDelay           /* I                                        */
    )
    {
        int     i, j, k, Winner_ind, RDmin_ind, RDmax_ind, last_smple_idx;
        int   Winner_rand_state;
        int   LTP_pred_Q14, LPC_pred_Q10, n_AR_Q10, n_LTP_Q14;
        int   n_LF_Q10;
        int   r_Q10, rr_Q20, rd1_Q10, rd2_Q10, RDmin_Q10, RDmax_Q10;
        int   q1_Q10, q2_Q10;
        int   dither;
        int   exc_Q10, LPC_exc_Q10, xq_Q10;
        int   tmp, sLF_AR_shp_Q10;
        int   pred_lag_ptr[], shp_lag_ptr[];
        int   pred_lag_ptr_offset, shp_lag_ptr_offset;
        int   []psLPC_Q14; int psLPC_Q14_offset;
        NSQ_sample_struct  psSampleState[][] = new NSQ_sample_struct[ Silk_define.DEL_DEC_STATES_MAX ][ 2 ];
        /*
         * psSampleState is an two-dimension array of reference, which should be created manually.
         */
        {
            for(int Ini_i=0; Ini_i<Silk_define.DEL_DEC_STATES_MAX; Ini_i++)
            {
                for(int Ini_j=0; Ini_j<2; Ini_j++)
                {
                    psSampleState[Ini_i][Ini_j] = new NSQ_sample_struct();
                }
            }
        }
        NSQ_del_dec_struct psDD;
        NSQ_sample_struct[]  psSS;

        shp_lag_ptr  = NSQ.sLTP_shp_Q10;
        shp_lag_ptr_offset = NSQ.sLTP_shp_buf_idx - lag + Silk_define.HARM_SHAPE_FIR_TAPS / 2;
        pred_lag_ptr = sLTP_Q16;
        pred_lag_ptr_offset = NSQ.sLTP_buf_idx - lag + Silk_define.LTP_ORDER / 2;

        for( i = 0; i < length; i++ ) {
            /* Perform common calculations used in all states */

            /* Long-term prediction */
            if( sigtype == Silk_define.SIG_TYPE_VOICED ) {
                /* Unrolled loop */
                LTP_pred_Q14 = Silk_macros.SKP_SMULWB(               pred_lag_ptr[ pred_lag_ptr_offset+0 ], b_Q14[ b_Q14_offset+0 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset-1 ], b_Q14[ b_Q14_offset+1 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset-2 ], b_Q14[ b_Q14_offset+2 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset-3 ], b_Q14[ b_Q14_offset+3 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset-4 ], b_Q14[ b_Q14_offset+4 ] );
                pred_lag_ptr_offset++;
            } else {
                LTP_pred_Q14 = 0;
            }

            /* Long-term shaping */
            if( lag > 0 ) {
                /* Symmetric, packed FIR coefficients */
                n_LTP_Q14 = Silk_macros.SKP_SMULWB( ( shp_lag_ptr[ shp_lag_ptr_offset+0 ] + shp_lag_ptr[ shp_lag_ptr_offset-2 ] ), HarmShapeFIRPacked_Q14 );
                n_LTP_Q14 = Silk_macros.SKP_SMLAWT( n_LTP_Q14, shp_lag_ptr[ shp_lag_ptr_offset-1 ], HarmShapeFIRPacked_Q14 );
//                n_LTP_Q14 = SKP_LSHIFT( n_LTP_Q14, 6 );
                n_LTP_Q14 = ( n_LTP_Q14 << 6 );
                shp_lag_ptr_offset++;
            } else {
                n_LTP_Q14 = 0;
            }

            for( k = 0; k < nStatesDelayedDecision; k++ ) {
                /* Delayed decision state */
                psDD = psDelDec[ k ];

                /* Sample state */
                psSS = psSampleState[ k ];

                /* Generate dither */
                psDD.Seed = Silk_SigProc_FIX.SKP_RAND( psDD.Seed );

                /* dither = rand_seed < 0 ? 0xFFFFFFFF : 0; */
//                dither = SKP_RSHIFT( psDD.Seed, 31 );
                dither = ( psDD.Seed >> 31 );

                /* Pointer used in short term prediction and shaping */
                psLPC_Q14 = psDD.sLPC_Q14;
                psLPC_Q14_offset = Silk_define.NSQ_LPC_BUF_LENGTH() - 1 + i;
                /* Short-term prediction */
                assert( predictLPCOrder >= 10 );            /* check that unrolling works */
                assert( ( predictLPCOrder  & 1 ) == 0 );    /* check that order is even */
//                SKP_assert( ( (SKP_int64)a_Q12 & 3 ) == 0 );    /* check that array starts at 4-byte aligned address */
                /* Partially unrolled */
                LPC_pred_Q10 = Silk_macros.SKP_SMULWB(               psLPC_Q14[ psLPC_Q14_offset+0 ], a_Q12[ a_Q12_offset+0 ] );
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
                    LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psLPC_Q14[ psLPC_Q14_offset-j ], a_Q12[ a_Q12_offset+j]);
                }

                /* Noise shape feedback */
                assert( ( shapingLPCOrder       & 1 ) == 0 );   /* check that order is even */
//                assert( ( (SKP_int64)AR_shp_Q13 & 3 ) == 0 );   /* check that array starts at 4-byte aligned address */
                assert( shapingLPCOrder >= 12 );                /* check that unrolling works */
                /* Partially unrolled */
                n_AR_Q10 = Silk_macros.SKP_SMULWB(           psLPC_Q14[  psLPC_Q14_offset+0 ], AR_shp_Q13[  AR_shp_Q13_offset+0 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-1 ], AR_shp_Q13[  AR_shp_Q13_offset+1 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-2 ], AR_shp_Q13[  AR_shp_Q13_offset+2 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-3 ], AR_shp_Q13[  AR_shp_Q13_offset+3 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-4 ], AR_shp_Q13[  AR_shp_Q13_offset+4 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-5 ], AR_shp_Q13[  AR_shp_Q13_offset+5 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-6 ], AR_shp_Q13[  AR_shp_Q13_offset+6 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-7 ], AR_shp_Q13[  AR_shp_Q13_offset+7 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-8 ], AR_shp_Q13[  AR_shp_Q13_offset+8 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-9 ], AR_shp_Q13[  AR_shp_Q13_offset+9 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-10 ],AR_shp_Q13[  AR_shp_Q13_offset+10 ] );
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[  psLPC_Q14_offset-11 ],AR_shp_Q13[  AR_shp_Q13_offset+11 ] );

                for( j = 12; j < shapingLPCOrder; j ++ ) {
                    n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psLPC_Q14[ psLPC_Q14_offset-j ], AR_shp_Q13[ AR_shp_Q13_offset+j ] );
                }

//                n_AR_Q10 = SKP_RSHIFT( n_AR_Q10, 1 );           /* Q11 -> Q10 */
                n_AR_Q10 = ( n_AR_Q10 >> 1 );           /* Q11 -> Q10 */
                n_AR_Q10 = Silk_macros.SKP_SMLAWB( n_AR_Q10, psDD.LF_AR_Q12, Tilt_Q14 );

                n_LF_Q10   = ( Silk_macros.SKP_SMULWB( psDD.Shape_Q10[ smpl_buf_idx[0] ], LF_shp_Q14 ) << 2 ); 
                n_LF_Q10   = Silk_macros.SKP_SMLAWT( n_LF_Q10, psDD.LF_AR_Q12, LF_shp_Q14 );       

                /* Input minus prediction plus noise feedback                       */
                /* r = x[ i ] - LTP_pred - LPC_pred + n_AR + n_Tilt + n_LF + n_LTP  */
                tmp   = ( LTP_pred_Q14 - n_LTP_Q14 );                       /* Add Q14 stuff */
                tmp   = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( tmp, 4 );                                 /* round to Q10  */
                tmp   = ( tmp + LPC_pred_Q10 );                             /* add Q10 stuff */ 
                tmp   = ( tmp - n_AR_Q10 );                                 /* subtract Q10 stuff */ 
                tmp   = ( tmp - n_LF_Q10 );                                 /* subtract Q10 stuff */ 
                r_Q10 = ( x_Q10[ i ] - tmp );                               /* residual error Q10 */
                
                /* Flip sign depending on dither */
                r_Q10 = ( r_Q10 ^ dither ) - dither;
                r_Q10 = ( r_Q10 - offset_Q10 );
                r_Q10 = Silk_SigProc_FIX.SKP_LIMIT_32( r_Q10, -64 << 10, 64 << 10 );

                /* Find two quantization level candidates and measure their rate-distortion */
                if( r_Q10 < -1536 ) {
                    q1_Q10  = ( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( r_Q10, 10 ) << 10 );
                    r_Q10   = ( r_Q10 - q1_Q10 );
                    rd1_Q10 = ( Silk_macros.SKP_SMLABB( ( -( q1_Q10 + offset_Q10 ) * Lambda_Q10 ), r_Q10, r_Q10 ) >> 10 );
                    rd2_Q10 = ( rd1_Q10 + 1024 );
                    rd2_Q10 = ( rd2_Q10 - Silk_SigProc_FIX.SKP_ADD_LSHIFT32( Lambda_Q10, r_Q10, 1 ) );
                    q2_Q10  = ( q1_Q10 + 1024 );
                } else if( r_Q10 > 512 ) {
                    q1_Q10  = ( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( r_Q10, 10 ) << 10 );
                    r_Q10   = ( r_Q10 - q1_Q10 );
                    rd1_Q10 = ( Silk_macros.SKP_SMLABB( ( ( q1_Q10 + offset_Q10 ) * Lambda_Q10 ), r_Q10, r_Q10 ) >> 10 );
                    rd2_Q10 = ( rd1_Q10 + 1024 );
                    rd2_Q10 = ( rd2_Q10 - Silk_SigProc_FIX.SKP_SUB_LSHIFT32( Lambda_Q10, r_Q10, 1 ) );
                    q2_Q10  = ( q1_Q10 - 1024 );
                } else {            /* r_Q10 >= -1536 && q1_Q10 <= 512 */
                    rr_Q20  = Silk_macros.SKP_SMULBB( offset_Q10, Lambda_Q10 );
                    rd2_Q10 = ( Silk_macros.SKP_SMLABB( rr_Q20, r_Q10, r_Q10 ) >> 10 );
                    rd1_Q10 = ( rd2_Q10 + 1024 );
                    rd1_Q10 = ( rd1_Q10 + Silk_SigProc_FIX.SKP_SUB_RSHIFT32( Silk_SigProc_FIX.SKP_ADD_LSHIFT32( Lambda_Q10, r_Q10, 1 ), rr_Q20, 9 ) );
                    q1_Q10  = -1024;
                    q2_Q10  = 0;
                }

                if( rd1_Q10 < rd2_Q10 ) {
                    psSS[ 0 ].RD_Q10 = ( psDD.RD_Q10 + rd1_Q10 ); 
                    psSS[ 1 ].RD_Q10 = ( psDD.RD_Q10 + rd2_Q10 );
                    psSS[ 0 ].Q_Q10 = q1_Q10;
                    psSS[ 1 ].Q_Q10 = q2_Q10;
                } else {
                    psSS[ 0 ].RD_Q10 = ( psDD.RD_Q10 + rd2_Q10 );
                    psSS[ 1 ].RD_Q10 = ( psDD.RD_Q10 + rd1_Q10 );
                    psSS[ 0 ].Q_Q10 = q2_Q10;
                    psSS[ 1 ].Q_Q10 = q1_Q10;
                }

                /* Update states for best quantization */

                /* Quantized excitation */
                exc_Q10 = ( offset_Q10 + psSS[ 0 ].Q_Q10 );
                exc_Q10 = ( exc_Q10 ^ dither ) - dither;

                /* Add predictions */
                LPC_exc_Q10 = exc_Q10 + Silk_SigProc_FIX.SKP_RSHIFT_ROUND( LTP_pred_Q14, 4 );
                xq_Q10      = ( LPC_exc_Q10 + LPC_pred_Q10 );

                /* Update states */
                sLF_AR_shp_Q10         = (  xq_Q10 - n_AR_Q10 );
                psSS[ 0 ].sLTP_shp_Q10 = (  sLF_AR_shp_Q10 - n_LF_Q10 );
                psSS[ 0 ].LF_AR_Q12    = ( sLF_AR_shp_Q10 << 2 );
                psSS[ 0 ].xq_Q14       = ( xq_Q10 << 4 );
                psSS[ 0 ].LPC_exc_Q16  = ( LPC_exc_Q10 << 6 );

                /* Update states for second best quantization */

                /* Quantized excitation */
                exc_Q10 = ( offset_Q10 + psSS[ 1 ].Q_Q10 );
                exc_Q10 = ( exc_Q10 ^ dither ) - dither;

                /* Add predictions */
                LPC_exc_Q10 = exc_Q10 + Silk_SigProc_FIX.SKP_RSHIFT_ROUND( LTP_pred_Q14, 4 );
                xq_Q10      = ( LPC_exc_Q10 + LPC_pred_Q10 );

                /* Update states */
                sLF_AR_shp_Q10         = (  xq_Q10 - n_AR_Q10 );
                psSS[ 1 ].sLTP_shp_Q10 = (  sLF_AR_shp_Q10 - n_LF_Q10 );
                psSS[ 1 ].LF_AR_Q12    = ( sLF_AR_shp_Q10 << 2 );
                psSS[ 1 ].xq_Q14       = ( xq_Q10 << 4 );
                psSS[ 1 ].LPC_exc_Q16  = ( LPC_exc_Q10 << 6 );
            }

            smpl_buf_idx[0]  = ( smpl_buf_idx[0] - 1 ) & Silk_define.DECISION_DELAY_MASK;                   /* Index to newest samples              */
            last_smple_idx = ( smpl_buf_idx[0] + decisionDelay ) & Silk_define.DECISION_DELAY_MASK;       /* Index to decisionDelay old samples   */

            /* Find winner */
            RDmin_Q10 = psSampleState[ 0 ][ 0 ].RD_Q10;
            Winner_ind = 0;
            for( k = 1; k < nStatesDelayedDecision; k++ ) {
                if( psSampleState[ k ][ 0 ].RD_Q10 < RDmin_Q10 ) {
                    RDmin_Q10   = psSampleState[ k ][ 0 ].RD_Q10;
                    Winner_ind = k;
                }
            }

            /* Increase RD values of expired states */
            Winner_rand_state = psDelDec[ Winner_ind ].RandState[ last_smple_idx ];
            for( k = 0; k < nStatesDelayedDecision; k++ ) {
                if( psDelDec[ k ].RandState[ last_smple_idx ] != Winner_rand_state ) {
                    psSampleState[ k ][ 0 ].RD_Q10 = ( psSampleState[ k ][ 0 ].RD_Q10 + ( Integer.MAX_VALUE >> 4 ) );
                    psSampleState[ k ][ 1 ].RD_Q10 = ( psSampleState[ k ][ 1 ].RD_Q10 + ( Integer.MAX_VALUE >> 4 ) );
                    assert( psSampleState[ k ][ 0 ].RD_Q10 >= 0 );
                }
            }

            /* Find worst in first set and best in second set */
            RDmax_Q10  = psSampleState[ 0 ][ 0 ].RD_Q10;
            RDmin_Q10  = psSampleState[ 0 ][ 1 ].RD_Q10;
            RDmax_ind = 0;
            RDmin_ind = 0;
            for( k = 1; k < nStatesDelayedDecision; k++ ) {
                /* find worst in first set */
                if( psSampleState[ k ][ 0 ].RD_Q10 > RDmax_Q10 ) {
                    RDmax_Q10  = psSampleState[ k ][ 0 ].RD_Q10;
                    RDmax_ind = k;
                }
                /* find best in second set */
                if( psSampleState[ k ][ 1 ].RD_Q10 < RDmin_Q10 ) {
                    RDmin_Q10  = psSampleState[ k ][ 1 ].RD_Q10;
                    RDmin_ind = k;
                }
            }

            /* Replace a state if best from second set outperforms worst in first set */
            if( RDmin_Q10 < RDmax_Q10 ) {
//                SKP_Silk_copy_del_dec_state( &psDelDec[ RDmax_ind ], &psDelDec[ RDmin_ind ], i ); 
                SKP_Silk_copy_del_dec_state( psDelDec[ RDmax_ind ], psDelDec[ RDmin_ind ], i );
//TODO:how to copy a struct ???                
//                SKP_memcpy( &psSampleState[ RDmax_ind ][ 0 ], &psSampleState[ RDmin_ind ][ 1 ], sizeof( NSQ_sample_struct ) );
                psSampleState[ RDmax_ind ][ 0 ] = (NSQ_sample_struct) psSampleState[ RDmin_ind ][ 1 ].clone();
            }

            /* Write samples from winner to output and long-term filter states */
            psDD = psDelDec[ Winner_ind ];
            if( subfr > 0 || i >= decisionDelay ) {
                q[  q_offset + i - decisionDelay ] = ( byte )( psDD.Q_Q10[ last_smple_idx ] >> 10 );
                xq[ xq_offset + i - decisionDelay ] = ( short )Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( 
                    Silk_macros.SKP_SMULWW( psDD.Xq_Q10[ last_smple_idx ], psDD.Gain_Q16[ last_smple_idx ] ), 10 ) );
                NSQ.sLTP_shp_Q10[ NSQ.sLTP_shp_buf_idx - decisionDelay ] = psDD.Shape_Q10[ last_smple_idx ];
                sLTP_Q16[          NSQ.sLTP_buf_idx     - decisionDelay ] = psDD.Pred_Q16[  last_smple_idx ];
            }
            NSQ.sLTP_shp_buf_idx++;
            NSQ.sLTP_buf_idx++;

            /* Update states */
            for( k = 0; k < nStatesDelayedDecision; k++ ) {
                psDD                                     = psDelDec[ k ];
//TODO: psSS is an array of reference rather than a reference.                
//                psSS                                     = &psSampleState[ k ][ 0 ];
                psSS                                     = psSampleState[ k ];
                psDD.LF_AR_Q12                          = psSS[0].LF_AR_Q12;
                psDD.sLPC_Q14[ Silk_define.NSQ_LPC_BUF_LENGTH() + i ] = psSS[0].xq_Q14;
                psDD.Xq_Q10[    smpl_buf_idx[0] ]         = ( psSS[0].xq_Q14 >> 4 );
                psDD.Q_Q10[     smpl_buf_idx[0] ]         = psSS[0].Q_Q10;
                psDD.Pred_Q16[  smpl_buf_idx[0] ]         = psSS[0].LPC_exc_Q16;
                psDD.Shape_Q10[ smpl_buf_idx[0] ]         = psSS[0].sLTP_shp_Q10;
                psDD.Seed                               = Silk_SigProc_FIX.SKP_ADD_RSHIFT32( psDD.Seed, psSS[0].Q_Q10, 10 );
                psDD.RandState[ smpl_buf_idx[0] ]         = psDD.Seed;
                psDD.RD_Q10                             = psSS[0].RD_Q10;
                psDD.Gain_Q16[  smpl_buf_idx[0] ]         = Gain_Q16;
            }
        }
        /* Update LPC states */
        for( k = 0; k < nStatesDelayedDecision; k++ ) {
            psDD = psDelDec[ k ];
            System.arraycopy(psDD.sLPC_Q14, length, psDD.sLPC_Q14, 0, Silk_define.NSQ_LPC_BUF_LENGTH());
        }
    }

    /**
     * 
     * @param NSQ NSQ state
     * @param psDelDec Delayed decision states
     * @param x Input in Q0
     * @param x_offset offset of valid data.
     * @param x_sc_Q10 nput scaled with 1/Gain in Q10
     * @param length Length of input
     * @param sLTP Re-whitened LTP state in Q0
     * @param sLTP_Q16 LTP state matching scaled input
     * @param subfr Subframe number
     * @param nStatesDelayedDecision Number of del dec states
     * @param smpl_buf_idx Index to newest samples in buffers
     * @param LTP_scale_Q14 LTP state scaling
     * @param Gains_Q16
     * @param pitchL Pitch lag
     */
    static void SKP_Silk_nsq_del_dec_scale_states(
            SKP_Silk_nsq_state  NSQ,                   /* I/O  NSQ state                           */
            NSQ_del_dec_struct  psDelDec[],             /* I/O  Delayed decision states             */
            final short         x[],                    /* I    Input in Q0                         */
            int                 x_offset,
            int                 x_sc_Q10[],             /* O    Input scaled with 1/Gain in Q10     */
            int                 length,                 /* I    Length of input                     */
            short               sLTP[],                 /* I    Re-whitened LTP state in Q0         */
            int                 sLTP_Q16[],             /* O    LTP state matching scaled input     */
            int                 subfr,                  /* I    Subframe number                     */
            int                 nStatesDelayedDecision, /* I    Number of del dec states            */
            int                 smpl_buf_idx,           /* I    Index to newest samples in buffers  */
            final int           LTP_scale_Q14,          /* I    LTP state scaling                   */
            final int           Gains_Q16[],  /* I                                        */
            final int           pitchL[]      /* I    Pitch lag                           */
    )
    {
        int            i, k, scale_length, lag;
        int            inv_gain_Q16, gain_adj_Q16, inv_gain_Q32;
        NSQ_del_dec_struct psDD;

        inv_gain_Q16 = ( Integer.MAX_VALUE / ( Gains_Q16[ subfr ] >> 1 ) );
        inv_gain_Q16 = ( inv_gain_Q16 < Short.MAX_VALUE ?  inv_gain_Q16:Short.MAX_VALUE);
        lag          = pitchL[ subfr ];
        /* After rewhitening the LTP state is un-scaled. So scale with inv_gain_Q16 */
        if( NSQ.rewhite_flag != 0) {
            inv_gain_Q32 = ( inv_gain_Q16 << 16 );
            if( subfr == 0 ) {
                /* Do LTP downscaling */
                inv_gain_Q32 = ( Silk_macros.SKP_SMULWB( inv_gain_Q32, LTP_scale_Q14 ) << 2 );
            }
            for( i = NSQ.sLTP_buf_idx - lag - Silk_define.LTP_ORDER / 2; i < NSQ.sLTP_buf_idx; i++ ) {
                assert( i < Silk_define.MAX_FRAME_LENGTH );
                sLTP_Q16[ i ] = Silk_macros.SKP_SMULWB( inv_gain_Q32, sLTP[ i ] );
            }
        }

        /* Adjust for changing gain */
        if( inv_gain_Q16 != NSQ.prev_inv_gain_Q16 ) {
            gain_adj_Q16 = Silk_Inlines.SKP_DIV32_varQ( inv_gain_Q16, NSQ.prev_inv_gain_Q16, 16 );

            for( k = 0; k < nStatesDelayedDecision; k++ ) {
                psDD = psDelDec[ k ];
                
                /* Scale scalar states */
                psDD.LF_AR_Q12 = Silk_macros.SKP_SMULWW( gain_adj_Q16, psDD.LF_AR_Q12 );
                
                /* scale short term state */
                for( i = 0; i < Silk_define.NSQ_LPC_BUF_LENGTH(); i++ ) {
                    psDD.sLPC_Q14[ Silk_define.NSQ_LPC_BUF_LENGTH() - i - 1 ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, 
                            psDD.sLPC_Q14[ Silk_define.NSQ_LPC_BUF_LENGTH() - i - 1 ] );
                }
                for( i = 0; i < Silk_define.DECISION_DELAY; i++ ) {
                    psDD.Pred_Q16[  i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, psDD.Pred_Q16[  i ] );
                    psDD.Shape_Q10[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, psDD.Shape_Q10[ i ] );
                }
            }

            /* Scale long term shaping state */

            /* Calculate length to be scaled, Worst case: Next frame is voiced with max lag */
            scale_length = length * Silk_define.NB_SUBFR;                                               /* aprox max lag */
            scale_length = scale_length - Silk_macros.SKP_SMULBB( Silk_define.NB_SUBFR - ( subfr + 1 ), length );   /* subtract samples that will be too old in next frame */
            scale_length = Math.max( scale_length, lag + Silk_define.LTP_ORDER );                    /* make sure to scale whole pitch period if voiced */

            for( i = NSQ.sLTP_shp_buf_idx - scale_length; i < NSQ.sLTP_shp_buf_idx; i++ ) {
                NSQ.sLTP_shp_Q10[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, NSQ.sLTP_shp_Q10[ i ] );
            }

            /* Scale LTP predict state */
            if( NSQ.rewhite_flag == 0 ) {
                for( i = NSQ.sLTP_buf_idx - lag - Silk_define.LTP_ORDER / 2; i < NSQ.sLTP_buf_idx; i++ ) {
                    sLTP_Q16[ i ] = Silk_macros.SKP_SMULWW( gain_adj_Q16, sLTP_Q16[ i ] );
                }
            }
        }

        /* Scale input */
        for( i = 0; i < length; i++ ) {
            x_sc_Q10[ i ] = (Silk_macros.SKP_SMULBB( x[ x_offset + i ], ( short )inv_gain_Q16 ) >> 6 );
        }

        /* save inv_gain */
        assert( inv_gain_Q16 != 0 );
        NSQ.prev_inv_gain_Q16 = inv_gain_Q16;
    }

    /**
     * 
     * @param DD_dst Dst del dec state
     * @param DD_src Src del dec state
     * @param LPC_state_idx Index to LPC buffer
     */
    static void SKP_Silk_copy_del_dec_state(
            NSQ_del_dec_struct  DD_dst,                /* I    Dst del dec state                   */
            NSQ_del_dec_struct  DD_src,                /* I    Src del dec state                   */
            int                 LPC_state_idx           /* I    Index to LPC buffer                 */
    )
    {        
        System.arraycopy(DD_src.RandState, 0, DD_dst.RandState, 0, Silk_define.DECISION_DELAY);
        System.arraycopy(DD_src.Q_Q10, 0, DD_dst.Q_Q10, 0, Silk_define.DECISION_DELAY);
        System.arraycopy(DD_src.Pred_Q16, 0, DD_dst.Pred_Q16, 0, Silk_define.DECISION_DELAY);
        System.arraycopy(DD_src.Shape_Q10, 0, DD_dst.Shape_Q10, 0, Silk_define.DECISION_DELAY);
        System.arraycopy(DD_src.Xq_Q10, 0, DD_dst.Xq_Q10, 0, Silk_define.DECISION_DELAY);

        System.arraycopy(DD_src.sLPC_Q14, LPC_state_idx, DD_dst.sLPC_Q14, LPC_state_idx, Silk_define.NSQ_LPC_BUF_LENGTH());
        DD_dst.LF_AR_Q12 = DD_src.LF_AR_Q12;
        DD_dst.Seed      = DD_src.Seed;
        DD_dst.SeedInit  = DD_src.SeedInit;
        DD_dst.RD_Q10    = DD_src.RD_Q10;
    }
}
