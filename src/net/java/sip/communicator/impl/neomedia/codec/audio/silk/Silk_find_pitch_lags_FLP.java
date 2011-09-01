/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_find_pitch_lags_FLP 
{
    /**
     * 
     * @param psEnc Encoder state FLP.
     * @param psEncCtrl Encoder control FLP.
     * @param res Residual.
     * @param x Speech signal.
     * @param x_offset offset of valid data.
     */
    static void SKP_Silk_find_pitch_lags_FLP(
            SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
            SKP_Silk_encoder_control_FLP    psEncCtrl,         /* I/O  Encoder control FLP                     */
            float                           res[],             /* O    Residual                                */
            float                           x[],               /* I    Speech signal                           */
            int                             x_offset
        )
        {
            SKP_Silk_predict_state_FLP psPredSt = psEnc.sPred;
//            const SKP_float *x_buf_ptr, *x_buf;
            float[] x_buf_ptr, x_buf;
            int x_buf_ptr_offset, x_buf_offset;
            float[] auto_corr = new float[ Silk_define.FIND_PITCH_LPC_ORDER_MAX + 1 ];
            float[] A = new float[         Silk_define.FIND_PITCH_LPC_ORDER_MAX ];
            float[] refl_coef = new float[ Silk_define.FIND_PITCH_LPC_ORDER_MAX ];
            float[] Wsig = new float[      Silk_define.FIND_PITCH_LPC_WIN_MAX ];
            float thrhld;
            float[] Wsig_ptr;
            int Wsig_ptr_offset;
            int   buf_len;

            /******************************************/
            /* Setup buffer lengths etc based of Fs   */
            /******************************************/
            buf_len = 2 * psEnc.sCmn.frame_length + psEnc.sCmn.la_pitch;

            /* Safty check */
            assert( buf_len >= psPredSt.pitch_LPC_win_length );

//            x_buf = x - psEnc->sCmn.frame_length;
            x_buf = x;
            x_buf_offset = x_offset - psEnc.sCmn.frame_length;

            /*************************************/
            /* Estimate LPC AR coeficients */
            /*************************************/
            
            /* Calculate windowed signal */
            
            /* First LA_LTP samples */
//            x_buf_ptr = x_buf + buf_len - psPredSt->pitch_LPC_win_length;
            x_buf_ptr = x_buf;
            x_buf_ptr_offset = x_buf_offset + buf_len - psPredSt.pitch_LPC_win_length;
            Wsig_ptr  = Wsig;
            Wsig_ptr_offset=0;
            Silk_apply_sine_window_FLP.SKP_Silk_apply_sine_window_FLP( Wsig_ptr,Wsig_ptr_offset, x_buf_ptr,x_buf_ptr_offset, 1, psEnc.sCmn.la_pitch );

            /* Middle non-windowed samples */
            Wsig_ptr_offset  += psEnc.sCmn.la_pitch;
            x_buf_ptr_offset += psEnc.sCmn.la_pitch;
//            SKP_memcpy( Wsig_ptr, x_buf_ptr, ( psPredSt->pitch_LPC_win_length - ( psEnc->sCmn.la_pitch << 1 ) ) * sizeof( SKP_float ) );
            for(int i_djinn=0; i_djinn< psPredSt.pitch_LPC_win_length - ( psEnc.sCmn.la_pitch << 1 ); i_djinn++)
                Wsig_ptr[Wsig_ptr_offset + i_djinn]  = x_buf_ptr[x_buf_ptr_offset+i_djinn];

            /* Last LA_LTP samples */
            Wsig_ptr_offset  += psPredSt.pitch_LPC_win_length - ( psEnc.sCmn.la_pitch << 1 );
            x_buf_ptr_offset += psPredSt.pitch_LPC_win_length - ( psEnc.sCmn.la_pitch << 1 );
            Silk_apply_sine_window_FLP.SKP_Silk_apply_sine_window_FLP( Wsig_ptr,Wsig_ptr_offset, x_buf_ptr,x_buf_ptr_offset, 2, psEnc.sCmn.la_pitch );

            /* Calculate autocorrelation sequence */
            Silk_autocorrelation_FLP.SKP_Silk_autocorrelation_FLP( auto_corr,0, Wsig,0, psPredSt.pitch_LPC_win_length, psEnc.sCmn.pitchEstimationLPCOrder + 1 );

            /* Add white noise, as a fraction of the energy */
            auto_corr[ 0 ] += auto_corr[ 0 ] * Silk_define_FLP.FIND_PITCH_WHITE_NOISE_FRACTION;

            /* Calculate the reflection coefficients using Schur */
            Silk_schur_FLP.SKP_Silk_schur_FLP( refl_coef,0, auto_corr,0, psEnc.sCmn.pitchEstimationLPCOrder );

            /* Convert reflection coefficients to prediction coefficients */
            Silk_k2a_FLP.SKP_Silk_k2a_FLP( A, refl_coef, psEnc.sCmn.pitchEstimationLPCOrder );

            /* Bandwidth expansion */
            Silk_bwexpander_FLP.SKP_Silk_bwexpander_FLP( A,0, psEnc.sCmn.pitchEstimationLPCOrder, Silk_define_FLP.FIND_PITCH_BANDWITH_EXPANSION );
            
            /*****************************************/
            /* LPC analysis filtering               */
            /*****************************************/
            Silk_LPC_analysis_filter_FLP.SKP_Silk_LPC_analysis_filter_FLP( res, A, x_buf, x_buf_offset, buf_len, psEnc.sCmn.pitchEstimationLPCOrder );
//            SKP_memset( res, 0, psEnc->sCmn.pitchEstimationLPCOrder * sizeof( SKP_float ) );
            for(int i_djinn=0; i_djinn<psEnc.sCmn.pitchEstimationLPCOrder; i_djinn++)
                res[i_djinn] = 0;

            /* Threshold for pitch estimator */
            thrhld  = 0.5f;
            thrhld -= 0.004f * psEnc.sCmn.pitchEstimationLPCOrder;
            thrhld -= 0.1f  * ( float )Math.sqrt( psEnc.speech_activity );
            thrhld += 0.14f * psEnc.sCmn.prev_sigtype;
            thrhld -= 0.12f * psEncCtrl.input_tilt;

            /*****************************************/
            /* Call Pitch estimator */
            /*****************************************/
            int[] lagIndex_djinnaddress = {psEncCtrl.sCmn.lagIndex};
            int[] contourIndex_djinnaddress = {psEncCtrl.sCmn.contourIndex};
            float[] LTPCorr_djinnaddress = {psEnc.LTPCorr};
            psEncCtrl.sCmn.sigtype = Silk_pitch_analysis_core_FLP.SKP_Silk_pitch_analysis_core_FLP( res, psEncCtrl.sCmn.pitchL, lagIndex_djinnaddress, 
                    contourIndex_djinnaddress, LTPCorr_djinnaddress, psEnc.sCmn.prevLag, psEnc.pitchEstimationThreshold, 
                thrhld, psEnc.sCmn.fs_kHz, psEnc.sCmn.pitchEstimationComplexity );
            psEncCtrl.sCmn.lagIndex = lagIndex_djinnaddress[0];    
            psEncCtrl.sCmn.contourIndex = contourIndex_djinnaddress[0];
            psEnc.LTPCorr = LTPCorr_djinnaddress[0];
        }
}
