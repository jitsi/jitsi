/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Pitch analysis.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
//TODO: float or dobule ???
public class Silk_pitch_analysis_core_FLP
{
    static final int SCRATCH_SIZE =   22;

    static final float eps =  1.192092896e-07f;

    /* using log2() helps the fixed-point conversion */
    static float SKP_P_log2(double x) 
    { 
        return (float)(3.32192809488736 * Math.log10(x)); 
    }

    /**
     * CORE PITCH ANALYSIS FUNCTION.
     * @param signal signal of length PITCH_EST_FRAME_LENGTH_MS*Fs_kHz
     * @param pitch_out 4 pitch lag values
     * @param lagIndex lag Index
     * @param contourIndex pitch contour Index
     * @param LTPCorr normalized correlation; input: value from previous frame
     * @param prevLag last lag of previous frame; set to zero is unvoiced
     * @param search_thres1 first stage threshold for lag candidates 0 - 1
     * @param search_thres2 final threshold for lag candidates 0 - 1
     * @param Fs_kHz sample frequency (kHz)
     * @param complexity Complexity setting, 0-2, where 2 is highest
     * @return voicing estimate: 0 voiced, 1 unvoiced
     */
    static int SKP_Silk_pitch_analysis_core_FLP( /* O voicing estimate: 0 voiced, 1 unvoiced                 */
        float[] signal,            /* I signal of length PITCH_EST_FRAME_LENGTH_MS*Fs_kHz              */
        int[]         pitch_out,         /* O 4 pitch lag values                                             */
        int[]         lagIndex,          /* O lag Index                                                      */
        int[]         contourIndex,      /* O pitch contour Index                                            */
        float[]       LTPCorr,           /* I/O normalized correlation; input: value from previous frame     */
        int         prevLag,            /* I last lag of previous frame; set to zero is unvoiced            */
        final float search_thres1,      /* I first stage threshold for lag candidates 0 - 1                 */
        final float search_thres2,      /* I final threshold for lag candidates 0 - 1                       */
        final int   Fs_kHz,             /* I sample frequency (kHz)                                         */
        final int   complexity          /* I Complexity setting, 0-2, where 2 is highest                    */
    )
    {
        float[] signal_8kHz = new float[ Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 8 ];
        float[] signal_4kHz = new float[ Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 4 ];
        float[] scratch_mem = new float[ Silk_common_pitch_est_defines.PITCH_EST_MAX_FRAME_LENGTH * 3 ];
        float[] filt_state = new float[ Silk_common_pitch_est_defines.PITCH_EST_MAX_DECIMATE_STATE_LENGTH ];
        int   i, k, d, j;
        float threshold, contour_bias;
        float[][] C = new float[Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR][(Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG >> 1) + 5]; /* use to be +2 but then valgrind reported errors for SWB */
        float[] CC = new float[Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE2_EXT];
        float[] target_ptr, basis_ptr;
        int target_ptr_offset, basis_ptr_offset;
        double    cross_corr, normalizer, energy, energy_tmp;
        int[]   d_srch = new int[Silk_common_pitch_est_defines.PITCH_EST_D_SRCH_LENGTH];
        short[] d_comp = new short[(Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG >> 1) + 5];
        int   length_d_srch, length_d_comp;
        float Cmax, CCmax, CCmax_b, CCmax_new_b, CCmax_new;
        int   CBimax, CBimax_new, lag, start_lag, end_lag, lag_new;
        int   cbk_offset, cbk_size;
        float lag_log2, prevLag_log2, delta_lag_log2_sqr;
        float[][][] energies_st3 = new float[ Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR ][ Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX ][ Silk_common_pitch_est_defines.PITCH_EST_NB_STAGE3_LAGS ];
        float[][][] cross_corr_st3 = new float[ Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR ][ Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX ][ Silk_common_pitch_est_defines.PITCH_EST_NB_STAGE3_LAGS ];

        int diff, lag_counter;
        int frame_length, frame_length_8kHz, frame_length_4kHz;
        int sf_length, sf_length_8kHz, sf_length_4kHz;
        int min_lag, min_lag_8kHz, min_lag_4kHz;
        int max_lag, max_lag_8kHz, max_lag_4kHz;

        int nb_cbks_stage2;

        /* Check for valid sampling frequency */
        assert( Fs_kHz == 8 || Fs_kHz == 12 || Fs_kHz == 16 || Fs_kHz == 24 );

        /* Check for valid complexity setting */
        assert( complexity >= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MIN_COMPLEX );
        assert( complexity <= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX );

        assert( search_thres1 >= 0.0f && search_thres1 <= 1.0f );
        assert( search_thres2 >= 0.0f && search_thres2 <= 1.0f );

        /* Setup frame lengths max / min lag for the sampling frequency */
        frame_length      = Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * Fs_kHz;
        frame_length_4kHz = Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 4;
        frame_length_8kHz = Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 8;
        sf_length         = frame_length >>      3;
        sf_length_4kHz    = frame_length_4kHz >> 3; 
        sf_length_8kHz    = frame_length_8kHz >> 3;
        min_lag           = Silk_common_pitch_est_defines.PITCH_EST_MIN_LAG_MS * Fs_kHz;
        min_lag_4kHz      = Silk_common_pitch_est_defines.PITCH_EST_MIN_LAG_MS * 4;
        min_lag_8kHz      = Silk_common_pitch_est_defines.PITCH_EST_MIN_LAG_MS * 8;
        max_lag           = Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG_MS * Fs_kHz;
        max_lag_4kHz      = Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG_MS * 4;
        max_lag_8kHz      = Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG_MS * 8;

        for(int i_djinn=0; i_djinn< Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i_djinn++)
        for(int j_djinn=0; j_djinn< (Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG >> 1) + 5; j_djinn++)
            C[i_djinn][j_djinn] = 0;
        
        /* Resample from input sampled at Fs_kHz to 8 kHz */
        if( Fs_kHz == 12 ) 
        {
            short[] signal_12 = new short[ 12 * Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS ];
            short[] signal_8 = new short[   8 * Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS ];
            int[] R23 = new int[ 6 ];

            /* Resample to 12 -> 8 khz */
            for(int i_djinn=0; i_djinn<6; i_djinn++)
                R23[i_djinn] = 0;
            Silk_SigProc_FLP.SKP_float2short_array( signal_12,0, signal,0, Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 12);
            Silk_resampler_down2_3.SKP_Silk_resampler_down2_3( R23,0, signal_8,0, signal_12,0, Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS * 12 );
            Silk_SigProc_FLP.SKP_short2float_array( signal_8kHz,0, signal_8,0, frame_length_8kHz );
        } 
        else if( Fs_kHz == 16 ) 
        {
            if( complexity == Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX ) 
            {
                assert( 4 <= Silk_common_pitch_est_defines.PITCH_EST_MAX_DECIMATE_STATE_LENGTH );
                for(int i_djinn=0; i_djinn<4; i_djinn++)
                    filt_state[i_djinn] = 0;

                Silk_decimate2_coarse_FLP.SKP_Silk_decimate2_coarse_FLP( signal,0, filt_state,0, signal_8kHz,0, 
                    scratch_mem,0, frame_length_8kHz );
            } 
            else 
            {
                assert( 2 <= Silk_common_pitch_est_defines.PITCH_EST_MAX_DECIMATE_STATE_LENGTH );
                for(int i_djinn=0; i_djinn<2; i_djinn++)
                    filt_state[i_djinn] = 0;
                
                Silk_decimate2_coarsest_FLP.SKP_Silk_decimate2_coarsest_FLP( signal,0, filt_state,0, signal_8kHz,0, 
                    scratch_mem,0, frame_length_8kHz );
            }
        } 
        else if( Fs_kHz == 24 ) 
        {
            short[] signal_24 = new short[ Silk_common_pitch_est_defines.PITCH_EST_MAX_FRAME_LENGTH ];
            short[] signal_8 = new short[ 8 * Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS ];
            int[] filt_state_fix = new int[ 8 ];

            /* Resample to 24 -> 8 khz */
            Silk_SigProc_FLP.SKP_float2short_array( signal_24,0, signal,0, 24 * Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS );
            for(int i_djinn=0; i_djinn<8; i_djinn++)
                filt_state_fix[i_djinn] = 0;
            Silk_resampler_down3.SKP_Silk_resampler_down3( filt_state_fix,0, signal_8,0, signal_24,0, 24 * Silk_common_pitch_est_defines.PITCH_EST_FRAME_LENGTH_MS );
            Silk_SigProc_FLP.SKP_short2float_array( signal_8kHz,0, signal_8,0, frame_length_8kHz );
        } 
        else
        {
            assert( Fs_kHz == 8 );
            for(int i_djinn=0; i_djinn<frame_length_8kHz; i_djinn++)
                signal_8kHz[i_djinn] = signal[i_djinn];
        }

        /* Decimate again to 4 kHz. Set mem to zero */
        if( complexity == Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX ) 
        {
            assert( 4 <= Silk_common_pitch_est_defines.PITCH_EST_MAX_DECIMATE_STATE_LENGTH );
            for(int i_djinn=0; i_djinn<4; i_djinn++)
                filt_state[i_djinn] = 0;
            Silk_decimate2_coarse_FLP.SKP_Silk_decimate2_coarse_FLP( signal_8kHz,0, filt_state,0, 
                signal_4kHz,0, scratch_mem,0, frame_length_4kHz );
        }
        else
        {
            assert( 2 <= Silk_common_pitch_est_defines.PITCH_EST_MAX_DECIMATE_STATE_LENGTH );
            for(int i_djinn=0; i_djinn<4; i_djinn++)
                filt_state[i_djinn] = 0;
            Silk_decimate2_coarsest_FLP.SKP_Silk_decimate2_coarsest_FLP( signal_8kHz,0, filt_state,0, 
                signal_4kHz,0, scratch_mem,0, frame_length_4kHz );
        }

        /* Low-pass filter */
        for( i = frame_length_4kHz - 1; i > 0; i-- ) {
            signal_4kHz[ i ] += signal_4kHz[ i - 1 ];
        }

        /******************************************************************************
        * FIRST STAGE, operating in 4 khz
        ******************************************************************************/
        target_ptr = signal_4kHz;
        target_ptr_offset = frame_length_4kHz >> 1;
        for( k = 0; k < 2; k++ ) 
        {
            /* Check that we are within range of the array */
            assert( target_ptr_offset >= 0 );
            assert( target_ptr_offset + sf_length_8kHz <= frame_length_4kHz );

            basis_ptr = target_ptr;
            basis_ptr_offset = target_ptr_offset - min_lag_4kHz;

            /* Check that we are within range of the array */
            assert( basis_ptr_offset >= 0 );
            assert( basis_ptr_offset + sf_length_8kHz <= frame_length_4kHz );

            /* Calculate first vector products before loop */
            cross_corr = Silk_inner_product_FLP.SKP_Silk_inner_product_FLP( target_ptr,target_ptr_offset, basis_ptr,basis_ptr_offset, sf_length_8kHz );
            normalizer = Silk_energy_FLP.SKP_Silk_energy_FLP( basis_ptr,basis_ptr_offset, sf_length_8kHz ) + 1000.0f;

            C[ 0 ][ min_lag_4kHz ] += (float)(cross_corr / Math.sqrt(normalizer));

            /* From now on normalizer is computed recursively */
            for(d = min_lag_4kHz + 1; d <= max_lag_4kHz; d++) 
            {
                basis_ptr_offset--;

                /* Check that we are within range of the array */
                assert( basis_ptr_offset >= 0 );
                assert( basis_ptr_offset + sf_length_8kHz <= frame_length_4kHz );

                cross_corr = Silk_inner_product_FLP.SKP_Silk_inner_product_FLP(target_ptr,target_ptr_offset, basis_ptr,basis_ptr_offset, sf_length_8kHz);

                /* Add contribution of new sample and remove contribution from oldest sample */
//                   normalizer +=
//                        basis_ptr[ 0 ] * basis_ptr[ 0 ] - 
//                        basis_ptr[ sf_length_8kHz ] * basis_ptr[ sf_length_8kHz ];
                normalizer +=
                    basis_ptr[ basis_ptr_offset + 0 ] * basis_ptr[ basis_ptr_offset + 0 ] - 
                    basis_ptr[ basis_ptr_offset + sf_length_8kHz ] * basis_ptr[ basis_ptr_offset + sf_length_8kHz ];
                C[ 0 ][ d ] += (float)(cross_corr / Math.sqrt( normalizer ));
            }
            /* Update target pointer */
            target_ptr_offset += sf_length_8kHz;
        }

        /* Apply short-lag bias */
        for( i = max_lag_4kHz; i >= min_lag_4kHz; i-- )
        {
            C[ 0 ][ i ] -= C[ 0 ][ i ] * i / 4096.0f;
        }

        /* Sort */
        length_d_srch = 5 + complexity;
        assert( length_d_srch <= Silk_common_pitch_est_defines.PITCH_EST_D_SRCH_LENGTH );
        Silk_sort_FLP.SKP_Silk_insertion_sort_decreasing_FLP( C[ 0 ],min_lag_4kHz, d_srch, max_lag_4kHz - min_lag_4kHz + 1, length_d_srch );

        /* Escape if correlation is very low already here */
        Cmax = C[ 0 ][ min_lag_4kHz ];
        target_ptr = signal_4kHz;
        target_ptr_offset = frame_length_4kHz >> 1;
        energy = 1000.0f;
        for( i = 0; i < frame_length_4kHz >> 1; i++ ) 
        {
            energy += target_ptr[target_ptr_offset+i] * target_ptr[target_ptr_offset+i];
        }
        threshold = Cmax * Cmax; 
        if( energy / 16.0f > threshold ) 
        {
            for(int i_djinn=0; i_djinn<Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i_djinn++)
                pitch_out[i_djinn] = 0;
            LTPCorr[0]      = 0.0f;
            lagIndex[0]     = 0;
            contourIndex[0] = 0;
            return 1;
        }

        threshold = search_thres1 * Cmax;
        for( i = 0; i < length_d_srch; i++ ) {
            /* Convert to 8 kHz indices for the sorted correlation that exceeds the threshold */
            if( C[ 0 ][ min_lag_4kHz + i ] > threshold ) 
            {
                d_srch[ i ] = ( d_srch[ i ] + min_lag_4kHz ) << 1;
            }
            else 
            {
                length_d_srch = i;
                break;
            }
        }
        assert( length_d_srch > 0 );

        for( i = min_lag_8kHz - 5; i < max_lag_8kHz + 5; i++ ) {
            d_comp[ i ] = 0;
        }
        for( i = 0; i < length_d_srch; i++ ) {
            d_comp[ d_srch[ i ] ] = 1;
        }

        /* Convolution */
        for( i = max_lag_8kHz + 3; i >= min_lag_8kHz; i-- ) {
            d_comp[ i ] += d_comp[ i - 1 ] + d_comp[ i - 2 ];
        }

        length_d_srch = 0;
        for( i = min_lag_8kHz; i < max_lag_8kHz + 1; i++ ) {    
            if( d_comp[ i + 1 ] > 0 ) {
                d_srch[ length_d_srch ] = i;
                length_d_srch++;
            }
        }

        /* Convolution */
        for( i = max_lag_8kHz + 3; i >= min_lag_8kHz; i-- ) {
            d_comp[ i ] += d_comp[ i - 1 ] + d_comp[ i - 2 ] + d_comp[ i - 3 ];
        }

        length_d_comp = 0;
        for( i = min_lag_8kHz; i < max_lag_8kHz + 4; i++ ) 
        {    
            if( d_comp[ i ] > 0 ) {
                d_comp[ length_d_comp ] = (short)(i - 2);
                length_d_comp++;
            }
        }

        /**********************************************************************************
        ** SECOND STAGE, operating at 8 kHz, on lag sections with high correlation
        *************************************************************************************/
        /********************************************************************************* 
        * Find energy of each subframe projected onto its history, for a range of delays
        *********************************************************************************/
        for(int i_djinn=0; i_djinn< Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i_djinn++)
            for(int j_djinn=0; j_djinn< ((Silk_common_pitch_est_defines.PITCH_EST_MAX_LAG >> 1) + 5); j_djinn++)
                C[i_djinn][j_djinn] = 0;
        
        target_ptr = signal_8kHz; /* point to middle of frame */
        target_ptr_offset = frame_length_4kHz;
        for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ )
        {      
            /* Check that we are within range of the array */
            assert( target_ptr_offset >= 0 );
            assert( target_ptr_offset + sf_length_8kHz <= frame_length_8kHz );

            energy_tmp = Silk_energy_FLP.SKP_Silk_energy_FLP( target_ptr,target_ptr_offset, sf_length_8kHz );
            for( j = 0; j < length_d_comp; j++ ) 
            {
                d = d_comp[ j ];
                basis_ptr = target_ptr;
                basis_ptr_offset = target_ptr_offset - d;

                /* Check that we are within range of the array */
                assert( basis_ptr_offset >= 0 );
                assert( basis_ptr_offset + sf_length_8kHz <= frame_length_8kHz );
            
                cross_corr = Silk_inner_product_FLP.SKP_Silk_inner_product_FLP( basis_ptr,basis_ptr_offset, target_ptr,target_ptr_offset, sf_length_8kHz );
                energy     = Silk_energy_FLP.SKP_Silk_energy_FLP( basis_ptr,basis_ptr_offset, sf_length_8kHz );
                if (cross_corr > 0.0f) 
                {
                    C[ k ][ d ] = (float)(cross_corr * cross_corr / (energy * energy_tmp + eps));
                }
                else 
                {
                    C[ k ][ d ] = 0.0f;
                }
            }
            target_ptr_offset += sf_length_8kHz;
        }

