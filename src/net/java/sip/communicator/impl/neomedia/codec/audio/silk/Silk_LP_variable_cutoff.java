/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Elliptic/Cauer filters designed with 0.1 dB passband ripple, 
 * 80 dB minimum stopband attenuation, and
 * [0.95 : 0.15 : 0.35] normalized cut off frequencies.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_LP_variable_cutoff 
{
    /**
     * Helper function, that interpolates the filter taps.
     * @param B_Q28
     * @param A_Q28
     * @param ind
     * @param fac_Q16
     */
    static void SKP_Silk_LP_interpolate_filter_taps( 
        int[] B_Q28,
        int[] A_Q28,
        final int     ind,
        final int     fac_Q16
    )
    {
        int nb, na;

        if( ind < Silk_define.TRANSITION_INT_NUM - 1 ) 
        {
            if( fac_Q16 > 0 ) 
            {
                if( fac_Q16 == Silk_SigProc_FIX.SKP_SAT16( fac_Q16 ) ) 
                { /* fac_Q16 is in range of a 16-bit int */
                    /* Piece-wise linear interpolation of B and A */
                    for( nb = 0; nb < Silk_define.TRANSITION_NB; nb++ ) 
                    {
                        B_Q28[ nb ] = Silk_macros.SKP_SMLAWB(
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind     ][ nb ],
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind + 1 ][ nb ] -
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind     ][ nb ],
                            fac_Q16 );
                    }
                    for( na = 0; na < Silk_define.TRANSITION_NA; na++ ) 
                    {
                        A_Q28[ na ] = Silk_macros.SKP_SMLAWB(
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind     ][ na ],
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind + 1 ][ na ] -
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind     ][ na ],
                            fac_Q16 );
                    }
                } 
                else if( fac_Q16 == ( 1 << 15 ) )
                { /* Neither fac_Q16 nor ( ( 1 << 16 ) - fac_Q16 ) is in range of a 16-bit int */

                    /* Piece-wise linear interpolation of B and A */
                    for( nb = 0; nb < Silk_define.TRANSITION_NB; nb++ ) 
                    {
                        B_Q28[ nb ] = Silk_SigProc_FIX.SKP_RSHIFT( 
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind     ][ nb ] +
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind + 1 ][ nb ],
                            1 );
                    }
                    for( na = 0; na < Silk_define.TRANSITION_NA; na++ ) 
                    {
                        A_Q28[ na ] = Silk_SigProc_FIX.SKP_RSHIFT( 
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind     ][ na ] + 
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind + 1 ][ na ], 
                            1 );
                    }
                }
                else 
                { /* ( ( 1 << 16 ) - fac_Q16 ) is in range of a 16-bit int */
                    
                    assert( ( ( 1 << 16 ) - fac_Q16 ) == Silk_SigProc_FIX.SKP_SAT16( ( ( 1 << 16 ) - fac_Q16) ) );
                    /* Piece-wise linear interpolation of B and A */
                    for( nb = 0; nb < Silk_define.TRANSITION_NB; nb++ ) 
                    {
                        B_Q28[ nb ] = Silk_macros.SKP_SMLAWB(
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind + 1 ][ nb ],
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind     ][ nb ] -
                            Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind + 1 ][ nb ],
                            ( 1 << 16 ) - fac_Q16 );
                    }
                    for( na = 0; na < Silk_define.TRANSITION_NA; na++ ) 
                    {
                        A_Q28[ na ] = Silk_macros.SKP_SMLAWB(
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind + 1 ][ na ],
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind     ][ na ] -
                            Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind + 1 ][ na ],
                            ( 1 << 16 ) - fac_Q16 );
                    }
                }
            } 
            else 
            {
                for(int i_djinn=0; i_djinn<Silk_define.TRANSITION_NB; i_djinn++)
                    B_Q28[i_djinn] = Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ ind ][i_djinn];
                for(int i_djinn=0; i_djinn<Silk_define.TRANSITION_NA; i_djinn++)
                    A_Q28[i_djinn] = Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ ind ][i_djinn];
            }
        } 
        else 
        {
            for(int i_djinn=0; i_djinn<Silk_define.TRANSITION_NB; i_djinn++)
                B_Q28[i_djinn] = Silk_tables_other.SKP_Silk_Transition_LP_B_Q28[ Silk_define.TRANSITION_INT_NUM - 1 ][i_djinn];
            for(int i_djinn=0; i_djinn<Silk_define.TRANSITION_NA; i_djinn++)
                A_Q28[i_djinn] = Silk_tables_other.SKP_Silk_Transition_LP_A_Q28[ Silk_define.TRANSITION_INT_NUM - 1 ][i_djinn];
        }
    }
    
    /**
     * Low-pass filter with variable cutoff frequency based on  
     * piece-wise linear interpolation between elliptic filters 
     * Start by setting psEncC->transition_frame_no = 1;            
     * Deactivate by setting psEncC->transition_frame_no = 0;  
     * @param psLP  LP filter state
     * @param out Low-pass filtered output signal
     * @param out_offset offset of valid data.
     * @param in Input signal 
     * @param in_offset offset of valid data.
     * @param frame_length Frame length
     */
    static void SKP_Silk_LP_variable_cutoff(
        SKP_Silk_LP_state               psLP,          /* I/O  LP filter state                     */
        short[]                         out,           /* O    Low-pass filtered output signal     */
        int out_offset,
        short[]                         in,            /* I    Input signal                        */
        int in_offset,
        final int                       frame_length    /* I    Frame length                        */
    )
    {
        int[]   B_Q28 = new int[ Silk_define.TRANSITION_NB ], A_Q28 = new int[ Silk_define.TRANSITION_NA ]; 
        int fac_Q16 = 0;
        int     ind = 0;

        assert( psLP.transition_frame_no >= 0 );
        assert( ( ( ( psLP.transition_frame_no <= Silk_define.TRANSITION_FRAMES_DOWN ) && ( psLP.mode == 0 ) ) || 
                      ( ( psLP.transition_frame_no <= Silk_define.TRANSITION_FRAMES_UP   ) && ( psLP.mode == 1 ) ) ) );

        /* Interpolate filter coefficients if needed */
        if( psLP.transition_frame_no > 0 ) 
        {
            if( psLP.mode == 0 ) 
            {
                if( psLP.transition_frame_no < Silk_define.TRANSITION_FRAMES_DOWN ) 
                {
                    /* Calculate index and interpolation factor for interpolation */
                    if( Silk_define.TRANSITION_INT_STEPS_DOWN == 32 )
                    fac_Q16 = psLP.transition_frame_no << ( 16 - 5 );
                    else
                    fac_Q16 = ( psLP.transition_frame_no << 16 ) / Silk_define.TRANSITION_INT_STEPS_DOWN ;
        
                    ind      = fac_Q16 >> 16;
                    fac_Q16 -= ind << 16;

                    assert( ind >= 0 );
                    assert( ind < Silk_define.TRANSITION_INT_NUM );

                    /* Interpolate filter coefficients */
                    SKP_Silk_LP_interpolate_filter_taps( B_Q28, A_Q28, ind, fac_Q16 );

                    /* Increment transition frame number for next frame */
                    psLP.transition_frame_no++;

                } 
                else if( psLP.transition_frame_no == Silk_define.TRANSITION_FRAMES_DOWN ) 
                {
                    /* End of transition phase */
                    SKP_Silk_LP_interpolate_filter_taps( B_Q28, A_Q28, Silk_define.TRANSITION_INT_NUM - 1, 0 );
                }
            } 
            else if( psLP.mode == 1 ) 
            {
                if( psLP.transition_frame_no < Silk_define.TRANSITION_FRAMES_UP ) 
                {
                    /* Calculate index and interpolation factor for interpolation */
                    if( Silk_define.TRANSITION_INT_STEPS_UP == 64 )
                    fac_Q16 = ( Silk_define.TRANSITION_FRAMES_UP - psLP.transition_frame_no ) << ( 16 - 6 );
                    else
                    fac_Q16 = ( ( Silk_define.TRANSITION_FRAMES_UP - psLP.transition_frame_no ) << 16 ) / Silk_define.TRANSITION_INT_STEPS_UP;

                    ind      = fac_Q16 >> 16;
                    fac_Q16 -= ind << 16;

                    assert( ind >= 0 );
                    assert( ind < Silk_define.TRANSITION_INT_NUM );

                    /* Interpolate filter coefficients */
                    SKP_Silk_LP_interpolate_filter_taps( B_Q28, A_Q28, ind, fac_Q16 );

                    /* Increment transition frame number for next frame */
                    psLP.transition_frame_no++;
                
                } 
                else if( psLP.transition_frame_no == Silk_define.TRANSITION_FRAMES_UP ) 
                {
                    /* End of transition phase */
                    SKP_Silk_LP_interpolate_filter_taps( B_Q28, A_Q28, 0, 0 );
                }
            }
        } 
        
        if( psLP.transition_frame_no > 0 ) 
        {
            /* ARMA low-pass filtering */
            assert( Silk_define.TRANSITION_NB == 3 && Silk_define.TRANSITION_NA == 2 );
            Silk_biquad_alt.SKP_Silk_biquad_alt( in,in_offset, B_Q28, A_Q28, psLP.In_LP_State, out,out_offset, frame_length );
        }
        else 
        {
            /* Instead of using the filter, copy input directly to output */
            for(int i_djinn=0; i_djinn<frame_length; i_djinn++)
                out[out_offset+i_djinn] = in[in_offset+i_djinn];
        }
    }
}
