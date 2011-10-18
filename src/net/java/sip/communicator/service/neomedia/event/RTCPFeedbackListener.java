/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

/**
 * <tt>RTCPFeedbackListener</tt> is used by codec to be notified
 * by RTCP feedback event such as PLI (Picture Loss Indication) or
 * FIR (Full Intra-frame Request).
 *
 * @author Sebastien Vincent
 */
public interface RTCPFeedbackListener
{
    /**
     * Event fired when RTCP feedback message is received.
     *
     * @param event <tt>RTCPFeedbackEvent</tt>
     */
    public void feedbackReceived(RTCPFeedbackEvent event);
}