        /* search over lag range and lags codebook */
        /* scale factor for lag codebook, as a function of center lag */

        CCmax   = 0.0f; /* This value doesn't matter */
        CCmax_b = -1000.0f;

        CBimax = 0; /* To avoid returning undefined lag values */
        lag = -1;   /* To check if lag with strong enough correlation has been found */

        if( prevLag > 0 ) {
            if( Fs_kHz == 12 ) 
            {
                prevLag = ( prevLag<<1 ) / 3;
            }
            else if( Fs_kHz == 16 ) 
            {
                prevLag = prevLag>>1;
            }
            else if( Fs_kHz == 24 ) 
            {
                prevLag = prevLag / 3;
            }
            prevLag_log2 = SKP_P_log2(prevLag);
        }
        else 
        {
            prevLag_log2 = 0;
        }

        /* If input is 8 khz use a larger codebook here because it is last stage */
        if( Fs_kHz == 8 && complexity > Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MIN_COMPLEX ) 
        {
            nb_cbks_stage2 = Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE2_EXT;  
        }
        else 
        {
            nb_cbks_stage2 = Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE2;
        }

        for( k = 0; k < length_d_srch; k++ ) 
        {
            d = d_srch[ k ];
            for( j = 0; j < nb_cbks_stage2; j++ ) 
            {
                CC[j] = 0.0f;
                for( i = 0; i < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i++ ) {
                    /* Try all codebooks */
                    CC[ j ] += C[ i ][ d + Silk_pitch_est_tables.SKP_Silk_CB_lags_stage2[ i ][ j ] ];
                }
            }
            /* Find best codebook */
            CCmax_new  = -1000.0f;
            CBimax_new = 0;
            for( i = 0; i < nb_cbks_stage2; i++ )
            {
                if( CC[ i ] > CCmax_new ) 
                {
                    CCmax_new = CC[ i ];
                    CBimax_new = i;
                }
            }
            CCmax_new = Math.max(CCmax_new, 0.0f); /* To avoid taking square root of negative number later */
            CCmax_new_b = CCmax_new;

            /* Bias towards shorter lags */
            lag_log2 = SKP_P_log2(d);
            CCmax_new_b -= Silk_pitch_est_defines_FLP.PITCH_EST_FLP_SHORTLAG_BIAS * Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR * lag_log2;

            /* Bias towards previous lag */
            if ( prevLag > 0 )
            {
                delta_lag_log2_sqr = lag_log2 - prevLag_log2;
                delta_lag_log2_sqr *= delta_lag_log2_sqr;
                CCmax_new_b -= Silk_pitch_est_defines_FLP.PITCH_EST_FLP_PREVLAG_BIAS * Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR * LTPCorr[0] * delta_lag_log2_sqr / (delta_lag_log2_sqr + 0.5f);
            }

            if ( CCmax_new_b > CCmax_b && CCmax_new > Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR * search_thres2 * search_thres2 )
            {
                CCmax_b = CCmax_new_b;
                CCmax   = CCmax_new;
                lag     = d;
                CBimax  = CBimax_new;
            }
        }

