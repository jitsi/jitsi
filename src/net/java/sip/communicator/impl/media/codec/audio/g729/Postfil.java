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
package net.java.sip.communicator.impl.media.codec.audio.g729;

/**
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Postfil
    extends Ld8k
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
 File : POSTFIL.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/


/************************************************************************/
/*      Post - filtering : short term + long term                       */
/*      Floating point computation                                      */
/************************************************************************/

/* Static arrays and variables */
private final float[] apond2 = new float[LONG_H_ST];           /* s.t. numerator coeff.        */
private final float[] mem_stp = new float[M];            /* s.t. postfilter memory       */
private final float[] mem_zero = new float[M];          /* null memory to compute h_st  */
private final float[] res2 = new float[SIZ_RES2];        /* A(gamma2) residual           */

/* Static pointers */
private int res2_ptr;
private float[] ptr_mem_stp;
private int ptr_mem_stp_offset;

/* Variables */
private float gain_prec;             /* for gain adjustment          */

/****   Short term postfilter :                                     *****/
/*      Hst(z) = Hst0(z) Hst1(z)                                        */
/*      Hst0(z) = 1/g0 A(gamma2)(z) / A(gamma1)(z)                      */
/*      if {hi} = i.r. filter A(gamma2)/A(gamma1) (truncated)           */
/*      g0 = SUM(|hi|) if > 1                                           */
/*      g0 = 1. else                                                    */
/*      Hst1(z) = 1/(1+ |mu|) (1 + mu z-1)                              */
/*      with mu = 1st parcor calculated on {hi}                         */
/****   Long term postfilter :                                      *****/
/*      harmonic postfilter :   H0(z) = gl * (1 + b * z-p)              */
/*      b = gamma_g * gain_ltp                                          */
/*      gl = 1 / 1 + b                                                  */
/*      copmuation of delay on A(gamma2)(z) s(z)                        */
/*      sub optimal research                                            */
/*      1. search best integer delay                                    */
/*      2. search around integer sub multiples (3 val. / sub mult)      */
/*      3. search around integer with fractionnal delays (1/8)          */
/************************************************************************/

/*----------------------------------------------------------------------------
 * init_pst -  Initialize postfilter functions
 *----------------------------------------------------------------------------
 */
void init_post_filter()
{
    int i;

    /* Initialize arrays and pointers */

    /* A(gamma2) residual */
    for(i=0; i<MEM_RES2; i++) res2[i] = 0.0f;
    res2_ptr = MEM_RES2;

    /* 1/A(gamma1) memory */
    for(i=0; i<M; i++) mem_stp[i] = 0.0f;
    ptr_mem_stp = mem_stp;
    ptr_mem_stp_offset = M - 1;

    /* fill apond2[M+1->LONG_H_ST-1] with zeroes */
    for(i=MP1; i<LONG_H_ST; i++) apond2[i] = 0.0f;

    /* null memory to compute i.r. of A(gamma2)/A(gamma1) */
    for(i=0; i<M; i++) mem_zero[i] = 0.0f;

    /* for gain adjustment */
    gain_prec =1.f;
}

/*----------------------------------------------------------------------------
 * post - adaptive postfilter main function
 *----------------------------------------------------------------------------
 */
int post(
 int t0,                /* input : pitch delay given by coder */
 float[] signal_ptr,     /* input : input signal (pointer to current subframe */
 int signal_ptr_offset,
 float[] coeff,          /* input : LPC coefficients for current subframe */
 int coeff_offset,
 float[] sig_out,        /* output: postfiltered output */
 int sig_out_offset
)
{
    int vo;                /* output: voicing decision 0 = uv,  > 0 delay */

    float[] apond1 = new float[MP1];           /* s.t. denominator coeff.      */
    float[] sig_ltp = new float[L_SUBFRP1];   /* H0 output signal             */
    int sig_ltp_ptr;
    float parcor0;

    /* Compute weighted LPC coefficients */
    Lpcfunc.weight_az(coeff, coeff_offset, GAMMA1_PST, M, apond1);
    Lpcfunc.weight_az(coeff, coeff_offset, GAMMA2_PST, M, apond2);

    /* Compute A(gamma2) residual */
    Filter.residu(apond2, 0, signal_ptr, signal_ptr_offset, res2, res2_ptr, L_SUBFR);

    /* Harmonic filtering */
    sig_ltp_ptr = 1;
    vo = pst_ltp(t0, res2, res2_ptr, sig_ltp, sig_ltp_ptr);

    /* Save last output of 1/A(gamma1)  */
    /* (from preceding subframe)        */
    sig_ltp[0] = ptr_mem_stp[ptr_mem_stp_offset];

    /* Control short term pst filter gain and compute parcor0   */
    parcor0 = calc_st_filt(apond2, apond1, sig_ltp, sig_ltp_ptr);

    /* 1/A(gamma1) filtering, mem_stp is updated */
    Filter.syn_filt(apond1, 0, sig_ltp, sig_ltp_ptr, sig_ltp, sig_ltp_ptr, L_SUBFR, mem_stp, 0, 1);

    /* (1 + mu z-1) tilt filtering */
    filt_mu(sig_ltp, sig_out, sig_out_offset, parcor0);

    /* gain control */
    gain_prec = scale_st(signal_ptr, signal_ptr_offset, sig_out, sig_out_offset, gain_prec);

    /**** Update for next frame */
    Util.copy(res2, L_SUBFR, res2, MEM_RES2);

    return vo;
}

