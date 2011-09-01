/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_structs_FLP 
{

}

/**
 * Noise shaping analysis state.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
class SKP_Silk_shape_state_FLP
{
    int     LastGainIndex;
    float   HarmBoost_smth;
    float   HarmShapeGain_smth;
    float   Tilt_smth;
    
    /**
     * set all fields of the instance to zero
     */
    public void memZero()
    {
        this.LastGainIndex = 0;
        this.HarmBoost_smth = 0;
        this.HarmShapeGain_smth = 0;
        this.Tilt_smth = 0;
    }
}

/**
 * Prefilter state 
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
class SKP_Silk_prefilter_state_FLP
{
    float[]   sLTP_shp1 = new float[ Silk_define.LTP_BUF_LENGTH ];
    float[]   sLTP_shp2 = new float[ Silk_define.LTP_BUF_LENGTH ];
    float[]   sAR_shp1 = new float[ Silk_define.SHAPE_LPC_ORDER_MAX + 1 ];
    float[]   sAR_shp2 = new float[ Silk_define.SHAPE_LPC_ORDER_MAX ];
    int     sLTP_shp_buf_idx1;
    int     sLTP_shp_buf_idx2;
    int     sAR_shp_buf_idx2;
    float   sLF_AR_shp1;
    float   sLF_MA_shp1;
    float   sLF_AR_shp2;
    float   sLF_MA_shp2;
    float   sHarmHP;
    int   rand_seed;
    int     lagPrev;
    
    /**
     * set all fields of the instance to zero
     */
    public void memZero()
    {
        Arrays.fill(this.sAR_shp1, 0);
        Arrays.fill(this.sAR_shp2, 0);
        Arrays.fill(this.sLTP_shp1, 0);
        Arrays.fill(this.sLTP_shp2, 0);
        
        this.sLTP_shp_buf_idx1 = 0;
        this.sLTP_shp_buf_idx2 = 0;
        this.sAR_shp_buf_idx2 = 0;
        this.sLF_AR_shp1 = 0;
        this.sLF_AR_shp2 = 0;
        this.sLF_MA_shp1 = 0;
        this.sLF_MA_shp2 = 0;
        this.sHarmHP = 0;
        this.rand_seed = 0;
        this.lagPrev = 0;
    }
} 

/**
 * Prediction analysis state
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
class SKP_Silk_predict_state_FLP
{
    int     pitch_LPC_win_length;
    int     min_pitch_lag;                      /* Lowest possible pitch lag (samples)  */
    int     max_pitch_lag;                      /* Highest possible pitch lag (samples) */
    float[]   prev_NLSFq = new float[ Silk_define.MAX_LPC_ORDER ];        /* Previously quantized NLSF vector     */
    
    /**
     * set all fields of the instance to zero
     */
    public void memZero()
    {
        this.pitch_LPC_win_length = 0;
        this.max_pitch_lag = 0;
        this.min_pitch_lag = 0;
        Arrays.fill(this.prev_NLSFq, 0.0f);
    }
}

/*******************************************/
/* Structure containing NLSF MSVQ codebook */
/*******************************************/
/* structure for one stage of MSVQ */
class SKP_Silk_NLSF_CBS_FLP
{
    public SKP_Silk_NLSF_CBS_FLP()
    {
        super();
    }
    
    public SKP_Silk_NLSF_CBS_FLP(int nVectors, float[] CB, float[] Rates)
    {
        this.nVectors = nVectors;
        this.CB = CB;
        this.Rates = Rates;
    }
    
    public SKP_Silk_NLSF_CBS_FLP(int nVectors, float[] CB, int CB_offset, float[] Rates, int Rates_offset)
    {
        this.nVectors = nVectors;
        this.CB = new float[CB.length - CB_offset];
        System.arraycopy(CB, CB_offset, this.CB, 0, this.CB.length);
        this.Rates = new float[Rates.length - Rates_offset];
        System.arraycopy(Rates, Rates_offset, this.Rates, 0, this.Rates.length);
    }

    int         nVectors;
    float[]     CB;
    float[]     Rates;
} 

class SKP_Silk_NLSF_CB_FLP 
{
    public SKP_Silk_NLSF_CB_FLP()
    {
        super();
    }
    
