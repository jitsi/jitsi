/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_wrappers_FLP 
{
    /* Wrappers. Calls flp / fix code */
    static long ar2_q13_file_offset = 0;
    static long x_16_file_offset = 0;
    static int frame_cnt = 0;
    
    /* Convert AR filter coefficients to NLSF parameters */
    static void SKP_Silk_A2NLSF_FLP( 
              float[]               pNLSF,             /* O    NLSF vector      [ LPC_order ]          */
              float[]               pAR,               /* I    LPC coefficients [ LPC_order ]          */
        final int                   LPC_order          /* I    LPC order                               */
    )
    {
        int   i;
        int[]   NLSF_fix = new int[  Silk_define.MAX_LPC_ORDER ];
        int[] a_fix_Q16 = new int[ Silk_define.MAX_LPC_ORDER ];

        for( i = 0; i < LPC_order; i++ ) 
        {
            a_fix_Q16[ i ] = Silk_SigProc_FLP.SKP_float2int( pAR[ i ] * 65536.0f );
        }
        Silk_A2NLSF.SKP_Silk_A2NLSF( NLSF_fix, a_fix_Q16, LPC_order );

        for( i = 0; i < LPC_order; i++ ) 
        {
            pNLSF[ i ] = ( float )NLSF_fix[ i ] * ( 1.0f / 32768.0f );
        }
    }

    /* Convert LSF parameters to AR prediction filter coefficients */
    static void SKP_Silk_NLSF2A_stable_FLP( 
              float []                pAR,               /* O    LPC coefficients [ LPC_order ]          */
              float[]                 pNLSF,             /* I    NLSF vector      [ LPC_order ]          */
        final int                     LPC_order          /* I    LPC order                               */
    )
    {
        int   i;
        int[]   NLSF_fix = new int[  Silk_define.MAX_LPC_ORDER ];
        short[] a_fix_Q12 = new short[ Silk_define.MAX_LPC_ORDER ];

        for( i = 0; i < LPC_order; i++ ) 
        {
            NLSF_fix[ i ] = ( int )Silk_SigProc_FLP.SKP_float2int( pNLSF[ i ] * 32768.0f );
        }

        Silk_NLSF2A_stable.SKP_Silk_NLSF2A_stable( a_fix_Q12, NLSF_fix, LPC_order );

        for( i = 0; i < LPC_order; i++ ) 
        {
            pAR[ i ] = ( float )a_fix_Q12[ i ] / 4096.0f;
        }
    }


    /* LSF stabilizer, for a single input data vector */
    static void SKP_Silk_NLSF_stabilize_FLP(
              float[]                 pNLSF,             /* I/O  (Un)stable NLSF vector [ LPC_order ]    */
              float[]                 pNDelta_min,       /* I    Normalized delta min vector[LPC_order+1]*/
        final int                     LPC_order          /* I    LPC order                               */
    )
    {
        int   i;
        int[]   NLSF_Q15 = new int[ Silk_define.MAX_LPC_ORDER ], ndelta_min_Q15 = new int[ Silk_define.MAX_LPC_ORDER + 1 ];

        for( i = 0; i < LPC_order; i++ ) 
        {
            NLSF_Q15[       i ] = ( int )Silk_SigProc_FLP.SKP_float2int( pNLSF[       i ] * 32768.0f );
            ndelta_min_Q15[ i ] = ( int )Silk_SigProc_FLP.SKP_float2int( pNDelta_min[ i ] * 32768.0f );
        }
        ndelta_min_Q15[ LPC_order ] = ( int )Silk_SigProc_FLP.SKP_float2int( pNDelta_min[ LPC_order ] * 32768.0f );

        /* NLSF stabilizer, for a single input data vector */
        Silk_NLSF_stabilize.SKP_Silk_NLSF_stabilize( NLSF_Q15,0, ndelta_min_Q15, LPC_order );

        for( i = 0; i < LPC_order; i++ )
        {
            pNLSF[ i ] = ( float )NLSF_Q15[ i ] * ( 1.0f / 32768.0f );
        }
    }

    /* Interpolation function with fixed point rounding */
    static void SKP_Silk_interpolate_wrapper_FLP(
              float                 xi[],               /* O    Interpolated vector                     */
              float                 x0[],               /* I    First vector                            */
              float                 x1[],               /* I    Second vector                           */
        final float                 ifact,              /* I    Interp. factor, weight on second vector */
        final int                   d                   /* I    Number of parameters                    */
    )
    {
        int[] x0_int = new int[ Silk_define.MAX_LPC_ORDER ], x1_int = new int[ Silk_define.MAX_LPC_ORDER ], xi_int = new int[ Silk_define.MAX_LPC_ORDER ];
        int ifact_Q2 = ( int )( ifact * 4.0f );
        int i;

        /* Convert input from flp to fix */
        for( i = 0; i < d; i++ ) {
            x0_int[ i ] = Silk_SigProc_FLP.SKP_float2int( x0[ i ] * 32768.0f );
            x1_int[ i ] = Silk_SigProc_FLP.SKP_float2int( x1[ i ] * 32768.0f );
        }

        /* Interpolate two vectors */
        Silk_interpolate.SKP_Silk_interpolate( xi_int, x0_int, x1_int, ifact_Q2, d );
        
        /* Convert output from fix to flp */
        for( i = 0; i < d; i++ ) 
        {
            xi[ i ] = ( float )xi_int[ i ] * ( 1.0f / 32768.0f );
        }
    }

    /****************************************/
    /* Floating-point Silk VAD wrapper      */
    /****************************************/
    static int SKP_Silk_VAD_FLP(
        SKP_Silk_encoder_state_FLP      psEnc,             /* I/O  Encoder state FLP                       */
        SKP_Silk_encoder_control_FLP    psEncCtrl,         /* I/O  Encoder control FLP                     */
        short[]                         pIn,               /* I    Input signal                            */
        int pIn_offset
    )
    {
        int i, ret; 
        int[] SA_Q8 = new int[1], SNR_dB_Q7 = new int[1], Tilt_Q15 = new int[1];
        int[] Quality_Bands_Q15 = new int[ Silk_define.VAD_N_BANDS ];

        ret = Silk_VAD.SKP_Silk_VAD_GetSA_Q8( psEnc.sCmn.sVAD, SA_Q8, SNR_dB_Q7, Quality_Bands_Q15, Tilt_Q15,
            pIn,pIn_offset, psEnc.sCmn.frame_length );

        psEnc.speech_activity = ( float )SA_Q8[0] / 256.0f;
        for( i = 0; i < Silk_define.VAD_N_BANDS; i++ ) 
        {
            psEncCtrl.input_quality_bands[ i ] = ( float )Quality_Bands_Q15[ i ] / 32768.0f;
        }
        psEncCtrl.input_tilt = ( float )Tilt_Q15[0] / 32768.0f;

        return ret;
    }

    /****************************************/
    /* Floating-point Silk NSQ wrapper      */
    /****************************************/
    static void SKP_Silk_NSQ_wrapper_FLP(
        SKP_Silk_encoder_state_FLP      psEnc,         /* I/O  Encoder state FLP                           */
        SKP_Silk_encoder_control_FLP    psEncCtrl,     /* I/O  Encoder control FLP                         */
              float                 x[],            /* I    Prefiltered input signal                    */
              int x_offset,
              byte                  q[],            /* O    Quantized pulse signal                      */
              int q_offset,
        final int                   useLBRR         /* I    LBRR flag                                   */
    )
    {
        int     i, j;
        float   tmp_float;
        short[]   x_16 = new short[ Silk_define.MAX_FRAME_LENGTH ];
        /* Prediction and coding parameters */
        int[]   Gains_Q16 = new int[ Silk_define.NB_SUBFR ];
        short[][] PredCoef_Q12 = new short[ 2 ][ Silk_define.MAX_LPC_ORDER ];
        short[]   LTPCoef_Q14 = new short[ Silk_define.LTP_ORDER * Silk_define.NB_SUBFR ];
        int     LTP_scale_Q14;

        /* Noise shaping parameters */
        /* Testing */
        short[] AR2_Q13 = new short[ Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX ];
        int[]   LF_shp_Q14 = new int[ Silk_define.NB_SUBFR ];         /* Packs two int16 coefficients per int32 value             */
        int     Lambda_Q10;
        int[]     Tilt_Q14 = new int[ Silk_define.NB_SUBFR ];
        int[]     HarmShapeGain_Q14 = new int[ Silk_define.NB_SUBFR ];

        /* Convert control struct to fix control struct */
        /* Noise shape parameters */
        for( i = 0; i < Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX; i++ ) 
        {
            AR2_Q13[ i ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FLP.SKP_float2int( psEncCtrl.AR2[ i ] * 8192.0f ) );
        }

        /*TEST************************************************************************/
        /*
         * test of the AR2_Q13
         * 
         */
        short[] ar2_q13 = new short[ Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX ];
        String ar2_q13_filename = "D:/gsoc/ar2_q13";
       
//        /*
//         * Option 1:
//         */
//        DataInputStream ar2_q13_datain = null;
//        try
//        {
//            ar2_q13_datain = new DataInputStream(
//                                                 new FileInputStream(
//                                                     new File(ar2_q13_filename)));
//            
//            for( i = 0; i < Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX; i++ ) 
//            {
//     //           AR2_Q13[ i ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FLP.SKP_float2int( psEncCtrl.AR2[ i ] * 8192.0f ) );
//                  try
//                {
//                    ar2_q13[i] = ar2_q13_datain.readShort();
//                    AR2_Q13[i] = (short) (((ar2_q13[i] << 8) & 0xFF00) | ((ar2_q13[i] >>> 8) & 0x00FF));
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }             
//            }
//            try
//            {
//                ar2_q13_datain.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        /*
         * Option 2;
         */
//        RandomAccessFile ar2_q13_datain_rand = null;
//        try
//        {
//            ar2_q13_datain_rand = new RandomAccessFile(new File(ar2_q13_filename), "r");
//            try
//            {
//                ar2_q13_datain_rand.seek(ar2_q13_file_offset);
//                for( i = 0; i < Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX; i++ ) 
//                {
//         //           AR2_Q13[ i ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FLP.SKP_float2int( psEncCtrl.AR2[ i ] * 8192.0f ) );
//                      try
//                    {
//                        ar2_q13[i] = ar2_q13_datain_rand.readShort();
//                        AR2_Q13[i] = (short) (((ar2_q13[i] << 8) & 0xFF00) | ((ar2_q13[i] >>> 8) & 0x00FF));
//                    }
//                    catch (IOException e)
//                    {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }             
//                }
//                ar2_q13_file_offset += i;
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            
//            try
//            {
//                ar2_q13_datain_rand.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e1)
//        {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
        
        /**
         * Option 3:
         */
//      ar2_q13_filename += frame_cnt;
//      DataInputStream ar2_q13_datain = null;
//      try
//        {
//            ar2_q13_datain = new DataInputStream(
//                                                 new FileInputStream(
//                                                     new File(ar2_q13_filename)));
//            
//            for( i = 0; i < Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX; i++ ) 
//            {
//     //           AR2_Q13[ i ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FLP.SKP_float2int( psEncCtrl.AR2[ i ] * 8192.0f ) );
//                  try
//                {
//                    ar2_q13[i] = ar2_q13_datain.readShort();
//                    AR2_Q13[i] = (short) (((ar2_q13[i] << 8) & 0xFF00) | ((ar2_q13[i] >>> 8) & 0x00FF));
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }             
//            }
//            try
//            {
//                ar2_q13_datain.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        /*TEST End***********************************************************************/
        
        for( i = 0; i < Silk_define.NB_SUBFR; i++ ) 
        {
            LF_shp_Q14[ i ] =   ( Silk_SigProc_FLP.SKP_float2int( psEncCtrl.LF_AR_shp[ i ]     * 16384.0f ) << 16 ) |
                                  ( 0x0000FFFF & Silk_SigProc_FLP.SKP_float2int( psEncCtrl.LF_MA_shp[ i ]     * 16384.0f ) );
            Tilt_Q14[ i ]   =        (int)Silk_SigProc_FLP.SKP_float2int( psEncCtrl.Tilt[ i ]          * 16384.0f );
            HarmShapeGain_Q14[ i ] = (int)Silk_SigProc_FLP.SKP_float2int( psEncCtrl.HarmShapeGain[ i ] * 16384.0f );    
        }
        Lambda_Q10 = ( int )Silk_SigProc_FLP.SKP_float2int( psEncCtrl.Lambda * 1024.0f );

        /* prediction and coding parameters */
        for( i = 0; i < Silk_define.NB_SUBFR * Silk_define.LTP_ORDER; i++ ) 
        {
            LTPCoef_Q14[ i ] = ( short )Silk_SigProc_FLP.SKP_float2int( psEncCtrl.LTPCoef[ i ] * 16384.0f );
        }

        for( j = 0; j < Silk_define.NB_SUBFR >> 1; j++ ) 
        {
            for( i = 0; i < Silk_define.MAX_LPC_ORDER; i++ ) 
            {
                PredCoef_Q12[ j ][ i ] = ( short )Silk_SigProc_FLP.SKP_float2int( psEncCtrl.PredCoef[ j ][ i ] * 4096.0f );
            }
        }

        for( i = 0; i < Silk_define.NB_SUBFR; i++ ) 
        {
            tmp_float = Silk_SigProc_FIX.SKP_LIMIT( ( psEncCtrl.Gains[ i ] * 65536.0f ), 2147483000.0f, -2147483000.0f );
            Gains_Q16[ i ] = Silk_SigProc_FLP.SKP_float2int( tmp_float );
            if( psEncCtrl.Gains[ i ] > 0.0f ) 
            {
                assert( tmp_float >= 0.0f );
                assert( Gains_Q16[ i ] >= 0 );
            }
        }

        if( psEncCtrl.sCmn.sigtype == Silk_define.SIG_TYPE_VOICED ) {
            
            LTP_scale_Q14 = Silk_tables_other.SKP_Silk_LTPScales_table_Q14[ psEncCtrl.sCmn.LTP_scaleIndex ];
        } 
        else 
        {
            LTP_scale_Q14 = 0;
        }

        /* Convert input to fix */
        Silk_SigProc_FLP.SKP_float2short_array( x_16,0, x,x_offset, psEnc.sCmn.frame_length );
        
        /*TEST************************************************************************/
        /**
         * test of x_16
         */
        short x_16_test[] = new short[ Silk_define.MAX_FRAME_LENGTH ];
        String x_16_filename = "D:/gsoc/x_16";
