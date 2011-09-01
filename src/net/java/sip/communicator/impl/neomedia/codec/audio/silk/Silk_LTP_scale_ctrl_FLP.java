/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

public class Silk_LTP_scale_ctrl_FLP 
{
    static final int NB_THRESHOLDS = 11;

    /**
     * Table containing trained thresholds for LTP scaling.
     */
    static final float LTPScaleThresholds[] = 
    {
        0.95f, 0.8f, 0.50f, 0.400f, 0.3f, 0.2f,
        0.15f, 0.1f, 0.08f, 0.075f, 0.0f
    };

    /**
     * 
     * @param psEnc Encoder state FLP
     * @param psEncCtrl Encoder control FLP
     */
    static void SKP_Silk_LTP_scale_ctrl_FLP(
            SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
            SKP_Silk_encoder_control_FLP    psEncCtrl          /* I/O  Encoder control FLP                     */
    )
    {
        int round_loss, frames_per_packet;
        float g_out, g_limit, thrld1, thrld2;

        /* 1st order high-pass filter */
        //g_HP(n) = g(n) - g(n-1) + 0.5 * g_HP(n-1);       // tune the 0.5: higher means longer impact of jump
        psEnc.HPLTPredCodGain =Math.max( psEncCtrl.LTPredCodGain - psEnc.prevLTPredCodGain, 0.0f ) 
                                + 0.5f * psEnc.HPLTPredCodGain;
        
        psEnc.prevLTPredCodGain = psEncCtrl.LTPredCodGain;

        /* combine input and filtered input */
        g_out = 0.5f * psEncCtrl.LTPredCodGain + ( 1.0f - 0.5f ) * psEnc.HPLTPredCodGain;
        g_limit = Silk_SigProc_FLP.SKP_sigmoid( 0.5f * ( g_out - 6 ) );
        
        
        /* Default is minimum scaling */
        psEncCtrl.sCmn.LTP_scaleIndex = 0;

        /* Round the loss measure to whole pct */
        round_loss = ( int )( psEnc.sCmn.PacketLoss_perc );
        round_loss = ( 0 > round_loss ? 0 : round_loss);


        /* Only scale if first frame in packet 0% */
        if( psEnc.sCmn.nFramesInPayloadBuf == 0 ){
            
            frames_per_packet = psEnc.sCmn.PacketSize_ms / Silk_define.FRAME_LENGTH_MS;

            round_loss += ( frames_per_packet - 1 );
//            thrld1 = LTPScaleThresholds[ Math.min( round_loss,     NB_THRESHOLDS - 1 ) ];
//            thrld2 = LTPScaleThresholds[ Math.min( round_loss + 1, NB_THRESHOLDS - 1 ) ];
            thrld1 = LTPScaleThresholds[ round_loss < (NB_THRESHOLDS - 1) ? round_loss : (NB_THRESHOLDS - 1)];
            thrld2 = LTPScaleThresholds[ (round_loss + 1) < (NB_THRESHOLDS - 1) ? (round_loss + 1):(NB_THRESHOLDS - 1)];
        
        
            if( g_limit > thrld1 ) {
                /* High Scaling */
                psEncCtrl.sCmn.LTP_scaleIndex = 2;
            } else if( g_limit > thrld2 ) {
                /* Middle Scaling */
                psEncCtrl.sCmn.LTP_scaleIndex = 1;
            }
        }
        psEncCtrl.LTP_scale = ( float)Silk_tables_other.SKP_Silk_LTPScales_table_Q14[ psEncCtrl.sCmn.LTP_scaleIndex ] / 16384.0f;

    }
}
