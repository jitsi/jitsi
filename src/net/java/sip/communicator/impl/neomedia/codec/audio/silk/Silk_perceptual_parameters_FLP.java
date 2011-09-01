/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Perceptual parameters.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
//TODO: float or double???
public class Silk_perceptual_parameters_FLP 
{
    /* reduction in coding SNR during low speech activity */
    static final float BG_SNR_DECR_dB =                             3.0f;

    /* factor for reducing quantization noise during voiced speech */
    static final float HARM_SNR_INCR_dB =                           2.0f;

    /* factor for reducing quantization noise for unvoiced sparse signals */
    static final float SPARSE_SNR_INCR_dB =                         2.0f;

    /* threshold for sparseness measure above which to use lower quantization offset during unvoiced */
    static final float SPARSENESS_THRESHOLD_QNT_OFFSET =            0.75f;


    /* noise shaping filter chirp factor */
    static final float BANDWIDTH_EXPANSION =                        0.94f;

    /* difference between chirp factors for analysis and synthesis noise shaping filters at low bitrates */
    static final float LOW_RATE_BANDWIDTH_EXPANSION_DELTA =         0.01f;

    /* factor to reduce all bandwdith expansion coefficients for super wideband, relative to wideband */
    static final float SWB_BANDWIDTH_EXPANSION_REDUCTION =          1.0f;

    /* gain reduction for fricatives */
    static final float DE_ESSER_COEF_SWB_dB =                       2.0f;
    static final float DE_ESSER_COEF_WB_dB =                        1.0f;


    /* extra harmonic boosting (signal shaping) at low bitrates */
    static final float LOW_RATE_HARMONIC_BOOST =                    0.1f;

    /* extra harmonic boosting (signal shaping) for noisy input signals */
    static final float LOW_INPUT_QUALITY_HARMONIC_BOOST =           0.1f;

    /* harmonic noise shaping */
    static final float HARMONIC_SHAPING =                           0.3f;

    /* extra harmonic noise shaping for high bitrates or noisy input */
    static final float HIGH_RATE_OR_LOW_QUALITY_HARMONIC_SHAPING =  0.2f;


    /* parameter for shaping noise towards higher frequencies */
    static final float HP_NOISE_COEF =                              0.3f;

    /* parameter for shaping noise extra towards higher frequencies during voiced speech */
    static final float HARM_HP_NOISE_COEF =                         0.45f;

    /* parameter for applying a high-pass tilt to the input signal */
    static final float INPUT_TILT =                                 0.04f;

    /* parameter for extra high-pass tilt to the input signal at high rates */
    static final float HIGH_RATE_INPUT_TILT =                       0.06f;

    /* parameter for reducing noise at the very low frequencies */
    static final float LOW_FREQ_SHAPING =                           3.0f;

    /* less reduction of noise at the very low frequencies for signals with low SNR at low frequencies */
    static final float LOW_QUALITY_LOW_FREQ_SHAPING_DECR =          0.5f;

    /* fraction added to first autocorrelation value */
    static final float SHAPE_WHITE_NOISE_FRACTION =                 4.7684e-5f;

    /* fraction of first autocorrelation value added to residual energy value; limits prediction gain */
    static final float SHAPE_MIN_ENERGY_RATIO =                     1.526e-5f;       // 1.526e-5 = 1/65536

    /* noise floor to put a low limit on the quantization step size */
    static final float NOISE_FLOOR_dB =                             4.0f;

    /* noise floor relative to active speech gain level */
    static final float RELATIVE_MIN_GAIN_dB =                       -50.0f;

    /* subframe smoothing coefficient for determining active speech gain level (lower -> more smoothing) */
    static final float GAIN_SMOOTHING_COEF =                        1e-3f;

    /* subframe smoothing coefficient for HarmBoost, HarmShapeGain, Tilt (lower -> more smoothing) */
    static final float SUBFR_SMTH_COEF =                            0.4f;

    /* Amount of noise added in prefilter, to simulate quantization noise */
    static final float NOISE_GAIN_VL =                              0.12f;
    static final float NOISE_GAIN_VH =                              0.12f;
    static final float NOISE_GAIN_UVL =                             0.1f;
    static final float NOISE_GAIN_UVH =                             0.15f;
}
