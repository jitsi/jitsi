/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * compute autocorrelation.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_autocorrelation_FLP 
{
    /**
     * compute autocorrelation.
     * @param results result (length correlationCount)
     * @param results_offset offset of valid data.
     * @param inputData input data to correlate
     * @param inputData_offset offset of valid data.
     * @param inputDataSize length of input 
     * @param correlationCount number of correlation taps to compute
     */
 //TODO: float or double???   
    static void SKP_Silk_autocorrelation_FLP( 
        float[]       results,           /* O    result (length correlationCount)            */
        int results_offset,
        float[]       inputData,         /* I    input data to correlate                     */
        int inputData_offset,
        int         inputDataSize,      /* I    length of input                             */
        int         correlationCount    /* I    number of correlation taps to compute       */
    )
    {
        int i;

        if ( correlationCount > inputDataSize )
        {
            correlationCount = inputDataSize;
        }

        for( i = 0; i < correlationCount; i++ ) 
        {
            results[ results_offset+i ] =  (float)Silk_inner_product_FLP.SKP_Silk_inner_product_FLP( inputData,inputData_offset, inputData,inputData_offset + i, inputDataSize - i );
        }
    }
}
