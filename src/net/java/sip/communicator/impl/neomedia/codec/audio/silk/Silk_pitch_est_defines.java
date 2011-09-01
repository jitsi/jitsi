/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Definitions For Fix pitch estimator
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_pitch_est_defines 
{
    static final int PITCH_EST_SHORTLAG_BIAS_Q15 =        6554;    /* 0.2f. for logarithmic weighting    */
    static final int PITCH_EST_PREVLAG_BIAS_Q15 =         6554;    /* Prev lag bias    */
    static final int PITCH_EST_FLATCONTOUR_BIAS_Q20 =     52429;   /* 0.05f */
}
