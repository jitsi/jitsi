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
 * @author Dingxin Xu
 */
public class Silk_prefilter_FLP 
{
    /**
     * SKP_Silk_prefilter. Main Prefilter Function.
     * @param psEnc Encoder state FLP.
     * @param psEncCtrl Encoder control FLP.
     * @param xw  Weighted signal.
     * @param x Speech signal.
     * @param x_offset offset of valid data.
     */
    static void SKP_Silk_prefilter_FLP(
        SKP_Silk_encoder_state_FLP    psEnc,         /* I/O  Encoder state FLP                       */
        SKP_Silk_encoder_control_FLP  psEncCtrl,     /* I    Encoder control FLP                     */
              float                   xw[],          /* O    Weighted signal                         */
              float                   x[],           /* I    Speech signal                           */
              int                     x_offset
    )
    {
        SKP_Silk_prefilter_state_FLP P = psEnc.sPrefilt;
        int   j, k, lag;
        float HarmShapeGain, Tilt, LF_MA_shp, LF_AR_shp;
        float[] B = new float[ 2 ];
        float[] AR1_shp = new float[ Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX ];
        float[] px;
        int px_offset;
        float[] pxw, pst_res;
        int pxw_offset;
        int pst_res_offset;
        float[] HarmShapeFIR = new float[ 3 ];
        float[] st_res = new float[ Silk_define.MAX_FRAME_LENGTH / Silk_define.NB_SUBFR + Silk_define.MAX_LPC_ORDER ];

        /* Setup pointers */
        px  = x;
        px_offset = x_offset;
        pxw = xw;
        pxw_offset = 0;
        lag = P.lagPrev;
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) 
        {
            /* Update Variables that change per sub frame */
            if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) 
            {
                lag = psEncCtrl.sCmn.pitchL[ k ];
            }

            /* Noise shape parameters */
            HarmShapeGain = psEncCtrl.HarmShapeGain[ k ] * ( 1.0f - psEncCtrl.HarmBoost[ k ] );
            HarmShapeFIR[ 0 ] = Silk_tables_other_FLP.SKP_Silk_HarmShapeFIR_FLP[ 0 ] * HarmShapeGain;
            HarmShapeFIR[ 1 ] = Silk_tables_other_FLP.SKP_Silk_HarmShapeFIR_FLP[ 1 ] * HarmShapeGain;
            HarmShapeFIR[ 2 ] = Silk_tables_other_FLP.SKP_Silk_HarmShapeFIR_FLP[ 2 ] * HarmShapeGain;
            Tilt      =  psEncCtrl.Tilt[ k ];
            LF_MA_shp =  psEncCtrl.LF_MA_shp[ k ];
            LF_AR_shp =  psEncCtrl.LF_AR_shp[ k ];
//TODO: copy the psEncCtrl.AR1 to a local buffer or use a reference(pointer) to the struct???            
//            AR1_shp   = psEncCtrl.AR1;
//            AR1_shp_offset = k * Silk_define.SHAPE_LPC_ORDER_MAX;
            Arrays.fill(AR1_shp, 0);
            System.arraycopy(psEncCtrl.AR1,  k * Silk_define.SHAPE_LPC_ORDER_MAX, 
                    AR1_shp, 0, psEncCtrl.AR1.length-k * Silk_define.SHAPE_LPC_ORDER_MAX);

            /* Short term FIR filtering*/
            Silk_LPC_analysis_filter_FLP.SKP_Silk_LPC_analysis_filter_FLP( st_res, AR1_shp, 
                    px, px_offset - psEnc.sCmn.shapingLPCOrder, 
                    psEnc.sCmn.subfr_length + psEnc.sCmn.shapingLPCOrder, psEnc.sCmn.shapingLPCOrder );

            pst_res = st_res;
            pst_res_offset = psEnc.sCmn.shapingLPCOrder; // Point to first sample

