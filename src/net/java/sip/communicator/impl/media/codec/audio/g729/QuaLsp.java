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
class QuaLsp
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
 File : QUA_LSP.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/*----------------------------------------------------------*
 *  qua_lsp.c                                               *
 *  ~~~~~~~~                                                *
 * Functions related to the quantization of LSP's           *
 *----------------------------------------------------------*/

/* static memory */
private final float[][] freq_prev = new float[Ld8k.MA_NP][Ld8k.M];    /* previous LSP vector       */
private static final float[/* M */] FREQ_PREV_RESET = {  /* previous LSP vector(init) */
 0.285599f,  0.571199f,  0.856798f,  1.142397f,  1.427997f,
 1.713596f,  1.999195f,  2.284795f,  2.570394f,  2.855993f
};     /* PI*(float)(j+1)/(float)(M+1) */


void qua_lsp(
  float lsp[],       /* (i) : Unquantized LSP            */
  float lsp_q[],     /* (o) : Quantized LSP              */
  int ana[]          /* (o) : indexes                    */
)
{
  int M = Ld8k.M;

  int i;
  float[] lsf = new float[M], lsf_q = new float[M];  /* domain 0.0<= lsf <PI */

  /* Convert LSPs to LSFs */

  for (i=0; i<M; i++ )
     lsf[i] = (float)Math.acos(lsp[i]);

  lsp_qua_cs(lsf, lsf_q, ana );

  /* Convert LSFs to LSPs */

  for (i=0; i<M; i++ )
     lsp_q[i] = (float)Math.cos(lsf_q[i]);
}

/*----------------------------------------------------------------------------
 * lsp_encw_reset - set the previous LSP vector
 *----------------------------------------------------------------------------
 */
void lsp_encw_reset()
{
   int M = Ld8k.M;
   int MA_NP = Ld8k.MA_NP;

   int  i;
   for(i=0; i<MA_NP; i++)
     Util.copy (FREQ_PREV_RESET, freq_prev[i], M );
}
/*----------------------------------------------------------------------------
 * lsp_qua_cs - lsp quantizer
 *----------------------------------------------------------------------------
 */
private void lsp_qua_cs(
 float[]  flsp_in,       /*  input : Original LSP parameters      */
 float[]  lspq_out,       /*  output: Quantized LSP parameters     */
 int[]  code             /*  output: codes of the selected LSP    */
)
{
   int M = Ld8k.M;
   float[][][] fg = TabLd8k.fg;
   float[][] fg_sum = TabLd8k.fg_sum;
   float[][] fg_sum_inv = TabLd8k.fg_sum_inv;
   float[][] lspcb1 = TabLd8k.lspcb1;
   float[][] lspcb2 = TabLd8k.lspcb2;

   float[]        wegt = new float[M];   /* weight coef. */

   get_wegt( flsp_in, wegt );

   relspwed( flsp_in, wegt, lspq_out, lspcb1, lspcb2, fg,
            freq_prev, fg_sum, fg_sum_inv, code);
}
/*----------------------------------------------------------------------------
 * relspwed -
 *----------------------------------------------------------------------------
 */
