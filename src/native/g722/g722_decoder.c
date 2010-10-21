/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**********************************************/
/*                                            */
/*****    Copyright (c) CMU    1993      *****/
/* Computer Science, Speech Group             */
/* Chengxiang Lu and Alex Hauptmann           */
/*                                            */
/*  This program performs the decoder for the */
/*  64 kb/s codec. It reads ADPCM values and  */
/*  writes PCM values to  standard output     */
/*                                            */
/*  With speed of 0.4xreal-time for coder     */
/*  and 0.3xreal-time for decoder on NeXT.    */
/*                                            */
/*             decoder                        */
/*  64kbit/s============>16kHzSamplingx16bit  */
/*                                            */
/**********************************************/

#include "g722_decoder.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int block4h(int);
static int block4l(int);

    /********************************  block3l *********************/
    static int wl[8] = {-60, -30, 58, 172, 334, 538, 1198, 3042 }  ;
    static int rl42[16] = {0, 7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3,  2, 1, 0 } ;
    static int ilb[32] = {2048, 2093, 2139, 2186, 2233, 2282, 2332,
                              2383, 2435, 2489, 2543, 2599, 2656, 2714,
                              2774, 2834, 2896, 2960, 3025, 3091, 3158,
                              3228, 3298, 3371, 3444, 3520, 3597, 3676,
                              3756, 3838, 3922, 4008 } ;
    /************************************** block3h  ***************/
    static int wh[3] = {0, -214, 798} ;
    static int rh2[4] = {2, 1, 2, 1} ;
    /******************* block5l ***********************************/
    static int qm6[64] =
    {-136,      -136,   -136,   -136,
         -24808,        -21904, -19008, -16704,
         -14984,        -13512, -12280, -11192,
         -10232,        -9360,  -8576,  -7856,
         -7192, -6576,  -6000,  -5456,
         -4944, -4464,  -4008,  -3576,
         -3168, -2776,  -2400,  -2032,
         -1688, -1360,  -1040,  -728,
         24808, 21904,  19008,  16704,
         14984, 13512,  12280,  11192,
         10232, 9360,   8576,   7856,
         7192,  6576,   6000,   5456,
         4944,  4464,   4008,   3576,
         3168,  2776,   2400,   2032,
         1688,  1360,   1040,   728,
         432,   136,    -432,   -136 } ;
    /********************** block 2h *******************************/
    static int qm2[4] = {-7408, -1616,  7408,   1616} ;
    /********************** block 2l *******************************/
    static int qm4[16] =
    {0, -20456, -12896, -8968,
         -6288, -4240,  -2584,  -1200,
         20456, 12896,  8968,   6288,
         4240,  2584,   1200,   0 } ;

typedef struct
{
    int slow;
    int detlow;
    int shigh;
    int dethigh;
    int xd[12];
    int xs[12];
    int k; /* counter */
    int nbl;
    int nbh;
}
g722_decoder_t;

