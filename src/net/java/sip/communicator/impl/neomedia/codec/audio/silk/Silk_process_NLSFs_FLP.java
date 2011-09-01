/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Limit, stabilize, convert and quantize NLSFs.
 *
 * @author Dingxin Xu
 */
public class Silk_process_NLSFs_FLP 
{
    /**
     * Limit, stabilize, convert and quantize NLSFs.
     * @param psEnc Encoder state FLP
     * @param psEncCtrl Encoder control FLP
     * @param pNLSF NLSFs (quantized output)
     */
    static void SKP_Silk_process_NLSFs_FLP(
        SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
        SKP_Silk_encoder_control_FLP    psEncCtrl,         /* I/O  Encoder control FLP                     */
        float                           []pNLSF              /* I/O  NLSFs (quantized output)                */
    )
    {
        boolean     doInterpolate;
        float   pNLSFW[] = new float[ Silk_define.MAX_LPC_ORDER ];
        float   NLSF_mu, NLSF_mu_fluc_red, i_sqr, NLSF_interpolation_factor = 0.0f;
        final SKP_Silk_NLSF_CB_FLP psNLSF_CB_FLP;


        /* Used only for NLSF interpolation */
        float   pNLSF0_temp[] = new float[  Silk_define.MAX_LPC_ORDER ];
        float   pNLSFW0_temp[] = new float[ Silk_define.MAX_LPC_ORDER ];
        int     i;

        assert( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED || psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_UNVOICED );

        /***********************/
        /* Calculate mu values */
        /***********************/
        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            NLSF_mu          = 0.002f - 0.001f * psEnc.speech_activity;
            NLSF_mu_fluc_red = 0.1f   - 0.05f  * psEnc.speech_activity;
        } else { 
            NLSF_mu          = 0.005f - 0.004f * psEnc.speech_activity;
            NLSF_mu_fluc_red = 0.2f   - 0.1f   * ( psEnc.speech_activity + psEncCtrl.sparseness );
        }

        /* Calculate NLSF weights */
        Silk_NLSF_VQ_weights_laroia_FLP.SKP_Silk_NLSF_VQ_weights_laroia_FLP( pNLSFW, pNLSF, psEnc.sCmn.predictLPCOrder );

        /* Update NLSF weights for interpolated NLSFs */
        doInterpolate = ( psEnc.sCmn.useInterpolatedNLSFs == 1 ) && ( psEncCtrl.sCmn.NLSFInterpCoef_Q2 < ( 1 << 2 ) );
        if( doInterpolate ) {

            /* Calculate the interpolated NLSF vector for the first half */
            NLSF_interpolation_factor = 0.25f * psEncCtrl.sCmn.NLSFInterpCoef_Q2;
            Silk_wrappers_FLP.SKP_Silk_interpolate_wrapper_FLP( pNLSF0_temp, psEnc.sPred.prev_NLSFq, pNLSF, 
                NLSF_interpolation_factor, psEnc.sCmn.predictLPCOrder );

            /* Calculate first half NLSF weights for the interpolated NLSFs */
            Silk_NLSF_VQ_weights_laroia_FLP.SKP_Silk_NLSF_VQ_weights_laroia_FLP( pNLSFW0_temp, pNLSF0_temp, psEnc.sCmn.predictLPCOrder );

            /* Update NLSF weights with contribution from first half */
            i_sqr = NLSF_interpolation_factor * NLSF_interpolation_factor;
            for( i = 0; i < psEnc.sCmn.predictLPCOrder; i++ ) {
                pNLSFW[ i ] = 0.5f * ( pNLSFW[ i ] + i_sqr * pNLSFW0_temp[ i ] );
            }
        }

        /* Set pointer to the NLSF codebook for the current signal type and LPC order */
        psNLSF_CB_FLP = psEnc.psNLSF_CB_FLP[ psEncCtrl.sCmn.sigtype ];

        /* Quantize NLSF parameters given the trained NLSF codebooks */
        Silk_NLSF_MSVQ_encode_FLP.SKP_Silk_NLSF_MSVQ_encode_FLP( psEncCtrl.sCmn.NLSFIndices, pNLSF, psNLSF_CB_FLP, psEnc.sPred.prev_NLSFq, 
                pNLSFW, NLSF_mu, NLSF_mu_fluc_red, psEnc.sCmn.NLSF_MSVQ_Survivors, 
                psEnc.sCmn.predictLPCOrder, psEnc.sCmn.first_frame_after_reset );

        /* Convert quantized NLSFs back to LPC coefficients */
        Silk_wrappers_FLP.SKP_Silk_NLSF2A_stable_FLP( psEncCtrl.PredCoef[ 1 ], pNLSF, psEnc.sCmn.predictLPCOrder );

        if( doInterpolate ) {
            /* Calculate the interpolated, quantized NLSF vector for the first half */
            Silk_wrappers_FLP.SKP_Silk_interpolate_wrapper_FLP( pNLSF0_temp, psEnc.sPred.prev_NLSFq, pNLSF, 
                NLSF_interpolation_factor, psEnc.sCmn.predictLPCOrder );

            /* Convert back to LPC coefficients */
            Silk_wrappers_FLP.SKP_Silk_NLSF2A_stable_FLP( psEncCtrl.PredCoef[ 0 ], pNLSF0_temp, psEnc.sCmn.predictLPCOrder );

        } else {
            /* Copy LPC coefficients for first half from second half */
            System.arraycopy(psEncCtrl.PredCoef[1], 0, psEncCtrl.PredCoef[0], 0, psEnc.sCmn.predictLPCOrder);
        }
    }
}
