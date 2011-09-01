/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Convert input to a linear scale.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_log2lin
{
    /**
     * Approximation of 2^() (very close inverse of Silk_lin2log.SKP_Silk_lin2log())
     * Convert input to a linear scale.
     * 
     * @param inLog_Q7 Input on log scale
     * @return
     */
    static int SKP_Silk_log2lin( final int inLog_Q7 )    /* I:    Input on log scale */ 
    {
        int out, frac_Q7;

        if( inLog_Q7 < 0 ) {
            return 0;
        }

        out = ( 1 << ( inLog_Q7 >> 7 ) );
        
        frac_Q7 = inLog_Q7 & 0x7F;
        if( inLog_Q7 < 2048 ) {
            /* Piece-wise parabolic approximation */
            out = Silk_SigProc_FIX.SKP_ADD_RSHIFT( out, Silk_SigProc_FIX.SKP_MUL( out, Silk_macros.SKP_SMLAWB( frac_Q7, Silk_SigProc_FIX.SKP_MUL( frac_Q7, 128 - frac_Q7 ), -174 ) ), 7 );
        } else {
            /* Piece-wise parabolic approximation */
            out = Silk_SigProc_FIX.SKP_MLA( out, Silk_SigProc_FIX.SKP_RSHIFT( out, 7 ), Silk_macros.SKP_SMLAWB( frac_Q7, Silk_SigProc_FIX.SKP_MUL( frac_Q7, 128 - frac_Q7 ), -174 ) );
        }
        return out;
    }
}
