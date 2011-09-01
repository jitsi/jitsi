/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * SILK PNG.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_PLC
{
    static final int BWE_COEF_Q16 =                   64880;           /* 0.99 in Q16                      */
    static final int V_PITCH_GAIN_START_MIN_Q14 =     11469;           /* 0.7 in Q14                       */
    static final int V_PITCH_GAIN_START_MAX_Q14 =     15565;           /* 0.95 in Q14                      */
    static final int MAX_PITCH_LAG_MS =               18;
    static final int SA_THRES_Q8   =                  50;
    static final boolean USE_SINGLE_TAP  =            true;
    static final int RAND_BUF_SIZE  =                 128;
    static final int RAND_BUF_MASK  =                 (RAND_BUF_SIZE - 1);
    static final int LOG2_INV_LPC_GAIN_HIGH_THRES =   3;               /* 2^3 = 8 dB LPC gain              */
    static final int LOG2_INV_LPC_GAIN_LOW_THRES =    8;               /* 2^8 = 24 dB LPC gain             */
    static final int PITCH_DRIFT_FAC_Q16   =          655;            /* 0.01 in Q16                      */

    static final int  NB_ATT =  2;

    static short[] HARM_ATT_Q15              = { 32440, 31130 }; /* 0.99, 0.95 */
    static short[] PLC_RAND_ATTENUATE_V_Q15  = { 31130, 26214 }; /* 0.95, 0.8 */
    static short[] PLC_RAND_ATTENUATE_UV_Q15 = { 32440, 29491 }; /* 0.99, 0.9 */
    
    /**
     * PLC reset.
     * @param psDec Decoder state.
     */
    static void SKP_Silk_PLC_Reset(
        SKP_Silk_decoder_state      psDec              /* I/O Decoder state        */
    )
    {
        psDec.sPLC.pitchL_Q8 = ( psDec.frame_length >> 1 );
    }

    /**
     * 
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param signal Concealed signal.
     * @param signal_offset offset of the valid data.
     * @param length Length of residual.
     * @param lost Loss flag.
     */
    static void SKP_Silk_PLC(
            SKP_Silk_decoder_state      psDec,             /* I Decoder state          */
            SKP_Silk_decoder_control    psDecCtrl,         /* I Decoder control        */
            short                       signal[],           /* O Concealed signal       */
            int                         signal_offset,
            int                            length,             /* I length of residual     */
            int                          lost                /* I Loss flag              */
    )
    {
        /* PLC control function */
        if( psDec.fs_kHz != psDec.sPLC.fs_kHz ) {
            SKP_Silk_PLC_Reset( psDec );
            psDec.sPLC.fs_kHz = psDec.fs_kHz;
        }

        if( lost != 0 ) {
            /****************************/
            /* Generate Signal          */
            /****************************/
            SKP_Silk_PLC_conceal( psDec, psDecCtrl, signal, signal_offset, length );
        } else {
            /****************************/
            /* Update state             */
            /****************************/
            SKP_Silk_PLC_update( psDec, psDecCtrl, signal, signal_offset, length );
        }
    }
    
    /**
     * Update state of PLC
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param signal 
     * @param signal_offset
     * @param length
     */
    static void SKP_Silk_PLC_update(
            SKP_Silk_decoder_state     psDec,             /* (I/O) Decoder state          */
            SKP_Silk_decoder_control   psDecCtrl,         /* (I/O) Decoder control        */
            short                      signal[],
            int                        signal_offset,
            int                    length
    )
    {
        int LTP_Gain_Q14, temp_LTP_Gain_Q14;
        int   i, j;
        SKP_Silk_PLC_struct psPLC;

        psPLC = psDec.sPLC;

        /* Update parameters used in case of packet loss */
        psDec.prev_sigtype = psDecCtrl.sigtype;
        LTP_Gain_Q14 = 0;
        if( psDecCtrl.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            /* Find the parameters for the last subframe which contains a pitch pulse */
            for( j = 0; j * psDec.subfr_length  < psDecCtrl.pitchL[ Silk_define.NB_SUBFR - 1 ]; j++ ) {
                temp_LTP_Gain_Q14 = 0;
                for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                    temp_LTP_Gain_Q14 += psDecCtrl.LTPCoef_Q14[ ( Silk_define.NB_SUBFR - 1 - j ) * Silk_define.LTP_ORDER  + i ];
                }
                if( temp_LTP_Gain_Q14 > LTP_Gain_Q14 ) {
                    LTP_Gain_Q14 = temp_LTP_Gain_Q14;
                    System.arraycopy(psDecCtrl.LTPCoef_Q14, Silk_macros.SKP_SMULBB( Silk_define.NB_SUBFR - 1 - j, Silk_define.LTP_ORDER ),
                            psPLC.LTPCoef_Q14, 0, Silk_define.LTP_ORDER);
                    psPLC.pitchL_Q8 = ( psDecCtrl.pitchL[ Silk_define.NB_SUBFR - 1 - j ] << 8 );
                }
            }

            if(USE_SINGLE_TAP)
            {
                Arrays.fill(psPLC.LTPCoef_Q14, 0, Silk_define.LTP_ORDER, (short)0);
                 psPLC.LTPCoef_Q14[ Silk_define.LTP_ORDER / 2 ] = (short) LTP_Gain_Q14;
            }
            
            /* Limit LT coefs */
            if( LTP_Gain_Q14 < V_PITCH_GAIN_START_MIN_Q14 ) {
                int   scale_Q10;
                int tmp;

                tmp = ( V_PITCH_GAIN_START_MIN_Q14 << 10 );
                
                scale_Q10 = ( tmp / Math.max( LTP_Gain_Q14, 1 ) );
                
                for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                     psPLC.LTPCoef_Q14[ i ] = (short) ( Silk_macros.SKP_SMULBB( psPLC.LTPCoef_Q14[ i ], scale_Q10 ) >> 10 );
                }
            } else if( LTP_Gain_Q14 > V_PITCH_GAIN_START_MAX_Q14 ) {
                int   scale_Q14;
                int tmp;

                tmp = ( V_PITCH_GAIN_START_MAX_Q14 << 14 );
                scale_Q14 = ( tmp / Math.max( LTP_Gain_Q14, 1 ) );
                
                for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                    psPLC.LTPCoef_Q14[ i ] = (short) ( Silk_macros.SKP_SMULBB( psPLC.LTPCoef_Q14[ i ], scale_Q14 ) << 14 );
                }
            }
        } else {
             psPLC.pitchL_Q8 = ( Silk_macros.SKP_SMULBB( psDec.fs_kHz, 18 ) << 8 );
             Arrays.fill(psPLC.LTPCoef_Q14, 0, Silk_define.LTP_ORDER, (short)0);
        }

        /* Save LPC coeficients */
        System.arraycopy(psDecCtrl.PredCoef_Q12[1], 0, psPLC.prevLPC_Q12, 0, psDec.LPC_order);
        psPLC.prevLTP_scale_Q14 = (short) psDecCtrl.LTP_scale_Q14;

        /* Save Gains */
        System.arraycopy(psDecCtrl.Gains_Q16, 0, psPLC.prevGain_Q16, 0, Silk_define.NB_SUBFR);
        
    }
    
    /**
     * 
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param signal concealed signal.
     * @param signal_offset offset of the valid data.
     * @param length Length of residual.
     */
    static void SKP_Silk_PLC_conceal(
            SKP_Silk_decoder_state      psDec,             /* I/O Decoder state */
            SKP_Silk_decoder_control    psDecCtrl,         /* I/O Decoder control */
            short                   signal[],           /* O concealed signal */
            int                     signal_offset,
            int                     length              /* I length of residual */
    )
    {
        int   i, j, k;
        short[] B_Q14;
        short[] exc_buf = new short[Silk_define.MAX_FRAME_LENGTH];
        short[] exc_buf_ptr;
        int     exc_buf_ptr_offset;
        
        short rand_scale_Q14;
        short[] A_Q12_tmp = new short[Silk_define.MAX_LPC_ORDER];
        
        
        int rand_seed, harm_Gain_Q15, rand_Gain_Q15;
        int   lag, idx, shift1, shift2;
        int shift1_ptr[] = new int[1];
        int shift2_ptr[] = new int[1];
                                   
        int energy1, energy2;
        int energy1_ptr[] = new int[1];
        int energy2_ptr[] = new int[1];
        
        int[]  rand_ptr, pred_lag_ptr;
        int    rand_ptr_offset, pred_lag_ptr_offset;
        
        int[] sig_Q10 = new int[Silk_define.MAX_FRAME_LENGTH];
        int[] sig_Q10_ptr;
        int   sig_Q10_ptr_offset;
        
        int   LPC_exc_Q10, LPC_pred_Q10,  LTP_pred_Q14;
        
        SKP_Silk_PLC_struct psPLC;
        psPLC = psDec.sPLC;

        /* Update LTP buffer */
        System.arraycopy(psDec.sLTP_Q16, psDec.frame_length, psDec.sLTP_Q16, 0, psDec.frame_length);
        
        /* LPC concealment. Apply BWE to previous LPC */
        Silk_bwexpander.SKP_Silk_bwexpander( psPLC.prevLPC_Q12, psDec.LPC_order, BWE_COEF_Q16 );

        /* Find random noise component */
        /* Scale previous excitation signal */
        exc_buf_ptr = exc_buf;
        exc_buf_ptr_offset = 0;
        
        for( k = ( Silk_define.NB_SUBFR >> 1 ); k < Silk_define.NB_SUBFR; k++ ) {
            for( i = 0; i < psDec.subfr_length; i++ ) {
                exc_buf_ptr[exc_buf_ptr_offset + i ] = ( short )( Silk_macros.SKP_SMULWW( psDec.exc_Q10[ i + k * psDec.subfr_length ], psPLC.prevGain_Q16[ k ] ) >> 10 );
                
            }
            exc_buf_ptr_offset += psDec.subfr_length;
        }
        /* Find the subframe with lowest energy of the last two and use that as random noise generator */ 
        Silk_sum_sqr_shift.SKP_Silk_sum_sqr_shift( energy1_ptr, shift1_ptr, exc_buf, 0, psDec.subfr_length );
        energy1 = energy1_ptr[0];
        shift1  = shift1_ptr[0];
        
        Silk_sum_sqr_shift.SKP_Silk_sum_sqr_shift( energy2_ptr, shift2_ptr, exc_buf,  psDec.subfr_length , psDec.subfr_length );
        energy2 = energy2_ptr[0];
        shift2  = shift2_ptr[0];
        
        if( ( energy1 >> shift2 ) < ( energy1 >> shift2 ) ) {
            /* First sub-frame has lowest energy */
            rand_ptr = psDec.exc_Q10;
            rand_ptr_offset = Math.max( 0, 3 * psDec.subfr_length - RAND_BUF_SIZE );
        } else {
            /* Second sub-frame has lowest energy */
            rand_ptr = psDec.exc_Q10;
            rand_ptr_offset = Math.max(0, psDec.frame_length - RAND_BUF_SIZE);
        }

        /* Setup Gain to random noise component */ 
        B_Q14          = psPLC.LTPCoef_Q14;
        rand_scale_Q14 = psPLC.randScale_Q14;

        /* Setup attenuation gains */
        harm_Gain_Q15 = HARM_ATT_Q15[ Math.min( NB_ATT - 1, psDec.lossCnt ) ];
        if( psDec.prev_sigtype == Silk_define.SIG_TYPE_VOICED ) {
            rand_Gain_Q15 = PLC_RAND_ATTENUATE_V_Q15[  Math.min( NB_ATT - 1, psDec.lossCnt ) ];
        } else {
            rand_Gain_Q15 = PLC_RAND_ATTENUATE_UV_Q15[ Math.min( NB_ATT - 1, psDec.lossCnt ) ];
        }

        /* First Lost frame */
        if( psDec.lossCnt == 0 ) {
            rand_scale_Q14 = (1 << 14 );
        
            /* Reduce random noise Gain for voiced frames */
            if( psDec.prev_sigtype == Silk_define.SIG_TYPE_VOICED ) {
                for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                    rand_scale_Q14 -= B_Q14[ i ];
                }
                rand_scale_Q14 = (short) Math.max( 3277, rand_scale_Q14 ); /* 0.2 */
                rand_scale_Q14 = ( short )( Silk_macros.SKP_SMULBB( rand_scale_Q14, psPLC.prevLTP_scale_Q14 ) >> 14 );
            }

            /* Reduce random noise for unvoiced frames with high LPC gain */
            if( psDec.prev_sigtype == Silk_define.SIG_TYPE_UNVOICED ) {
                int invGain_Q30, down_scale_Q30;
                int invGain_Q30_ptr[] = new int[1];
                Silk_LPC_inv_pred_gain.SKP_Silk_LPC_inverse_pred_gain( invGain_Q30_ptr, psPLC.prevLPC_Q12, psDec.LPC_order );
                invGain_Q30 = invGain_Q30_ptr[0];
                
                down_scale_Q30 = Math.min( ( ( 1 << 30 ) >> LOG2_INV_LPC_GAIN_HIGH_THRES ), invGain_Q30 );
                down_scale_Q30 = Math.max( ( ( 1 << 30 ) >> LOG2_INV_LPC_GAIN_LOW_THRES ), down_scale_Q30 );
                down_scale_Q30 = ( down_scale_Q30 << LOG2_INV_LPC_GAIN_HIGH_THRES );
                
                rand_Gain_Q15 = ( Silk_macros.SKP_SMULWB( down_scale_Q30, rand_Gain_Q15 ) >> 14 );
            }
        }

        rand_seed           = psPLC.rand_seed;
        lag                 = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( psPLC.pitchL_Q8, 8 );
        psDec.sLTP_buf_idx = psDec.frame_length;

        /***************************/
        /* LTP synthesis filtering */
        /***************************/
        sig_Q10_ptr = sig_Q10;
        sig_Q10_ptr_offset = 0;
        
        
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            /* Setup pointer */
            pred_lag_ptr = psDec.sLTP_Q16;
            pred_lag_ptr_offset = psDec.sLTP_buf_idx - lag + Silk_define.LTP_ORDER / 2;
            
            for( i = 0; i < psDec.subfr_length; i++ ) {
                rand_seed = Silk_SigProc_FIX.SKP_RAND( rand_seed );
                idx = ( rand_seed >> 25 ) & RAND_BUF_MASK;

                /* Unrolled loop */
//                LTP_pred_Q14 = Silk_macros.SKP_SMULWB(               pred_lag_ptr[ pred_lag_ptr_offset + 0 ], B_Q14[ 0 ] );
//TODO: array bonds check???                
                LTP_pred_Q14 = Silk_macros.SKP_SMULWB(               pred_lag_ptr[ pred_lag_ptr_offset + 0 ], B_Q14[ 0 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset - 1 ], B_Q14[ 1 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset - 2 ], B_Q14[ 2 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset - 3 ], B_Q14[ 3 ] );
                LTP_pred_Q14 = Silk_macros.SKP_SMLAWB( LTP_pred_Q14, pred_lag_ptr[ pred_lag_ptr_offset - 4 ], B_Q14[ 4 ] );
                pred_lag_ptr_offset++;
                
                /* Generate LPC residual */
                LPC_exc_Q10 = ( Silk_macros.SKP_SMULWB( rand_ptr[rand_ptr_offset + idx ], rand_scale_Q14 ) << 2 ); /* Random noise part */
                LPC_exc_Q10 = ( LPC_exc_Q10 + Silk_SigProc_FIX.SKP_RSHIFT_ROUND( LTP_pred_Q14, 4 ) );  /* Harmonic part */
                
                /* Update states */
                psDec.sLTP_Q16[ psDec.sLTP_buf_idx ] = ( LPC_exc_Q10 << 6 );
                psDec.sLTP_buf_idx++;
                    
                /* Save LPC residual */
                sig_Q10_ptr[ sig_Q10_ptr_offset + i ] = LPC_exc_Q10;
            }
            sig_Q10_ptr_offset += psDec.subfr_length;
            /* Gradually reduce LTP gain */
            for( j = 0; j < Silk_define.LTP_ORDER; j++ ) {
                B_Q14[ j ] = (short) ( Silk_macros.SKP_SMULBB( harm_Gain_Q15, B_Q14[ j ] )>> 15 );
            }
            /* Gradually reduce excitation gain */
            rand_scale_Q14 = (short) ( Silk_macros.SKP_SMULBB( rand_scale_Q14, rand_Gain_Q15 ) >> 15 );

            /* Slowly increase pitch lag */
            psPLC.pitchL_Q8 += Silk_macros.SKP_SMULWB( psPLC.pitchL_Q8, PITCH_DRIFT_FAC_Q16 );
            psPLC.pitchL_Q8 = Math.min( psPLC.pitchL_Q8, ( Silk_macros.SKP_SMULBB( MAX_PITCH_LAG_MS, psDec.fs_kHz ) << 8 ) );
            lag = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( psPLC.pitchL_Q8, 8 );
        }

        /***************************/
        /* LPC synthesis filtering */
        /***************************/
        sig_Q10_ptr = sig_Q10;
        sig_Q10_ptr_offset = 0;
        /* Preload LPC coeficients to array on stack. Gives small performance gain */
        System.arraycopy(psPLC.prevLPC_Q12, 0, A_Q12_tmp, 0, psDec.LPC_order);
        Silk_typedef.SKP_assert( psDec.LPC_order >= 10 ); /* check that unrolling works */
        
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            for( i = 0; i < psDec.subfr_length; i++ ){
                /* partly unrolled */
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

                for( j = 10; j < psDec.LPC_order; j++ ) {
                    LPC_pred_Q10 = Silk_macros.SKP_SMLAWB( LPC_pred_Q10, psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i - j - 1 ], A_Q12_tmp[ j ] );
                }
                /* Add prediction to LPC residual */
                sig_Q10_ptr[ sig_Q10_ptr_offset = i ] = ( sig_Q10_ptr[ sig_Q10_ptr_offset +i ] + LPC_pred_Q10 );
                    
                /* Update states */
                psDec.sLPC_Q14[ Silk_define.MAX_LPC_ORDER + i ] = ( sig_Q10_ptr[sig_Q10_ptr_offset+ i ] << 4 );
            }
            sig_Q10_ptr_offset  += psDec.subfr_length;
            /* Update LPC filter state */
            System.arraycopy(psDec.sLPC_Q14, psDec.subfr_length, psDec.sLPC_Q14, 0, Silk_define.MAX_LPC_ORDER);
        }

        /* Scale with Gain */
        for( i = 0; i < psDec.frame_length; i++ ) {
            signal[ signal_offset + i ] = ( short )Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( Silk_macros.SKP_SMULWW( sig_Q10[ i ], psPLC.prevGain_Q16[ Silk_define.NB_SUBFR - 1 ] ), 10 ) );
        }

        /**************************************/
        /* Update states                      */
        /**************************************/
        psPLC.rand_seed     = rand_seed;
        psPLC.randScale_Q14 = rand_scale_Q14;
        for( i = 0; i < Silk_define.NB_SUBFR; i++ ) {
            psDecCtrl.pitchL[ i ] = lag;
        }
    }

    /**
     * Glues concealed frames with new good recieved frames.
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param signal signal.
     * @param signal_offset offset of the valid data.
     * @param length length of the residual.
     */
    static void SKP_Silk_PLC_glue_frames(
            SKP_Silk_decoder_state      psDec,             /* I/O decoder state    */
            SKP_Silk_decoder_control    psDecCtrl,         /* I/O Decoder control  */
            short                       signal[],           /* I/O signal           */
            int                            signal_offset,
            int                         length              /* I length of residual */
    )
    {
        int   i, energy_shift;
        int energy;
        int energy_ptr[] = new int[1];
        int energy_shift_ptr[] = new int[1];
        
        SKP_Silk_PLC_struct psPLC;
        psPLC = psDec.sPLC;

        if( psDec.lossCnt != 0) {
            /* Calculate energy in concealed residual */
            int[] conc_energy_ptr = new int[1];
            int[] conc_energy_shift_ptr = new int[1];
            Silk_sum_sqr_shift.SKP_Silk_sum_sqr_shift( conc_energy_ptr, conc_energy_shift_ptr, signal, signal_offset, length );
            psPLC.conc_energy = conc_energy_ptr[0];
            psPLC.conc_energy_shift = conc_energy_shift_ptr[0];
            
            psPLC.last_frame_lost = 1;
        } else {
            if( psDec.sPLC.last_frame_lost != 0 ) {
                /* Calculate residual in decoded signal if last frame was lost */
                Silk_sum_sqr_shift.SKP_Silk_sum_sqr_shift( energy_ptr, energy_shift_ptr, signal, signal_offset, length );
                energy = energy_ptr[0];
                energy_shift = energy_shift_ptr[0];
                

                /* Normalize energies */
                if( energy_shift > psPLC.conc_energy_shift ) {
                    psPLC.conc_energy = ( psPLC.conc_energy >> energy_shift - psPLC.conc_energy_shift );
                } else if( energy_shift < psPLC.conc_energy_shift ) {
                    energy = ( energy >> psPLC.conc_energy_shift - energy_shift );
                }

                /* Fade in the energy difference */
                if( energy > psPLC.conc_energy ) {
                    int frac_Q24, LZ;
                    int gain_Q12, slope_Q12;

                    LZ = Silk_macros.SKP_Silk_CLZ32( psPLC.conc_energy );
                    LZ = LZ - 1;
                    psPLC.conc_energy = ( psPLC.conc_energy << LZ );
                    energy = ( energy >> Math.max( 24 - LZ, 0 ) );
                    
                    frac_Q24 = ( psPLC.conc_energy / Math.max( energy, 1 ) );
                    
                    gain_Q12 = Silk_Inlines.SKP_Silk_SQRT_APPROX( frac_Q24 );
                    slope_Q12 = ( ( 1 << 12 ) - gain_Q12 / length );

                    for( i = 0; i < length; i++ ) {
                        signal[signal_offset + i ] = (short) ( ( gain_Q12 * signal[ signal_offset + i ] ) >> 12 );
                        gain_Q12 += slope_Q12;
                        gain_Q12 = Math.min( gain_Q12, ( 1 << 12 ) );
                    }
                }
            }
            psPLC.last_frame_lost = 0;

        }
    }
}
