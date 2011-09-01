/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Interpolate two vectors.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_interpolate 
{
    /**
     * Interpolate two vectors.
     * 
     * @param xi interpolated vector.
     * @param x0 first vector.
     * @param x1 second vector.
     * @param ifact_Q2 interp. factor, weight on 2nd vector.
     * @param d number of parameters.
     */
    static void SKP_Silk_interpolate(
        int[] xi,                                             /* O    interpolated vector                     */
        int[] x0,                                             /* I    first vector                            */
        int[] x1,                                             /* I    second vector                           */
        final int                   ifact_Q2,               /* I    interp. factor, weight on 2nd vector    */
        final int                   d                       /* I    number of parameters                    */
    )
    {
        int i;

        assert( ifact_Q2 >= 0 );
        assert( ifact_Q2 <= ( 1 << 2 ) );

        for( i = 0; i < d; i++ ) 
        {
            xi[ i ] = ( int )( ( int )x0[ i ] + ( ( ( int )x1[ i ] - ( int )x0[ i ] ) * ifact_Q2 >> 2 ) );
        }
    }
}
