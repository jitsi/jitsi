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
class Lpcfunc
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
 File : LPCFUNC.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/

/*-----------------------------------------------------------------------------
 * lsp_az - convert LSPs to predictor coefficients a[]
 *-----------------------------------------------------------------------------
 */
private static void lsp_az(
 float[] lsp,            /* input : lsp[0:M-1] */
 float[] a,               /* output: predictor coeffs a[0:M], a[0] = 1. */
 int a_offset
)
{
  int M = Ld8k.M;
  int NC = Ld8k.NC;

  float[] f1 = new float[NC+1], f2 = new float[NC+1];
  int i,j;


  get_lsp_pol(lsp, 0,f1);
  get_lsp_pol(lsp, 1,f2);

  for (i = NC; i > 0; i--)
  {
    f1[i] += f1[i-1];
    f2[i] -= f2[i-1];
  }
  a[a_offset + 0] = 1.0f;
  for (i = 1, j = M; i <= NC; i++, j--)
  {
    a[a_offset + i] = 0.5f*(f1[i] + f2[i]);
    a[a_offset + j] = 0.5f*(f1[i] - f2[i]);
  }
}


/*----------------------------------------------------------------------------
 * get_lsp_pol - find the polynomial F1(z) or F2(z) from the LSFs
 *----------------------------------------------------------------------------
 */
private static void get_lsp_pol(
   float lsp[],           /* input : line spectral freq. (cosine domain)  */
   int lsp_offset,
   float f[]              /* output: the coefficients of F1 or F2 */
)
{
  int NC = Ld8k.NC;

  float b;
  int   i,j;

  f[0] = 1.0f;
  b = -2.0f*lsp[lsp_offset + 0];
  f[1] = b;
  for (i = 2; i <= NC; i++)
  {
    b = -2.0f*lsp[lsp_offset + 2*i-2];
    f[i] = b*f[i-1] + 2.0f*f[i-2];
    for (j = i-1; j > 1; j--)
      f[j] += b*f[j-1] + f[j-2];
    f[1] += b;
  }
}

/*----------------------------------------------------------------------------
 * lsf_lsp - convert from lsf[0..M-1 to lsp[0..M-1]
 *----------------------------------------------------------------------------
 */
static void lsf_lsp(
 float lsf[],          /* input :  lsf */
 float lsp[],          /* output: lsp */
 int m
)
{
    int     i;
    for ( i = 0; i < m; i++ )
        lsp[i] = (float)Math.cos((double)lsf[i]);
}

/*----------------------------------------------------------------------------
 * lsp_lsf - convert from lsp[0..M-1 to lsf[0..M-1]
 *----------------------------------------------------------------------------
 */
static void lsp_lsf(
 float lsp[],          /* input :  lsp coefficients */
 float lsf[],          /* output:  lsf (normalized frequencies */
 int m
)
{
    int     i;

    for ( i = 0; i < m; i++ )
        lsf[i] = (float)Math.acos((double)lsp[i]);
}


/*---------------------------------------------------------------------------
 * weigh_az:  Weighting of LPC coefficients  ap[i]  =  a[i] * (gamma ** i)
 *---------------------------------------------------------------------------
 */
static void weight_az(
 float[] a,              /* input : lpc coefficients a[0:m] */
 int a_offset,
 float gamma,           /* input : weighting factor */
 int m,                  /* input : filter order */
 float[] ap             /* output: weighted coefficients ap[0:m] */
)
{
    float fac;
    int i;

    ap[0] = a[a_offset + 0];
    fac = gamma;
    for (i = 1; i <m; i++) {
        ap[i] = fac * a[a_offset + i];
        fac *= gamma;
    }
    ap[m] = fac * a[a_offset + m];
}



/*-----------------------------------------------------------------------------
 * int_qlpc -  interpolated M LSP parameters and convert to M+1 LPC coeffs
 *-----------------------------------------------------------------------------
 */
static void int_qlpc(
 float lsp_old[],       /* input : LSPs for past frame (0:M-1) */
 float lsp_new[],       /* input : LSPs for present frame (0:M-1) */
 float az[]             /* output: filter parameters in 2 subfr (dim 2(m+1)) */
)
{
  int M = Ld8k.M;

  int i;
  float[] lsp = new float[M];

  for (i = 0; i < M; i++)
    lsp[i] = lsp_old[i]*0.5f + lsp_new[i]*0.5f;

  lsp_az(lsp, az, 0);
  lsp_az(lsp_new, az, M+1);
}
/*-----------------------------------------------------------------------------
 * int_lpc -  interpolated M LSP parameters and convert to M+1 LPC coeffs
 *-----------------------------------------------------------------------------
 */
static void int_lpc(
 float lsp_old[],       /* input : LSPs for past frame (0:M-1) */
 float lsp_new[],       /* input : LSPs for present frame (0:M-1) */
 float lsf_int[],        /* output: interpolated lsf coefficients */
 float lsf_new[],       /* input : LSFs for present frame (0:M-1) */
 float az[]             /* output: filter parameters in 2 subfr (dim 2(m+1)) */
)
{
    int M = Ld8k.M;

    int i;
    float[] lsp = new float[M];


    for (i = 0; i < M; i++)
        lsp[i] = lsp_old[i]*0.5f + lsp_new[i]*0.5f;

    lsp_az(lsp, az, 0);

    lsp_lsf(lsp, lsf_int, M);
    lsp_lsf(lsp_new, lsf_new, M);
}
}
