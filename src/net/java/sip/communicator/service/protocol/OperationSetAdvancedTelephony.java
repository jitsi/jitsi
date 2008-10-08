/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Extends <code>OperationSetBasicTelephony</code> with advanced telephony
 * operations such as call transfer.
 * 
 * @author Lubomir Marinov
 */
public interface OperationSetAdvancedTelephony
    extends OperationSetBasicTelephony
{

    /**
     * Transfers (in the sense of call transfer) a specific
     * <code>CallParticipant</code> to a specific callee address which already
     * participates in an active <code>Call</code>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param participant the <code>CallParticipant</code> to be transfered to
     *            the specified callee address
     * @param target the address in the form of <code>CallParticipant</code> of
     *            the callee to transfer <code>participant</code> to
     * @throws OperationFailedException
     */
    void transfer(CallParticipant participant, CallParticipant target)
        throws OperationFailedException;

    /**
     * Transfers (in the sense of call transfer) a specific
     * <code>CallParticipant</code> to a specific callee address which may or
     * may not already be participating in an active <code>Call</code>.
     * <p>
     * The method is suitable for providing the implementation of unattended
     * call transfer (though no such requirement is imposed).
     * </p>
     * 
     * @param participant the <code>CallParticipant</code> to be transfered to
     *            the specified callee address
     * @param target the address of the callee to transfer
     *            <code>participant</code> to
     * @throws OperationFailedException
     */
    void transfer(CallParticipant participant, String target)
        throws OperationFailedException;
}
