/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the registration state of a ProtocolProvider.
 * @author Emil Ivov
 */
public interface RegistrationStateChangeListener extends java.util.EventListener
{
    /**
     * The method is called by a ProtocolProvider implementation whenver
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt);
}
