/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * 
 * @author Jing Dai
 * @author Dingxin Xu 
 */
public class Silk_energy_FLP 
{
    /**
     * sum of squares of a float array, with result as double.
     * @param data
     * @param data_offset
     * @param dataSize
     * @return
     */
//TODO: float or double???    
    static double SKP_Silk_energy_FLP
    ( 
        float[]     data,
        int data_offset,
        int             dataSize
    )
    {
        int  i, dataSize4;
        double   result;

        /* 4x unrolled loop */
        result = 0.0f;
        dataSize4 = dataSize & 0xFFFC;
        for( i = 0; i < dataSize4; i += 4 ) 
        {
            result += data[data_offset+ i + 0 ] * data[data_offset+ i + 0 ] + 
                      data[data_offset+ i + 1 ] * data[data_offset+ i + 1 ] +
                      data[data_offset+ i + 2 ] * data[data_offset+ i + 2 ] +
                      data[data_offset+ i + 3 ] * data[data_offset+ i + 3 ];
        }

        /* add any remaining products */
        for( ; i < dataSize; i++ ) 
        {
            result += data[data_offset+ i ] * data[data_offset+ i ];
        }

        assert( result >= 0.0 );
        return result;
    }
}
