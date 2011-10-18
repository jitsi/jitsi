/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the registration state of a
 * <code>ProtocolProviderService</code>.
 * 
 * @author Emil Ivov
 */
public interface RegistrationStateChangeListener
    extends EventListener
{
    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     * 
     * @param evt
     *            the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt);
}
