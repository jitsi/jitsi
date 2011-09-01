/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**                                                                   
 * First order low-pass filter, with input as SKP_int16, running at     
 * 48 kHz     
 *                                                           
 * @author Dingxin Xu
 */
public class Silk_lowpass_short 
{
    /**
     * First order low-pass filter, with input as SKP_int16, running at 48 kHz.
     * @param in Q15 48 kHz signal; [len]
     * @param in_offset offset of valid data.
     * @param S Q25 state; length = 1 
     * @param S_offset offset of valid data.
     * @param out Q25 48 kHz signal; [len]
     * @param out_offset offset of valid data.
     * @param len Signal length
     */
    static void SKP_Silk_lowpass_short(
        final short []in,        /* I:   Q15 48 kHz signal; [len]    */
        int         in_offset,
        int         []S,         /* I/O: Q25 state; length = 1       */
        int         S_offset,
        int         []out,       /* O:   Q25 48 kHz signal; [len]    */
        int         out_offset,
        final int   len         /* O:   Signal length               */
    )
    {
        int        k;
        int    in_tmp, out_tmp, state;
        
        state = S[ S_offset + 0 ];
        for( k = 0; k < len; k++ ) {    
            in_tmp   = ( 768 * (int)in[in_offset + k] );    /* multiply by 0.75, going from Q15 to Q25 */
            out_tmp  = state + in_tmp;                      /* zero at nyquist                         */
            state    = in_tmp - ( out_tmp >> 1 );   /* pole                                    */
            out[ out_offset + k ] = out_tmp;
        }
        S[ S_offset + 0 ] = state;
    }
}
