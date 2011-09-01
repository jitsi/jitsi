/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Interface to collection of resamplers.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
/* Matrix of resampling methods used:
 *                                        Fs_out (kHz)
 *                        8      12     16     24     32     44.1   48
 *
 *               8        C      UF     U      UF     UF     UF     UF
 *              12        AF     C      UF     U      UF     UF     UF
 *              16        D      AF     C      UF     U      UF     UF
 * Fs_in (kHz)  24        AIF    D      AF     C      UF     UF     U
 *              32        UF     AF     D      AF     C      UF     UF
 *              44.1      AMI    AMI    AMI    AMI    AMI    C      UF
 *              48        DAF    DAF    AF     D      AF     UF     C
 *
 * default method: UF
 *
 * C   -> Copy (no resampling)
 * D   -> Allpass-based 2x downsampling
 * U   -> Allpass-based 2x upsampling
 * DAF -> Allpass-based 2x downsampling followed by AR2 filter followed by FIR interpolation
 * UF  -> Allpass-based 2x upsampling followed by FIR interpolation
 * AMI -> ARMA4 filter followed by FIR interpolation
 * AF  -> AR2 filter followed by FIR interpolation
 *
 * Input signals sampled above 48 kHz are first downsampled to at most 48 kHz.
 * Output signals sampled above 48 kHz are upsampled from at most 48 kHz.
 */
public class Silk_resampler 
{
    /**
     * Greatest common divisor.
     * @param a
     * @param b 
     */
    static int gcd(int a, int b)
    {
        int tmp;
        while( b > 0 ) 
        {
            tmp = a - b * (int)(a/b);
            a   = b;
            b   = tmp;
        }
        return a;
    }

