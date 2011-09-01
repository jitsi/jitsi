/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * scale a vector.
 * 
 * @author Dingxin Xu
 */
public class Silk_scale_vector_FLP
{
    /**
     * multiply a vector by a constant.
     * @param data1 
     * @param gain
     * @param dataSize
     */
    static void SKP_Silk_scale_vector_FLP( 
        float           []data1, 
        int             data1_offset,
        float           gain, 
        int             dataSize
    )
    {
        int  i, dataSize4;

        /* 4x unrolled loop */
        dataSize4 = dataSize & 0xFFFC;
        for( i = 0; i < dataSize4; i += 4 ) {
            data1[ data1_offset + i + 0 ] *= gain;
            data1[ data1_offset + i + 1 ] *= gain;
            data1[ data1_offset + i + 2 ] *= gain;
            data1[ data1_offset + i + 3 ] *= gain;
        }

        /* any remaining elements */
        for( ; i < dataSize; i++ ) {
            data1[ data1_offset + i ] *= gain;
        }
    }
}
