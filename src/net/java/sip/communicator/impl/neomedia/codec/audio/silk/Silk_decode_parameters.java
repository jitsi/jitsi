/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Decode parameters from payload
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_decode_parameters
{
    /**
     * Decode parameters from payload.
     * @param psDec decoder state.
     * @param psDecCtrl Decoder control.
     * @param q Excitation signal
     * @param fullDecoding Flag to tell if only arithmetic decoding
     */
    static void SKP_Silk_decode_parameters(
            SKP_Silk_decoder_state      psDec,             /* I/O  State                                       */
            SKP_Silk_decoder_control    psDecCtrl,         /* I/O  Decoder control                             */
            int[]                       q,                 /* O    Excitation signal                           */
            final int                   fullDecoding       /* I    Flag to tell if only arithmetic decoding    */
        )
    {
        int   i, k, Ix, fs_kHz_dec, nBytesUsed;
        int[] Ix_ptr = new int[1];
        int[]   Ixs = new int[ Silk_define.NB_SUBFR ];
        int[]   GainsIndices = new int[ Silk_define.NB_SUBFR ];
        int[]   NLSFIndices = new int[ Silk_define.NLSF_MSVQ_MAX_CB_STAGES ];
        int[]   pNLSF_Q15 = new int[ Silk_define.MAX_LPC_ORDER ];
        int []  pNLSF0_Q15 = new int[ Silk_define.MAX_LPC_ORDER ];
        
        short[] cbk_ptr_Q14;
        SKP_Silk_NLSF_CB_struct psNLSF_CB = null;
        
        SKP_Silk_range_coder_state  psRC = psDec.sRC;
        /************************/
        /* Decode sampling rate */
        /************************/
        /* only done for first frame of packet */
        if( psDec.nFramesDecoded == 0 ) {
            Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0, psRC, Silk_tables_other.SKP_Silk_SamplingRates_CDF, 0, Silk_tables_other.SKP_Silk_SamplingRates_offset );
            Ix = Ix_ptr[0];
            
            /* check that sampling rate is supported */
            if( Ix < 0 || Ix > 3 ) {
                psRC.error = Silk_define.RANGE_CODER_ILLEGAL_SAMPLING_RATE;
                return;
            }
            fs_kHz_dec = Silk_tables_other.SKP_Silk_SamplingRates_table[ Ix ];
            Silk_decoder_set_fs.SKP_Silk_decoder_set_fs( psDec, fs_kHz_dec );
        }

        /*******************************************/
        /* Decode signal type and quantizer offset */
        /*******************************************/
        if( psDec.nFramesDecoded == 0 ) {
            /* first frame in packet: independent coding */
            Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0,  psRC, Silk_tables_type_offset.SKP_Silk_type_offset_CDF, 0, Silk_tables_type_offset.SKP_Silk_type_offset_CDF_offset );
            Ix = Ix_ptr[0];
        } else {
            /* condidtional coding */
            Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0,  psRC, Silk_tables_type_offset.SKP_Silk_type_offset_joint_CDF[ psDec.typeOffsetPrev ], 0,
                    Silk_tables_type_offset.SKP_Silk_type_offset_CDF_offset );
            Ix = Ix_ptr[0];
        }
        psDecCtrl.sigtype         = Ix>>1;
        psDecCtrl.QuantOffsetType = Ix & 1;
        psDec.typeOffsetPrev      = Ix;

        /****************/
        /* Decode gains */
        /****************/
        /* first subframe */    
        if( psDec.nFramesDecoded == 0 ) {
            /* first frame in packet: independent coding */
            Silk_range_coder.SKP_Silk_range_decoder( GainsIndices, 0, psRC, Silk_tables_gain.SKP_Silk_gain_CDF[ psDecCtrl.sigtype ], 0, Silk_tables_gain.SKP_Silk_gain_CDF_offset );
        } else {
            /* condidtional coding */
            Silk_range_coder.SKP_Silk_range_decoder( GainsIndices, 0, psRC, Silk_tables_gain.SKP_Silk_delta_gain_CDF, 0, Silk_tables_gain.SKP_Silk_delta_gain_CDF_offset );
        }

        /* remaining subframes */
        for( i = 1; i < Silk_define.NB_SUBFR; i++ ) {
            Silk_range_coder.SKP_Silk_range_decoder( GainsIndices, i, psRC, Silk_tables_gain.SKP_Silk_delta_gain_CDF, 0, Silk_tables_gain.SKP_Silk_delta_gain_CDF_offset );
        }
        
        /* Dequant Gains */
        int LastGainIndex_ptr[] = new int[1];
        LastGainIndex_ptr[0] = psDec.LastGainIndex;
        Silk_gain_quant.SKP_Silk_gains_dequant( psDecCtrl.Gains_Q16, GainsIndices, LastGainIndex_ptr, psDec.nFramesDecoded );
        psDec.LastGainIndex = LastGainIndex_ptr[0];
        
        /****************/
        /* Decode NLSFs */
        /****************/
        /* Set pointer to NLSF VQ CB for the current signal type */
        psNLSF_CB = psDec.psNLSF_CB[ psDecCtrl.sigtype ];

        /* Arithmetically decode NLSF path */
        Silk_range_coder.SKP_Silk_range_decoder_multi( NLSFIndices, psRC, psNLSF_CB.StartPtr, psNLSF_CB.MiddleIx, psNLSF_CB.nStages );

        
        /* From the NLSF path, decode an NLSF vector */
        Silk_NLSF_MSVQ_decode.SKP_Silk_NLSF_MSVQ_decode( pNLSF_Q15, psNLSF_CB, NLSFIndices, psDec.LPC_order );


        /************************************/
        /* Decode NLSF interpolation factor */
        /************************************/
        int[] NLSFInterpCoef_Q2_ptr = new int[1];
        NLSFInterpCoef_Q2_ptr[0] = psDecCtrl.NLSFInterpCoef_Q2;
        
        Silk_range_coder.SKP_Silk_range_decoder( NLSFInterpCoef_Q2_ptr, 0, psRC, Silk_tables_other.SKP_Silk_NLSF_interpolation_factor_CDF, 0, 
                Silk_tables_other.SKP_Silk_NLSF_interpolation_factor_offset );
        psDecCtrl.NLSFInterpCoef_Q2 = NLSFInterpCoef_Q2_ptr[0];
        
        /* If just reset, e.g., because internal Fs changed, do not allow interpolation */
        /* improves the case of packet loss in the first frame after a switch           */
        if( psDec.first_frame_after_reset == 1 ) {
            psDecCtrl.NLSFInterpCoef_Q2 = 4;
        }

        if( fullDecoding !=0) {
            /* Convert NLSF parameters to AR prediction filter coefficients */
            Silk_NLSF2A_stable.SKP_Silk_NLSF2A_stable( psDecCtrl.PredCoef_Q12[ 1 ], pNLSF_Q15, psDec.LPC_order );
            if( psDecCtrl.NLSFInterpCoef_Q2 < 4 ) {
                /* Calculation of the interpolated NLSF0 vector from the interpolation factor, */ 
                /* the previous NLSF1, and the current NLSF1                                   */
                for( i = 0; i < psDec.LPC_order; i++ ) {
                    pNLSF0_Q15[ i ] = psDec.prevNLSF_Q15[ i ] + ( ( psDecCtrl.NLSFInterpCoef_Q2 * 
                            ( pNLSF_Q15[ i ] - psDec.prevNLSF_Q15[ i ] ) ) >> 2 );
                }

                /* Convert NLSF parameters to AR prediction filter coefficients */
                Silk_NLSF2A_stable.SKP_Silk_NLSF2A_stable( psDecCtrl.PredCoef_Q12[ 0 ], pNLSF0_Q15, psDec.LPC_order );
            } else {
                /* Copy LPC coefficients for first half from second half */
                System.arraycopy(psDecCtrl.PredCoef_Q12[1], 0, psDecCtrl.PredCoef_Q12[0], 0, psDec.LPC_order);
            }
        }

        System.arraycopy(pNLSF_Q15, 0, psDec.prevNLSF_Q15, 0, psDec.LPC_order);
            
        /* After a packet loss do BWE of LPC coefs */
        if( psDec.lossCnt !=0 ) {
            Silk_bwexpander.SKP_Silk_bwexpander( psDecCtrl.PredCoef_Q12[ 0 ], psDec.LPC_order, Silk_define.BWE_AFTER_LOSS_Q16 );
            Silk_bwexpander.SKP_Silk_bwexpander( psDecCtrl.PredCoef_Q12[ 1 ], psDec.LPC_order, Silk_define.BWE_AFTER_LOSS_Q16 );
        }

        if( psDecCtrl.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            /*********************/
            /* Decode pitch lags */
            /*********************/
            /* Get lag index */
            if( psDec.fs_kHz == 8 ) {
                Silk_range_coder.SKP_Silk_range_decoder(Ixs, 0, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_NB_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_NB_CDF_offset);
            } else if( psDec.fs_kHz == 12 ) {
                Silk_range_coder.SKP_Silk_range_decoder(Ixs, 0, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_MB_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_MB_CDF_offset);
            } else if( psDec.fs_kHz == 16 ) {
                Silk_range_coder.SKP_Silk_range_decoder(Ixs, 0, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_WB_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_WB_CDF_offset);
            } else {
                Silk_range_coder.SKP_Silk_range_decoder(Ixs, 0, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_SWB_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_lag_SWB_CDF_offset);
            }
            
            /* Get countour index */
            if( psDec.fs_kHz == 8 ) {
                /* Less codevectors used in 8 khz mode */
                Silk_range_coder.SKP_Silk_range_decoder(Ixs, 1, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_contour_NB_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_contour_NB_CDF_offset);
            } else {
                /* Joint for 12, 16, and 24 khz */
//                SKP_Silk_range_decoder( &Ixs[ 1 ], psRC, SKP_Silk_pitch_contour_CDF, SKP_Silk_pitch_contour_CDF_offset );
                Silk_range_coder.SKP_Silk_range_decoder( Ixs, 1, psRC, Silk_tables_pitch_lag.SKP_Silk_pitch_contour_CDF, 0, Silk_tables_pitch_lag.SKP_Silk_pitch_contour_CDF_offset );
            }
            
            /* Decode pitch values */
            Silk_decode_pitch.SKP_Silk_decode_pitch( Ixs[ 0 ], Ixs[ 1 ], psDecCtrl.pitchL, psDec.fs_kHz );
            
            /********************/
            /* Decode LTP gains */
            /********************/
            /* Decode PERIndex value */
            int PERIndex_ptr[] = new int[1];
            PERIndex_ptr[0] =  psDecCtrl.PERIndex;
            
            Silk_range_coder.SKP_Silk_range_decoder( PERIndex_ptr, 0,  psRC, Silk_tables_LTP.SKP_Silk_LTP_per_index_CDF, 0, 
                    Silk_tables_LTP.SKP_Silk_LTP_per_index_CDF_offset );
            psDecCtrl.PERIndex = PERIndex_ptr[0];
            
            /* Decode Codebook Index */
            cbk_ptr_Q14 = Silk_tables_LTP.SKP_Silk_LTP_vq_ptrs_Q14[ psDecCtrl.PERIndex ]; // set pointer to start of codebook
            
            for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
                Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0, psRC, Silk_tables_LTP.SKP_Silk_LTP_gain_CDF_ptrs[psDecCtrl.PERIndex],  0 , 
                        Silk_tables_LTP.SKP_Silk_LTP_gain_CDF_offsets[ psDecCtrl.PERIndex ] );
                Ix = Ix_ptr[0];
                for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                    psDecCtrl.LTPCoef_Q14[ Silk_macros.SKP_SMULBB( k, Silk_define.LTP_ORDER ) + i ] = cbk_ptr_Q14[ Silk_macros.SKP_SMULBB( Ix, Silk_define.LTP_ORDER ) + i ];
                }
            }

            /**********************/
            /* Decode LTP scaling */
            /**********************/
            Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0, psRC, Silk_tables_other.SKP_Silk_LTPscale_CDF, 0, Silk_tables_other.SKP_Silk_LTPscale_offset );
            Ix = Ix_ptr[0];
            psDecCtrl.LTP_scale_Q14 = Silk_tables_other.SKP_Silk_LTPScales_table_Q14[ Ix ];
        } else {
            Arrays.fill(psDecCtrl.pitchL, 0, Silk_define.NB_SUBFR, 0);
            Arrays.fill(psDecCtrl.LTPCoef_Q14, 0, Silk_define.NB_SUBFR, (short)0);
            psDecCtrl.PERIndex      = 0;
            psDecCtrl.LTP_scale_Q14 = 0;
        }

        /***************/
        /* Decode seed */
        /***************/
        Silk_range_coder.SKP_Silk_range_decoder( Ix_ptr, 0, psRC, Silk_tables_other.SKP_Silk_Seed_CDF, 0, Silk_tables_other.SKP_Silk_Seed_offset );
        Ix = Ix_ptr[0];
        psDecCtrl.Seed = ( int )Ix;
        /*********************************************/
        /* Decode quantization indices of excitation */
        /*********************************************/
        Silk_decode_pulses.SKP_Silk_decode_pulses( psRC, psDecCtrl, q, psDec.frame_length );

        /*********************************************/
        /* Decode VAD flag                           */
        /*********************************************/
        int[] vadFlag_ptr = new int[1];
        vadFlag_ptr[0] = psDec.vadFlag;
        Silk_range_coder.SKP_Silk_range_decoder( vadFlag_ptr, 0, psRC, Silk_tables_other.SKP_Silk_vadflag_CDF, 0, Silk_tables_other.SKP_Silk_vadflag_offset );
        psDec.vadFlag = vadFlag_ptr[0];
        
        /**************************************/
        /* Decode Frame termination indicator */
        /**************************************/
        int[] FrameTermination_ptr = new int[1];
        FrameTermination_ptr[0] = psDec.FrameTermination;
        Silk_range_coder.SKP_Silk_range_decoder( FrameTermination_ptr, 0, psRC, Silk_tables_other.SKP_Silk_FrameTermination_CDF, 0, Silk_tables_other.SKP_Silk_FrameTermination_offset );
        psDec.FrameTermination = FrameTermination_ptr[0];
        
        /****************************************/
        /* get number of bytes used so far      */
        /****************************************/
        int nBytesUsed_ptr[] = new int[1];
        Silk_range_coder.SKP_Silk_range_coder_get_length( psRC, nBytesUsed_ptr );
        nBytesUsed = nBytesUsed_ptr[0];
        
        psDec.nBytesLeft = psRC.bufferLength - nBytesUsed;
        if( psDec.nBytesLeft < 0 ) {
            psRC.error = Silk_define.RANGE_CODER_READ_BEYOND_BUFFER;
        }

        /****************************************/
        /* check remaining bits in last byte    */
        /****************************************/
        if( psDec.nBytesLeft == 0 ) {
            Silk_range_coder.SKP_Silk_range_coder_check_after_decoding( psRC );
        }
    }
}
