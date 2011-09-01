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
class Silk_Inlines_constants
{
    static final int SKP_SIN_APPROX_CONST0 =      1073735400;
    static final int SKP_SIN_APPROX_CONST1 =       -82778932;
    static final int SKP_SIN_APPROX_CONST2 =         1059577;
    static final int SKP_SIN_APPROX_CONST3 =           -5013;    
}

public class Silk_Inlines
    extends Silk_Inlines_constants
{
    /**
     * count leading zeros of long.
     * @param in. input
     * @return
     */
    static int SKP_Silk_CLZ64(long in)
    {
        return Long.numberOfLeadingZeros(in);
    }

    /**
     * get number of leading zeros and fractional part (the bits right after the leading one).
     * @param in
     * @param lz
     * @param frac_Q7
     */
    static void SKP_Silk_CLZ_FRAC(int in,            /* I: input */
                                  int[] lz,          /* O: number of leading zeros */
                                  int[] frac_Q7)     /* O: the 7 bits right after the leading one */
    {
        int lzeros = Integer.numberOfLeadingZeros(in);

        lz[0] = lzeros;
        frac_Q7[0] = Silk_SigProc_FIX.SKP_ROR32(in, 24 - lzeros) & 0x7f;
    }
    
    /**
     * Approximation of square root                                          
     * Accuracy: < +/- 10% for output values > 15                            
                 < +/- 2.5% for output values > 120         
     * @param x
     * @return
     */
    static int SKP_Silk_SQRT_APPROX(int x)
    {
        int y;
        int[] lz = new int[1], frac_Q7 = new int[1];

        if( x <= 0 ) 
        {
            return 0;
        }

        SKP_Silk_CLZ_FRAC(x, lz, frac_Q7);

        if( (lz[0] & 1) != 0 )
        {
            y = 32768;
        }
        else 
        {
            y = 46214;        /* 46214 = sqrt(2) * 32768 */
        }

        /* get scaling right */
        y >>= (lz[0]>>1);

        /* increment using fractional part of input */
        y = Silk_macros.SKP_SMLAWB(y, y, Silk_macros.SKP_SMULBB(213, frac_Q7[0]));

        return y;
    }
    
    /**
     * returns the number of left shifts before overflow for a 16 bit 
     * number (ITU definition with norm(0)=0).
     * @param a
     * @return
     */
    static int SKP_Silk_norm16(short a)
    {
      int a32;

      /* if ((a == 0) || (a == SKP_int16_MIN)) return(0); */
      if ((a << 1) == 0) 
          return(0);

      a32 = a;
      /* if (a32 < 0) a32 = -a32 - 1; */
      a32 ^= (a32>>31);

      return Integer.numberOfLeadingZeros(a32) - 17;
    }
    
    /**
     * returns the number of left shifts before overflow for a 32 bit 
     * number (ITU definition with norm(0)=0)
     * @param a
     * @return
     */
    static int SKP_Silk_norm32(int a) 
    {
      
      /* if ((a == 0) || (a == Interger.MIN_VALUE)) return(0); */
      if ((a << 1) == 0) 
          return(0);

      /* if (a < 0) a = -a - 1; */
      a ^= (a>>31);

      return Integer.numberOfLeadingZeros(a) - 1;
    }

    /**
     * Divide two int32 values and return result as int32 in a given Q-domain.
     * @param a32 numerator (Q0)
     * @param b32 denominator (Q0)
     * @param Qres Q-domain of result (>= 0)
     * @return returns a good approximation of "(a32 << Qres) / b32"
     */
    static int SKP_DIV32_varQ         /* O    returns a good approximation of "(a32 << Qres) / b32" */
    ( 
        final int        a32,         /* I    numerator (Q0)                  */
        final int        b32,         /* I    denominator (Q0)                */
        final int        Qres         /* I    Q-domain of result (>= 0)       */
    )
    {
        int   a_headrm, b_headrm, lshift;
        int b32_inv, a32_nrm, b32_nrm, result;

        assert( b32 != 0 );
        assert( Qres >= 0 );

        /* Compute number of bits head room and normalize inputs */
        a_headrm = Integer.numberOfLeadingZeros( Math.abs(a32) ) - 1;
        a32_nrm = a32<<a_headrm;                                    /* Q: a_headrm                    */
        b_headrm = Integer.numberOfLeadingZeros( Math.abs(b32) ) - 1;
        b32_nrm = b32<<b_headrm;                                    /* Q: b_headrm                    */

        /* Inverse of b32, with 14 bits of precision */
        b32_inv = (Integer.MAX_VALUE >> 2) / (b32_nrm>>16) ;  /* Q: 29 + 16 - b_headrm        */

        /* First approximation */
        result = Silk_macros.SKP_SMULWB(a32_nrm, b32_inv);                                  /* Q: 29 + a_headrm - b_headrm    */

        /* Compute residual by subtracting product of denominator and first approximation */
        a32_nrm -= Silk_SigProc_FIX.SKP_SMMUL(b32_nrm, result)<<3;           /* Q: a_headrm                    */

        /* Refinement */
        result = Silk_macros.SKP_SMLAWB(result, a32_nrm, b32_inv);                          /* Q: 29 + a_headrm - b_headrm    */

        /* Convert to Qres domain */
        lshift = 29 + a_headrm - b_headrm - Qres;
        if( lshift <= 0 ) 
        {
            return Silk_SigProc_FIX.SKP_LSHIFT_SAT32(result, -lshift);
        } 
        else 
        {
            if( lshift < 32)
            {
                return result>>lshift;
            } 
            else 
            {
                /* Avoid undefined result */
                return 0;
            }
        }
    }

    /**
     * Invert int32 value and return result as int32 in a given Q-domain.
     * @param b32 denominator (Q0)
     * @param Qres Q-domain of result (> 0)
     * @return returns a good approximation of "(1 << Qres) / b32"
     */
    static int SKP_INVERSE32_varQ         /* O    returns a good approximation of "(1 << Qres) / b32" */
    (
        final int        b32,             /* I    denominator (Q0)                */
        final int        Qres             /* I    Q-domain of result (> 0)        */
    )
    {
        int   b_headrm, lshift;
        int b32_inv, b32_nrm, err_Q32, result;

        assert( b32 != 0 );
        assert( Qres > 0 );

        /* Compute number of bits head room and normalize input */
        b_headrm = Integer.numberOfLeadingZeros( Math.abs(b32) ) - 1;
        b32_nrm = b32<<b_headrm;                                    /* Q: b_headrm                */

        /* Inverse of b32, with 14 bits of precision */
        b32_inv = (Integer.MAX_VALUE >> 2) / (b32_nrm>>16);  /* Q: 29 + 16 - b_headrm    */

        /* First approximation */
        result = b32_inv<<16;                                       /* Q: 61 - b_headrm            */

        /* Compute residual by subtracting product of denominator and first approximation from one */
        err_Q32 = -Silk_macros.SKP_SMULWB(b32_nrm, b32_inv)<<3;         /* Q32                        */

        /* Refinement */
        result = Silk_macros.SKP_SMLAWW(result, err_Q32, b32_inv);                          /* Q: 61 - b_headrm            */

        /* Convert to Qres domain */
        lshift = 61 - b_headrm - Qres;
        if( lshift <= 0 ) 
        {
            return Silk_SigProc_FIX.SKP_LSHIFT_SAT32(result, -lshift);
        }
        else
        {
            if( lshift < 32)
            {
                return result>>lshift;
            }
            else
            {
                /* Avoid undefined result */
                return 0;
            }
        }
    }

    /**
     * Sine approximation; an input of 65536 corresponds to 2 * pi 
     * Uses polynomial expansion of the input to the power 0, 2, 4 and 6 
     * The relative error is below 1e-5 
     * 
     * @param x
     * @return returns approximately 2^24 * sin(x * 2 * pi / 65536).
     */
    static int SKP_Silk_SIN_APPROX_Q24(        /* O    returns approximately 2^24 * sin(x * 2 * pi / 65536) */
                                        int        x)
    {
        int y_Q30;

        /* Keep only bottom 16 bits (the function repeats itself with period 65536) */
        x &= 65535;

        /* Split range in four quadrants */
        if( x <= 32768 )
        {
            if( x < 16384 ) 
            {
                /* Return cos(pi/2 - x) */
                x = 16384 - x;
            } 
            else
            {
                /* Return cos(x - pi/2) */
                x -= 16384;
            }
            if( x < 1100 ) 
            {
                /* Special case: high accuracy */
                return Silk_macros.SKP_SMLAWB( 1 << 24, x*x, -5053 );
            }
            x = Silk_macros.SKP_SMULWB( x<<8 , x );        /* contains x^2 in Q20 */
            y_Q30 = Silk_macros.SKP_SMLAWB( SKP_SIN_APPROX_CONST2, x, SKP_SIN_APPROX_CONST3 );
            y_Q30 = Silk_macros.SKP_SMLAWW( SKP_SIN_APPROX_CONST1, x, y_Q30 );
            y_Q30 = Silk_macros.SKP_SMLAWW( SKP_SIN_APPROX_CONST0 + 66, x, y_Q30 );
        } 
        else
        {
            if( x < 49152 ) 
            {
                /* Return -cos(3*pi/2 - x) */
                x = 49152 - x;
            }
            else 
            {
                /* Return -cos(x - 3*pi/2) */
                x -= 49152;
            }
            if( x < 1100 ) 
            {
                /* Special case: high accuracy */
                return Silk_macros.SKP_SMLAWB( -1 << 24, x*x , 5053 );
            }
            x = Silk_macros.SKP_SMULWB( x<<8 , x );        /* contains x^2 in Q20 */
            y_Q30 = Silk_macros.SKP_SMLAWB( -SKP_SIN_APPROX_CONST2, x, -SKP_SIN_APPROX_CONST3 );
            y_Q30 = Silk_macros.SKP_SMLAWW( -SKP_SIN_APPROX_CONST1, x, y_Q30 );
            y_Q30 = Silk_macros.SKP_SMLAWW( -SKP_SIN_APPROX_CONST0, x, y_Q30 );
        }
        return Silk_SigProc_FIX.SKP_RSHIFT_ROUND( y_Q30, 6 );
    }
    
    /**
     * Cosine approximation; an input of 65536 corresponds to 2 * pi 
     * The relative error is below 1e-5 
     * @param x
     * @return returns approximately 2^24 * cos(x * 2 * pi / 65536).
     */
    static int SKP_Silk_COS_APPROX_Q24(        /* O    returns approximately 2^24 * cos(x * 2 * pi / 65536) */
                                        int        x)
    {
        return SKP_Silk_SIN_APPROX_Q24( x + 16384 );
    }
}
