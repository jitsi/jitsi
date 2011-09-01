/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * inner product of two SKP_float arrays, with result as double.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_inner_product_FLP 
{
    /**
     * inner product of two SKP_float arrays, with result as double.
     * @param data1 vector1.
     * @param data1_offset offset of valid data.
     * @param data2 vector2.
     * @param data2_offset offset of valid data.
     * @param dataSize length of vectors.
     * @return result.
     */
    static double SKP_Silk_inner_product_FLP(    /* O    result              */
        float[]     data1,         /* I    vector 1            */
        int data1_offset,
        float[]     data2,         /* I    vector 2            */
        int data2_offset,
        int         dataSize       /* I    length of vectors   */
    )
    {
        int  i, dataSize4;
        double   result;

        /* 4x unrolled loop */
        result = 0.0f;
        dataSize4 = dataSize & 0xFFFC;
        for( i = 0; i < dataSize4; i += 4 ) 
        {
            result += data1[ data1_offset + i + 0 ] * data2[ data2_offset + i + 0 ] + 
                      data1[ data1_offset + i + 1 ] * data2[ data2_offset + i + 1 ] +
                      data1[ data1_offset + i + 2 ] * data2[ data2_offset + i + 2 ] +
                      data1[ data1_offset + i + 3 ] * data2[ data2_offset + i + 3 ];
        }

        /* add any remaining products */
        for( ; i < dataSize; i++ ) 
        {
            result += data1[ data1_offset+i ] * data2[ data2_offset+i ];
        }

        return result;
    }
}
