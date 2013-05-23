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
 * that occur in the registration state of a <tt>ProtocolProviderService</tt>.
 *
 * @author Emil Ivov
 */
public interface RegistrationStateChangeListener
    extends EventListener
{
    /**
     * The method is called by a <tt>ProtocolProviderService</tt> implementation
     * whenever a change in its registration state has occurred.
     *
     * @param evt a <tt>RegistrationStateChangeEvent</tt> which describes the
     * registration state change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt);
}
