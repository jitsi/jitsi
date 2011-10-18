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
 * Taming functions.
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Taming
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
 File : TAMING.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

private final float[] exc_err = new float[4];

void init_exc_err()
{
  int i;
  for(i=0; i<4; i++) exc_err[i] = 1.0f;
}

/**
 * Computes the accumulated potential error in the     
 * adaptive codebook contribution
 *
 * @param t0        (i) integer part of pitch delay
 * @param t0_frac   (i) fractional part of pitch delay
 * @return          flag set to 1 if taming is necessary
 */
int test_err( 
int t0,    
int t0_frac  
)
{
    float INV_L_SUBFR = Ld8k.INV_L_SUBFR;
    int L_INTER10 = Ld8k.L_INTER10;
    int L_SUBFR = Ld8k.L_SUBFR;
    float THRESH_ERR = Ld8k.THRESH_ERR; 

    int i, t1, zone1, zone2, flag;
    float maxloc;

    t1 = (t0_frac > 0) ? (t0+1) : t0;

    i = t1 - L_SUBFR - L_INTER10;
    if(i < 0) i = 0;
    zone1 = (int) ( (float)i * INV_L_SUBFR);

    i = t1 + L_INTER10 - 2;
    zone2 = (int)( (float)i * INV_L_SUBFR);

    maxloc = -1.0f;
    flag = 0 ;
    for(i=zone2; i>=zone1; i--) {
        if(exc_err[i] > maxloc) maxloc = exc_err[i];
    }
    if(maxloc > THRESH_ERR) {
        flag = 1;
    }
    return(flag);
}

/**
 * Maintains the memory used to compute the error
 * function due to an adaptive codebook mismatch between encoder and
 * decoder
 *
 * @param gain_pit      (i) pitch gain
 * @param t0            (i) integer part of pitch delay
 */
void update_exc_err(
 float gain_pit, 
 int t0
)
{
    float INV_L_SUBFR = Ld8k.INV_L_SUBFR;
    int L_SUBFR = Ld8k.L_SUBFR;

    int i, zone1, zone2, n;
    float worst, temp;

    worst = (float)-1.;

    n = t0- L_SUBFR;
    if(n < 0) {
        temp = 1.0f + gain_pit * exc_err[0];
        if(temp > worst) worst = temp;
        temp = 1.0f + gain_pit * temp;
        if(temp > worst) worst = temp;
    }

    else {
        zone1 = (int) ((float)n * INV_L_SUBFR);

        i = t0 - 1;
        zone2 = (int)((float)i * INV_L_SUBFR);

        for(i = zone1; i <= zone2; i++) {
            temp = 1.0f + gain_pit * exc_err[i];
            if(temp > worst) worst = temp;
        }
    }

    for(i=3; i>=1; i--) exc_err[i] = exc_err[i-1];
    exc_err[0] = worst;
}

}