    /**
     * Initialize/reset the resampler state for a given pair of input/output sampling rates. 
     * @param S resampler state.
     * @param Fs_Hz_in Input sampling rate (Hz).
     * @param Fs_Hz_out Output sampling rate (Hz).
     * @return
     */
    static int SKP_Silk_resampler_init( 
        SKP_Silk_resampler_state_struct    S,        /* I/O: Resampler state             */
        int                            Fs_Hz_in,    /* I:    Input sampling rate (Hz)    */
        int                            Fs_Hz_out    /* I:    Output sampling rate (Hz)    */
    )
    {
        int cycleLen, cyclesPerBatch, up2 = 0, down2 = 0;

        /* Clear state */
//TODO:how to set all fields of S to 0?
        //S = new SKP_Silk_resampler_state_struct();
        S.memZero();

        /* Input checking */
        if(Silk_resampler_structs.RESAMPLER_SUPPORT_ABOVE_48KHZ != 0)
        {
            if( Fs_Hz_in < 8000 || Fs_Hz_in > 192000 || Fs_Hz_out < 8000 || Fs_Hz_out > 192000 ) 
            {
                assert( false );
                return -1;
            }
        }
        else
        {
            if( Fs_Hz_in < 8000 || Fs_Hz_in >  48000 || Fs_Hz_out < 8000 || Fs_Hz_out >  48000 ) 
            {
                assert( false );
                return -1;
            }
        }
        
        if(Silk_resampler_structs.RESAMPLER_SUPPORT_ABOVE_48KHZ != 0)
        {
            /* Determine pre downsampling and post upsampling */
            if( Fs_Hz_in > 96000 ) 
            {
                S.nPreDownsamplers = 2;
                S.down_pre_function = "SKP_Silk_resampler_private_down4";
                S.downPreCB = new DownPreImplDown4();
            } 
            else if( Fs_Hz_in > 48000 ) 
            {
                S.nPreDownsamplers = 1;
                S.down_pre_function = "SKP_Silk_resampler_down2";
                S.downPreCB = new DownPreImplDown2();
            } 
            else 
            {
                S.nPreDownsamplers = 0;
                S.down_pre_function = null;
                S.downPreCB = null;
            }
    
            if( Fs_Hz_out > 96000 ) 
            {
                S.nPostUpsamplers = 2;
                S.up_post_function = "SKP_Silk_resampler_private_up4";
                S.upPostCB = new UpPostImplUp4();
            } 
            else if( Fs_Hz_out > 48000 )
            {
                S.nPostUpsamplers = 1;
                S.up_post_function = "SKP_Silk_resampler_up2";
                S.upPostCB = new UpPostImplUp2();
            } 
            else 
            {
                S.nPostUpsamplers = 0;
                S.up_post_function = null;
                S.upPostCB = null;
            }
    
            if( S.nPreDownsamplers + S.nPostUpsamplers > 0 ) 
            {
                /* Ratio of output/input samples */
                S.ratio_Q16 = ( ( Fs_Hz_out<<13 ) / Fs_Hz_in )<<3;
                /* Make sure the ratio is rounded up */
                while( Silk_macros.SKP_SMULWW( S.ratio_Q16, Fs_Hz_in ) < Fs_Hz_out ) 
                    S.ratio_Q16++;
    
                /* Batch size is 10 ms */
                S.batchSizePrePost = Fs_Hz_in/100 ;
    
                /* Convert sampling rate to those after pre-downsampling and before post-upsampling */
                Fs_Hz_in  = Fs_Hz_in >>  S.nPreDownsamplers ;
                Fs_Hz_out = Fs_Hz_out >> S.nPostUpsamplers  ;
            }
        }

        /* Number of samples processed per batch */
        /* First, try 10 ms frames */
        S.batchSize = Fs_Hz_in/100;
        if( ( S.batchSize*100 != Fs_Hz_in ) || ( Fs_Hz_in % 100 != 0 ) ) 
        {
            /* No integer number of input or output samples with 10 ms frames, use greatest common divisor */
            cycleLen = Fs_Hz_in / gcd( Fs_Hz_in, Fs_Hz_out );
            cyclesPerBatch = Silk_resampler_private.RESAMPLER_MAX_BATCH_SIZE_IN / cycleLen;
            if( cyclesPerBatch == 0 ) 
            {
                /* cycleLen too big, let's just use the maximum batch size. Some distortion will result. */
                S.batchSize = Silk_resampler_private.RESAMPLER_MAX_BATCH_SIZE_IN;
                assert( false );
            } 
            else 
            {
                S.batchSize = cyclesPerBatch * cycleLen;
            }
        }

        /* Find resampler with the right sampling ratio */
        if( Fs_Hz_out > Fs_Hz_in ) 
        {
            /* Upsample */
            if( Fs_Hz_out == Fs_Hz_in * 2 )
            {                             /* Fs_out : Fs_in = 2 : 1 */
                /* Special case: directly use 2x upsampler */
                S.resampler_function = "SKP_Silk_resampler_private_up2_HQ_wrapper";
                S.resamplerCB = new ResamplerImplWrapper();
            } 
            else
            {
                /* Default resampler */
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
                up2 = 1;
                if( Fs_Hz_in > 24000 ) 
                {
                    /* Low-quality all-pass upsampler */
                    S.up2_function = "SKP_Silk_resampler_up2";
                    S.up2CB = new Up2ImplUp2();
                   
                } 
                else
                {
                    /* High-quality all-pass upsampler */
                    S.up2_function = "SKP_Silk_resampler_private_up2_HQ";
                    S.up2CB = new Up2ImplHQ();
                    
                }
            }
        } 
        else if ( Fs_Hz_out < Fs_Hz_in ) 
        {
            /* Downsample */
            if( Fs_Hz_out * 4 == Fs_Hz_in * 3 )
            {               /* Fs_out : Fs_in = 3 : 4 */
                S.FIR_Fracs = 3;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_3_4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 3 == Fs_Hz_in * 2 )
            {        /* Fs_out : Fs_in = 2 : 3 */
                S.FIR_Fracs = 2;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_2_3_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 2 == Fs_Hz_in )
            {                      /* Fs_out : Fs_in = 1 : 2 */
                S.FIR_Fracs = 1;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_1_2_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 8 == Fs_Hz_in * 3 ) 
            {        /* Fs_out : Fs_in = 3 : 8 */
                S.FIR_Fracs = 3;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_3_8_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 3 == Fs_Hz_in ) 
            {                      /* Fs_out : Fs_in = 1 : 3 */
                S.FIR_Fracs = 1;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_1_3_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 4 == Fs_Hz_in )
            {                      /* Fs_out : Fs_in = 1 : 4 */
                S.FIR_Fracs = 1;
                down2 = 1;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_1_2_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 6 == Fs_Hz_in )
            {                      /* Fs_out : Fs_in = 1 : 6 */
                S.FIR_Fracs = 1;
                down2 = 1;
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_1_3_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_down_FIR";
                S.resamplerCB = new ResamplerImplDownFIR();
            } 
            else if( Fs_Hz_out * 441 == Fs_Hz_in * 80 )
            {     /* Fs_out : Fs_in = 80 : 441 */
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_80_441_ARMA4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
            } 
            else if( Fs_Hz_out * 441 == Fs_Hz_in * 120 ) 
            {    /* Fs_out : Fs_in = 120 : 441 */
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_120_441_ARMA4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
            } 
            else if( Fs_Hz_out * 441 == Fs_Hz_in * 160 )
            {    /* Fs_out : Fs_in = 160 : 441 */
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_160_441_ARMA4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
            } 
            else if( Fs_Hz_out * 441 == Fs_Hz_in * 240 ) 
            {    /* Fs_out : Fs_in = 240 : 441 */
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_240_441_ARMA4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
            } 
            else if( Fs_Hz_out * 441 == Fs_Hz_in * 320 )
            {    /* Fs_out : Fs_in = 320 : 441 */
                S.Coefs = Silk_resampler_rom.SKP_Silk_Resampler_320_441_ARMA4_COEFS;
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
            } 
            else
            {
                /* Default resampler */
                S.resampler_function = "SKP_Silk_resampler_private_IIR_FIR";
                S.resamplerCB = new ResamplerImplIIRFIR();
                up2 = 1;
                if( Fs_Hz_in > 24000 ) 
                {
                    /* Low-quality all-pass upsampler */
                    S.up2_function = "SKP_Silk_resampler_up2";
                    S.up2CB = new Up2ImplUp2();
                }
                else 
                {
                    /* High-quality all-pass upsampler */
                    S.up2_function = "SKP_Silk_resampler_private_up2_HQ";
                    S.up2CB = new Up2ImplHQ();
                }
            }
        } 
        else
        {
            /* Input and output sampling rates are equal: copy */
            S.resampler_function = "SKP_Silk_resampler_private_copy";
            S.resamplerCB = new ResamplerImplCopy();
        }

        S.input2x = up2 | down2;

        /* Ratio of input/output samples */
        S.invRatio_Q16 = ( ( Fs_Hz_in << 14 + up2 - down2 ) / Fs_Hz_out ) << 2 ;
        /* Make sure the ratio is rounded up */
        while( Silk_macros.SKP_SMULWW( S.invRatio_Q16, Fs_Hz_out << down2 ) < ( Fs_Hz_in << up2 ) ) 
        {
            S.invRatio_Q16++;
        }

        S.magic_number = 123456789;

        return 0;
    }

