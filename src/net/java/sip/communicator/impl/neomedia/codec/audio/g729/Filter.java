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

/**
 * General filter routines.
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Filter
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
 File : FILTER.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/**
 * Convolve vectors x and h and put result in y.
 *
 * @param x         input : input vector x[0:l]
 * @param x_offset  input : input vector offset
 * @param h         input : impulse response or second input h[0:l]
 * @param y         output: x convolved with h , y[0:l]
 * @param l         input : dimension of all vectors
 */
static void convolve(
 float x[],        
 int x_offset,
 float h[],            
 float y[],            
 int  l              
)
{
   float temp;
   int    i, n;

   for (n = 0; n < l; n++)
     {
        temp = 0.0f;
        for (i = 0; i <= n; i++)
          temp += x[x_offset + i]*h[n-i];
        y[n] = temp;
     }
}

/**
 * Filter with synthesis filter 1/A(z).
 *
 * @param a          input : predictor coefficients a[0:m]
 * @param a_offset   input : predictor coefficients a offset
 * @param x          input : excitation signal  
 * @param x_offset   input : excitation signal offset
 * @param y          output: filtered output signal
 * @param y_offset   output: filtered output signal offset
 * @param l          input : vector dimension
 * @param mem        in/out: filter memory
 * @param mem_offset input : filter memory ofset
 * @param update     input : 0 = no memory update, 1 = update
 */
static void syn_filt(
 float a[],     
 int a_offset,
 float x[],     
 int x_offset,
 float y[],    
 int y_offset,
 int  l,       
 float mem[],   
 int mem_offset,
 int  update    
)
{
   int L_SUBFR = Ld8k.L_SUBFR;
   int M = Ld8k.M;

   int  i,j;

   /* This is usually done by memory allocation (l+m) */
   float[] yy_b = new float[L_SUBFR+M];
   float s;
   int yy, py, pa;
   /* Copy mem[] to yy[] */
   yy = 0; //index instead of pointer
   for (i = 0; i <M; i++)  yy_b[yy++] =  mem[mem_offset ++];

   /* Filtering */

   for (i = 0; i < l; i++)
     {
        py=yy;
        pa=0; //index instead of pointer
        s = x[x_offset ++];
        for (j = 0; j <M; j++)  s -= (a[a_offset + ++pa]) * (yy_b[--py]);
        yy_b[yy++] = s;
        y[y_offset ++] = s;
     }

   /* Update memory if required */

   if(update !=0 ) for (i = 0; i <M; i++)  mem[--mem_offset] = yy_b[--yy];
}

/**
 * Filter input vector with all-zero filter A(Z).
 *
 * @param a         input : prediction coefficients a[0:m+1], a[0]=1.
 * @param a_offset  input : prediction coefficients a offset
 * @param x         input : input signal x[0:l-1], x[-1:m] are needed
 * @param x_offset  input : input signal x offset
 * @param y         output: output signal y[0:l-1].
 *                  NOTE: x[] and y[] cannot point to same array
 * @param y_offset  input : output signal y offset
 * @param l         input : dimension of x and y
 */
static void residu(    
 float[] a,      
 int a_offset,
 float[] x,      
 int x_offset,
 float[] y,  
 int y_offset,
 int  l      
)
{
  int M = Ld8k.M;

  float s;
  int  i, j;

  for (i = 0; i < l; i++)
  {
    s = x[x_offset + i];
    for (j = 1; j <= M; j++) s += a[a_offset + j]*x[x_offset + i-j];
    y[y_offset + i] = s;
  }
}
}
