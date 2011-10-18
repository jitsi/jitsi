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
class Ld8k
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
 File : LD8K.H
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/


/*---------------------------------------------------------------------------
 * ld8k.h - include file for all ITU-T 8 kb/s CELP coder routines
 *---------------------------------------------------------------------------
 */

static final float PI =              3.14159265358979323846f;
/**
 * Largest floating point number 
 */
static final float FLT_MAX_G729 =         1.e38f;   
/**
 * Largest floating point number
 */
static final float FLT_MIN_G729 =         -FLT_MAX_G729;    

/**
 * Total size of speech buffer 
 */
static final int L_TOTAL =         240;   
/**
 * LPC update frame size
 */
static final int L_FRAME =         80;     
/**
 * Sub-frame size 
 */
static final int L_SUBFR =         40;   

/*---------------------------------------------------------------------------*
 * Constants for bitstream packing                                           *
 *---------------------------------------------------------------------------*/
/**
 * Definition of one-bit in bit-stream.
 */
static final short BIT_1 =     0x0081;
/**
 * Definition of zero-bit in bit-stream.
 */
static final short BIT_0 =     0x007f; 
/**
 * Definition of frame erasure flag.
 */
static final short SYNC_WORD = 0x6b21;
/**
 * Size of bitstream frame.
 */
static final short SIZE_WORD =       80; 
/**
 * Number of parameters per 10 ms frame. 
 */
static final int PRM_SIZE =        11;     
/**
 * Bits per frame.
 */
static final int SERIAL_SIZE =     82;     

/*---------------------------------------------------------------------------*
 * Constants for lpc analysis and lsp quantizer.                             *
 *---------------------------------------------------------------------------*/
/**
 * LPC analysis window size.   
 */
static final int L_WINDOW =        240;    
/**
 * Samples of next frame needed for LPC ana.
 */
static final int L_NEXT =          40;     

/**
 * LPC order.
 */
static final int M =               10;     
/**
 * LPC order+1. 
 */
static final int MP1 =            (M+1); 
/**
 * Resolution of lsp search.
 */
static final int GRID_POINTS =     60;     

/**
 * MA prediction order for LSP.
 */
static final int MA_NP =           4;     
/**
 * Number of modes for MA prediction.
 */
static final int MODE =            2;      
/**
 * Number of bits in first stage.
 */
static final int NC0_B =           7;    
/**
 * Number of entries in first stage.
 */
static final int NC0 =          (1<<NC0_B);
/**
 * Number of bits in second stage.
 */
static final int NC1_B =           5;       
/**
 * Number of entries in second stage.
 */
static final int NC1 =          (1<<NC1_B); 
/**
 * LPC order / 2.
 */
static final int NC =              (M/2);   

static final float L_LIMIT =         0.005f;   
static final float M_LIMIT =         3.135f;   
static final float GAP1 =            0.0012f; 
static final float GAP2 =            0.0006f;  
static final float GAP3 =            0.0392f;  
/**
 * pi*0.04 
 */
static final float PI04 =            PI*0.04f;   
/**
 * pi*0.92 
 */
static final float PI92 =            PI*0.92f;   
static final float CONST12 =         1.2f;

/*-------------------------------------------------------------------------
 *  pwf constants
 *-------------------------------------------------------------------------
 */

static final float THRESH_L1 =   -1.74f;
static final float THRESH_L2 =   -1.52f;
static final float THRESH_H1 =   0.65f;
static final float THRESH_H2 =   0.43f;
static final float GAMMA1_0 =    0.98f;
static final float GAMMA2_0_H =  0.7f;
static final float GAMMA2_0_L =  0.4f;
static final float GAMMA1_1 =    0.94f;
static final float GAMMA2_1 =    0.6f;
static final float ALPHA =       -6.0f;
static final float BETA =        1.0f;

/*----------------------------------------------------------------------------
 * Constants for long-term predictor
 *----------------------------------------------------------------------------
 */
/**
 * Minimum pitch lag in samples
 */
static final int PIT_MIN =         20;     
/**
 * Maximum pitch lag in samples
 */
static final int PIT_MAX =         143;    
/**
 * Length of filter for interpolation.
 */
static final int L_INTERPOL =      (10+1);
/**
 * Length for pitch interpolation
 */
static final int L_INTER10 =       10;   
/**
 * upsampling ration for pitch search  
 */
static final int L_INTER4 =        4;   
/**
 * resolution of fractional delays
 */
static final int UP_SAMP =         3;      
/**
 * Threshold to favor smaller pitch lags 
 */
static final float THRESHPIT =    0.85f;  
/**
 * maximum adaptive codebook gain 
 */
static final float GAIN_PIT_MAX = 1.2f;     
static final int FIR_SIZE_ANA = (UP_SAMP*L_INTER4+1);
static final int FIR_SIZE_SYN = (UP_SAMP*L_INTER10+1);