            /* reduce (mainly) low frequencies during harmonic emphasis */
            B[ 0 ] =  psEncCtrl.GainsPre[ k ];
            B[ 1 ] = -psEncCtrl.GainsPre[ k ] * 
                ( psEncCtrl.HarmBoost[ k ] * HarmShapeGain + Silk_perceptual_parameters_FLP.INPUT_TILT + 
                        psEncCtrl.coding_quality * Silk_perceptual_parameters_FLP.HIGH_RATE_INPUT_TILT );
            pxw[ pxw_offset + 0 ] = B[ 0 ] * pst_res[ pst_res_offset + 0 ] + B[ 1 ] * P.sHarmHP;
            for( j = 1; j < psEnc.sCmn.subfr_length; j++ ) {
                pxw[ pxw_offset + j ] = B[ 0 ] * pst_res[ pst_res_offset + j ] + B[ 1 ] * pst_res[ pst_res_offset + j - 1 ];
            }
            P.sHarmHP = pst_res[ pst_res_offset + psEnc.sCmn.subfr_length - 1 ];

            SKP_Silk_prefilt_FLP( P, pxw, pxw_offset, pxw, pxw_offset, HarmShapeFIR, Tilt, LF_MA_shp, LF_AR_shp, lag, psEnc.sCmn.subfr_length );
            
            px_offset  += psEnc.sCmn.subfr_length;
            pxw_offset += psEnc.sCmn.subfr_length;
        }
        P.lagPrev = psEncCtrl.sCmn.pitchL[ Silk_define.NB_SUBFR - 1 ];
    }

    /**
     * SKP_Silk_prefilter_part1. Prefilter for finding Quantizer input signal.  
     * @param P
     * @param st_res
     * @param st_res_offset
     * @param xw
     * @param xw_offset
     * @param HarmShapeFIR
     * @param Tilt
     * @param LF_MA_shp
     * @param LF_AR_shp
     * @param lag
     * @param length
     */
    static void SKP_Silk_prefilt_FLP(
        SKP_Silk_prefilter_state_FLP P,/* (I/O) state */
        float st_res[],             /* (I) */
        int   st_res_offset,
        float xw[],                 /* (O) */
        int   xw_offset,
        float []HarmShapeFIR,        /* (I) */
        float Tilt,                 /* (I) */
        float LF_MA_shp,            /* (I) */
        float LF_AR_shp,            /* (I) */
        int   lag,                  /* (I) */
        int   length                /* (I) */
    )
    {
        int   i;
        int   idx, LTP_shp_buf_idx;
        float n_Tilt, n_LF, n_LTP; 
        float sLF_AR_shp, sLF_MA_shp;
        float []LTP_shp_buf;

        /* To speed up use temp variables instead of using the struct */
        LTP_shp_buf     = P.sLTP_shp1;
        LTP_shp_buf_idx = P.sLTP_shp_buf_idx1;
        sLF_AR_shp      = P.sLF_AR_shp1;
        sLF_MA_shp      = P.sLF_MA_shp1;
            
        for( i = 0; i < length; i++ ) {
            if( lag > 0 ) {
                assert( Silk_define.HARM_SHAPE_FIR_TAPS == 3 );
                idx = lag + LTP_shp_buf_idx;
                n_LTP  = LTP_shp_buf[ ( idx - Silk_define.HARM_SHAPE_FIR_TAPS / 2 - 1) & Silk_define.LTP_MASK ] * HarmShapeFIR[ 0 ];
                n_LTP += LTP_shp_buf[ ( idx - Silk_define.HARM_SHAPE_FIR_TAPS / 2    ) & Silk_define.LTP_MASK ] * HarmShapeFIR[ 1 ];
                n_LTP += LTP_shp_buf[ ( idx - Silk_define.HARM_SHAPE_FIR_TAPS / 2 + 1) & Silk_define.LTP_MASK ] * HarmShapeFIR[ 2 ];
            } else {
                n_LTP = 0;
            }

            n_Tilt = sLF_AR_shp * Tilt;
            n_LF   = sLF_AR_shp * LF_AR_shp + sLF_MA_shp * LF_MA_shp;

            sLF_AR_shp = st_res[ st_res_offset + i ] - n_Tilt;
            sLF_MA_shp = sLF_AR_shp - n_LF;

            LTP_shp_buf_idx = ( LTP_shp_buf_idx - 1 ) & Silk_define.LTP_MASK;
            LTP_shp_buf[ LTP_shp_buf_idx ] = sLF_MA_shp;
            xw[ xw_offset + i ] = sLF_MA_shp - n_LTP;
        }
        /* Copy temp variable back to state */
        P.sLF_AR_shp1       = sLF_AR_shp;
        P.sLF_MA_shp1       = sLF_MA_shp;
        P.sLTP_shp_buf_idx1 = LTP_shp_buf_idx;
    }
}
