/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * processing of gains.
 * 
 * @author Dingxin Xu
 */
public class Silk_process_gains_FLP 
{
    /**
     * Processing of gains.
     * @param psEnc Encoder state FLP
     * @param psEncCtrl Encoder control FLP
     */
    static void SKP_Silk_process_gains_FLP(
        SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
        SKP_Silk_encoder_control_FLP    psEncCtrl          /* I/O  Encoder control FLP                     */
    )
    {
        SKP_Silk_shape_state_FLP psShapeSt = psEnc.sShape;
        int     k;
        int     pGains_Q16[] = new int[ Silk_define.NB_SUBFR ];
        float   s, InvMaxSqrVal, gain;

        /* Gain reduction when LTP coding gain is high */
        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            s = 1.0f - 0.5f * Silk_SigProc_FLP.SKP_sigmoid( 0.25f * ( psEncCtrl.LTPredCodGain - 12.0f ) );
            for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {   
                psEncCtrl.Gains[ k ] *= s;
            }
        }

        /* Limit the quantized signal */
        InvMaxSqrVal = ( float )( Math.pow( 2.0f, 0.33f * ( 21.0f - psEncCtrl.current_SNR_dB ) ) / psEnc.sCmn.subfr_length );

        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            /* Soft limit on ratio residual energy and squared gains */
            gain = psEncCtrl.Gains[ k ];
            gain = ( float )Math.sqrt( gain * gain + psEncCtrl.ResNrg[ k ] * InvMaxSqrVal );
            psEncCtrl.Gains[ k ] = ( gain < 32767.0f ? gain : 32767.0f );
        }

        /* Prepare gains for noise shaping quantization */
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            pGains_Q16[ k ] = ( int ) ( psEncCtrl.Gains[ k ] * 65536.0f ); 
        }

        /* Noise shaping quantization */
        int[] LastGainIndex_ptr = new int[1];
        LastGainIndex_ptr[0] = psShapeSt.LastGainIndex;
        Silk_gain_quant.SKP_Silk_gains_quant( psEncCtrl.sCmn.GainsIndices, pGains_Q16, 
                LastGainIndex_ptr, psEnc.sCmn.nFramesInPayloadBuf );
        psShapeSt.LastGainIndex = LastGainIndex_ptr[0];
        /* Overwrite unquantized gains with quantized gains and convert back to Q0 from Q16 */
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            psEncCtrl.Gains[ k ] = pGains_Q16[ k ] / 65536.0f;
        }

        /* Set quantizer offset for voiced signals. Larger offset when LTP coding gain is low or tilt is high (ie low-pass) */
        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            if( psEncCtrl.LTPredCodGain + psEncCtrl.input_tilt > 1.0f ) {
                psEncCtrl.sCmn.QuantOffsetType = 0;
            } else {
                psEncCtrl.sCmn.QuantOffsetType = 1;
            }
        }

        /* Quantizer boundary adjustment */
        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            psEncCtrl.Lambda = 1.2f - 0.4f * psEnc.speech_activity 
                                     - 0.3f * psEncCtrl.input_quality   
                                     + 0.2f * psEncCtrl.sCmn.QuantOffsetType
                                     - 0.1f * psEncCtrl.coding_quality;
        } else {
            psEncCtrl.Lambda = 1.2f - 0.4f * psEnc.speech_activity 
                                     - 0.4f * psEncCtrl.input_quality
                                     + 0.4f * psEncCtrl.sCmn.QuantOffsetType
                                     - 0.1f * psEncCtrl.coding_quality;
        }

        assert( psEncCtrl.Lambda >= 0.0f );
        assert( psEncCtrl.Lambda <  2.0f );
    }
}