/*----------------------------------------------------------------------------
 *  pst_ltp - harmonic postfilter
 *----------------------------------------------------------------------------
 */
private int pst_ltp(
 int t0,                /* input : pitch delay given by coder */
 float[] ptr_sig_in,     /* input : postfilter input filter (residu2) */
 int ptr_sig_in_offset,
 float[] ptr_sig_pst0,   /* output: harmonic postfilter output */
 int ptr_sig_pst0_offset
)
{
    int vo;                /* output: voicing decision 0 = uv,  > 0 delay */

/**** Declare variables                                 */
    int ltpdel, phase;
    float num_gltp, den_gltp;
    float num2_gltp, den2_gltp;
    float gain_plt;
    float[] y_up = new float[SIZ_Y_UP];
    float[] ptr_y_up;
    int ptr_y_up_offset;
    int off_yup;

    /* Sub optimal delay search */
    IntReference _ltpdel = new IntReference();
    IntReference _phase = new IntReference();
    FloatReference _num_gltp = new FloatReference();
    FloatReference _den_gltp = new FloatReference();
    IntReference _off_yup = new IntReference();
    search_del(t0, ptr_sig_in, ptr_sig_in_offset, _ltpdel, _phase, _num_gltp, _den_gltp,
                        y_up, _off_yup);
    ltpdel = _ltpdel.value;
    phase = _phase.value;
    num_gltp = _num_gltp.value;
    den_gltp = _den_gltp.value;
    off_yup = _off_yup.value;

    vo = ltpdel;

    if(num_gltp == 0.f)  {
        Util.copy(ptr_sig_in, ptr_sig_in_offset, ptr_sig_pst0, ptr_sig_pst0_offset, L_SUBFR);
    }
    else {

        if(phase == 0) {
            ptr_y_up = ptr_sig_in;
            ptr_y_up_offset = ptr_sig_in_offset - ltpdel;
        }

        else {
            /* Filtering with long filter */
            FloatReference _num2_gltp = new FloatReference();
            FloatReference _den2_gltp = new FloatReference();
            compute_ltp_l(ptr_sig_in, ptr_sig_in_offset, ltpdel, phase, ptr_sig_pst0, ptr_sig_pst0_offset,
                _num2_gltp, _den2_gltp);
            num2_gltp = _num2_gltp.value;
            den2_gltp = _den2_gltp.value;

            if(select_ltp(num_gltp, den_gltp, num2_gltp, den2_gltp) == 1) {

                /* select short filter */
                ptr_y_up = y_up;
                ptr_y_up_offset = ((phase-1) * L_SUBFRP1 + off_yup);
            }
            else {
                /* select long filter */
                num_gltp = num2_gltp;
                den_gltp = den2_gltp;
                ptr_y_up = ptr_sig_pst0;
                ptr_y_up_offset = ptr_sig_pst0_offset;
            }
        }

        if(num_gltp > den_gltp) {
            /* beta bounded to 1 */
            gain_plt = MIN_GPLT;
        }
        else {
            gain_plt = den_gltp / (den_gltp + GAMMA_G * num_gltp);
        }

        /** filtering by H0(z) (harmonic filter) **/
        filt_plt(ptr_sig_in, ptr_sig_in_offset, ptr_y_up, ptr_y_up_offset, ptr_sig_pst0, ptr_sig_pst0_offset, gain_plt);
    }
    return vo;
}

