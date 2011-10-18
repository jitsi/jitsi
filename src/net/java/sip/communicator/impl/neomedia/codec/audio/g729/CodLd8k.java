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
 * Functions coder_ld8k and init_coder_ld8k
 *    Coder constant parameters (defined in "ld8k.h")
 * <pre>
 *   L_WINDOW    : LPC analysis window size.
 *   L_NEXT      : Samples of next frame needed for autocor.
 *   L_FRAME     : Frame size.
 *   L_SUBFR     : Sub-frame size.
 *   M           : LPC order.
 *   MP1         : LPC order+1
 *   L_TOTAL     : Total size of speech buffer.
 *   PIT_MIN     : Minimum pitch lag.
 *   PIT_MAX     : Maximum pitch lag.
 *   L_INTERPOL  : Length of filter for interpolation
 * </pre>
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class CodLd8k
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
 File : COD_LD8K.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/




 /*--------------------------------------------------------*
  *         Static memory allocation.                      *
  *--------------------------------------------------------*/

        /* Speech vector */
private final float[] old_speech = new float[L_TOTAL];
float[] new_speech;
int new_speech_offset;
private float[] speech;
private int speech_offset;
private float[] p_window;
private int p_window_offset;

                /* Weighted speech vector */

private final float[] old_wsp = new float[L_FRAME+PIT_MAX];
private float[] wsp;
private int wsp_offset;

                /* Excitation vector */

private final float[] old_exc = new float[L_FRAME+PIT_MAX+L_INTERPOL];
private float[] exc;
private int exc_offset;

        /* Zero vector */

private final float[] ai_zero = new float[L_SUBFR+MP1];
private float[] zero;
private int zero_offset;


                /* Lsp (Line spectral pairs) */
private final float[/* M */] lsp_old =
     { 0.9595f,  0.8413f,  0.6549f,  0.4154f,  0.1423f,
      -0.1423f, -0.4154f, -0.6549f, -0.8413f, -0.9595f};
private final float[] lsp_old_q = new float[M];

        /* Filter's memory */

private final float[] mem_syn = new float[M];
private final float[] mem_w0 = new float[M];
private final float[] mem_w = new float[M];
private final float[] mem_err = new float[M+L_SUBFR];
private float[] error;
private int error_offset;

private float sharp;

private final AcelpCo acelpCo = new AcelpCo();
private final Pwf pwf = new Pwf();
private final QuaGain quaGain = new QuaGain();
private final QuaLsp quaLsp = new QuaLsp();
private final Taming taming = new Taming();

/**
 * Initialization of variables for the encoder.
 * Initialize pointers to speech vector.
 *<pre>
 *   |--------------------|-------------|-------------|------------|
 *     previous speech           sf1           sf2         L_NEXT
 *
 *   <----------------  Total speech vector (L_TOTAL)   ----------->
 *   |   <------------  LPC analysis window (L_WINDOW)  ----------->
 *   |   |               <-- present frame (L_FRAME) -->
 * old_speech            |              <-- new speech (L_FRAME) -->
 *     p_wind            |              |
 *                     speech           |
 *                             new_speech
 * </pre>
 */
void init_coder_ld8k()
{

  new_speech = old_speech;
  new_speech_offset = L_TOTAL - L_FRAME;         /* New speech     */
  speech     = new_speech;                    /* Present frame  */
  speech_offset = new_speech_offset - L_NEXT;
  p_window   = old_speech;
  p_window_offset = L_TOTAL - L_WINDOW;        /* For LPC window */

  /* Initialize static pointers */

  wsp    = old_wsp;
  wsp_offset = PIT_MAX;
  exc    = old_exc;
  exc_offset = PIT_MAX + L_INTERPOL;
  zero   = ai_zero;
  zero_offset = MP1;
  error  = mem_err;
  error_offset = M;

  /* Static vectors to zero */
  Util.set_zero(old_speech, L_TOTAL);
  Util.set_zero(old_exc, PIT_MAX+L_INTERPOL);
  Util.set_zero(old_wsp, PIT_MAX);
  Util.set_zero(mem_syn, M);
  Util.set_zero(mem_w,   M);
  Util.set_zero(mem_w0,  M);
  Util.set_zero(mem_err, M);
  Util.set_zero(zero, zero_offset, L_SUBFR);
  sharp = SHARPMIN;

  /* Initialize lsp_old_q[] */
  Util.copy(lsp_old, lsp_old_q, M);

  quaLsp.lsp_encw_reset();
  taming.init_exc_err();
}

