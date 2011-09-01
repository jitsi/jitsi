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
public class Silk_decimate2_coarse_FLP 
{
    /* coefficients for coarser 2-fold resampling */
    static float A20c_FLP[  ] = {0.064666748046875f, 0.508514404296875f};
    static float A21c_FLP[  ] = {0.245666503906250f, 0.819732666015625f};

    /**
     * downsample by a factor 2, coarser.
     * @param in 16 kHz signal [2*len].
     * @param in_offset offset of the valid data.
     * @param S state vector [4]. 
     * @param S_offset offset of the valid data.
     * @param out 8 kHz signal [len]
     * @param out_offset offset of the valid data.
     * @param scratch scratch memory [3*len].
     * @param scratch_offset offset of the valid data.
     * @param len number of OUTPUT samples.
     */
    static void SKP_Silk_decimate2_coarse_FLP
    (
        float[]        in,        /* I:   16 kHz signal [2*len]       */
        int            in_offset,
        float[]        S,         /* I/O: state vector [4]            */
        int            S_offset,
        float[]        out,       /* O:   8 kHz signal [len]          */
        int            out_offset,
        float[]        scratch,   /* I:   scratch memory [3*len]      */
        int            scratch_offset,
        final int      len        /* I:   number of OUTPUT samples    */
    )
    {
        int k;

        /* de-interleave allpass inputs */
        for ( k = 0; k < len; k++)
        {
            scratch[ scratch_offset + k ]       = in[ in_offset + 2 * k ];
            scratch[ scratch_offset + k + len ] = in[ in_offset + 2 * k + 1 ];
        }

        /* allpass filters */
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset, S,S_offset, A21c_FLP[ 0 ], scratch,scratch_offset + 2 * len, len );
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset + 2 * len, S,S_offset + 1, A21c_FLP[ 1 ], scratch,scratch_offset, len );
        
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset + len, S,S_offset + 2, A20c_FLP[ 0 ], scratch,scratch_offset + 2 * len, len );
        Silk_allpass_int_FLP.SKP_Silk_allpass_int_FLP( scratch,scratch_offset + 2 * len, S,S_offset + 3, A20c_FLP[ 1 ], scratch,scratch_offset + len, len );
        
        /* add two allpass outputs */
        for ( k = 0; k < len; k++ ) 
        {
            out[ out_offset+k ] = 0.5f * ( scratch[ scratch_offset+k ] + scratch[ scratch_offset + k + len ] );
        }       
    }
}
