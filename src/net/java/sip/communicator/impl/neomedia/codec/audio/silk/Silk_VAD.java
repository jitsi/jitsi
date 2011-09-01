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
public class Silk_VAD 
{
    /**
     * Initialization of the Silk VAD.
     * @param psSilk_VAD Silk VAD state.
     * @return return 0 if success.
     */
    static int SKP_Silk_VAD_Init(                              /* O    Return value, 0 if success                  */ 
        SKP_Silk_VAD_state              psSilk_VAD         /* I/O  Pointer to Silk VAD state                   */ 
    )
    {
        int b, ret = 0;

        /* reset state memory */
//TODO: memset      
//      SKP_memset( psSilk_VAD, 0, sizeof( SKP_Silk_VAD_state ) );

        /* init noise levels */
        /* Initialize array with approx pink noise levels (psd proportional to inverse of frequency) */
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {
            psSilk_VAD.NoiseLevelBias[ b ] = Math.max( Silk_define.VAD_NOISE_LEVELS_BIAS / ( b + 1 ), 1 );
        }

        /* Initialize state */
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {
            psSilk_VAD.NL[ b ]     = 100 * psSilk_VAD.NoiseLevelBias[ b ];
            psSilk_VAD.inv_NL[ b ] = Silk_typedef.SKP_int32_MAX / psSilk_VAD.NL[ b ];
        }
        psSilk_VAD.counter = 15;

        /* init smoothed energy-to-noise ratio*/
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) {
            psSilk_VAD.NrgRatioSmth_Q8[ b ] = 100 * 256;       /* 100 * 256 --> 20 dB SNR */
        }