static int block4h (int d)
{
    static int sh = 0 ;
    register int wd1, wd2, wd3;
    static int sph = 0 ;
    static int szh = 0 ;

    static int rh0=0, rh1=0, rh2=0 ;
    static int ah1=0, ah2=0 ;
    static int ph0=0, ph1=0, ph2=0 ;

    static int dh  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int bh   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int bph  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int sg0=0, sg1=0, sg2=0;
    static int sg   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;

    register int *dhp, *dhp_1, *bhp, *bphp, *sgp;

    dh[0] = d;
    /*************************************** BLOCK 4H, RECONS  ***********/



    rh0 = sh + d ;
    /*
      if (rh0 > 32767) rh0 = 32767;
      if (rh0 < -32768) rh0 = -32768;
      */

    /*************************************** BLOCK 4H, PARREC  ***********/


    ph0 = d + szh ;
    /*
      if (ph0 > 32767) ph0 = 32767;
      if (ph0 < -32768) ph0 = -32768;
      */


    /*****************************BLOCK 4H,  UPPOL2*************************/


    sg0 = ph0 >> 15 ;
    sg1 = ph1 >> 15 ;
    sg2 = ph2 >> 15 ;

    wd1 = ah1 << 2;

    if (wd1 > 32767)
	wd1 = 32767;
    else if (wd1 < -32768)
	wd1 = -32768;

    wd2= ( sg0 == sg1 )? - wd1 : wd1 ;
    if (wd2 > 32767) wd2 = 32767;


    wd2 = wd2 >> 7 ;


    wd3= ( sg0 == sg2 )? 128 : - 128 ;


    wd2 = wd2 + wd3 ;
    wd3 = (ah2 * 127) >> 7 ;


    ah2 = wd2 + wd3 ;
    if ( ah2 >  12288 )
	ah2 =  12288 ;
    else if ( ah2 < -12288 )
	ah2 = -12288 ;
    /************************************* BLOCK 4H, UPPOL1  ***************/


    sg0 = ph0 >> 15 ;
    sg1 = ph1 >> 15 ;


    wd1= ( sg0 == sg1 )? 192 : - 192 ;


    wd2 = (ah1 * 255) >> 8 ;


    ah1 = wd1 + wd2 ;
    /*
      if (ah2 > 32767) ah2 = 32767;
      if (ah2 < -32768) ah2 = -32768;
      */

    wd3 = (15360 - ah2) ;
    /*
      if (wd3 > 32767) wd3 = 32767;
      if (wd3 < -32768) wd3 = -32768;
      */
    if ( ah1 >  wd3)
	ah1 =  wd3 ;
    else if ( ah1 < -wd3)
	ah1 = -wd3 ;


    /*************************************** BLOCK 4H, UPZERO  ************/


    wd1= ( d == 0 )? 0 : 128 ;


    sg0 = d >> 15 ;
	sgp = sg, dhp = dh, bhp = bh, bphp = bph;

	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
	*++sgp = *++dhp >> 15 ;
	wd2= ( *sgp == sg0 )? wd1 : - wd1 ;
	wd3 = (*++bhp * 255) >> 8 ;
	*++bphp = wd2 + wd3 ;
    /********************************* BLOCK 4H, DELAYA  ******************/
    dhp_1=dhp-1,bhp=bh, bphp=bph;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
	*dhp--=*dhp_1--;
	*++bhp=*++bphp;
    rh2=rh1;
    rh1=rh0;
    ph2=ph1;
    ph1=ph0;
    /********************************* BLOCK 4H, FILTEP  ******************/

    wd1 = ( ah1 * rh1 ) >> 14 ;

    wd2 = ( ah2 * rh2 ) >> 14 ;

    sph = wd1 + wd2 ;
    /*
      if (sph > 32767) sph = 32767;
      if (sph < -32768) sph = -32768;
      */

    /*************************************** BLOCK 4H, FILTEZ  ***********/


    dhp = dh, bhp = bh;
	szh = ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/
	szh += ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/
	szh += ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/
	szh += ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/
	szh += ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/
	szh += ((*++bhp) * *++dhp )>>14;
	/*
	  if (szh > 32767) szh = 32767;
	  else if (szh < -32768) szh = -32768;
	*/

    /*********************************BLOCK 4H, PREDIC  *******************/


    sh = sph + szh ;
    /*
      if (sh > 32767) sh = 32767;
      if (sh < -32768) sh = -32768;
      */

    return (sh) ;
}

