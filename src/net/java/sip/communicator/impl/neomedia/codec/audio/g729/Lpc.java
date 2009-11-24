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
 * Lpc analysis routines.
 * NOTE: these routines are assuming that the order is defined as M
 * and that NC is defined as M/2. M has to be even
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Lpc
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
 File : LPC.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/



/**
 * Compute the auto-correlations of windowed speech signal
 *
 * @param x         (i) input signal x[0:L_WINDOW]
 * @param x_offset  (i) input signal offset
 * @param m         (i) LPC order
 * @param r         (o) auto-correlation vector r[0:M]
 */
static void autocorr(
     float[] x,             
     int x_offset,
     int m,              
     float[] r            
)
{
   int L_WINDOW = Ld8k.L_WINDOW;
   float[] hamwindow = TabLd8k.hamwindow;

   float[] y = new float[L_WINDOW];  
   float sum;
   int i, j;


   for (i = 0; i < L_WINDOW; i++)
        y[i] = x[x_offset + i]*hamwindow[i];

   for (i = 0; i <= m; i++)
   {
     sum = 0.0f;
     for (j = 0; j < L_WINDOW-i; j++)
          sum += y[j]*y[j+i];
     r[i] = sum;
   }
   if (r[0]<1.0f) r[0]=1.0f;
}

/**
 * Lag windowing of the autocorrelations
 *
 * @param m (i) LPC order
 * @param r (i/o) correlation
 */
static void lag_window(
     int m,
     float   r[] 
)
{
   float[] lwindow = TabLd8k.lwindow;

   int i;

   for (i=1; i<= m; i++)
     r[i] *= lwindow[i-1];
}

/**
 * Levinson-Durbin recursion to compute LPC parameters.
 *
 * @param r         (i) auto correlation coefficients r[0:M]
 * @param a         (o) lpc coefficients a[0] = 1
 * @param a_offset  (i) lpc coefficients offset
 * @param rc        (o) reflection coefficients rc[0:M-1]
 * @return          prediction error (energy)
 */
static float levinson(        
 float[] r,            
 float[] a,            
 int a_offset,
 float[] rc             
)
{
   int M = Ld8k.M;

   float s, at, err;
   int i, j, l;

   rc[0] = (-r[1])/r[0];
   a[a_offset + 0] = 1.0f;
   a[a_offset + 1] = rc[0];
   err = r[0] + r[1]*rc[0];
   for (i = 2; i <= M; i++)
   {
     s = 0.0f;
     for (j = 0; j < i; j++)
       s += r[i-j]*a[a_offset + j];
     rc[i-1]= (-s)/(err);
     for (j = 1; j <= (i/2); j++)
     {
       l = i-j;
       at = a[a_offset + j] + rc[i-1]*a[a_offset + l];
       a[a_offset + l] += rc[i-1]*a[a_offset + j];
       a[a_offset + j] = at;
     }
     a[a_offset + i] = rc[i-1];
     err += rc[i-1]*s;
     if (err <= 0.0f)
        err = 0.001f;
   }
   return (err);
}

/**                                                                                         *
 * Compute the LSPs from  the LP coefficients a[] using Chebyshev   
 * polynomials. The found LSPs are in the cosine domain with values 
 * in the range from 1 down to -1.                                  
 * The table grid[] contains the points (in the cosine domain) at   
 * which the polynomials are evaluated. The table corresponds to    
 * NO_POINTS frequencies uniformly spaced between 0 and pi.
 *
 * @param a         (i) LP filter coefficients
 * @param a_offset  (i) LP filter coefficients offset
 * @param lsp       (o) Line spectral pairs (in the cosine domain)
 * @param old_lsp   (i) LSP vector from past frame
 */
