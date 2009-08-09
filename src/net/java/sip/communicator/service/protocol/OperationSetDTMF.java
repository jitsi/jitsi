/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An operation set that allows other modules to send DMF tones through this
 * protocol provider.
 *
 * @author JM HEITZ
 *
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
     * @throws NullPointerException if one of the arguments is null.
     *
     * @throws IllegalArgumentException in case the call peer does not
     * belong to the underlying implementation.
     */
    public void sendDTMF(CallPeer callPeer, DTMFTone tone)
        throws OperationFailedException,
               NullPointerException,
               ClassCastException;
}