        if( lag == -1 ) 
        {
            /* No suitable candidate found */
            for(int i_djinn=0; i_djinn<Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i_djinn++)
                pitch_out[i_djinn] = 0;
            LTPCorr[0]      = 0.0f;
            lagIndex[0]     = 0;
            contourIndex[0] = 0;
            return 1;
        }

        if( Fs_kHz > 8 ) {
            /* Search in original signal */

            /* Compensate for decimation */
            assert( lag == Silk_SigProc_FIX.SKP_SAT16( lag ) );
            if( Fs_kHz == 12 ) 
            {
                lag = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( Silk_macros.SKP_SMULBB( lag, 3 ), 1 );
            }
            else if( Fs_kHz == 16 )
            {
                lag = lag<<1;
            } 
            else 
            {
                lag = Silk_macros.SKP_SMULBB( lag, 3 );
            }

            lag = Silk_SigProc_FIX.SKP_LIMIT_int( lag, min_lag, max_lag );
            start_lag = Math.max( lag - 2, min_lag );
            end_lag   = Math.min( lag + 2, max_lag );
            lag_new   = lag;                                    /* to avoid undefined lag */
            CBimax    = 0;                                      /* to avoid undefined lag */
            assert( CCmax >= 0.0f ); 
            LTPCorr[0] = (float)Math.sqrt( CCmax / Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR );   // Output normalized correlation

            CCmax = -1000.0f;

            /* Calculate the correlations and energies needed in stage 3 */
            SKP_P_Ana_calc_corr_st3( cross_corr_st3, signal,0, start_lag, sf_length, complexity );
            SKP_P_Ana_calc_energy_st3( energies_st3, signal,0, start_lag, sf_length, complexity );

            lag_counter = 0;
            assert( lag == Silk_SigProc_FIX.SKP_SAT16( lag ) );
            contour_bias = Silk_pitch_est_defines_FLP.PITCH_EST_FLP_FLATCONTOUR_BIAS / lag;

            /* Setup cbk parameters acording to complexity setting */
            cbk_size   = Silk_pitch_est_tables.SKP_Silk_cbk_sizes_stage3[   complexity ];
            cbk_offset = Silk_pitch_est_tables.SKP_Silk_cbk_offsets_stage3[ complexity ];

            for( d = start_lag; d <= end_lag; d++ ) 
            {
                for( j = cbk_offset; j < ( cbk_offset + cbk_size ); j++ ) 
                {
                    cross_corr = 0.0;
                    energy = eps;
                    for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ ) 
                    {
                        energy     +=   energies_st3[ k ][ j ][ lag_counter ];
                        cross_corr += cross_corr_st3[ k ][ j ][ lag_counter ];
                    }
                    if( cross_corr > 0.0 ) 
                    {
                        CCmax_new = (float)(cross_corr * cross_corr / energy);
                        /* Reduce depending on flatness of contour */
                        diff = j - ( Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX >> 1 );
                        CCmax_new *= ( 1.0f - contour_bias * diff * diff );
                    } 
                    else
                    {
                        CCmax_new = 0.0f;               
                    }

                    if( CCmax_new > CCmax ) 
                    {
                        CCmax   = CCmax_new;
                        lag_new = d;
                        CBimax  = j;
                    }
                }
                lag_counter++;
            }

