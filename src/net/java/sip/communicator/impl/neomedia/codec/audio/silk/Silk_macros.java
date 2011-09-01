/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Translated from what is an inline header file for general platform.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_macros 
{
    // (a32 * (SKP_int32)((SKP_int16)(b32))) >> 16 output have to be 32bit int
    static int SKP_SMULWB(int a32, int b32)
    {
        return ((((a32) >> 16) * (int)((short)(b32))) + ((((a32) & 0x0000FFFF) * (int)((short)(b32))) >> 16));
    }

    // a32 + (b32 * (SKP_int32)((SKP_int16)(c32))) >> 16 output have to be 32bit int
    static int SKP_SMLAWB(int a32, int b32, int c32)
    {
        return ((a32) + ((((b32) >> 16) * (int)((short)(c32))) + ((((b32) & 0x0000FFFF) * (int)((short)(c32))) >> 16)));
    }

    // (a32 * (b32 >> 16)) >> 16
    static int SKP_SMULWT(int a32, int b32)
    {
        return (((a32) >> 16) * ((b32) >> 16) + ((((a32) & 0x0000FFFF) * ((b32) >> 16)) >> 16));
    }

    // a32 + (b32 * (c32 >> 16)) >> 16
    static int SKP_SMLAWT(int a32, int b32, int c32)
    {
        return ((a32) + (((b32) >> 16) * ((c32) >> 16)) + ((((b32) & 0x0000FFFF) * ((c32) >> 16)) >> 16));
    }

    // (SKP_int32)((SKP_int16)(a3))) * (SKP_int32)((SKP_int16)(b32)) output have to be 32bit int
    static int SKP_SMULBB(int a32, int b32)
    {
        return ((int)((short)(a32)) * (int)((short)(b32)));
    }

    // a32 + (SKP_int32)((SKP_int16)(b32)) * (SKP_int32)((SKP_int16)(c32)) output have to be 32bit int
    static int SKP_SMLABB(int a32, int b32, int c32)
    {
        return ((a32) + ((int)((short)(b32))) * (int)((short)(c32)));
    }

    // (SKP_int32)((SKP_int16)(a32)) * (b32 >> 16)
    static int SKP_SMULBT(int a32, int b32)
    {
        return ((int)((short)(a32)) * ((b32) >> 16));
    }

    // a32 + (SKP_int32)((SKP_int16)(b32)) * (c32 >> 16)
    static int SKP_SMLABT(int a32, int b32, int c32)
    {
        return ((a32) + ((int)((short)(b32))) * ((c32) >> 16));
    }

    // a64 + (b32 * c32)
    static long SKP_SMLAL(long a64, int b32, int c32)
    {
        return a64 + (long)b32 * (long)c32;
    }

    // (a32 * b32) >> 16
    static int SKP_SMULWW(int a32, int b32)
    {
        return SKP_SMULWB(a32, b32) + a32 * Silk_SigProc_FIX.SKP_RSHIFT_ROUND(b32, 16);
    }

    // a32 + ((b32 * c32) >> 16)
    static int SKP_SMLAWW(int a32, int b32, int c32)
    {
        return SKP_SMLAWB(a32, b32, c32) + b32 * Silk_SigProc_FIX.SKP_RSHIFT_ROUND(c32, 16);
    }

    /* add/subtract with output saturated */
    static int SKP_ADD_SAT32(int a, int b)
    {
        if( ((a + b) & 0x80000000) == 0 )
            return ((a & b) & 0x80000000) != 0 ? Integer.MIN_VALUE : a+b;
        else
            return ((a | b) & 0x80000000) == 0 ? Integer.MAX_VALUE : a+b;
    }

    static int SKP_SUB_SAT32(int a, int b)
    {
        if( ((a - b) & 0x80000000) == 0 )
            return ( a & (b^0x80000000) & 0x80000000) != 0 ? Integer.MIN_VALUE : a-b;
        else
            return ( (a^0x80000000) & b & 0x80000000) != 0 ? Integer.MAX_VALUE : a-b;
    }
        
    static int SKP_Silk_CLZ16(short in16)
    {
        return Integer.numberOfLeadingZeros((int)in16 & 0x0000FFFF) - 16;
    }

    static int SKP_Silk_CLZ32(int in32)
    {
        return Integer.numberOfLeadingZeros(in32);
    }
}
