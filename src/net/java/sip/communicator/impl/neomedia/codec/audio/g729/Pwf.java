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
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Pwf
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
 File : PWF.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/


private int     smooth = 1;
private final float[/* 2 */]   lar_old = {0.0f, 0.0f};

/**
 * Adaptive bandwidth expansion for perceptual weighting filter
 *
 * @param gamma1        output: gamma1 value
 * @param gamma2        output: gamma2 value
 * @param lsfint        input : Interpolated lsf vector : 1st subframe
 * @param lsfnew        input : lsf vector : 2nd subframe
 * @param r_c           input : Reflection coefficients
 */
void perc_var(
 float[] gamma1,         
 float[] gamma2,        
 float[] lsfint,         
 float[] lsfnew,        
 float[] r_c             
)
{
    float ALPHA = Ld8k.ALPHA;
    float BETA = Ld8k.BETA;
    float GAMMA1_0 = Ld8k.GAMMA1_0;
    float GAMMA1_1 = Ld8k.GAMMA1_1;
    float GAMMA2_0_H = Ld8k.GAMMA2_0_H;
    float GAMMA2_0_L = Ld8k.GAMMA2_0_L;
    float GAMMA2_1 = Ld8k.GAMMA2_1;
    int M = Ld8k.M;
    float THRESH_H1 = Ld8k.THRESH_H1;
    float THRESH_H2 = Ld8k.THRESH_H2;
    float THRESH_L1 = Ld8k.THRESH_L1;
    float THRESH_L2 = Ld8k.THRESH_L2;

    float[]    lar = new float[4];
    float[]   lsf;
    float    critlar0, critlar1;
    float    d_min, temp;
    int      i, k;


    float[] lar_new = lar;
    int lar_new_offset = 2;

    /* reflection coefficients --> lar */
    for (i=0; i<2; i++)
        lar_new[lar_new_offset + i] = (float)Math.log10( (double)( ( 1.0f + r_c[i]) / (1.0f - r_c[i])));

    /* Interpolation of lar for the 1st subframe */
    for (i=0; i<2; i++) {
        lar[i] = 0.5f * (lar_new[lar_new_offset + i] + lar_old[i]);
        lar_old[i] = lar_new[lar_new_offset + i];
    }

    for (k=0; k<2; k++) {    /* LOOP : gamma2 for 1st to 2nd subframes */

      /* ----------------------------------------------------- */
      /*   First criterion based on the first two lars         */
      /*                                                       */
      /* smooth == 1  ==>  gamma2 is set to 0.6                    */
      /*                   gamma1 is set to 0.94               */
      /*                                                       */
      /* smooth == 0  ==>  gamma2 can vary from 0.4 to 0.7     */
      /*                   (gamma2 = -6.0 dmin + 1.0)          */
      /*                   gamma1 is set to 0.98               */
      /* ----------------------------------------------------- */
        critlar0 = lar[2*k];
        critlar1 = lar[2*k+1];

        if (smooth != 0) {
            if ((critlar0 <THRESH_L1 )&&(critlar1 > THRESH_H1)) smooth = 0;
        }
        else {
            if ((critlar0 > THRESH_L2)||(critlar1 < THRESH_H2)) smooth = 1;
        }

        if (smooth == 0) {
     /* ------------------------------------------------------ */
     /* Second criterion based on the minimum distance between */
     /* two successives lsfs                                   */
     /* ------------------------------------------------------ */
            gamma1[k] = GAMMA1_0;
            if (k == 0) lsf = lsfint;
            else lsf = lsfnew;
            d_min = lsf[1] - lsf[0];
            for (i=1; i<M-1; i++) {
                temp = lsf[i+1] - lsf[i];
                if (temp < d_min) d_min = temp;
            }

            gamma2[k] =  ALPHA * d_min + BETA;

            if (gamma2[k] > GAMMA2_0_H) gamma2[k] = GAMMA2_0_H;
            if (gamma2[k] < GAMMA2_0_L) gamma2[k] = GAMMA2_0_L;
        }
        else {
            gamma1[k] = GAMMA1_1;
            gamma2[k] = GAMMA2_1;;
        }
    }
}
}
