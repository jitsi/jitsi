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
 * Preprocessing of input speech.
 *   - 2nd order high pass filter with cut off frequency at 140 Hz.
 *
 * 2nd order high pass filter with cut off frequency at 140 Hz.
 * Designed with SPPACK efi command -40 dB att, 0.25 ri.
 *
 * Algorithm:
 * <pre>
 *  y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2]
 *                   + a[1]*y[i-1] + a[2]*y[i-2];
 *
 *     b[3] = {0.92727435E+00, -0.18544941E+01, 0.92727435E+00};
 *     a[3] = {0.10000000E+01, 0.19059465E+01, -0.91140240E+00};
 * </pre>
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class PreProc
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
 File : PRE_PROC.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/

/**
 * High-pass fir memory
 */
private float x0, x1; 

/**
 * High-pass iir memory
 */
private float y1, y2;         

/**
 * Init Pre Process
 */
void init_pre_process()
{
  x0 = x1 = 0.0f;
  y2 = y1 = 0.0f;
}

/**
 * Pre Process
 *
 * @param signal            (i/o)  : signal   
 * @param signal_offset     (input)  : signal offset
 * @param lg                (i)    : length of signal
 */
void pre_process(
   float[] signal,      
   int signal_offset,
   int lg               
)
{
  float[] a140 = TabLd8k.a140;
  float[] b140 = TabLd8k.b140;

  float x2;
  float y0;

  for(int i=signal_offset, toIndex=lg+signal_offset; i<toIndex; i++)
  {
    x2 = x1;
    x1 = x0;
    x0 = signal[i];

    y0 = y1*a140[1] + y2*a140[2] + x0*b140[0] + x1*b140[1] + x2*b140[2];

    signal[i] = y0;
    y2 = y1;
    y1 = y0;
  }
}
}
