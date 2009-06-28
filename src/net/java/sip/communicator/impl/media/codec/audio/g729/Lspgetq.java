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
class Lspgetq
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
 File : LSPGETQ.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/*----------------------------------------------------------------------------
 * lsp_get_quant - reconstruct quantized LSP parameter and check the stabilty
 *----------------------------------------------------------------------------
 */

static void lsp_get_quant(
 float  lspcb1[][/* M */],    /*input : first stage LSP codebook     */
 float  lspcb2[][/* M */],    /*input : Second stage LSP codebook    */
 int    code0,          /*input : selected code of first stage */
 int    code1,          /*input : selected code of second stage*/
 int    code2,          /*input : selected code of second stage*/
 float  fg[][/* M */],        /*input : MA prediction coef.          */
 float  freq_prev[][/* M */], /*input : previous LSP vector          */
 float  lspq[],         /*output: quantized LSP parameters     */
 float  fg_sum[]        /*input : present MA prediction coef.  */
)
{
   float GAP1 = Ld8k.GAP1;
   float GAP2 = Ld8k.GAP2;
   int M = Ld8k.M;
   int NC = Ld8k.NC;

   int  j;
   float[]  buf = new float[M];


   for(j=0; j<NC; j++)
     buf[j] = lspcb1[code0][j] + lspcb2[code1][j];
   for(j=NC; j<M; j++)
     buf[j] = lspcb1[code0][j] + lspcb2[code2][j];

   /* check */
   lsp_expand_1_2(buf, GAP1);
   lsp_expand_1_2(buf, GAP2);

   /* reconstruct quantized LSP parameters */
   lsp_prev_compose(buf, lspq, fg, freq_prev, fg_sum);

   lsp_prev_update(buf, freq_prev);

   lsp_stability( lspq );  /* check the stabilty */
}

/*----------------------------------------------------------------------------
 * lsp_expand_1  - check for lower (0-4)
 *----------------------------------------------------------------------------
 */
static void lsp_expand_1(
 float  buf[],          /* in/out: lsp vectors  */
 float gap
)
{
   int NC = Ld8k.NC;

   int   j;
   float diff, tmp;

   for(j=1; j<NC; j++) {
      diff = buf[j-1] - buf[j];
      tmp  = (diff + gap) * 0.5f;
      if(tmp >  0) {
         buf[j-1] -= tmp;
         buf[j]   += tmp;
      }
   }
}

/*----------------------------------------------------------------------------
 * lsp_expand_2 - check for higher (5-9)
 *----------------------------------------------------------------------------
 */
static void lsp_expand_2(
 float  buf[],          /*in/out: lsp vectors  */
 float gap
)
{
   int M = Ld8k.M;
   int NC = Ld8k.NC;

   int   j;
   float diff, tmp;

   for(j=NC; j<M; j++) {
      diff = buf[j-1] - buf[j];
      tmp  = (diff + gap) * 0.5f;
      if(tmp >  0) {
         buf[j-1] -= tmp;
         buf[j]   += tmp;
      }
   }
}

/*----------------------------------------------------------------------------
 * lsp_expand_1_2 - ..
 *----------------------------------------------------------------------------
 */
static void lsp_expand_1_2(
 float  buf[],          /*in/out: LSP parameters  */
 float  gap             /*input      */
)
{
   int M = Ld8k.M;

   int   j;
   float diff, tmp;

   for(j=1; j<M; j++) {
      diff = buf[j-1] - buf[j];
      tmp  = (diff + gap) * 0.5f;
      if(tmp >  0) {
         buf[j-1] -= tmp;
         buf[j]   += tmp;
      }
   }
}



/*
  Functions which use previous LSP parameter (freq_prev).
*/


/*
  compose LSP parameter from elementary LSP with previous LSP.
*/
private static void lsp_prev_compose(
  float lsp_ele[],             /* (i) Q13 : LSP vectors                 */
  float lsp[],                 /* (o) Q13 : quantized LSP parameters    */
  float fg[][/* M */],               /* (i) Q15 : MA prediction coef.         */
  float freq_prev[][/* M */],        /* (i) Q13 : previous LSP vector         */
  float fg_sum[]               /* (i) Q15 : present MA prediction coef. */
)
{
   int M = Ld8k.M;
   int MA_NP = Ld8k.MA_NP;

   int j, k;

   for(j=0; j<M; j++) {
      lsp[j] = lsp_ele[j] * fg_sum[j];
      for(k=0; k<MA_NP; k++) lsp[j] += freq_prev[k][j]*fg[k][j];
   }
}

/*
  extract elementary LSP from composed LSP with previous LSP
*/
static void lsp_prev_extract(
  float lsp[/* M */],                /* (i) Q13 : unquantized LSP parameters  */
  float lsp_ele[/* M */],            /* (o) Q13 : target vector               */
  float fg[/* MA_NP */][/* M */],          /* (i) Q15 : MA prediction coef.         */
  float freq_prev[/* MA_NP */][/* M */],   /* (i) Q13 : previous LSP vector         */
  float fg_sum_inv[/* M */]          /* (i) Q12 : inverse previous LSP vector */
)
{
  int M = Ld8k.M;
  int MA_NP = Ld8k.MA_NP;

  int j, k;

  /*----- compute target vectors for each MA coef.-----*/
  for( j = 0 ; j < M ; j++ ) {
      lsp_ele[j]=lsp[j];
      for ( k = 0 ; k < MA_NP ; k++ )
         lsp_ele[j] -= freq_prev[k][j] * fg[k][j];
      lsp_ele[j] *= fg_sum_inv[j];
   }
}
/*
  update previous LSP parameter
*/
static void lsp_prev_update(
  float lsp_ele[/* M */],             /* input : LSP vectors           */
  float freq_prev[/* MA_NP */][/* M */]     /* input/output: previous LSP vectors  */
)
{
  int M = Ld8k.M;
  int MA_NP = Ld8k.MA_NP;

  int k;

  for ( k = MA_NP-1 ; k > 0 ; k-- )
    Util.copy(freq_prev[k-1], freq_prev[k], M);

  Util.copy(lsp_ele, freq_prev[0], M);
}
/*----------------------------------------------------------------------------
 * lsp_stability - check stability of lsp coefficients
 *----------------------------------------------------------------------------
 */
private static void lsp_stability(
 float  buf[]           /*in/out: LSP parameters  */
)
{
   float GAP3 = Ld8k.GAP3;
   float L_LIMIT = Ld8k.L_LIMIT;
   int M = Ld8k.M;
   float M_LIMIT = Ld8k.M_LIMIT;

   int   j;
   float diff, tmp;


   for(j=0; j<M-1; j++) {
      diff = buf[j+1] - buf[j];
      if( diff < 0.f ) {
         tmp      = buf[j+1];
         buf[j+1] = buf[j];
         buf[j]   = tmp;
      }
   }

   if( buf[0] < L_LIMIT ) {
      buf[0] = L_LIMIT;
      System.out.printf("warning LSP Low \n");
   }
   for(j=0; j<M-1; j++) {
      diff = buf[j+1] - buf[j];
      if( diff < GAP3 ) {
        buf[j+1] = buf[j]+ GAP3;
      }
   }
   if( buf[M-1] > M_LIMIT ) {
      buf[M-1] = M_LIMIT;
      System.out.printf("warning LSP High \n");
   }
}
}
