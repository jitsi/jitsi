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
     * Gets the secure state of the call session in which a specific participant
     * is involved
     *
     * @param participant the participant for who the call state is required
     * @return the call state
     */
    public boolean isSecure(CallPeer participant);

    /**
     * Sets the SAS verifications state of the call session in which a specific participant
     * is involved
     *
     * @param participant the participant who toggled (or for whom is remotely
     *        toggled) the SAS verfied flag
     * @param verified the new SAS verification status
     * @param source the source who generated the call change
     */
    public boolean setSasVerified(CallPeer participant, boolean verified);
}