/*---------------------------------------------------------------------------*
 * Constants for fixed codebook.                                            *
 *---------------------------------------------------------------------------*/
/**
 * Size of correlation matrix  
 */
static final int DIM_RR =  616; 
/**
 * Number of positions for each pulse 
 */
static final int NB_POS =  8;   
/**
 * Step betweem position of the same pulse.
 */
static final int STEP =    5;  
/**
 * Size of vectors for cross-correlation between 2 pulses
 */
static final int MSIZE =   64;  
/**
 * Maximum value of pitch sharpening
 */
static final float SHARPMAX =        0.7945f;  
/**
 * minimum value of pitch sharpening
 */
static final float SHARPMIN =        0.2f;    

/*--------------------------------------------------------------------------*
 * Example values for threshold and approximated worst case complexity:     *
 *                                                                          *
 *     threshold=0.40   maxtime= 75   extra=30   Mips =  6.0                *
 *--------------------------------------------------------------------------*/
static final float THRESHFCB =       0.40f;
static final int MAX_TIME =        75;      

/*--------------------------------------------------------------------------*
 * Constants for taming procedure.                           *
 *--------------------------------------------------------------------------*/
/**
 * Maximum pitch gain if taming is needed
 */
static final float GPCLIP =      0.95f;   
/**
 * Maximum pitch gain if taming is needed
 */
static final float GPCLIP2 =     0.94f;   
/**
 * Maximum pitch gain if taming is needed 
 */
static final float GP0999 =      0.9999f;
/**
 * Error threshold taming
 */
static final float THRESH_ERR =  60000.0f;  
static final float INV_L_SUBFR = (1.0f/(float)L_SUBFR); /* =0.025 */
/*-------------------------------------------------------------------------
 * gain quantizer  constants
 *-------------------------------------------------------------------------
 */
/**
 * Average innovation energy
 */
static final float MEAN_ENER =        36.0f;   
/**
 * Number of Codebook-bit  
 */
static final int NCODE1_B =  3;               
/**
 * Number of Codebook-bit      
 */
static final int NCODE2_B =  4;               
/**
 * Codebook 1 size     
 */
static final int NCODE1 =    (1<<NCODE1_B);   
/**
 * Codebook 2 size
 */
static final int NCODE2 =    (1<<NCODE2_B);   
/**
 * Pre-selecting order for #1
 */
static final int NCAN1 =            4;      
/**
 * Pre-selecting order for #2
 */
static final int NCAN2 =            8;     
static final float INV_COEF =   -0.032623f;

/*---------------------------------------------------------------------------
 * Constants for postfilter.
 *---------------------------------------------------------------------------
 */
/* short term pst parameters :  */
/** 
 * denominator weighting factor
 */
static final float GAMMA1_PST =      0.7f;  
/**
 * numerator  weighting factor
 */
static final float GAMMA2_PST =      0.55f;    
/** 
 * impulse response length
 */
static final int LONG_H_ST =       20;     
/** 
 * tilt weighting factor when k1>0
 */
static final float GAMMA3_PLUS =     0.2f;     
/** 
 * tilt weighting factor when k1<0
 */
static final float GAMMA3_MINUS =    0.9f;    

/* long term pst parameters :   */
/**
 * Sub-frame size + 1
 */
static final int L_SUBFRP1 = (L_SUBFR + 1);
/**
 * resolution for fractionnal delay   
 */
static final int F_UP_PST =        8;       
/**
 * length of short interp. subfilters  
 */
static final int LH2_S =           4;       
/**
 * length of long interp. subfilters
 */
static final int LH2_L =           16;      
/**
 * threshold LT pst switch off       
 */
static final float THRESCRIT =       0.5f;     
/**
 * LT weighting factor          
 */
static final float GAMMA_G =         0.5f;     
/**
 * gain adjustment factor             
 */
static final float AGC_FAC =         0.9875f; 
/**
 * gain adjustment factor                 
 */
static final float AGC_FAC1 =         (1.f - AGC_FAC);    
static final int LH_UP_S =         (LH2_S/2);
static final int LH_UP_L =         (LH2_L/2);
static final int LH2_L_P1 =    (LH2_L + 1);
/**
 * LT gain minimum         
 */
static final float MIN_GPLT =     (1.f / (1.f + GAMMA_G));  

/* Array sizes */
static final int MEM_RES2 = (PIT_MAX + 1 + LH_UP_L);
static final int SIZ_RES2 = (MEM_RES2 + L_SUBFR);
static final int SIZ_Y_UP =  ((F_UP_PST-1) * L_SUBFRP1);
static final int SIZ_TAB_HUP_L = ((F_UP_PST-1) * LH2_L);
static final int SIZ_TAB_HUP_S = ((F_UP_PST-1) * LH2_S);
}