/*----------------------------------------------------------------------------
 *  search_del: computes best (shortest) integer LTP delay + fine search
 *----------------------------------------------------------------------------
 */
private void search_del(
 int t0,                /* input : pitch delay given by coder */
 float[] ptr_sig_in,     /* input : input signal (with delay line) */
 int ptr_sig_in_offset,
 IntReference ltpdel,           /* output: delay = *ltpdel - *phase / f_up */
 IntReference phase,            /* output: phase */
 FloatReference num_gltp,       /* output: numerator of LTP gain */
 FloatReference den_gltp,       /* output: denominator of LTP gain */
 float[] y_up,           /*       : */
 IntReference off_yup           /*       : */
)
{
    float[] tab_hup_s = TabLd8k.tab_hup_s;

    /* pointers on tables of constants */
    int ptr_h;

    /* Variables and local arrays */
    float[] tab_den0 = new float[F_UP_PST-1], tab_den1 = new float[F_UP_PST-1];
    int ptr_den0, ptr_den1;
    int ptr_sig_past, ptr_sig_past0;
    int ptr1;

    int i, n, ioff, i_max;
    float ener, num, numsq, den0, den1;
    float den_int, num_int;
    float den_max, num_max, numsq_max;
    int phi_max;
    int lambda, phi;
    float temp0, temp1;
    int ptr_y_up;

    /*****************************************/
    /* Compute current signal energy         */
    /*****************************************/

    ener = 0.f;
    for(i=0; i<L_SUBFR; i++) {
        ener += ptr_sig_in[ptr_sig_in_offset+i] * ptr_sig_in[ptr_sig_in_offset+i];
    }
    if(ener < 0.1f) {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value = 0;
        phase.value = 0;
        return;
    }

    /*************************************/
    /* Selects best of 3 integer delays  */
    /* Maximum of 3 numerators around t0 */
    /* coder LTP delay                   */
    /*************************************/

    lambda = t0-1;

    ptr_sig_past = ptr_sig_in_offset - lambda;

    num_int = -1.0e30f;

   /* initialization used only to suppress Microsoft Visual C++ warnings */
    i_max = 0;
    for(i=0; i<3; i++) {
        num=0.f;
        for(n=0; n<L_SUBFR; n++) {
            num += ptr_sig_in[ptr_sig_in_offset+n]* ptr_sig_in[ptr_sig_past + n];
        }
        if(num > num_int) {
            i_max   = i;
            num_int = num;
        }
        ptr_sig_past--;
    }
    if(num_int <= 0.f) {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value   = 0;
        phase.value    = 0;
        return;
    }

    /* Calculates denominator for lambda_max */
    lambda += i_max;
    ptr_sig_past = ptr_sig_in_offset - lambda;
    den_int=0.f;
    for(n=0; n<L_SUBFR; n++) {
        den_int += ptr_sig_in[ptr_sig_past + n]* ptr_sig_in[ptr_sig_past + n];
    }
    if(den_int < 0.1f) {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value   = 0;
        phase.value    = 0;
        return;
    }
    /***********************************/
    /* Select best phase around lambda */
    /***********************************/

    /* Compute y_up & denominators */
    /*******************************/
    ptr_y_up = 0;
    den_max = den_int;
    ptr_den0 = 0;
    ptr_den1 = 0;
    ptr_h = 0;
    ptr_sig_past0 = ptr_sig_in_offset + LH_UP_S - 1 - lambda; /* points on lambda_max+1 */

    /* loop on phase  */
    for(phi=1; phi<F_UP_PST; phi++) {

        /* Computes criterion for (lambda_max+1) - phi/F_UP_PST     */
        /* and lambda_max - phi/F_UP_PST                            */
        ptr_sig_past = ptr_sig_past0;
        /* computes y_up[n] */
        for(n = 0; n<=L_SUBFR; n++) {
            ptr1 = ptr_sig_past++;
            temp0 = 0.f;
            for(i=0; i<LH2_S; i++) {
                temp0 += tab_hup_s[ptr_h + i] * ptr_sig_in[ptr1 - i];
            }
            y_up[ptr_y_up + n] = temp0;
        }

        /* recursive computation of den0 (lambda_max+1) and den1 (lambda_max) */

        /* common part to den0 and den1 */
        temp0 = 0.f;
        for(n=1; n<L_SUBFR; n++) {
            temp0 += y_up[ptr_y_up + n] * y_up[ptr_y_up + n];
        }

        /* den0 */
        den0  = temp0 + y_up[ptr_y_up + 0] * y_up[ptr_y_up + 0];
        tab_den0[ptr_den0] = den0;
        ptr_den0++;

        /* den1 */
        den1 = temp0 + y_up[ptr_y_up + L_SUBFR] * y_up[ptr_y_up + L_SUBFR];
        tab_den1[ptr_den1] = den1;
        ptr_den1++;

        if(Math.abs(y_up[ptr_y_up + 0])>Math.abs(y_up[ptr_y_up + L_SUBFR])) {
            if(den0 > den_max) {
                den_max = den0;
            }
        }
        else {
            if(den1 > den_max) {
                den_max = den1;
            }
        }
        ptr_y_up += L_SUBFRP1;
        ptr_h += LH2_S;
    }
    if(den_max < 0.1f ) {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value   = 0;
        phase.value    = 0;
        return;
    }
    /* Computation of the numerators                */
    /* and selection of best num*num/den            */
    /* for non null phases                          */

    /* Initialize with null phase */
    num_max      = num_int;
    den_max      = den_int;
    numsq_max   =  num_max * num_max;
    phi_max      = 0;
    ioff         = 1;

    ptr_den0   = 0;
    ptr_den1   = 0;
    ptr_y_up     = 0;

    /* if den_max = 0 : will be selected and declared unvoiced */
    /* if num!=0 & den=0 : will be selected and declared unvoiced */
    /* degenerated seldom cases, switch off LT is OK */

    /* Loop on phase */
    for(phi=1; phi<F_UP_PST; phi++) {


        /* computes num for lambda_max+1 - phi/F_UP_PST */
        num = 0.f;
        for(n = 0; n<L_SUBFR; n++) {
            num += ptr_sig_in[n]  * y_up[ptr_y_up + n];
        }
        if(num < 0.f) num = 0.f;
        numsq = num * num;

        /* selection if num/sqrt(den0) max */
        den0 = tab_den0[ptr_den0];
        ptr_den0++;
        temp0 = numsq * den_max;
        temp1 = numsq_max * den0;
        if(temp0 > temp1) {
            num_max     = num;
            numsq_max   = numsq;
            den_max     = den0;
            ioff        = 0;
            phi_max     = phi;
        }

        /* computes num for lambda_max - phi/F_UP_PST */
        ptr_y_up++;
        num = 0.f;
        for(n = 0; n<L_SUBFR; n++) {
            num += ptr_sig_in[n]  * y_up[ptr_y_up + n];
        }
        if(num < 0.f) num = 0.f;
        numsq = num * num;

        /* selection if num/sqrt(den1) max */
        den1 = tab_den1[ptr_den1];
        ptr_den1++;
        temp0 = numsq * den_max;
        temp1 = numsq_max * den1;
        if(temp0 > temp1) {
            num_max     = num;
            numsq_max   = numsq;
            den_max     = den1;
            ioff        = 1;
            phi_max     = phi;
        }
        ptr_y_up += L_SUBFR;
    }

    /***************************************************/
    /*** test if normalised crit0[iopt] > THRESCRIT  ***/
    /***************************************************/

    if((num_max == 0.f) || (den_max <= 0.1f)) {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value = 0;
        phase.value = 0;
        return;
    }

    /* comparison num * num            */
    /* with ener * den x THRESCRIT      */
    temp1 = den_max * ener * THRESCRIT;
    if(numsq_max >= temp1) {
        ltpdel.value   = lambda + 1 - ioff;
        off_yup.value  = ioff;
        phase.value    = phi_max;
        num_gltp.value = num_max;
        den_gltp.value = den_max;
    }
    else {
        num_gltp.value = 0.f;
        den_gltp.value = 1.f;
        ltpdel.value   = 0;
        phase.value    = 0;
    }
}

