/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Interacts with user for received transfer request for unknown calls.
 *
 * @author Damian Minkov
 */
public interface TransferAuthority
{
    /**
     * Checks with user for unknown transfer. Returns <tt>true</tt> if user
     * accepts and we must process the transfer, <tt>false</tt> otherwise.
     *
     * @param fromContact the contact initiating the transfer.
     * @param transferTo the address we will be transferred to.
     * @return <tt>true</tt> if transfer is allowed to process, <tt>false</tt>
     * otherwise.
     */
    public boolean processTransfer(Contact fromContact, String transferTo);

    /**
     * Checks with user for unknown transfer. Returns <tt>true</tt> if user
     * accepts and we must process the transfer, <tt>false</tt> otherwise.
     *
     * @param fromAddress the address initiating the transfer.
     * @param transferTo the address we will be transferred to.
     * @return <tt>true</tt> if transfer is allowed to process, <tt>false</tt>
     * otherwise.
     */
    public boolean processTransfer(String fromAddress, String transferTo);
}