//        /*
//         * Option 1:
//         */
//        DataInputStream x_16_datain = null;
//        try
//        {
//            x_16_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(x_16_filename)));
//            for(int k = 0; k < psEnc.sCmn.frame_length; k++)
//            {
//                try
//                {
//                    x_16_test[k] = x_16_datain.readShort();
//                    x_16[k] = (short) (((x_16_test[k]<<8)&0xFF00)|((x_16_test[k]>>>8)&0x00FF));
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            try
//            {
//                x_16_datain.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        /*
         * Option 2:
         */
//        RandomAccessFile x_16_datain_rand = null;
//        int k;
//        try
//        {
//            x_16_datain_rand = new RandomAccessFile(new File(x_16_filename), "r");
//            try
//            {
//                x_16_datain_rand.seek(x_16_file_offset);
//                for(k = 0; k < psEnc.sCmn.frame_length; k++)
//                {
//                    try
//                    {
//                        x_16_test[k] = x_16_datain_rand.readShort();
//                        x_16[k] = (short) (((x_16_test[k]<<8)&0xFF00)|((x_16_test[k]>>>8)&0x00FF));
//                    }
//                    catch (IOException e)
//                    {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                x_16_file_offset += k;
//            }
//            catch (IOException e1)
//            {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//            }
//            try
//            {
//                x_16_datain_rand.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        /**
         * Optino 3:
         */
