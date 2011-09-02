/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * compute number of bits to right shift the sum of squares of a vector 
 * of int16s to make it fit in an int32
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_sum_sqr_shift 
{
    /**
     * Compute number of bits to right shift the sum of squares of a vector
     * of int16s to make it fit in an int32.
     * @param energy Energy of x, after shifting to the right.
     * @param shift Number of bits right shift applied to energy.
     * @param x Input vector.
     * @param x_offset offset of valid data.
     * @param len Length of input vector.
     */
    static void SKP_Silk_sum_sqr_shift(
            int       []energy,            /* O    Energy of x, after shifting to the right            */
            int       []shift,             /* O    Number of bits right shift applied to energy        */
            short     []x,                 /* I    Input vector                                        */
            int       x_offset,
            int       len                 /* I    Length of input vector                              */
        )
    {
        int   i, shft;
        int in32, nrg_tmp, nrg;
//TODO:
//        if( (int)( (SKP_int_ptr_size)x & 2 ) != 0 ) {   
        if( false ) {
            /* Input is not 4-byte aligned */
            nrg = Silk_macros.SKP_SMULBB( x[ x_offset + 0 ], x[x_offset + 0 ] );
            i = 1;
        } else {
            nrg = 0;
            i   = 0;
        }
        shft = 0;
        len--;
        while( i < len ) {
            /* Load two values at once */
            in32 = x[ x_offset + i];
            
            nrg = Silk_SigProc_FIX.SKP_SMLABB_ovflw( nrg, in32, in32 );
            nrg = Silk_SigProc_FIX.SKP_SMLATT_ovflw( nrg, in32, in32 );
            i += 2;
            if( nrg < 0 ) {
                /* Scale down */
//                nrg = (int)SKP_RSHIFT_uint( (SKP_uint32)nrg, 2 );
//TODO:                
//                nrg = (int)( ((long)nrg)&0xFFFFFFFFL >>> 2 );
                nrg = (int)( ((nrg)&0xFFFFFFFFL) >>> 2 );
                shft = 2;
                break;
            }
        }
        for( ; i < len; i += 2 ) {
            /* Load two values at once */
            in32 =  x[x_offset + i];
            nrg_tmp = Silk_macros.SKP_SMULBB( in32, in32 );
            nrg_tmp = Silk_SigProc_FIX.SKP_SMLATT_ovflw( nrg_tmp, in32, in32 );
// TODO: ???       
//            nrg = (int)SKP_ADD_RSHIFT_uint( nrg, (SKP_uint32)nrg_tmp, shft );
            nrg = nrg + (nrg_tmp>>>shft);
//or            nrg = (int)( nrg + (((long)nrg_tmp)&0xFFFFFFFFL) >>>  shft );

            if( nrg < 0 ) {
                /* Scale down */
                nrg = ( nrg >>> 2 );
                shft += 2;
            }
        }
        if( i == len ) {
            /* One sample left to process */
            nrg_tmp = Silk_macros.SKP_SMULBB( x[ x_offset + i ], x[ x_offset + i ] );
//TODO:            
//            nrg = (int)SKP_ADD_RSHIFT_uint( nrg, nrg_tmp, shft );
            nrg = ( nrg + (nrg_tmp >>> shft) );
        }

        /* Make sure to have at least one extra leading zero (two leading zeros in total) */
        if( (nrg & 0xC0000000) != 0 ) {
            nrg = (  nrg >>> 2 );
            shft += 2;
        }

        /* Output arguments */
        shift[0]  = shft;
        energy[0] = nrg;
    }
}
