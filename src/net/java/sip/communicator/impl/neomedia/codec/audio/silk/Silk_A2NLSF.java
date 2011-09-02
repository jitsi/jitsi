/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;
             
class Silk_A2NLSF_constants
{
    /* Number of binary divisions, when not in low complexity mode */
    static final int BIN_DIV_STEPS_A2NLSF_FIX =     2; /* must be no higher than 16 - log2( LSF_COS_TAB_SZ_FIX ) */
    static final int QPoly =                       16;
    static final int MAX_ITERATIONS_A2NLSF_FIX =   50;

    /* Flag for using 2x as many cosine sampling points, reduces the risk of missing a root */
    static final int OVERSAMPLE_COSINE_TABLE =      0;
}

/**
 * Conversion between prediction filter coefficients and NLSFs.
 * Requires the order to be an even number.                     
 * A piecewise linear approximation maps LSF <-> cos(LSF). 
 * Therefore the result is not accurate NLSFs, but the two.    
 * function are accurate inverses of each other.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_A2NLSF
    extends Silk_A2NLSF_constants
{ 
    /**
     * Helper function for A2NLSF(..).                
     * Transforms polynomials from cos(n*f) to cos(f)^n.
     * @param p Polynomial
     * @param dd Polynomial order (= filter order / 2 )
     */
    static void SKP_Silk_A2NLSF_trans_poly(
        int[]        p,     /* I/O    Polynomial                                */
        final int    dd     /* I      Polynomial order (= filter order / 2 )    */
    )
    {
        int k, n;
        
        for( k = 2; k <= dd; k++ ) 
        {
            for( n = dd; n > k; n-- ) 
            {
                p[ n - 2 ] -= p[ n ];
            }
            p[ k - 2 ] -= p[ k ] << 1 ;
        }
    }    
     
    /**
     * Helper function for A2NLSF(..).           
     * Polynomial evaluation. 
     * @param p Polynomial, QPoly
     * @param x Evaluation point, Q12
     * @param dd  Order
     * @return return the polynomial evaluation, in QPoly
     */
    static int SKP_Silk_A2NLSF_eval_poly(    /* return the polynomial evaluation, in QPoly */
        int[]        p,    /* I    Polynomial, QPoly        */
        final int    x,    /* I    Evaluation point, Q12    */
        final int    dd    /* I    Order                    */
    )
    {
        int   n;
        int x_Q16, y32;

        y32 = p[ dd ];                                    /* QPoly */
        x_Q16 = x << 4;
        for( n = dd - 1; n >= 0; n-- ) 
        {
            y32 = Silk_macros.SKP_SMLAWW( p[ n ], y32, x_Q16 );       /* QPoly */
        }
        return y32;
    }

    static void SKP_Silk_A2NLSF_init(
         int[]    a_Q16,
         int[]            P, 
         int[]            Q, 
         final int        dd
    ) 
    {
        int k;

        /* Convert filter coefs to even and odd polynomials */
        P[dd] = 1 << QPoly;
        Q[dd] = 1 << QPoly;
        for( k = 0; k < dd; k++ ) 
        {
            if( QPoly < 16 )
            {
                P[ k ] = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( -a_Q16[ dd - k - 1 ] - a_Q16[ dd + k ], 16 - QPoly ); /* QPoly */
                Q[ k ] = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( -a_Q16[ dd - k - 1 ] + a_Q16[ dd + k ], 16 - QPoly ); /* QPoly */
            }
            else if( QPoly == 16 )
            {
                P[ k ] = -a_Q16[ dd - k - 1 ] - a_Q16[ dd + k ]; // QPoly
                Q[ k ] = -a_Q16[ dd - k - 1 ] + a_Q16[ dd + k ]; // QPoly
            }
            else
            {
                P[ k ] = ( -a_Q16[ dd - k - 1 ] - a_Q16[ dd + k ] ) << ( QPoly - 16 ); /* QPoly */
                Q[ k ] = ( -a_Q16[ dd - k - 1 ] + a_Q16[ dd + k ] ) << ( QPoly - 16 ); /* QPoly */
            }
        }

        /* Divide out zeros as we have that for even filter orders, */
        /* z =  1 is always a root in Q, and                        */
        /* z = -1 is always a root in P                             */
        for( k = dd; k > 0; k-- ) 
        {
            P[ k - 1 ] -= P[ k ]; 
            Q[ k - 1 ] += Q[ k ]; 
        }

        /* Transform polynomials from cos(n*f) to cos(f)^n */
        SKP_Silk_A2NLSF_trans_poly( P, dd );
        SKP_Silk_A2NLSF_trans_poly( Q, dd );
    }

    /**
     * Compute Normalized Line Spectral Frequencies (NLSFs) from whitening filter coefficients.
     * If not all roots are found, the a_Q16 coefficients are bandwidth expanded until convergence.
     * @param NLSF Normalized Line Spectral Frequencies, Q15 (0 - (2^15-1)), [d]
     * @param a_Q16 Monic whitening filter coefficients in Q16 [d]
     * @param d Filter order (must be even)
     */
    static void SKP_Silk_A2NLSF(
        int[]        NLSF,                 /* O    Normalized Line Spectral Frequencies, Q15 (0 - (2^15-1)), [d]    */
        int[]        a_Q16,                /* I/O  Monic whitening filter coefficients in Q16 [d]                   */
        final int    d                     /* I    Filter order (must be even)                                      */
    )
    {
        int      i, k, m, dd, root_ix, ffrac;
        int xlo, xhi, xmid;
        int ylo, yhi, ymid;
        int nom, den;
        int[] P = new int[ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC / 2 + 1 ];
        int[] Q = new int[ Silk_SigProc_FIX.SKP_Silk_MAX_ORDER_LPC / 2 + 1 ];
        int[][] PQ = new int[ 2 ][   ];
        int[] p;

        /* Store pointers to array */
        PQ[ 0 ] = P;
        PQ[ 1 ] = Q;

        dd =  d >> 1;

        SKP_Silk_A2NLSF_init( a_Q16, P, Q, dd );

        /* Find roots, alternating between P and Q */
        p = P;    /* Pointer to polynomial */
        
        xlo = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ 0 ]; // Q12
        ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );

        if( ylo < 0 ) 
        {
            /* Set the first NLSF to zero and move on to the next */
            NLSF[ 0 ] = 0;
            p = Q;                      /* Pointer to polynomial */
            ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );
            root_ix = 1;                /* Index of current root */
        }
        else
        {
            root_ix = 0;                /* Index of current root */
        }
        k = 1;                          /* Loop counter */
        i = 0;                          /* Counter for bandwidth expansions applied */
        while( true ) 
        {
            /* Evaluate polynomial */
            if(OVERSAMPLE_COSINE_TABLE!=0)
            {
                xhi = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[   k       >> 1 ] +
                  ( ( Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ ( k + 1 ) >> 1 ] - 
                      Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[   k       >> 1 ] ) >> 1 );    /* Q12 */
            }
            else
            {
                xhi = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ k ]; /* Q12 */
            }

            yhi = SKP_Silk_A2NLSF_eval_poly( p, xhi, dd );
            
            /* Detect zero crossing */
            if( ( ylo <= 0 && yhi >= 0 ) || ( ylo >= 0 && yhi <= 0 ) ) 
            {
                /* Binary division */
                if(OVERSAMPLE_COSINE_TABLE!=0)
                   ffrac = -128;
                else
                   ffrac = -256;
                
                for( m = 0; m < BIN_DIV_STEPS_A2NLSF_FIX; m++ ) 
                {
                    /* Evaluate polynomial */
                    xmid = Silk_SigProc_FIX.SKP_RSHIFT_ROUND( xlo + xhi, 1 );
                    ymid = SKP_Silk_A2NLSF_eval_poly( p, xmid, dd );

                    /* Detect zero crossing */
                    if( ( ylo <= 0 && ymid >= 0 ) || ( ylo >= 0 && ymid <= 0 ) ) 
                    {
                        /* Reduce frequency */
                        xhi = xmid;
                        yhi = ymid;
                    }
                    else
                    {
                        /* Increase frequency */
                        xlo = xmid;
                        ylo = ymid;
                        if(OVERSAMPLE_COSINE_TABLE!=0)
                            ffrac = ffrac + (64>>m);
                        else
                            ffrac = ffrac + (128>>m);
                    }
                }
                
                /* Interpolate */
                if( Math.abs( ylo ) < 65536 ) 
                {
                    /* Avoid dividing by zero */
                    den = ylo - yhi;
                    nom = ( ylo << ( 8 - BIN_DIV_STEPS_A2NLSF_FIX ) ) + ( den >> 1 );
                    if( den != 0 ) 
                    {
                        ffrac += nom / den;
                    }
                }
                else 
                {
                    /* No risk of dividing by zero because abs(ylo - yhi) >= abs(ylo) >= 65536 */
                    ffrac += ylo / ( ( ylo - yhi ) >> ( 8 - BIN_DIV_STEPS_A2NLSF_FIX ) );
                }
                if(OVERSAMPLE_COSINE_TABLE!=0)
                    NLSF[ root_ix ] = Math.min( ( k << 7 ) + ffrac, Silk_typedef.SKP_int16_MAX ); 
                else
                    NLSF[ root_ix ] = Math.min( ( k << 8 ) + ffrac, Silk_typedef.SKP_int16_MAX ); 

                assert( NLSF[ root_ix ] >=     0 );
                assert( NLSF[ root_ix ] <= 32767 );

                root_ix++;        /* Next root */
                if( root_ix >= d ) 
                {
                    /* Found all roots */
                    break;
                }
                /* Alternate pointer to polynomial */
                p = PQ[ root_ix & 1 ];
                
                /* Evaluate polynomial */
                if(OVERSAMPLE_COSINE_TABLE!=0)
                    xlo = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ ( k - 1 ) >> 1 ] +
                      ( ( Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[   k       >> 1 ] - 
                          Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ ( k - 1 ) >> 1 ] ) >> 1 ); // Q12
                else
                    xlo = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ k - 1 ]; // Q12

                ylo = ( 1 - ( root_ix & 2 ) ) << 12;
            } 
            else
            {
                /* Increment loop counter */
                k++;
                xlo    = xhi;
                ylo    = yhi;
                
                if (OVERSAMPLE_COSINE_TABLE != 0)
                {
                    if( k > 2 * Silk_SigProc_FIX.LSF_COS_TAB_SZ_FIX ) 
                    {
                        i++;
                        if( i > MAX_ITERATIONS_A2NLSF_FIX ) 
                        {
                            /* Set NLSFs to white spectrum and exit */
                            NLSF[ 0 ] = ( 1 << 15 ) / ( d + 1 );
                            for( k = 1; k < d; k++ ) 
                            {
                                NLSF[ k ] = Silk_macros.SKP_SMULBB( k + 1, NLSF[ 0 ] );
                            }
                            return;
                        }

                        /* Error: Apply progressively more bandwidth expansion and run again */
                        Silk_bwexpander_32.SKP_Silk_bwexpander_32( a_Q16, d, 65536 - Silk_macros.SKP_SMULBB( 66, i ) ); // 66_Q16 = 0.001

                        SKP_Silk_A2NLSF_init( a_Q16, P, Q, dd );
                        p = P;                            /* Pointer to polynomial */
                        xlo = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ 0 ]; // Q12
                        ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );
                        if( ylo < 0 ) 
                        {
                            /* Set the first NLSF to zero and move on to the next */
                            NLSF[ 0 ] = 0;
                            p = Q;                        /* Pointer to polynomial */
                            ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );
                            root_ix = 1;                /* Index of current root */
                        } 
                        else
                        {
                            root_ix = 0;                /* Index of current root */
                        }
                        k = 1;                            /* Reset loop counter */
                    }
                }
                else
                {
                    if( k > Silk_SigProc_FIX.LSF_COS_TAB_SZ_FIX ) 
                    {
                        i++;
                        if( i > MAX_ITERATIONS_A2NLSF_FIX ) 
                        {
                            /* Set NLSFs to white spectrum and exit */
                            NLSF[ 0 ] = ( 1 << 15 ) / ( d + 1 );
                            for( k = 1; k < d; k++ ) 
                            {
                                NLSF[ k ] = Silk_macros.SKP_SMULBB( k + 1, NLSF[ 0 ] );
                            }
                            return;
                        }

                        /* Error: Apply progressively more bandwidth expansion and run again */
                        Silk_bwexpander_32.SKP_Silk_bwexpander_32( a_Q16, d, 65536 - Silk_macros.SKP_SMULBB( 66, i ) ); // 66_Q16 = 0.001

                        SKP_Silk_A2NLSF_init( a_Q16, P, Q, dd );
                        p = P;                            /* Pointer to polynomial */
                        xlo = Silk_LSF_cos_table.SKP_Silk_LSFCosTab_FIX_Q12[ 0 ]; // Q12
                        ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );
                        if( ylo < 0 ) 
                        {
                            /* Set the first NLSF to zero and move on to the next */
                            NLSF[ 0 ] = 0;
                            p = Q;                        /* Pointer to polynomial */
                            ylo = SKP_Silk_A2NLSF_eval_poly( p, xlo, dd );
                            root_ix = 1;                /* Index of current root */
                        } 
                        else
                        {
                            root_ix = 0;                /* Index of current root */
                        }
                        k = 1;                            /* Reset loop counter */
                    }
                }
            }
        }
    }
}
