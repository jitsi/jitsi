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
 * Functions init_decod_ld8k  and decod_ld8k.
 * <pre>
 * Decoder constant parameters (defined in "ld8k.h")
 *   L_FRAME     : Frame size.
 *   L_SUBFR     : Sub-frame size.
 *   M           : LPC order.
 *   MP1         : LPC order+1
 *   PIT_MIN     : Minimum pitch lag.
 *   PIT_MAX     : Maximum pitch lag.
 *   L_INTERPOL  : Length of filter for interpolation
 *   PRM_SIZE    : Size of vector containing analysis parameters
 * </pre>
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class DecLd8k
    extends Ld8k
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
 File : DEC_LD8K.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/

/*--------------------------------------------------------*
 *         Static memory allocation.                      *
 *--------------------------------------------------------*/

/**
 * Excitation vector
 */
private final float[] old_exc = new float[L_FRAME+PIT_MAX+L_INTERPOL];

/**
 * Excitation vector
 */
private float[] exc;

/**
 * Excitation vector offset
 */
private int exc_offset;

/**
 * Lsp (Line spectral pairs)
 */
private final float[/* M */] lsp_old={
       0.9595f,  0.8413f,  0.6549f,  0.4154f,  0.1423f,
      -0.1423f, -0.4154f, -0.6549f, -0.8413f, -0.9595f};

/** 
 * Filter's memory
 */
private final float[] mem_syn = new float[M];      

/**
 * pitch sharpening of previous fr
 */
private float sharp ; 

/** 
* integer delay of previous frame
*/
private int old_t0;   

/**
 * fixed codebook gain
 */
private final FloatReference gain_code = new FloatReference();  

/**
 * adaptive codebook gain
 */
private final FloatReference gain_pitch = new FloatReference();       

private final DecGain decGain = new DecGain();
private final Lspdec lspdec = new Lspdec();

/**
 * Initialization of variables for the decoder section.
 */
void init_decod_ld8k()
{
    /* Initialize static pointer */
    exc    = old_exc;
    exc_offset = PIT_MAX + L_INTERPOL;

    /* Static vectors to zero */
    Util.set_zero(old_exc,PIT_MAX + L_INTERPOL);
    Util.set_zero(mem_syn, M);

    sharp = SHARPMIN;
    old_t0 = 60;
    gain_code.value = 0.f;
    gain_pitch.value = 0.f;

    lspdec.lsp_decw_reset();
}

/**
 * Decoder
 *
 * @param parm          input : synthesis parameters (parm[0] = bfi)
 * @param voicing       input : voicing decision from previous frame
 * @param synth         output: synthesized speech
 * @param synth_offset  input : synthesized speech offset
 * @param A_t           output: two sets of A(z) coefficients length=2*MP1
 * @return              output: integer delay of first subframe
 */
