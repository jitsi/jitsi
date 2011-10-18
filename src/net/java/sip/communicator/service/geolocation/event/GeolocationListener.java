/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.geolocation.event;

import java.beans.*;

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the state of a ProtocolProvider.
 *
 * @author Guillaume Schreiner
 */
public interface GeolocationListener extends java.util.EventListener
{
    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the Geolocation of the corresponding provider had occurred.
     *
     * @param evt
     *            ProviderGeolocationPresenceChangeEvent the event describing
     *            the status change.
     */
    public void providerStatusChanged(LocalPositionChangeEvent evt);

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the status message of the corresponding provider has occurred
     * and has been confirmed by the server.
     *
     * @param evt
     *            a PropertyChangeEvent with a STATUS_MESSAGE property name,
     *            containing the old and new Geolocation.
     */
    public void providerStatusMessageChanged(PropertyChangeEvent evt);
}
