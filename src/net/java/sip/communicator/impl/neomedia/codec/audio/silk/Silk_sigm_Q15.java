/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * approximate sigmoid function.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_sigm_Q15 
{
    /* fprintf(1, '%d, ', round(1024 * ([1 ./ (1 + exp(-(1:5))), 1] - 1 ./ (1 + exp(-(0:5)))))); */
    static int[] sigm_LUT_slope_Q10 = 
    {
        237, 153, 73, 30, 12, 7
    };
    /* fprintf(1, '%d, ', round(32767 * 1 ./ (1 + exp(-(0:5))))); */
    static int[] sigm_LUT_pos_Q15 = 
    {
        16384, 23955, 28861, 31213, 32178, 32548
    };
    /* fprintf(1, '%d, ', round(32767 * 1 ./ (1 + exp((0:5))))); */
    static int[] sigm_LUT_neg_Q15 = 
    {
        16384, 8812, 3906, 1554, 589, 219
    };

    static int SKP_Silk_sigm_Q15( int in_Q5 ) 
    {
        int ind;

        if( in_Q5 < 0 ) 
        {
            /* Negative input */
            in_Q5 = -in_Q5;
            if( in_Q5 >= 6 * 32 ) 
            {
                return 0;        /* Clip */
            }
            else 
            {
                /* Linear interpolation of look up table */
                ind = in_Q5 >> 5;
                return( sigm_LUT_neg_Q15[ ind ] - Silk_macros.SKP_SMULBB( sigm_LUT_slope_Q10[ ind ], in_Q5 & 0x1F ) );
            }
        } 
        else 
        {
            /* Positive input */
            if( in_Q5 >= 6 * 32 ) 
            {
                return 32767;        /* clip */
            }
            else 
            {
                /* Linear interpolation of look up table */
                ind = in_Q5 >> 5;
                return( sigm_LUT_pos_Q15[ ind ] + Silk_macros.SKP_SMULBB( sigm_LUT_slope_Q10[ ind ], in_Q5 & 0x1F ) );
            }
        }
    }
}