static void az_lsp(
  float[] a,  
  int a_offset,
  float[] lsp,   
  float[] old_lsp   
)
{
 int GRID_POINTS = Ld8k.GRID_POINTS;
 int M = Ld8k.M;
 int NC = Ld8k.NC;
 float[] grid = TabLd8k.grid;

 int i, j, nf, ip;
 float xlow,ylow,xhigh,yhigh,xmid,ymid,xint;
 float[] coef;

 float[] f1 = new float[NC+1], f2 = new float[NC+1];

 /*-------------------------------------------------------------*
  * find the sum and diff polynomials F1(z) and F2(z)           *
  *      F1(z) = [A(z) + z^11 A(z^-1)]/(1+z^-1)                 *
  *      F2(z) = [A(z) - z^11 A(z^-1)]/(1-z^-1)                 *
  *-------------------------------------------------------------*/

 f1[0] = 1.0f;
 f2[0] = 1.0f;
 for (i=1, j=a_offset+M; i<=NC; i++, j--){
    float ai = a[a_offset + i];
    float aj = a[j];
    f1[i] = ai+aj-f1[i-1];
    f2[i] = ai-aj+f2[i-1];
 }

 /*---------------------------------------------------------------------*
  * Find the LSPs (roots of F1(z) and F2(z) ) using the                 *
  * Chebyshev polynomial evaluation.                                    *
  * The roots of F1(z) and F2(z) are alternatively searched.            *
  * We start by finding the first root of F1(z) then we switch          *
  * to F2(z) then back to F1(z) and so on until all roots are found.    *
  *                                                                     *
  *  - Evaluate Chebyshev pol. at grid points and check for sign change.*
  *  - If sign change track the root by subdividing the interval        *
  *    NO_ITER times and ckecking sign change.                          *
  *---------------------------------------------------------------------*/

 nf=0;      /* number of found frequencies */
 ip=0;      /* flag to first polynomial   */

 coef = f1;  /* start with F1(z) */

 xlow=grid[0];
 ylow = chebyshev(xlow,coef,NC);

 j = 0;
 while ( (nf < M) && (j < GRID_POINTS) )
 {
   j++;
   xhigh = xlow;
   yhigh = ylow;
   xlow = grid[j];
   ylow = chebyshev(xlow,coef,NC);

   if (ylow*yhigh <= 0.0f)  /* if sign change new root exists */
   {
     j--;

     /* divide the interval of sign change by 4 */

     for (i = 0; i < 4; i++)
     {
       xmid = 0.5f*(xlow + xhigh);
       ymid = chebyshev(xmid,coef,NC);
       if (ylow*ymid <= 0.0f)
       {
         yhigh = ymid;
         xhigh = xmid;
       }
       else
       {
         ylow = ymid;
         xlow = xmid;
       }
     }

     /* linear interpolation for evaluating the root */

     xint = xlow - ylow*(xhigh-xlow)/(yhigh-ylow);

     lsp[nf] = xint;    /* new root */
     nf++;

     ip = 1 - ip;         /* flag to other polynomial    */
     coef = (ip != 0) ? f2 : f1;  /* pointer to other polynomial */

     xlow = xint;
     ylow = chebyshev(xlow,coef,NC);
   }
 }

 /* Check if M roots found */
 /* if not use the LSPs from previous frame */

 if ( nf < M)
    for(i=0; i<M; i++)  lsp[i] = old_lsp[i];
}

/**
 * Evaluates the Chebyshev polynomial series.
 *
 *  The polynomial order is
 *     n = m/2   (m is the prediction order)
 *  The polynomial is given by
 *    C(x) = T_n(x) + f(1)T_n-1(x) + ... +f(n-1)T_1(x) + f(n)/2
 *
 * @param x     (i) value of evaluation; x=cos(freq)
 * @param f     (i) coefficients of sum or diff polynomial
 * @param n     (i) order of polynomial
 * @return      the value of the polynomial C(x)
 */
private static float chebyshev(
  float x,       
  float[] f,       
  int n            
)
{
  float b1, b2, b0, x2;
  int i;                              /* for the special case of 10th order */
                                      /*       filter (n=5)                 */
  x2 = 2.0f*x;                      /* x2 = 2.0*x;                        */
  b2 = 1.0f;           /* f[0] */   /*                                    */
  b1 = x2 + f[1];                     /* b1 = x2 + f[1];                    */
  for (i=2; i<n; i++) {               /*                                    */
    b0 = x2*b1 - b2 + f[i];           /* b0 = x2 * b1 - 1. + f[2];          */
    b2 = b1;                          /* b2 = x2 * b0 - b1 + f[3];          */
    b1 = b0;                          /* b1 = x2 * b2 - b0 + f[4];          */
  }                                   /*                                    */
  return (x*b1 - b2 + 0.5f*f[n]);   /* return (x*b1 - b2 + 0.5*f[5]);     */
}

}
