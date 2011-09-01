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
public class Silk_code_signs 
{
    /* shifting avoids if-statement */
//    #define SKP_enc_map(a)                  ( SKP_RSHIFT( (a), 15 ) + 1 )
    static int SKP_enc_map(int a)
    {
        return (a>>15)+1;
    }

//    #define SKP_dec_map(a)                  ( SKP_LSHIFT( (a),  1 ) - 1 )
    static int SKP_dec_map(int a)
    {
        return (a<<1)-1;
    }

    /**
     * Encodes signs of excitation.
     * @param sRC Range coder state.
     * @param q Pulse signal.
     * @param length Length of input.
     * @param sigtype Signal type.
     * @param QuantOffsetType QuantOffsetType.
     * @param RateLevelIndex Rate level index.
     */
    static void SKP_Silk_encode_signs(
        SKP_Silk_range_coder_state      sRC,               /* I/O  Range coder state                       */
        byte[]                      q,                  /* I    Pulse signal                            */
        final int                   length,             /* I    Length of input                         */
        final int                   sigtype,            /* I    Signal type                             */
        final int                   QuantOffsetType,    /* I    Quantization offset type                */
        final int                   RateLevelIndex      /* I    Rate level index                        */
    )
    {
        int i;
        int inData;
        int[] cdf = new int[3];

        i = Silk_macros.SKP_SMULBB( Silk_define.N_RATE_LEVELS - 1, ( sigtype << 1 ) + QuantOffsetType ) + RateLevelIndex;
        cdf[ 0 ] = 0;
        cdf[ 1 ] = Silk_tables_sign.SKP_Silk_sign_CDF[ i ];
        cdf[ 2 ] = 65535;
        
        for( i = 0; i < length; i++ ) 
        {
            if( q[ i ] != 0 )
            {
//                inData = SKP_enc_map( q[ i ] ); /* - = 0, + = 1 */
                inData = (q[i] >>15) + 1; /* - = 0, + = 1 */       
                Silk_range_coder.SKP_Silk_range_encoder( sRC, inData, cdf, 0 );
            }
        }
    }

    /**
     * Decodes signs of excitation.
     * @param sRC Range coder state.
     * @param q pulse signal.
     * @param length length of output.
     * @param sigtype Signal type.
     * @param QuantOffsetType Quantization offset type.
     * @param RateLevelIndex Rate Level Index.
     */
    static void SKP_Silk_decode_signs(
            SKP_Silk_range_coder_state      sRC,               /* I/O  Range coder state                           */
            int                         q[],                /* I/O  pulse signal                                */
            final int                   length,             /* I    length of output                            */
            final int                   sigtype,            /* I    Signal type                                 */
            final int                   QuantOffsetType,    /* I    Quantization offset type                    */
            final int                   RateLevelIndex      /* I    Rate Level Index                            */
        )
    {
        int i;
        int data;
        int data_ptr[] = new int[1];
        int[] cdf = new int[3];

        i = Silk_macros.SKP_SMULBB( Silk_define.N_RATE_LEVELS - 1, ( sigtype << 1 ) + QuantOffsetType ) + RateLevelIndex;
        cdf[ 0 ] = 0;
        cdf[ 1 ] = Silk_tables_sign.SKP_Silk_sign_CDF[ i ];
        cdf[ 2 ] = 65535;
        
        for( i = 0; i < length; i++ ) {
            if( q[ i ] > 0 ) {                
                Silk_range_coder.SKP_Silk_range_decoder( data_ptr, 0, sRC, cdf, 0, 1 );
                data = data_ptr[0];
                /* attach sign */
                /* implementation with shift, subtraction, multiplication */
//                q[ i ] *= SKP_dec_map( data );
                q[ i ] *= (data<<1) - 1; 
            }
        }
    }
}
