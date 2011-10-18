/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the state of a ProtocolProvider (e.g. PresenceStatusChanges)
 * @author Emil Ivov
 */
public interface ProviderChangeListener
{
    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the presence status of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void providerStatusChanged(ProviderStatusChangeEvent evt);
}