    public SKP_Silk_NLSF_CB_FLP(int nStages, SKP_Silk_NLSF_CBS_FLP[] CBStages, 
            float[] NDeltaMin, int[] CDF, int[][] StartPtr, int[] MiddleIx)
    {
        this.nStages = nStages;
        this.CBStages = CBStages;
        this.NDeltaMin = NDeltaMin;
        this.CDF = CDF;
        this.StartPtr = StartPtr;
        this.MiddleIx = MiddleIx;
    }
//const SKP_int32                         nStages;    
    int                         nStages;

    /* fields for (de)quantizing */
    SKP_Silk_NLSF_CBS_FLP[] CBStages;
    float[]                         NDeltaMin;

    /* fields for arithmetic (de)coding */
//    const SKP_uint16                        *CDF;
    int[] CDF;
//    const SKP_uint16 * const                *StartPtr;
    int[][] StartPtr;
//    const SKP_int                           *MiddleIx;
    int[] MiddleIx;
} 

/************************************/
/* Noise shaping quantization state */
/************************************/

/**
 * Encoder state FLP.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
class SKP_Silk_encoder_state_FLP
{
//    SKP_Silk_encoder_state              sCmn;                       /* Common struct, shared with fixed-point code */
    SKP_Silk_encoder_state              sCmn = new SKP_Silk_encoder_state(); /* Common struct, shared with fixed-point code */


    float                           variable_HP_smth1;          /* State of first smoother */
    float                           variable_HP_smth2;          /* State of second smoother */

    SKP_Silk_shape_state_FLP            sShape = new SKP_Silk_shape_state_FLP();                     /* Noise shaping state */
    SKP_Silk_prefilter_state_FLP        sPrefilt = new SKP_Silk_prefilter_state_FLP();                   /* Prefilter State */
    SKP_Silk_predict_state_FLP          sPred = new SKP_Silk_predict_state_FLP();                      /* Prediction State */
    SKP_Silk_nsq_state                  sNSQ = new SKP_Silk_nsq_state();                       /* Noise Shape Quantizer State */
    SKP_Silk_nsq_state                  sNSQ_LBRR = new SKP_Silk_nsq_state();                  /* Noise Shape Quantizer State ( for low bitrate redundancy )*/

    /* Function pointer to noise shaping quantizer (will be set to SKP_Silk_NSQ or SKP_Silk_NSQ_del_dec) */
//    void    (* NoiseShapingQuantizer)( SKP_Silk_encoder_state *, SKP_Silk_encoder_control *, SKP_Silk_nsq_state *, const SKP_int16 *, 
//                                       SKP_int8 *, const SKP_int, const SKP_int16 *, const SKP_int16 *, const SKP_int16 *, const SKP_int *, 
//                                        const SKP_int *, const SKP_int32 *, const SKP_int32 *, SKP_int, const SKP_int
//    );
    NoiseShapingQuantizerFP noiseShapingQuantizerCB;
    void    NoiseShapingQuantizer( SKP_Silk_encoder_state psEnc, SKP_Silk_encoder_control psEncCtrl, SKP_Silk_nsq_state NSQ, final short[]x , 
        byte[]q , final int arg6, final short[] arg7, final short[]arg8, final short[]arg9, final int[]arg10, 
         final int []arg11, final int[]arg12, final int[]arg13, int arg14 , final int arg15
    )
    {
        noiseShapingQuantizerCB.NoiseShapingQuantizer(psEnc, psEncCtrl, NSQ, x, q, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }
    

    /* Buffer for find pitch and noise shape analysis */
    float[]                         x_buf = new float[ 2 * Silk_define.MAX_FRAME_LENGTH + Silk_define.LA_SHAPE_MAX ];/* Buffer for find pitch and noise shape analysis */
// djinn: add a parameter: offset
    int x_buf_offset;
    float                           LTPCorr;                    /* Normalized correlation from pitch lag estimator */
    float                           mu_LTP;                     /* Rate-distortion tradeoff in LTP quantization */
    float                           SNR_dB;                     /* Quality setting */
    float                           avgGain;                    /* average gain during active speech */
    float                           BufferedInChannel_ms;       /* Simulated number of ms buffer in channel because of exceeded TargetRate_bps */
    float                           speech_activity;            /* Speech activity */
    float                           pitchEstimationThreshold;   /* Threshold for pitch estimator */

    /* Parameters for LTP scaling control */
    float                           prevLTPredCodGain;
    float                           HPLTPredCodGain;

    float                           inBandFEC_SNR_comp;         /* Compensation to SNR_DB when using inband FEC Voiced */

    SKP_Silk_NLSF_CB_FLP[]  psNLSF_CB_FLP = new SKP_Silk_NLSF_CB_FLP[ 2 ];        /* Pointers to voiced/unvoiced NLSF codebooks */
} 

