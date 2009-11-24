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
class DecLag3
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
 File : DEC_LAG3.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/**
 * Decoding of fractional pitch lag with 1/3 resolution.
 * See the source for more details about the encoding procedure.
 *
 * @param index      input : received pitch index
 * @param pit_min    input : minimum pitch lag
 * @param pit_max    input : maximum pitch lag
 * @param i_subfr    input : subframe flag
 * @param T0         output: integer part of pitch lag
 * @param T0_frac    output: fractional part of pitch lag
 */
static void dec_lag3(     
  int index,       
  int pit_min,
  int pit_max,    
  int i_subfr, 
  IntReference T0,         
  IntReference T0_frac  
)
{
  int i;
  int _T0 = T0.value, _T0_frac = T0_frac.value;
  int T0_min, T0_max;

  if (i_subfr == 0)                  /* if 1st subframe */
  {
    if (index < 197)
    {
       _T0 = (index+2)/3 + 19;
       _T0_frac = index - _T0*3 + 58;
    }
    else
    {
      _T0 = index - 112;
      _T0_frac = 0;
    }
  }

  else  /* second subframe */
  {
    /* find T0_min and T0_max for 2nd subframe */

    T0_min = _T0 - 5;
    if (T0_min < pit_min)
      T0_min = pit_min;

    T0_max = T0_min + 9;
    if(T0_max > pit_max)
    {
      T0_max = pit_max;
      T0_min = T0_max -9;
    }

    i = (index+2)/3 - 1;
    _T0 = i + T0_min;
    _T0_frac = index - 2 - i*3;
  }
  T0.value = _T0;
  T0_frac.value = _T0_frac;
}
}
