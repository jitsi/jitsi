/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Resample with a 2x downsampler (optional), a 2nd order AR filter followed by FIR interpolation.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_private_down_FIR 
{
    /**
     * Resample with a 2x downsampler (optional), 
     * a 2nd order AR filter followed by FIR interpolation.
     * @param SS Resampler state.
     * @param out Output signal.
     * @param out_offset offset of valid data.
     * @param in Input signal.
     * @param in_offset offset of valid data.
     * @param inLen Number of input samples.
     */
    static void SKP_Silk_resampler_private_down_FIR(
        Object                      SS,               /* I/O: Resampler state                         */
        short[]                        out,            /* O:    Output signal                             */
        int out_offset,
        short[]                        in,             /* I:    Input signal                            */
        int in_offset,
        int                         inLen            /* I:    Number of input samples                    */
    )
    {
        SKP_Silk_resampler_state_struct S = (SKP_Silk_resampler_state_struct)SS;
        int nSamplesIn, interpol_ind;
        int max_index_Q16, index_Q16, index_increment_Q16, res_Q6;
        short[] buf1 = new short[ Silk_resampler_private.RESAMPLER_MAX_BATCH_SIZE_IN / 2 ];
        int[] buf2 = new int[ Silk_resampler_private.RESAMPLER_MAX_BATCH_SIZE_IN + Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR ];
        int[] buf_ptr;
        int buf_ptr_offset;
        short[] interpol_ptr, FIR_Coefs;
        int interpol_ptr_offset, FIR_Coefs_offset;

        /* Copy buffered samples to start of buffer */    
//TODO: arrayCopy();        
//        SKP_memcpy( buf2, S->sFIR, RESAMPLER_DOWN_ORDER_FIR * sizeof( SKP_int32 ) );
        for(int i_djinn=0; i_djinn<Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR; i_djinn++)
            buf2[i_djinn] = S.sFIR[i_djinn];

        FIR_Coefs = S.Coefs;
        FIR_Coefs_offset = 2;

        /* Iterate over blocks of frameSizeIn input samples */
        index_increment_Q16 = S.invRatio_Q16;
        while( true ) 
        {
            nSamplesIn = Math.min( inLen, S.batchSize );

            if( S.input2x == 1 ) 
            {
                /* Downsample 2x */
                Silk_resampler_down2.SKP_Silk_resampler_down2( S.sDown2,0, buf1,0, in,in_offset, nSamplesIn );

                nSamplesIn = nSamplesIn >> 1;

                /* Second-order AR filter (output in Q8) */
                Silk_resampler_private_AR2.SKP_Silk_resampler_private_AR2( S.sIIR,0, buf2,Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR, buf1,0, S.Coefs,0, nSamplesIn );
            }
            else
            {
                /* Second-order AR filter (output in Q8) */
                Silk_resampler_private_AR2.SKP_Silk_resampler_private_AR2( S.sIIR,0, buf2,Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR, in,in_offset, S.Coefs,0, nSamplesIn );
            }

            max_index_Q16 = nSamplesIn << 16;

            /* Interpolate filtered signal */
            if( S.FIR_Fracs == 1 ) 
            {
                for( index_Q16 = 0; index_Q16 < max_index_Q16; index_Q16 += index_increment_Q16 )
                {
                    /* Integer part gives pointer to buffered input */
                    buf_ptr = buf2;
                    buf_ptr_offset = index_Q16 >> 16;

                    /* Inner product */
                    res_Q6 = Silk_macros.SKP_SMULWB(         buf_ptr[ buf_ptr_offset   ]+buf_ptr[ buf_ptr_offset+11 ], FIR_Coefs[ FIR_Coefs_offset   ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+1 ]+buf_ptr[ buf_ptr_offset+10 ], FIR_Coefs[ FIR_Coefs_offset+1 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+2 ]+buf_ptr[  buf_ptr_offset+9 ], FIR_Coefs[ FIR_Coefs_offset+2 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+3 ]+buf_ptr[  buf_ptr_offset+8 ], FIR_Coefs[ FIR_Coefs_offset+3 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+4 ]+buf_ptr[  buf_ptr_offset+7 ], FIR_Coefs[ FIR_Coefs_offset+4 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+5 ]+buf_ptr[  buf_ptr_offset+6 ], FIR_Coefs[ FIR_Coefs_offset+5 ] );

                    /* Scale down, saturate and store in output array */
                    out[out_offset++] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( res_Q6, 6 ) );
                }
            } 
            else 
            {
                for( index_Q16 = 0; index_Q16 < max_index_Q16; index_Q16 += index_increment_Q16 ) 
                {
                    /* Integer part gives pointer to buffered input */
                    buf_ptr = buf2;
                    buf_ptr_offset = index_Q16 >> 16;

                    /* Fractional part gives interpolation coefficients */
                    interpol_ind = Silk_macros.SKP_SMULWB( index_Q16 & 0xFFFF, S.FIR_Fracs );

                    /* Inner product */
                    interpol_ptr = FIR_Coefs;
//BugFix                    interpol_ptr_offset = Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR / 2 * interpol_ind;
                    interpol_ptr_offset = FIR_Coefs_offset + Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR / 2 * interpol_ind;
                    res_Q6 = Silk_macros.SKP_SMULWB(         buf_ptr[ buf_ptr_offset   ], interpol_ptr[ interpol_ptr_offset   ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+1 ], interpol_ptr[ interpol_ptr_offset+1 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+2 ], interpol_ptr[ interpol_ptr_offset+2 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+3 ], interpol_ptr[ interpol_ptr_offset+3 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+4 ], interpol_ptr[ interpol_ptr_offset+4 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+5 ], interpol_ptr[ interpol_ptr_offset+5 ] );
                    interpol_ptr = FIR_Coefs;
//BugFix                    interpol_ptr_offset = Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR / 2 * ( S.FIR_Fracs - 1 - interpol_ind );
                    interpol_ptr_offset = FIR_Coefs_offset + Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR / 2 * ( S.FIR_Fracs - 1 - interpol_ind );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+11 ], interpol_ptr[ interpol_ptr_offset   ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[ buf_ptr_offset+10 ], interpol_ptr[ interpol_ptr_offset+1 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[  buf_ptr_offset+9 ], interpol_ptr[ interpol_ptr_offset+2 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[  buf_ptr_offset+8 ], interpol_ptr[ interpol_ptr_offset+3 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[  buf_ptr_offset+7 ], interpol_ptr[ interpol_ptr_offset+4 ] );
                    res_Q6 = Silk_macros.SKP_SMLAWB( res_Q6, buf_ptr[  buf_ptr_offset+6 ], interpol_ptr[ interpol_ptr_offset+5 ] );

                    /* Scale down, saturate and store in output array */
                    out[out_offset++] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( res_Q6, 6 ) );
                }
            }

            in_offset += nSamplesIn << S.input2x;
            inLen -= nSamplesIn << S.input2x;

            if( inLen > S.input2x ) 
            {
                /* More iterations to do; copy last part of filtered signal to beginning of buffer */
//TODO: arrayCopy();
//                SKP_memcpy( buf2, &buf2[ nSamplesIn ], RESAMPLER_DOWN_ORDER_FIR * sizeof( SKP_int32 ) );
                for(int i_djinn=0; i_djinn<Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR; i_djinn++)
                    buf2[i_djinn] = buf2[nSamplesIn+i_djinn];
            }
            else
            {
                break;
            }
        }

        /* Copy last part of filtered signal to the state for the next call */
//TODO: arrayCopy();        
//        SKP_memcpy( S->sFIR, &buf2[ nSamplesIn ], RESAMPLER_DOWN_ORDER_FIR * sizeof( SKP_int32 ) );
        for(int i_djinn=0; i_djinn<Silk_resampler_rom.RESAMPLER_DOWN_ORDER_FIR; i_djinn++)
            S.sFIR[i_djinn] = buf2[nSamplesIn+i_djinn];
    }
}
