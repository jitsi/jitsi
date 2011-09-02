/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * The Decoder API.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_dec_API 
{

    /**
     * Reset the decoder state.
     * 
     * @param decState the decoder state
     * @return ret 
     */
    static int SKP_Silk_SDK_InitDecoder(
            Object decState                                      /* I/O: State                                          */
    )
    {
        int ret = 0;
        SKP_Silk_decoder_state struc;
        struc = (SKP_Silk_decoder_state )decState;
        ret  = Silk_create_init_destroy.SKP_Silk_init_decoder( struc );

        return ret;
    }

    /**
     * Decode a frame.
     * 
     * @param decState the decoder state.
     * @param decControl the decoder control.
     * @param lostFlag the lost flag. 0: no loss; 1: loss.
     * @param inData encoding input vector.
     * @param inData_offset the actual data offset in the input vector.
     * @param nBytesIn number of input bytes.
     * @param samplesOut decoded output speech vector.
     * @param samplesOut_offset the actual data offset in the output vector.
     * @param nSamplesOut number of samples.
     * @return the returned value carries the error message. 
     * 0 indicates OK; other indicates error.
     */
    static int SKP_Silk_SDK_Decode(
            Object                               decState,       /* I/O: State                                           */
            SKP_SILK_SDK_DecControlStruct        decControl,     /* I/O: Control structure                               */
            int                                  lostFlag,       /* I:   0: no loss, 1 loss                              */
            byte[]                               inData,        /* I:   Encoded input vector                            */
            int                                  inData_offset,
            final int                            nBytesIn,       /* I:   Number of input Bytes                           */
            short[]                              samplesOut,    /* O:   Decoded output speech vector                    */
            int                                     samplesOut_offset,
            short[]                              nSamplesOut    /* I/O: Number of samples (vector/decoded)              */
    )
    {
        int ret = 0, used_bytes, prev_fs_kHz;
        SKP_Silk_decoder_state psDec;
        
        psDec = (SKP_Silk_decoder_state )decState;

        /**********************************/
        /* Test if first frame in payload */
        /**********************************/
        if( psDec.moreInternalDecoderFrames == 0 ) {
            /* First Frame in Payload */
            psDec.nFramesDecoded = 0;  /* Used to count frames in packet */
        }

        if( psDec.moreInternalDecoderFrames == 0 &&    /* First frame in packet    */
            lostFlag == 0 &&                            /* Not packet loss          */
            nBytesIn > Silk_define.MAX_ARITHM_BYTES ) {             /* Too long payload         */
                /* Avoid trying to decode a too large packet */
                lostFlag = 1;
                ret = Silk_errors.SKP_SILK_DEC_PAYLOAD_TOO_LARGE;
        }
                
        /* Save previous sample frequency */
        prev_fs_kHz = psDec.fs_kHz;
        
        /* Call decoder for one frame */
        int[] used_bytes_ptr =new int[1];
        ret += Silk_decode_frame.SKP_Silk_decode_frame( psDec, samplesOut, samplesOut_offset, nSamplesOut, inData, inData_offset,
                nBytesIn, lostFlag, used_bytes_ptr );
        used_bytes = used_bytes_ptr[0];
        
        if( used_bytes !=0) /* Only Call if not a packet loss */
        { 
            if( psDec.nBytesLeft > 0 && psDec.FrameTermination == Silk_define.SKP_SILK_MORE_FRAMES && psDec.nFramesDecoded < 5 ) {
                /* We have more frames in the Payload */
                psDec.moreInternalDecoderFrames = 1;
            } else {
                /* Last frame in Payload */
                psDec.moreInternalDecoderFrames = 0;
                psDec.nFramesInPacket = psDec.nFramesDecoded;
            
                /* Track inband FEC usage */
                if( psDec.vadFlag == Silk_define.VOICE_ACTIVITY ) {
                    if( psDec.FrameTermination == Silk_define.SKP_SILK_LAST_FRAME ) {
                        psDec.no_FEC_counter++;
                        if( psDec.no_FEC_counter > Silk_define.NO_LBRR_THRES ) {
                            psDec.inband_FEC_offset = 0;
                        }
                    } else if( psDec.FrameTermination == Silk_define.SKP_SILK_LBRR_VER1 ) {
                        psDec.inband_FEC_offset = 1; /* FEC info with 1 packet delay */
                        psDec.no_FEC_counter    = 0;
                    } else if( psDec.FrameTermination == Silk_define.SKP_SILK_LBRR_VER2 ) {
                        psDec.inband_FEC_offset = 2; /* FEC info with 2 packets delay */
                        psDec.no_FEC_counter    = 0;
                    }
                }
            }
        }

        if( Silk_define.MAX_API_FS_KHZ * 1000 < decControl.API_sampleRate ||
            8000       > decControl.API_sampleRate ) {
            ret = Silk_errors.SKP_SILK_DEC_INVALID_SAMPLING_FREQUENCY;
            return( ret );
        }

        /* Resample if needed */
        if( psDec.fs_kHz * 1000 != decControl.API_sampleRate ) { 
            short[] samplesOut_tmp = new short[Silk_define.MAX_API_FS_KHZ * Silk_define.FRAME_LENGTH_MS];
            Silk_typedef.SKP_assert( psDec.fs_kHz <= Silk_define.MAX_API_FS_KHZ );

            /* Copy to a tmp buffer as the resampling writes to samplesOut */
            System.arraycopy(samplesOut, samplesOut_offset+0, samplesOut_tmp, 0, nSamplesOut[0]);
            /* (Re-)initialize resampler state when switching internal sampling frequency */
            if( prev_fs_kHz != psDec.fs_kHz || psDec.prev_API_sampleRate != decControl.API_sampleRate ) {
                ret = Silk_resampler.SKP_Silk_resampler_init( psDec.resampler_state, psDec.fs_kHz*1000, decControl.API_sampleRate );
            }

            /* Resample the output to API_sampleRate */
            ret += Silk_resampler.SKP_Silk_resampler( psDec.resampler_state, samplesOut, samplesOut_offset, samplesOut_tmp, 0, nSamplesOut[0]);

            /* Update the number of output samples */
            nSamplesOut[0] = (short)((nSamplesOut[0] * decControl.API_sampleRate) / (psDec.fs_kHz * 1000));
        }

        psDec.prev_API_sampleRate = decControl.API_sampleRate;

        /* Copy all parameters that are needed out of internal structure to the control stucture */
        decControl.frameSize                 = psDec.frame_length;
        decControl.framesPerPacket           = psDec.nFramesInPacket;
        decControl.inBandFECOffset           = psDec.inband_FEC_offset;
        decControl.moreInternalDecoderFrames = psDec.moreInternalDecoderFrames;

        return ret;
    }

    /**
     * Find LBRR information in a packet.
     * 
     * @param inData encoded input vector.
     * @param inData_offset offset of the valid data.
     * @param nBytesIn number of input bytes.
     * @param lost_offset offset from lost packet.
     * @param LBRRData LBRR payload.
     * @param LBRRData_offset offset of the valid data.
     * @param nLBRRBytes number of LBRR bytes.
     */
    static void SKP_Silk_SDK_search_for_LBRR(
            byte[]                          inData,        /* I:   Encoded input vector                            */
            int                             inData_offset,
            final short                     nBytesIn,       /* I:   Number of input Bytes                           */
            int                             lost_offset,    /* I:   Offset from lost packet                         */
            byte[]                          LBRRData,      /* O:   LBRR payload                                    */
            int                                LBRRData_offset,
            short[]                         nLBRRBytes     /* O:   Number of LBRR Bytes                            */
    )
    {
        SKP_Silk_decoder_state   sDec = new SKP_Silk_decoder_state(); // Local decoder state to avoid interfering with running decoder */
        SKP_Silk_decoder_control sDecCtrl = new SKP_Silk_decoder_control();
        int[] TempQ = new int[ Silk_define.MAX_FRAME_LENGTH ];

        if( lost_offset < 1 || lost_offset > Silk_define.MAX_LBRR_DELAY ) {
            /* No useful FEC in this packet */
            nLBRRBytes[0] = 0;
            return;
        }

        sDec.nFramesDecoded = 0;
        sDec.fs_kHz         = 0; /* Force update parameters LPC_order etc */
        Arrays.fill(sDec.prevNLSF_Q15, 0, Silk_define.MAX_LPC_ORDER, 0);
        
        for(int i=0; i<Silk_define.MAX_LPC_ORDER; i++)
            sDec.prevNLSF_Q15[i] = 0;
        
        Silk_range_coder.SKP_Silk_range_dec_init( sDec.sRC, inData, inData_offset, nBytesIn );
        
        while(true) {
            Silk_decode_parameters.SKP_Silk_decode_parameters(sDec, sDecCtrl, TempQ, 0);
            if( sDec.sRC.error!=0 ) {
                /* Corrupt stream */
                nLBRRBytes[0] = 0;
                return;
            }
//TODO:note the semicolon;
//          };
            if( (( sDec.FrameTermination - 1 ) & lost_offset)!=0 && sDec.FrameTermination > 0 && sDec.nBytesLeft >= 0 ) {
                /* The wanted FEC is present in the packet */
                nLBRRBytes[0] = (short)sDec.nBytesLeft;
                System.arraycopy(inData, inData_offset+nBytesIn - sDec.nBytesLeft, LBRRData, LBRRData_offset+0, sDec.nBytesLeft);
                break;
            }
            if( sDec.nBytesLeft > 0 && sDec.FrameTermination == Silk_define.SKP_SILK_MORE_FRAMES ) {
                sDec.nFramesDecoded++;
            } else {
                LBRRData = null;
                nLBRRBytes[0] = 0;
                break;
            }
        }
    }

    /**
     * Getting type of content for a packet.
     * @param inData encoded input vector.
     * @param nBytesIn number of input bytes.
     * @param Silk_TOC type of content.
     */
    static void SKP_Silk_SDK_get_TOC(
             byte[]                        inData,        /* I:   Encoded input vector                            */
             final short                   nBytesIn,       /* I:   Number of input bytes                           */
            SKP_Silk_TOC_struct            Silk_TOC       /* O:   Type of content                                 */
        )
    {
        SKP_Silk_decoder_state      sDec = new SKP_Silk_decoder_state();
        SKP_Silk_decoder_control    sDecCtrl = new SKP_Silk_decoder_control();
        int[] TempQ = new int[ Silk_define.MAX_FRAME_LENGTH ];

        sDec.nFramesDecoded = 0;
        sDec.fs_kHz         = 0; /* Force update parameters LPC_order etc */
        Silk_range_coder.SKP_Silk_range_dec_init( sDec.sRC, inData, 0,  nBytesIn );

        Silk_TOC.corrupt = 0;
        while( true ) {
            Silk_decode_parameters.SKP_Silk_decode_parameters( sDec, sDecCtrl, TempQ, 0 );
            
            Silk_TOC.vadFlags[     sDec.nFramesDecoded ] = sDec.vadFlag;
            Silk_TOC.sigtypeFlags[ sDec.nFramesDecoded ] = sDecCtrl.sigtype;
        
            if( sDec.sRC.error != 0) {
                /* Corrupt stream */
                Silk_TOC.corrupt = 1;
                break;
            }    
//TODO:note the semicolon;
//          };
            if( sDec.nBytesLeft > 0 && sDec.FrameTermination == Silk_define.SKP_SILK_MORE_FRAMES ) {
                sDec.nFramesDecoded++;
            } else {
                break;
            }
        }
        if( Silk_TOC.corrupt !=0 || sDec.FrameTermination == Silk_define.SKP_SILK_MORE_FRAMES || 
            sDec.nFramesInPacket > Silk_SDK_API.SILK_MAX_FRAMES_PER_PACKET ) {
            /* Corrupt packet */    
            {
                Silk_TOC.corrupt = 0;
                Silk_TOC.framesInPacket = 0;
                Silk_TOC.fs_kHz = 0;
                Silk_TOC.inbandLBRR = 0;
                
                for(int i=0; i<Silk_TOC.vadFlags.length; i++)
                    Silk_TOC.vadFlags[i] = 0;
                for(int i=0; i<Silk_TOC.sigtypeFlags.length; i++)
                    Silk_TOC.sigtypeFlags[i] = 0;
            }
            Silk_TOC.corrupt = 1;
        } else {
            Silk_TOC.framesInPacket = sDec.nFramesDecoded + 1;
            Silk_TOC.fs_kHz         = sDec.fs_kHz;
            if( sDec.FrameTermination == Silk_define.SKP_SILK_LAST_FRAME ) {
                Silk_TOC.inbandLBRR = sDec.FrameTermination;
            } else {
                Silk_TOC.inbandLBRR = sDec.FrameTermination - 1;
            }
        }
    }
    
    /**
     * Get the version number.
     * @return the string specifying the version number.
     */
    static String SKP_Silk_SDK_get_version()
    {
        String version = "1.0.6";
        return version;
    }
}