/**************************** BLOCK 4L  *******************************/
static int block4l (int dl)
{
    static int sl = 0 ;
    register int wd1, wd2, wd3;
    static int spl = 0 ;
    static int szl = 0 ;
    
    static int rlt0=0, rlt1=0, rlt2=0;
    static int al1=0, al2=0;
    static int plt0=0, plt1=0, plt2=0;
    
    static int dlt  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int bl   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int bpl  [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    static int sg0=0, sg1=0, sg2=0;
    static int sg   [7] = { 0, 0, 0, 0, 0, 0, 0 } ;
    
    register int *sgp, *dltp, *dltp_1, *blp, *bplp;
    
    dlt[0] = dl;
    /*************************************** BLOCK 4L, RECONS  ***********/ 
    
    rlt0 = sl + dl ;
    /*
      if (rlt0 > 32767) rlt0 = 32767;
      if (rlt0 < -32768) rlt0 = -32768;
      */
    /*************************************** BLOCK 4L, PARREC  ***********/
    
    plt0 = dl + szl ;
    /*
      if (plt0 > 32767) plt0 = 32767;
      if (plt0 < -32768) plt0 = -32768;
      */
    
    /*****************************BLOCK 4L,  UPPOL2*************************/
    
    sg0 = plt0 >> 15 ;
    sg1 = plt1 >> 15 ;
    sg2 = plt2 >> 15 ;
    
    wd1 = al1 << 2;
    if (wd1 > 32767)
	wd1 = 32767;
    else if (wd1 < -32768)
	wd1 = -32768;
    
    wd2= ( sg0 == sg1 )? - wd1 : wd1 ;
    /*
      if (wd2 > 32767) wd2 = 32767;
      */
    wd2 = wd2 >> 7 ;
    
    wd3= ( sg0 == sg2 )? 128 : - 128 ;
    
    wd2 = wd2 + wd3 ;
    wd3 = (al2 * 127) >> 7 ;
    
    al2 = wd2 + wd3 ;
    if ( al2 >  12288 )
	al2 =  12288 ;
    else if ( al2 < -12288 )
	al2 = -12288 ;
    /************************************* BLOCK 4L, UPPOL1  ***************/
    
    sg0 = plt0 >> 15 ;
    sg1 = plt1 >> 15 ;
    
    wd1= ( sg0 == sg1 )? 192 : - 192 ;
    
    wd2 = (al1 * 255) >> 8 ;
    
    al1 = wd1 + wd2 ;
    /*
      if (al1 > 32767) al1 = 32767;
      if (al1 < -32768) al1 = -32768;
      */ 
    
    wd3 = (15360 - al2) ;
    /*
      if (wd3 > 32767) wd3 = 32767;
      if (wd3 < -32768) wd3 = -32768;
      */
    if ( al1 >  wd3)
	al1 =  wd3 ;
    else if ( al1 < -wd3)
	al1 = -wd3 ;
    
    /*************************************** BLOCK 4L, UPZERO  ************/
    
    wd1= ( dl == 0 )?  0 : 128 ;
    
    sg0 = dl >> 15 ;
    
    sgp=sg, dltp=dlt, blp=bl, bplp=bpl;
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
	*++sgp = *++dltp >> 15 ;
	wd2= ( *sgp == sg0 )?  wd1 : - wd1 ;
	wd3 = (*++blp * 255) >> 8 ;
	*++bplp = wd2 + wd3 ;
/*
  	if (*bplp > 32767) *bplp = 32767;
	else if (*bplp < -32768) *bplp = -32768;
*/
    /********************************* BLOCK 4L, DELAYA  ******************/
    
    dltp=dlt+6, dltp_1=dltp-1, blp=bl, bplp=bpl;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;
	*dltp-- = *dltp_1--;
	*++blp  = *++bplp;

    rlt2=rlt1;
    rlt1=rlt0;
    plt2=plt1;
    plt1=plt0;
    
    /********************************* BLOCK 4L, FILTEP  ******************/
    
    wd1 = ( al1 * rlt1 ) >> 14 ;
    
    wd2 = ( al2 * rlt2 ) >> 14 ;
    
    spl = wd1 + wd2 ;
    /*
      if (spl > 32767) spl = 32767;
      if (spl < -32768) spl = -32768;
      */ 
    
    /*************************************** BLOCK 4L, FILTEZ  ***********/
    
    
    dltp = dlt, blp=bl;
	szl = (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
	szl += (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
	szl += (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
	szl += (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
	szl += (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
	szl += (*++blp * *++dltp)>>14;
	/*
	  if (szl > 32767) szl = 32767;
	  else if (szl < -32768) szl = -32768;
	*/
    
    /*********************************BLOCK 4L, PREDIC  *******************/
    
    
    sl = spl + szl ;
    /*
      if (sl > 32767) sl = 32767;
      if (sl < -32768) sl = -32768;
      */
    
    return (sl) ;
}

void g722_decoder_close(void *decoder)
{
    free(decoder);
}

void *g722_decoder_open()
{
    g722_decoder_t *d = malloc(sizeof(g722_decoder_t));

    if (d)
    {
        d->slow = 0;
        d->detlow = 32;
        d->shigh = 0;
        d->dethigh = 8;
        memset(d->xd, 0, sizeof(d->xd));
        memset(d->xs, 0, sizeof(d->xs));
        d->k = 1;
        d->nbl = 0; /* block3l */
        d->nbh = 0; /* block3h */
    }
    return d;
}

void g722_decoder_process(
        void *decoder,
        unsigned short *input, short *output, int outputLength)
{
    g722_decoder_t *d = (g722_decoder_t *) decoder;

    int j;

    for(j=0; j < outputLength;) {
	int ilowr, dlowt, rlow;
	int ihigh, dhigh, rhigh;
	register int wd1, wd2, wd3;
	register int *xdp, *xsp, *xdp_1, *xsp_1;
	register int xout1;

	int nbl, nbh;

	ilowr = *input >> 10;
	ihigh = (*input >> 8) & 3;

nexttwodata:
	/*********************** BLOCK 5L, LOW BAND INVQBL ****************/

	wd2 = qm6[ilowr] ;
	wd2 = ((d->detlow) * wd2 ) >> 15 ;
	/******************************** BLOCK 5L, RECONS ****************/
	rlow = (d->slow) + wd2 ;
	/*
	  if (rlow > 32767) rlow = 32767;
	  if (rlow < -32768) rlow = -32768;
	  */
	/******************************** BLOCK 6L, LIMIT ******************/
	/*
	  if (rlow > 16383 ) rlow = 16383;
	  if (rlow < -16383 ) rlow = -16383;
	  */
	/************************************** BLOCK 2L, INVQAL ************/

	wd1 = ilowr >> 2 ;
	wd2 = qm4[wd1] ;
	dlowt = ((d->detlow) * wd2) >> 15 ;
	/************************************** BLOCK 3L, LOGSCL *************/

        wd2 = rl42[wd1] ;
        nbl = d->nbl;
        wd1 = (nbl * 127) >> 7 ;
        nbl = wd1 + wl[wd2] ;
        if (nbl <     0)
	    nbl = 0 ;
        else if (nbl > 18432)
	    nbl = 18432 ;
        d->nbl = nbl;
	/************************************** BLOCK 3L, SCALEL *************/

        wd1 =  (nbl >> 6) & 31 ;
        wd2 = nbl >> 11 ;
        wd3= ((8 - wd2) < 0)? ilb[wd1] << (wd2 - 8) : ilb[wd1] >> (8 - wd2) ;
        d->detlow = wd3 << 2 ;

	d->slow = block4l (dlowt) ;
	/************************************** BLOCK 2H, INVQAH ************/

	wd2 = qm2[ihigh] ;
	dhigh  = ((d->dethigh) * wd2) >> 15 ;

        rhigh  = dhigh + (d->shigh);
	/*
	  if (rhigh >  16383 )  rhigh =  16383 ;
	  if (rhigh < -16384 )  rhigh = -16384 ;
	  */
	/************************************** BLOCK 2H, INVQAH ************/

        wd2 = rh2[ihigh] ;
        nbh = d->nbh;
        wd1 = (nbh * 127) >> 7 ;
        nbh = wd1 + wh[wd2] ;


        if (nbh <     0)
	    nbh = 0 ;
	else if (nbh > 22528)
	    nbh = 22528 ;
        d->nbh = nbh;
	/************************************** BLOCK 3H, SCALEH *************/

        wd1 =  (nbh >> 6) & 31 ;
        wd2 = nbh >> 11 ;
        wd3= ((10 - wd2) < 0)? ilb[wd1] << (wd2 - 10) : ilb[wd1] >> (10 - wd2) ;
        d->dethigh = wd3 << 2 ;

	d->shigh = block4h (dhigh) ;
	/******************************* RECIEVE QMF ************************/

	xdp=(d->xd)+11, xsp=(d->xs)+11, xdp_1=(d->xd)+10, xsp_1=(d->xs)+10;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
         *xdp-- = *xdp_1--; *xsp-- = *xsp_1--;
	/************************************* RECA ***************************/

        *xdp  = rlow - rhigh ;
	/*
	  if (*xdp > 16383) *xdp = 16383;
	  if (*xdp < -16384) *xdp = -16384;
	  */

	/************************************* RECA ***************************/

        *xsp  = rlow + rhigh ;
	/*
	  if (*xsp > 16383) *xsp = 16383;
	  if (*xsp < -16384) *xsp = -16384;
	  */
	/************************************* ACCUM C&D *************************/
                                                /* qmf tap coefficients          */
            xout1 = *xdp++ * 3;
            xout1 += *xdp++ * -11;
            xout1 += *xdp++ * 12;
            xout1 += *xdp++ * 32;
            xout1 += *xdp++ * -210;
            xout1 += *xdp++ * 951;
            xout1 += *xdp++ * 3876;
            xout1 += *xdp++ * -805;
            xout1 += *xdp++ * 362;
            xout1 += *xdp++ * -156;
            xout1 += *xdp++ * 53;
            xout1 += *xdp++ * -11;

            *output++ = xout1 >> 12 ;

            xout1 = *xsp++ * -11;
            xout1 += *xsp++ * 53;
            xout1 += *xsp++ * -156;
            xout1 += *xsp++ * 362;
            xout1 += *xsp++ * -805;
            xout1 += *xsp++ * 3876;
            xout1 += *xsp++ * 951;
            xout1 += *xsp++ * -210;
            xout1 += *xsp++ * 32;
            xout1 += *xsp++ * 12;
            xout1 += *xsp++ * -11;
            xout1 += *xsp++ * 3;

            *output++ = xout1 >> 12 ;

	j++, j++;

	d->k=-(d->k);
	/*
	  if (xout1 >  16383)  xout1 =  16383 ;
	  if (xout1 < -16384)  xout1 = -16384 ;
	  if (xout1 >  16383)  xout1 =  16383 ;
	  if (xout1 < -16384)  xout1 = -16384 ;
	  */
	if((d->k)<0){
	ilowr = (*input >> 2) & 63;
	ihigh = (*input++) & 3;
	goto nexttwodata;
	}
    }
}
