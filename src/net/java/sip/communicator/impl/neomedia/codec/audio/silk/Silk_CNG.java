/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * SILK CNG.
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_CNG 
{
    /**
     * Generates excitation for CNG LPC synthesis.
     * @param residual CNG residual signal Q0.
     * @param residual_offset offset of the valid data.
     * @param exc_buf_Q10 Random samples buffer Q10.
     * @param exc_buf_Q10_offset offset of the valid data.
     * @param Gain_Q16 Gain to apply
     * @param length Length
     * @param rand_seed Seed to random index generator
     * 
     */
    static void SKP_Silk_CNG_exc(
            short                     residual[],         /* O    CNG residual signal Q0                      */
            int                       residual_offset,
            int                       exc_buf_Q10[],      /* I    Random samples buffer Q10                   */
            int                       exc_buf_Q10_offset,
            int                       Gain_Q16,           /* I    Gain to apply                               */
            int                       length,             /* I    Length                                      */
            int[]                     rand_seed          /* I/O  Seed to random index generator              */
    )
    {
        int seed;
        int   i, idx, exc_mask;

        exc_mask = Silk_define.CNG_BUF_MASK_MAX;
        while( exc_mask > length ) {
             exc_mask = ( exc_mask >> 1 );
        }

        seed = rand_seed[0];
        
        for( i = 0; i < length; i++ ) {
            seed = Silk_SigProc_FIX.SKP_RAND( seed );
            idx = ( ( seed >> 24 ) & exc_mask );
            Silk_typedef.SKP_assert( idx >= 0 );
            Silk_typedef.SKP_assert( idx <= Silk_define.CNG_BUF_MASK_MAX );
            residual[ residual_offset+i ] = ( short )Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( Silk_macros.SKP_SMULWW( exc_buf_Q10[ idx ], Gain_Q16 ), 10 ) );
        }
        rand_seed[0] = seed;
    }

    /**
     * Reset CNG.
     * @param psDec Decoder state.
     */
    static void SKP_Silk_CNG_Reset(
            SKP_Silk_decoder_state     psDec              /* I/O  Decoder state                               */
    )
    {
        int i, NLSF_step_Q15, NLSF_acc_Q15;

        NLSF_step_Q15 = ( Silk_typedef.SKP_int16_MAX / (psDec.LPC_order + 1) );
        NLSF_acc_Q15 = 0;
        for( i = 0; i < psDec.LPC_order; i++ ) {
            NLSF_acc_Q15 += NLSF_step_Q15;
            psDec.sCNG.CNG_smth_NLSF_Q15[ i ] = NLSF_acc_Q15;
        }
        psDec.sCNG.CNG_smth_Gain_Q16 = 0;
        psDec.sCNG.rand_seed = 3176576;
    }

    /**
     * Updates CNG estimate, and applies the CNG when packet was lost.
     * @param psDec Decoder state.
     * @param psDecCtrl Decoder control.
     * @param signal Signal.
     * @param signal_offset offset of the valid data.
     * @param length Length of residual.
     */
    static void SKP_Silk_CNG(
            SKP_Silk_decoder_state      psDec,             /* I/O  Decoder state                               */
            SKP_Silk_decoder_control    psDecCtrl,         /* I/O  Decoder control                             */
            short                       signal[],           /* I/O  Signal                                      */
            int                            signal_offset,
            int                         length              /* I    Length of residual                          */
    )
    {
        int   i, subfr;
        int tmp_32, Gain_Q26, max_Gain_Q16;
        short[] LPC_buf = new short[Silk_define.MAX_LPC_ORDER];
        short[] CNG_sig = new short[Silk_define.MAX_FRAME_LENGTH];
        
        SKP_Silk_CNG_struct  psCNG;
        
        psCNG = psDec.sCNG;

        if( psDec.fs_kHz != psCNG.fs_kHz ) {
            /* Reset state */
            SKP_Silk_CNG_Reset( psDec );

            psCNG.fs_kHz = psDec.fs_kHz;
        }
        if( psDec.lossCnt == 0 && psDec.vadFlag == Silk_define.NO_VOICE_ACTIVITY ) {
            /* Update CNG parameters */

            /* Smoothing of LSF's  */
            for( i = 0; i < psDec.LPC_order; i++ ) {
                psCNG.CNG_smth_NLSF_Q15[ i ] += Silk_macros.SKP_SMULWB( psDec.prevNLSF_Q15[ i ] - psCNG.CNG_smth_NLSF_Q15[ i ], Silk_define.CNG_NLSF_SMTH_Q16 );
            }
            /* Find the subframe with the highest gain */
            max_Gain_Q16 = 0;
            subfr        = 0;
            for( i = 0; i < Silk_define.NB_SUBFR; i++ ) {
                if( psDecCtrl.Gains_Q16[ i ] > max_Gain_Q16 ) {
                    max_Gain_Q16 = psDecCtrl.Gains_Q16[ i ];
                    subfr        = i;
                }
            }
            /* Update CNG excitation buffer with excitation from this subframe */
            System.arraycopy(psCNG.CNG_exc_buf_Q10, 0, psCNG.CNG_exc_buf_Q10, psDec.subfr_length, ( Silk_define.NB_SUBFR - 1 ) * psDec.subfr_length);
            System.arraycopy(psDec.exc_Q10, subfr * psDec.subfr_length , psCNG.CNG_exc_buf_Q10, 0, psDec.subfr_length);
            /* Smooth gains */
            for( i = 0; i < Silk_define.NB_SUBFR; i++ ) {
                psCNG.CNG_smth_Gain_Q16 += Silk_macros.SKP_SMULWB( psDecCtrl.Gains_Q16[ i ] - psCNG.CNG_smth_Gain_Q16, Silk_define.CNG_GAIN_SMTH_Q16 );
            }
        }

        /* Add CNG when packet is lost and / or when low speech activity */
        if( psDec.lossCnt != 0 ) {//|| psDec.vadFlag == NO_VOICE_ACTIVITY ) {

            /* Generate CNG excitation */
            int[] psCNG_rand_seed_ptr = new int[1];
            psCNG_rand_seed_ptr[0] = psCNG.rand_seed;
            
             SKP_Silk_CNG_exc( CNG_sig, 0,  psCNG.CNG_exc_buf_Q10, 0,
                        psCNG.CNG_smth_Gain_Q16, length, psCNG_rand_seed_ptr );
             psCNG.rand_seed = psCNG_rand_seed_ptr[0];
             
            /* Convert CNG NLSF to filter representation */
            Silk_NLSF2A_stable.SKP_Silk_NLSF2A_stable( LPC_buf, psCNG.CNG_smth_NLSF_Q15, psDec.LPC_order );

            Gain_Q26 = 1 << 26; /* 1.0 */
            
            /* Generate CNG signal, by synthesis filtering */
            if( psDec.LPC_order == 16 ) {
                Silk_LPC_synthesis_order16.SKP_Silk_LPC_synthesis_order16( CNG_sig, LPC_buf, 
                        Gain_Q26, psCNG.CNG_synth_state, CNG_sig, length );
            } else {
                Silk_LPC_synthesis_filter.SKP_Silk_LPC_synthesis_filter( CNG_sig, LPC_buf, 
                        Gain_Q26, psCNG.CNG_synth_state, CNG_sig, length, psDec.LPC_order );
            }
            /* Mix with signal */
            for( i = 0; i < length; i++ ) {
                tmp_32 = signal[ signal_offset + i ] + CNG_sig[ i ];
                signal[ signal_offset+i ] = (short) Silk_SigProc_FIX.SKP_SAT16( tmp_32 );
            }
        } else {
            Arrays.fill(psCNG.CNG_synth_state,0, psDec.LPC_order,0);
        }
    }
}
