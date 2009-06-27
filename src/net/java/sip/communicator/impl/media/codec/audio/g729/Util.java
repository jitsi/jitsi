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
package net.java.sip.communicator.impl.media.codec.audio.g729;

import java.io.*;

/**
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

/*****************************************************************************/
/* auxiliary functions                                                       */
/*****************************************************************************/

/*-------------------------------------------------------------------*
 * Function  set zero()                                              *
 *           ~~~~~~~~~~                                              *
 * Set vector x[] to zero                                            *
 *-------------------------------------------------------------------*/

static void set_zero(
  float  x[],       /* (o)    : vector to clear     */
  int L             /* (i)    : length of vector    */
)
{
    set_zero(x, 0, L);
}

static void set_zero(float[] x, int offset, int length)
{
   for (int i = offset, toIndex = offset + length; i < toIndex; i++)
     x[i] = 0.0f;
}

/*-------------------------------------------------------------------*
 * Function  copy:                                                   *
 *           ~~~~~                                                   *
 * Copy vector x[] to y[]                                            *
 *-------------------------------------------------------------------*/

static void copy(
  float  x[],      /* (i)   : input vector   */
  float  y[],      /* (o)   : output vector  */
  int L            /* (i)   : vector length  */
)
{
    copy(x, 0, y, L);
}

static void copy(float[] x, int x_offset, float[] y, int L)
{
    copy(x, x_offset, y, 0, L);
}

static void copy(float[] x, int x_offset, float[] y, int y_offset, int L)
{
   int i;

   for (i = 0; i < L; i++)
     y[y_offset + i] = x[x_offset + i];
}

/* Random generator  */
private static short seed = 21845;

static short random_g729()
{
  seed = (short) (seed * 31821L + 13849L);

  return(seed);
}

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
