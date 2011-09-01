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
public class Silk_typedef 
{
    static int SKP_STR_CASEINSENSITIVE_COMPARE(String x, String y)
    {
        return x.compareTo(y);
    }
    
    static final long SKP_int64_MAX =  0x7FFFFFFFFFFFFFFFL;   //  2^63 - 1  
    static final long SKP_int64_MIN =  0x8000000000000000L;   // -2^63     
    static final int SKP_int32_MAX =  0x7FFFFFFF;             //  2^31 - 1 =  2147483647
    static final int SKP_int32_MIN =  0x80000000;             // -2^31     = -2147483648
    static final short SKP_int16_MAX =  0x7FFF;               //  2^15 - 1 =  32767
    static final short SKP_int16_MIN =  (short)0x8000;        // -2^15     = -32768
    static final byte SKP_int8_MAX =   0x7F;                  //  2^7 - 1  =  127
    static final byte SKP_int8_MIN =   (byte)0x80;            // -2^7      = -128

    static final long SKP_uint32_MAX = 0xFFFFFFFFL;  // 2^32 - 1 = 4294967295
    static final long SKP_uint32_MIN = 0x00000000L;
    static final int SKP_uint16_MAX = 0xFFFF;        // 2^16 - 1 = 65535
    static final int SKP_uint16_MIN = 0x0000;
    static final short SKP_uint8_MAX =  0xFF;        //  2^8 - 1 = 255
    static final short SKP_uint8_MIN =  0x00;

    static final boolean SKP_TRUE =       true;
    static final boolean SKP_FALSE =      false;
        
    /* assertions */
    static void SKP_assert(boolean COND)
    {
        assert(COND);
    }
}
