/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 *  NLSF stabilizer:
 *  - Moves NLSFs further apart if they are too close                                                   
 *  - Moves NLSFs away from borders if they are too close    
 *    - High effort to achieve a modification with minimum Euclidean distance to input vector                   
 *    - Output are sorted NLSF coefficients   
 *  
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_NLSF_stabilize 
{
    /**
     * Constant Definitions.
     */
    static final int MAX_LOOPS  =      20;
    
    /**
     * NLSF stabilizer, for a single input data vector.
     * @param NLSF_Q15 Unstable/stabilized normalized LSF vector in Q15 [L].
     * @param NLSF_Q15_offset offset of valid data.
     * @param NDeltaMin_Q15 Normalized delta min vector in Q15, NDeltaMin_Q15[L] must be >= 1 [L+1].
     * @param L Number of NLSF parameters in the input vector.
     */
    static void SKP_Silk_NLSF_stabilize(
              int    []NLSF_Q15,            /* I/O:  Unstable/stabilized normalized LSF vector in Q15 [L]                    */
              int      NLSF_Q15_offset,
              int    []NDeltaMin_Q15,       /* I:    Normalized delta min vector in Q15, NDeltaMin_Q15[L] must be >= 1 [L+1] */
       final  int      L                    /* I:    Number of NLSF parameters in the input vector                           */
    )
    {
        int        center_freq_Q15, diff_Q15, min_center_Q15, max_center_Q15;
        int    min_diff_Q15;
        int        loops;
        int        i, I=0, k;

        /* This is necessary to ensure an output within range of a SKP_int16 */
        Silk_typedef.SKP_assert( NDeltaMin_Q15[L] >= 1 );

        for( loops = 0; loops < MAX_LOOPS; loops++ ) {
            /**************************/
            /* Find smallest distance */
            /**************************/
            /* First element */
            min_diff_Q15 = NLSF_Q15[ NLSF_Q15_offset + 0] - NDeltaMin_Q15[0];
            I = 0;
            /* Middle elements */
            for( i = 1; i <= L-1; i++ ) {
                diff_Q15 = NLSF_Q15[ NLSF_Q15_offset + i] - ( NLSF_Q15[ NLSF_Q15_offset + i-1] + NDeltaMin_Q15[i] );
                if( diff_Q15 < min_diff_Q15 ) {
                    min_diff_Q15 = diff_Q15;
                    I = i;
                }
            }
            /* Last element */
            diff_Q15 = (1<<15) - ( NLSF_Q15[ NLSF_Q15_offset + L-1] + NDeltaMin_Q15[L] );
            if( diff_Q15 < min_diff_Q15 ) {
                min_diff_Q15 = diff_Q15;
                I = L;
            }

            /***************************************************/
            /* Now check if the smallest distance non-negative */
            /***************************************************/
            if (min_diff_Q15 >= 0) {
                return;
            }

            if( I == 0 ) {
                /* Move away from lower limit */
                NLSF_Q15[ NLSF_Q15_offset + 0] = NDeltaMin_Q15[0];
            
            } else if( I == L) {
                /* Move away from higher limit */
                NLSF_Q15[ NLSF_Q15_offset + L-1] = (1<<15) - NDeltaMin_Q15[L];
            
            } else {
                /* Find the lower extreme for the location of the current center frequency */ 
                min_center_Q15 = 0;
                for( k = 0; k < I; k++ ) {
                    min_center_Q15 += NDeltaMin_Q15[k];
                }
                min_center_Q15 += ( NDeltaMin_Q15[I] >> 1 );

                
                /* Find the upper extreme for the location of the current center frequency */
                max_center_Q15 = (1<<15);
                for( k = L; k > I; k-- ) {
                    max_center_Q15 -= NDeltaMin_Q15[k];
                }
                max_center_Q15 -= ( NDeltaMin_Q15[I] - ( NDeltaMin_Q15[I] >> 1 ) );

                /* Move apart, sorted by value, keeping the same center frequency */
                center_freq_Q15 = Silk_SigProc_FIX.SKP_LIMIT_32( Silk_SigProc_FIX.SKP_RSHIFT_ROUND( NLSF_Q15[ NLSF_Q15_offset + I-1] + NLSF_Q15[ NLSF_Q15_offset + I], 1 ),
                        min_center_Q15, max_center_Q15 );
                NLSF_Q15[ NLSF_Q15_offset + I-1] = center_freq_Q15 - ( NDeltaMin_Q15[I] >> 1 );
                NLSF_Q15[ NLSF_Q15_offset + I] = NLSF_Q15[ NLSF_Q15_offset + I-1] + NDeltaMin_Q15[I];
            }
        }

        /* Safe and simple fall back method, which is less ideal than the above */
        if( loops == MAX_LOOPS )
        {
            /* Insertion sort (fast for already almost sorted arrays):   */
            /* Best case:  O(n)   for an already sorted array            */
            /* Worst case: O(n^2) for an inversely sorted array          */
            Silk_sort.SKP_Silk_insertion_sort_increasing_all_values(NLSF_Q15,  NLSF_Q15_offset + 0, L);
                
            /* First NLSF should be no less than NDeltaMin[0] */
            NLSF_Q15[ NLSF_Q15_offset + 0] = Silk_SigProc_FIX.SKP_max_int( NLSF_Q15[ NLSF_Q15_offset + 0], NDeltaMin_Q15[0] );
            
            /* Keep delta_min distance between the NLSFs */
            for( i = 1; i < L; i++ )
                NLSF_Q15[ NLSF_Q15_offset + i] = Silk_SigProc_FIX.SKP_max_int( NLSF_Q15[ NLSF_Q15_offset + i], NLSF_Q15[ NLSF_Q15_offset + i-1] + NDeltaMin_Q15[i] );

            /* Last NLSF should be no higher than 1 - NDeltaMin[L] */
            NLSF_Q15[ NLSF_Q15_offset + L-1] = Silk_SigProc_FIX.SKP_min_int( NLSF_Q15[ NLSF_Q15_offset + L-1], (1<<15) - NDeltaMin_Q15[L] );

            /* Keep NDeltaMin distance between the NLSFs */
            for( i = L-2; i >= 0; i-- ) 
                NLSF_Q15[ NLSF_Q15_offset + i] = Silk_SigProc_FIX.SKP_min_int( NLSF_Q15[ NLSF_Q15_offset + i], NLSF_Q15[ NLSF_Q15_offset + i+1] - NDeltaMin_Q15[i+1] );
        }
    }
    
    /**
     * NLSF stabilizer, over multiple input column data vectors.
     * @param NLSF_Q15 Unstable/stabilized normalized LSF vectors in Q15 [LxN].
     * @param NDeltaMin_Q15 Normalized delta min vector in Q15, NDeltaMin_Q15[L] must be >= 1 [L+1].
     * @param N Number of input vectors to be stabilized.
     * @param L NLSF vector dimension.
     */
    static void SKP_Silk_NLSF_stabilize_multi(
              int        []NLSF_Q15,        /* I/O:  Unstable/stabilized normalized LSF vectors in Q15 [LxN]                 */
              int        []NDeltaMin_Q15,   /* I:    Normalized delta min vector in Q15, NDeltaMin_Q15[L] must be >= 1 [L+1] */
        final int         N,               /* I:    Number of input vectors to be stabilized                                */
        final int         L                /* I:    NLSF vector dimension                                                   */
    )
    {
        int n;
        
        /* loop over input data */
        for( n = 0; n < N; n++ ) {
            SKP_Silk_NLSF_stabilize( NLSF_Q15, n * L, NDeltaMin_Q15, L );
        }
    }
}