int decod_ld8k(
 int parm[],        
 int voicing,          
 float synth[],         
 int synth_offset,
 float A_t[]          
)
{
   int parm_offset = 0;

   int t0_first = 0;          /* output: integer delay of first subframe            */
   float[] Az;                  /* Pointer to A_t (LPC coefficients)  */
   int Az_offset;
   float[] lsp_new = new float[M];           /* LSPs                               */
   float[] code = new float[L_SUBFR];        /* algebraic codevector               */

  /* Scalars */
  int   i, i_subfr;
  IntReference   t0 = new IntReference(), t0_frac = new IntReference();
  int index;

  int bfi;
  int bad_pitch;

  /* Test bad frame indicator (bfi) */

  bfi = parm[parm_offset];
  parm_offset++;

  /* Decode the LSPs */

  lspdec.d_lsp(parm, parm_offset, lsp_new, bfi);
  parm_offset += 2;             /* Advance synthesis parameters pointer */

  /* Interpolation of LPC for the 2 subframes */

  Lpcfunc.int_qlpc(lsp_old, lsp_new, A_t);

  /* update the LSFs for the next frame */

  Util.copy(lsp_new, lsp_old, M);

/*------------------------------------------------------------------------*
 *          Loop for every subframe in the analysis frame                 *
 *------------------------------------------------------------------------*
 * The subframe size is L_SUBFR and the loop is repeated L_FRAME/L_SUBFR  *
 *  times                                                                 *
 *     - decode the pitch delay                                           *
 *     - decode algebraic code                                            *
 *     - decode pitch and codebook gains                                  *
 *     - find the excitation and compute synthesis speech                 *
 *------------------------------------------------------------------------*/

  Az = A_t;            /* pointer to interpolated LPC parameters */
  Az_offset = 0;

  for (i_subfr = 0; i_subfr < L_FRAME; i_subfr += L_SUBFR) {

   index = parm[parm_offset];          /* pitch index */
   parm_offset++;

   if (i_subfr == 0) {      /* if first subframe */
     i = parm[parm_offset];             /* get parity check result */
     parm_offset++;
     bad_pitch = bfi+ i;
     if( bad_pitch == 0)
     {
       DecLag3.dec_lag3(index, PIT_MIN, PIT_MAX, i_subfr, t0, t0_frac);
       old_t0 = t0.value;
     }
     else                     /* Bad frame, or parity error */
     {
       t0.value  =  old_t0;
       t0_frac.value = 0;
       old_t0++;
       if( old_t0> PIT_MAX) {
           old_t0 = PIT_MAX;
       }
     }
      t0_first = t0.value;         /* If first frame */
   }
   else                       /* second subframe */
   {
     if( bfi == 0)
     {
       DecLag3.dec_lag3(index, PIT_MIN, PIT_MAX, i_subfr, t0, t0_frac);
       old_t0 = t0.value;
     }
     else
     {
       t0.value  =  old_t0;
       t0_frac.value = 0;
       old_t0++;
       if( old_t0 >PIT_MAX) {
           old_t0 = PIT_MAX;
       }
     }
   }


   /*-------------------------------------------------*
    *  - Find the adaptive codebook vector.            *
    *--------------------------------------------------*/

   PredLt3.pred_lt_3(exc, exc_offset + i_subfr, t0.value, t0_frac.value, L_SUBFR);

   /*-------------------------------------------------------*
    * - Decode innovative codebook.                         *
    * - Add the fixed-gain pitch contribution to code[].    *
    *-------------------------------------------------------*/

   if(bfi != 0) {            /* Bad Frame Error Concealment */
     parm[parm_offset + 0] = (Util.random_g729() & 0x1fff);      /* 13 bits random*/
     parm[parm_offset + 1]= (Util.random_g729() & 0x000f);      /*  4 bits random */
   }

   DeAcelp.decod_ACELP(parm[parm_offset + 1], parm[parm_offset + 0], code);
   parm_offset +=2;
   for (i = t0.value; i < L_SUBFR; i++)   code[i] += sharp * code[i-t0.value];

   /*-------------------------------------------------*
    * - Decode pitch and codebook gains.              *
    *-------------------------------------------------*/

   index = parm[parm_offset];          /* index of energy VQ */
   parm_offset++;
   decGain.dec_gain(index, code, L_SUBFR, bfi, gain_pitch, gain_code);

   /*-------------------------------------------------------------*
    * - Update pitch sharpening "sharp" with quantized gain_pitch *
    *-------------------------------------------------------------*/

   sharp = gain_pitch.value;
   if (sharp > SHARPMAX) sharp = SHARPMAX;
   if (sharp < SHARPMIN) sharp = SHARPMIN;

   /*-------------------------------------------------------*
    * - Find the total excitation.                          *
    *-------------------------------------------------------*/

   if(bfi != 0 ) {
     if(voicing  == 0) {     /* for unvoiced frame */
         for (i = 0; i < L_SUBFR;  i++) {
            exc[exc_offset + i+i_subfr] = gain_code.value*code[i];
         }
      } else {               /* for voiced frame */
         for (i = 0; i < L_SUBFR;  i++) {
            exc[exc_offset + i+i_subfr] = gain_pitch.value*exc[exc_offset + i+i_subfr];
         }
      }
    } else {                  /* No frame errors */
      for (i = 0; i < L_SUBFR;  i++) {
         exc[exc_offset + i+i_subfr] = gain_pitch.value*exc[exc_offset + i+i_subfr] + gain_code.value*code[i];
      }
    }

    /*-------------------------------------------------------*
     * - Find synthesis speech corresponding to exc[].       *
     *-------------------------------------------------------*/

    Filter.syn_filt(Az, Az_offset, exc, exc_offset + i_subfr, synth, synth_offset + i_subfr, L_SUBFR, mem_syn, 0, 1);

    Az_offset  += MP1;        /* interpolated LPC parameters for next subframe */
  }

   /*--------------------------------------------------*
    * Update signal for next frame.                    *
    * -> shift to the left by L_FRAME  exc[]           *
    *--------------------------------------------------*/
  Util.copy(old_exc, L_FRAME, old_exc, PIT_MAX+L_INTERPOL);
  return t0_first;
}
}
