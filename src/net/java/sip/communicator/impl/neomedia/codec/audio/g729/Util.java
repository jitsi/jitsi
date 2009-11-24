/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
/*
 * WARNING: The use of G.729 may require a license fee and/or royalty fee in
 * some countries and is licensed by
 * <a href="http://www.sipro.com">SIPRO Lab Telecom</a>.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.g729;

import java.io.*;

/**
 * Auxiliary functions.
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Util
{

/* ITU-T G.729 Software Package Release 2 (November 2006) */
/*
   ITU-T G.729 Annex C - Reference C code for floating point
                         implementation of G.729
                         Version 1.01 of 15.September.98
*/

/*
----------------------------------------------------------------------
                    COPYRIGHT NOTICE
----------------------------------------------------------------------
   ITU-T G.729 Annex C ANSI C source code
   Copyright (C) 1998, AT&T, France Telecom, NTT, University of
   Sherbrooke.  All rights reserved.

----------------------------------------------------------------------
*/

/*
 File : UTIL.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/**
 * Assigns the value zero to element of the specified array of floats.
 * The number of components set to zero equal to the length argument.
 *
 * @param x     (o)    : vector to clear
 * @param L     (i)    : length of vector
 */
static void set_zero(
  float  x[],      
  int L         
)
{
    set_zero(x, 0, L);
}

/**
 * Assigns the value zero to element of the specified array of floats.
 * The number of components set to zero equal to the length argument.
 * The components at positions offset through offset+length-1 in the
 * array are set to zero.
 *
 * @param x          (o)    : vector to clear
 * @param offset     (i)    : offset of vector
 * @param length     (i)    : length of vector
 */
static void set_zero(float[] x, int offset, int length)
{
   for (int i = offset, toIndex = offset + length; i < toIndex; i++)
     x[i] = 0.0f;
}

/**
 * Copies an array from the specified x array, to the specified y array.
 * The number of components copied is equal to the length argument.
 *
 * @param x     (i)   : input vector
 * @param y     (o)   : output vector
 * @param L     (i)   : vector length
 */
static void copy(
  float  x[],    
  float  y[],     
  int L           
)
{
    copy(x, 0, y, L);
}

/**
 * Copies an array from the specified source array,
 * beginning at the specified destination array.
 * A subsequence of array components are copied from the source array referenced
 * by x to the destination array referenced by y.
 * The number of components copied is equal to the length argument.
 * The components at positions x_offset through x_offset+length-1 in the source
 * array are copied into positions 0 through length-1,
 * respectively, of the destination array.
 *
 * @param x         (i)   : input vector
 * @param x_offset  (i)   : input vector offset
 * @param y         (o)   : output vector
 * @param L         (i)   : vector length
 */
static void copy(float[] x, int x_offset, float[] y, int L)
{
    copy(x, x_offset, y, 0, L);
}

/**
 * Copies an array from the specified source array,
 * beginning at the specified position,
 * to the specified position of the destination array.
 * A subsequence of array components are copied from the source array referenced
 * by x to the destination array referenced by y.
 * The number of components copied is equal to the length argument.
 * The components at positions x_offset through x_offset+length-1 in the source
 * array are copied into positions y_offset through y_offset+length-1,
 * respectively, of the destination array.
 *
 * @param x         (i)   : input vector
 * @param x_offset  (i)   : input vector offset
 * @param y         (o)   : output vector
 * @param y_offset  (i)   : output vector offset
 * @param L         (i)   : vector length
 */
static void copy(float[] x, int x_offset, float[] y, int y_offset, int L)
{
   int i;

   for (i = 0; i < L; i++)
     y[y_offset + i] = x[x_offset + i];
}

/* Random generator  */
private static short seed = 21845;

/**
 * Return random short.
 *
 * @return random short
 */
static short random_g729()
{
  seed = (short) (seed * 31821L + 13849L);

  return(seed);
}

/**
 * Write <code>data</code> in <code>fp</code>
 *
 * @param data
 * @param length
 * @param fp
 * @throws java.io.IOException
 */
static void fwrite(short[] data, int length, OutputStream fp)
    throws IOException
{
    byte[] bytes = new byte[2];

    for (int i = 0; i < length; i++)
    {
        int value = data[i];
        bytes[0] = (byte) (value & 0xFF);
        bytes[1] = (byte) (value >> 8);
        fp.write(bytes);
    }
}

/**
 * Read <code>data</code> from <code>fp</code>.
 *
 * @param data
 * @param length
 * @param fp
 * @return length of resulting data array
 * @throws java.io.IOException
 */
static int fread(short[] data, int length, InputStream fp)
    throws IOException
{
    byte[] bytes = new byte[2];
    int readLength = 0;

    for (int i = 0; i < length; i++)
    {
        if (fp.read(bytes) != 2)
            break;
        data[i] = (short) ((bytes[1] << 8) | (bytes[0] & 0x00FF));
        readLength++;
    }
    return readLength;
}
}
