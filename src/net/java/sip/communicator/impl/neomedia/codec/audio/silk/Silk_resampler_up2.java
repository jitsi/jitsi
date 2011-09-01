/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Up-sample by a factor 2, low quality.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_up2 
{
    /**
     * Up-sample by a factor 2, low quality.
     * @param S State vector [ 2 ].
     * @param S_offset offset of valid data.
     * @param out Output signal [ 2 * len ].
     * @param out_offset offset of valid data.
     * @param in Input signal [ len ].
     * @param in_offset offset of valid data.
     * @param len Number of input samples.
     */
    static void SKP_Silk_resampler_up2(
        int[]                           S,         /* I/O: State vector [ 2 ]                  */
        int S_offset,
        short[]                         out,       /* O:   Output signal [ 2 * len ]           */
        int out_offset,
        short[]                         in,        /* I:   Input signal [ len ]                */
        int in_offset,
        int                             len        /* I:   Number of input samples             */
    )
    {
        int k;
        int in32, out32, Y, X;

        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_lq_0 > 0 );
        assert( Silk_resampler_rom.SKP_Silk_resampler_up2_lq_1 < 0 );
        /* Internal variables and state are in Q10 format */
        for( k = 0; k < len; k++ ) 
        {
            /* Convert to Q10 */
            in32 = (int)in[ in_offset+k ] << 10;

            /* All-pass section for even output sample */
            Y      = in32 - S[ S_offset + 0 ];
            X      = Silk_macros.SKP_SMULWB( Y, Silk_resampler_rom.SKP_Silk_resampler_up2_lq_0 );
            out32  = S[ S_offset + 0 ] + X;
            S[ S_offset + 0 ] = in32 + X;

            /* Convert back to int16 and store to output */
            out[ out_offset + 2 * k ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32, 10 ) );

            /* All-pass section for odd output sample */
            Y      = in32 - S[ S_offset + 1 ];
            X      = Silk_macros.SKP_SMLAWB( Y, Y, Silk_resampler_rom.SKP_Silk_resampler_up2_lq_1 );
            out32  = S[ S_offset + 1 ] + X;
            S[ S_offset + 1 ] = in32 + X;

            /* Convert back to int16 and store to output */
            out[ out_offset + 2 * k + 1 ] = (short)Silk_SigProc_FIX.SKP_SAT16( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( out32, 10 ) );
        }
    }
}
