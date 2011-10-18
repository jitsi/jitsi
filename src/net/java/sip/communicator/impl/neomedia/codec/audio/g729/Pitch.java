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
 * Long Term Prediction Routines
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Pitch
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
 File : PITCH.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/

/**
 * Compute the open loop pitch lag.
 *
 * @param signal            input : signal to compute pitch
 *                          s[-PIT_MAX : l_frame-1]
 * @param signal_offset     input : signal offset
 * @param pit_min           input : minimum pitch lag
 * @param pit_max           input : maximum pitch lag 
 * @param l_frame           input : error minimization window
 * @return                  open-loop pitch lag
 */
static int pitch_ol(           
   float[] signal,     
                        
   int signal_offset,
   int pit_min,         
   int pit_max,         
   int l_frame          
)
{
    float THRESHPIT = Ld8k.THRESHPIT;

    float  max1, max2, max3;
    int    p_max1, p_max2, p_max3;

   /*--------------------------------------------------------------------*
    *  The pitch lag search is divided in three sections.                *
    *  Each section cannot have a pitch multiple.                        *
    *  We find a maximum for each section.                               *
    *  We compare the maxima of each section by favoring small lag.      *
    *                                                                    *
    *  First section:  lag delay = PIT_MAX to 80                         *
    *  Second section: lag delay = 79 to 40                              *
    *  Third section:  lag delay = 39 to 20                              *
    *--------------------------------------------------------------------*/

    FloatReference maxRef = new FloatReference();
    p_max1 = lag_max(signal, signal_offset, l_frame, pit_max, 80 , maxRef);
    max1 = maxRef.value;
    p_max2 = lag_max(signal, signal_offset, l_frame, 79     , 40 , maxRef);
    max2 = maxRef.value;
    p_max3 = lag_max(signal, signal_offset, l_frame, 39     , pit_min , maxRef);
    max3 = maxRef.value;

   /*--------------------------------------------------------------------*
    * Compare the 3 sections maxima, and favor small lag.                *
    *--------------------------------------------------------------------*/

    if ( max1 * THRESHPIT < max2 ) {
        max1 = max2;
        p_max1 = p_max2;
    }

    if ( max1 * THRESHPIT < max3 )  p_max1 = p_max3;

    return (p_max1);
}

/**
 * Find the lag that has maximum correlation
 *
 * @param signal            input : Signal to compute the open loop pitch
 *                          signal[-142:-1] should be known.
 * @param signal_offset     input : signal offset
 * @param l_frame           input : Length of frame to compute pitch
 * @param lagmax            input : maximum lag
 * @param lagmin            input : minimum lag
 * @param cor_max           input : normalized correlation of selected lag
 * @return lag found
 */
private static int lag_max(  
  float[] signal,       
  int signal_offset,
  int l_frame,         
  int lagmax,       
  int lagmin,      
  FloatReference cor_max    
)
{
    float FLT_MIN_G729 = Ld8k.FLT_MIN_G729;

    int    i, j;
    int    p, p1;
    float  max, t0;
    int    p_max = 0;

    max = FLT_MIN_G729;

    for (i = lagmax; i >= lagmin; i--) {
        p  = signal_offset;
        p1 = signal_offset - i;
        t0 = 0.0f;

        for (j=0; j<l_frame; j++, p++, p1++) {
            t0 += signal[p] * signal[p1];
        }

        if (t0 >= max) {
            max    = t0;
            p_max = i;
        }
    }

    /* compute energy */

    t0 = 0.01f;                  /* to avoid division by zero */
    p = signal_offset - p_max;
    for(i=0; i<l_frame; i++, p++) {
        t0 += signal[p] * signal[p];
    }
    t0 = inv_sqrt(t0);          /* 1/sqrt(energy)    */

    cor_max.value = max * t0;        /* max/sqrt(energy)  */

    return(p_max);
}

/**
 * Find the pitch period  with 1/3 subsample resolution
 *
 * @param exc           input : excitation buffer
 * @param exc_offset    input : excitation buffer offset
 * @param xn            input : target vector
 * @param h             input : impulse response of filters. 
 * @param l_subfr       input : Length of frame to compute pitch
 * @param t0_min        input : minimum value in the searched range
 * @param t0_max        input : maximum value in the searched range
 * @param i_subfr       input : indicator for first subframe
 * @param pit_frac      output: chosen fraction
 * @return          integer part of pitch period  
 */
