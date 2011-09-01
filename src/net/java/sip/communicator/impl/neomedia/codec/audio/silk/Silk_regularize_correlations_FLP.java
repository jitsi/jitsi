/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Dingxin Xu
 */
public class Silk_regularize_correlations_FLP
{
    /**
     * 
     * @param XX Correlation matrices
     * @param xx Correlation values
     * @param xx_offset offset of valid data.
     * @param noise Noise energy to add
     * @param D Dimension of XX
     */
   static void SKP_Silk_regularize_correlations_FLP(
        float                 []XX,                /* I/O  Correlation matrices                    */
        int                   XX_offset,
        float                 []xx,                /* I/O  Correlation values                      */
        int                   xx_offset,       
        float                 noise,              /* I    Noise energy to add                     */
        int                   D                   /* I    Dimension of XX                         */
   )
  {
      int i;

      for( i = 0; i < D; i++ ) {
          XX[XX_offset + i*D+i] += noise;
      }
      xx[ xx_offset ] += noise;
   }
}
