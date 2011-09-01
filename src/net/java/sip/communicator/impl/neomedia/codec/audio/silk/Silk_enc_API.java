/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Encoder API.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_enc_API 
{
    /***
     * TODO: TEST
     */
    static int frame_cnt = 0;
    
    /**
     * Read control structure from encoder.
     * @param encState State Vecotr.
     * @param encStatus Control Structure.
     * @return
     */
    static int SKP_Silk_SDK_QueryEncoder
    (
        Object encState,                        /* I:   State Vector                                    */
        SKP_SILK_SDK_EncControlStruct encStatus /* O:   Control Structure                               */
    )
    {
        SKP_Silk_encoder_state_FLP psEnc;
        int ret = 0;    

        psEnc = ( SKP_Silk_encoder_state_FLP )encState;

        encStatus.API_sampleRate        = psEnc.sCmn.API_fs_Hz;
        encStatus.maxInternalSampleRate = Silk_macros.SKP_SMULBB( psEnc.sCmn.maxInternal_fs_kHz, 1000 );
        encStatus.packetSize            = ( int )( psEnc.sCmn.API_fs_Hz * psEnc.sCmn.PacketSize_ms / 1000 );  /* convert samples -> ms */
        encStatus.bitRate               = psEnc.sCmn.TargetRate_bps;
        encStatus.packetLossPercentage  = psEnc.sCmn.PacketLoss_perc;
        encStatus.complexity            = psEnc.sCmn.Complexity;
        encStatus.useInBandFEC          = psEnc.sCmn.useInBandFEC;
        encStatus.useDTX                = psEnc.sCmn.useDTX;
        return ret;
    }

    /**
     * Init or Reset encoder.
     * @param encState
     * @param encStatus
     * @return
     */
    static int SKP_Silk_SDK_InitEncoder
    (
        Object                            encState,          /* I/O: State                                           */
        SKP_SILK_SDK_EncControlStruct     encStatus          /* O:   Control structure                               */
    )
    {
        SKP_Silk_encoder_state_FLP psEnc;
        int ret = 0;

            
        psEnc = ( SKP_Silk_encoder_state_FLP )encState;

        /* Reset Encoder */
        if( (ret += Silk_init_encoder_FLP.SKP_Silk_init_encoder_FLP( psEnc )) != 0 ) 
        {
            assert( false );
        }

        /* Read control structure */
        if( (ret += SKP_Silk_SDK_QueryEncoder( encState, encStatus )) != 0 )
        {
            assert( false );
        }
        return ret;
    }
    
    /**
     * Encode frame with Silk.
     * @param encState State
     * @param encControl Control structure
     * @param samplesIn Speech sample input vector
     * @param samplesIn_offset offset of valid data.
     * @param nSamplesIn Number of samples in input vector
     * @param outData Encoded output vector
     * @param outData_offset offset of valid data.
     * @param nBytesOut  Number of bytes in outData (input: Max bytes)
     * @return
     */
    static int SKP_Silk_SDK_Encode
    ( 
        Object                         encState,      /* I/O: State                                           */
        SKP_SILK_SDK_EncControlStruct  encControl,    /* I:   Control structure                               */
        final short[]                  samplesIn,     /* I:   Speech sample input vector                      */
        int samplesIn_offset,
        int                            nSamplesIn,    /* I:   Number of samples in input vector               */
        byte[]                         outData,       /* O:   Encoded output vector                           */
        int outData_offset,
        short[]                        nBytesOut      /* I/O: Number of bytes in outData (input: Max bytes)   */
    )
    {
        int   max_internal_fs_kHz, PacketSize_ms, PacketLoss_perc, UseInBandFEC, UseDTX, ret = 0;
        int   nSamplesToBuffer, Complexity, input_ms, nSamplesFromInput = 0;
        int TargetRate_bps, API_fs_Hz;
        short MaxBytesOut;
        SKP_Silk_encoder_state_FLP psEnc = ( SKP_Silk_encoder_state_FLP )encState;

        assert( encControl != null );

        /* Check sampling frequency first, to avoid divide by zero later */
        if( ( ( encControl.API_sampleRate        !=  8000 ) &&
              ( encControl.API_sampleRate        != 12000 ) &&
              ( encControl.API_sampleRate        != 16000 ) &&
              ( encControl.API_sampleRate        != 24000 ) && 
              ( encControl.API_sampleRate        != 32000 ) &&
              ( encControl.API_sampleRate        != 44100 ) &&
              ( encControl.API_sampleRate        != 48000 ) ) ||
            ( ( encControl.maxInternalSampleRate !=  8000 ) &&
              ( encControl.maxInternalSampleRate != 12000 ) &&
              ( encControl.maxInternalSampleRate != 16000 ) &&
              ( encControl.maxInternalSampleRate != 24000 ) ) ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_FS_NOT_SUPPORTED;
            assert( false );
            return( ret );
        }

        /* Set encoder parameters from control structure */
        API_fs_Hz           = encControl.API_sampleRate;
        max_internal_fs_kHz = ( int )encControl.maxInternalSampleRate / 1000;   /* convert Hz -> kHz */
        PacketSize_ms       = 1000 * ( int )encControl.packetSize / API_fs_Hz;
        TargetRate_bps      = ( int )encControl.bitRate;
        PacketLoss_perc     = ( int )encControl.packetLossPercentage;
        UseInBandFEC        = ( int )encControl.useInBandFEC;
        Complexity          = ( int )encControl.complexity;
        UseDTX              = ( int )encControl.useDTX;
        /* Save values in state */
        psEnc.sCmn.API_fs_Hz          = API_fs_Hz;
        psEnc.sCmn.maxInternal_fs_kHz = max_internal_fs_kHz;
        psEnc.sCmn.useInBandFEC       = UseInBandFEC;

        /* Only accept input lengths that are a multiplum of 10 ms */
        input_ms = 1000 * nSamplesIn / API_fs_Hz;
        if( ( input_ms % 10) != 0 || nSamplesIn < 0 ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_INPUT_INVALID_NO_OF_SAMPLES;
            assert( false );
            return( ret );
        }

        /* Make sure no more than one packet can be produced */
        if( nSamplesIn > (int)( psEnc.sCmn.PacketSize_ms * API_fs_Hz / 1000 ) ) 
        {
            ret = Silk_errors.SKP_SILK_ENC_INPUT_INVALID_NO_OF_SAMPLES;
            assert( false );
            return( ret );
        }
        if( ( ret = Silk_control_codec_FLP.SKP_Silk_control_encoder_FLP( psEnc, API_fs_Hz, max_internal_fs_kHz, PacketSize_ms, TargetRate_bps, 
                        PacketLoss_perc, UseInBandFEC, UseDTX, input_ms, Complexity) ) != 0 ) 
        {
            assert( false );
            return( ret );
        }
        /* Detect energy above 8 kHz */
        if( Math.min( API_fs_Hz, 1000 * max_internal_fs_kHz ) == 24000 && psEnc.sCmn.sSWBdetect.SWB_detected == 0 && psEnc.sCmn.sSWBdetect.WB_detected == 0 )
        {
            Silk_detect_SWB_input.SKP_Silk_detect_SWB_input( psEnc.sCmn.sSWBdetect, samplesIn,samplesIn_offset, ( int )nSamplesIn );
        }

        /* Input buffering/resampling and encoding */
        MaxBytesOut = 0;                    /* return 0 output bytes if no encoder called */
        while( true )
        {
            nSamplesToBuffer = psEnc.sCmn.frame_length - psEnc.sCmn.inputBufIx;
            if( API_fs_Hz == Silk_macros.SKP_SMULBB( 1000, psEnc.sCmn.fs_kHz ) ) 
            { 
                nSamplesToBuffer  = Math.min( nSamplesToBuffer, nSamplesIn );
                nSamplesFromInput = nSamplesToBuffer;
                /* Copy to buffer */
                System.arraycopy(samplesIn, samplesIn_offset, psEnc.sCmn.inputBuf, psEnc.sCmn.inputBufIx, nSamplesFromInput);
            } 
            else 
            {  
                nSamplesToBuffer  = Math.min( nSamplesToBuffer, ( int )nSamplesIn * psEnc.sCmn.fs_kHz * 1000 / API_fs_Hz );
                nSamplesFromInput = (int)( nSamplesToBuffer * API_fs_Hz / ( psEnc.sCmn.fs_kHz * 1000 ) );
                /* Resample and write to buffer */
                ret += Silk_resampler.SKP_Silk_resampler(psEnc.sCmn.resampler_state, 
                        psEnc.sCmn.inputBuf, psEnc.sCmn.inputBufIx, samplesIn, samplesIn_offset, nSamplesFromInput);
                
///*TEST****************************************************************************/
//                /**
//                 * test for inputbuf
//                 */
//                short[]       inputbuf = psEnc.sCmn.inputBuf;
//                String inputbuf_filename = "D:/gsoc/inputbuf-res/inputbuf-res";
//                inputbuf_filename += frame_cnt;
//                DataInputStream inputbuf_datain = null;
//                try
//                {
//                    inputbuf_datain = new DataInputStream(
//                                      new FileInputStream(
//                                          new File(inputbuf_filename)));
//                    byte[] buffer = new byte[2];
//                    for(int ii = 0; ii < inputbuf.length; ii++ )
//                    {
//                        try
//                        {
//                            
//                            int res = inputbuf_datain.read(buffer);
//                            if(res != buffer.length)
//                            {
//                                throw new IOException("Unexpected End of Stream");
//                            }
//                            inputbuf[ii] = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
//                        }
//                        catch (IOException e)
//                        {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    } 
//                }
//                catch (FileNotFoundException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//                finally
//                {
//                    if(inputbuf_datain != null)
//                    {
//                        try
//                        {
//                            inputbuf_datain.close();
//                        }
//                        catch (IOException e)
//                        {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                frame_cnt++;
///*TEST END****************************************************************************/     
            } 
            samplesIn_offset              += nSamplesFromInput;
            nSamplesIn             -= nSamplesFromInput;
            psEnc.sCmn.inputBufIx += nSamplesToBuffer;        
            
            /* Silk encoder */
            if( psEnc.sCmn.inputBufIx >= psEnc.sCmn.frame_length ) 
            {
                /* Enough data in input buffer, so encode */
                if( MaxBytesOut == 0 ) 
                {
                    /* No payload obtained so far */
                    MaxBytesOut = nBytesOut[0];
                    short MaxBytesOut_ptr[] = new short[1];
                    MaxBytesOut_ptr[0] = MaxBytesOut;
//                    if( ( ret = Silk_encode_frame_FLP.SKP_Silk_encode_frame_FLP( psEnc, outData, outData_offset, 
//                            MaxBytesOut_ptr, psEnc.sCmn.inputBuf, psEnc.sCmn.inputBufIx ) ) != 0 )
                    if( ( ret = Silk_encode_frame_FLP.SKP_Silk_encode_frame_FLP( psEnc, outData, outData_offset, 
                            MaxBytesOut_ptr, psEnc.sCmn.inputBuf, 0 ) ) != 0 )
                    {
                        assert( false );
                    }
                    MaxBytesOut = MaxBytesOut_ptr[0];
                } 
                else
                {
                    /* outData already contains a payload */
//                    if( ( ret = Silk_encode_frame_FLP.SKP_Silk_encode_frame_FLP( psEnc, outData, outData_offset,
//                            nBytesOut, psEnc.sCmn.inputBuf, psEnc.sCmn.inputBufIx) ) != 0 ) 
                    if( ( ret = Silk_encode_frame_FLP.SKP_Silk_encode_frame_FLP( psEnc, outData, outData_offset,
                           nBytesOut, psEnc.sCmn.inputBuf, 0) ) != 0 ) 
                    {
                        assert( false );
                    }
                    /* Check that no second payload was created */
                    assert( nBytesOut[0] == 0 );
                }
                psEnc.sCmn.inputBufIx = 0;
            } 
            else
            {
                break;
            }
        }

        nBytesOut[0] = MaxBytesOut;
        if( psEnc.sCmn.useDTX!=0 && psEnc.sCmn.inDTX!=0 )
        {
            /* DTX simulation */
            nBytesOut[0] = 0;
        }

        return ret;
    }
}
