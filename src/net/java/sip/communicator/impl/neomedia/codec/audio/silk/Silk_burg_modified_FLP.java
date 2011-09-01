/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Calculates the reflection coefficients from the input vector
 * Input vector contains nb_subfr sub vectors of length L_sub + D
 * 
 * @author Dingxin Xu
 */
public class Silk_burg_modified_FLP 
{
    static final int MAX_FRAME_SIZE = 544; // subfr_length * nb_subfr = ( 0.005 * 24000 + 16 ) * 4 = 544
    static final int MAX_NB_SUBFR = 4;

    /**
     * Compute reflection coefficients from input signal.
     * @param A prediction coefficients (length order).
     * @param x input signal, length: nb_subfr*(D+L_sub).
     * @param x_offset offset of valid data.
     * @param subfr_length input signal subframe length (including D preceeding samples).
     * @param nb_subfr number of subframes stacked in x.
     * @param WhiteNoiseFrac fraction added to zero-lag autocorrelation.
     * @param D order.
     * @return
     */
    static float SKP_Silk_burg_modified_FLP(     /* O    returns residual energy                                         */
            float       A[],                /* O    prediction coefficients (length order)                          */
            final float x[],                /* I    input signal, length: nb_subfr*(D+L_sub)                        */
            int         x_offset,
            final int   subfr_length,       /* I    input signal subframe length (including D preceeding samples)   */
            final int   nb_subfr,           /* I    number of subframes stacked in x                                */
            final float WhiteNoiseFrac,     /* I    fraction added to zero-lag autocorrelation                      */
            final int   D                   /* I    order                                                           */
    )
    {
        int         k, n, s;
        double          C0, num, nrg_f, nrg_b, rc, Atmp, tmp1, tmp2;
        float []x_ptr;
        int x_ptr_offset;
        double          C_first_row[] = new double [ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC ], 
                        C_last_row[]  = new double [ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC ];
        double          CAf[] = new double [ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC + 1 ], 
                        CAb[] = new double [ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC + 1 ];
        double          Af[] = new double [ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC ];

        assert( subfr_length * nb_subfr <= MAX_FRAME_SIZE );
        assert( nb_subfr <= MAX_NB_SUBFR );

        /* Compute autocorrelations, added over subframes */
        C0 = Silk_energy_FLP.SKP_Silk_energy_FLP( x, x_offset, nb_subfr * subfr_length );
        for( s = 0; s < nb_subfr; s++ ) {
            x_ptr = x;
            x_ptr_offset = x_offset + s * subfr_length;
            for( n = 1; n < D + 1; n++ ) {
                C_first_row[ n - 1 ] += Silk_inner_product_FLP.SKP_Silk_inner_product_FLP( x_ptr, x_ptr_offset, 
                        x_ptr, x_ptr_offset + n, subfr_length - n );
            }
        }
        System.arraycopy(C_first_row, 0, C_last_row, 0, Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC);

        /* Initialize */
        CAb[ 0 ] = CAf[ 0 ] = C0 + WhiteNoiseFrac * C0 + 1e-9f;

        for( n = 0; n < D; n++ ) {
            /* Update first row of correlation matrix (without first element) */
            /* Update last row of correlation matrix (without last element, stored in reversed order) */
            /* Update C * Af */
            /* Update C * flipud(Af) (stored in reversed order) */
            for( s = 0; s < nb_subfr; s++ ) {
                x_ptr = x;
                x_ptr_offset = x_offset + s * subfr_length;
                tmp1 = x_ptr[ x_ptr_offset + n ];
                tmp2 = x_ptr[ x_ptr_offset + subfr_length - n - 1 ];
                for( k = 0; k < n; k++ ) {
                    C_first_row[ k ] -= x_ptr[ x_ptr_offset + n ] * x_ptr[ x_ptr_offset + n - k - 1 ];
                    C_last_row[ k ]  -= x_ptr[ x_ptr_offset + subfr_length - n - 1 ] * x_ptr[ x_ptr_offset + subfr_length - n + k ];
                    Atmp = Af[ k ];
                    tmp1 += x_ptr[ x_ptr_offset + n - k - 1 ] * Atmp;
                    tmp2 += x_ptr[ x_ptr_offset + subfr_length - n + k ] * Atmp;
                }
                for( k = 0; k <= n; k++ ) {
                    CAf[ k ] -= tmp1 * x_ptr[ x_ptr_offset + n - k ];
                    CAb[ k ] -= tmp2 * x_ptr[ x_ptr_offset + subfr_length - n + k - 1 ];
                }
            }
            tmp1 = C_first_row[ n ];
            tmp2 = C_last_row[ n ];
            for( k = 0; k < n; k++ ) {
                Atmp = Af[ k ];
                tmp1 += C_last_row[ n - k - 1 ]  * Atmp;
                tmp2 += C_first_row[ n - k - 1 ] * Atmp;
            }
            CAf[ n + 1 ] = tmp1;
            CAb[ n + 1 ] = tmp2;

            /* Calculate nominator and denominator for the next order reflection (parcor) coefficient */
            num = CAb[ n + 1 ];
            nrg_b = CAb[ 0 ];
            nrg_f = CAf[ 0 ];
            for( k = 0; k < n; k++ ) {
                Atmp = Af[ k ];
                num   += CAb[ n - k ] * Atmp;
                nrg_b += CAb[ k + 1 ] * Atmp;
                nrg_f += CAf[ k + 1 ] * Atmp;
            }
            assert( nrg_f > 0.0 );
            assert( nrg_b > 0.0 );

            /* Calculate the next order reflection (parcor) coefficient */
            rc = -2.0 * num / ( nrg_f + nrg_b );
            assert( rc > -1.0 && rc < 1.0 );

            /* Update the AR coefficients */
            for( k = 0; k < (n + 1) >> 1; k++ ) {
                tmp1 = Af[ k ];
                tmp2 = Af[ n - k - 1 ];
                Af[ k ]         = tmp1 + rc * tmp2;
                Af[ n - k - 1 ] = tmp2 + rc * tmp1;
            }
            Af[ n ] = rc;

            /* Update C * Af and C * Ab */
            for( k = 0; k <= n + 1; k++ ) {
                tmp1 = CAf[ k ];
                CAf[ k ]          += rc * CAb[ n - k + 1 ];
                CAb[ n - k + 1  ] += rc * tmp1;
            }
        }

        /* Return residual energy */
        nrg_f = CAf[ 0 ];
        tmp1 = 1.0;
        for( k = 0; k < D; k++ ) {
            Atmp = Af[ k ];
            nrg_f += CAf[ k + 1 ] * Atmp;
            tmp1  += Atmp * Atmp;
            A[ k ] = (float)(-Atmp);
        }
        nrg_f -= WhiteNoiseFrac * C0 * tmp1;

        return (float)nrg_f;
    }
}
