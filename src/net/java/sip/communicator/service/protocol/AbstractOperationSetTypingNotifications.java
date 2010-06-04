/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * <code>OperationSetTypingNotifications</code> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 * 
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetTypingNotifications<T extends ProtocolProviderService>
    implements OperationSetTypingNotifications
{
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetTypingNotifications.class);

    /**
     * The provider that created us.
     */
    protected final T parentProvider;

    /**
     * The list of currently registered <code>TypingNotificationsListener</code>
     * s.
     */
    private final List<TypingNotificationsListener> typingNotificationsListeners
        = new ArrayList<TypingNotificationsListener>();

    /**
     * Initializes a new <code>AbstractOperationSetTypingNotifications</code>
     * instance created by a specific <code>ProtocolProviderService</code>
     * instance.
     * 
     * @param parentProvider
     *            the <code>ProtocolProviderService</code> which creates the new
     *            instance
     */
    protected AbstractOperationSetTypingNotifications(T parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    /*
     * Implements
     * OperationSetTypingNotifications#addTypingNotificationsListener(
     * TypingNotificationsListener).
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
     * @throws java.lang.IllegalStateException
     *             if the underlying stack is not registered and initialized.
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
     * @param sourceContact
     *            the contact who has sent the notification
     * @param evtCode
     *            the code of the event to deliver
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

    /*
     * Implements
     * OperationSetTypingNotifications#removeTypingNotificationsListener
     * (TypingNotificationsListener).
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
