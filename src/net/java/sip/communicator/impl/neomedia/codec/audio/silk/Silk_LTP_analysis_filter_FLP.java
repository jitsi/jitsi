/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

public class Silk_LTP_analysis_filter_FLP 
{
    /**
     * 
     * @param LTP_res LTP res NB_SUBFR*(pre_lgth+subfr_lngth)
     * @param x Input signal, with preceeding samples
     * @param x_offset offset of valid data.
     * @param B LTP coefficients for each subframe
     * @param pitchL Pitch lags
     * @param invGains Inverse quantization gains
     * @param subfr_length Length of each subframe
     * @param pre_length Preceeding samples for each subframe.
     */ 
    static void SKP_Silk_LTP_analysis_filter_FLP(
              float         []LTP_res,                   /* O    LTP res NB_SUBFR*(pre_lgth+subfr_lngth) */
        final float         []x,                         /* I    Input signal, with preceeding samples   */
              int           x_offset,
        final float         B[],                         /* I    LTP coefficients for each subframe      */
        final int           pitchL[],                    /* I    Pitch lags                              */
        final float         invGains[],                  /* I    Inverse quantization gains              */
        final int           subfr_length,                /* I    Length of each subframe                 */
        final int           pre_length                   /* I    Preceeding samples for each subframe    */
    )
    {
        final float []x_ptr;
        float [] x_lag_ptr;
        int x_ptr_offset, x_lag_ptr_offset;
        
        float   Btmp[] = new float[ Silk_define.LTP_ORDER ];
        float   []LTP_res_ptr;
        int     LTP_res_ptr_offset;
        float   inv_gain;
        int     k, i, j;

        x_ptr = x;
        x_ptr_offset = x_offset;
        LTP_res_ptr = LTP_res;
        LTP_res_ptr_offset = 0;
        for( k = 0; k < Silk_define.NB_SUBFR; k++ ) {
            x_lag_ptr = x_ptr;
            x_lag_ptr_offset = x_ptr_offset - pitchL[ k ];
            inv_gain = invGains[ k ];
            for( i = 0; i < Silk_define.LTP_ORDER; i++ ) {
                Btmp[ i ] = B[ k * Silk_define.LTP_ORDER + i ];
            }
            
            /* LTP analysis FIR filter */
            for( i = 0; i < subfr_length + pre_length; i++ ) {
                LTP_res_ptr[ LTP_res_ptr_offset + i ] = x_ptr[ x_ptr_offset + i ];
                /* Subtract long-term prediction */ 
                for( j = 0; j < Silk_define.LTP_ORDER; j++ ) {
                    LTP_res_ptr[ LTP_res_ptr_offset + i ] -= Btmp[ j ] * x_lag_ptr[ x_lag_ptr_offset + Silk_define.LTP_ORDER / 2 - j ];
                }
                LTP_res_ptr[ LTP_res_ptr_offset + i ] *= inv_gain;
                x_lag_ptr_offset++;
            }

            /* Update pointers */
            LTP_res_ptr_offset += subfr_length + pre_length; 
            x_ptr_offset       += subfr_length;
        }
    }
}
