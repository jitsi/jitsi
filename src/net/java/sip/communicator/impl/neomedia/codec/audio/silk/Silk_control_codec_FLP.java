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
public class Silk_control_codec_FLP {
    /**
     * Control encoder SNR.
     * 
     * @param psEnc
     *            Pointer to Silk encoder state FLP
     * @param API_fs_Hz
     *            External (API) sampling rate (Hz).
     * @param max_internal_fs_kHz
     *            Maximum internal sampling rate (kHz).
     * @param PacketSize_ms
     *            Packet length (ms).
     * @param TargetRate_bps
     *            Target max bitrate (if SNR_dB == 0).
     * @param PacketLoss_perc
     *            Packet loss rate (in percent).
     * @param INBandFEC_enabled
     *            Enable (1) / disable (0) inband FEC.
     * @param DTX_enabled
     *            Enable / disable DTX.
     * @param InputFramesize_ms
     *            Inputframe in ms.
     * @param Complexity
     *            Complexity (0->low; 1->medium; 2->high).
     * @return
     */
    static int SKP_Silk_control_encoder_FLP( 
        SKP_Silk_encoder_state_FLP  psEnc,              /* I/O  Pointer to Silk encoder state FLP       */
        final int                   API_fs_Hz,          /* I    External (API) sampling rate (Hz)       */
        final int                   max_internal_fs_kHz,/* I    Maximum internal sampling rate (kHz)    */
        final int                   PacketSize_ms,      /* I    Packet length (ms)                      */
              int                   TargetRate_bps,     /* I    Target max bitrate (if SNR_dB == 0)     */
        final int                   PacketLoss_perc,    /* I    Packet loss rate (in percent)           */
        final int                   INBandFEC_enabled,  /* I    Enable (1) / disable (0) inband FEC     */
        final int                   DTX_enabled,        /* I    Enable / disable DTX                    */
        final int                   InputFramesize_ms,  /* I    Inputframe in ms                        */
        final int                   Complexity          /* I    Complexity (0->low; 1->medium; 2->high) */
    )
    {
        int LBRRRate_thres_bps;
        int   k, fs_kHz, ret = 0;
        float frac;
        int[] rateTable;

        /* State machine for the SWB/WB switching */
        fs_kHz = psEnc.sCmn.fs_kHz;
        
        /* Only switch during low speech activity, when no frames are sitting in the payload buffer */
        if( API_fs_Hz == 8000 || fs_kHz == 0 || API_fs_Hz < fs_kHz * 1000 || fs_kHz > max_internal_fs_kHz )
        {
            /* Switching is not possible, encoder just initialized, internal mode higher than external, */
            /* or internal mode higher than maximum allowed internal mode                               */
            fs_kHz = Math.min( API_fs_Hz/1000, max_internal_fs_kHz );
        } 
        else
        {
            /* Accumulate the difference between the target rate and limit for switching down */
            psEnc.sCmn.bitrateDiff += InputFramesize_ms * (TargetRate_bps - psEnc.sCmn.bitrate_threshold_down);
            psEnc.sCmn.bitrateDiff  = Math.min( psEnc.sCmn.bitrateDiff, 0 );

            if( psEnc.speech_activity < 0.5f && psEnc.sCmn.nFramesInPayloadBuf == 0 )
            { /* Low speech activity and payload buffer empty */
                /* Check if we should switch down */
                if(Silk_define.SWITCH_TRANSITION_FILTERING!=0)
                {
                    if( ( psEnc.sCmn.sLP.transition_frame_no == 0 ) &&                         /* Transition phase not active */
                        ( psEnc.sCmn.bitrateDiff <= -Silk_define.ACCUM_BITS_DIFF_THRESHOLD ||              /* Bitrate threshold is met */
                        ( psEnc.sCmn.sSWBdetect.WB_detected * psEnc.sCmn.fs_kHz == 24 ) ) ) 
                    { /* Forced down-switching due to WB input */
                        psEnc.sCmn.sLP.transition_frame_no = 1;                                /* Begin transition phase */
                        psEnc.sCmn.sLP.mode                = 0;                                /* Switch down */
                    } 
                    else if( 
                        ( psEnc.sCmn.sLP.transition_frame_no >= Silk_define.TRANSITION_FRAMES_DOWN ) &&    /* Transition phase complete */
                        ( psEnc.sCmn.sLP.mode == 0 ) ) 
                    {                                       /* Ready to switch down */
                        psEnc.sCmn.sLP.transition_frame_no = 0;                                /* Ready for new transition phase */
                        psEnc.sCmn.bitrateDiff = 0;
                        
                        /* Switch to a lower sample frequency */
                        if( psEnc.sCmn.fs_kHz == 24 ) 
                        {
                            fs_kHz = 16;
                        } 
                        else if( psEnc.sCmn.fs_kHz == 16 )
                        {
                            fs_kHz = 12;
                        } 
                        else 
                        {
                            assert( psEnc.sCmn.fs_kHz == 12 );
                            fs_kHz = 8;
                        }
                    }
                }
                else
                {
                    if( psEnc.sCmn.bitrateDiff <= -Silk_define.ACCUM_BITS_DIFF_THRESHOLD ) 
                    {               /* Bitrate threshold is met */
                        psEnc.sCmn.bitrateDiff = 0;
                        
                        /* Switch to a lower sample frequency */
                        if( psEnc.sCmn.fs_kHz == 24 ) 
                        {
                            fs_kHz = 16;
                        } 
                        else if( psEnc.sCmn.fs_kHz == 16 )
                        {
                            fs_kHz = 12;
                        } 
                        else 
                        {
                            assert( psEnc.sCmn.fs_kHz == 12 );
                            fs_kHz = 8;
                        }
                    }
                }
                /* Check if we should switch up */
                if(Silk_define.SWITCH_TRANSITION_FILTERING != 0) 
                {
                    if( ( ( psEnc.sCmn.fs_kHz * 1000 < API_fs_Hz ) &&
                        ( TargetRate_bps >= psEnc.sCmn.bitrate_threshold_up ) && 
                        ( psEnc.sCmn.sSWBdetect.WB_detected * psEnc.sCmn.fs_kHz != 16 ) ) && 
                        ( ( psEnc.sCmn.fs_kHz == 16 ) && ( max_internal_fs_kHz >= 24 ) || 
                          ( psEnc.sCmn.fs_kHz == 12 ) && ( max_internal_fs_kHz >= 16 ) ||
                          ( psEnc.sCmn.fs_kHz ==  8 ) && ( max_internal_fs_kHz >= 12 ) ) 
    //    #if SWITCH_TRANSITION_FILTERING
                          && ( psEnc.sCmn.sLP.transition_frame_no == 0 ) ) 
                    { /* No transition phase running, ready to switch */
                            psEnc.sCmn.sLP.mode = 1; /* Switch up */
    //    #else
    //                    ) {
    //    #endif
                        psEnc.sCmn.bitrateDiff = 0;
    
                        /* Switch to a higher sample frequency */
                        if( psEnc.sCmn.fs_kHz == 8 ) 
                        {
                            fs_kHz = 12;
                        } 
                        else if( psEnc.sCmn.fs_kHz == 12 ) 
                        {
                            fs_kHz = 16;
                        }
                        else
                        {
                            assert( psEnc.sCmn.fs_kHz == 16 );
                            fs_kHz = 24;
                        } 
                    }
                }
                else 
                {
                    /* Check if we should switch up */
                    if( ( ( psEnc.sCmn.fs_kHz * 1000 < API_fs_Hz ) &&
                        ( TargetRate_bps >= psEnc.sCmn.bitrate_threshold_up ) && 
                        ( psEnc.sCmn.sSWBdetect.WB_detected * psEnc.sCmn.fs_kHz != 16 ) ) && 
                        ( ( psEnc.sCmn.fs_kHz == 16 ) && ( max_internal_fs_kHz >= 24 ) || 
                          ( psEnc.sCmn.fs_kHz == 12 ) && ( max_internal_fs_kHz >= 16 ) ||
                          ( psEnc.sCmn.fs_kHz ==  8 ) && ( max_internal_fs_kHz >= 12 ) ) 
//    //    #if SWITCH_TRANSITION_FILTERING
//                          && ( psEnc.sCmn.sLP.transition_frame_no == 0 ) ) 
//                    { /* No transition phase running, ready to switch */
//                            psEnc.sCmn.sLP.mode = 1; /* Switch up */
//    //    #else
                        ) {
    //    #endif
                        psEnc.sCmn.bitrateDiff = 0;
    
                        /* Switch to a higher sample frequency */
                        if( psEnc.sCmn.fs_kHz == 8 ) 
                        {
                            fs_kHz = 12;
                        } 
                        else if( psEnc.sCmn.fs_kHz == 12 ) 
                        {
                            fs_kHz = 16;
                        }
                        else
                        {
                            assert( psEnc.sCmn.fs_kHz == 16 );
                            fs_kHz = 24;
                        } 
                    }
                }
            }
        }

        if(Silk_define.SWITCH_TRANSITION_FILTERING != 0)
        {
            /* After switching up, stop transition filter during speech inactivity */
            if( ( psEnc.sCmn.sLP.mode == 1 ) &&
                ( psEnc.sCmn.sLP.transition_frame_no >= Silk_define.TRANSITION_FRAMES_UP ) && 
                ( psEnc.speech_activity < 0.5f ) && 
                ( psEnc.sCmn.nFramesInPayloadBuf == 0 ) ) 
            {
                
                psEnc.sCmn.sLP.transition_frame_no = 0;
    
                /* Reset transition filter state */
                Arrays.fill(psEnc.sCmn.sLP.In_LP_State, 0, 2, 0);
            }
        }

        /* Resampler setup */
        if( psEnc.sCmn.fs_kHz != fs_kHz || psEnc.sCmn.prev_API_fs_Hz != API_fs_Hz )
        {
            /* Allocate space for worst case temporary upsampling, 8 to 48 kHz, so a factor 6 */
            short[] x_buf_API_fs_Hz = new short[ ( Silk_define.MAX_API_FS_KHZ / 8 ) * ( 2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_SHAPE_MAX ) ];
            short[] x_bufFIX = new short[               2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_SHAPE_MAX ]; 

            int nSamples_temp = 2 * psEnc.sCmn.frame_length + psEnc.sCmn.la_shape;

            Silk_SigProc_FLP.SKP_float2short_array( x_bufFIX, 0, psEnc.x_buf, 0, 2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_SHAPE_MAX );

            if( fs_kHz * 1000 < API_fs_Hz && psEnc.sCmn.fs_kHz != 0 ) 
            {
                /* Resample buffered data in x_buf to API_fs_Hz */

                SKP_Silk_resampler_state_struct  temp_resampler_state = new SKP_Silk_resampler_state_struct();

                /* Initialize resampler for temporary resampling of x_buf data to API_fs_Hz */
                ret += Silk_resampler.SKP_Silk_resampler_init( temp_resampler_state, psEnc.sCmn.fs_kHz * 1000, API_fs_Hz );

                /* Temporary resampling of x_buf data to API_fs_Hz */
                ret += Silk_resampler.SKP_Silk_resampler( temp_resampler_state, x_buf_API_fs_Hz,0, x_bufFIX,0, nSamples_temp );

                /* Calculate number of samples that has been temporarily upsampled */
                nSamples_temp = ( nSamples_temp * API_fs_Hz ) / ( psEnc.sCmn.fs_kHz * 1000 );

                /* Initialize the resampler for enc_API.c preparing resampling from API_fs_Hz to fs_kHz */
                ret += Silk_resampler.SKP_Silk_resampler_init( psEnc.sCmn.resampler_state, API_fs_Hz, fs_kHz * 1000 );

            } 
            else
            {
                /* Copy data */
                System.arraycopy(x_bufFIX, 0, x_buf_API_fs_Hz, 0, nSamples_temp);
            }

            if( 1000 * fs_kHz != API_fs_Hz ) 
            {
                /* Correct resampler state (unless resampling by a factor 1) by resampling buffered data from API_fs_Hz to fs_kHz */
                ret += Silk_resampler.SKP_Silk_resampler( psEnc.sCmn.resampler_state, x_bufFIX,0, x_buf_API_fs_Hz,0, nSamples_temp );
            }
            Silk_SigProc_FLP.SKP_short2float_array( psEnc.x_buf,0, x_bufFIX,0, 2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_SHAPE_MAX );
        }

        psEnc.sCmn.prev_API_fs_Hz = API_fs_Hz;

        /* Set internal sampling frequency */
        if( psEnc.sCmn.fs_kHz != fs_kHz ) {
//TODO:whether need to set 0 explicitly???            
            /* reset part of the state */
//            SKP_memset( &psEnc->sShape,          0,                            sizeof( SKP_Silk_shape_state_FLP ) );
            psEnc.sShape.memZero();
//            SKP_memset( &psEnc->sPrefilt,        0,                            sizeof( SKP_Silk_prefilter_state_FLP ) );
            psEnc.sPrefilt.memZero();
//            SKP_memset( &psEnc->sNSQ,            0,                            sizeof( SKP_Silk_nsq_state ) );
            psEnc.sNSQ.memZero();
//            SKP_memset( &psEnc->sPred,           0,                            sizeof( SKP_Silk_predict_state_FLP ) );
            psEnc.sPred.memZero();
//            SKP_memset( psEnc->sNSQ.xq,          0, ( 2 * MAX_FRAME_LENGTH ) * sizeof( SKP_float ) );
//TODO: psEnc.sNSQ.vq is of short[], why sizeof(SKP_float)???            
            Arrays.fill(psEnc.sNSQ.xq, 0, 2 * Silk_define.MAX_FRAME_LENGTH, (short)0);
//            SKP_memset( psEnc->sNSQ_LBRR.xq,     0, ( 2 * MAX_FRAME_LENGTH ) * sizeof( SKP_float ) );
            Arrays.fill(psEnc.sNSQ_LBRR.xq, (short)0);
//            SKP_memset( psEnc->sCmn.LBRR_buffer, 0,           MAX_LBRR_DELAY * sizeof( SKP_SILK_LBRR_struct ) );
            for(int i=0; i < Silk_define.MAX_LBRR_DELAY; i++)
            {
                psEnc.sCmn.LBRR_buffer[i].memZero();
            }
            
            if(Silk_define.SWITCH_TRANSITION_FILTERING != 0)
            {
                Arrays.fill(psEnc.sCmn.sLP.In_LP_State, 0, 2, 0);
                if( psEnc.sCmn.sLP.mode == 1 )
                {
                    /* Begin transition phase */
                    psEnc.sCmn.sLP.transition_frame_no = 1;
                } 
                else
                {
                    /* End transition phase */
                    psEnc.sCmn.sLP.transition_frame_no = 0;
                }
            }
            psEnc.sCmn.inputBufIx          = 0;
            psEnc.sCmn.nFramesInPayloadBuf = 0;
            psEnc.sCmn.nBytesInPayloadBuf  = 0;
            psEnc.sCmn.oldest_LBRR_idx     = 0;
            psEnc.sCmn.TargetRate_bps      = 0; /* Ensures that psEnc->SNR_dB is recomputed */

//            SKP_memset( psEnc->sPred.prev_NLSFq, 0, MAX_LPC_ORDER * sizeof( SKP_float ) );
            Arrays.fill(psEnc.sPred.prev_NLSFq, 0, Silk_define.MAX_LPC_ORDER, 0);

            /* Initialize non-zero parameters */
            psEnc.sCmn.prevLag                 = 100;
            psEnc.sCmn.prev_sigtype            = Silk_define.SIG_TYPE_UNVOICED;
            psEnc.sCmn.first_frame_after_reset = 1;
            psEnc.sPrefilt.lagPrev             = 100;
            psEnc.sShape.LastGainIndex         = 1;
            psEnc.sNSQ.lagPrev                 = 100;
            psEnc.sNSQ.prev_inv_gain_Q16       = 65536;
            psEnc.sNSQ_LBRR.prev_inv_gain_Q16  = 65536;

            psEnc.sCmn.fs_kHz = fs_kHz;
            if( psEnc.sCmn.fs_kHz == 8 ) 
            {
                psEnc.sCmn.predictLPCOrder = Silk_define.MIN_LPC_ORDER;
//                psEnc.sCmn.psNLSF_CB[ 0 ]  = &SKP_Silk_NLSF_CB0_10;
//                psEnc.sCmn.psNLSF_CB[ 1 ]  = &SKP_Silk_NLSF_CB1_10;
//                psEnc.psNLSF_CB_FLP[  0 ]  = &SKP_Silk_NLSF_CB0_10_FLP;
//                psEnc.psNLSF_CB_FLP[  1 ]  = &SKP_Silk_NLSF_CB1_10_FLP;
                psEnc.sCmn.psNLSF_CB[ 0 ]  = Silk_tables_NLSF_CB0_10.SKP_Silk_NLSF_CB0_10;
                psEnc.sCmn.psNLSF_CB[ 1 ]  = Silk_tables_NLSF_CB1_10.SKP_Silk_NLSF_CB1_10;
                psEnc.psNLSF_CB_FLP[  0 ]  = Silk_tables_NLSF_CB0_10_FLP.SKP_Silk_NLSF_CB0_10_FLP;
                psEnc.psNLSF_CB_FLP[  1 ]  = Silk_tables_NLSF_CB1_10_FLP.SKP_Silk_NLSF_CB1_10_FLP;
            } 
            else 
            {
                psEnc.sCmn.predictLPCOrder = Silk_define.MAX_LPC_ORDER;
                psEnc.sCmn.psNLSF_CB[ 0 ]  = Silk_tables_NLSF_CB0_16.SKP_Silk_NLSF_CB0_16;
                psEnc.sCmn.psNLSF_CB[ 1 ]  = Silk_tables_NLSF_CB1_16.SKP_Silk_NLSF_CB1_16;
                psEnc.psNLSF_CB_FLP[  0 ]  = Silk_tables_NLSF_CB0_16_FLP.SKP_Silk_NLSF_CB0_16_FLP;
                psEnc.psNLSF_CB_FLP[  1 ]  = Silk_tables_NLSF_CB1_16_FLP.SKP_Silk_NLSF_CB1_16_FLP;
            }
            psEnc.sCmn.frame_length   = Silk_define.FRAME_LENGTH_MS * fs_kHz;
            psEnc.sCmn.subfr_length   = psEnc.sCmn.frame_length / Silk_define.NB_SUBFR;
            psEnc.sCmn.la_pitch       = Silk_define.LA_PITCH_MS * fs_kHz;
            psEnc.sCmn.la_shape       = Silk_define.LA_SHAPE_MS * fs_kHz;
            psEnc.sPred.min_pitch_lag =  3 * fs_kHz;
            psEnc.sPred.max_pitch_lag = 18 * fs_kHz;
            psEnc.sPred.pitch_LPC_win_length = Silk_define.FIND_PITCH_LPC_WIN_MS * fs_kHz;
            if( psEnc.sCmn.fs_kHz == 24 ) 
            {
                psEnc.mu_LTP = Silk_define_FLP.MU_LTP_QUANT_SWB;
                psEnc.sCmn.bitrate_threshold_up   = Silk_typedef.SKP_int32_MAX;
                psEnc.sCmn.bitrate_threshold_down = Silk_define.SWB2WB_BITRATE_BPS; 
            } 
            else if( psEnc.sCmn.fs_kHz == 16 ) 
            {
                psEnc.mu_LTP = Silk_define_FLP.MU_LTP_QUANT_WB;
                psEnc.sCmn.bitrate_threshold_up   = Silk_define.WB2SWB_BITRATE_BPS;
                psEnc.sCmn.bitrate_threshold_down = Silk_define.WB2MB_BITRATE_BPS; 
            } 
            else if( psEnc.sCmn.fs_kHz == 12 ) 
            {
                psEnc.mu_LTP = Silk_define_FLP.MU_LTP_QUANT_MB;
                psEnc.sCmn.bitrate_threshold_up   = Silk_define.MB2WB_BITRATE_BPS;
                psEnc.sCmn.bitrate_threshold_down = Silk_define.MB2NB_BITRATE_BPS;
            } 
            else 
            {
                psEnc.mu_LTP = Silk_define_FLP.MU_LTP_QUANT_NB;
                psEnc.sCmn.bitrate_threshold_up   = Silk_define.NB2MB_BITRATE_BPS;
                psEnc.sCmn.bitrate_threshold_down = 0;
            }
            psEnc.sCmn.fs_kHz_changed = 1;

            /* Check that settings are valid */
            assert( ( psEnc.sCmn.subfr_length * Silk_define.NB_SUBFR ) == psEnc.sCmn.frame_length );
        }

        /* Check that settings are valid */
        if( Silk_define.LOW_COMPLEXITY_ONLY !=0 && Complexity != 0 ) 
        { 
            ret = Silk_errors.SKP_SILK_ENC_INVALID_COMPLEXITY_SETTING;
        }

        /* Set encoding complexity */
        if( Complexity == 0 || Silk_define.LOW_COMPLEXITY_ONLY !=0 ) 
        {
            /* Low complexity */
            psEnc.sCmn.Complexity                  = 0;
            psEnc.sCmn.pitchEstimationComplexity   = Silk_define.PITCH_EST_COMPLEXITY_LC_MODE;
            psEnc.pitchEstimationThreshold         = Silk_define_FLP.FIND_PITCH_CORRELATION_THRESHOLD_LC_MODE;
            psEnc.sCmn.pitchEstimationLPCOrder     = 8;
            psEnc.sCmn.shapingLPCOrder             = 12;
            psEnc.sCmn.nStatesDelayedDecision      = 1;
//            psEnc.NoiseShapingQuantizer            = SKP_Silk_NSQ;
            psEnc.noiseShapingQuantizerCB          = new NSQImplNSQ();
            psEnc.sCmn.useInterpolatedNLSFs        = 0;
            psEnc.sCmn.LTPQuantLowComplexity       = 1;
            psEnc.sCmn.NLSF_MSVQ_Survivors         = Silk_define.MAX_NLSF_MSVQ_SURVIVORS_LC_MODE;
        } 
        else if( Complexity == 1 )
        {
            /* Medium complexity */
            psEnc.sCmn.Complexity                  = 1;
            psEnc.sCmn.pitchEstimationComplexity   = Silk_define.PITCH_EST_COMPLEXITY_MC_MODE;
            psEnc.pitchEstimationThreshold         = Silk_define_FLP.FIND_PITCH_CORRELATION_THRESHOLD_MC_MODE;
            psEnc.sCmn.pitchEstimationLPCOrder     = 12;
            psEnc.sCmn.shapingLPCOrder             = 16;
            psEnc.sCmn.nStatesDelayedDecision      = 2;
//            psEnc.NoiseShapingQuantizer            = SKP_Silk_NSQ_del_dec;
            psEnc.noiseShapingQuantizerCB          = new NSQImplNSQDelDec();
            psEnc.sCmn.useInterpolatedNLSFs        = 0;
            psEnc.sCmn.LTPQuantLowComplexity       = 0;
            psEnc.sCmn.NLSF_MSVQ_Survivors         = Silk_define.MAX_NLSF_MSVQ_SURVIVORS_MC_MODE;
        }
        else if( Complexity == 2 )
        {
            /* High complexity */
            psEnc.sCmn.Complexity                  = 2;
            psEnc.sCmn.pitchEstimationComplexity   = Silk_define.PITCH_EST_COMPLEXITY_HC_MODE;
            psEnc.pitchEstimationThreshold         = Silk_define_FLP.FIND_PITCH_CORRELATION_THRESHOLD_HC_MODE;
            psEnc.sCmn.pitchEstimationLPCOrder     = 16;
            psEnc.sCmn.shapingLPCOrder             = 16;
            psEnc.sCmn.nStatesDelayedDecision      = 4;
//            psEnc.NoiseShapingQuantizer            = SKP_Silk_NSQ_del_dec;
            psEnc.noiseShapingQuantizerCB          = new NSQImplNSQDelDec();
            psEnc.sCmn.useInterpolatedNLSFs        = 1;
            psEnc.sCmn.LTPQuantLowComplexity       = 0;
            psEnc.sCmn.NLSF_MSVQ_Survivors         = Silk_define.MAX_NLSF_MSVQ_SURVIVORS;
        } 
        else 
        {
            ret = Silk_errors.SKP_SILK_ENC_INVALID_COMPLEXITY_SETTING;
        }

        /* Do not allow higher pitch estimation LPC order than predict LPC order */
        psEnc.sCmn.pitchEstimationLPCOrder = Math.min( psEnc.sCmn.pitchEstimationLPCOrder, psEnc.sCmn.predictLPCOrder );

        assert( psEnc.sCmn.pitchEstimationLPCOrder <= Silk_define.FIND_PITCH_LPC_ORDER_MAX );
        assert( psEnc.sCmn.shapingLPCOrder         <= Silk_define.SHAPE_LPC_ORDER_MAX      );
        assert( psEnc.sCmn.nStatesDelayedDecision  <= Silk_define.DEL_DEC_STATES_MAX       );

        /* Set bitrate/coding quality */
        TargetRate_bps = Math.min( TargetRate_bps, 100000 );
        if( psEnc.sCmn.fs_kHz == 8 ) 
        {
            TargetRate_bps = Math.max( TargetRate_bps, Silk_define.MIN_TARGET_RATE_NB_BPS );
        }
        else if( psEnc.sCmn.fs_kHz == 12 ) 
        {
            TargetRate_bps = Math.max( TargetRate_bps, Silk_define.MIN_TARGET_RATE_MB_BPS );
        }
        else if( psEnc.sCmn.fs_kHz == 16 ) 
        {
            TargetRate_bps = Math.max( TargetRate_bps, Silk_define.MIN_TARGET_RATE_WB_BPS );
        } 
        else 
        {
            TargetRate_bps = Math.max( TargetRate_bps, Silk_define.MIN_TARGET_RATE_SWB_BPS );
        }
        if( TargetRate_bps != psEnc.sCmn.TargetRate_bps ) 
        {
            psEnc.sCmn.TargetRate_bps = TargetRate_bps;

            /* If new TargetRate_bps, translate to SNR_dB value */
            if( psEnc.sCmn.fs_kHz == 8 ) 
            {
                rateTable = Silk_tables_other.TargetRate_table_NB;
            }
            else if( psEnc.sCmn.fs_kHz == 12 ) 
            {
                rateTable = Silk_tables_other.TargetRate_table_MB;
            } 
            else if( psEnc.sCmn.fs_kHz == 16 ) 
            {
                rateTable = Silk_tables_other.TargetRate_table_WB;
            } 
            else
            {
                rateTable = Silk_tables_other.TargetRate_table_SWB;
            }
            for( k = 1; k < Silk_define.TARGET_RATE_TAB_SZ; k++ ) 
            {
                /* Find bitrate interval in table and interpolate */
                if( TargetRate_bps < rateTable[ k ] )
                {
                    frac = (float)( TargetRate_bps - rateTable[ k - 1 ] ) / 
                           (float)( rateTable[ k ] - rateTable[ k - 1 ] );
                    psEnc.SNR_dB = 0.5f * ( Silk_tables_other.SNR_table_Q1[ k - 1 ] + frac * ( Silk_tables_other.SNR_table_Q1[ k ] - Silk_tables_other.SNR_table_Q1[ k - 1 ] ) );
                    break;
                }
            }
        }

        /* Set packet size */
        if( ( PacketSize_ms !=  20 ) && 
            ( PacketSize_ms !=  40 ) && 
            ( PacketSize_ms !=  60 ) && 
            ( PacketSize_ms !=  80 ) && 
            ( PacketSize_ms != 100 ) )
        {
            ret = Silk_errors.SKP_SILK_ENC_PACKET_SIZE_NOT_SUPPORTED;
        } 
        else 
        {
            if( PacketSize_ms != psEnc.sCmn.PacketSize_ms ) 
            {
                psEnc.sCmn.PacketSize_ms = PacketSize_ms;

                /* Packet length changes. Reset LBRR buffer */
                Silk_LBRR_reset.SKP_Silk_LBRR_reset( psEnc.sCmn );
            }
        }

        /* Set packet loss rate measured by farend */
        if( ( PacketLoss_perc < 0 ) || ( PacketLoss_perc > 100 ) ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_INVALID_LOSS_RATE;
        }
        psEnc.sCmn.PacketLoss_perc = PacketLoss_perc;

//    #if USE_LBRR
        if(Silk_define.USE_LBRR != 0)
        {
        if( INBandFEC_enabled < 0 || INBandFEC_enabled > 1 ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_INVALID_INBAND_FEC_SETTING;
        }
        
        /* Only change settings if first frame in packet */
        if( psEnc.sCmn.nFramesInPayloadBuf == 0 ) 
        {            
            psEnc.sCmn.LBRR_enabled = INBandFEC_enabled;
            if( psEnc.sCmn.fs_kHz == 8 ) 
            {
                LBRRRate_thres_bps = Silk_define.INBAND_FEC_MIN_RATE_BPS - 9000;
            }
            else if( psEnc.sCmn.fs_kHz == 12 ) 
            {
                LBRRRate_thres_bps = Silk_define.INBAND_FEC_MIN_RATE_BPS - 6000;;
            }
            else if( psEnc.sCmn.fs_kHz == 16 ) 
            {
                LBRRRate_thres_bps = Silk_define.INBAND_FEC_MIN_RATE_BPS - 3000;
            }
            else
            {
                LBRRRate_thres_bps = Silk_define.INBAND_FEC_MIN_RATE_BPS;
            }

            if( psEnc.sCmn.TargetRate_bps >= LBRRRate_thres_bps ) 
            {
                /* Set gain increase / rate reduction for LBRR usage */
                /* Coarsely tuned with PESQ for now. */
                /* Linear regression coefs G = 8 - 0.5 * loss */
                /* Meaning that at 16% loss main rate and redundant rate is the same, -> G = 0 */
                psEnc.sCmn.LBRR_GainIncreases = Math.max( 8 - ( psEnc.sCmn.PacketLoss_perc >> 1 ), 0 );

                /* Set main stream rate compensation */
                if( psEnc.sCmn.LBRR_enabled != 0 && psEnc.sCmn.PacketLoss_perc > Silk_define.LBRR_LOSS_THRES ) 
                {
                    /* Tuned to give aprox same mean / weighted bitrate as no inband FEC */
                    psEnc.inBandFEC_SNR_comp = 6.0f - 0.5f * psEnc.sCmn.LBRR_GainIncreases;
                } 
                else 
                {
                    psEnc.inBandFEC_SNR_comp = 0;
                    psEnc.sCmn.LBRR_enabled  = 0;
                }
            } 
            else 
            {
                psEnc.inBandFEC_SNR_comp     = 0;
                psEnc.sCmn.LBRR_enabled      = 0;
            }
        }
        }
//    #else
        else
        {
        if( INBandFEC_enabled != 0 ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_INVALID_INBAND_FEC_SETTING;
        }
        psEnc.sCmn.LBRR_enabled = 0;
//    #endif
        }

        /* Set DTX mode */
        if( DTX_enabled < 0 || DTX_enabled > 1 )
        {
            ret = Silk_errors.SKP_SILK_ENC_INVALID_DTX_SETTING;
        }
        psEnc.sCmn.useDTX = DTX_enabled;

        return ret;
    }
    