/**
 * Encoder control FLP
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
class SKP_Silk_encoder_control_FLP
{
    SKP_Silk_encoder_control    sCmn = new SKP_Silk_encoder_control();                               /* Common struct, shared with fixed-point code */

    /* Prediction and coding parameters */
    float[]                   Gains = new float[Silk_define.NB_SUBFR];
    float[][]                   PredCoef = new float[ 2 ][ Silk_define.MAX_LPC_ORDER ];     /* holds interpolated and final coefficients */
    float[]                   LTPCoef = new float[Silk_define.LTP_ORDER * Silk_define.NB_SUBFR];
    float                   LTP_scale;

    /* Prediction and coding parameters */
    int[]                   Gains_Q16 = new int[ Silk_define.NB_SUBFR ];
//TODO:    SKP_array_of_int16_4_byte_aligned( PredCoef_Q12[ 2 ], MAX_LPC_ORDER );
    int dummy_int32PredCoef_Q12[] = new int[ 2 ];                                
    short PredCoef_Q12[][] = new short[ 2 ][Silk_define.MAX_LPC_ORDER];

    short[]                   LTPCoef_Q14 = new short[ Silk_define.LTP_ORDER * Silk_define.NB_SUBFR ];
    int                     LTP_scale_Q14;

    /* Noise shaping parameters */
    /* Testing */
//TODO    SKP_array_of_int16_4_byte_aligned( AR2_Q13, NB_SUBFR * SHAPE_LPC_ORDER_MAX );
    int dummy_int32AR2_Q13;
    short[] AR2_Q13 = new short[Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX];

    int[]                     LF_shp_Q14 = new int[        Silk_define.NB_SUBFR ];      /* Packs two int16 coefficients per int32 value             */
    int[]                     Tilt_Q14 = new int[          Silk_define.NB_SUBFR ];
    int[]                     HarmShapeGain_Q14 = new int[ Silk_define.NB_SUBFR ];
    int                     Lambda_Q10;

    /* Noise shaping parameters */
    float[]                   AR1 = new float[ Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX ];
    float[]                   AR2 = new float[ Silk_define.NB_SUBFR * Silk_define.SHAPE_LPC_ORDER_MAX ];
    float[]                   LF_MA_shp = new float[     Silk_define.NB_SUBFR ];
    float[]                   LF_AR_shp = new float[     Silk_define.NB_SUBFR ];
    float[]                   GainsPre = new float[      Silk_define.NB_SUBFR ];
    float[]                   HarmBoost = new float[     Silk_define.NB_SUBFR ];
    float[]                   Tilt = new float[          Silk_define.NB_SUBFR ];
    float[]                   HarmShapeGain = new float[ Silk_define.NB_SUBFR ];
    float                   Lambda;
    float                   input_quality;
    float                   coding_quality;
    float                   pitch_freq_low_Hz;
    float                   current_SNR_dB;

    /* Measures */
    float                   sparseness;
    float                   LTPredCodGain;
    float[]                   input_quality_bands = new float[ Silk_define.VAD_N_BANDS ];
    float                   input_tilt;
    float[]                   ResNrg = new float[ Silk_define.NB_SUBFR ];                 /* Residual energy per subframe */
} 

interface NoiseShapingQuantizerFP
{
    /* Function pointer to noise shaping quantizer (will be set to SKP_Silk_NSQ or SKP_Silk_NSQ_del_dec) */
  void    NoiseShapingQuantizer( SKP_Silk_encoder_state psEnc, SKP_Silk_encoder_control psEncCtrl, SKP_Silk_nsq_state NSQ, final short[]x , 
                                     byte[]q , final int arg6, final short[] arg7, final short[]arg8, final short[]arg9, final int[]arg10, 
                                      final int []arg11, final int[]arg12, final int[]arg13, int arg14 , final int arg15
  );
  
    /* Function pointer to noise shaping quantizer (will be set to SKP_Silk_NSQ or SKP_Silk_NSQ_del_dec) */
//  void    (* NoiseShapingQuantizer)( SKP_Silk_encoder_state *, SKP_Silk_encoder_control *, SKP_Silk_nsq_state *, const SKP_int16 *, 
//                                     SKP_int8 *, const SKP_int, const SKP_int16 *, const SKP_int16 *, const SKP_int16 *, const SKP_int *, 
//                                      const SKP_int *, const SKP_int32 *, const SKP_int32 *, SKP_int, const SKP_int
//  );
}
