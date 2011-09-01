/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Decode quantization indices of excitation.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_decode_pulses
{
    /**
     * Decode quantization indices of excitation.
     * @param psRC Range coder state.
     * @param psDecCtrl Decoder control.
     * @param q Excitation signal.
     * @param frame_length Frame length (preliminary).
     */
    static void SKP_Silk_decode_pulses(
            SKP_Silk_range_coder_state      psRC,              /* I/O  Range coder state                           */
            SKP_Silk_decoder_control        psDecCtrl,         /* I/O  Decoder control                             */
            int                             q[],               /* O    Excitation signal                           */
            final int                       frame_length       /* I    Frame length (preliminary)                  */
    )
    {
        int   i, j, k, iter, abs_q, nLS, bit;
        int[]   sum_pulses = new int[ Silk_define.MAX_NB_SHELL_BLOCKS ];
        int[]   nLshifts = new int[ Silk_define.MAX_NB_SHELL_BLOCKS ];
        int[]   pulses_ptr;
        int     pulses_ptr_offset;
        int[]   cdf_ptr;
        
        /*********************/
        /* Decode rate level */
        /*********************/
        int RateLevelIndex_ptr[] = new int[1];
        RateLevelIndex_ptr[0] = psDecCtrl.RateLevelIndex;
        Silk_range_coder.SKP_Silk_range_decoder( RateLevelIndex_ptr, 0, psRC, 
                Silk_tables_pulses_per_block.SKP_Silk_rate_levels_CDF[ psDecCtrl.sigtype ], 0, Silk_tables_pulses_per_block.SKP_Silk_rate_levels_CDF_offset );
        psDecCtrl.RateLevelIndex = RateLevelIndex_ptr[0];
        
        /* Calculate number of shell blocks */
        iter = frame_length / Silk_define.SHELL_CODEC_FRAME_LENGTH;
        
        /***************************************************/
        /* Sum-Weighted-Pulses Decoding                    */
        /***************************************************/
        cdf_ptr = Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF[ psDecCtrl.RateLevelIndex ];
        
        for( i = 0; i < iter; i++ ) {
            nLshifts[ i ] = 0;
            Silk_range_coder.SKP_Silk_range_decoder( sum_pulses, i, psRC, cdf_ptr, 0, Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF_offset );

            /* LSB indication */
            while( sum_pulses[ i ] == ( Silk_define.MAX_PULSES + 1 ) ) {
                nLshifts[ i ]++;
                Silk_range_coder.SKP_Silk_range_decoder( sum_pulses, i, psRC, 
                        Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF[ Silk_define.N_RATE_LEVELS - 1 ], 0, Silk_tables_pulses_per_block.SKP_Silk_pulses_per_block_CDF_offset );
            }
        }
        
        /***************************************************/
        /* Shell decoding                                  */
        /***************************************************/
        for( i = 0; i < iter; i++ ) {
            if( sum_pulses[ i ] > 0 ) {
                Silk_shell_coder.SKP_Silk_shell_decoder( q, Silk_macros.SKP_SMULBB( i, Silk_define.SHELL_CODEC_FRAME_LENGTH ), psRC, sum_pulses[ i ] );
            } else {
                Arrays.fill(q, (Silk_macros.SKP_SMULBB(i, Silk_define.SHELL_CODEC_FRAME_LENGTH)),
                        ((Silk_macros.SKP_SMULBB(i, Silk_define.SHELL_CODEC_FRAME_LENGTH)) + Silk_define.SHELL_CODEC_FRAME_LENGTH), 0);
            }
        }

        /***************************************************/
        /* LSB Decoding                                    */
        /***************************************************/
        for( i = 0; i < iter; i++ ) {
            if( nLshifts[ i ] > 0 ) {
                nLS = nLshifts[ i ];
                pulses_ptr = q;
                pulses_ptr_offset = Silk_macros.SKP_SMULBB(i, Silk_define.SHELL_CODEC_FRAME_LENGTH);
                
                for( k = 0; k < Silk_define.SHELL_CODEC_FRAME_LENGTH; k++ ) {
                    abs_q = pulses_ptr[pulses_ptr_offset + k];
                    for( j = 0; j < nLS; j++ ) {
                        abs_q = abs_q << 1;
                        int bit_ptr[] = new int[1];
                        Silk_range_coder.SKP_Silk_range_decoder( bit_ptr, 0, psRC, Silk_tables_other.SKP_Silk_lsb_CDF, 0, 1 );
                        bit = bit_ptr[0];
                        abs_q += bit;
                    }
                    pulses_ptr[pulses_ptr_offset + k] = abs_q;
                }
            }
        }

        /****************************************/
        /* Decode and add signs to pulse signal */
        /****************************************/
        Silk_code_signs.SKP_Silk_decode_signs( psRC, q, frame_length, psDecCtrl.sigtype, 
                psDecCtrl.QuantOffsetType, psDecCtrl.RateLevelIndex);
    }
}
