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
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_find_pred_coefs_FLP 
{
    /************************
     * TEST for nlsf
     */
    static int frame_cnt = 0;
    /*************************************/
    
    /**
     * 
     * @param psEnc Encoder state FLP.
     * @param psEncCtrl Encoder control FLP.
     * @param res_pitch Residual from pitch analysis.
     */
    static void SKP_Silk_find_pred_coefs_FLP(
            SKP_Silk_encoder_state_FLP      psEnc,         /* I/O  Encoder state FLP               */
            SKP_Silk_encoder_control_FLP    psEncCtrl,     /* I/O  Encoder control FLP             */
            float                           res_pitch[]     /* I    Residual from pitch analysis    */
    )
    {
        int         i;
        float[]       WLTP = new float[ Silk_define.NB_SUBFR * Silk_define.LTP_ORDER * Silk_define.LTP_ORDER ];
        float[]       invGains = new float[ Silk_define.NB_SUBFR ], Wght = new float[ Silk_define.NB_SUBFR ];
        float[]       NLSF = new float[ Silk_define.MAX_LPC_ORDER ];
        float[] x_ptr;
        int x_ptr_offset;
        float[]       x_pre_ptr, LPC_in_pre = new float[ Silk_define.NB_SUBFR * Silk_define.MAX_LPC_ORDER + Silk_define.MAX_FRAME_LENGTH ];
        int x_pre_ptr_offset;

        /* Weighting for weighted least squares */
        for( i = 0; i < Silk_define.NB_SUBFR; i++ ) 
        {
            assert( psEncCtrl.Gains[ i ] > 0.0f );
            invGains[ i ] = 1.0f / psEncCtrl.Gains[ i ];
            Wght[ i ]     = invGains[ i ] * invGains[ i ];
        }

        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) 
        {
            /**********/
            /* VOICED */
            /**********/
            assert( psEnc.sCmn.frame_length - psEnc.sCmn.predictLPCOrder >= psEncCtrl.sCmn.pitchL[ 0 ] + Silk_define.LTP_ORDER / 2 );

            /* LTP analysis */
            float[] LTPredCodGain_ptr = new float[1];
            LTPredCodGain_ptr[0] = psEncCtrl.LTPredCodGain;
            Silk_find_LTP_FLP.SKP_Silk_find_LTP_FLP( psEncCtrl.LTPCoef, WLTP, LTPredCodGain_ptr, res_pitch, 
                res_pitch,( psEnc.sCmn.frame_length >> 1 ), psEncCtrl.sCmn.pitchL, Wght, 
                psEnc.sCmn.subfr_length, psEnc.sCmn.frame_length );
            psEncCtrl.LTPredCodGain = LTPredCodGain_ptr[0];


            /* Quantize LTP gain parameters */
            int[] PERIndex_ptr = new int[1];
            PERIndex_ptr[0] = psEncCtrl.sCmn.PERIndex;
            Silk_quant_LTP_gains_FLP.SKP_Silk_quant_LTP_gains_FLP( psEncCtrl.LTPCoef, psEncCtrl.sCmn.LTPIndex, PERIndex_ptr, 
                WLTP, psEnc.mu_LTP, psEnc.sCmn.LTPQuantLowComplexity );
            psEncCtrl.sCmn.PERIndex = PERIndex_ptr[0];

            /* Control LTP scaling */
            Silk_LTP_scale_ctrl_FLP.SKP_Silk_LTP_scale_ctrl_FLP( psEnc, psEncCtrl );

            /* Create LTP residual */
            Silk_LTP_analysis_filter_FLP.SKP_Silk_LTP_analysis_filter_FLP( LPC_in_pre, psEnc.x_buf, psEnc.sCmn.frame_length - psEnc.sCmn.predictLPCOrder, 
                psEncCtrl.LTPCoef, psEncCtrl.sCmn.pitchL, invGains, psEnc.sCmn.subfr_length, psEnc.sCmn.predictLPCOrder );

        } else {
            /************/
            /* UNVOICED */
            /************/
            /* Create signal with prepended subframes, scaled by inverse gains */
            x_ptr     = psEnc.x_buf;
            x_ptr_offset = psEnc.sCmn.frame_length - psEnc.sCmn.predictLPCOrder;

            x_pre_ptr = LPC_in_pre;
            x_pre_ptr_offset = 0;
            for( i = 0; i < Silk_define.NB_SUBFR; i++ ) {
                Silk_scale_copy_vector_FLP.SKP_Silk_scale_copy_vector_FLP( x_pre_ptr, x_pre_ptr_offset, x_ptr, x_ptr_offset, invGains[ i ], 
                    psEnc.sCmn.subfr_length + psEnc.sCmn.predictLPCOrder );
                x_pre_ptr_offset += psEnc.sCmn.subfr_length + psEnc.sCmn.predictLPCOrder;
                x_ptr_offset     += psEnc.sCmn.subfr_length;
            }

            Arrays.fill(psEncCtrl.LTPCoef, 0, Silk_define.NB_SUBFR * Silk_define.LTP_ORDER, 0.0f);
            psEncCtrl.LTPredCodGain = 0.0f;
        }

        /* LPC_in_pre contains the LTP-filtered input for voiced, and the unfiltered input for unvoiced */
        int[] NLSFInterpCoef_Q2_ptr = new int[1];
        NLSFInterpCoef_Q2_ptr[0] = psEncCtrl.sCmn.NLSFInterpCoef_Q2;
        Silk_find_LPC_FLP.SKP_Silk_find_LPC_FLP( NLSF, NLSFInterpCoef_Q2_ptr, psEnc.sPred.prev_NLSFq, 
            psEnc.sCmn.useInterpolatedNLSFs * ( 1 - psEnc.sCmn.first_frame_after_reset ), psEnc.sCmn.predictLPCOrder, 
            LPC_in_pre, psEnc.sCmn.subfr_length + psEnc.sCmn.predictLPCOrder );
        psEncCtrl.sCmn.NLSFInterpCoef_Q2 = NLSFInterpCoef_Q2_ptr[0];


        /* Quantize LSFs */
/*TEST************************************************************************/
//        /**
//         * Test for NLSF
//         */
//        float[]       nlsf = new float[ Silk_define.MAX_LPC_ORDER ];
//        String nlsf_filename = "D:/gsoc/nlsf/nlsf";
//        nlsf_filename += frame_cnt;
//        DataInputStream nlsf_datain = null;
//        try
//        {
//            nlsf_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(nlsf_filename)));
//            byte[] buffer = new byte[4];
//            for(int ii = 0; ii < NLSF.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = nlsf_datain.read(buffer);
//                    if(res != 4)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    nlsf[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                    NLSF[ii] = nlsf[ii];
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            } 
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        finally
//        {
//            if(nlsf_datain != null)
//            {
//                try
//                {
//                    nlsf_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
/*TEST END********************************************************************/
        Silk_process_NLSFs_FLP.SKP_Silk_process_NLSFs_FLP( psEnc, psEncCtrl, NLSF );

        /* Calculate residual energy using quantized LPC coefficients */
        Silk_residual_energy_FLP.SKP_Silk_residual_energy_FLP( psEncCtrl.ResNrg, LPC_in_pre, psEncCtrl.PredCoef, psEncCtrl.Gains,
            psEnc.sCmn.subfr_length, psEnc.sCmn.predictLPCOrder );

        /* Copy to prediction struct for use in next frame for fluctuation reduction */
        System.arraycopy(NLSF, 0, psEnc.sPred.prev_NLSFq, 0, psEnc.sCmn.predictLPCOrder);
    }
}
