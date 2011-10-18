/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

/**
 * The purpose of a <tt>DTMFListener</tt> is to notify implementors when new
 * DMTF tones are received by this MediaService implementation.
 *
 * @author Emil Ivov
 */
public interface DTMFListener
{

    /**
     * Indicates that we have started receiving a <tt>DTMFTone</tt>.
     *
     * @param event the <tt>DTMFToneEvent</tt> instance containing the
     * <tt>DTMFTone</tt>
     */
    public void dtmfToneReceptionStarted(DTMFToneEvent event);

    /**
     * Indicates that reception of a DTMF tone has stopped.
     *
     * @param event the <tt>DTMFToneEvent</tt> instance containing the
     * <tt>DTMFTone</tt>
     */
    public void dtmfToneReceptionEnded(DTMFToneEvent event);
}
