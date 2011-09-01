/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Correlation matrix computations for LS estimate.
 * 
 * @author Dingxin Xu
 */
public class Silk_corrMatrix_FLP
{
    /**
     * Calculates correlation vector X'*t.
     * @param x x vector [L+order-1] used to create X.
     * @param x_offset offset of valid data.
     * @param t Target vector [L].
     * @param t_offset offset of valid data.
     * @param L Length of vecors.
     * @param Order Max lag for correlation.
     * @param Xt X'*t correlation vector [order].
     */
    static void SKP_Silk_corrVector_FLP(
        final float                 []x,                 /* I    x vector [L+order-1] used to create X   */
        int                         x_offset,
        final float                 []t,                 /* I    Target vector [L]                       */
        int                         t_offset,
        final int                   L,                  /* I    Length of vecors                        */
        final int                   Order,              /* I    Max lag for correlation                 */
        final float                 []Xt                 /* O    X'*t correlation vector [order]         */
    )
    {
        int lag;
        final float []ptr1;
        int ptr1_offset;
        
        ptr1 = x; /* Points to first sample of column 0 of X: X[:,0] */
        ptr1_offset = x_offset + Order-1;
        for( lag = 0; lag < Order; lag++ ) {
            /* Calculate X[:,lag]'*t */
            Xt[lag] = (float) Silk_inner_product_FLP.SKP_Silk_inner_product_FLP(ptr1, ptr1_offset, t, t_offset, L);
            ptr1_offset--;                                 /* Next column of X */
        }   
    }

    /**
     * Calculates correlation matrix X'*X.
     * @param x x vector [ L+order-1 ] used to create X.
     * @param x_offset offset of valid data.
     * @param L Length of vectors.
     * @param Order Max lag for correlation.
     * @param XX X'*X correlation matrix [order x order].
     * @param XX_offset offset of valid data.
     */
    static void SKP_Silk_corrMatrix_FLP(
        final float                 []x,                 /* I    x vector [ L+order-1 ] used to create X */
              int                   x_offset,
        final int                   L,                  /* I    Length of vectors                       */
        final int                   Order,              /* I    Max lag for correlation                 */
              float                 []XX,                 /* O    X'*X correlation matrix [order x order] */
              int                   XX_offset
    )
    {
        int j, lag;
        double  energy;
        final float ptr1[], ptr2[];
        int ptr1_offset, ptr2_offset;

        ptr1 = x; /* First sample of column 0 of X */
        ptr1_offset = x_offset + Order-1;
        
        energy = Silk_energy_FLP.SKP_Silk_energy_FLP(ptr1, ptr1_offset, L);/* X[:,0]'*X[:,0] */
        
//        matrix_ptr( XX, 0, 0, Order ) = ( SKP_float )energy;
        XX[XX_offset + 0]  = (float) energy;
        
        for( j = 1; j < Order; j++ ) {
            /* Calculate X[:,j]'*X[:,j] */
            energy += ptr1[ ptr1_offset -j ] * ptr1[ ptr1_offset-j ] - ptr1[ ptr1_offset + L - j ] * ptr1[ ptr1_offset + L - j ];
//            matrix_ptr( XX, j, j, Order ) = ( SKP_float )energy;
            XX[XX_offset + j*Order+j] = (float) energy;
        }
     
        ptr2 = x;                     /* First sample of column 1 of X */
        ptr2_offset = x_offset + Order - 2;
        for( lag = 1; lag < Order; lag++ ) {
            /* Calculate X[:,0]'*X[:,lag] */
//            matrix_ptr( XX, lag, 0, Order ) = ( SKP_float )energy;
//            matrix_ptr( XX, 0, lag, Order ) = ( SKP_float )energy;
            energy = Silk_inner_product_FLP.SKP_Silk_inner_product_FLP(ptr1, ptr1_offset, ptr2, ptr2_offset, L);
            /* Calculate X[:,j]'*X[:,j + lag] */
            for( j = 1; j < ( Order - lag ); j++ ) {
//                matrix_ptr( XX, lag + j, j, Order ) = ( SKP_float )energy;
//                matrix_ptr( XX, j, lag + j, Order ) = ( SKP_float )energy;
                energy += ptr1[ ptr1_offset-j ] * ptr2[ ptr2_offset-j ] - ptr1[ ptr1_offset + L - j ] * ptr2[ ptr2_offset + L - j ];
                XX[XX_offset + (lag+j)*Order + j] = (float) energy;
                XX[XX_offset + j*Order + (lag+j)] = (float) energy;
            }
            ptr2_offset--;                                 /* Next column of X */
        }
    }
}