private void relspwed(
 float  lsp[],                  /*input: unquantized LSP parameters  */
 float  wegt[],                 /*input: weight coef.                */
 float  lspq[],                 /*output:quantized LSP parameters    */
 float  lspcb1[][/* M */],            /*input: first stage LSP codebook    */
 float  lspcb2[][/* M */],            /*input: Second stage LSP codebook   */
 float  fg[/* MODE */][/* MA_NP */][/* M */],     /*input: MA prediction coef.         */
 float  freq_prev[/* MA_NP */][/* M */],    /*input: previous LSP vector         */
 float  fg_sum[/* MODE */][/* M */],        /*input: present MA prediction coef. */
 float  fg_sum_inv[/* MODE */][/* M */],    /*input: inverse coef.               */
 int    code_ana[]              /*output:codes of the selected LSP   */
)
{
   float GAP1 = Ld8k.GAP1;
   float GAP2 = Ld8k.GAP2;
   int M = Ld8k.M;
   int MODE = Ld8k.MODE;
   int NC = Ld8k.NC;
   int NC0_B = Ld8k.NC0_B;
   int NC1_B = Ld8k.NC1_B;

   int  mode, j;
   int  index, mode_index;
   int[]  cand = new int[MODE];
   int cand_cur;
   int[]  tindex1 = new int[MODE], tindex2 = new int[MODE];
   float[]        tdist = new float[MODE];
   float[]        rbuf = new float[M];
   float[]        buf = new float[M];

   for(mode = 0; mode<MODE; mode++) {

      Lspgetq.lsp_prev_extract(lsp, rbuf, fg[mode], freq_prev, fg_sum_inv[mode]);

      /*----- search the first stage lsp codebook -----*/
      cand_cur = lsp_pre_select(rbuf, lspcb1);
      cand[mode]=cand_cur;

      /*----- search the second stage lsp codebook (lower 0-4) ----- */
      index = lsp_select_1(rbuf, lspcb1[cand_cur], wegt, lspcb2);

      tindex1[mode] = index;

      for(j=0; j<NC; j++)
        buf[j]=lspcb1[cand_cur][j]+lspcb2[index][j];

      Lspgetq.lsp_expand_1(buf, GAP1);  /* check */

      /*----- search the second stage lsp codebook (Higher 5-9) ----- */
      index = lsp_select_2(rbuf, lspcb1[cand_cur], wegt, lspcb2);

      tindex2[mode] = index;

      for(j=NC; j<M; j++)
        buf[j]=lspcb1[cand_cur][j]+lspcb2[index][j];
      Lspgetq.lsp_expand_2(buf, GAP1);  /* check */


      /* check */
      Lspgetq.lsp_expand_1_2(buf, GAP2);

      tdist[mode] = lsp_get_tdist(wegt, buf, rbuf, fg_sum[mode]);  /* calculate the distortion */

   } /* mode */


   mode_index = lsp_last_select(tdist); /* select the codes */

   /* pack codes for lsp parameters */
   code_ana[0] = (mode_index<<NC0_B) | cand[mode_index];
   code_ana[1] = (tindex1[mode_index]<<NC1_B) | tindex2[mode_index];

   /* reconstruct quantized LSP parameter and check the stabilty */
   Lspgetq.lsp_get_quant(lspcb1, lspcb2, cand[mode_index],
                 tindex1[mode_index], tindex2[mode_index],
                 fg[mode_index],
                 freq_prev,
                 lspq, fg_sum[mode_index]);
}
/*----------------------------------------------------------------------------
 * lsp_pre_select - select the code of first stage lsp codebook
 *----------------------------------------------------------------------------
 */
private int lsp_pre_select(
 float  rbuf[],         /*input : target vetor             */
 float  lspcb1[][/* M */]    /*input : first stage lsp codebook */
)
{
   float FLT_MAX_G729 = Ld8k.FLT_MAX_G729;
   int M = Ld8k.M;
   int NC0 = Ld8k.NC0;

   int  i, j;
   float dmin, dist, temp;

   /* calculate the distortion */

   int cand = 0;           /*output: selected code            */
   dmin= FLT_MAX_G729;
   for(i=0; i<NC0; i++) {
      dist =0.f;
      for(j=0; j<M; j++){
        temp = rbuf[j]-lspcb1[i][j];
        dist += temp * temp;
      }

      if(dist<dmin)
      {
        dmin=dist;
        cand=i;
      }
    }
   return cand;
}

/*----------------------------------------------------------------------------
 * lsp_pre_select_1 - select the code of second stage lsp codebook (lower 0-4)
 *----------------------------------------------------------------------------
 */
private int lsp_select_1(
 float  rbuf[],         /*input : target vector            */
 float  lspcb1[],       /*input : first stage lsp codebook */
 float  wegt[],         /*input : weight coef.             */
 float  lspcb2[][/* M */]    /*input : second stage lsp codebook*/
)
{
   float FLT_MAX_G729 = Ld8k.FLT_MAX_G729;
   int M = Ld8k.M;
   int NC = Ld8k.NC;
   int NC1 = Ld8k.NC1;

   int  j, k1;
   float[]        buf = new float[M];
   float        dist, dmin, tmp;

   for(j=0; j<NC; j++)
        buf[j]=rbuf[j]-lspcb1[j];

   int index = 0;          /*output: selected codebook index     */
   dmin=FLT_MAX_G729;
   for(k1 = 0; k1<NC1; k1++) {
      /* calculate the distortion */
      dist = 0.f;
      for(j=0; j<NC; j++) {
         tmp = buf[j]-lspcb2[k1][j];
         dist += wegt[j] * tmp * tmp;
      }

      if(dist<dmin) {
         dmin = dist;
         index = k1;
      }
   }
   return index;
}

