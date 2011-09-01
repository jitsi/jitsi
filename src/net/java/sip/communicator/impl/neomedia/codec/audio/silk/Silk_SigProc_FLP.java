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
class Silk_SigProc_FLP_constants
{
    /* Pitch estimator */
    static final int SigProc_PITCH_EST_MIN_COMPLEX =       0;
    static final int SigProc_PITCH_EST_MID_COMPLEX =       1;
    static final int SigProc_PITCH_EST_MAX_COMPLEX =       2;
    
    static final float PI =              3.1415926536f;
}

public class Silk_SigProc_FLP
    extends Silk_SigProc_FLP_constants
{
    static float SKP_min_float(float a, float b)
    {
        return (((a) < (b)) ? (a) :  (b));
    }
    static float SKP_max_float(float a, float b)
    {
        return (((a) > (b)) ? (a) :  (b));
    }
    static float SKP_abs_float(float a)
    {
        return Math.abs(a);
    }

    static float SKP_LIMIT_float( float a, float limit1, float limit2)
    {
        if( limit1 > limit2 )
            return a > limit1 ? limit1 : (a < limit2 ? limit2 : a);
        else
            return a > limit2 ? limit2 : (a < limit1 ? limit1 : a);
    }

    /* sigmoid function */
    static float SKP_sigmoid(float x)
    {
        return (float)(1.0 / (1.0 + Math.exp(-x)));
    }

    /* floating-point to integer conversion (rounding) */
    static void SKP_float2short_array
    (
        short[]       out,
        int out_offset,
        float[]       in, 
        int in_offset,
        int       length
    ) 
    {
        int k;
        for (k = length-1; k >= 0; k--) 
        {
            double x = in[in_offset+k];
            out[out_offset+k] = (short)Silk_SigProc_FIX.SKP_SAT16( (int)(( x > 0 ) ? x + 0.5 : x - 0.5) );
        }
    }

    /* floating-point to integer conversion (rounding) */
    static int SKP_float2int(double x) 
    {
        return (int)( ( x > 0 ) ? x + 0.5 : x - 0.5 );
    }

    /* integer to floating-point conversion */
    static void SKP_short2float_array
    (
        float[]       out,
        int out_offset,
        short[]           in,
        int in_offset,
        int       length
    ) 
    {
        int k;
        for (k = length-1; k >= 0; k--)
        {
            out[out_offset+k] = (float)in[in_offset+k];
        }
    }

//TODO:    #define SKP_round(x)        (SKP_float)((x)>=0 ? (SKP_int64)((x)+0.5) : (SKP_int64)((x)-0.5))
    static float SKP_round(float x)
    {
        return (float)((x)>=0 ? (long)(x+0.5) : (long)(x-0.5));
    }
}
