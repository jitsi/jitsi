/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Residual energy.
 * 
 * @author Dingxin Xu
 */
public class Silk_residual_energy_FLP
{


    static final int  MAX_ITERATIONS_RESIDUAL_NRG = 10;
    static final float  REGULARIZATION_FACTOR = 1e-8f;
    

    /**
     * Residual energy: nrg = wxx - 2 * wXx * c + c' * wXX * c.
     * @param c Filter coefficients
     * @param c_offset offset of valid data.
     * @param wXX Weighted correlation matrix, reg. out
     * @param wXX_offset offset of valid data.
     * @param wXx Weighted correlation vector
     * @param wxx Weighted correlation value
     * @param D Dimension 
     * @return Weighted residual energy
     */
    static float SKP_Silk_residual_energy_covar_FLP(           /* O    Weighted residual energy                */
        final float                 []c,                 /* I    Filter coefficients                     */
              int                   c_offset,
              float                 []wXX,               /* I/O  Weighted correlation matrix, reg. out   */
              int                   wXX_offset,
        final float                 []wXx,               /* I    Weighted correlation vector             */
        final float                 wxx,                /* I    Weighted correlation value              */
        final int                   D                   /* I    Dimension                               */
    )
    {
        int   i, j, k;
        float tmp, nrg = 0, regularization;

        /* Safety checks */
        assert( D >= 0 );

        regularization = REGULARIZATION_FACTOR * ( wXX[ wXX_offset + 0 ] + wXX[ wXX_offset + D * D - 1 ] );
        for( k = 0; k < MAX_ITERATIONS_RESIDUAL_NRG; k++ ) {
            nrg = wxx;

            tmp = 0.0f;
            for( i = 0; i < D; i++ ) {
                tmp += wXx[ i ] * c[ c_offset + i ];
            }
            nrg -= 2.0f * tmp;

            /* compute c' * wXX * c, assuming wXX is symmetric */
            for( i = 0; i < D; i++ ) {
                tmp = 0.0f;
                for( j = i + 1; j < D; j++ ) {
//                    tmp += matrix_c_ptr( wXX, i, j, D ) * c[ j ];
                    tmp += wXX[wXX_offset + i+j*D] * c[c_offset + j];
                }
//                nrg += c[ i ] * ( 2.0f * tmp + matrix_c_ptr( wXX, i, i, D ) * c[ i ] );
                nrg += c[ c_offset + i ] * ( 2.0f * tmp + wXX[wXX_offset + i + D*i] * c[ c_offset + i ] );
            }
            if( nrg > 0 ) {
                break;
            } else {
                /* Add white noise */
                for( i = 0; i < D; i++ ) {
//                    matrix_c_ptr( wXX, i, i, D ) +=  regularization;
                    wXX[wXX_offset + i + D*i] += regularization;
                }
                /* Increase noise for next run */
                regularization *= 2.0f;
            }
        }
        if( k == MAX_ITERATIONS_RESIDUAL_NRG ) {
            assert( nrg == 0 );
            nrg = 1.0f;
        }

        return nrg;
    }

    /**
     * Calculates residual energies of input subframes where all subframes have LPC_order
     * of preceeding samples 
     * @param nrgs Residual energy per subframe
     * @param x Input signal
     * @param a AR coefs for each frame half
     * @param gains Quantization gains
     * @param subfr_length Subframe length
     * @param LPC_order LPC order
     */
    static void SKP_Silk_residual_energy_FLP(  
              float nrgs[],                     /* O    Residual energy per subframe    */
        final float x[],                        /* I    Input signal                    */
        final float a[][ ],    /* I    AR coefs for each frame half    */
        final float gains[],                    /* I    Quantization gains              */
        final int   subfr_length,               /* I    Subframe length                 */
        final int   LPC_order                   /* I    LPC order                       */
    )
    {
        int         shift;
//        SKP_float       *LPC_res_ptr, LPC_res[ ( MAX_FRAME_LENGTH + NB_SUBFR * MAX_LPC_ORDER ) / 2 ];
        float       LPC_res_ptr[], LPC_res[] = new float[ ( Silk_define.MAX_FRAME_LENGTH + Silk_define.NB_SUBFR * Silk_define.MAX_LPC_ORDER ) / 2 ];

//        LPC_res_ptr = LPC_res + LPC_order;
        LPC_res_ptr = LPC_res;
        int LPC_res_ptr_offset = LPC_order;
        shift = LPC_order + subfr_length;

        /* Filter input to create the LPC residual for each frame half, and measure subframe energies */
        Silk_LPC_analysis_filter_FLP.SKP_Silk_LPC_analysis_filter_FLP( LPC_res, a[ 0 ], x, 0 + 0 * shift, 2 * shift, LPC_order );
        nrgs[ 0 ] = (float) ( gains[ 0 ] * gains[ 0 ] * Silk_energy_FLP.SKP_Silk_energy_FLP( LPC_res_ptr, LPC_res_ptr_offset + 0 * shift, subfr_length ) );
        nrgs[ 1 ] = (float) ( gains[ 1 ] * gains[ 1 ] * Silk_energy_FLP.SKP_Silk_energy_FLP( LPC_res_ptr, LPC_res_ptr_offset + 1 * shift, subfr_length ) );

        Silk_LPC_analysis_filter_FLP.SKP_Silk_LPC_analysis_filter_FLP( LPC_res, a[ 1 ], x, 0 + 2 * shift, 2 * shift, LPC_order );
        nrgs[ 2 ] = ( float )( gains[ 2 ] * gains[ 2 ] * Silk_energy_FLP.SKP_Silk_energy_FLP( LPC_res_ptr, LPC_res_ptr_offset + 0 * shift, subfr_length ) );
        nrgs[ 3 ] = ( float )( gains[ 3 ] * gains[ 3 ] * Silk_energy_FLP.SKP_Silk_energy_FLP( LPC_res_ptr, LPC_res_ptr_offset + 1 * shift, subfr_length ) );
    }
}
