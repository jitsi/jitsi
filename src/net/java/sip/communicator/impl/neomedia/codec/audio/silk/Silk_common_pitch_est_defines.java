/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Definitions For Fix pitch estimator.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_common_pitch_est_defines 
{
    static final int PITCH_EST_MAX_FS_KHZ =               24; /* Maximum sampling frequency used */

    static final int PITCH_EST_FRAME_LENGTH_MS =          40; /* 40 ms */

    static final int PITCH_EST_MAX_FRAME_LENGTH =         (PITCH_EST_FRAME_LENGTH_MS * PITCH_EST_MAX_FS_KHZ);
    static final int PITCH_EST_MAX_FRAME_LENGTH_ST_1 =    (PITCH_EST_MAX_FRAME_LENGTH >> 2);
    static final int PITCH_EST_MAX_FRAME_LENGTH_ST_2 =    (PITCH_EST_MAX_FRAME_LENGTH >> 1);
//TODO: PITCH_EST_SUB_FRAME is neither defined nor used, temporally ignore it;
//    static final int PITCH_EST_MAX_SF_FRAME_LENGTH =      (PITCH_EST_SUB_FRAME * PITCH_EST_MAX_FS_KHZ);

    static final int PITCH_EST_MAX_LAG_MS =               18;           /* 18 ms -> 56 Hz */
    static final int PITCH_EST_MIN_LAG_MS =               2;            /* 2 ms -> 500 Hz */
    static final int PITCH_EST_MAX_LAG =                  (PITCH_EST_MAX_LAG_MS * PITCH_EST_MAX_FS_KHZ);
    static final int PITCH_EST_MIN_LAG =                  (PITCH_EST_MIN_LAG_MS * PITCH_EST_MAX_FS_KHZ);

    static final int PITCH_EST_NB_SUBFR =                 4;

    static final int PITCH_EST_D_SRCH_LENGTH =            24;

    static final int PITCH_EST_MAX_DECIMATE_STATE_LENGTH = 7;

    static final int PITCH_EST_NB_STAGE3_LAGS =           5;

    static final int PITCH_EST_NB_CBKS_STAGE2 =           3;
    static final int PITCH_EST_NB_CBKS_STAGE2_EXT =       11;

    static final int PITCH_EST_CB_mn2 =                   1;
    static final int PITCH_EST_CB_mx2 =                   2;

    static final int PITCH_EST_NB_CBKS_STAGE3_MAX =       34;
    static final int PITCH_EST_NB_CBKS_STAGE3_MID =       24;
    static final int PITCH_EST_NB_CBKS_STAGE3_MIN =       16;
}