/*----------------------------------------------------------------------------
 *  filt_plt -  ltp  postfilter
 *----------------------------------------------------------------------------
 */
private void filt_plt(
 float[] s_in,       /* input : input signal with past*/
 int s_in_offset,
 float[] s_ltp,      /* input : filtered signal with gain 1 */
 int s_ltp_offset,
 float[] s_out,      /* output: output signal */
 int s_out_offset,
 float gain_plt     /* input : filter gain  */
)
{

    /* Local variables */
    int n;
    float temp;
    float gain_plt_1;

    gain_plt_1 = 1.f - gain_plt;

    for(n=0;  n<L_SUBFR; n++) {
        /* s_out(n) = gain_plt x s_in(n) + gain_plt_1 x s_ltp(n)    */
        temp =  gain_plt   * s_in[s_in_offset + n];
        temp += gain_plt_1 * s_ltp[s_ltp_offset + n];
        s_out[s_out_offset + n] = temp;
    }
}

/*----------------------------------------------------------------------------
 *  compute_ltp_l : compute delayed signal,
                    num & den of gain for fractional delay
 *                  with long interpolation filter
 *----------------------------------------------------------------------------
 */
private void compute_ltp_l(
 float[] s_in,       /* input signal with past*/
 int s_in_offset,
 int ltpdel,      /* delay factor */
 int phase,       /* phase factor */
 float[] y_up,       /* delayed signal */
 int y_up_offset,
 FloatReference num,        /* numerator of LTP gain */
 FloatReference den        /* denominator of LTP gain */
)
{
    float[] tab_hup_l = TabLd8k.tab_hup_l;

    /* Pointer on table of constants */
    int ptr_h;

    /* Local variables */
    int i;
    int ptr2;
    float temp;

    /* Filtering with long filter */
    ptr_h = (phase-1) * LH2_L;
    ptr2 = s_in_offset - ltpdel + LH_UP_L;

    /* Compute y_up */
    for(int n = y_up_offset, toIndex = y_up_offset + L_SUBFR; n<toIndex; n++) {
        temp = 0.f;
        for(i=0; i<LH2_L; i++) {
            temp += tab_hup_l[ptr_h + i] * s_in[ptr2];
            ptr2--;
        }
        y_up[n] = temp;
        ptr2 += LH2_L_P1;
    }

    float _num = 0.f;
    /* Compute num */
    for(int n = 0; n<L_SUBFR; n++) {
        _num += y_up[y_up_offset + n]* s_in[s_in_offset + n];
    }
    if(_num < 0.0f) _num = 0.0f;
    num.value = _num;

    float _den = 0.f;
    /* Compute den */
    for(int n = y_up_offset, toIndex = y_up_offset + L_SUBFR; n<toIndex; n++) {
        _den += y_up[n]* y_up[n];
    }
    den.value = _den;
}
/*----------------------------------------------------------------------------
 *  select_ltp : selects best of (gain1, gain2)
 *  with gain1 = num1 / den1
 *  and  gain2 = num2 / den2
 *----------------------------------------------------------------------------
 */
