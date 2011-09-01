/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Pitch analyzer function.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_decode_pitch 
{
    /**
     * Pitch analyzer function.
     * @param lagIndex 
     * @param contourIndex
     * @param pitch_lags 4 pitch values.
     * @param Fs_kHz sampling frequency(kHz).
     */
    static void SKP_Silk_decode_pitch(
            int          lagIndex,                        /* I                             */
            int          contourIndex,                    /* O                             */
            int          pitch_lags[],                    /* O 4 pitch values              */
            int          Fs_kHz                           /* I sampling frequency (kHz)    */
    )
    {
        int lag, i, min_lag;

        min_lag = Silk_macros.SKP_SMULBB(Silk_common_pitch_est_defines.PITCH_EST_MIN_LAG_MS, Fs_kHz);

        /* Only for 24 / 16 kHz version for now */
        lag = min_lag + lagIndex;
        if( Fs_kHz == 8 ) {
            /* Only a small codebook for 8 khz */
            for( i = 0; i < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i++ ) {
                pitch_lags[ i ] = lag + Silk_pitch_est_tables.SKP_Silk_CB_lags_stage2[ i ][ contourIndex ];
            }
        } else {
            for( i = 0; i < Silk_common_pitch_est_defines.PITCH_EST_NB_SUBFR; i++ ) {
                pitch_lags[ i ] = lag + Silk_pitch_est_tables.SKP_Silk_CB_lags_stage3[ i ][ contourIndex ];
            }
        }
    }
}
