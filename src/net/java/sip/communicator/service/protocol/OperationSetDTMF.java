/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An <tt>OperationSet</tt> that allows other modules to send DMF tones through
 * this protocol provider.
 *
 * @author JM HEITZ
 */
public interface OperationSetDTMF
    extends OperationSet
{
    /**
     * Sends the <tt>DTMFTone</tt> <tt>tone</tt> to <tt>callPeer</tt>.
     *
     * @param callPeer the  call peer to send <tt>tone</tt> to.
     * @param tone the DTMF tone to send to <tt>callPeer</tt>.
     *
     * @throws OperationFailedException with code OPERATION_NOT_SUPPORTED if
     * DTMF tones are not supported for <tt>callPeer</tt>.
     *
     * @throws IllegalArgumentException in case the call peer does not
     * belong to the underlying implementation.
     */
    public void startSendingDTMF(CallPeer callPeer, DTMFTone tone)
        throws OperationFailedException;

    /**
     * Stop sending of the currently transmitting DTMF tone.
     *
     * @param callPeer the  call peer to stop send <tt>tone</tt> to.
     */
    public void stopSendingDTMF(CallPeer callPeer);
}
