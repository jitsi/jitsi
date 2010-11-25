/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides notification for custom/generic events and possibility to generate
 * such events.
 *
 * @author Damian Minkov
 */
public interface OperationSetGenericNotifications
    extends OperationSet
{
    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param contact the contact to receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            Contact contact,
            String eventName,
            String eventValue);

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            String jid,
            String eventName,
            String eventValue);

    /**
     * Registers a <tt>GenericEventListener</tt> with this
     * operation set so that it gets notifications for new
     * event notifications.
     *
     * @param eventName register the listener for certain event name.
     * @param listener the <tt>GenericEventListener</tt>
     * to register.
     */
    public void addGenericEventListener(
            String eventName,
            GenericEventListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new event notifications.
     *
     * @param eventName unregister the listener for certain event name.
     * @param listener the <tt>GenericEventListener</tt>
     * to unregister.
     */
    public void removeGenericEventListener(
            String eventName,
            GenericEventListener listener);
}