static int pitch_fr3(    
 float[] exc,           /*                  */
 int exc_offset,
 float xn[],            /*                        */
 float h[],             /*        */
 int l_subfr,           /*     */
 int t0_min,            /*  */
 int t0_max,            /*  */
 int i_subfr,           /*         */
 IntReference pit_frac          /*                      */
)
{
  int L_INTER4 = Ld8k.L_INTER4;

  int    i, frac;
  int    lag, t_min, t_max;
  float  max;
  float  corr_int;
  float[]  corr_v = new float[10+2*L_INTER4];  /* size: 2*L_INTER4+t0_max-t0_min+1 */
  float[]  corr;
  int corr_offset;

  /* Find interval to compute normalized correlation */

  t_min = t0_min - L_INTER4;
  t_max = t0_max + L_INTER4;

  corr = corr_v;    /* corr[t_min:t_max] */
  corr_offset = -t_min;

  /* Compute normalized correlation between target and filtered excitation */

  norm_corr(exc, exc_offset, xn, h, l_subfr, t_min, t_max, corr, corr_offset);

  /* find integer pitch */

  max = corr[corr_offset + t0_min];
  lag  = t0_min;

  for(i= t0_min+1; i<=t0_max; i++)
  {
    if( corr[corr_offset + i] >= max)
    {
      max = corr[corr_offset + i];
      lag = i;
    }
  }

  /* If first subframe and lag > 84 do not search fractionnal pitch */

  if( (i_subfr == 0) && (lag > 84) )
  {
    pit_frac.value = 0;
    return(lag);
  }

  /* test the fractions around lag and choose the one which maximizes
     the interpolated normalized correlation */
  corr_offset += lag;
  max  = interpol_3(corr, corr_offset, -2);
  frac = -2;

  for (i = -1; i <= 2; i++)
  {
    corr_int = interpol_3(corr, corr_offset, i);
    if(corr_int > max)
    {
      max = corr_int;
      frac = i;
    }
  }

  /* limit the fraction value in the interval [-1,0,1] */

  if (frac == -2)
  {
    frac = 1;
    lag -= 1;
  }
  if (frac == 2)
  {
    frac = -1;
    lag += 1;
  }

  pit_frac.value = frac;

  return lag;
}

/**
 * Find the normalized correlation between the target vector and
 * the filtered past excitation.
 *
 * @param exc                   input : excitation buffer
 * @param exc_offset            input : excitation buffer offset
 * @param xn                    input : target vector
 * @param h                     input : imp response of synth and weighting flt
 * @param l_subfr               input : Length of frame to compute pitch
 * @param t_min                 input : minimum value of searched range
 * @param t_max                 input : maximum value of search range
 * @param corr_norm             output: normalized correlation (correlation
 *                              between target and filtered excitation divided
 *                              by the square root of energy of filtered
 *                              excitation)
 * @param corr_norm_offset      input: normalized correlation offset
 */
private static void norm_corr(
 float[] exc,          
 int exc_offset,
 float xn[],          
 float h[],             
 int l_subfr,          
 int t_min,             
 int t_max,            
 float corr_norm[], 
 int corr_norm_offset
)
{
 int L_SUBFR = Ld8k.L_SUBFR;

 int    i, j, k;
 float[] excf = new float[L_SUBFR];     /* filtered past excitation */
 float  alp, s, norm;

 k = exc_offset -t_min;

 /* compute the filtered excitation for the first delay t_min */

 Filter.convolve(exc, k, h, excf, l_subfr);

 /* loop for every possible period */

 for (i = t_min; i <= t_max; i++)
 {
   /* Compute 1/sqrt(energie of excf[]) */

   alp = 0.01f;
   for (j = 0; j < l_subfr; j++)
     alp += excf[j]*excf[j];

   norm = inv_sqrt(alp);


   /* Compute correlation between xn[] and excf[] */

   s = 0.0f;
   for (j = 0; j < l_subfr; j++)  s += xn[j]*excf[j];


   /* Normalize correlation = correlation * (1/sqrt(energie)) */

   corr_norm[corr_norm_offset + i] = s*norm;

   /* modify the filtered excitation excf[] for the next iteration */

   if (i != t_max)
   {
     k--;
     for (j = l_subfr-1; j > 0; j--)
        excf[j] = excf[j-1] + exc[k]*h[j];
     excf[0] = exc[k];
   }
 }
}

/**
 * Compute adaptive codebook gain and compute <y1,y1> , -2<xn,y1>
 *
 * @param xn        input : target vector
 * @param y1        input : filtered adaptive codebook vector
 * @param g_coeff   output: <y1,y1> and -2<xn,y1>
 * @param l_subfr   input : vector dimension
 * @return          pitch gain
 */
