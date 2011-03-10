/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap.event;

import java.util.*;

/**
 * Implementation of LdapEventManager.
 *
 * Class to be extended by any class which should send LdapEvent-s
 * and register LdapListenerS.
 *
 * @author Sebastien Mazy
 */

public class DefaultLdapEventManager
    implements LdapEventManager
{
    /**
     * All property change listeners registered so far.
     */
    protected Set<LdapListener> ldapListeners =
        Collections.synchronizedSet(new HashSet<LdapListener>());

    /**
     * Adds listener to our list of listeners
     *
     * @param listener  The LdapListener to be added
     *
     * @see net.java.sip.communicator.service.ldap.LdapDirectory#addLdapListener
     */
    public void addLdapListener(LdapListener listener)
    {
        this.ldapListeners.add(listener);
    }

    /**
     * Removes a LdapListener from the listener list.
     *
     * @param listener The LdapListener to be removed
     *
     * @see net.java.sip.communicator.service.ldap.
     * LdapDirectory#removeLdapListener
     */
    public void removeLdapListener(
            LdapListener listener)
    {
        this.ldapListeners.remove(listener);
    }

    /**
     * Fires an existing LdapEvent to any registered listeners.
     * @param event  The LdapEvent object.
     */
    public void fireLdapEvent(LdapEvent event)
    {
        for(LdapListener listener : this.ldapListeners)
        {
            this.fireLdapEvent(event, listener);
        }
    }

    /**
     * Fires an existing LdapEvent to a single listener.
     *
     * @param event  The LdapEvent object.
     * @param listener the listener to send the event to
     */
    public void fireLdapEvent(LdapEvent event, LdapListener listener)
    {
        listener.ldapEventReceived(event);
    }
}
