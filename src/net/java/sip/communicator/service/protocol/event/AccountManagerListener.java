/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a listener receiving notifications from {@link AccountManager}.
 * 
 * @author Lubomir Marinov
 */
public interface AccountManagerListener
    extends EventListener
{

    /**
     * Notifies this listener about an event fired by a specific
     * <code>AccountManager</code>.
     * 
     * @param event the <code>AccountManagerEvent</code> describing the
     *            <code>AccountManager</code> firing the notification and the
     *            other details of the specific notification.
     */
    void handleAccountManagerEvent(AccountManagerEvent event);
}
