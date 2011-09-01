/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * LPC analysis filter                     
 * NB! State is kept internally and the    
 * filter always starts with zero state    
 * first Order output samples are not set 
 * 
 * @author Jing Dai
 * @author Dignxin Xu
 */
public class Silk_LPC_analysis_filter_FLP 
{
    /**
     * 
     * @param r_LPC LPC residual signal
     * @param PredCoef LPC coefficients
     * @param s Input signal
     * @param s_offset offset of valid data.
     * @param length Length of input signal
     * @param Order LPC order
     */
    static void SKP_Silk_LPC_analysis_filter_FLP(
              float                 r_LPC[],            /* O    LPC residual signal                     */
              float                 PredCoef[],         /* I    LPC coefficients                        */
              float                 s[],                /* I    Input signal                            */
              int                   s_offset,
        final int                   length,             /* I    Length of input signal                  */
        final int                   Order               /* I    LPC order                               */
    )
    {
        assert( Order <= length );

        switch( Order ) 
        {
            case 8:
                SKP_Silk_LPC_analysis_filter8_FLP(  r_LPC, PredCoef, s, s_offset, length );
            break;

            case 10:
                SKP_Silk_LPC_analysis_filter10_FLP( r_LPC, PredCoef, s, s_offset, length );
            break;

            case 12:
                SKP_Silk_LPC_analysis_filter12_FLP( r_LPC, PredCoef, s, s_offset, length );
            break;

            case 16:
                SKP_Silk_LPC_analysis_filter16_FLP( r_LPC, PredCoef, s, s_offset, length );
            break;

            default:
                assert( false );
            break;
        }

        /* Set first LPC Order samples to zero instead of undefined */
        for(int i=0; i<Order; i++)
            r_LPC[i] = 0;
    }

    /**
     * 16th order LPC analysis filter, does not write first 16 samples.
     * @param r_LPC LPC residual signal
     * @param PredCoef LPC coefficients
     * @param s Input signal
     * @param s_offset
     * @param length Length of input signal
     */
    static void SKP_Silk_LPC_analysis_filter16_FLP(
              float                 r_LPC[],            /* O    LPC residual signal                     */
              float                 PredCoef[],         /* I    LPC coefficients                        */
              float                 s[],                /* I    Input signal                            */
              int                   s_offset,
        final int                   length              /* I    Length of input signal                  */
    )
    {
        int   ix = 16;
        float LPC_pred;
        float[] s_ptr;
        int s_ptr_offset;

        for ( ; ix < length; ix++) 
        {
            s_ptr = s;
            s_ptr_offset = s_offset + ix - 1;

            /* short-term prediction */
            LPC_pred = s_ptr[ s_ptr_offset ]   * PredCoef[ 0 ]  + 
                       s_ptr[s_ptr_offset-1]  * PredCoef[ 1 ]  +
                       s_ptr[s_ptr_offset-2]  * PredCoef[ 2 ]  +
                       s_ptr[s_ptr_offset-3]  * PredCoef[ 3 ]  +
                       s_ptr[s_ptr_offset-4]  * PredCoef[ 4 ]  +
                       s_ptr[s_ptr_offset-5]  * PredCoef[ 5 ]  +
                       s_ptr[s_ptr_offset-6]  * PredCoef[ 6 ]  +
                       s_ptr[s_ptr_offset-7]  * PredCoef[ 7 ]  +
                       s_ptr[s_ptr_offset-8]  * PredCoef[ 8 ]  +
                       s_ptr[s_ptr_offset-9]  * PredCoef[ 9 ]  +
                       s_ptr[s_ptr_offset-10] * PredCoef[ 10 ] +
                       s_ptr[s_ptr_offset-11] * PredCoef[ 11 ] +
                       s_ptr[s_ptr_offset-12] * PredCoef[ 12 ] +
                       s_ptr[s_ptr_offset-13] * PredCoef[ 13 ] +
                       s_ptr[s_ptr_offset-14] * PredCoef[ 14 ] +
                       s_ptr[s_ptr_offset-15] * PredCoef[ 15 ];

            /* prediction error */
            r_LPC[ix] = s_ptr[ s_ptr_offset+1 ] - LPC_pred;
        }
    }

