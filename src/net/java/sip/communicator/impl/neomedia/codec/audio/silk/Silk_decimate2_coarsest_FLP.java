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
public class Silk_decimate2_coarsest_FLP 
{
    /* coefficients for coarsest 2-fold resampling */
    /* note that these differ from the interpolator with the same filter orders! */
    static float A20cst_FLP[  ] = {0.289001464843750f};
    static float A21cst_FLP[  ] = {0.780487060546875f};
    
    /**
     * downsample by a factor 2, coarsest.
     * @param in 16 kHz signal [2*len].
     * @param in_offset offset of the valid data.
     * @param S state vector [2].
     * @param S_offset offset of the valid data.
     * @param out 8 kHz signal [len].
     * @param out_offset offset of the valid data.
     * @param scratch scratch memory [3*len].
     * @param scratch_offset offset of the valid data.
     * @param len number of OUTPUT samples.
     */
    static void SKP_Silk_decimate2_coarsest_FLP(
        float[]           in,        /* I:   16 kHz signal [2*len]       */
        int               in_offset,
        float[]           S,         /* I/O: state vector [2]            */
        int               S_offset,
        float[]           out,       /* O:   8 kHz signal [len]          */
        int               out_offset,
        float[]           scratch,   /* I:   scratch memory [3*len]      */
        int               scratch_offset,
        final int         len         /* I:   number of OUTPUT samples    */
    )
    {
        int k;

        /* de-interleave allpass inputs */
        for ( k = 0; k < len; k++ ) 
        {
            scratch[ scratch_offset + k ]       = in[ in_offset + 2 * k + 0 ];
            scratch[ scratch_offset + k + len ] = in[ in_offset + 2 * k + 1 ];
        }

        /* allpass filters */
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset,     S,S_offset,   A21cst_FLP[ 0 ], scratch,scratch_offset+2 * len, len );
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset+len, S,S_offset+1, A20cst_FLP[ 0 ], scratch,scratch_offset,         len );

        /* add two allpass outputs */
        for ( k = 0; k < len; k++ ) 
        {
            out[ out_offset+k ] = 0.5f * ( scratch[ scratch_offset + k ] + scratch[ scratch_offset + k + 2 * len ] );
        }
    }
}
