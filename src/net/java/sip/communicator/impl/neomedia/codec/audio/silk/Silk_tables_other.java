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
public class Silk_tables_other 
{
    /* Piece-wise linear mapping from bitrate in kbps to coding quality in dB SNR */
    static final int[] TargetRate_table_NB =
    {
        0,      8000,   9000,   11000,  13000,  16000,  22000,  100000
    };
    
    static final int[] TargetRate_table_MB = {
        0,      10000,  12000,  14000,  17000,  21000,  28000,  100000
    };
    
    static final int[] TargetRate_table_WB = {
        0,      11000,  14000,  17000,  21000,  26000,  36000,  100000
    };
    
    static final int[] TargetRate_table_SWB = {
        0,      13000,  16000,  19000,  25000,  32000,  46000,  100000
    };
    
    static final int[] SNR_table_Q1 = {
        19,     31,     35,     39,     43,     47,     54,     59
    };

    static final int[] SNR_table_one_bit_per_sample_Q7 = {
        1984,   2240,   2408,   2708
    };

    /* Filter coeficicnts for HP filter: 4. Order filter implementad as two biquad filters  */
    static final short[][] SKP_Silk_SWB_detect_B_HP_Q13 = {
        {575, -948, 575}, {575, -221, 575}, {575, 104, 575} 
    };
    static final short[][] SKP_Silk_SWB_detect_A_HP_Q13 = {
        {14613, 6868}, {12883, 7337}, {11586, 7911}
    };

    /* Decoder high-pass filter coefficients for 24 kHz sampling, -6 dB @ 44 Hz */
    static final short[] SKP_Silk_Dec_A_HP_24 = {-16220, 8030};              // second order AR coefs, Q13
    static final short[] SKP_Silk_Dec_B_HP_24 = {8000, -16000, 8000};        // second order MA coefs, Q13
    
    /* Decoder high-pass filter coefficients for 16 kHz sampling, - 6 dB @ 46 Hz */
    static final short[] SKP_Silk_Dec_A_HP_16 = {-16127, 7940};              // second order AR coefs, Q13
    static final short[] SKP_Silk_Dec_B_HP_16  = {8000, -16000, 8000};        // second order MA coefs, Q13

    
    /* Decoder high-pass filter coefficients for 12 kHz sampling, -6 dB @ 44 Hz */
    static final short[] SKP_Silk_Dec_A_HP_12  = {-16043, 7859};              // second order AR coefs, Q13
    static final short[] SKP_Silk_Dec_B_HP_12 = {8000, -16000, 8000};        // second order MA coefs, Q13

    
    /* Decoder high-pass filter coefficients for 8 kHz sampling, -6 dB @ 43 Hz */
    static final short[] SKP_Silk_Dec_A_HP_8 = {-15885, 7710};               // second order AR coefs, Q13
    static final short[] SKP_Silk_Dec_B_HP_8 = {8000, -16000, 8000};         // second order MA coefs, Q13
    
    /* table for LSB coding */
    static final int[]  SKP_Silk_lsb_CDF = {0,  40000,  65535};

    /* tables for LTPScale */
    static final int[] SKP_Silk_LTPscale_CDF = {0,  32000,  48000,  65535};
    static final int SKP_Silk_LTPscale_offset   = 2;
    
    /* tables for VAD flag */
    static final int[] SKP_Silk_vadflag_CDF = {0,  22000,  65535}; // 66% for speech, 33% for no speech
    static final int SKP_Silk_vadflag_offset =1;
    
    /* tables for sampling rate */
    static final int[] SKP_Silk_SamplingRates_table  = {8, 12, 16, 24};
    static final int[] SKP_Silk_SamplingRates_CDF   = {0,  16000,  32000,  48000,  65535};
    static final int    SKP_Silk_SamplingRates_offset     = 2;

    /* tables for NLSF interpolation factor */
    static final int[] SKP_Silk_NLSF_interpolation_factor_CDF = {0,   3706,   8703,  19226,  30926,  65535};
    static final int SKP_Silk_NLSF_interpolation_factor_offset   = 4;

    /* Table for frame termination indication */
    static final int[] SKP_Silk_FrameTermination_CDF = {0, 20000, 45000, 56000, 65535};
    static final int SKP_Silk_FrameTermination_offset   = 2;

    /* Table for random seed */
    static final int[] SKP_Silk_Seed_CDF  = {0, 16384, 32768, 49152, 65535};
    static final int SKP_Silk_Seed_offset   = 2;
    
    /* Quantization offsets */
    static final short[][] SKP_Silk_Quantization_Offsets_Q10 = {
            { Silk_define.OFFSET_VL_Q10, Silk_define.OFFSET_VH_Q10 }, { Silk_define.OFFSET_UVL_Q10, Silk_define.OFFSET_UVH_Q10 }
        };

    /* Table for LTPScale */
    static final short[] SKP_Silk_LTPScales_table_Q14 = { 15565, 11469, 8192 };

    
    /*  Elliptic/Cauer filters designed with 0.1 dB passband ripple, 
            80 dB minimum stopband attenuation, and
            [0.95 : 0.15 : 0.35] normalized cut off frequencies. */

    /* Interpolation points for filter coefficients used in the bandwidth transition smoother */
    static final int[][] SKP_Silk_Transition_LP_B_Q28=
    {
    {    250767114,  501534038,  250767114  },
    {    209867381,  419732057,  209867381  },
    {    170987846,  341967853,  170987846  },
    {    131531482,  263046905,  131531482  },
    {     89306658,  178584282,   89306658  }
    };

    /* Interpolation points for filter coefficients used in the bandwidth transition smoother */
    static final int[][] SKP_Silk_Transition_LP_A_Q28 = 
    {
    {    506393414,  239854379  },
    {    411067935,  169683996  },
    {    306733530,  116694253  },
    {    185807084,   77959395  },
    {     35497197,   57401098  }
    };
}
