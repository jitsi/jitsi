/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Convert input to a log scale
 *  
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_lin2log
{
    /**
     * Approximation of 128 * log2() (very close inverse of approx 2^() below) 
     * Convert input to a log scale.
     * 
     * @param inLin Input in linear scale
     * @return
     */
    static int SKP_Silk_lin2log( final int inLin )    /* I:    Input in linear scale */
    {
        int lz, frac_Q7;

        int[] lz_ptr = new int[1];
        int[] frac_Q7_ptr = new int[1];
        
        Silk_Inlines.SKP_Silk_CLZ_FRAC( inLin, lz_ptr, frac_Q7_ptr );
        lz = lz_ptr[0];
        frac_Q7 = frac_Q7_ptr[0];
        
        /* Piece-wise parabolic approximation */
        return( Silk_SigProc_FIX.SKP_LSHIFT( 31 - lz, 7 ) + Silk_macros.SKP_SMLAWB( frac_Q7, Silk_SigProc_FIX.SKP_MUL( frac_Q7, 128 - frac_Q7 ), 179 ) );
    }
}