    /**
     * 12th order LPC analysis filter, does not write first 12 samples.
     * @param r_LPC LPC residual signal
     * @param PredCoef LPC coefficients
     * @param s Input signal
     * @param s_offset offset of valid data.
     * @param length Length of input signal
     */
    static void SKP_Silk_LPC_analysis_filter12_FLP(
              float                 r_LPC[],            /* O    LPC residual signal                     */
              float                 PredCoef[],         /* I    LPC coefficients                        */
              float                 s[],                /* I    Input signal                            */
              int                   s_offset,
        final int                   length              /* I    Length of input signal                  */
    )
    {
        int   ix = 12;
        float LPC_pred;
        float[] s_ptr;
        int s_ptr_offset;

        for ( ; ix < length; ix++) 
        {
            s_ptr = s;
            s_ptr_offset = s_offset + ix - 1;

            /* short-term prediction */
            LPC_pred = s_ptr[ s_ptr_offset ]   * PredCoef[ 0 ]  + 
                       s_ptr[s_ptr_offset-1]  * PredCoef[ 1 ]  +
                       s_ptr[s_ptr_offset-2]  * PredCoef[ 2 ]  +
                       s_ptr[s_ptr_offset-3]  * PredCoef[ 3 ]  +
                       s_ptr[s_ptr_offset-4]  * PredCoef[ 4 ]  +
                       s_ptr[s_ptr_offset-5]  * PredCoef[ 5 ]  +
                       s_ptr[s_ptr_offset-6]  * PredCoef[ 6 ]  +
                       s_ptr[s_ptr_offset-7]  * PredCoef[ 7 ]  +
                       s_ptr[s_ptr_offset-8]  * PredCoef[ 8 ]  +
                       s_ptr[s_ptr_offset-9]  * PredCoef[ 9 ]  +
                       s_ptr[s_ptr_offset-10] * PredCoef[ 10 ] +
                       s_ptr[s_ptr_offset-11] * PredCoef[ 11 ];

            /* prediction error */
            r_LPC[ix] = s_ptr[ s_ptr_offset+1 ] - LPC_pred;
        }
    }

    /**
     * 10th order LPC analysis filter, does not write first 10 samples
     * @param r_LPC LPC residual signal
     * @param PredCoef LPC coefficients 
     * @param s Input signal 
     * @param s_offset offset of valid data.
     * @param length Length of input signal
     */
    static void SKP_Silk_LPC_analysis_filter10_FLP(
            float                 r_LPC[],            /* O    LPC residual signal                     */
            float                 PredCoef[],         /* I    LPC coefficients                        */
            float                 s[],                /* I    Input signal                            */
            int                   s_offset,
        final int                 length              /* I    Length of input signal                  */
    )
    {
        int   ix = 10;
        float LPC_pred;
        float[] s_ptr;
        int s_ptr_offset;

        for ( ; ix < length; ix++) {
            s_ptr = s;
            s_ptr_offset = s_offset + ix - 1;

            /* short-term prediction */
            LPC_pred = s_ptr[ s_ptr_offset ]   * PredCoef[ 0 ]  + 
                       s_ptr[s_ptr_offset-1]  * PredCoef[ 1 ]  +
                       s_ptr[s_ptr_offset-2]  * PredCoef[ 2 ]  +
                       s_ptr[s_ptr_offset-3]  * PredCoef[ 3 ]  +
                       s_ptr[s_ptr_offset-4]  * PredCoef[ 4 ]  +
                       s_ptr[s_ptr_offset-5]  * PredCoef[ 5 ]  +
                       s_ptr[s_ptr_offset-6]  * PredCoef[ 6 ]  +
                       s_ptr[s_ptr_offset-7]  * PredCoef[ 7 ]  +
                       s_ptr[s_ptr_offset-8]  * PredCoef[ 8 ]  +
                       s_ptr[s_ptr_offset-9]  * PredCoef[ 9 ];

            /* prediction error */
            r_LPC[ix] = s_ptr[ s_ptr_offset+1 ] - LPC_pred;
        }
    }

    /**
     * 8th order LPC analysis filter, does not write first 8 samples.
     * @param r_LPC LPC residual signal
     * @param PredCoef LPC coefficients 
     * @param s Input signal
     * @param s_offset offset of valid data.
     * @param length  Length of input signal
     */
    static void SKP_Silk_LPC_analysis_filter8_FLP(
              float                 r_LPC[],            /* O    LPC residual signal                     */
              float                 PredCoef[],         /* I    LPC coefficients                        */
              float                 s[],                /* I    Input signal                            */
              int                   s_offset,
        final int                   length              /* I    Length of input signal                  */
    )
    {
        int   ix = 8;
        float LPC_pred;
        float[] s_ptr;
        int s_ptr_offset;

        for ( ; ix < length; ix++) {
            s_ptr = s;
            s_ptr_offset = s_offset + ix - 1;

            /* short-term prediction */
            LPC_pred = s_ptr[  s_ptr_offset ] * PredCoef[ 0 ]  + 
                       s_ptr[ s_ptr_offset-1 ] * PredCoef[ 1 ]  +
                       s_ptr[ s_ptr_offset-2 ] * PredCoef[ 2 ]  +
                       s_ptr[ s_ptr_offset-3 ] * PredCoef[ 3 ]  +
                       s_ptr[ s_ptr_offset-4 ] * PredCoef[ 4 ]  +
                       s_ptr[ s_ptr_offset-5 ] * PredCoef[ 5 ]  +
                       s_ptr[ s_ptr_offset-6 ] * PredCoef[ 6 ]  +
                       s_ptr[ s_ptr_offset-7 ] * PredCoef[ 7 ];

            /* prediction error */
            r_LPC[ix] = s_ptr[ s_ptr_offset+1 ] - LPC_pred;
        }
    }
}
