/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * This class contains a number of defines that controls the operation of SILK.
 * Most of these should be left alone for ensuring proper operation. However, a
 * few can be changed if operation different from the default is desired.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_define 
{
    static final int MAX_FRAMES_PER_PACKET =                  5;

    /* MAX DELTA LAG used for multiframe packets */
    static final int MAX_DELTA_LAG =                          10;

    /* Lower limit on bitrate for each mode */
    static final int MIN_TARGET_RATE_NB_BPS =                 5000;
    static final int MIN_TARGET_RATE_MB_BPS =                 7000;
    static final int MIN_TARGET_RATE_WB_BPS =                 8000;
    static final int MIN_TARGET_RATE_SWB_BPS =                20000;

    /* Transition bitrates between modes */
    static final int SWB2WB_BITRATE_BPS =                     26000;
    static final int WB2SWB_BITRATE_BPS =                     32000;
    static final int WB2MB_BITRATE_BPS  =                     15000;
    static final int MB2WB_BITRATE_BPS  =                     20000;
    static final int MB2NB_BITRATE_BPS  =                     10000;
    static final int NB2MB_BITRATE_BPS  =                     14000;

    /* Integration/hysteresis threshold for lowering internal sample frequency */
    /* 30000000 -> 6 sec if bitrate is 5000 bps below limit; 3 sec if bitrate is 10000 bps below limit */
    static final int ACCUM_BITS_DIFF_THRESHOLD =              30000000; 
    static final int TARGET_RATE_TAB_SZ =                     8;

    /* DTX settings                                 */
    static final int NO_SPEECH_FRAMES_BEFORE_DTX =            5;       /* eq 100 ms */
    static final int MAX_CONSECUTIVE_DTX =                    20;      /* eq 400 ms */

    static final int USE_LBRR =                               1;

    /* Amount of concecutive no FEC packets before telling JB */
    static final int NO_LBRR_THRES =                          10;

    /* Maximum delay between real packet and LBRR packet */
    static final int MAX_LBRR_DELAY =                         2;
    static final int LBRR_IDX_MASK  =                         1;

    static final int INBAND_FEC_MIN_RATE_BPS =                18000;  /* Dont use inband FEC below this total target rate  */
    static final int LBRR_LOSS_THRES =                        2;   /* Start adding LBRR at this loss rate (needs tuning)   */

    /* LBRR usage defines */
    static final int SKP_SILK_NO_LBRR =                       0;   /* No LBRR information for this packet                  */
    static final int SKP_SILK_ADD_LBRR_TO_PLUS1 =             1;   /* Add LBRR for this packet to packet n + 1             */
    static final int SKP_SILK_ADD_LBRR_TO_PLUS2 =             2;   /* Add LBRR for this packet to packet n + 2             */

    /* Frame termination indicator defines */
    static final int SKP_SILK_LAST_FRAME =                    0;   /* Last frames in packet                                */
    static final int SKP_SILK_MORE_FRAMES =                   1;   /* More frames to follow this one                       */
    static final int SKP_SILK_LBRR_VER1 =                     2;  /* LBRR information from packet n - 1                   */
    static final int SKP_SILK_LBRR_VER2 =                     3;   /* LBRR information from packet n - 2                   */
    static final int SKP_SILK_EXT_LAYER =                     4;   /* Extension layers added                               */

    /* Number of Second order Sections for SWB detection HP filter */
    static final int NB_SOS =                                 3;
    static final int HP_8_KHZ_THRES=                          10;          /* average energy per sample, above 8 kHz       */
    static final int CONCEC_SWB_SMPLS_THRES=                  480 * 15;    /* 300 ms                                       */
    static final int WB_DETECT_ACTIVE_SPEECH_MS_THRES =       15000;       /* ms of active speech needed for WB detection  */

    /* Low complexity setting */
    static final int LOW_COMPLEXITY_ONLY =                    0;

    /* Activate bandwidth transition filtering for mode switching */
    static final int SWITCH_TRANSITION_FILTERING =            1;
    
    /* Decoder Parameters */
    static final int DEC_HP_ORDER =                           2;

    /* Maximum sampling frequency, should be 16 for embedded */
    static final int MAX_FS_KHZ =                             24; 
    static final int MAX_API_FS_KHZ=                          48;

    /* Signal Types used by silk */
    static final int SIG_TYPE_VOICED =                        0;
    static final int SIG_TYPE_UNVOICED =                      1;

    /* VAD Types used by silk */
    static final int NO_VOICE_ACTIVITY =                      0;
    static final int VOICE_ACTIVITY =                         1;

    /* Number of samples per frame */ 
    static final int FRAME_LENGTH_MS =                        20; /* 20 ms */
    static final int MAX_FRAME_LENGTH =                       (FRAME_LENGTH_MS * MAX_FS_KHZ);

    /* Milliseconds of lookahead for pitch analysis */
    static final int LA_PITCH_MS =                            3;
    static final int LA_PITCH_MAX =                           (LA_PITCH_MS * MAX_FS_KHZ);

    /* Milliseconds of lookahead for noise shape analysis */
    static final int LA_SHAPE_MS  =                           5;
    static final int LA_SHAPE_MAX =                           (LA_SHAPE_MS * MAX_FS_KHZ);

    /* Order of LPC used in find pitch */
    static final int FIND_PITCH_LPC_ORDER_MAX =               16;

    /* Length of LPC window used in find pitch */
    static final int FIND_PITCH_LPC_WIN_MS =                  (30 + (LA_PITCH_MS << 1));
    static final int FIND_PITCH_LPC_WIN_MAX =                 (FIND_PITCH_LPC_WIN_MS * MAX_FS_KHZ);

    static final int PITCH_EST_COMPLEXITY_HC_MODE =           Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MAX_COMPLEX;
    static final int PITCH_EST_COMPLEXITY_MC_MODE =           Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MID_COMPLEX;
    static final int PITCH_EST_COMPLEXITY_LC_MODE =           Silk_SigProc_FIX.SKP_Silk_PITCH_EST_MIN_COMPLEX;


    /* Max number of bytes in payload output buffer (may contain multiple frames) */
    static final int MAX_ARITHM_BYTES =                       1024;

    static final int RANGE_CODER_WRITE_BEYOND_BUFFER =        -1;
    static final int RANGE_CODER_CDF_OUT_OF_RANGE =           -2;
    static final int RANGE_CODER_NORMALIZATION_FAILED =       -3;
    static final int RANGE_CODER_ZERO_INTERVAL_WIDTH =        -4;
    static final int RANGE_CODER_DECODER_CHECK_FAILED =       -5;
    static final int RANGE_CODER_READ_BEYOND_BUFFER =         -6;
    static final int RANGE_CODER_ILLEGAL_SAMPLING_RATE =      -7;
    static final int RANGE_CODER_DEC_PAYLOAD_TOO_LONG =       -8;

    /* dB level of lowest gain quantization level */
    static final int MIN_QGAIN_DB =                           6;
    /* dB level of highest gain quantization level */
    static final int MAX_QGAIN_DB =                           86;
    /* Number of gain quantization levels */
    static final int N_LEVELS_QGAIN  =                        64;
    /* Max increase in gain quantization index */
    static final int MAX_DELTA_GAIN_QUANT =                   40;
    /* Max decrease in gain quantization index */
    static final int MIN_DELTA_GAIN_QUANT =                   -4;

    /* Quantization offsets (multiples of 4) */
    static final int OFFSET_VL_Q10 =                          32;
    static final int OFFSET_VH_Q10 =                          100;
    static final int OFFSET_UVL_Q10 =                         100;
    static final int OFFSET_UVH_Q10 =                         256;

    /* Maximum numbers of iterations used to stabilize a LPC vector */
    static final int MAX_LPC_STABILIZE_ITERATIONS =           20;

    static final int MAX_LPC_ORDER =                          16;
    static final int MIN_LPC_ORDER =                          10;

    /* Find Pred Coef defines */
    static final int LTP_ORDER =                              5;

    /* LTP quantization settings */
    static final int NB_LTP_CBKS =                            3;

    /* Number of subframes */
    static final int NB_SUBFR =                               4;

    /* Flag to use harmonic noise shaping */
    static final int USE_HARM_SHAPING  =                      1;

    /* Max LPC order of noise shaping filters */
    static final int SHAPE_LPC_ORDER_MAX =                    16;

    static final int HARM_SHAPE_FIR_TAPS =                    3;

    /* Length of LPC window used in noise shape analysis */
    static final int SHAPE_LPC_WIN_MS =                       15;
    static final int SHAPE_LPC_WIN_16_KHZ =                   (SHAPE_LPC_WIN_MS * 16);
    static final int SHAPE_LPC_WIN_24_KHZ =                   (SHAPE_LPC_WIN_MS * 24);
    static final int SHAPE_LPC_WIN_MAX =                      (SHAPE_LPC_WIN_MS * MAX_FS_KHZ);

    /* Maximum number of delayed decision states */
    static final int DEL_DEC_STATES_MAX =                     4;

    static final int LTP_BUF_LENGTH =                         512;
    static final int LTP_MASK =                               (LTP_BUF_LENGTH - 1);

    static final int DECISION_DELAY =                         32;
    static final int DECISION_DELAY_MASK =                    (DECISION_DELAY - 1);

    /* number of subframes for excitation entropy coding */
    static final int SHELL_CODEC_FRAME_LENGTH =               16;
    static final int MAX_NB_SHELL_BLOCKS =                    (MAX_FRAME_LENGTH / SHELL_CODEC_FRAME_LENGTH);

    /* number of rate levels, for entropy coding of excitation */
    static final int N_RATE_LEVELS =                          10;

    /* maximum sum of pulses per shell coding frame */
    static final int MAX_PULSES =                             18;

    static final int MAX_MATRIX_SIZE =                        MAX_LPC_ORDER; /* Max of LPC Order and LTP order */

//TODO: convert a macro to a method.    
    static int NSQ_LPC_BUF_LENGTH()
    {
        if(MAX_LPC_ORDER > DECISION_DELAY)
            return MAX_LPC_ORDER;
        else
            return DECISION_DELAY;
    }
    /***********************/
    /* High pass filtering */
    /***********************/
    static final int HIGH_PASS_INPUT =                        1;

    /***************************/
    /* Voice activity detector */
    /***************************/
    static final int VAD_N_BANDS =                            4;       /* 0-1, 1-2, 2-4, and 4-8 kHz                       */

    static final int VAD_INTERNAL_SUBFRAMES_LOG2 =            2;
    static final int VAD_INTERNAL_SUBFRAMES =                 (1 << VAD_INTERNAL_SUBFRAMES_LOG2);
        
    static final int VAD_NOISE_LEVEL_SMOOTH_COEF_Q16 =        1024;    /* Must be <  4096                                  */
    static final int VAD_NOISE_LEVELS_BIAS =                  50 ;

    /* Sigmoid settings */
    static final int VAD_NEGATIVE_OFFSET_Q5 =                 128 ;    /* sigmoid is 0 at -128                             */
    static final int VAD_SNR_FACTOR_Q16 =                     45000 ;

    /* smoothing for SNR measurement */
    static final int VAD_SNR_SMOOTH_COEF_Q18 =                4096;

    /******************/
    /* NLSF quantizer */
    /******************/
    static final int NLSF_MSVQ_MAX_CB_STAGES =                     10;  /* Update manually when changing codebooks      */
    static final int NLSF_MSVQ_MAX_VECTORS_IN_STAGE =              128;/* Update manually when changing codebooks      */
    static final int NLSF_MSVQ_MAX_VECTORS_IN_STAGE_TWO_TO_END =   16;  /* Update manually when changing codebooks      */

    static final int NLSF_MSVQ_FLUCTUATION_REDUCTION =        1;
    static final int MAX_NLSF_MSVQ_SURVIVORS =                16;
    static final int MAX_NLSF_MSVQ_SURVIVORS_LC_MODE =        2;
    static final int MAX_NLSF_MSVQ_SURVIVORS_MC_MODE =        4;

    /* Based on above defines, calculate how much memory is necessary to allocate */
    static int NLSF_MSVQ_TREE_SEARCH_MAX_VECTORS_EVALUATED_LC_MODE()
    {
        if(NLSF_MSVQ_MAX_VECTORS_IN_STAGE > ( MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * NLSF_MSVQ_MAX_VECTORS_IN_STAGE_TWO_TO_END ))
            return NLSF_MSVQ_MAX_VECTORS_IN_STAGE;
        else 
            return MAX_NLSF_MSVQ_SURVIVORS_LC_MODE * NLSF_MSVQ_MAX_VECTORS_IN_STAGE_TWO_TO_END;
    }

    static int NLSF_MSVQ_TREE_SEARCH_MAX_VECTORS_EVALUATED()
    {
        if( NLSF_MSVQ_MAX_VECTORS_IN_STAGE > ( MAX_NLSF_MSVQ_SURVIVORS * NLSF_MSVQ_MAX_VECTORS_IN_STAGE_TWO_TO_END ) )
            return NLSF_MSVQ_MAX_VECTORS_IN_STAGE;
        else
            return MAX_NLSF_MSVQ_SURVIVORS * NLSF_MSVQ_MAX_VECTORS_IN_STAGE_TWO_TO_END;
    }
    
    static final int NLSF_MSVQ_SURV_MAX_REL_RD  =             4;

    /* Transition filtering for mode switching */
    static final int TRANSITION_TIME_UP_MS =            5120; // 5120 = 64 * FRAME_LENGTH_MS * ( TRANSITION_INT_NUM - 1 ) = 64*(20*4)
    static final int TRANSITION_TIME_DOWN_MS =          2560; // 2560 = 32 * FRAME_LENGTH_MS * ( TRANSITION_INT_NUM - 1 ) = 32*(20*4)
    static final int TRANSITION_NB =                    3; /* Hardcoded in tables */
    static final int TRANSITION_NA =                    2; /* Hardcoded in tables */
    static final int TRANSITION_INT_NUM =               5; /* Hardcoded in tables */
    static final int TRANSITION_FRAMES_UP =         ( TRANSITION_TIME_UP_MS   / FRAME_LENGTH_MS );
    static final int TRANSITION_FRAMES_DOWN =       ( TRANSITION_TIME_DOWN_MS / FRAME_LENGTH_MS );
    static final int TRANSITION_INT_STEPS_UP =      ( TRANSITION_FRAMES_UP    / ( TRANSITION_INT_NUM - 1 )  );
    static final int TRANSITION_INT_STEPS_DOWN =    ( TRANSITION_FRAMES_DOWN  / ( TRANSITION_INT_NUM - 1 )  );
    
//TODO:no need to convert from C to Java?    
    /* Row based */
//    #define matrix_ptr(Matrix_base_adr, row, column, N)         *(Matrix_base_adr + ((row)*(N)+(column)))
//    #define matrix_adr(Matrix_base_adr, row, column, N)          (Matrix_base_adr + ((row)*(N)+(column)))

    /* Column based */
//    #ifndef matrix_c_ptr
//    #   define matrix_c_ptr(Matrix_base_adr, row, column, M)    *(Matrix_base_adr + ((row)+(M)*(column)))
//    #endif
//    #define matrix_c_adr(Matrix_base_adr, row, column, M)        (Matrix_base_adr + ((row)+(M)*(column)))

    /* BWE factors to apply after packet loss */
    static final int BWE_AFTER_LOSS_Q16 =                             63570;

    /* Defines for CN generation */
    static final int CNG_BUF_MASK_MAX  =                              255;             /* 2^floor(log2(MAX_FRAME_LENGTH))  */
    static final int CNG_GAIN_SMTH_Q16 =                              4634;            /* 0.25^(1/4)                       */
    static final int CNG_NLSF_SMTH_Q16 =                              16348;           /* 0.25                             */
}
