/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>NotificationActionHandler</tt> is the parent interface of all specific
 * notification handlers used for handling different action types. This
 * interface is used in the NotificationService in all methods dealing with
 * action handlers.
 * 
 * @author Yana Stamcheva
 */
public interface NotificationHandler
{
    /**
     * Gets the type of this handler.
     * @return the type of this handler.
     */
    String getActionType();
}
