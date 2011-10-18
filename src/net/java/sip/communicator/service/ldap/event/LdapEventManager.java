/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap.event;

/**
 * Class to be extended by any class which should send LdapEventS
 * and register LdapListenerS.
 *
 * @author Sebastien Mazy
 */

public interface LdapEventManager
{
    /**
     * Adds listener to our list of listeners
     *
     * @param listener  The LdapListener to be added
     */
    public void addLdapListener(
            LdapListener listener);

    /**
     * Removes a LdapListener from the listener list.
     *
     * @param listener The LdapListener to be removed
     */
    public void removeLdapListener(
            LdapListener listener);

    /**
     * Fires an existing LdapEvent to any registered listeners.
     *
     * @param event  The LdapEvent object.
     */
    public void fireLdapEvent(LdapEvent event);

    /**
     * Fires an existing LdapEvent to a single listener.
     *
     * @param event  The LdapEvent object.
     * @param listener the listener to send the event to
     */
    public void fireLdapEvent(LdapEvent event, LdapListener listener);
}