/*----------------------------------------------------------------------------
 * lsp_pre_select_2 - select the code of second stage lsp codebook (higher 5-9)
 *----------------------------------------------------------------------------
 */
private int lsp_select_2(
 float  rbuf[],         /*input : target vector            */
 float  lspcb1[],       /*input : first stage lsp codebook */
 float  wegt[],         /*input : weighting coef.             */
 float  lspcb2[][/* M */]    /*input : second stage lsp codebook*/
)
{
   float FLT_MAX_G729 = Ld8k.FLT_MAX_G729;
   int M = Ld8k.M;
   int NC = Ld8k.NC;
   int NC1 = Ld8k.NC1;

   int  j, k1;
   float[]        buf = new float[M];
   float        dist, dmin, tmp;

   for(j=NC; j<M; j++)
        buf[j]=rbuf[j]-lspcb1[j];


   int index = 0;          /*output: selected codebook index    */
   dmin= FLT_MAX_G729;
   for(k1 = 0; k1<NC1; k1++) {
      dist = 0.0f;
      for(j=NC; j<M; j++) {
        tmp = buf[j] - lspcb2[k1][j];
        dist += wegt[j] * tmp * tmp;
      }

      if(dist<dmin) {
         dmin = dist;
         index = k1;
      }
   }
   return index;
}
/*----------------------------------------------------------------------------
 * lsp_get_tdist - calculate the distortion
 *----------------------------------------------------------------------------
 */
private float lsp_get_tdist(
 float  wegt[],         /*input : weight coef.          */
 float  buf[],          /*input : candidate LSP vector  */
 float  rbuf[],         /*input : target vector         */
 float  fg_sum[]        /*input : present MA prediction coef.  */
)
{
   int M = Ld8k.M;

   int  j;
   float        tmp;

   float tdist = 0.0f;         /*output: distortion            */
   for(j=0; j<M; j++) {
      tmp = (buf[j] - rbuf[j]) * fg_sum[j];
      tdist += wegt[j] * tmp * tmp;
   }
   return tdist;
}

/*----------------------------------------------------------------------------
 * lsp_last_select - select the mode
 *----------------------------------------------------------------------------
 */
private int lsp_last_select(
 float  tdist[]        /*input : distortion         */
)
{
   int mode_index = 0;     /*output: the selected mode  */
   if( tdist[1] < tdist[0] ) mode_index = 1;
   return mode_index;
}
/*----------------------------------------------------------------------------
 * get_wegt - compute lsp weights
 *----------------------------------------------------------------------------
 */
private void get_wegt(
 float  flsp[],         /* input : M LSP parameters */
 float  wegt[]          /* output: M weighting coefficients */
)
{
   float CONST12 = Ld8k.CONST12;
   int M = Ld8k.M;
   float PI04 = Ld8k.PI04;
   float PI92 = Ld8k.PI92;

   int  i;
   float        tmp;

   tmp = flsp[1] - PI04 - 1.0f;
   if (tmp > 0.0f)       wegt[0] = 1.0f;
   else         wegt[0] = tmp * tmp * 10.f + 1.0f;

   for ( i=1; i<M-1; i++ ) {
      tmp = flsp[i+1] - flsp[i-1] - 1.0f;
      if (tmp > 0.0f)    wegt[i] = 1.0f;
      else              wegt[i] = tmp * tmp * 10.f + 1.0f;
   }

   tmp = PI92 - flsp[M-2] - 1.0f;
   if (tmp > 0.0f)       wegt[M-1] = 1.0f;
   else         wegt[M-1] = tmp * tmp * 10.f + 1.0f;

   wegt[4] *= CONST12;
   wegt[5] *= CONST12;
}

}
