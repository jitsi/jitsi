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
class QuaGain
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
 File : QUA_GAIN.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/


/* gain quantizer routines                                                   */

private final float[/* 4 */] past_qua_en={-14.0f,-14.0f,-14.0f,-14.0f};

/**
 * Quantization of pitch and codebook gains
 *
 * @param code          input : fixed codebook vector
 * @param g_coeff       input : correlation factors
 * @param l_subfr       input : fcb vector length
 * @param gain_pit      output: quantized acb gain
 * @param gain_code     output: quantized fcb gain
 * @param tameflag      input : flag set to 1 if taming is needed
 * @return              quantizer index
 */
int qua_gain(           
  float code[],        
  float[] g_coeff,      
  int l_subfr,          
  FloatReference gain_pit,      
  FloatReference gain_code,    
  int tameflag          
)
{
   float FLT_MAX_G729 = Ld8k.FLT_MAX_G729;
   float GP0999 = Ld8k.GP0999;
   float GPCLIP2 = Ld8k.GPCLIP2;
   int NCAN1 = Ld8k.NCAN1;
   int NCAN2 = Ld8k.NCAN2;
   int NCODE2 = Ld8k.NCODE2;
   float[][] gbk1 = TabLd8k.gbk1;
   float[][] gbk2 = TabLd8k.gbk2;
   int[] map1 = TabLd8k.map1;
   int[] map2 = TabLd8k.map2;

 /*
 * MA prediction is performed on the innovation energy (in dB with mean      *
 * removed).                                                                 *
 * An initial predicted gain, g_0, is first determined and the correction    *
 * factor     alpha = gain / g_0    is quantized.                            *
 * The pitch gain and the correction factor are vector quantized and the     *
 * mean-squared weighted error criterion is used in the quantizer search.    *
 *   CS Codebook , fast pre-selection version                                *
 */

   int    i,j, index1 = 0, index2 = 0;
   int    cand1,cand2 ;
   float  gcode0 ;
   float  dist, dist_min, g_pitch, g_code;
   float[]  best_gain = new float[2];
   float tmp;

  /*---------------------------------------------------*
   *-  energy due to innovation                       -*
   *-  predicted energy                               -*
   *-  predicted codebook gain => gcode0[exp_gcode0]  -*
   *---------------------------------------------------*/

   gcode0 = Gainpred.gain_predict( past_qua_en, code, l_subfr);

   /*-- pre-selection --*/
   tmp = -1.f/(4.f*g_coeff[0]*g_coeff[2]-g_coeff[4]*g_coeff[4]) ;
   best_gain[0] = (2.f*g_coeff[2]*g_coeff[1]-g_coeff[3]*g_coeff[4])*tmp ;
   best_gain[1] = (2.f*g_coeff[0]*g_coeff[3]-g_coeff[1]*g_coeff[4])*tmp ;

   if (tameflag == 1){
     if(best_gain[0]> GPCLIP2) best_gain[0] = GPCLIP2;
   }
  /*----------------------------------------------*
   *   - presearch for gain codebook -            *
   *----------------------------------------------*/

   IntReference cand1Ref = new IntReference();
   IntReference cand2Ref = new IntReference();
   gbk_presel(best_gain,cand1Ref,cand2Ref,gcode0) ;
   cand1 = cand1Ref.value;
   cand2 = cand2Ref.value;

   /*-- selection --*/
   dist_min = FLT_MAX_G729;
   if(tameflag == 1) {
       for (i=0;i<NCAN1;i++){
          for(j=0;j<NCAN2;j++){
             g_pitch=gbk1[cand1+i][0]+gbk2[cand2+j][0];
             if(g_pitch < GP0999) {
                 g_code=gcode0*(gbk1[cand1+i][1]+gbk2[cand2+j][1]);
                 dist = g_pitch*g_pitch * g_coeff[0]
                       + g_pitch         * g_coeff[1]
                       + g_code*g_code   * g_coeff[2]
                       + g_code          * g_coeff[3]
                       + g_pitch*g_code  * g_coeff[4] ;
                     if (dist < dist_min){
                        dist_min = dist;
                        index1 = cand1+i ;
                        index2 = cand2+j ;
                     }
                }
          }
        }
    }
    else {
       for (i=0;i<NCAN1;i++){
          for(j=0;j<NCAN2;j++){
             g_pitch=gbk1[cand1+i][0]+gbk2[cand2+j][0];
             g_code=gcode0*(gbk1[cand1+i][1]+gbk2[cand2+j][1]);
             dist = g_pitch*g_pitch * g_coeff[0]
                   + g_pitch         * g_coeff[1]
                   + g_code*g_code   * g_coeff[2]
                   + g_code          * g_coeff[3]
                   + g_pitch*g_code  * g_coeff[4] ;
             if (dist < dist_min){
                dist_min = dist;
                index1 = cand1+i ;
                index2 = cand2+j ;
             }
          }
        }
    }
   gain_pit.value  = gbk1[index1][0]+gbk2[index2][0] ;
   g_code = gbk1[index1][1]+gbk2[index2][1];
   gain_code.value =  g_code * gcode0;
  /*----------------------------------------------*
   * update table of past quantized energies      *
   *----------------------------------------------*/
   Gainpred.gain_update( past_qua_en, g_code);

   return (map1[index1]*NCODE2+map2[index2]);
}

