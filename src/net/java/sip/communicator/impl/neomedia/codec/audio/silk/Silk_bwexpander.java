/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Chirp (bandwidth expand) LP AR filter
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_bwexpander
{
    /**
     * Chirp (bandwidth expand) LP AR filter.
     * @param ar AR filter to be expanded (without leading 1).
     * @param d Length of ar.
     * @param chirp_Q16 Chirp factor (typically in the range 0 to 1).
     */
    static void SKP_Silk_bwexpander( 
            short            []ar,        /* I/O  AR filter to be expanded (without leading 1)    */
            final int        d,          /* I    Length of ar                                    */
            int              chirp_Q16   /* I    Chirp factor (typically in the range 0 to 1)    */
    )
    {
        int   i;
        int chirp_minus_one_Q16;

        chirp_minus_one_Q16 = chirp_Q16 - 65536;

        /* NB: Dont use SKP_SMULWB, instead of SKP_RSHIFT_ROUND( SKP_MUL() , 16 ), below. */
        /* Bias in SKP_SMULWB can lead to unstable filters                                */
        for( i = 0; i < d - 1; i++ ) {
            ar[ i ]    = (short)Silk_SigProc_FIX.SKP_RSHIFT_ROUND( ( chirp_Q16 * ar[ i ]), 16 );
            chirp_Q16 +=            Silk_SigProc_FIX.SKP_RSHIFT_ROUND( ( chirp_Q16 * chirp_minus_one_Q16 ), 16 );
        }
        ar[ d - 1 ] = (short)Silk_SigProc_FIX.SKP_RSHIFT_ROUND( ( chirp_Q16 * ar[ d - 1 ] ), 16 );
    }
}