static float g_pitch(        
 float xn[],           
 float y1[],           
 float g_coeff[],       
 int l_subfr            
)
{
    float GAIN_PIT_MAX = Ld8k.GAIN_PIT_MAX;

    float xy, yy, gain;
    int   i;

    xy = 0.0f;
    for (i = 0; i < l_subfr; i++) {
        xy += xn[i] * y1[i];
    }
    yy = 0.01f;
    for (i = 0; i < l_subfr; i++) {
        yy += y1[i] * y1[i];          /* energy of filtered excitation */
    }
    g_coeff[0] = yy;
    g_coeff[1] = -2.0f*xy +0.01f;

    /* find pitch gain and bound it by [0,1.2] */

    gain = xy/yy;

    if (gain<0.0f)  gain = 0.0f;
    if (gain>GAIN_PIT_MAX) gain = GAIN_PIT_MAX;

    return gain;
}

/**
 * Function enc_lag3()
 * Encoding of fractional pitch lag with 1/3 resolution.
 * <pre>
 * The pitch range for the first subframe is divided as follows:
 *   19 1/3  to   84 2/3   resolution 1/3
 *   85      to   143      resolution 1
 *
 * The period in the first subframe is encoded with 8 bits.
 * For the range with fractions:
 *   index = (T-19)*3 + frac - 1;   where T=[19..85] and frac=[-1,0,1]
 * and for the integer only range
 *   index = (T - 85) + 197;        where T=[86..143]
 *----------------------------------------------------------------------
 * For the second subframe a resolution of 1/3 is always used, and the
 * search range is relative to the lag in the first subframe.
 * If t0 is the lag in the first subframe then
 *  t_min=t0-5   and  t_max=t0+4   and  the range is given by
 *       t_min - 2/3   to  t_max + 2/3
 *
 * The period in the 2nd subframe is encoded with 5 bits:
 *   index = (T-(t_min-1))*3 + frac - 1;    where T[t_min-1 .. t_max+1]
 * </pre>
 *
 * @param T0            input : Pitch delay
 * @param T0_frac       input : Fractional pitch delay
 * @param T0_min        in/out: Minimum search delay 
 * @param T0_max        in/out: Maximum search delay
 * @param pit_min       input : Minimum pitch delay
 * @param pit_max       input : Maximum pitch delay
 * @param pit_flag      input : Flag for 1st subframe
 * @return              Return index of encoding
 */
static int  enc_lag3(     
  int  T0,        
  int  T0_frac,   
  IntReference  T0_min,  
  IntReference  T0_max,   
  int pit_min,   
  int pit_max,  
  int  pit_flag 
)
{
  int index;
  int _T0_min = T0_min.value, _T0_max = T0_max.value;

  if (pit_flag == 0)   /* if 1st subframe */
  {
     /* encode pitch delay (with fraction) */

     if (T0 <= 85)
       index = T0*3 - 58 + T0_frac;
     else
       index = T0 + 112;

     /* find T0_min and T0_max for second subframe */

     _T0_min = T0 - 5;
     if (_T0_min < pit_min) _T0_min = pit_min;
     _T0_max = _T0_min + 9;
     if (_T0_max > pit_max)
     {
         _T0_max = pit_max;
         _T0_min = _T0_max - 9;
     }
  }

  else                    /* second subframe */
  {
     index = T0 - _T0_min;
     index = index*3 + 2 + T0_frac;
  }
  T0_min.value = _T0_min;
  T0_max.value = _T0_max;
  return index;
}

/**
 * For interpolating the normalized correlation
 *
 * @param x          input : function to be interpolated
 * @param x_offset   input : function offset
 * @param frac       input : fraction value to evaluate
 * @return           interpolated value
 */
private static float interpol_3(  
 float[] x,              
 int x_offset,
 int frac               
)
{
  int L_INTER4 = Ld8k.L_INTER4;
  int UP_SAMP = Ld8k.UP_SAMP;
  float[] inter_3 = TabLd8k.inter_3;

  int i;
  float s;
  int x1, x2, c1, c2;

  if (frac < 0) {
    frac += UP_SAMP;
    x_offset--;
  }
  x1 = x_offset;
  x2 = x_offset + 1;
  c1 = frac;
  c2 = UP_SAMP-frac;

  s = 0.0f;
  for(i=0; i< L_INTER4; i++, c1+=UP_SAMP, c2+=UP_SAMP)
  {
     s+= x[x1] * inter_3[c1] + x[x2] * inter_3[c2];
     x1--;
     x2++;
  }

  return s;
}

/**
 * Compute y = 1 / sqrt(x)
 *
 * @param x input : value of x
 * @return output: 1/sqrt(x)
 */
private static float inv_sqrt(
 float x
)
{
   return (1.0f / (float)Math.sqrt((double)x) );
}
}