    /**
     * Control low bitrate redundancy usage.
     * @param psEnc Encoder state FLP.
     * @param psEncCtrl Encoder control.
     */
    static void SKP_Silk_LBRR_ctrl_FLP(
            SKP_Silk_encoder_state_FLP psEnc,    /* I/O Encoder state FLP*/
            SKP_Silk_encoder_control   psEncCtrl /* I/O Encoder control */
     ) 
    {
        int LBRR_usage;

        if (psEnc.sCmn.LBRR_enabled != 0) {
            /* Control LBRR */

            /* Usage Control based on sensitivity and packet loss caracteristics */
            /*
             * For now only enable adding to next for active frames. Make more
             * complex later
             */
            LBRR_usage = Silk_define.SKP_SILK_NO_LBRR;
            if (psEnc.speech_activity > Silk_define_FLP.LBRR_SPEECH_ACTIVITY_THRES
                    && psEnc.sCmn.PacketLoss_perc > Silk_define.LBRR_LOSS_THRES) 
            { // nb! maybe multiply loss prob and speech activity
                LBRR_usage = Silk_define.SKP_SILK_ADD_LBRR_TO_PLUS1;
            }
            psEncCtrl.LBRR_usage = LBRR_usage;
        } else {
            psEncCtrl.LBRR_usage = Silk_define.SKP_SILK_NO_LBRR;
        }
    }
}

