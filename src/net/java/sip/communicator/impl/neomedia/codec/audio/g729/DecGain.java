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
class DecGain
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
 File : DEC_GAIN.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/
private final float[/* 4 */] past_qua_en={-14.0f,-14.0f,-14.0f,-14.0f};

/**
 * Decode the adaptive and fixed codebook gains.
 *
 * @param index         input : quantizer index
 * @param code          input : fixed code book vector
 * @param l_subfr       input : subframe size
 * @param bfi           input : bad frame indicator good = 0
 * @param gain_pit      output: quantized acb gain
 * @param gain_code     output: quantized fcb gain
 */
void dec_gain(
 int index,        
 float code[],        
 int l_subfr,           
 int bfi,              
 FloatReference gain_pit,    
 FloatReference gain_code      
)
{
   int NCODE2 = Ld8k.NCODE2;
   float[][] gbk1 = TabLd8k.gbk1;
   float[][] gbk2 = TabLd8k.gbk2;
   int[] imap1 = TabLd8k.imap1;
   int[] imap2 = TabLd8k.imap2;

   int    index1,index2;
   float  gcode0, g_code;

   /*----------------- Test erasure ---------------*/
   if (bfi != 0)
     {
        gain_pit.value *= 0.9f;
        if(gain_pit.value > 0.9f) gain_pit.value=0.9f;
        gain_code.value *= 0.98f;

     /*----------------------------------------------*
      * update table of past quantized energies      *
      *                              (frame erasure) *
      *----------------------------------------------*/
      Gainpred.gain_update_erasure(past_qua_en);

        return;
     }

   /*-------------- Decode pitch gain ---------------*/

   index1 = imap1[index/NCODE2] ;
   index2 = imap2[index%NCODE2] ;
   gain_pit.value = gbk1[index1][0]+gbk2[index2][0] ;

   /*-------------- Decode codebook gain ---------------*/

  /*---------------------------------------------------*
   *-  energy due to innovation                       -*
   *-  predicted energy                               -*
   *-  predicted codebook gain => gcode0[exp_gcode0]  -*
   *---------------------------------------------------*/

   gcode0 = Gainpred.gain_predict( past_qua_en, code, l_subfr);

  /*-----------------------------------------------------------------*
   * *gain_code = (gbk1[indice1][1]+gbk2[indice2][1]) * gcode0;      *
   *-----------------------------------------------------------------*/

   g_code = gbk1[index1][1]+gbk2[index2][1];
   gain_code.value =  g_code * gcode0;

  /*----------------------------------------------*
   * update table of past quantized energies      *
   *----------------------------------------------*/

   Gainpred.gain_update( past_qua_en, g_code);
}
}
