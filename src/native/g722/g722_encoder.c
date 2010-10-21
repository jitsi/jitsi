/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**********************************************/
/*                                            */
/*****    Copyright (c) CMU    1993       *****/
/* Computer Science, Speech Group             */
/* Chengxiang Lu and Alex Hauptmann           */
/*                                            */
/*  This program performs the encoder for the */
/*  64 kb/s CCITT ADPCM codec. It reads 16kHz */
/*  sampled 14bit PCM values and writes high  */
/*  and low band ADPCM values(2bits and 6bits,*/
/*  respectively) out.                        */
/*                                            */
/*  With speed of 0.4xreal-time for coder     */
/*  and 0.3xreal-time for decoder on NeXT.    */
/*                                            */
/*                       encoder              */ 
/*  16kHzSamplingx16bit==============>64kb/s  */
/*                                            */
/**********************************************/

#include "g722_encoder.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int block4h(int);
static int block4l(int);

/*************************** block1l *********************************/
	static int q6[32] = {0, 35, 72, 110, 150, 190, 233, 276, 323,
		370, 422, 473, 530, 587, 650, 714, 786,
		858, 940, 1023, 1121, 1219, 1339, 1458,
		1612, 1765, 1980, 2195, 2557, 2919, 0, 0} ;

	static int iln[32] = {0, 63, 62, 31, 30, 29, 28, 27, 26, 25,
		24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14,
		13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 0 } ;

	static int ilp[32] = {0, 61, 60, 59, 58, 57, 56, 55, 54, 53, 52,
		51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41,
		40, 39, 38, 37, 36, 35, 34, 33, 32, 0 } ;
/*************************** BLOCK 2L ********************************/
	static int qm4[16] =
		{0,	-20456,	-12896,	-8968,
		-6288,	-4240,	-2584,	-1200,
		20456,	12896,	8968,	6288,
		4240,	2584,	1200,	0 } ;
/*************************** BLOCK 3L ********************************/
	static int wl[8] = {-60, -30, 58, 172, 334, 538, 1198, 3042 } ;
	static int rl42[16] = {0, 7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3, 2,
		1, 0 } ;
	static int ilb[32] = {2048, 2093, 2139, 2186, 2233, 2282, 2332,
		2383, 2435, 2489, 2543, 2599, 2656, 2714,
		2774, 2834, 2896, 2960, 3025, 3091, 3158,
		3228, 3298, 3371, 3444, 3520, 3597, 3676,
		3756, 3838, 3922, 4008 } ;
/*************************** BLOCK 1H ********************************/
	static int ihn[3] = { 0, 1, 0 } ;
	static int ihp[3] = { 0, 3, 2 } ;
/************************** BLOCK 2H *********************************/
	static int qm2[4] =
		{-7408,	-1616,	7408,	1616} ;
/************************** BLOCK 3H *********************************/
	static int wh[3] = {0, -214, 798} ;
	static int rh2[4] = {2, 1, 2, 1} ;

typedef struct
{
    int k; /* counter */
    int x[24]; /* storage for signal passing through the qmf */
    int slow;
    int detlow;
    int shigh;
    int dethigh;
    int nbl; /* block 3l */
    int nbh; /* block 3h */
}
g722_encoder_t;

