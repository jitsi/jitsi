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
 * Bit stream manipulation routines.
 * <p>prm2bits_ld8k -converts encoder parameter vector into vector of serial bits</p>
 * <p>bits2prm_ld8k - converts serial received bits to  encoder parameter vector</p>
 * <pre>
 * The transmitted parameters for 8000 bits/sec are:
 *
 *     LPC:     1st codebook           7+1 bit
 *              2nd codebook           5+5 bit
 *
 *     1st subframe:
 *          pitch period                 8 bit
 *          parity check on 1st period   1 bit
 *          codebook index1 (positions) 13 bit
 *          codebook index2 (signs)      4 bit
 *          pitch and codebook gains   4+3 bit
 *
 *     2nd subframe:
 *          pitch period (relative)      5 bit
 *          codebook index1 (positions) 13 bit
 *          codebook index2 (signs)      4 bit
 *          pitch and codebook gains   4+3 bit
 * </pre>
 * 
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Bits
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
 File : BITS.C
 Used for the floating point version of both
 G.729 main body and G.729A
*/


/**
 * Converts encoder parameter vector into vector of serial bits.
 *
 * @param prm        input : encoded parameters
 * @param bits       output: serial bits
 */
static void prm2bits_ld8k(
 int  prm[],         
 short bits[]      
)
{
   int PRM_SIZE = Ld8k.PRM_SIZE;
   short SIZE_WORD = Ld8k.SIZE_WORD;
   short SYNC_WORD = Ld8k.SYNC_WORD;
   int[] bitsno = TabLd8k.bitsno;

   int  j = 0, i;
   bits[j] = SYNC_WORD;     /* At receiver this bit indicates BFI */
   j++;
   bits[j] = SIZE_WORD;     /* Number of bits in this frame       */
   j++;

   for (i = 0; i < PRM_SIZE; i++)
   {
      int2bin(prm[i], bitsno[i], bits, j);
      j += bitsno[i];
   }
}

/**
 * Convert integer to binary and write the bits bitstream array.
 *
 * @param value             input : decimal value
 * @param no_of_bits        input : number of bits to use
 * @param bitstream         output: bitstream
 * @param bitstream_offset  input: bitstream offset
 */
private static void int2bin(
 int  value,          
 int  no_of_bits,       
 short[] bitstream,       
 int bitstream_offset
)
{
   short BIT_0 = Ld8k.BIT_0;
   short BIT_1 = Ld8k.BIT_1;

   int pt_bitstream;
   int    i, bit;

   pt_bitstream = bitstream_offset + no_of_bits;

   for (i = 0; i < no_of_bits; i++)
   {
     bit = value & 0x0001;      /* get lsb */
     if (bit == 0)
         bitstream[--pt_bitstream] = BIT_0;
     else
         bitstream[--pt_bitstream] = BIT_1;
     value >>= 1;
   }
}

/**
 * Converts serial received bits to  encoder parameter vector.
 *
 * @param bits  input : serial bits
 * @param prm   output: decoded parameters
 */
static void bits2prm_ld8k(short[] bits, int[] prm)
{
    bits2prm_ld8k(bits, 0, prm, 0);
}

/**
 * Converts serial received bits to  encoder parameter vector.
 *
 * @param bits           input : serial bits
 * @param bits_offset    input : serial bits offset
 * @param prm            output: decoded parameters
 * @param prm_offset     input: decoded parameters offset
 */
static void bits2prm_ld8k(
 short bits[],
 int bits_offset,
 int  prm[], 
 int prm_offset
)
{
   int PRM_SIZE = Ld8k.PRM_SIZE;
   int[] bitsno = TabLd8k.bitsno;

   int  i;
   for (i = 0; i < PRM_SIZE; i++)
   {
      prm[i+prm_offset] = bin2int(bitsno[i], bits, bits_offset);
      bits_offset  += bitsno[i];
   }
}


/**
 * Read specified bits from bit array  and convert to integer value.
 *
 * @param no_of_bits        input : number of bits to read
 * @param bitstream         input : array containing bits
 * @param bitstream_offset  input : array offset
 * @return                   decimal value of bit pattern
 */
private static int bin2int( 
 int  no_of_bits,   
 short[] bitstream,     
 int bitstream_offset
)
{
   short BIT_1 = Ld8k.BIT_1;

   int    value, i;
   int  bit;

   value = 0;
   for (i = 0; i < no_of_bits; i++)
   {
     value <<= 1;
     bit = bitstream[bitstream_offset ++];
     if (bit == BIT_1)  value += 1;
   }
   return(value);
}
}
