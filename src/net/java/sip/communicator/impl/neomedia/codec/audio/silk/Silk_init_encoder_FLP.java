/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Initialize Silk Encoder state.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_init_encoder_FLP
{
    /**
     * Initialize Silk Encoder state.
     * @param psEnc
     * @return
     */
    static int SKP_Silk_init_encoder_FLP(
        SKP_Silk_encoder_state_FLP      psEnc              /* I/O  Encoder state FLP                       */
    ) 
    {
        int ret = 0;

        /* Clear the entire encoder state */
//TODO: psEnc = new SKP_Silk_encoder_state_FLP();        
//        SKP_memset( psEnc, 0, sizeof( SKP_Silk_encoder_state_FLP ) );

        /* Initialize to 24 kHz API sampling, 24 kHz max internal sampling, 20 ms packets, 25 kbps, 0% packet loss, and init non-zero values */
        ret = Silk_control_codec_FLP.SKP_Silk_control_encoder_FLP( psEnc, 24000, 24, 20, 25, 0, 0, 0, 10, 0 );

        if(Silk_define.HIGH_PASS_INPUT!=0)
        {
            psEnc.variable_HP_smth1 = Silk_main_FLP.SKP_Silk_log2( 70.0 );
            psEnc.variable_HP_smth2 = Silk_main_FLP.SKP_Silk_log2( 70.0 );
        }

        /* Used to deactivate e.g. LSF interpolation and fluctuation reduction */
        psEnc.sCmn.first_frame_after_reset = 1;
        psEnc.sCmn.fs_kHz_changed          = 0;
        psEnc.sCmn.LBRR_enabled            = 0;

        /* Initialize Silk VAD */
        ret += Silk_VAD.SKP_Silk_VAD_Init( psEnc.sCmn.sVAD );

        /* Initialize NSQ */
        psEnc.sNSQ.prev_inv_gain_Q16      = 65536;
        psEnc.sNSQ_LBRR.prev_inv_gain_Q16 = 65536;

        return( ret );
    }
}
