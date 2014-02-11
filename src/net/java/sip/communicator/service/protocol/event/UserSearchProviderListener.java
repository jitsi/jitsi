/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * An interface for listener that will be notified when provider that supports
 * user search is added or removed.
 * @author Hristo Terezov
 */
public interface UserSearchProviderListener
{
    /**
     * Notifies the listener with <tt>UserSearchProviderEvent</tt> event.
     * @param event the event
     */
    public void onUserSearchProviderEvent(UserSearchProviderEvent event);
}