private int select_ltp(  /* output : 1 = 1st gain, 2 = 2nd gain */
 float num1,       /* input : numerator of gain1 */
 float den1,       /* input : denominator of gain1 */
 float num2,       /* input : numerator of gain2 */
 float den2        /* input : denominator of gain2 */
)
{
    if(den2 == 0.f) {
        return(1);
    }
    if(num2 * num2 * den1> num1 * num1 * den2) {
        return(2);
    }
    else {
        return(1);
    }
}



/*----------------------------------------------------------------------------
 *   calc_st_filt -  computes impulse response of A(gamma2) / A(gamma1)
 *   controls gain : computation of energy impulse response as
 *                    SUMn  (abs (h[n])) and computes parcor0
 *----------------------------------------------------------------------------
 */
private float calc_st_filt(
 float[] apond2,     /* input : coefficients of numerator */
 float[] apond1,     /* input : coefficients of denominator */
 float[] sig_ltp_ptr,    /* in/out: input of 1/A(gamma1) : scaled by 1/g0 */
 int sig_ltp_ptr_offset
)
{
    float[] h = new float[LONG_H_ST];
    float parcor0;    /* output: 1st parcor calcul. on composed filter */
    float g0, temp;

    /* computes impulse response of  apond1 / apond2 */
    Filter.syn_filt(apond1, 0, apond2, 0, h, 0, LONG_H_ST, mem_zero, 0, 0);

    /* computes 1st parcor */
    parcor0 = calc_rc0_h(h);

    /* computes gain g0 */
    g0 = 0.f;
    for(int i=0; i<LONG_H_ST; i++) {
        g0 += Math.abs(h[i]);
    }

    /* Scale signal input of 1/A(gamma1) */
    if(g0 > 1.f) {
        temp = 1.f/g0;
        for(int i=sig_ltp_ptr_offset, toIndex = sig_ltp_ptr_offset + L_SUBFR; i<toIndex; i++) {
            sig_ltp_ptr[i] = sig_ltp_ptr[i] * temp;
        }
    }

    return parcor0;
}

