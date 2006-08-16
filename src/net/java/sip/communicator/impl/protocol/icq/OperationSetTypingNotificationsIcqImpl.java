/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * joust sim.
 *
 * @author Emil Ivov
 */
public class OperationSetTypingNotificationsIcqImpl
    implements OperationSetTypingNotifications
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsIcqImpl.class);

    /**
     * All currently registered TN listeners.
     */
    private List typingNotificationsListeners = new ArrayList();

    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceIcqImpl opSetPersPresence = null;

    /**
     * The joust sim listener that we use for catching chat events.
     */
    private JoustSimIcbmListener joustSimIcbmListener
        = new JoustSimIcbmListener();

    /**
     * That's the listener that would gather the typing notifications
     * themselves.
     */
    private JoustSimTypingListener joustSimTypingListener
        = new JoustSimTypingListener();

    /**
     * We use this listener to ceise the moment when the protocol provider
     * has been successfully registered.
     */
    private ProviderRegListener providerRegListener = new ProviderRegListener();

    /**
     * @param icqProvider a ref to the <tt>ProtocolProviderServiceIcqImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;
        icqProvider.addRegistrationStateChangeListener(providerRegListener);
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

        synchronized(typingNotificationsListeners)
        {
            for (int i = 0; i < typingNotificationsListeners.size(); i++)
            {
              ((TypingNotificationsListener)typingNotificationsListeners.get(i))
                  .typingNotificationReceifed(evt);
            }
        }
    }

    /**
     * Converts the <tt>typingState</tt> variable to its corresponding joustsim
     * TypingState instance
     * @param typingState one of the <tt>STATE_XXX</tt> int fields of this
     * operation set.
     * @return the <tt>TypingState</tt> corresponding to the
     * <tt>typingState</tt> argument.
     */
    private TypingState intToTypingState(int typingState)
    {
        switch (typingState)
        {
            case STATE_PAUSED:   return TypingState.PAUSED;
            case STATE_TYPING:   return TypingState.TYPING;
            case STATE_STOPPED:  return TypingState.NO_TEXT;
        }
        //if unknown return STOPPED
        return TypingState.NO_TEXT;
    }

    /**
     * Returns the int var (one of the STATE_XXX fields in
     * OperationSetTypingNotifications) that best corresponds to <tt>state</tt>
     * @param state the <tt>TypingState</tt> that we'd like to translate.
     * @return one of the <tt>STATE_XXX</tt> int fields that best corresponds
     * to <tt>state</tt>
     */
    private int typingStateToInt(TypingState state)
    {
        if (state == TypingState.TYPING)
            return STATE_TYPING;
        else if(state == TypingState.PAUSED)
            return STATE_PAUSED;
        else if(state == TypingState.NO_TEXT)
            return STATE_STOPPED;

        return STATE_UNKNOWN;
    }

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     *
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    public void sendTypingNotification(Contact notifiedContact, int typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        icqProvider.getAimConnection().getIcbmService().getImConversation(
            new Screenname(notifiedContact.getAddress())).setTypingState(
                intToTypingState(typingState));
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered to icq and joust
     * sim is ready to accept us as a listener.
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
            logger.debug("The ICQ provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                icqProvider.getAimConnection().getIcbmService()
                    .addIcbmListener(joustSimIcbmListener);

                opSetPersPresence = (OperationSetPersistentPresenceIcqImpl)
                    icqProvider.getSupportedOperationSets()
                        .get(OperationSetPersistentPresence.class.getName());

            }
        }
    }

    /**
     * We track newly created conversations and register a typing listener with
     * every one of them so that we could fire events for typing events.
     */
    private class JoustSimIcbmListener implements IcbmListener
    {
        /**
         * All we do here is add a listener that would snoop for typing events
         * sent by oscar.jar
         * @param service the IcbmService that is sending the event
         * @param conv the Conversation where we're to add ourselves as a
         * listener.
         */
        public void newConversation(IcbmService service, Conversation conv)
        {
            conv.addConversationListener(joustSimTypingListener);
        }

        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info)
        {
            logger.debug("buddyInfoUpdated for:"+buddy+" info: " +info);
        }

    }

    /**
     * The oscar.jar lib sends us typing events through this listener.
     */
    private class JoustSimTypingListener implements IcbmListener, TypingListener
    {
        public void gotTypingState(Conversation conversation,
                                   TypingInfo typingInfo)
        {
            Contact sourceContact = opSetPersPresence.findContactByID(
                conversation.getBuddy().getFormatted());

            if(sourceContact == null)
            {
                logger.debug("Received a typing notification from an unknown "+
                             "buddy=" + conversation.getBuddy());
                //create the volatile contact
                sourceContact = opSetPersPresence
                    .createVolatileContact(
                        conversation.getBuddy().getFormatted());
            }

            fireTypingNotificationsEvent(
                sourceContact, typingStateToInt(typingInfo.getTypingState()));
        }

        //the follwoing methods only have dummy implementations here as they
        //do not interest us. complete implementatios are provider in the
        //basic instant messaging operation set.
        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info){}
        public void conversationClosed(Conversation c){}
        public void gotMessage(Conversation c, MessageInfo minfo){}
        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event){}
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event){}
        public void canSendMessageChanged(Conversation c, boolean canSend){}
        public void conversationOpened(Conversation c){}
        public void newConversation(IcbmService service, Conversation conv){}
        public void sentMessage(Conversation c, MessageInfo minfo){}
    }
}
