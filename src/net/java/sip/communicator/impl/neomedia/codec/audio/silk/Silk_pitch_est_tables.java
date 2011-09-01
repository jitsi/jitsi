/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Auto Generated File from generate_pitch_est_tables.m
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_pitch_est_tables 
{
    static short[][] SKP_Silk_CB_lags_stage2 =
    {
        {0, 2,-1,-1,-1, 0, 0, 1, 1, 0, 1},
        {0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0},
        {0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0},
        {0,-1, 2, 1, 0, 1, 1, 0, 0,-1,-1} 
    };

    static short[][] SKP_Silk_CB_lags_stage3 =
    {
        {-9,-7,-6,-5,-5,-4,-4,-3,-3,-2,-2,-2,-1,-1,-1, 0, 0, 0, 1, 1, 0, 1, 2, 2, 2, 3, 3, 4, 4, 5, 6, 5, 6, 8},
        {-3,-2,-2,-2,-1,-1,-1,-1,-1, 0, 0,-1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 2, 1, 2, 2, 2, 2, 3},
        { 3, 3, 2, 2, 2, 2, 1, 2, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,-1, 0, 0,-1,-1,-1,-1,-1,-2,-2,-2},
        { 9, 8, 6, 5, 6, 5, 4, 4, 3, 3, 2, 2, 2, 1, 0, 1, 1, 0, 0, 0,-1,-1,-1,-2,-2,-2,-3,-3,-4,-4,-5,-5,-6,-7}
     };

    static short[][][] SKP_Silk_Lag_range_stage3 =
    {
        /* Lags to search for low number of stage3 cbks */
        {
            {-2,6},
            {-1,5},
            {-1,5},
            {-2,7}
        },
        /* Lags to search for middle number of stage3 cbks */
        {
            {-4,8},
            {-1,6},
            {-1,6},
            {-4,9}
        },
        /* Lags to search for max number of stage3 cbks */
        {
            {-9,12},
            {-3,7},
            {-2,7},
            {-7,13}
        }
    };

    static short[] SKP_Silk_cbk_sizes_stage3 = 
    {
        Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MIN,
        Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MID,
        Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX
    };

    static short[] SKP_Silk_cbk_offsets_stage3 = 
    {
        ((Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX - Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MIN) >> 1),
        ((Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MAX - Silk_common_pitch_est_defines.PITCH_EST_NB_CBKS_STAGE3_MID) >> 1),
        0
    };
}
