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
     * Sets the secure state of the call session in which a specific participant
     * is involved
     *
     * @param participant the participant who toggled (or for whom is remotely
     *        toggled) the secure status change for the call
     * @param secure the new secure status
     * @param source the source who generated the call change
     */
    public void setSecure(CallParticipant participant, boolean secure,
                           SecureStatusChangeSource source);

    /**
     * Gets the secure state of the call session in which a specific participant
     * is involved
     *
     * @param participant the participant for who the call state is required
     * @return the call state
     */
    public boolean isSecure(CallParticipant participant);

    /**
     * Sets the SAS verifications state of the call session in which a specific participant
     * is involved
     *
     * @param participant the participant who toggled (or for whom is remotely
     *        toggled) the SAS verfied flag
     * @param verified the new SAS verification status
     * @param source the source who generated the call change
     */
    public boolean setSasVerified(CallParticipant participant, boolean verified);

    /**
     * Use this to indicate the source of setting the secure status
     * of the communication as being the local or remote peer or reverted by
     * local
     */
    public static enum SecureStatusChangeSource {
        SECURE_STATUS_CHANGE_BY_LOCAL,
        SECURE_STATUS_CHANGE_BY_REMOTE,
        SECURE_STATUS_REVERTED;
    }
}
