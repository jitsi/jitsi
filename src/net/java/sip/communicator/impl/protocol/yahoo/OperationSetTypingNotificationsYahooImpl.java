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
package net.java.sip.communicator.impl.protocol.yahoo;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import ymsg.network.event.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsYahooImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceYahooImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsYahooImpl.class);

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceYahooImpl opSetPersPresence = null;

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsYahooImpl(
        ProtocolProviderServiceYahooImpl provider)
    {
        super(provider);

        provider.addRegistrationStateChangeListener(new ProviderRegListener());
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

        if( !(notifiedContact instanceof ContactYahooImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not an yahoo contact."
               + notifiedContact);

       if(typingState == OperationSetTypingNotifications.STATE_TYPING)
       {

           parentProvider.getYahooSession().
               keyTyped(notifiedContact.getAddress(),
                       parentProvider.getAccountID().getUserID());
       }
       else
           if(typingState == OperationSetTypingNotifications.STATE_STOPPED ||
           typingState == OperationSetTypingNotifications.STATE_PAUSED)
           {
               parentProvider.getYahooSession().
                   stopTyping(notifiedContact.getAddress(),
                           parentProvider.getAccountID().getUserID());
           }
    }

    private class TypingListener
        extends SessionAdapter
    {
        @Override
        public void notifyReceived(SessionNotifyEvent evt)
        {
            if(evt.isTyping())
            {
                String typingUserID = evt.getFrom();

                if(typingUserID != null)
                {
                    Contact sourceContact =
                        opSetPersPresence.findContactByID(typingUserID);

                    if(sourceContact == null)
                        return;

                    // typing on
                    fireTypingNotificationsEvent(
                        sourceContact,
                        (evt.getMode() == 1) ? STATE_TYPING : STATE_STOPPED);
                }
            }
        }
    }

    /**
     * Our listener that will tell us when we're registered and
     * ready to accept us as a listener.
     */
    private class ProviderRegListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceYahooImpl) parentProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                parentProvider
                    .getYahooSession().addSessionListener(new TypingListener());
            }
        }
    }
}