        return( ret );
    }

    /* Weighting factors for tilt measure */
    static int[] tiltWeights = { 30000, 6000, -12000, -12000 };
    
    /**
     * Get the speech activity level in Q8.
     * @param psSilk_VAD Silk VAD state.
     * @param pSA_Q8 Speech activity level in Q8.
     * @param pSNR_dB_Q7 SNR for current frame in Q7.
     * @param pQuality_Q15 Smoothed SNR for each band.
     * @param pTilt_Q15 Smoothed SNR for each band.
     * @param pIn PCM input[framelength].
     * @param pIn_offset offset of valid data.
     * @param framelength Input frame length.
     * @return Return value, 0 if success.
     */
    static int SKP_Silk_VAD_GetSA_Q8(                                /* O    Return value, 0 if success      */
        SKP_Silk_VAD_state            psSilk_VAD,                    /* I/O  Silk VAD state                  */
        int[]                         pSA_Q8,                        /* O    Speech activity level in Q8     */
        int[]                         pSNR_dB_Q7,                    /* O    SNR for current frame in Q7     */
        int[]                         pQuality_Q15,                  /* O    Smoothed SNR for each band      */
        int[]                         pTilt_Q15,                     /* O    current frame's frequency tilt  */
        short[]                       pIn,                           /* I    PCM input       [framelength]   */
        int                           pIn_offset,
        int                           framelength                    /* I    Input frame length              */
    )
    {
        int   SA_Q15, input_tilt;
        int[] scratch = new int[ 3 * Silk_define.MAX_FRAME_LENGTH / 2 ];
        int   decimated_framelength, dec_subframe_length, dec_subframe_offset, SNR_Q7, i, b, s;
        int sumSquared=0, smooth_coef_Q16;
        short HPstateTmp;

        short[][] X = new short[ Silk_define.VAD_N_BANDS ][ Silk_define.MAX_FRAME_LENGTH / 2 ];
        int[] Xnrg = new int[ Silk_define.VAD_N_BANDS ];
        int[] NrgToNoiseRatio_Q8 = new int[ Silk_define.VAD_N_BANDS ];
        int speech_nrg, x_tmp;
        int   ret = 0;

        /* Safety checks */
        assert( Silk_define.VAD_N_BANDS == 4 );
        assert( Silk_define.MAX_FRAME_LENGTH >= framelength );
        assert( framelength <= 512 );

        /***********************/
        /* Filter and Decimate */
        /***********************/
        /* 0-8 kHz to 0-4 kHz and 4-8 kHz */
        Silk_ana_filt_bank_1.SKP_Silk_ana_filt_bank_1( pIn,pIn_offset,          psSilk_VAD.AnaState,0, X[ 0 ],0, X[ 3 ],0, scratch, framelength );        
        
        /* 0-4 kHz to 0-2 kHz and 2-4 kHz */
        Silk_ana_filt_bank_1.SKP_Silk_ana_filt_bank_1( X[ 0 ],0, psSilk_VAD.AnaState1,0, X[ 0 ],0, X[ 2 ],0, scratch, framelength>>1 );
        
        /* 0-2 kHz to 0-1 kHz and 1-2 kHz */
        Silk_ana_filt_bank_1.SKP_Silk_ana_filt_bank_1( X[ 0 ],0, psSilk_VAD.AnaState2,0, X[ 0 ],0, X[ 1 ],0, scratch, framelength>>2 );

        /*********************************************/
        /* HP filter on lowest band (differentiator) */
        /*********************************************/
        decimated_framelength = framelength >> 3;
        X[ 0 ][ decimated_framelength - 1 ] = (short)( X[ 0 ][ decimated_framelength - 1 ] >> 1 );
        HPstateTmp = X[ 0 ][ decimated_framelength - 1 ];
        for( i = decimated_framelength - 1; i > 0; i-- ) 
        {
            X[ 0 ][ i - 1 ]  = (short)( X[ 0 ][ i - 1 ] >> 1 );
            X[ 0 ][ i ]     -= X[ 0 ][ i - 1 ];
        }
        X[ 0 ][ 0 ] -= psSilk_VAD.HPstate;
        psSilk_VAD.HPstate = HPstateTmp;

        /*************************************/
        /* Calculate the energy in each band */
        /*************************************/
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {        
            /* Find the decimated framelength in the non-uniformly divided bands */
            decimated_framelength = framelength >> Math.min( Silk_define.VAD_N_BANDS - b, Silk_define.VAD_N_BANDS - 1);

            /* Split length into subframe lengths */
            dec_subframe_length = decimated_framelength >> Silk_define.VAD_INTERNAL_SUBFRAMES_LOG2;
            dec_subframe_offset = 0;

            /* Compute energy per sub-frame */
            /* initialize with summed energy of last subframe */
            Xnrg[ b ] = psSilk_VAD.XnrgSubfr[ b ];
            for( s = 0; s < Silk_define.VAD_INTERNAL_SUBFRAMES; s++ ) 
            {
                sumSquared = 0;
                for( i = 0; i < dec_subframe_length; i++ ) 
                {
                    /* The energy will be less than dec_subframe_length * ( SKP_int16_MIN / 8 )^2.              */
                    /* Therefore we can accumulate with no risk of overflow (unless dec_subframe_length > 128)  */
                    x_tmp = X[ b ][ i + dec_subframe_offset ] >> 3;
                    sumSquared = Silk_macros.SKP_SMLABB( sumSquared, x_tmp, x_tmp );

                    /* Safety check */
                    assert( sumSquared >= 0 );
                }

                /* add/saturate summed energy of current subframe */
                if( s < Silk_define.VAD_INTERNAL_SUBFRAMES - 1 ) 
                {
                    Xnrg[ b ] = Silk_SigProc_FIX.SKP_ADD_POS_SAT32( Xnrg[ b ], sumSquared );
                }
                else 
                {
                    /* look-ahead subframe */
                    Xnrg[ b ] = Silk_SigProc_FIX.SKP_ADD_POS_SAT32( Xnrg[ b ], sumSquared>>1 );
                }

                dec_subframe_offset += dec_subframe_length;
            }
            psSilk_VAD.XnrgSubfr[ b ] = sumSquared; 
        }

        /********************/
        /* Noise estimation */
        /********************/
        SKP_Silk_VAD_GetNoiseLevels( Xnrg, psSilk_VAD );

        /***********************************************/
        /* Signal-plus-noise to noise ratio estimation */
        /***********************************************/
        sumSquared = 0;
        input_tilt = 0;
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {
            speech_nrg = Xnrg[ b ] - psSilk_VAD.NL[ b ];
            if( speech_nrg > 0 ) 
            {
                /* Divide, with sufficient resolution */
                if( ( Xnrg[ b ] & 0xFF800000 ) == 0 ) 
                {
                    NrgToNoiseRatio_Q8[ b ] = ( Xnrg[ b ] << 8 ) / ( psSilk_VAD.NL[ b ] + 1 );
                }
                else
                {
                    NrgToNoiseRatio_Q8[ b ] = Xnrg[ b ] / ( ( psSilk_VAD.NL[ b ] >> 8 ) + 1 );
                }

                /* Convert to log domain */
                SNR_Q7 = Silk_lin2log.SKP_Silk_lin2log( NrgToNoiseRatio_Q8[ b ] ) - 8 * 128;

                /* Sum-of-squares */
                sumSquared = Silk_macros.SKP_SMLABB( sumSquared, SNR_Q7, SNR_Q7 );          /* Q14 */

                /* Tilt measure */
                if( speech_nrg < ( 1 << 20 ) ) {
                    /* Scale down SNR value for small subband speech energies */
                    SNR_Q7 = Silk_macros.SKP_SMULWB( Silk_Inlines.SKP_Silk_SQRT_APPROX( speech_nrg ) << 6, SNR_Q7 );
                }
                input_tilt = Silk_macros.SKP_SMLAWB( input_tilt, tiltWeights[ b ], SNR_Q7 );
            }
            else
            {
                NrgToNoiseRatio_Q8[ b ] = 256;
            }
        }

        /* Mean-of-squares */
        sumSquared = sumSquared / Silk_define.VAD_N_BANDS;           /* Q14 */

        /* Root-mean-square approximation, scale to dBs, and write to output pointer */
        pSNR_dB_Q7[0] = ( short )( 3 * Silk_Inlines.SKP_Silk_SQRT_APPROX( sumSquared ) );  /* Q7 */

        /*********************************/
        /* Speech Probability Estimation */
        /*********************************/
        SA_Q15 = Silk_sigm_Q15.SKP_Silk_sigm_Q15( Silk_macros.SKP_SMULWB( Silk_define.VAD_SNR_FACTOR_Q16, pSNR_dB_Q7[0] ) - Silk_define.VAD_NEGATIVE_OFFSET_Q5 );

        /**************************/
        /* Frequency Tilt Measure */
        /**************************/
        pTilt_Q15[0] = ( Silk_sigm_Q15.SKP_Silk_sigm_Q15( input_tilt ) - 16384 ) << 1;

        /**************************************************/
        /* Scale the sigmoid output based on power levels */
        /**************************************************/
        speech_nrg = 0;
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {
            /* Accumulate signal-without-noise energies, higher frequency bands have more weight */
            speech_nrg += ( b + 1 ) * ( ( Xnrg[ b ] - psSilk_VAD.NL[ b ] ) >> 4 );
        }

        /* Power scaling */
        if( speech_nrg <= 0 ) 
        {
            SA_Q15 = SA_Q15 >> 1; 
        } 
        else if( speech_nrg < 32768 ) 
        {
            /* square-root */
            speech_nrg = Silk_Inlines.SKP_Silk_SQRT_APPROX( speech_nrg << 15 );
            SA_Q15 = Silk_macros.SKP_SMULWB( 32768 + speech_nrg, SA_Q15 ); 
        }

        /* Copy the resulting speech activity in Q8 to *pSA_Q8 */
        pSA_Q8[0] = Math.min( SA_Q15 >> 7, Silk_typedef.SKP_uint8_MAX );

        /***********************************/
        /* Energy Level and SNR estimation */
        /***********************************/
        /* smoothing coefficient */
        smooth_coef_Q16 = Silk_macros.SKP_SMULWB( Silk_define.VAD_SNR_SMOOTH_COEF_Q18, Silk_macros.SKP_SMULWB( SA_Q15, SA_Q15 ) );
        for( b = 0; b < Silk_define.VAD_N_BANDS; b++ ) 
        {
            /* compute smoothed energy-to-noise ratio per band */
            psSilk_VAD.NrgRatioSmth_Q8[ b ] = Silk_macros.SKP_SMLAWB( psSilk_VAD.NrgRatioSmth_Q8[ b ], 
                NrgToNoiseRatio_Q8[ b ] - psSilk_VAD.NrgRatioSmth_Q8[ b ], smooth_coef_Q16 );

            /* signal to noise ratio in dB per band */
            SNR_Q7 = 3 * ( Silk_lin2log.SKP_Silk_lin2log( psSilk_VAD.NrgRatioSmth_Q8[b] ) - 8 * 128 );
            /* quality = sigmoid( 0.25 * ( SNR_dB - 16 ) ); */
            pQuality_Q15[ b ] = Silk_sigm_Q15.SKP_Silk_sigm_Q15( ( SNR_Q7 - 16 * 128 ) >> 4 );
        }

        return( ret );
    }
    
    /**
     * Noise level estimation.
     * @param pX subband energies.
     * @param psSilk_VAD Silk VAD state.
     */
    static void SKP_Silk_VAD_GetNoiseLevels
    (
        int[]                 pX,                /* I    subband energies                            */
        SKP_Silk_VAD_state    psSilk_VAD         /* I/O  Pointer to Silk VAD state                   */ 
    )
    {
        int   k;
        int nl, nrg, inv_nrg;
        int   coef, min_coef;

        /* Initially faster smoothing */
        if( psSilk_VAD.counter < 1000 ) 
        { /* 1000 = 20 sec */
            min_coef = Silk_typedef.SKP_int16_MAX / ( ( psSilk_VAD.counter >> 4 ) + 1 );  
        }
        else 
        {
            min_coef = 0;
        }

        for( k = 0; k < Silk_define.VAD_N_BANDS; k++ ) 
        {
            /* Get old noise level estimate for current band */
            nl = psSilk_VAD.NL[ k ];
            assert( nl >= 0 );
            
            /* Add bias */
            nrg = Silk_SigProc_FIX.SKP_ADD_POS_SAT32( pX[ k ], psSilk_VAD.NoiseLevelBias[ k ] ); 
            assert( nrg > 0 );
            
            /* Invert energies */
            inv_nrg = Silk_typedef.SKP_int32_MAX / nrg;
            assert( inv_nrg >= 0 );
            
            /* Less update when subband energy is high */
            if( nrg > nl<<3 ) 
            {
                coef = Silk_define.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16 >> 3;
            }
            else if( nrg < nl )
            {
                coef = Silk_define.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16;
            }
            else
            {
                coef = Silk_macros.SKP_SMULWB( Silk_macros.SKP_SMULWW( inv_nrg, nl ), Silk_define.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16 << 1 );
            }

            /* Initially faster smoothing */
            coef = Math.max( coef, min_coef );

            /* Smooth inverse energies */
            psSilk_VAD.inv_NL[ k ] = Silk_macros.SKP_SMLAWB( psSilk_VAD.inv_NL[ k ], inv_nrg - psSilk_VAD.inv_NL[ k ], coef );
            assert( psSilk_VAD.inv_NL[ k ] >= 0 );

            /* Compute noise level by inverting again */
            nl = Silk_typedef.SKP_int32_MAX / psSilk_VAD.inv_NL[ k ];
            assert( nl >= 0 );

            /* Limit noise levels (guarantee 7 bits of head room) */
            nl = Math.min( nl, 0x00FFFFFF );

            /* Store as part of state */
            psSilk_VAD.NL[ k ] = nl;
        }

        /* Increment frame counter */
        psSilk_VAD.counter++;
    }
}
