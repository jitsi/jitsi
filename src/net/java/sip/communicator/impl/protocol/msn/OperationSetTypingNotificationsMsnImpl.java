/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsMsnImpl
    implements OperationSetTypingNotifications
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsMsnImpl.class);

    /**
     * All currently registered TN listeners.
     */
    private List typingNotificationsListeners = new ArrayList();

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceMsnImpl opSetPersPresence = null;

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsMsnImpl(
        ProtocolProviderServiceMsnImpl provider)
    {
        this.msnProvider = provider;
    }

    /**
     * Adds <tt>l</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     *
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd
     *   like to add
     *   method
     */
    public void addTypingNotificationsListener(TypingNotificationsListener l)
    {
        synchronized(typingNotificationsListeners)
        {
            typingNotificationsListeners.add(l);
        }
    }

    /**
     * Removes <tt>l</tt> from the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     *
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd
     *   like to remove
     */
    public void removeTypingNotificationsListener(TypingNotificationsListener l)
    {
        synchronized(typingNotificationsListeners)
        {
            typingNotificationsListeners.remove(l);
        }
    }

    /**
     * Delivers a <tt>TypingNotificationEvent</tt> to all registered listeners.
     * @param sourceContact the contact who has sent the notification.
     * @param evtCode the code of the event to deliver.
     */
    private void fireTypingNotificationsEvent(Contact sourceContact
                                              ,int evtCode)
    {
        logger.debug("Dispatching a TypingNotif. event to "
            + typingNotificationsListeners.size()+" listeners. Contact "
            + sourceContact.getAddress() + " has now a typing status of "
            + evtCode);

        TypingNotificationEvent evt = new TypingNotificationEvent(
            sourceContact, evtCode);

        Iterator listeners = null;
        synchronized (typingNotificationsListeners)
        {
            listeners = new ArrayList(typingNotificationsListeners).iterator();
        }

        while (listeners.hasNext())
        {
            TypingNotificationsListener listener
                = (TypingNotificationsListener) listeners.next();

              listener.typingNotificationReceifed(evt);
        }
    }

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     *
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    public void sendTypingNotification(Contact notifiedContact, int typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (msnProvider == null)
            throw new IllegalStateException(
                "The msn provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!msnProvider.isRegistered())
            throw new IllegalStateException(
                "The msn provider must be signed on the service before "
                +"being able to communicate.");
    }
}