/**
 * The NSQ callback implementation.
 * 
 * @author Dingxin Xu
 */
class NSQImplNSQ implements NoiseShapingQuantizerFP
{
    public void NoiseShapingQuantizer(SKP_Silk_encoder_state psEnc,
            SKP_Silk_encoder_control psEncCtrl, SKP_Silk_nsq_state NSQ,
            short[] x, byte[] q, int arg6, short[] arg7, short[] arg8,
            short[] arg9, int[] arg10, int[] arg11, int[] arg12, int[] arg13,
            int arg14, int arg15) {
        // TODO Auto-generated method stub
        Silk_NSQ.SKP_Silk_NSQ(psEnc, psEncCtrl, NSQ, x, q, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }
}

/**
 * The NSQ callback implementation
 * 
 * @author Dingxin Xu
 */
class NSQImplNSQDelDec implements NoiseShapingQuantizerFP
{
    public void NoiseShapingQuantizer(SKP_Silk_encoder_state psEnc,
            SKP_Silk_encoder_control psEncCtrl, SKP_Silk_nsq_state NSQ,
            short[] x, byte[] q, int arg6, short[] arg7, short[] arg8,
            short[] arg9, int[] arg10, int[] arg11, int[] arg12, int[] arg13,
            int arg14, int arg15) {
        // TODO Auto-generated method stub
        Silk_NSQ_del_dec.SKP_Silk_NSQ_del_dec(psEnc, psEncCtrl, NSQ, x, q, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }
}
