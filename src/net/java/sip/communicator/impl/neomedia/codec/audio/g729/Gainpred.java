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
class Gainpred
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
 File : GAINPRED.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/
    
/**
 * MA prediction is performed on the innovation energy (in dB with mean
 * removed).
 *
 * @param past_qua_en       (i)     :Past quantized energies
 * @param code              (i)     :Innovative vector.
 * @param l_subfr           (i)     :Subframe length.
 * @return                  Predicted codebook gain
 */
static float gain_predict(
   float past_qua_en[],
   float code[],      
   int l_subfr       
)
{
   float MEAN_ENER = Ld8k.MEAN_ENER;
   float[] pred = TabLd8k.pred;

   float ener_code, pred_code;
   int i;
   float gcode0; /* (o)     :Predicted codebook gain        */

   pred_code = MEAN_ENER ;

   /* innovation energy */
   ener_code = 0.01f;
   for(i=0; i<l_subfr; i++)
     ener_code += code[i] * code[i];
   ener_code = 10.0f * (float)Math.log10(ener_code /(float)l_subfr);

   pred_code -= ener_code;

   /* predicted energy */
   for (i=0; i<4; i++) pred_code += pred[i]*past_qua_en[i];

   /* predicted codebook gain */
   gcode0 = pred_code;
   gcode0 = (float)Math.pow(10.0, gcode0/20.0);   /* predicted gain */

   return gcode0;
}


/**
 * Update table of past quantized energies.
 *
 * @param past_qua_en        input/output :Past quantized energies
 * @param g_code             input: gbk1[indice1][1]+gbk2[indice2][1]
 */
static void gain_update(
   float past_qua_en[],  
   float g_code  
)
{
   int i;

   /* update table of past quantized energies */
   for (i = 3; i > 0; i--)
     past_qua_en[i] = past_qua_en[i-1];
   past_qua_en[0] = 20.0f*(float)Math.log10((double)g_code);
}

/**
 * Update table of past quantized energies (frame erasure).
 * <pre>
 *     av_pred_en = 0.0;                                                     
 *     for (i = 0; i < 4; i++)                                               
 *        av_pred_en += past_qua_en[i];                                      
 *     av_pred_en = av_pred_en*0.25 - 4.0;                                   
 *     if (av_pred_en < -14.0) av_pred_en = -14.0; 
 * </pre>
 *
 * @param past_qua_en   input/output:Past quantized energies
 */
static void gain_update_erasure(
   float past_qua_en[]    
)
{
   int i;
   float  av_pred_en;

    av_pred_en = 0.0f;
    for (i = 0; i < 4; i++)
        av_pred_en += past_qua_en[i];
    av_pred_en = av_pred_en*0.25f - 4.0f;
    if (av_pred_en < -14.0f) av_pred_en = -14.0f;

    for (i = 3; i > 0; i--)
        past_qua_en[i] = past_qua_en[i-1];
    past_qua_en[0] = av_pred_en;
}

}