/*----------------------------------------------------------------------------
 * calc_rc0_h - computes 1st parcor from composed filter impulse response
 *----------------------------------------------------------------------------
 */
private float calc_rc0_h(
 float[] h      /* input : impulse response of composed filter */
)
{
    float acf0, acf1;
    float temp, temp2;
    int ptrs;
    int i;

    /* computation of the autocorrelation function acf */
    temp = 0.f;
    for(i=0;i<LONG_H_ST;i++){
        temp += h[i] * h[i];
    }
    acf0 = temp;

    temp = 0.f;
    ptrs = 0;
    for(i=0;i<LONG_H_ST-1;i++){
        temp2 = h[ptrs];
        ptrs++;
        temp += temp2 * h[ptrs];
    }
    acf1 = temp;

    /* Initialisation of the calculation */
    if( acf0 == 0.f) {
        return 0.f; /* output: 1st parcor */
    }

    /* Compute 1st parcor */
    /**********************/
    if(acf0 < Math.abs(acf1) ) {
        return 0.0f; /* output: 1st parcor */
    }
    return - acf1 / acf0; /* output: 1st parcor */
}

/*----------------------------------------------------------------------------
 * filt_mu - tilt filtering with : (1 + mu z-1) * (1/1-|mu|)
 *   computes y[n] = (1/1-|mu|) (x[n]+mu*x[n-1])
 *----------------------------------------------------------------------------
 */
private void filt_mu(
 float[] sig_in,     /* input : input signal (beginning at sample -1) */
 float[] sig_out,    /* output: output signal */
 int sig_out_offset,
 float parcor0      /* input : parcor0 (mu = parcor0 * gamma3) */
)
{
    int n;
    float mu, ga, temp;
    int ptrs;

    if(parcor0 > 0.f) {
        mu = parcor0 * GAMMA3_PLUS;
    }
    else {
        mu = parcor0 * GAMMA3_MINUS;
    }
    ga = 1.f / (1.f - Math.abs(mu));

    ptrs = 0;      /* points on sig_in(-1) */
    for(n=0; n<L_SUBFR; n++) {
        temp = mu * sig_in[ptrs];
        ptrs++;
        temp += sig_in[ptrs];
        sig_out[sig_out_offset + n] = ga * temp;
    }
}

/*----------------------------------------------------------------------------
 *   scale_st  - control of the subframe gain
 *   gain[n] = AGC_FAC * gain[n-1] + (1 - AGC_FAC) g_in/g_out
 *----------------------------------------------------------------------------
 */
private float scale_st(
 float[] sig_in,     /* input : postfilter input signal */
 int sig_in_offset,
 float[] sig_out,    /* in/out: postfilter output signal */
 int sig_out_offset,
 float gain_prec   /* in/out: last value of gain for subframe */
)
{
    float gain_in, gain_out;
    float g0;

    /* compute input gain */
    gain_in = 0.f;
    for(int i=sig_in_offset, toIndex = sig_in_offset + L_SUBFR; i<toIndex; i++) {
        gain_in += Math.abs(sig_in[i]);
    }
    if(gain_in == 0.f) {
        g0 = 0.f;
    }
    else {

        /* Compute output gain */
        gain_out = 0.f;
        for(int i=sig_out_offset, toIndex = sig_out_offset + L_SUBFR; i<toIndex; i++) {
            gain_out += Math.abs(sig_out[i]);
        }
        if(gain_out == 0.f) {
            gain_prec = 0.f;
            return gain_prec;
        }

        g0 = gain_in/ gain_out;
        g0 *= AGC_FAC1;
    }

    /* compute gain(n) = AGC_FAC gain(n-1) + (1-AGC_FAC)gain_in/gain_out */
    /* sig_out(n) = gain(n) sig_out(n)                                   */
    for(int i=sig_out_offset, toIndex = sig_out_offset + L_SUBFR; i<toIndex; i++) {
        gain_prec *= AGC_FAC;
        gain_prec += g0;
        sig_out[i] *= gain_prec;
    }
    return gain_prec;
}
}
