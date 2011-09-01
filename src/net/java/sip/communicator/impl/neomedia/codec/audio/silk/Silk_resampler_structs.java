/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

import java.util.*;

/**
 * Classes for IIR/FIR resamplers.
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_resampler_structs 
{
    /**
     * Flag to enable support for input/output sampling rates above 48 kHz. Turn off for embedded devices.
     */
    static final int RESAMPLER_SUPPORT_ABOVE_48KHZ =                   1;

    static final int SKP_Silk_RESAMPLER_MAX_FIR_ORDER =                16;
    static final int SKP_Silk_RESAMPLER_MAX_IIR_ORDER =                6;
}

 class SKP_Silk_resampler_state_struct
 {
    int[]       sIIR = new int[ Silk_resampler_structs.SKP_Silk_RESAMPLER_MAX_IIR_ORDER ];        /* this must be the first element of this struct */
    int[]       sFIR = new int[ Silk_resampler_structs.SKP_Silk_RESAMPLER_MAX_FIR_ORDER ];
    int[]       sDown2 = new int[ 2 ];
    
    String resampler_function;
    ResamplerFP resamplerCB;
    void resampler_function( Object state, short[] out, int out_offset, short[] in, int in_offset, int len )
    {
        resamplerCB.resampler_function(state, out, out_offset, in, in_offset, len);
    }
    
    String up2_function;
    Up2FP up2CB;
    void up2_function(  int[] state, short[] out, int out_offset, short[] in, int in_offset, int len )
    {
        up2CB.up2_function(state, out, out_offset, in, in_offset, len);
        
    }
    
    int       batchSize;
    int       invRatio_Q16;
    int       FIR_Fracs;
    int       input2x;
    short[]   Coefs;
    
    int[]       sDownPre = new int[ 2 ];
    int[]       sUpPost = new int[ 2 ];
    
    String down_pre_function;
    DownPreFP  downPreCB;
    void down_pre_function ( int[] state, short[] out, int out_offset, short[] in, int in_offset, int len )
    {
        downPreCB.down_pre_function(state, out, out_offset, in, in_offset, len);
    }

    String up_post_function;
    UpPostFP  upPostCB;
    void up_post_function ( int[] state, short[] out, int out_offset, short[] in, int in_offset, int len )
    {
        upPostCB.up_post_function(state, out, out_offset, in, in_offset, len);
    }
    int       batchSizePrePost;
    int       ratio_Q16;
    int       nPreDownsamplers;
    int       nPostUpsamplers;
    int magic_number;
    
    /**
     * set all fields of the instance to zero.
     */
    public void memZero()
    {
//        {
//            if(this.Coefs != null)
//            {
//                Arrays.fill(this.Coefs, (short)0);
//            }
//        }
        this.Coefs = null;
        
        Arrays.fill(this.sDown2, 0);
        Arrays.fill(this.sDownPre, 0);
        Arrays.fill(this.sFIR, 0);
        Arrays.fill(this.sIIR, 0);
        Arrays.fill(this.sUpPost, 0);

        this.batchSize = 0;
        this.batchSizePrePost = 0;
        this.down_pre_function = null;
        this.downPreCB = null;
        this.FIR_Fracs = 0;
        this.input2x = 0;
        this.invRatio_Q16 = 0;
        this.magic_number = 0;
        this.nPostUpsamplers = 0;
        this.nPreDownsamplers = 0;
        this.ratio_Q16 = 0;
        this.resampler_function = null;
        this.resamplerCB = null;
        this.up2_function = null;
        this.up2CB = null;
        this.up_post_function = null;
        this.upPostCB = null;    
    }
}
 /*************************************************************************************/
 interface ResamplerFP
 {
     void resampler_function( Object state, short[] out, int out_offset, short[] in, int in_offset, int len );
 }
 interface Up2FP
 {
     void up2_function(  int[] state, short[] out, int out_offset, short[] in, int in_offset, int len );
 }
 interface DownPreFP
 {
     void down_pre_function ( int[] state, short[] out, int out_offset, short[] in, int in_offset, int len );
 }
 interface UpPostFP
 {
     void up_post_function ( int[] state, short[] out, int out_offset, short[] in, int in_offset, int len );
 }
 /*************************************************************************************/
 