//      x_16_filename += frame_cnt;
//      DataInputStream x_16_datain = null;
//      try
//        {
//            x_16_datain = new DataInputStream(
//                              new FileInputStream(
//                                  new File(x_16_filename)));
//            for(int k = 0; k < psEnc.sCmn.frame_length; k++)
//            {
//                try
//                {
//                    x_16_test[k] = x_16_datain.readShort();
//                    x_16[k] = (short) (((x_16_test[k]<<8)&0xFF00)|((x_16_test[k]>>>8)&0x00FF));
//                }
//                catch (IOException e)
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            try
//            {
//                x_16_datain.close();
//            }
//            catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        catch (FileNotFoundException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        frame_cnt++;
        /*TEST END************************************************************************/
        
        /* Call NSQ */
        short[] PredCoef_Q12_dim1_tmp= new short[PredCoef_Q12.length * PredCoef_Q12[0].length];
        int PredCoef_Q12_offset = 0;
        for(int PredCoef_Q12_i = 0; PredCoef_Q12_i < PredCoef_Q12.length; PredCoef_Q12_i++)
        {
            System.arraycopy(PredCoef_Q12[PredCoef_Q12_i],0, PredCoef_Q12_dim1_tmp, PredCoef_Q12_offset, PredCoef_Q12[PredCoef_Q12_i].length);
            PredCoef_Q12_offset += PredCoef_Q12[PredCoef_Q12_i].length;
        }
        if( useLBRR!=0 ) 
        {
//            psEnc.NoiseShapingQuantizer( psEnc.sCmn, psEncCtrl.sCmn, psEnc.sNSQ_LBRR, 
//                x_16, q, psEncCtrl.sCmn.NLSFInterpCoef_Q2, PredCoef_Q12[ 0 ], LTPCoef_Q14, AR2_Q13, 
//                HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );\
               psEnc.NoiseShapingQuantizer( psEnc.sCmn, psEncCtrl.sCmn, psEnc.sNSQ_LBRR, 
                    x_16, q, psEncCtrl.sCmn.NLSFInterpCoef_Q2, PredCoef_Q12_dim1_tmp, LTPCoef_Q14, AR2_Q13, 
                    HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );
//             psEnc.NoiseShapingQuantizer( &psEnc->sCmn, &psEncCtrl->sCmn, &psEnc->sNSQ_LBRR, 
//          x_16, q, psEncCtrl->sCmn.NLSFInterpCoef_Q2, PredCoef_Q12[ 0 ], LTPCoef_Q14, AR2_Q13, 
//          HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );
        } 
        else
        {
//            psEnc.NoiseShapingQuantizer( psEnc.sCmn, psEncCtrl.sCmn, psEnc.sNSQ, 
//                x_16, q, psEncCtrl.sCmn.NLSFInterpCoef_Q2, PredCoef_Q12[ 0 ], LTPCoef_Q14, AR2_Q13, 
//                HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );
               psEnc.NoiseShapingQuantizer( psEnc.sCmn, psEncCtrl.sCmn, psEnc.sNSQ, 
                    x_16, q, psEncCtrl.sCmn.NLSFInterpCoef_Q2, PredCoef_Q12_dim1_tmp, LTPCoef_Q14, AR2_Q13, 
                    HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );
//               psEnc.NoiseShapingQuantizer( &psEnc->sCmn, &psEncCtrl->sCmn, &psEnc->sNSQ, 
//                    x_16, q, psEncCtrl->sCmn.NLSFInterpCoef_Q2, PredCoef_Q12[ 0 ], LTPCoef_Q14, AR2_Q13, 
//                    HarmShapeGain_Q14, Tilt_Q14, LF_shp_Q14, Gains_Q16, Lambda_Q10, LTP_scale_Q14 );
        }
    }
}
