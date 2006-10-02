/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsJabberImpl
    implements OperationSetTypingNotifications
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsJabberImpl.class);

    /**
     * All currently registered TN listeners.
     */
    private List typingNotificationsListeners = new ArrayList();

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * We use this listener to ceise the moment when the protocol provider
     * has been successfully registered.
     */
    private ProviderRegListener providerRegListener = new ProviderRegListener();

    /**
     * The manger which send us the typing info and through which we send inf
     */
    private MessageEventManager messageEventManager = null;

    private Hashtable packetIDsTable = new Hashtable();

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;
        provider.addRegistrationStateChangeListener(providerRegListener);
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

        String packetID =
            (String)packetIDsTable.get(notifiedContact.getAddress());

        if(packetID != null)
        {
            if(typingState == STATE_TYPING)
                messageEventManager.
                    sendComposingNotification(notifiedContact.getAddress(),
                                              packetID);
            else if(typingState == STATE_STOPPED)
            {
                messageEventManager.
                    sendCancelledNotification(notifiedContact.getAddress(),
                                              packetID);
                packetIDsTable.remove(notifiedContact.getAddress());
            }
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The jabber provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!jabberProvider.isRegistered())
            throw new IllegalStateException(
                "The jabber provider must be signed on the service before "
                +"being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered and
     * ready to accept us as a listener.
     */
    private class ProviderRegListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence = (OperationSetPersistentPresenceJabberImpl)
                    jabberProvider.getSupportedOperationSets()
                        .get(OperationSetPersistentPresence.class.getName());

                messageEventManager =
                    new MessageEventManager(jabberProvider.getConnection());

                messageEventManager.addMessageEventRequestListener(
                    new JabberMessageEventRequestListener());
                messageEventManager.addMessageEventNotificationListener(
                    new IncomingMessageEventsListener());
            }
        }
    }

    /**
     * Listens for incoming request for typing info
     */
    private class JabberMessageEventRequestListener
        implements MessageEventRequestListener
    {
        public void deliveredNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            messageEventManager.sendDeliveredNotification(from, packetID);
        }

        public void displayedNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            messageEventManager.sendDisplayedNotification(from, packetID);
        }

        public void composingNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            if(packetID != null)
            {
                String fromID = StringUtils.parseBareAddress(from);
                packetIDsTable.put(fromID, packetID);
            }
        }

        public void offlineNotificationRequested(String from, String packetID,
                                                 MessageEventManager
                                                 messageEventManager)
        {}
    }

    /**
     * Receives incoming typing info
     */
    private class IncomingMessageEventsListener
        implements MessageEventNotificationListener
    {
        public void deliveredNotification(String from, String packetID)
        {}

        public void displayedNotification(String from, String packetID)
        {}

        public void composingNotification(String from, String packetID)
        {
            String fromID = StringUtils.parseBareAddress(from);

            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_TYPING);
        }

        public void offlineNotification(String from, String packetID)
        {}

        public void cancelledNotification(String from, String packetID)
        {
            String fromID = StringUtils.parseBareAddress(from);
            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_STOPPED);
        }
    }
}
