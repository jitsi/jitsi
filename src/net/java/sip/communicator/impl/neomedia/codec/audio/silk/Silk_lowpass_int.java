/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**                                                                                                             
 * First order low-pass filter, with input as SKP_int32, running at    
 * 48 kHz   
 *                                                            
 * @author Dingxin Xu
 */
public class Silk_lowpass_int
{
    /**
     * First order low-pass filter, with input as SKP_int32, running at 48 kHz
     * @param in Q25 48 kHz signal; length = len
     * @param in_offset offset of valid data.
     * @param S Q25 state; length = 1  
     * @param S_offset offset of valid data.
     * @param out Q25 48 kHz signal; length = len
     * @param out_offset offset of valid data.
     * @param len Number of samples
     */
    static void SKP_Silk_lowpass_int(
        final int      []in,            /* I:    Q25 48 kHz signal; length = len */
        int            in_offset,
        int            []S,             /* I/O: Q25 state; length = 1            */
        int            S_offset,
        int            []out,           /* O:    Q25 48 kHz signal; length = len */
        int            out_offset,
        final int      len             /* I:    Number of samples               */
    )
    {
        int        k;
        int    in_tmp, out_tmp, state;
        
        state = S[ S_offset + 0 ];
        for( k = len; k > 0; k-- ) {    
            in_tmp  = in[in_offset++];
            in_tmp -= ( in_tmp >> 2 );              /* multiply by 0.75 */
            out_tmp = state + in_tmp;                       /* zero at nyquist  */
            state   = in_tmp - ( out_tmp >> 1 );    /* pole             */
            out[out_offset++]  = out_tmp;
        }
        S[ S_offset + 0 ] = state;
    }
}
