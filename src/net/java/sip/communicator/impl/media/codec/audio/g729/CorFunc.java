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

/**
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

/* Functions corr_xy2() and cor_h_x()   */

/*----------------------------------------------------------------------------
 * corr_xy2 - compute the correlation products needed for gain computation
 *----------------------------------------------------------------------------
 */
static void corr_xy2(
 float xn[],            /* input : target vector x[0:l_subfr] */
 float y1[],            /* input : filtered adaptive codebook vector */
 float y2[],            /* input : filtered 1st codebook innovation */
 float g_coeff[]        /* output: <y2,y2> , -2<xn,y2> , and 2<y1,y2>*/
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

/*--------------------------------------------------------------------------*
 *  Function  cor_h_x()                                                     *
 *  ~~~~~~~~~~~~~~~~~~~~                                                    *
 * Compute  correlations of input response h[] with the target vector X[].  *
 *--------------------------------------------------------------------------*/

static void cor_h_x(
     float h[],        /* (i) :Impulse response of filters      */
     float x[],        /* (i) :Target vector                    */
     float d[]         /* (o) :Correlations between h[] and x[] */
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