/**
 * Presearch for gain codebook
 *
 * @param best_gain     input : [0] unquantized pitch gain
 *                              [1] unquantized code gain
 * @param cand1         output: index of best 1st stage vector
 * @param cand2         output: index of best 2nd stage vector
 * @param gcode0        input : presearch for gain codebook
 */
private void   gbk_presel(
 float best_gain[],     
 IntReference cand1,          
 IntReference cand2,          
 float gcode0           
)
{
   float INV_COEF = Ld8k.INV_COEF;
   int NCAN1 = Ld8k.NCAN1;
   int NCAN2 = Ld8k.NCAN2;
   int NCODE1 = Ld8k.NCODE1;
   int NCODE2 = Ld8k.NCODE2;
   float[][] coef = TabLd8k.coef;
   float[] thr1 = TabLd8k.thr1;
   float[] thr2 = TabLd8k.thr2;

   int _cand1 = cand1.value, _cand2 = cand2.value;

   float    x,y ;

   x = (best_gain[1]-(coef[0][0]*best_gain[0]+coef[1][1])*gcode0) * INV_COEF ;
   y = (coef[1][0]*(-coef[0][1]+best_gain[0]*coef[0][0])*gcode0
        -coef[0][0]*best_gain[1]) * INV_COEF ;

   if(gcode0>0.0f){
      /* pre select codebook #1 */
      _cand1 = 0 ;
      do{
         if(y>thr1[_cand1]*gcode0) (_cand1)++ ;
         else               break ;
      } while((_cand1)<(NCODE1-NCAN1)) ;
      /* pre select codebook #2 */
      _cand2 = 0 ;
      do{
         if(x>thr2[_cand2]*gcode0) (_cand2)++ ;
         else               break ;
      } while((_cand2)<(NCODE2-NCAN2)) ;
   }
   else{
      /* pre select codebook #1 */
      _cand1 = 0 ;
      do{
         if(y<thr1[_cand1]*gcode0) (_cand1)++ ;
         else               break ;
      } while((_cand1)<(NCODE1-NCAN1)) ;
      /* pre select codebook #2 */
      _cand2 = 0 ;
      do{
         if(x<thr2[_cand2]*gcode0) (_cand2)++ ;
         else               break ;
      } while((_cand2)<(NCODE2-NCAN2)) ;
   }
   cand1.value = _cand1;
   cand2.value = _cand2;
}
}
