/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An Operation Set defining the encryption operations for telephony.
 *
 * @author Emanuel Onica
 * @author Romain Kuntz
 * @author Emil Ivov
 */
public interface OperationSetSecureTelephony
    extends OperationSet
{
    /**
     * Gets the secure state of the call session in which a specific peer
     * is involved
     *
     * @param peer the peer for who the call state is required
     * @return the call state
     */
    public boolean isSecure(CallPeer peer);

    /**
     * Sets the SAS verifications state of the call session in which a specific
     * peer is involved
     *
     * @param peer the peer who toggled (or for whom is remotely
     *        toggled) the SAS verified flag
     * @param verified the new SAS verification status
     */
    public boolean setSasVerified(CallPeer peer, boolean verified);
}
