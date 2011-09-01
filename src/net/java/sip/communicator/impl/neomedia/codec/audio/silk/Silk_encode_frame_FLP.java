/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Encode frame.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_encode_frame_FLP
{
    /**
     * TODO: TEST
     */
    static int frame_cnt = 0;
    /**
     * Encode frame.
     * @param psEnc Encoder state FLP
     * @param pCode payload
     * @param pCode_offset offset of valid data.
     * @param pnBytesOut Number of payload bytes; input: max length; output: used.
     * @param pIn Input speech frame
     * @param pIn_offset offset of valid data.
     * @return
     */
    static int SKP_Silk_encode_frame_FLP( 
        SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
              byte[]                    pCode,
              int                       pCode_offset,
              short[]                   pnBytesOut,        /* I/O  Number of payload bytes;                */
                                                           /*      input: max length; output: used         */
              short[]                   pIn,                /* I    Input speech frame                      */
              int                       pIn_offset
    )
    {
        SKP_Silk_encoder_control_FLP sEncCtrl = new SKP_Silk_encoder_control_FLP();
        int     k, nBytes[] = new int[1], ret = 0;
        float[]   x_frame, res_pitch_frame;
        int x_frame_offset, res_pitch_frame_offset;
        short[]   pIn_HP = new short[    Silk_define.MAX_FRAME_LENGTH ];
        short[]   pIn_HP_LP = new short[ Silk_define.MAX_FRAME_LENGTH ];
        float[]   xfw = new float[       Silk_define.MAX_FRAME_LENGTH ];
        float[]   res_pitch = new float[ 2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_PITCH_MAX ];
        int     LBRR_idx, frame_terminator;

        /* Low bitrate redundancy parameters */
        byte[] LBRRpayload = new byte[Silk_define.MAX_ARITHM_BYTES];
        short[]   nBytesLBRR = new short[1];

        int[] FrameTermination_CDF;


        sEncCtrl.sCmn.Seed = psEnc.sCmn.frameCounter++ & 3;
        /**************************************************************/
        /* Setup Input Pointers, and insert frame in input buffer    */
        /*************************************************************/
        /* pointers aligned with start of frame to encode */
        x_frame                = psEnc.x_buf;
        x_frame_offset         = psEnc.x_buf_offset + psEnc.sCmn.frame_length; // start of frame to encode
        res_pitch_frame        = res_pitch;
        res_pitch_frame_offset = psEnc.sCmn.frame_length; // start of pitch LPC residual frame

        /****************************/
        /* Voice Activity Detection */
        /****************************/
        Silk_wrappers_FLP.SKP_Silk_VAD_FLP( psEnc, sEncCtrl, pIn, pIn_offset );

        /*******************************************/
        /* High-pass filtering of the input signal */
        /*******************************************/
        if (Silk_define.HIGH_PASS_INPUT !=0) {
            /* Variable high-pass filter */
            Silk_HP_variable_cutoff_FLP.SKP_Silk_HP_variable_cutoff_FLP( psEnc, sEncCtrl, pIn_HP, 0, pIn, pIn_offset );
        } else {
            System.arraycopy(pIn, pIn_offset, pIn_HP, 0, psEnc.sCmn.frame_length);
        }

        
//        /*TEST****************************************************************************/
//        /**
//         * test for psEnc.x_buf
//         */
//        short[]       pin_hp = pIn_HP;
//        String pin_hp_filename = "D:/gsoc/pin_hp/pin_hp";
//        pin_hp_filename += frame_cnt;
//        DataInputStream pin_hp_datain = null;
//        try
//        {
//            pin_hp_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(pin_hp_filename)));
//            byte[] buffer = new byte[2];
//            for(int ii = 0; ii < pin_hp.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = pin_hp_datain.read(buffer);
//                    if(res != buffer.length)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    pin_hp[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
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
//            if(pin_hp_datain != null)
//            {
//                try
//                {
//                    pin_hp_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
/*TEST END****************************************************************************/        
        if (Silk_define.SWITCH_TRANSITION_FILTERING != 0) {
            /* Ensure smooth bandwidth transitions */
            Silk_LP_variable_cutoff.SKP_Silk_LP_variable_cutoff( psEnc.sCmn.sLP, pIn_HP_LP, 0, pIn_HP, 0, psEnc.sCmn.frame_length );
        } else {
            System.arraycopy(pIn_HP, 0, pIn_HP_LP, 0, psEnc.sCmn.frame_length);
        }
        
///*TEST****************************************************************************/
//        /**
//         * test for psEnc.x_buf
//         */
//        short[]       pin_hp_lp = pIn_HP_LP;
//        String pin_hp_lp_filename = "D:/gsoc/pin_hp_lp/pin_hp_lp";
//        pin_hp_lp_filename += frame_cnt;
//        DataInputStream pin_hp_lp_datain = null;
//        try
//        {
//            pin_hp_lp_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(pin_hp_lp_filename)));
//            byte[] buffer = new byte[2];
//            for(int ii = 0; ii < pin_hp_lp.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = pin_hp_lp_datain.read(buffer);
//                    if(res != buffer.length)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    pin_hp_lp[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
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
//            if(pin_hp_lp_datain != null)
//            {
//                try
//                {
//                    pin_hp_lp_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
///*TEST END****************************************************************************/             
        
        
///*TEST****************************************************************************/
//        /**
//         * test for psEnc.x_buf
//         */
//        float[]       x_buf = psEnc.x_buf;
//        String x_buf_filename = "D:/gsoc/x_buf/x_buf";
//        x_buf_filename += frame_cnt;
//        DataInputStream x_buf_datain = null;
//        try
//        {
//            x_buf_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(x_buf_filename)));
//            byte[] buffer = new byte[4];
//            for(int ii = 0; ii < x_buf.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = x_buf_datain.read(buffer);
//                    if(res != 4)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    x_buf[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
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
//            if(x_buf_datain != null)
//            {
//                try
//                {
//                    x_buf_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
///*TEST END****************************************************************************/
        /*******************************************/
        /* Copy new frame to front of input buffer */
        /*******************************************/
        Silk_SigProc_FLP.SKP_short2float_array( x_frame, x_frame_offset +psEnc.sCmn.la_shape, 
                pIn_HP_LP, 0, psEnc.sCmn.frame_length );

///*TEST****************************************************************************/
//        /**
//         * test for psEnc.x_buf
//         */
//        float[]       x_buf = psEnc.x_buf;
//        String x_buf_filename = "D:/gsoc/x_buf/x_buf";
//        x_buf_filename += frame_cnt;
//        DataInputStream x_buf_datain = null;
//        try
//        {
//            x_buf_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(x_buf_filename)));
//            byte[] buffer = new byte[4];
//            for(int ii = 0; ii < x_buf.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = x_buf_datain.read(buffer);
//                    if(res != 4)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    x_buf[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
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
//            if(x_buf_datain != null)
//            {
//                try
//                {
//                    x_buf_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
///*TEST END****************************************************************************/        

        /* Add tiny signal to avoid high CPU load from denormalized floating point numbers */
        for( k = 0; k < 8; k++ ) {
            x_frame[ x_frame_offset + psEnc.sCmn.la_shape + k * ( psEnc.sCmn.frame_length >> 3 ) ] += ( 1 - ( k & 2 ) ) * 1e-6f;
        }
/*TEST****************************************************************************/
        /**
         * test for psEnc.x_buf
         */
        /**
         * Test for NLSF
         */
//        float[]       x_buf = new float[ psEnc.x_buf.length ];
//        float[]       x_buf = psEnc.x_buf;
//        String x_buf_filename = "D:/gsoc/x_buf/x_buf";
//        x_buf_filename += frame_cnt;
//        DataInputStream x_buf_datain = null;
//        try
//        {
//            x_buf_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(x_buf_filename)));
//            byte[] buffer = new byte[4];
//            for(int ii = 0; ii < x_buf.length; ii++ )
//            {
//                try
//                {
//                    
//                    int res = x_buf_datain.read(buffer);
//                    if(res != 4)
//                    {
//                        throw new IOException("Unexpected End of Stream");
//                    }
//                    x_buf[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
////                    NLSF[ii] = nlsf[ii];
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
//            if(x_buf_datain != null)
//            {
//                try
//                {
//                    x_buf_datain.close();
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//        frame_cnt++;
/*TEST END****************************************************************************/        
        /*****************************************/
        /* Find pitch lags, initial LPC analysis */
        /*****************************************/
        Silk_find_pitch_lags_FLP.SKP_Silk_find_pitch_lags_FLP( psEnc, sEncCtrl, res_pitch, x_frame, x_frame_offset );

        /************************/
        /* Noise shape analysis */
        /************************/
        Silk_noise_shape_analysis_FLP.SKP_Silk_noise_shape_analysis_FLP( psEnc, sEncCtrl, 
                res_pitch_frame, res_pitch_frame_offset, x_frame, x_frame_offset );

        /*****************************************/
        /* Prefiltering for noise shaper         */
        /*****************************************/
        Silk_prefilter_FLP.SKP_Silk_prefilter_FLP( psEnc, sEncCtrl, xfw, x_frame, x_frame_offset );

        /***************************************************/
        /* Find linear prediction coefficients (LPC + LTP) */
        /***************************************************/
        Silk_find_pred_coefs_FLP.SKP_Silk_find_pred_coefs_FLP( psEnc, sEncCtrl, res_pitch );

        /****************************************/
        /* Process gains                        */
        /****************************************/
        Silk_process_gains_FLP.SKP_Silk_process_gains_FLP( psEnc, sEncCtrl );
        
        /****************************************/
        /* Low Bitrate Redundant Encoding       */
        /****************************************/
        nBytesLBRR[0] = Silk_define.MAX_ARITHM_BYTES;
        SKP_Silk_LBRR_encode_FLP( psEnc, sEncCtrl, LBRRpayload, nBytesLBRR, xfw );

        /*****************************************/
        /* Noise shaping quantization            */
        /*****************************************/
        Silk_wrappers_FLP.SKP_Silk_NSQ_wrapper_FLP( psEnc, sEncCtrl, xfw, 0, psEnc.sCmn.q, 0, 0 );

        /**************************************************/
        /* Convert speech activity into VAD and DTX flags */
        /**************************************************/
        if( psEnc.speech_activity < Silk_define_FLP.SPEECH_ACTIVITY_DTX_THRES ) {
            psEnc.sCmn.vadFlag = Silk_define.NO_VOICE_ACTIVITY;
            psEnc.sCmn.noSpeechCounter++;
            if( psEnc.sCmn.noSpeechCounter > Silk_define.NO_SPEECH_FRAMES_BEFORE_DTX ) {
                psEnc.sCmn.inDTX = 1;
            }
            if( psEnc.sCmn.noSpeechCounter > Silk_define.MAX_CONSECUTIVE_DTX ) {
                psEnc.sCmn.noSpeechCounter = 0;
                psEnc.sCmn.inDTX           = 0;
            }
        } else {
            psEnc.sCmn.noSpeechCounter = 0;
            psEnc.sCmn.inDTX           = 0;
            psEnc.sCmn.vadFlag         = Silk_define.VOICE_ACTIVITY;
        }

        /****************************************/
        /* Initialize arithmetic coder          */
        /****************************************/
        if( psEnc.sCmn.nFramesInPayloadBuf == 0 ) 
        {
            Silk_range_coder.SKP_Silk_range_enc_init( psEnc.sCmn.sRC );
            psEnc.sCmn.nBytesInPayloadBuf = 0;
        }

        /****************************************/
        /* Encode Parameters                    */
        /****************************************/
        Silk_encode_parameters.SKP_Silk_encode_parameters( psEnc.sCmn, sEncCtrl.sCmn, psEnc.sCmn.sRC, psEnc.sCmn.q );
        FrameTermination_CDF = Silk_tables_other.SKP_Silk_FrameTermination_CDF;

        /****************************************/
        /* Update Buffers and State             */
        /****************************************/
        /* Update input buffer */
        System.arraycopy(psEnc.x_buf, psEnc.x_buf_offset + psEnc.sCmn.frame_length, 
                psEnc.x_buf, psEnc.x_buf_offset, psEnc.sCmn.frame_length + psEnc.sCmn.la_shape);
        
        /* Parameters needed for next frame */
        psEnc.sCmn.prev_sigtype = sEncCtrl.sCmn.sigtype;
        psEnc.sCmn.prevLag      = sEncCtrl.sCmn.pitchL[ Silk_define.NB_SUBFR - 1];
        psEnc.sCmn.first_frame_after_reset = 0;

        if( psEnc.sCmn.sRC.error != 0 ) {
            /* Encoder returned error: Clear payload buffer */
            psEnc.sCmn.nFramesInPayloadBuf = 0;
        } else {
            psEnc.sCmn.nFramesInPayloadBuf++;
        }

        /****************************************/
        /* Finalize payload and copy to output  */
        /****************************************/
        if( psEnc.sCmn.nFramesInPayloadBuf * Silk_define.FRAME_LENGTH_MS >= psEnc.sCmn.PacketSize_ms ) {

            LBRR_idx = ( psEnc.sCmn.oldest_LBRR_idx + 1 ) & Silk_define.LBRR_IDX_MASK;

            /* Check if FEC information should be added */
            frame_terminator = Silk_define.SKP_SILK_LAST_FRAME;
            if( psEnc.sCmn.LBRR_buffer[ LBRR_idx ].usage == Silk_define.SKP_SILK_ADD_LBRR_TO_PLUS1 ) {
                frame_terminator = Silk_define.SKP_SILK_LBRR_VER1;
            }
            if( psEnc.sCmn.LBRR_buffer[ psEnc.sCmn.oldest_LBRR_idx ].usage == Silk_define.SKP_SILK_ADD_LBRR_TO_PLUS2 ) {
                frame_terminator = Silk_define.SKP_SILK_LBRR_VER2;
                LBRR_idx = psEnc.sCmn.oldest_LBRR_idx;
            }

            /* Add the frame termination info to stream */
            Silk_range_coder.SKP_Silk_range_encoder( psEnc.sCmn.sRC, frame_terminator, FrameTermination_CDF,0 );

            /* Payload length so far */
            Silk_range_coder.SKP_Silk_range_coder_get_length( psEnc.sCmn.sRC, nBytes );

            /* Check that there is enough space in external output buffer, and move data */
            if( pnBytesOut[0] >= nBytes[0] ) {
                Silk_range_coder.SKP_Silk_range_enc_wrap_up( psEnc.sCmn.sRC );
                System.arraycopy(psEnc.sCmn.sRC.buffer, 0, pCode, pCode_offset, nBytes[0]);

                if( frame_terminator > Silk_define.SKP_SILK_MORE_FRAMES && 
                        pnBytesOut[0] >= nBytes[0] + psEnc.sCmn.LBRR_buffer[ LBRR_idx ].nBytes ) {
                    /* Get old packet and add to payload. */
                    System.arraycopy(psEnc.sCmn.LBRR_buffer[ LBRR_idx ].payload, 0, 
                            pCode, pCode_offset+nBytes[0], psEnc.sCmn.LBRR_buffer[ LBRR_idx ].nBytes);
                    nBytes[0] += psEnc.sCmn.LBRR_buffer[ LBRR_idx ].nBytes;
                }
                pnBytesOut[0] = (short) nBytes[0];
            
                /* Update FEC buffer */
                System.arraycopy(LBRRpayload, 0, 
                        psEnc.sCmn.LBRR_buffer[ psEnc.sCmn.oldest_LBRR_idx ].payload, 0, nBytesLBRR[0]);
                psEnc.sCmn.LBRR_buffer[ psEnc.sCmn.oldest_LBRR_idx ].nBytes = nBytesLBRR[0];
                /* The below line describes how FEC should be used */ 
                psEnc.sCmn.LBRR_buffer[ psEnc.sCmn.oldest_LBRR_idx ].usage = sEncCtrl.sCmn.LBRR_usage;
                psEnc.sCmn.oldest_LBRR_idx = ( ( psEnc.sCmn.oldest_LBRR_idx + 1 ) & Silk_define.LBRR_IDX_MASK );

                /* Reset the number of frames in payload buffer */
                psEnc.sCmn.nFramesInPayloadBuf = 0;
            } else {
                /* Not enough space: Payload will be discarded */
                pnBytesOut[0] = 0;
                nBytes[0]      = 0;
                psEnc.sCmn.nFramesInPayloadBuf = 0;
                ret = Silk_errors.SKP_SILK_ENC_PAYLOAD_BUF_TOO_SHORT;
            }
        } else {
            /* No payload for you this time */
            pnBytesOut[0] = 0;

            /* Encode that more frames follows */
            frame_terminator = Silk_define.SKP_SILK_MORE_FRAMES;
            Silk_range_coder.SKP_Silk_range_encoder( psEnc.sCmn.sRC, frame_terminator, FrameTermination_CDF, 0);

            /* Payload length so far */
            Silk_range_coder.SKP_Silk_range_coder_get_length( psEnc.sCmn.sRC, nBytes );
        }

        /* Check for arithmetic coder errors */
        if( psEnc.sCmn.sRC.error != 0 ) {
            ret = Silk_errors.SKP_SILK_ENC_INTERNAL_ERROR;
        }

        /* simulate number of ms buffered in channel because of exceeding TargetRate */
        psEnc.BufferedInChannel_ms   += ( 8.0f * 1000.0f * ( nBytes[0] - psEnc.sCmn.nBytesInPayloadBuf ) ) / psEnc.sCmn.TargetRate_bps;
        psEnc.BufferedInChannel_ms   -= Silk_define.FRAME_LENGTH_MS;
        psEnc.BufferedInChannel_ms    = Silk_SigProc_FLP.SKP_LIMIT_float( psEnc.BufferedInChannel_ms, 0.0f, 100.0f );
        psEnc.sCmn.nBytesInPayloadBuf = nBytes[0];

        if( psEnc.speech_activity > Silk_define_FLP.WB_DETECT_ACTIVE_SPEECH_LEVEL_THRES ) {
            psEnc.sCmn.sSWBdetect.ActiveSpeech_ms = Silk_SigProc_FIX.SKP_ADD_POS_SAT32( psEnc.sCmn.sSWBdetect.ActiveSpeech_ms, Silk_define.FRAME_LENGTH_MS ); 
        }

        return( ret );
    }
    
    /**
     * Low Bitrate Redundancy (LBRR) encoding. Reuse all parameters but encode with lower bitrate.
     * @param psEnc Encoder state FLP.
     * @param psEncCtrl Encoder control FLP.
     * @param pCode Payload.
     * @param pnBytesOut Payload bytes; in: max; out: used.
     * @param xfw Input signal.
     */
    static void SKP_Silk_LBRR_encode_FLP(
        SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
        SKP_Silk_encoder_control_FLP    psEncCtrl,         /* I/O  Encoder control FLP                     */
              byte                      []pCode,             /* O    Payload                                 */
              short                     []pnBytesOut,        /* I/O  Payload bytes; in: max; out: used       */
              float                     xfw[]               /* I    Input signal                            */
    )
    {
        int[]   Gains_Q16 = new int[ Silk_define.NB_SUBFR ];
        int     k, TempGainsIndices[] = new int[ Silk_define.NB_SUBFR ], frame_terminator;
        int     nBytes[] = new int[1], nFramesInPayloadBuf;
        float   TempGains[] = new float[ Silk_define.NB_SUBFR ];
        int     typeOffset, LTP_scaleIndex, Rate_only_parameters = 0;
        /* Control use of inband LBRR */
        Silk_control_codec_FLP.SKP_Silk_LBRR_ctrl_FLP( psEnc, psEncCtrl.sCmn );

        if( psEnc.sCmn.LBRR_enabled != 0 ) {
            /* Save original gains */
            System.arraycopy(psEncCtrl.sCmn.GainsIndices, 0, TempGainsIndices, 0, Silk_define.NB_SUBFR);
            System.arraycopy(psEncCtrl.Gains, 0, TempGains, 0, Silk_define.NB_SUBFR);

            typeOffset     = psEnc.sCmn.typeOffsetPrev; // Temp save as cannot be overwritten
            LTP_scaleIndex = psEncCtrl.sCmn.LTP_scaleIndex;

            /* Set max rate where quant signal is encoded */
            if( psEnc.sCmn.fs_kHz == 8 ) {
                Rate_only_parameters = 13500;
            } else if( psEnc.sCmn.fs_kHz == 12 ) {
                Rate_only_parameters = 15500;
            } else if( psEnc.sCmn.fs_kHz == 16 ) {
                Rate_only_parameters = 17500;
            } else if( psEnc.sCmn.fs_kHz == 24 ) {
                Rate_only_parameters = 19500;
            } else {
                assert( false );
            }

            if( psEnc.sCmn.Complexity > 0 && psEnc.sCmn.TargetRate_bps > Rate_only_parameters ) {
                if( psEnc.sCmn.nFramesInPayloadBuf == 0 ) {
                    /* First frame in packet copy everything */
//TODO:use clone rather than memory copy.               
                    psEnc.sNSQ_LBRR = (SKP_Silk_nsq_state) psEnc.sNSQ.clone();
                    
                    psEnc.sCmn.LBRRprevLastGainIndex = psEnc.sShape.LastGainIndex;
                    /* Increase Gains to get target LBRR rate */
                    psEncCtrl.sCmn.GainsIndices[ 0 ] += psEnc.sCmn.LBRR_GainIncreases;
                    psEncCtrl.sCmn.GainsIndices[ 0 ]  = Silk_SigProc_FIX.SKP_LIMIT( psEncCtrl.sCmn.GainsIndices[ 0 ], 0, Silk_define.N_LEVELS_QGAIN - 1 );
                }
                /* Decode to get Gains in sync with decoder */
                int LBRRprevLastGainIndex_ptr[] = new int[1];
                LBRRprevLastGainIndex_ptr[0] = psEnc.sCmn.LBRRprevLastGainIndex;
                Silk_gain_quant.SKP_Silk_gains_dequant( Gains_Q16, psEncCtrl.sCmn.GainsIndices, 
                    LBRRprevLastGainIndex_ptr, psEnc.sCmn.nFramesInPayloadBuf );
                psEnc.sCmn.LBRRprevLastGainIndex = LBRRprevLastGainIndex_ptr[0];

                /* Overwrite unquantized gains with quantized gains and convert back to Q0 from Q16 */
                for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
                    psEncCtrl.Gains[ k ] = Gains_Q16[ k ] / 65536.0f;
                }

                /*****************************************/
                /* Noise shaping quantization            */
                /*****************************************/
                Silk_wrappers_FLP.SKP_Silk_NSQ_wrapper_FLP( psEnc, psEncCtrl, xfw, 0, psEnc.sCmn.q_LBRR, 0, 1 );
            } else {
                Arrays.fill(psEnc.sCmn.q_LBRR, (byte)0);
                psEncCtrl.sCmn.LTP_scaleIndex = 0;
            }
            /****************************************/
            /* Initialize arithmetic coder          */
            /****************************************/
            if( psEnc.sCmn.nFramesInPayloadBuf == 0 ) {
                Silk_range_coder.SKP_Silk_range_enc_init( psEnc.sCmn.sRC_LBRR );
                psEnc.sCmn.nBytesInPayloadBuf = 0;
            }

            /****************************************/
            /* Encode Parameters                    */
            /****************************************/
            Silk_encode_parameters.SKP_Silk_encode_parameters( psEnc.sCmn, psEncCtrl.sCmn, psEnc.sCmn.sRC_LBRR, psEnc.sCmn.q_LBRR );
            /****************************************/
            /* Encode Parameters                    */
            /****************************************/
            if( psEnc.sCmn.sRC_LBRR.error != 0) {
                /* Encoder returned error: Clear payload buffer */
                nFramesInPayloadBuf = 0;
            } else {
                nFramesInPayloadBuf = psEnc.sCmn.nFramesInPayloadBuf + 1;
            }

            /****************************************/
            /* Finalize payload and copy to output  */
            /****************************************/
            if( Silk_macros.SKP_SMULBB( nFramesInPayloadBuf, Silk_define.FRAME_LENGTH_MS ) >= psEnc.sCmn.PacketSize_ms ) {

                /* Check if FEC information should be added */
                frame_terminator = Silk_define.SKP_SILK_LAST_FRAME;

                /* Add the frame termination info to stream */
                Silk_range_coder.SKP_Silk_range_encoder( psEnc.sCmn.sRC_LBRR, frame_terminator, Silk_tables_other.SKP_Silk_FrameTermination_CDF, 0 );

                /* Payload length so far */
                Silk_range_coder.SKP_Silk_range_coder_get_length( psEnc.sCmn.sRC_LBRR, nBytes );

                /* Check that there is enough space in external output buffer and move data */
                if( pnBytesOut[0] >= nBytes[0] ) {
                    Silk_range_coder.SKP_Silk_range_enc_wrap_up( psEnc.sCmn.sRC_LBRR );
                    System.arraycopy(psEnc.sCmn.sRC_LBRR.buffer, 0, pCode, 0, nBytes[0]);
                    
                    pnBytesOut[0] = (short) nBytes[0];               
                } else {
                    /* Not enough space: Payload will be discarded */
                    pnBytesOut[0] = 0;
                    assert( false );
                }
            } else {
                /* No payload for you this time */
                pnBytesOut[0] = 0;

                /* Encode that more frames follows */
                frame_terminator = Silk_define.SKP_SILK_MORE_FRAMES;
                Silk_range_coder.SKP_Silk_range_encoder( psEnc.sCmn.sRC_LBRR, frame_terminator, Silk_tables_other.SKP_Silk_FrameTermination_CDF, 0 );
            }

            /* Restore original Gains */
            System.arraycopy(TempGainsIndices, 0, psEncCtrl.sCmn.GainsIndices, 0, Silk_define.NB_SUBFR);
            System.arraycopy(TempGains, 0, psEncCtrl.Gains, 0, Silk_define.NB_SUBFR);
        
            /* Restore LTP scale index and typeoffset */
            psEncCtrl.sCmn.LTP_scaleIndex = LTP_scaleIndex;
            psEnc.sCmn.typeOffsetPrev     = typeOffset;
        }
    }
}