/**************************** BLOCK 4H *******************************/
static int block4h (int d)
{
static int sh = 0 ;
	int wd1, wd2, wd3, wd4, wd5;
	static int sph = 0 ;
	static int szh = 0 ;
	static int rh  [3] = { 0, 0, 0 } ;
	static int ah   [3] = { 0, 0, 0 } ;
	static int ph  [3] = { 0, 0, 0 } ;
	static int dh  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
	static int bh   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
	static int sg   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;

	register int *sgp, *bhp, *dhp, *php, *ahp, *rhp;
	register int *dhp_1, *rhp_1, *php_1;
/*************************************** BLOCK 4H, RECONS ***********/

	*dh = d;
	*rh = sh + d ;
/*
        if ( rh[0] > 32767 ) rh[0] = 32767;
        else if ( rh[0] < -32768 ) rh[0] = -32768;
*/
/*************************************** BLOCK 4H, PARREC ***********/

	*ph = d + szh ;
/*
        if ( ph[0] > 32767 ) ph[0] = 32767;
        else if ( ph[0] < -32768 ) ph[0] = -32768;
*/
/*****************************BLOCK 4H, UPPOL2*************************/
	sgp = sg, php = ph, ahp =ah ;
	*sg = *ph >> 15 ;
	*++sgp = *++php >> 15 ;
	*++sgp = *++php >> 15 ;
	wd1 = (*++ahp) << 2;

        if ( wd1 > 32767 ) wd1 = 32767;
        else if ( wd1 < -32768 ) wd1 = -32768;

	wd2 = ( *sg == *--sgp ) ? - wd1 : wd1;
        if ( wd2 > 32767 ) wd2 = 32767;

	wd2 = wd2 >> 7 ;
	wd3 = ( *sg == *++sgp ) ? 128:-128 ;

	wd4 = wd2 + wd3 ;
	wd5 = (*++ahp * 32512) >> 15 ;

	*ahp = wd4 + wd5 ;
	if ( *ahp  >  12288 )  *ahp  =  12288 ;
	else if ( *ahp  < -12288 )  *ahp  = -12288 ;
/************************************* BLOCK 4H, UPPOL1 ***************/

	*sg = *ph >> 15 ;
	*--sgp  = *--php >> 15 ;
	wd1 = ( *sg == *sgp ) ? 192 : -192;

	wd2 = (*--ahp * 32640) >> 15 ;

 	*ahp  = wd1 + wd2 ;
/*
        if ( *ahp > 32767 ) *ahp = 32767;
        else if ( *ahp < -32768 ) *ahp = -32768;
*/
	wd3 = (15360 - *++ahp) ;
/*
        if ( wd3 > 32767 ) wd3 = 32767;
        else if ( wd3 < -32768 ) wd3 = -32768;
*/
	if ( *--ahp  >  wd3)  *ahp  =  wd3 ;
	else if ( *ahp  < -wd3)  *ahp  = -wd3 ;

/*************************************** BLOCK 4H, UPZERO ************/
	wd1 = ( d == 0 ) ? 0 : 128;

	*sg = d >> 15 ;
	dhp = dh, bhp = bh;
		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

		*sgp = *++dhp >> 15 ;
		wd2 = ( *sgp++  == *sg ) ? wd1 : -wd1;
		wd3 = (*++bhp * 32640) >> 15 ;
		*bhp = wd2 + wd3 ;
/*
                if ( *bhp > 32767 ) *bhp= 32767;
                else if ( *bhp < -32768 ) *bhp = -32768;
*/

/********************************* BLOCK 4H, DELAYA ******************/
	dhp_1 = dhp - 1;

		*dhp-- = *dhp_1-- ;
		*dhp-- = *dhp_1-- ;
		*dhp-- = *dhp_1-- ;
		*dhp-- = *dhp_1-- ;
		*dhp-- = *dhp_1-- ;
		*dhp-- = *dhp_1-- ;

	rhp = rh+2;
	php++;
        rhp_1 = rhp - 1, php_1 = php - 1;

		*rhp-- = *rhp_1-- ;
		*php-- = *php_1-- ;
		*rhp-- = *rhp_1-- ;
		*php-- = *php_1-- ;
/********************************* BLOCK 4H, FILTEP ******************/

	wd1 = ( *ahp * *++rhp ) >> 14 ;

	wd2 = ( *++ahp * *++rhp ) >> 14 ;

	sph = wd1 + wd2 ;
/*
        if ( sph > 32767 ) sph = 32767;
        if ( sph < -32768 ) sph = -32768;
*/
/*************************************** BLOCK 4H, FILTEZ ***********/

	bhp = bhp -6;
		szh = (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
		szh += (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
		szh += (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
		szh += (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
		szh += (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
		szh += (*++bhp * *++dhp ) >> 14 ;
/*
                if ( szh > 32767 ) szh = 32767;
                else if ( szh < -32768 ) szh = -32768;
*/
/*********************************BLOCK 4L, PREDIC *******************/

	sh = sph + szh ;
/*
        if ( sh > 32767 ) sh = 32767;
        if ( sh < -32768 ) sh = -32768;
*/
	return (sh) ;
}

/**************************** BLOCK 4L *******************************/
static int block4l (int dl)
{
static int sl = 0 ;
	int wd1, wd2, wd3, wd4, wd5;
	static int spl = 0 ;
	static int szl = 0 ;
	static int rlt  [3] = { 0, 0, 0 } ;
	static int al   [3] = { 0, 0, 0 } ;
	static int plt  [3] = { 0, 0, 0 } ;
	static int dlt  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
	static int bl   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
	static int sg   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
/****************** pointer ****************************/
	register int *sgp, *pltp, *alp, *dltp, *blp, *rltp;
	register int *pltp_1, *dltp_1, *rltp_1;
/*************************************** BLOCK 4L, RECONS ***********/
	*dlt = dl;

	*rlt = sl + dl ;
/*
        if ( *rlt > 32767 ) *rlt = 32767;
        else if ( *rlt < -32768 ) *rlt = -32768;
*/
/*************************************** BLOCK 4L, PARREC ***********/

	*plt = dl + szl ;
/*
        if ( plt[0] > 32767 ) plt[0] = 32767;
        else if ( plt[0] < -32768 ) plt[0] = -32768;
*/
/*****************************BLOCK 4L, UPPOL2*************************/
	sgp = sg, pltp = plt, alp = al ;
		*sgp++ = *pltp++ >> 15 ;
		*sgp++ = *pltp++ >> 15 ;
		*sgp++ = *pltp++ >> 15 ;

	wd1 = *++alp << 2;

        if ( wd1 > 32767 ) wd1 = 32767;
        else if ( wd1 < -32768 ) wd1 = -32768;

	wd2= ( *sg == *(sg+1) )?  -wd1: wd1 ;
        if ( wd2 > 32767 ) wd2 = 32767;

	wd2 = wd2 >> 7 ;
	wd3= ( *sg == *(sg+2) )? 128: -128 ;
	wd4 = wd2 + wd3 ;
	wd5 = (*++alp * 32512) >> 15 ;
	
	*alp = wd4 + wd5 ;

	if ( *alp  >  12288 )  *alp =  12288 ;
	else if ( *alp  < -12288 )  *alp = -12288 ;

/************************************* BLOCK 4L, UPPOL1 ***************/

	*sg = *plt >> 15 ;
	*(sg+1) = *(plt+1) >> 15 ;
	wd1 = ( *sg == *(sg+1) )?  192 : -192 ;

	wd2 = (*--alp * 32640) >> 15 ;

	*alp = wd1 + wd2 ;
/*
        if ( *alp > 32767 ) *alp = 32767;
        else if ( *alp < -32768 ) *alp = -32768;
*/
	wd3 = (15360 - *++alp) ;
/*
        if ( wd3 > 32767 ) wd3 = 32767;
        else if ( wd3 < -32768 ) wd3 = -32768;
*/
	if ( *--alp >  wd3)  *alp =  wd3 ;
	else if ( *alp  < -wd3)  *alp = -wd3 ;

/*************************************** BLOCK 4L, UPZERO ************/
	wd1 = ( dl == 0 ) ? 0 : 128;
	*sg = dl >> 15 ;
	sgp = sg, dltp = dlt, blp = bl; 

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

		*++sgp = *++dltp >> 15 ;
		wd2 = ( *sgp == *sg ) ? wd1 : -wd1 ;
		wd3 = (*++blp * 32640) >> 15 ;
		*blp = wd2 + wd3 ;
/*
                if ( *blp > 32767 ) *blp = 32767;
                else if ( *blp < -32768 ) *blp = -32768;
*/

/********************************* BLOCK 4L, DELAYA ******************/
	dltp_1 = dltp - 1; 
		*dltp--  = *dltp_1-- ;
		*dltp--  = *dltp_1-- ;
		*dltp--  = *dltp_1-- ;
		*dltp--  = *dltp_1-- ;
		*dltp--  = *dltp_1-- ;
		*dltp--  = *dltp_1-- ;

	rltp =rlt+2, pltp = plt+2 ;
        rltp_1 = rltp - 1, pltp_1 = pltp - 1;

		*rltp-- = *rltp_1--;
		*pltp-- = *pltp_1--; 
		*rltp-- = *rltp_1--;
		*pltp-- = *pltp_1--; 
/********************************* BLOCK 4L, FILTEP ******************/

	wd1 = ( *alp * *++rltp ) >> 14 ;

	wd2 = ( *++alp * *++rltp ) >> 14 ;

	spl = wd1 + wd2 ;
/*
        if ( spl > 32767 ) spl = 32767;
        else if ( spl < -32768 ) spl = -32768;
*/
/*************************************** BLOCK 4L, FILTEZ ***********/

	blp = blp - 6;
		szl = (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
		szl += (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
		szl += (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
		szl += (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
		szl += (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
		szl += (*++blp * *++dltp) >> 14 ;
/*
                if ( szl > 32767 ) szl = 32767;
                else if ( szl < -32768 ) szl = -32768;
*/
/*********************************BLOCK 4L, PREDIC *******************/

	sl = spl + szl ;
/*
        if ( sl > 32767 ) sl = 32767;
        else if ( sl < -32768 ) sl = -32768;
*/

	return (sl) ;
}

void g722_encoder_close(void *encoder)
{
    free(encoder);
}

void *g722_encoder_open()
{
    g722_encoder_t *e = malloc(sizeof(g722_encoder_t));

    if (e)
    {
        e->k = 1;
        memset(e->x, 0, sizeof(e->x));
        e->slow = 0;
        e->detlow = 32;
        e->shigh = 0;
        e->dethigh = 8;
        e->nbl = 0;
        e->nbh = 0;
    }
    return e;
}

void g722_encoder_process(
        void *encoder,
        short *input, unsigned short *output, int outputLength)
{
    g722_encoder_t *e = (g722_encoder_t *) encoder;

	int j=0;

	while( j< outputLength ){
	register int *xp, *xp_2, *q6p;
	int sumeven, sumodd;       /* even and odd tap accumulators  */
	int xlow, xhigh;           /* low and high band pcm from qmf */
	register int el, wd, mil, wd1;
	int wd2;
	int ilow=0, ihigh=0;
	int ril;
	int dlowt;
	int  il4, wd3;
	int eh, mih;
	register int hdu;
	int dhigh;
	int ih2;

	int detlow, nbl, nbh;

/* PROCESS PCM THROUGH THE QMF FILTER                               */

nexttwodata:
	xp = (e->x) + 23; xp_2 = (e->x) +21;

	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;
	*xp-- = *xp_2--;


	*xp = *input++;
	*--xp = *input++;

/* DISCARD EVERY OTHER QMF OUTPUT                                   */

		sumeven = *xp++ * 3;
		sumodd = *xp++ * (-11);
		sumeven += *xp++ * (-11);
		sumodd += *xp++ * 53;
		sumeven += *xp++ * 12;
		sumodd += *xp++ * (-156);
		sumeven += *xp++ * 32;
		sumodd += *xp++ * 362;
		sumeven += *xp++ * (-210);
		sumodd += *xp++ * (-805);
		sumeven += *xp++ * 951;
		sumodd += *xp++ * 3876;
		sumeven += *xp++ * 3876;
		sumodd += *xp++ * 951;
		sumeven += *xp++ * (-805);
		sumodd += *xp++ * (-210);
		sumeven += *xp++ * 362;
		sumodd += *xp++ * 32;
		sumeven += *xp++ * (-156);
		sumodd += *xp++ * 12;
		sumeven += *xp++ * 53;
		sumodd += *xp++ * (-11);
		sumeven += *xp++ * (-11);
		sumodd += *xp++ * 3;

	xlow = (sumeven + sumodd) >>13;
	xhigh = (sumeven - sumodd) >>13;

/*************************************** BLOCK 1L, SUBTRA ************/

	el = xlow - (e->slow);

        if ( el > 32767 ) el = 32767;
        else if ( el < -32768 ) el = -32768;

/*************************************** BLOCK 1L, QUANTL ************/

	wd= (el>= 0 ) ?  el: -(el+1) ;

	q6p = q6;
	detlow = e->detlow;
	mil = 1;
		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

		wd2 = *++q6p * detlow ;
		wd1 = wd2 >> 12 ;
		if (wd >= wd1)  mil++;
		else goto next_ilow;

next_ilow:
	ilow = ( el < 0 ) ? iln[mil] : ilp[mil];

/************************************** BLOCK 2L, INVQAL ************/

	ril = ilow >> 2 ;
	wd2 = qm4[ril] ;
	dlowt = (detlow * wd2) >> 15 ;

/************************************** BLOCK 3L, LOGSCL *************/

	il4 = rl42[ril] ;

	nbl = e->nbl;
	wd = (nbl * 127) >> 7 ;

	nbl = wd + wl[il4] ;

	if (nbl <     0) nbl = 0 ;
	else if (nbl > 18432) nbl = 18432 ;
	e->nbl = nbl;

/************************************** BLOCK 3L, SCALEL *************/
	wd1 =  (nbl >> 6) & 31 ;
	wd2 = nbl >> 11 ;
	wd3=((8 - wd2) < 0) ? ilb[wd1]<<(wd2 - 8) : ilb[wd1]>>(8 - wd2);
	e->detlow = detlow  = wd3 << 2 ;
/************************************** BLOCK 3L, DELAYA *************/

	e->slow = block4l (dlowt) ;


/*************************************** BLOCK 1H, SUBTRA ************/

	eh = xhigh - (e->shigh);
/*
        if ( eh > 32767 ) eh = 32767;
        else if ( eh < -32768 ) eh = -32768;
*/
/*************************************** BLOCK 1H, QUANTH ************/

	wd = (eh>= 0 )? eh : -( eh +1 ) ;

	hdu = 564 * (e->dethigh);
	wd1 = hdu >> 12 ;
	mih = (wd >= wd1)? 2: 1 ;
	ihigh = (eh<0) ? ihn[mih] : ihp[mih] ;

/************************************** BLOCK 2H, INVQAH ************/

	wd2 = qm2[ihigh] ;
	dhigh = ((e->dethigh) * wd2) >> 15 ;


/************************************** BLOCK 3H, LOGSCH *************/

	ih2 = rh2[ihigh] ;

	nbh = e->nbh;
	wd = (nbh * 127) >> 7 ;
	nbh = wd + wh[ih2] ;

	if (nbh <     0) nbh = 0 ;
	else if (nbh > 22528) nbh = 22528 ;
	e->nbh = nbh;

/************************************** BLOCK 3H, SCALEH *************/
	wd1 =  (nbh >> 6) & 31 ;
	wd2 = nbh >> 11 ;
	wd3=((10-wd2) < 0)? ilb[wd1] << (wd2-10): ilb[wd1] >> (10-wd2) ;
	e->dethigh = wd3 << 2 ;
/************************************** BLOCK 3L, DELAYA *************/

	e->shigh = block4h (dhigh) ;
	if((e->k)>0){
                output[j] = ilow;
                output[j] = (output[j]<<2) + ihigh;
                e->k = -(e->k);
                goto nexttwodata;
        }
        else {
                output[j] = (output[j]<<6) + ilow;
                output[j] = (output[j]<<2) + ihigh;
                e->k = -(e->k);
        }
	j++;
	}
}
