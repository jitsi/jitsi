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
class DeAcelp
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
 File : DE_ACELP.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/


/**
 * Algebraic codebook decoder.
 *
 * @param sign      input : signs of 4 pulses
 * @param index     input : positions of 4 pulses
 * @param cod       output: innovative codevector
 */
static void decod_ACELP(
 int sign,             
 int index,            
 float cod[]            
)
{
   int L_SUBFR = Ld8k.L_SUBFR;

   int[] pos = new int[4];
   int i, j;

   /* decode the positions of 4 pulses */

   i = index & 7;
   pos[0] = i*5;

   index >>= 3;
   i = index & 7;
   pos[1] = i*5 + 1;

   index >>= 3;
   i = index & 7;
   pos[2] = i*5 + 2;

   index >>= 3;
   j = index & 1;
   index >>= 1;
   i = index & 7;
   pos[3] = i*5 + 3 + j;

   /* find the algebraic codeword */

   for (i = 0; i < L_SUBFR; i++) cod[i] = 0;

   /* decode the signs of 4 pulses */

   for (j=0; j<4; j++)
   {

     i = sign & 1;
     sign >>= 1;

     if (i != 0) {
       cod[pos[j]] = 1.0f;
     }
     else {
       cod[pos[j]] = -1.0f;
     }
   }
}
}
