/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Encode quantization indices of excitation.
 * 
 * @author Dingxin Xu
 */
public class Silk_encode_pulses 
{
    /**
     * 
     * @param pulses_comb
     * @param pulses_in
     * @param pulses_in_offset offset of valid data.
     * @param max_pulses max value for sum of pulses.
     * @param len number of output values.
     * @return
     */
    static int combine_and_check(       /* return ok */
        int         []pulses_comb,           /* O */
        final int   []pulses_in,             /* I */
        int         pulses_in_offset,
        int         max_pulses,             /* I    max value for sum of pulses */
        int         len                     /* I    number of output values */
    ) 
    {
        int k, sum;

        for( k = 0; k < len; k++ ) {
            sum = pulses_in[ pulses_in_offset + 2 * k ] + pulses_in[ pulses_in_offset+ 2 * k + 1 ];
            if( sum > max_pulses ) {
                return 1;
            }
            pulses_comb[ k ] = sum;
        }
        return 0;
    }

    /**
     * Encode quantization indices of excitation.
     * @param psRC Range coder state
     * @param sigtype Sigtype
     * @param QuantOffsetType QuantOffsetType
     * @param q quantization
     * @param frame_length Frame length
     */
    static void SKP_Silk_encode_pulses(
            SKP_Silk_range_coder_state  psRC,           /* I/O  Range coder state               */
            final int                   sigtype,        /* I    Sigtype                         */
            final int                   QuantOffsetType,/* I    QuantOffsetType                 */
            final byte                  q[],            /* I    quantization indices            */
            final int                   frame_length    /* I    Frame length                    */
    )
    {
        int   i, k, j, iter, bit, nLS, scale_down, RateLevelIndex = 0;
        int abs_q, minSumBits_Q6, sumBits_Q6;
        int[]   abs_pulses = new int[ Silk_define.MAX_FRAME_LENGTH ];
        int[]   sum_pulses = new int[ Silk_define.MAX_NB_SHELL_BLOCKS ];
        int[]   nRshifts   = new int[ Silk_define.MAX_NB_SHELL_BLOCKS ];
        int[]   pulses_comb = new int[ 8 ];
        int   []abs_pulses_ptr;
        int abs_pulses_ptr_offset;
        byte []pulses_ptr;
        int pulses_ptr_offset;
        final int [] cdf_ptr;
        short[] nBits_ptr;


        /****************************/
        /* Prepare for shell coding */
        /****************************/
        /* Calculate number of shell blocks */
        iter = frame_length / Silk_define.SHELL_CODEC_FRAME_LENGTH;
        
        /* Take the absolute value of the pulses */
        for( i = 0; i < frame_length; i+=4 ) {
            abs_pulses[i+0] = q[i+0] > 0 ? q[i+0] : (-q[i+0]);
            abs_pulses[i+1] = q[i+1] > 0 ? q[i+1] : (-q[i+1]);
            abs_pulses[i+2] = q[i+2] > 0 ? q[i+2] : (-q[i+2]);
            abs_pulses[i+3] = q[i+3] > 0 ? q[i+3] : (-q[i+3]);
        }

        /* Calc sum pulses per shell code frame */
        abs_pulses_ptr = abs_pulses;
        abs_pulses_ptr_offset = 0;
        for( i = 0; i < iter; i++ ) {
            nRshifts[ i ] = 0;

            while( true ) {
                /* 1+1 -> 2 */
                scale_down = combine_and_check( pulses_comb, abs_pulses_ptr, abs_pulses_ptr_offset, 
                        Silk_tables_pulses_per_block.SKP_Silk_max_pulses_table[ 0 ], 8 );

                /* 2+2 -> 4 */
                scale_down += combine_and_check( pulses_comb, pulses_comb, 0, 
                        Silk_tables_pulses_per_block.SKP_Silk_max_pulses_table[ 1 ], 4 );

                /* 4+4 -> 8 */
                scale_down += combine_and_check( pulses_comb, pulses_comb, 0, 
                        Silk_tables_pulses_per_block.SKP_Silk_max_pulses_table[ 2 ], 2 );

                /* 8+8 -> 16 */
                sum_pulses[ i ] = pulses_comb[ 0 ] + pulses_comb[ 1 ];
                if( sum_pulses[ i ] > Silk_tables_pulses_per_block.SKP_Silk_max_pulses_table[ 3 ] ) {
                    scale_down++;
                }

                if( scale_down !=0 ) {
                    /* We need to down scale the quantization signal */
                    nRshifts[ i ]++;                
                    for( k = 0; k < Silk_define.SHELL_CODEC_FRAME_LENGTH; k++ ) {
                        abs_pulses_ptr[ abs_pulses_ptr_offset + k ] = ( abs_pulses_ptr[ abs_pulses_ptr_offset + k ] >>1 );
                    }
                } else {
                    /* Jump out of while(1) loop and go to next shell coding frame */
                    break;
                }
            }
            abs_pulses_ptr_offset += Silk_define.SHELL_CODEC_FRAME_LENGTH;
        }

        /**************/
        /* Rate level */
        /**************/
        /* find rate level that leads to fewest bits for coding of pulses per block info */
        minSumBits_Q6 = Integer.MAX_VALUE;
        for( k = 0; k < Silk_define.N_RATE_LEVELS - 1; k++ ) {
            nBits_ptr  = Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_BITS_Q6[ k ];
            sumBits_Q6 = Silk_tables_pulses_per_block.SKP_Silk_rate_levels_BITS_Q6[sigtype][ k ];
            for( i = 0; i < iter; i++ ) {
                if( nRshifts[ i ] > 0 ) {
                    sumBits_Q6 += nBits_ptr[ Silk_define.MAX_PULSES + 1 ];
                } else {
                    sumBits_Q6 += nBits_ptr[ sum_pulses[ i ] ];
                }
            }
            if( sumBits_Q6 < minSumBits_Q6 ) {
                minSumBits_Q6 = sumBits_Q6;
                RateLevelIndex = k;
            }
        }
        Silk_range_coder.SKP_Silk_range_encoder( psRC, RateLevelIndex, 
                Silk_tables_pulses_per_block.SKP_Silk_rate_levels_CDF[ sigtype ], 0);

        /***************************************************/
        /* Sum-Weighted-Pulses Encoding                    */
        /***************************************************/
        cdf_ptr = Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF[ RateLevelIndex ];
        for( i = 0; i < iter; i++ ) {
            if( nRshifts[ i ] == 0 ) {
                Silk_range_coder.SKP_Silk_range_encoder( psRC, sum_pulses[ i ], cdf_ptr, 0);
            } else {
                Silk_range_coder.SKP_Silk_range_encoder( psRC, Silk_define.MAX_PULSES + 1, cdf_ptr, 0);
                for( k = 0; k < nRshifts[ i ] - 1; k++ ) {
                    Silk_range_coder.SKP_Silk_range_encoder( psRC, Silk_define.MAX_PULSES + 1, 
                            Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF[ Silk_define.N_RATE_LEVELS - 1 ], 0);
                }
                Silk_range_coder.SKP_Silk_range_encoder( psRC, sum_pulses[ i ], 
                        Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF[ Silk_define.N_RATE_LEVELS - 1 ], 0);
            }
        }

        /******************/
        /* Shell Encoding */
        /******************/
        for( i = 0; i < iter; i++ ) {
            if( sum_pulses[ i ] > 0 ) {
                Silk_shell_coder.SKP_Silk_shell_encoder( psRC, abs_pulses, i * Silk_define.SHELL_CODEC_FRAME_LENGTH);
            }
        }

        /****************/
        /* LSB Encoding */
        /****************/
        for( i = 0; i < iter; i++ ) {
            if( nRshifts[ i ] > 0 ) {
                pulses_ptr = q;
                pulses_ptr_offset = i * Silk_define.SHELL_CODEC_FRAME_LENGTH;
                nLS = nRshifts[ i ] - 1;
                for( k = 0; k < Silk_define.SHELL_CODEC_FRAME_LENGTH; k++ ) {
                    abs_q = pulses_ptr[pulses_ptr_offset + k] > 0 ? pulses_ptr[pulses_ptr_offset + k]: (-pulses_ptr[pulses_ptr_offset + k]);
                    for( j = nLS; j > 0; j-- ) {
                        bit = ( abs_q >> j ) & 1;
                        Silk_range_coder.SKP_Silk_range_encoder( psRC, bit, Silk_tables_other.SKP_Silk_lsb_CDF, 0);
                    }
                    bit = abs_q & 1;
                    Silk_range_coder.SKP_Silk_range_encoder( psRC, bit, Silk_tables_other.SKP_Silk_lsb_CDF, 0);
                }
            }
        }

        /****************/
        /* Encode signs */
        /****************/
        Silk_code_signs.SKP_Silk_encode_signs( psRC, q, frame_length, sigtype, QuantOffsetType, RateLevelIndex );
    }
}
