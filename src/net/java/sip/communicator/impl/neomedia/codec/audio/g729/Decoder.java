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

import java.io.*;

/**
 * Main program of the G.729  8.0 kbit/s decoder.
 * Usage : decoder  bitstream_file  synth_file.
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Decoder
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
 File : DECODER.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/

    /**
     * DecLd8k reference
     */
    private final DecLd8k decLd8k = new DecLd8k();

    /**
     * Postfil reference
     */
    private final Postfil postfil = new Postfil();

    /**
     * PostPro reference
     */
    private final PostPro postPro = new PostPro();
    /**
     * Synthesis
     */
    private final float[] synth_buf = new float[L_FRAME+M];

    /**
     * Synthesis
     */
    private final float[] synth;


    /**
     * Synthesis
     */
    private int synth_offset;

    /**
     * Synthesis parameters + BFI
     */
    private final int[] parm = new int[PRM_SIZE+1];

    /**
     * Synthesis parameters + BFI
     */
    private final float[] Az_dec = new float[2*MP1];

    /**
     *  postfilter output
     */
    private final float[]  pst_out = new float[L_FRAME];
    
    /**
     * voicing for previous subframe
     */
    private int voicing;
    
    /**
     * Initialization of decoder
     */
    Decoder()
    {

        synth = synth_buf;
        synth_offset = M;

        decLd8k.init_decod_ld8k();
        postfil.init_post_filter();
        postPro.init_post_process();

        voicing = 60;
    }

    /**
     * Converts floats array into shorts array.
     *
     * @param floats
     * @param shorts
     */
    private static void floats2shorts(float[] floats, short[] shorts)
    {
        for (int i = 0; i < floats.length; i++)
        {
            /* round and convert to int */
            float f = floats[i];
            if (f >= 0.0f)
                f += 0.5f;
            else
                f -= 0.5f;
            if (f > 32767.0f)
                f = 32767.0f;
            if (f < -32768.0f)
                f = -32768.0f;
            shorts[i] = (short) f;
        }
    }

/**
 * Main decoder routine
 * Usage :Decoder bitstream_file  outputspeech_file
 *
 * Format for bitstream_file:
 * One (2-byte) synchronization word
 *   One (2-byte) size word,
 *   80 words (2-byte) containing 80 bits.
 *
 * Format for outputspeech_file:
 *   Synthesis is written to a binary file of 16 bits data.
 *
 * @param args bitstream_file  outputspeech_file
 * @throws java.io.IOException
 */
public static void main(String[] args)
   throws IOException
{
   OutputStream f_syn;
   InputStream f_serial;

   short[]  serial = new short[SERIAL_SIZE];             /* Serial stream              */
   short[] sp16 = new short[L_FRAME];         /* Buffer to write 16 bits speech */

   int   frame;

   System.out.printf("\n");
   System.out.printf("**************    G.729  8 KBIT/S SPEECH DECODER    ************\n");
   System.out.printf("\n");
   System.out.printf("----------------- Floating point C simulation ----------------\n");
   System.out.printf("\n");
   System.out.printf("------------ Version 1.01 (Release 2, November 2006) --------\n");
   System.out.printf("\n");

   /* Passed arguments */

   if ( args.length != 2)
     {
        System.out.printf("Usage :Decoder bitstream_file  outputspeech_file\n");
        System.out.printf("\n");
        System.out.printf("Format for bitstream_file:\n");
        System.out.printf("  One (2-byte) synchronization word \n");
        System.out.printf("  One (2-byte) size word,\n");
        System.out.printf("  80 words (2-byte) containing 80 bits.\n");
        System.out.printf("\n");
        System.out.printf("Format for outputspeech_file:\n");
        System.out.printf("  Synthesis is written to a binary file of 16 bits data.\n");
        System.exit( 1 );
     }

   /* Open file for synthesis and packed serial stream */

   try
   {
      f_serial = new FileInputStream(args[0]);
   }
   catch (IOException ex)
   {
      System.out.printf("Decoder - Error opening file  %s !!\n", args[0]);
      System.exit(0);
      throw ex; // Silence the compiler.
   }

   try
   {
      f_syn = new FileOutputStream(args[1]);
   }
   catch (IOException ex)
   {
      System.out.printf("Decoder - Error opening file  %s !!\n", args[1]);
      System.exit(0);
      throw ex; // Silence the compiler.
   }

   System.out.printf("Input bitstream file  :   %s\n",args[0]);
   System.out.printf("Synthesis speech file :   %s\n",args[1]);

/*-----------------------------------------------------------------*
 *           Initialization of decoder                             *
 *-----------------------------------------------------------------*/

  Decoder decoder = new Decoder();

/*-----------------------------------------------------------------*
 *            Loop for each "L_FRAME" speech data                  *
 *-----------------------------------------------------------------*/

   frame =0;
   while(Util.fread(serial, SERIAL_SIZE, f_serial) == SERIAL_SIZE)
   {
      frame++;
      System.out.printf(" Frame: %d\r", frame);

      decoder.process(serial, sp16);

      Util.fwrite(sp16, L_FRAME, f_syn);
   }

   f_syn.close();
   f_serial.close();
}

    /**
     * Process <code>SERIAL_SIZE</code> short of speech.
     *
     * @param serial    input : serial array encoded in bits_ld8k
     * @param sp16      output : speech short array
     */
    void process(short[] serial, short[] sp16)
    {
        Bits.bits2prm_ld8k(serial, 2, parm, 1);

        /* the hardware detects frame erasures by checking if all bits
           are set to zero
        */
        parm[0] = 0;           /* No frame erasure */
        for (int i=2; i < SERIAL_SIZE; i++)
          if (serial[i] == 0 ) parm[0] = 1; /* frame erased     */

        /* check parity and put 1 in parm[4] if parity error */

        parm[4] = PParity.check_parity_pitch(parm[3], parm[4] );

        int t0_first = decLd8k.decod_ld8k(parm, voicing, synth, synth_offset, Az_dec);  /* Decoder */

        /* Post-filter and decision on voicing parameter */
        voicing = 0;

        float[] ptr_Az = Az_dec;          /* Decoded Az for post-filter */
        int ptr_Az_offset = 0;

        for(int i=0; i<L_FRAME; i+=L_SUBFR) {
          int sf_voic;                    /* voicing for subframe */

          sf_voic = postfil.post(t0_first, synth, synth_offset + i, ptr_Az, ptr_Az_offset, pst_out, i);
          if (sf_voic != 0) { voicing = sf_voic;}
          ptr_Az_offset += MP1;
        }
        Util.copy(synth_buf, L_FRAME, synth_buf, M);

        postPro.post_process(pst_out, L_FRAME);

        floats2shorts(pst_out, sp16);
    }
}
