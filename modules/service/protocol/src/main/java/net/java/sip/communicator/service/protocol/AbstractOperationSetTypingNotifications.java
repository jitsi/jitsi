/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetTypingNotifications</tt> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @param <T> the type of the <tt>ProtocolProviderService</tt> implementation
 * providing the <tt>AbstractOperationSetTypingNotifications</tt> implementation
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetTypingNotifications<T extends ProtocolProviderService>
    implements OperationSetTypingNotifications
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetTypingNotifications</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetTypingNotifications.class);

    /**
     * The provider that created us.
     */
    protected final T parentProvider;

    /**
     * The list of currently registered <tt>TypingNotificationsListener</tt>s.
     */
    private final List<TypingNotificationsListener> typingNotificationsListeners
        = new ArrayList<TypingNotificationsListener>();

    /**
     * Initializes a new <tt>AbstractOperationSetTypingNotifications</tt>
     * instance created by a specific <tt>ProtocolProviderService</tt> instance.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> which creates
     * the new instance
     */
    protected AbstractOperationSetTypingNotifications(T parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s.
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     * we'd like to add
     * @see OperationSetTypingNotifications#addTypingNotificationsListener(
     * TypingNotificationsListener)
     */
    public void addTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized (typingNotificationsListeners)
        {
            if (!typingNotificationsListeners.contains(listener))
                typingNotificationsListeners.add(listener);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws IllegalStateException if the underlying stack is not registered
     * and initialized
     */
    protected void assertConnected()
        throws IllegalStateException
    {
        if (parentProvider == null)
            throw
                new IllegalStateException(
                        "The provider must be non-null"
                            + " before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw
                new IllegalStateException(
                        "The provider must be signed on the service"
                            + " before being able to communicate.");
    }

    /**
     * Delivers a <tt>TypingNotificationEvent</tt> to all registered listeners.
     *
     * @param sourceContact the contact who has sent the notification
     * @param evtCode the code of the event to deliver
     */
    public void fireTypingNotificationsEvent(
        Contact sourceContact,
        int evtCode)
    {
        TypingNotificationsListener[] listeners;
        synchronized (typingNotificationsListeners)
        {
            listeners
                = typingNotificationsListeners
                    .toArray(
                        new TypingNotificationsListener[
                                typingNotificationsListeners.size()]);
        }

        if (logger.isDebugEnabled())
            logger.debug(
            "Dispatching a TypingNotificationEvent to "
                + listeners.length
                + " listeners. Contact "
                + sourceContact.getAddress()
                + " has now a typing status of "
                + evtCode);

        TypingNotificationEvent evt
            = new TypingNotificationEvent(sourceContact, evtCode);

        for (TypingNotificationsListener listener : listeners)
            listener.typingNotificationReceived(evt);
    }

    /**
     * Delivers a <tt>TypingNotificationEvent</tt> to all registered listeners
     * for delivery failed event.
     *
     * @param sourceContact the contact who has sent the notification
     * @param evtCode the code of the event to deliver
     */
    public void fireTypingNotificationsDeliveryFailedEvent(
        Contact sourceContact,
        int evtCode)
    {
        TypingNotificationsListener[] listeners;
        synchronized (typingNotificationsListeners)
        {
            listeners
                = typingNotificationsListeners
                    .toArray(
                        new TypingNotificationsListener[
                                typingNotificationsListeners.size()]);
        }

        if (logger.isDebugEnabled())
            logger.debug(
            "Dispatching a TypingNotificationEvent to "
                + listeners.length
                + " listeners for typingNotificationDeliveryFailed. Contact "
                + sourceContact.getAddress()
                + " has now a typing status of "
                + evtCode);

        TypingNotificationEvent evt
            = new TypingNotificationEvent(sourceContact, evtCode);

        for (TypingNotificationsListener listener : listeners)
            listener.typingNotificationDeliveryFailed(evt);
    }

    /**
     * Removes <tt>listener</tt> from the list of listeners registered for
     * receiving <tt>TypingNotificationEvent</tt>s.
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     * we'd like to remove
     * @see OperationSetTypingNotifications#removeTypingNotificationsListener(
     * TypingNotificationsListener)
     */
    public void removeTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized (typingNotificationsListeners)
        {
            typingNotificationsListeners.remove(listener);
        }
    }
}
