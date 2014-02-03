/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The <tt>OperationSetCusaxUtils</tt> provides utility methods related to a
 * CUSAX implementation.
 *
 * @author Yana Stamcheva
 */
public interface OperationSetCusaxUtils
    extends OperationSet
{
    /**
     * Returns the linked CUSAX provider for this protocol provider.
     *
     * @return the linked CUSAX provider for this protocol provider or null
     * if such isn't specified
     */
    public ProtocolProviderService getLinkedCusaxProvider();
}
