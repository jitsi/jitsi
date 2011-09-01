/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * copy and multiply a vector by a constant
 *
 * @author Dingxin Xu
 */
public class Silk_scale_copy_vector_FLP 
{
    /**
     * copy and multiply a vector by a constant.
     * @param data_out
     * @param data_out_offset
     * @param data_in
     * @param data_in_offset
     * @param gain
     * @param dataSize
     */
    static void SKP_Silk_scale_copy_vector_FLP( 
            float           []data_out, 
            int             data_out_offset,
            final float     []data_in, 
            int             data_in_offset,
            float           gain, 
            int             dataSize
    )
    {
        int  i, dataSize4;

        /* 4x unrolled loop */
        dataSize4 = dataSize & 0xFFFC;
        for( i = 0; i < dataSize4; i += 4 ) {
            data_out[ data_out_offset + i + 0 ] = gain * data_in[ data_in_offset + i + 0 ];
            data_out[ data_out_offset + i + 1 ] = gain * data_in[ data_in_offset + i + 1 ];
            data_out[ data_out_offset + i + 2 ] = gain * data_in[ data_in_offset + i + 2 ];
            data_out[ data_out_offset + i + 3 ] = gain * data_in[ data_in_offset + i + 3 ];
        }

        /* any remaining elements */
        for( ; i < dataSize; i++ ) {
            data_out[ data_out_offset + i ] = gain * data_in[ data_in_offset + i ];
        }
    }
}
