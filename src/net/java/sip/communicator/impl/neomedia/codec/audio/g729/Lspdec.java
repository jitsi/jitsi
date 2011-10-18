/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Lspdec
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
 File : LSPDEC.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

private static final int M = Ld8k.M;
private static final int MA_NP = Ld8k.MA_NP;

/* static memory */
/**
 * Previous LSP vector
 */
private final float[][] freq_prev = new float[MA_NP][M];
/**
 * Previous LSP vector(init)
 */
private static final float[/* M */] FREQ_PREV_RESET = { 
 0.285599f,  0.571199f,  0.856798f,  1.142397f,  1.427997f,
 1.713596f,  1.999195f,  2.284795f,  2.570394f,  2.855993f
};     /* PI*(float)(j+1)/(float)(M+1) */

/* static memory for frame erase operation */
/**
 * Previous MA prediction coef
 */
private int prev_ma;
/**
 * Previous LSP vector
 */
private final float[] prev_lsp = new float[M]; 


/**
 * Set the previous LSP vectors.
 */
void lsp_decw_reset()
{
   int  i;

   for(i=0; i<MA_NP; i++)
     Util.copy (FREQ_PREV_RESET, freq_prev[i], M );

   prev_ma = 0;

   Util.copy (FREQ_PREV_RESET, prev_lsp, M );
}

/**
 * LSP main quantization routine
 *
 * @param prm           input : codes of the selected LSP
 * @param prm_offset    input : codes offset
 * @param lsp_q         output: Quantized LSP parameters
 * @param erase         input : frame erase information
 */
private void lsp_iqua_cs(
 int    prm[],        
 int prm_offset,
 float  lsp_q[],       
 int    erase           
)
{
   int NC0 = Ld8k.NC0;
   int NC0_B = Ld8k.NC0_B;
   int NC1 = Ld8k.NC1;
   int NC1_B = Ld8k.NC1_B;
   float[][][] fg = TabLd8k.fg;
   float[][] fg_sum = TabLd8k.fg_sum;
   float[][] fg_sum_inv = TabLd8k.fg_sum_inv;
   float[][] lspcb1 = TabLd8k.lspcb1;
   float[][] lspcb2 = TabLd8k.lspcb2;

   int  mode_index;
   int  code0;
   int  code1;
   int  code2;
   float[] buf = new float[M];


   if(erase==0)                 /* Not frame erasure */
     {
        mode_index = (prm[prm_offset + 0] >>> NC0_B) & 1;
        code0 = prm[prm_offset + 0] & (short)(NC0 - 1);
        code1 = (prm[prm_offset + 1] >>> NC1_B) & (short)(NC1 - 1);
        code2 = prm[prm_offset + 1] & (short)(NC1 - 1);

        Lspgetq.lsp_get_quant(lspcb1, lspcb2, code0, code1, code2, fg[mode_index],
              freq_prev, lsp_q, fg_sum[mode_index]);

        Util.copy(lsp_q, prev_lsp, M );
        prev_ma = mode_index;
     }
   else                         /* Frame erased */
     {
       Util.copy(prev_lsp, lsp_q, M );

        /* update freq_prev */
       Lspgetq.lsp_prev_extract(prev_lsp, buf,
          fg[prev_ma], freq_prev, fg_sum_inv[prev_ma]);
       Lspgetq.lsp_prev_update(buf, freq_prev);
     }
}

/**
 * Decode lsp parameters
 *
 * @param index          input : indexes
 * @param index_offset   input : indexes offset
 * @param lsp_q          output: decoded lsp
 * @param bfi            input : frame erase information
 */
void d_lsp(
    int     index[],  
    int index_offset,
    float   lsp_q[],  
    int     bfi         
)
{
   int i;

   lsp_iqua_cs(index, index_offset, lsp_q,bfi); /* decode quantized information */

   /* Convert LSFs to LSPs */

   for (i=0; i<M; i++ )
     lsp_q[i] = (float)Math.cos(lsp_q[i]);
}

}