/**
 * Encoder routine ( speech data should be in new_speech ).
 *
 * @param ana   output: analysis parameters
 */
void coder_ld8k(
 int ana[]  
)
{
  /* LPC coefficients */
  float[] r = new float[MP1];                /* Autocorrelations low and hi          */
  float[] A_t = new float[(MP1)*2];          /* A(z) unquantized for the 2 subframes */
  float[] Aq_t = new float[(MP1)*2];         /* A(z)   quantized for the 2 subframes */
  float[] Ap1 = new float[MP1];              /* A(z) with spectral expansion         */
  float[] Ap2 = new float[MP1];              /* A(z) with spectral expansion         */
  float[] A, Aq;               /* Pointer on A_t and Aq_t              */
  int A_offset, Aq_offset;

  /* LSP coefficients */
  float[] lsp_new = new float[M], lsp_new_q = new float[M]; /* LSPs at 2th subframe                 */
  float[] lsf_int = new float[M];               /* Interpolated LSF 1st subframe.       */
  float[] lsf_new = new float[M];

  /* Variable added for adaptive gamma1 and gamma2 of the PWF */

  float[] rc = new float[M];                        /* Reflection coefficients */
  float[] gamma1 = new float[2];             /* Gamma1 for 1st and 2nd subframes */
  float[] gamma2 = new float[2];             /* Gamma2 for 1st and 2nd subframes */

  /* Other vectors */
  float[] synth = new float[L_FRAME];        /* Buffer for synthesis speech        */
  float[] h1 = new float[L_SUBFR];           /* Impulse response h1[]              */
  float[] xn = new float[L_SUBFR];           /* Target vector for pitch search     */
  float[] xn2 = new float[L_SUBFR];          /* Target vector for codebook search  */
  float[] code = new float[L_SUBFR];         /* Fixed codebook excitation          */
  float[] y1 = new float[L_SUBFR];           /* Filtered adaptive excitation       */
  float[] y2 = new float[L_SUBFR];           /* Filtered fixed codebook excitation */
  float[] g_coeff = new float[5];            /* Correlations between xn, y1, & y2:
                                  <y1,y1>, <xn,y1>, <y2,y2>, <xn,y2>,<y1,y2>*/

  /* Scalars */

  int   i, j, i_gamma, i_subfr;
  IntReference iRef = new IntReference();
  int   T_op, t0;
  IntReference t0_min = new IntReference(), t0_max = new IntReference(), t0_frac = new IntReference();
  int   index, taming;
  float gain_pit, gain_code = 0.0f;
  FloatReference _gain_pit = new FloatReference(), _gain_code = new FloatReference();

  int ana_offset = 0;

/*------------------------------------------------------------------------*
 *  - Perform LPC analysis:                                               *
 *       * autocorrelation + lag windowing                                *
 *       * Levinson-durbin algorithm to find a[]                          *
 *       * convert a[] to lsp[]                                           *
 *       * quantize and code the LSPs                                     *
 *       * find the interpolated LSPs and convert to a[] for the 2        *
 *         subframes (both quantized and unquantized)                     *
 *------------------------------------------------------------------------*/

  /* LP analysis */

  Lpc.autocorr(p_window, p_window_offset, M, r);                     /* Autocorrelations */
  Lpc.lag_window(M, r);                             /* Lag windowing    */
  Lpc.levinson(r, A_t, MP1, rc);                   /* Levinson Durbin  */
  Lpc.az_lsp(A_t, MP1, lsp_new, lsp_old);          /* From A(z) to lsp */

  /* LSP quantization */

  quaLsp.qua_lsp(lsp_new, lsp_new_q, ana);
  ana_offset += 2;                         /* Advance analysis parameters pointer */

  /*--------------------------------------------------------------------*
   * Find interpolated LPC parameters in all subframes (both quantized  *
   * and unquantized).                                                  *
   * The interpolated parameters are in array A_t[] of size (M+1)*4     *
   * and the quantized interpolated parameters are in array Aq_t[]      *
   *--------------------------------------------------------------------*/

  Lpcfunc.int_lpc(lsp_old, lsp_new, lsf_int, lsf_new,  A_t);
  Lpcfunc.int_qlpc(lsp_old_q, lsp_new_q, Aq_t);

  /* update the LSPs for the next frame */

  for(i=0; i<M; i++)
  {
    lsp_old[i]   = lsp_new[i];
    lsp_old_q[i] = lsp_new_q[i];
  }

 /*----------------------------------------------------------------------*
  * - Find the weighting factors                                         *
  *----------------------------------------------------------------------*/

  pwf.perc_var(gamma1, gamma2, lsf_int, lsf_new, rc);


 /*----------------------------------------------------------------------*
  * - Find the weighted input speech w_sp[] for the whole speech frame   *
  * - Find the open-loop pitch delay for the whole speech frame          *
  * - Set the range for searching closed-loop pitch in 1st subframe      *
  *----------------------------------------------------------------------*/

  Lpcfunc.weight_az(A_t, 0, gamma1[0], M, Ap1);
  Lpcfunc.weight_az(A_t, 0, gamma2[0], M, Ap2);
  Filter.residu(Ap1, 0, speech, speech_offset, wsp, wsp_offset, L_SUBFR);
  Filter.syn_filt(Ap2, 0, wsp, wsp_offset, wsp, wsp_offset, L_SUBFR, mem_w, 0, 1);

  Lpcfunc.weight_az(A_t, MP1, gamma1[1], M, Ap1);
  Lpcfunc.weight_az(A_t, MP1, gamma2[1], M, Ap2);
  Filter.residu(Ap1, 0, speech, speech_offset + L_SUBFR, wsp, wsp_offset + L_SUBFR, L_SUBFR);
  Filter.syn_filt(Ap2, 0, wsp, wsp_offset + L_SUBFR, wsp, wsp_offset + L_SUBFR, L_SUBFR, mem_w, 0, 1);

  /* Find open loop pitch lag for whole speech frame */

  T_op = Pitch.pitch_ol(wsp, wsp_offset, PIT_MIN, PIT_MAX, L_FRAME);

  /* range for closed loop pitch search in 1st subframe */

  t0_min.value = T_op - 3;
  if (t0_min.value < PIT_MIN) t0_min.value = PIT_MIN;
  t0_max.value = t0_min.value + 6;
  if (t0_max.value > PIT_MAX)
    {
       t0_max.value = PIT_MAX;
       t0_min.value = t0_max.value - 6;
    }

 /*------------------------------------------------------------------------*
  *          Loop for every subframe in the analysis frame                 *
  *------------------------------------------------------------------------*
  *  To find the pitch and innovation parameters. The subframe size is     *
  *  L_SUBFR and the loop is repeated L_FRAME/L_SUBFR times.               *
  *     - find the weighted LPC coefficients                               *
  *     - find the LPC residual signal                                     *
  *     - compute the target signal for pitch search                       *
  *     - compute impulse response of weighted synthesis filter (h1[])     *
  *     - find the closed-loop pitch parameters                            *
  *     - encode the pitch delay                                           *
  *     - update the impulse response h1[] by including fixed-gain pitch   *
  *     - find target vector for codebook search                           *
  *     - codebook search                                                  *
  *     - encode codebook address                                          *
  *     - VQ of pitch and codebook gains                                   *
  *     - find synthesis speech                                            *
  *     - update states of weighting filter                                *
  *------------------------------------------------------------------------*/

  A  = A_t;     /* pointer to interpolated LPC parameters           */
  A_offset = 0;
  Aq = Aq_t;    /* pointer to interpolated quantized LPC parameters */
  Aq_offset = 0;

  i_gamma = 0;

  for (i_subfr = 0;  i_subfr < L_FRAME; i_subfr += L_SUBFR)
  {
   /*---------------------------------------------------------------*
    * Find the weighted LPC coefficients for the weighting filter.  *
    *---------------------------------------------------------------*/

    Lpcfunc.weight_az(A, A_offset, gamma1[i_gamma], M, Ap1);
    Lpcfunc.weight_az(A, A_offset, gamma2[i_gamma], M, Ap2);
    i_gamma++;

   /*---------------------------------------------------------------*
    * Compute impulse response, h1[], of weighted synthesis filter  *
    *---------------------------------------------------------------*/

    for (i = 0; i <= M; i++) ai_zero[i] = Ap1[i];
    Filter.syn_filt(Aq, Aq_offset, ai_zero, 0, h1, 0, L_SUBFR, zero, zero_offset, 0);
    Filter.syn_filt(Ap2, 0, h1, 0, h1, 0, L_SUBFR, zero, zero_offset, 0);

   /*------------------------------------------------------------------------*
    *                                                                        *
    *          Find the target vector for pitch search:                      *
    *          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~                       *
    *                                                                        *
    *              |------|  res[n]                                          *
    *  speech[n]---| A(z) |--------                                          *
    *              |------|       |   |--------| error[n]  |------|          *
    *                    zero -- (-)--| 1/A(z) |-----------| W(z) |-- target *
    *                    exc          |--------|           |------|          *
    *                                                                        *
    * Instead of subtracting the zero-input response of filters from         *
    * the weighted input speech, the above configuration is used to          *
    * compute the target vector. This configuration gives better performance *
    * with fixed-point implementation. The memory of 1/A(z) is updated by    *
    * filtering (res[n]-exc[n]) through 1/A(z), or simply by subtracting     *
    * the synthesis speech from the input speech:                            *
    *    error[n] = speech[n] - syn[n].                                      *
    * The memory of W(z) is updated by filtering error[n] through W(z),      *
    * or more simply by subtracting the filtered adaptive and fixed          *
    * codebook excitations from the target:                                  *
    *     target[n] - gain_pit*y1[n] - gain_code*y2[n]                       *
    * as these signals are already available.                                *
    *                                                                        *
    *------------------------------------------------------------------------*/


    Filter.residu(Aq, Aq_offset, speech, speech_offset + i_subfr, exc, exc_offset + i_subfr, L_SUBFR);   /* LPC residual */

    Filter.syn_filt(Aq, Aq_offset, exc, exc_offset + i_subfr, error, error_offset, L_SUBFR, mem_err, 0, 0);

    Filter.residu(Ap1, 0, error, error_offset, xn, 0, L_SUBFR);

    Filter.syn_filt(Ap2, 0, xn, 0, xn, 0, L_SUBFR, mem_w0, 0, 0);    /* target signal xn[]*/

   /*----------------------------------------------------------------------*
    *                 Closed-loop fractional pitch search                  *
    *----------------------------------------------------------------------*/

    t0 = Pitch.pitch_fr3(exc, exc_offset + i_subfr, xn, h1, L_SUBFR, t0_min.value, t0_max.value,
                              i_subfr, t0_frac);


    index = Pitch.enc_lag3(t0, t0_frac.value, t0_min, t0_max,PIT_MIN,PIT_MAX,i_subfr);

    ana[ana_offset] = index;
    ana_offset++;
    if (i_subfr == 0)
    {
      ana[ana_offset] = PParity.parity_pitch(index);
      ana_offset++;
    }


   /*-----------------------------------------------------------------*
    *   - find unity gain pitch excitation (adaptive codebook entry)  *
    *     with fractional interpolation.                              *
    *   - find filtered pitch exc. y1[]=exc[] convolve with h1[])     *
    *   - compute pitch gain and limit between 0 and 1.2              *
    *   - update target vector for codebook search                    *
    *   - find LTP residual.                                          *
    *-----------------------------------------------------------------*/

    PredLt3.pred_lt_3(exc, exc_offset + i_subfr, t0, t0_frac.value, L_SUBFR);

    Filter.convolve(exc, exc_offset + i_subfr, h1, y1, L_SUBFR);

    gain_pit = Pitch.g_pitch(xn, y1, g_coeff, L_SUBFR);

    /* clip pitch gain if taming is necessary */
    taming = this.taming.test_err(t0, t0_frac.value);

    if( taming == 1){
      if ( gain_pit>  GPCLIP) {
        gain_pit = GPCLIP;
      }
    }

    for (i = 0; i < L_SUBFR; i++)
       xn2[i] = xn[i] - y1[i]*gain_pit;

   /*-----------------------------------------------------*
    * - Innovative codebook search.                       *
    *-----------------------------------------------------*/

    iRef.value = i;
    index = acelpCo.ACELP_codebook(xn2, h1, t0, sharp, i_subfr, code, y2, iRef);
    i = iRef.value;
    ana[ana_offset] = index;        /* Positions index */
    ana_offset++;
    ana[ana_offset] = i;            /* Signs index     */
    ana_offset++;


   /*-----------------------------------------------------*
    * - Quantization of gains.                            *
    *-----------------------------------------------------*/
    CorFunc.corr_xy2(xn, y1, y2, g_coeff);

    _gain_pit.value = gain_pit;
    _gain_code.value = gain_code;
    ana[ana_offset] = quaGain.qua_gain(code, g_coeff, L_SUBFR, _gain_pit, _gain_code, taming );
    gain_pit = _gain_pit.value;
    gain_code = _gain_code.value;
    ana_offset++;

   /*------------------------------------------------------------*
    * - Update pitch sharpening "sharp" with quantized gain_pit  *
    *------------------------------------------------------------*/

    sharp = gain_pit;
    if (sharp > SHARPMAX) sharp = SHARPMAX;
    if (sharp < SHARPMIN) sharp = SHARPMIN;
    /*------------------------------------------------------*
     * - Find the total excitation                          *
     * - find synthesis speech corresponding to exc[]       *
     * - update filters' memories for finding the target    *
     *   vector in the next subframe                        *
     *   (update error[-m..-1] and mem_w0[])                *
     *   update error function for taming process           *
     *------------------------------------------------------*/

    for (i = 0; i < L_SUBFR;  i++)
      exc[exc_offset + i+i_subfr] = gain_pit*exc[exc_offset + i+i_subfr] + gain_code*code[i];

    this.taming.update_exc_err(gain_pit, t0);

    Filter.syn_filt(Aq, Aq_offset, exc, exc_offset + i_subfr, synth, i_subfr, L_SUBFR, mem_syn, 0, 1);

    for (i = L_SUBFR-M, j = 0; i < L_SUBFR; i++, j++)
      {
         mem_err[j] = speech[speech_offset + i_subfr+i] - synth[i_subfr+i];
         mem_w0[j]  = xn[i] - gain_pit*y1[i] - gain_code*y2[i];
      }
    A_offset  += MP1;      /* interpolated LPC parameters for next subframe */
    Aq_offset += MP1;

  }

  /*--------------------------------------------------*
   * Update signal for next frame.                    *
   * -> shift to the left by L_FRAME:                 *
   *     speech[], wsp[] and  exc[]                   *
   *--------------------------------------------------*/

  Util.copy(old_speech, L_FRAME, old_speech, L_TOTAL-L_FRAME);
  Util.copy(old_wsp, L_FRAME, old_wsp, PIT_MAX);
  Util.copy(old_exc, L_FRAME, old_exc, PIT_MAX+L_INTERPOL);
}

}