            for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ ) 
            {
                pitch_out[k] = lag_new + Silk_pitch_est_tables.SKP_Silk_CB_lags_stage3[ k ][ CBimax ];
            }
            lagIndex[0] = lag_new - min_lag;
            contourIndex[0] = CBimax;
        } 
        else
        {
            /* Save Lags and correlation */
            assert( CCmax >= 0.0f );
            LTPCorr[0] = (float)Math.sqrt(CCmax / Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR); /* Output normalized correlation */
            for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ ) 
            {
                pitch_out[ k ] = lag + Silk_pitch_est_tables.SKP_Silk_CB_lags_stage2[ k ][ CBimax ];
            }
            lagIndex[0] = lag - min_lag;
            contourIndex[0] = CBimax;
        }
        assert( lagIndex[0] >= 0 );
        /* return as voiced */
        return 0;
    }

    /**
     * Internally used functions.
     * 
     * @param cross_corr_st3 3 DIM correlation array.
     * @param signal vector to correlate.
     * @param signal_offset offset of valid data.
     * @param start_lag start lag.
     * @param sf_length sub frame length.
     * @param complexity Complexity setting.
     */
    static void SKP_P_Ana_calc_corr_st3
    (
        float[][][] cross_corr_st3,
        float signal[],           /* I vector to correlate                                            */
        int signal_offset,
        int start_lag,                  /* I start lag                                                      */
        int sf_length,                  /* I sub frame length                                               */
        int complexity                  /* I Complexity setting                                             */
    )
        /***********************************************************************
         Calculates the correlations used in stage 3 search. In order to cover 
         the whole lag codebook for all the searched offset lags (lag +- 2), 
         the following correlations are needed in each sub frame:

         sf1: lag range [-8,...,7] total 16 correlations
         sf2: lag range [-4,...,4] total 9 correlations
         sf3: lag range [-3,....4] total 8 correltions
         sf4: lag range [-6,....8] total 15 correlations

         In total 48 correlations. The direct implementation computed in worst case 
         4*12*5 = 240 correlations, but more likely around 120. 
         **********************************************************************/
    {
        float[] target_ptr, basis_ptr;
        int target_ptr_offset, basis_ptr_offset;
        int     i, j, k, lag_counter;
        int     cbk_offset, cbk_size, delta, idx;
        float[]   scratch_mem = new float[ SCRATCH_SIZE ];

        assert( complexity >= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MIN_COMPLEX );
        assert( complexity <= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX );

        cbk_offset = Silk_pitch_est_tables.SKP_Silk_cbk_offsets_stage3[ complexity ];
        cbk_size   = Silk_pitch_est_tables.SKP_Silk_cbk_sizes_stage3[   complexity ];

        target_ptr = signal;/* Pointer to middle of frame */
        target_ptr_offset = signal_offset+( sf_length << 2 );
        for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ ) 
        {
            lag_counter = 0;

            /* Calculate the correlations for each subframe */
            for( j = Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 0 ]; j <= Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 1 ]; j++ ) 
            {
                basis_ptr = target_ptr;
                basis_ptr_offset = target_ptr_offset - ( start_lag + j );
                assert( lag_counter < SCRATCH_SIZE );
                scratch_mem[ lag_counter ] = (float)Silk_inner_product_FLP.SKP_Silk_inner_product_FLP( target_ptr,target_ptr_offset, basis_ptr,basis_ptr_offset, sf_length );
                lag_counter++;
            }

            delta = Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 0 ];
            for( i = cbk_offset; i < ( cbk_offset + cbk_size ); i++ ) 
            { 
                /* Fill out the 3 dim array that stores the correlations for */
                /* each code_book vector for each start lag */
                idx = Silk_pitch_est_tables.SKP_Silk_CB_lags_stage3[ k ][ i ] - delta;
                for( j = 0; j < Silk_common_pitch_est_defines.PITCH_EST_NB_STAGE3_LAGS; j++ ) 
                {
                    assert( idx + j < SCRATCH_SIZE );
                    assert( idx + j < lag_counter );
                    cross_corr_st3[ k ][ i ][ j ] = scratch_mem[ idx + j ];
                }
            }
            target_ptr_offset += sf_length;
        }
    }

    /**
     * @param energies_st3 3 DIM correlation array.
     * @param signal vector to correlate.
     * @param signal_offset offset of valid data.
     * @param start_lag start lag.
     * @param sf_length sub frame length.
     * @param complexity Complexity setting.
     */
    static void SKP_P_Ana_calc_energy_st3
    (
        float[][][] energies_st3,
        float signal[],           /* I vector to correlate                                            */
        int signal_offset,
        int start_lag,                  /* I start lag                                                      */
        int sf_length,                  /* I sub frame length                                               */
        int complexity                  /* I Complexity setting                                             */
    )
    /****************************************************************
    Calculate the energies for first two subframes. The energies are
    calculated recursively. 
    ****************************************************************/
    {
        float[] target_ptr, basis_ptr;
        int target_ptr_offset, basis_ptr_offset;
        double      energy;
        int     k, i, j, lag_counter;
        int     cbk_offset, cbk_size, delta, idx;
        float[]   scratch_mem = new float[ SCRATCH_SIZE ];

        assert( complexity >= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MIN_COMPLEX );
        assert( complexity <= Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX );

        cbk_offset = Silk_pitch_est_tables.SKP_Silk_cbk_offsets_stage3[ complexity ];
        cbk_size   = Silk_pitch_est_tables.SKP_Silk_cbk_sizes_stage3[   complexity ];

        target_ptr = signal;
        target_ptr_offset = signal_offset+( sf_length << 2 );
        for( k = 0; k < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; k++ ) 
        {
            lag_counter = 0;

            /* Calculate the energy for first lag */
            basis_ptr = target_ptr;
            basis_ptr_offset = target_ptr_offset - ( start_lag + Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[complexity ][ k ][ 0 ]);
            energy = Silk_energy_FLP.SKP_Silk_energy_FLP( basis_ptr,basis_ptr_offset, sf_length ) + 1e-3;
            assert( energy >= 0.0 );
            scratch_mem[lag_counter] = (float)energy;
            lag_counter++;

            for( i = 1; i < ( Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 1 ] - Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 0 ] + 1 ); i++ ) 
            {
                /* remove part outside new window */
                energy -= basis_ptr[basis_ptr_offset + sf_length - i] * basis_ptr[basis_ptr_offset + sf_length - i];
                assert( energy >= 0.0 );

                /* add part that comes into window */
                energy += basis_ptr[ basis_ptr_offset -i ] * basis_ptr[ basis_ptr_offset-i ];
                assert( energy >= 0.0 );
                assert( lag_counter < SCRATCH_SIZE );
                scratch_mem[lag_counter] = (float)energy;
                lag_counter++;
            }

            delta = Silk_pitch_est_tables.SKP_Silk_Lag_range_stage3[ complexity ][ k ][ 0 ];
            for( i = cbk_offset; i < ( cbk_offset + cbk_size ); i++ ) 
            { 
                /* Fill out the 3 dim array that stores the correlations for    */
                /* each code_book vector for each start lag                     */
                idx = Silk_pitch_est_tables.SKP_Silk_CB_lags_stage3[ k ][ i ] - delta;
                for(j = 0; j < Silk_common_pitch_est_defines.PITCH_EST_NB_STAGE3_LAGS; j++)
                {
                    assert( idx + j < SCRATCH_SIZE );
                    assert( idx + j < lag_counter );
                    energies_st3[ k ][ i ][ j ] = scratch_mem[ idx + j ];
                    assert( energies_st3[ k ][ i ][ j ] >= 0.0f );
                }
            }
            target_ptr_offset += sf_length;
        }
    }
}
