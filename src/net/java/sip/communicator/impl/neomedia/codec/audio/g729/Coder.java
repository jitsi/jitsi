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

import java.io.*;

/**
 * Main program of the ITU-T G.729   8 kbit/s encoder.
 * Usage : coder speech_file  bitstream_file
 *
 * @author Lubomir Marinov (translation of ITU-T C source code to Java)
 */
class Coder
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
 File : CODER.C
 Used for the floating point version of G.729 main body
 (not for G.729A)
*/

    /**
     * Init the PreProc
     */
    private final PreProc preProc = new PreProc();

    /**
     * Init the Ld8k Coder
     */
    private final CodLd8k codLd8k = new CodLd8k();
   
    /**
     *  Transmitted parameters
     */
    private final int[] prm = new int[PRM_SIZE];
    
    /**
     * Initialization of the coder.
     */
    Coder()
    {
         preProc.init_pre_process();
         codLd8k.init_coder_ld8k();           /* Initialize the coder             */
    }
/**
 * Usage : coder  speech_file  bitstream_file
 *
 * Format for speech_file:
 *  Speech is read form a binary file of 16 bits data.
 *
 * Format for bitstream_file:
 *   One word (2-bytes) to indicate erasure.
 *   One word (2 bytes) to indicate bit rate
 *   80 words (2-bytes) containing 80 bits.
 *
 * @param args speech_file  bitstream_file
 * @throws java.io.IOException
 */
public static void main(String[] args)
   throws IOException
{
   InputStream f_speech;                     /* Speech data        */
   OutputStream f_serial;                     /* Serial bit stream  */

   short[] sp16 = new short[L_FRAME];         /* Buffer to read 16 bits speech */
   short[] serial = new short[SERIAL_SIZE];   /* Output bit stream buffer      */

   int   frame;

   System.out.printf("\n");
   System.out.printf("************  ITU G.729  8 Kbit/S SPEECH CODER  **************\n");
   System.out.printf("\n");
   System.out.printf("----------------- Floating point C simulation ----------------\n");
   System.out.printf("\n");
   System.out.printf("------------ Version 1.01 (Release 2, November 2006) --------\n");
   System.out.printf("\n");


   /*-----------------------------------------------------------------------*
    * Open speech file and result file (output serial bit stream)           *
    *-----------------------------------------------------------------------*/

   if ( args.length != 2 )
     {
        System.out.printf("Usage : coder  speech_file  bitstream_file \n");
        System.out.printf("\n");
        System.out.printf("Format for speech_file:\n");
        System.out.printf("  Speech is read form a binary file of 16 bits data.\n");
        System.out.printf("\n");
        System.out.printf("Format for bitstream_file:\n");
        System.out.printf("  One word (2-bytes) to indicate erasure.\n");
        System.out.printf("  One word (2 bytes) to indicate bit rate\n");
        System.out.printf("  80 words (2-bytes) containing 80 bits.\n");
        System.out.printf("\n");
        System.exit( 1 );
     }

   try
   {
      f_speech = new FileInputStream(args[0]);
   }
   catch (IOException ex)
   {
      System.out.printf("Codder - Error opening file  %s !!\n", args[0]);
      System.exit(0);
      throw ex; // Silence the compiler.
   }
   System.out.printf(" Input speech file     :  %s\n", args[0]);

   try
   {
      f_serial = new FileOutputStream(args[1]);
   }
   catch (IOException ex)
   {
      System.out.printf("Coder - Error opening file  %s !!\n", args[1]);
      System.exit(0);
      throw ex; // Silence the compiler
   }
   System.out.printf(" Output bitstream file :  %s\n", args[1]);

  /*-------------------------------------------------*
   * Initialization of the coder.                    *
   *-------------------------------------------------*/

   Coder coder = new Coder();

   /*-------------------------------------------------------------------------*
    * Loop for every analysis/transmission frame.                             *
    * -New L_FRAME data are read. (L_FRAME = number of speech data per frame) *
    * -Conversion of the speech data from 16 bit integer to real              *
    * -Call cod_ld8k to encode the speech.                                    *
    * -The compressed serial output stream is written to a file.              *
    * -The synthesis speech is written to a file                              *
    *-------------------------------------------------------------------------*
    */

   frame=0;
   while(Util.fread(sp16, L_FRAME, f_speech) == L_FRAME){
      frame++;
      System.out.printf(" Frame: %d\r", frame);

      coder.process(sp16, serial);

      Util.fwrite(serial, SERIAL_SIZE,  f_serial);
   }

   f_serial.close();
   f_speech.close();
} /* end of main() */

    /**
     * Process <code>L_FRAME</code> short of speech.
     *
     * @param sp16      input : speach short array
     * @param serial    output : serial array encoded in bits_ld8k
     */
    void process(short[] sp16, short[] serial)
    {
        float[] new_speech = codLd8k.new_speech;           /* Pointer to new speech data   */
        int new_speech_offset = codLd8k.new_speech_offset;

        for (int i = 0; i < L_FRAME; i++)
            new_speech[new_speech_offset + i] = sp16[i];

        preProc.pre_process(new_speech, new_speech_offset, L_FRAME);

        codLd8k.coder_ld8k(prm);

        Bits.prm2bits_ld8k(prm, serial);
    }
}
