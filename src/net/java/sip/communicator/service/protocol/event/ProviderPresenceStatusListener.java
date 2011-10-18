/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

import java.beans.*;

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the state of a ProtocolProvider (e.g. PresenceStatusChanges)
 * @author Emil Ivov
 */
public interface ProviderPresenceStatusListener extends java.util.EventListener
{
    /**
     * The property name of PropertyChangeEvents announcing changes in our
     * status message.
     */
    public static final String STATUS_MESSAGE = "StatusMessage";

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the presence status of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt);

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the status message of the corresponding provider has occurred
     * and has been confirmed by the server.
     *
     * @param evt a PropertyChangeEvent with a STATUS_MESSAGE property name,
     * containing the old and new status messages.
     */
    public void providerStatusMessageChanged(PropertyChangeEvent evt);
}