    /**
     * Clear the states of all resampling filters, without resetting sampling rate ratio.
     * @param S Resampler state.
     * @return
     */
    static int SKP_Silk_resampler_clear( 
        SKP_Silk_resampler_state_struct    S            /* I/O: Resampler state             */
    )
    {
        Arrays.fill(S.sDown2, 0);
        Arrays.fill(S.sIIR, 0);
        Arrays.fill(S.sFIR, 0);
        if (Silk_resampler_structs.RESAMPLER_SUPPORT_ABOVE_48KHZ != 0)
        {
            Arrays.fill(S.sDownPre, 0);
            Arrays.fill(S.sUpPost, 0);
        }
        return 0;
    }

    /**
     * Resampler: convert from one sampling rate to another.
     * @param S Resampler state.
     * @param out Output signal.
     * @param out_offset offset of vaild data.
     * @param in Input signal.
     * @param in_offset offset of valid data.
     * @param inLen Number of input samples
     * @return
     */
    static int SKP_Silk_resampler( 
        SKP_Silk_resampler_state_struct    S,            /* I/O: Resampler state             */
        short[]                                out,    /* O:    Output signal                 */
        int out_offset,
        short[]                                in,        /* I:    Input signal                */
        int in_offset,
        int                                    inLen    /* I:    Number of input samples        */
    )
    {
        /* Verify that state was initialized and has not been corrupted */
        if( S.magic_number != 123456789 ) 
        {
            assert( false );
            return -1;
        }
        
        if (Silk_resampler_structs.RESAMPLER_SUPPORT_ABOVE_48KHZ != 0)
        {
            if( S.nPreDownsamplers + S.nPostUpsamplers > 0 ) {
                /* The input and/or output sampling rate is above 48000 Hz */
                int       nSamplesIn, nSamplesOut;
                short[]        in_buf = new short[ 480 ];
                short[]     out_buf = new short[ 480 ];

                while( inLen > 0 ) {
                    /* Number of input and output samples to process */
                    nSamplesIn = Silk_SigProc_FIX.SKP_min( inLen, S.batchSizePrePost );
                    nSamplesOut = Silk_macros.SKP_SMULWB( S.ratio_Q16, nSamplesIn );

                    Silk_typedef.SKP_assert( ( nSamplesIn  >>  S.nPreDownsamplers ) <= 480 );
                    Silk_typedef.SKP_assert( ( nSamplesOut >> S.nPostUpsamplers  ) <= 480 );

                    if( S.nPreDownsamplers > 0 ) {
                        S.down_pre_function(S.sDownPre, in_buf, 0, in, in_offset, nSamplesIn);
                        
                        if( S.nPostUpsamplers > 0 ) {
                            S.resampler_function(S, out_buf, 0, in_buf, 0, ( nSamplesIn >> S.nPreDownsamplers ));
                            S.up_post_function(S.sUpPost, out, out_offset, out_buf, 0, ( nSamplesOut >> S.nPostUpsamplers ));
                        } else {
                            S.resampler_function( S, out, out_offset, in_buf, 0, ( nSamplesIn >> S.nPreDownsamplers ));
                        }
                    } else {
                        S.resampler_function( S, out_buf, 0, in, in_offset, ( nSamplesIn >> S.nPreDownsamplers ));
                        S.up_post_function( S.sUpPost, out, out_offset, out_buf, 0, ( nSamplesOut >> S.nPostUpsamplers ));
                    }

                    in_offset += nSamplesIn;
                       out_offset += nSamplesOut;
                    inLen -= nSamplesIn;
                }
            } else {
                /* Input and output sampling rate are at most 48000 Hz */
                S.resampler_function( S, out, out_offset, in, in_offset, inLen);
            }
        }
        else{
            /* Input and output sampling rate are at most 48000 Hz */
            S.resampler_function( S, out, out_offset, in, in_offset, inLen);
        }
        return 0;
    }
}
/*************************************************************************************/
class DownPreImplDown4 implements DownPreFP
{
    public void down_pre_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_down4.SKP_Silk_resampler_private_down4(state, 0, 
                out, outOffset, in, inOffset, len);
    }
}
class DownPreImplDown2 implements DownPreFP
{
    public void down_pre_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_down2.SKP_Silk_resampler_down2(state, 0, 
                out, outOffset, in, inOffset, len);
    }
}
/*----------------------------------------------------------------*/
class UpPostImplUp4 implements UpPostFP
{
    public void up_post_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_up4.SKP_Silk_resampler_private_up4(state, 0, 
                out, outOffset, in, inOffset, len);
    }
    
}
class UpPostImplUp2 implements UpPostFP
{
    public void up_post_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_up2.SKP_Silk_resampler_up2(state, 0, 
                out, outOffset, in, inOffset, len);
    }    
}
/*----------------------------------------------------------------*/
class ResamplerImplWrapper implements ResamplerFP
{
    public void resampler_function(Object state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_up2_HQ.SKP_Silk_resampler_private_up2_HQ_wrapper(state, 
                out, outOffset, in, inOffset, len);
    }    
}
class ResamplerImplIIRFIR implements ResamplerFP
{
    public void resampler_function(Object state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_IIR_FIR.SKP_Silk_resampler_private_IIR_FIR(state, 
                out, outOffset, in, inOffset, len);
    }
}
class ResamplerImplDownFIR implements ResamplerFP
{
    public void resampler_function(Object state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_down_FIR.SKP_Silk_resampler_private_down_FIR(state, 
                out, outOffset, in, inOffset, len);
    }    
}
class ResamplerImplCopy implements ResamplerFP
{
    public void resampler_function(Object state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_copy.SKP_Silk_resampler_private_copy(state, out, outOffset, in, inOffset, len);
    }
    
}
/*----------------------------------------------------------------*/
class Up2ImplUp2 implements Up2FP
{
    public void up2_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_up2.SKP_Silk_resampler_up2(state, 0, 
                out, outOffset, in, inOffset, len);
    }
}
class Up2ImplHQ implements Up2FP
{
    public void up2_function(int[] state, short[] out, int outOffset,
            short[] in, int inOffset, int len) {
        Silk_resampler_private_up2_HQ.SKP_Silk_resampler_private_up2_HQ(state, 0, 
                out, outOffset, in, inOffset, len);
    }
    
}
/*************************************************************************************/
