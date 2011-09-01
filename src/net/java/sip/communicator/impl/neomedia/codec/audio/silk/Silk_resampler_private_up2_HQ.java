/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Upsample by a factor 2, high quality.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_private_up2_HQ
{
    /**
     * Upsample by a factor 2, high quality.
     * Uses 2nd order allpass filters for the 2x upsampling, followed by a      
     * notch filter just above Nyquist.   
     * @param S Resampler state [ 6 ].
     * @param S_offset offset of valid data.
     * @param out Output signal [ 2 * len ].
     * @param out_offset offset of valid data.
     * @param in Input signal [ len ].
     * @param in_offset offset of valid data.
     * @param len Number of INPUT samples.
     */
    static void SKP_Silk_resampler_private_up2_HQ(
        int[]                        S,               /* I/O: Resampler state [ 6 ]                   */
        int S_offset,
        short[]                     out,           /* O:   Output signal [ 2 * len ]               */
        int out_offset,
        short[]                     in,            /* I:   Input signal [ len ]                    */
        int in_offset,
        int                         len            /* I:   Number of INPUT samples                 */
    )
    {
        int k;
        int in32, out32_1, out32_2, Y, X;

        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_hq_0[ 0 ] > 0 );
        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_hq_0[ 1 ] < 0 );
        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_hq_1[ 0 ] > 0 );
        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_hq_1[ 1 ] < 0 );
        
        /* Internal variables and state are in Q10 format */
        for( k = 0; k < len; k++ ) 
        {
            /* Convert to Q10 */
            in32 = (int)in[ in_offset+k ] << 10;

            /* First all-pass section for even output sample */
            Y       = in32 - S[ S_offset ];
            X       = Silk_macros.SKP_SMULWB( Y, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_0[ 0 ] );
            out32_1 = S[ S_offset ] + X;
            S[ S_offset ]  = in32 + X;

            /* Second all-pass section for even output sample */
            Y       = out32_1 - S[ S_offset+1 ];
            X       = Silk_macros.SKP_SMLAWB( Y, Y, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_0[ 1 ] );
            out32_2 = S[ S_offset+1 ] + X;
            S[ S_offset+1 ]  = out32_1 + X;

            /* Biquad notch filter */
            out32_2 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+5 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 2 ] );
            out32_2 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+4 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 1 ] );
            out32_1 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+4 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 0 ] );
            S[ S_offset+5 ]  = out32_2 - S[ S_offset+5 ];
            
            /* Apply gain in Q15, convert back to int16 and store to output */
            out[ out_offset + 2 * k ] = (short)Silk_SigProc_FIX.SKP_SAT16( 
                Silk_macros.SKP_SMLAWB( 256, out32_1, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 3 ] ) >> 9 );

            /* First all-pass section for odd output sample */
            Y       = in32 - S[ S_offset+2 ];
            X       = Silk_macros.SKP_SMULWB( Y, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_1[ 0 ] );
            out32_1 = S[ S_offset+2 ] + X;
            S[ S_offset+2 ]  = in32 + X;

            /* Second all-pass section for odd output sample */
            Y       = out32_1 - S[ S_offset+3 ];
            X       = Silk_macros.SKP_SMLAWB( Y, Y, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_1[ 1 ] );
            out32_2 = S[ S_offset+3 ] + X;
            S[ S_offset+3 ]  = out32_1 + X;

            /* Biquad notch filter */
            out32_2 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+4 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 2 ] );
            out32_2 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+5 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 1 ] );
            out32_1 = Silk_macros.SKP_SMLAWB( out32_2, S[ S_offset+5 ], Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 0 ] );
            S[ S_offset+4 ]  = out32_2 - S[ S_offset+4 ];
            
            /* Apply gain in Q15, convert back to int16 and store to output */
            out[ out_offset + 2 * k + 1 ] = (short)Silk_SigProc_FIX.SKP_SAT16(  
                Silk_macros.SKP_SMLAWB( 256, out32_1, Silk_resampler_rom.SKP_Silk_resampler_up2_hq_notch[ 3 ] ) >> 9 );
        }
    }
    
    /**
     * the wrapper method.
     * @param SS Resampler state (unused).
     * @param out Output signal [ 2 * len ].
     * @param out_offset offset of valid data.
     * @param in Input signal [ len ].
     * @param in_offset offset of valid data.
     * @param len Number of input samples.
     */
    static void SKP_Silk_resampler_private_up2_HQ_wrapper(
        Object                            SS,               /* I/O: Resampler state (unused)                */
        short[]                         out,           /* O:   Output signal [ 2 * len ]               */
        int out_offset,
        short[]                         in,            /* I:   Input signal [ len ]                    */
        int in_offset,
        int                             len            /* I:   Number of input samples                 */
    )
    {
        SKP_Silk_resampler_state_struct S = (SKP_Silk_resampler_state_struct )SS;
        SKP_Silk_resampler_private_up2_HQ( S.sIIR,0, out,out_offset, in,in_offset, len );
    }
}
