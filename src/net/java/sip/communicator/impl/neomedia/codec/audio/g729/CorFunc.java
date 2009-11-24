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
 * Functions corr_xy2() and cor_h_x().
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class CorFunc
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
 File : COR_FUNC.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/**
 * Compute the correlation products needed for gain computation.
 *
 * @param xn        input : target vector x[0:l_subfr]
 * @param y1        input : filtered adaptive codebook vector
 * @param y2        input : filtered 1st codebook innovation
 * @param g_coeff   output: <y2,y2> , -2<xn,y2> , and 2<y1,y2>
 */
static void corr_xy2(
 float xn[],
 float y1[],
 float y2[],
 float g_coeff[]
)
{
   int L_SUBFR = Ld8k.L_SUBFR;

   float y2y2, xny2, y1y2;
   int i;

   y2y2= 0.01f;
   for (i = 0; i < L_SUBFR; i++) y2y2 += y2[i]*y2[i];
   g_coeff[2] = y2y2 ;

   xny2 = 0.01f;
   for (i = 0; i < L_SUBFR; i++) xny2+= xn[i]*y2[i];
   g_coeff[3] = -2.0f * xny2;

   y1y2 = 0.01f;
   for (i = 0; i < L_SUBFR; i++) y1y2 += y1[i]*y2[i];
   g_coeff[4] = 2.0f * y1y2 ;
}

/**
 * Compute  correlations of input response h[] with the target vector X[].
 *
 * @param h     (i) :Impulse response of filters
 * @param x     (i) :Target vector
 * @param d     (o) :Correlations between h[] and x[]
 */
static void cor_h_x(
     float h[], 
     float x[],     
     float d[]      
)
{
   int L_SUBFR = Ld8k.L_SUBFR;

   int i, j;
   float  s;

   for (i = 0; i < L_SUBFR; i++)
   {
     s = 0.0f;
     for (j = i; j <  L_SUBFR; j++)
       s += x[j] * h[j-i];
     d[i] = s;
   }
}

}
