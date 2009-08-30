/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.message.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsMsnImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceMsnImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsMsnImpl.class);

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceMsnImpl opSetPersPresence = null;

    private MsnMessenger messenger = null;

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsMsnImpl(
        ProtocolProviderServiceMsnImpl provider)
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

        if( !(notifiedContact instanceof ContactMsnImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not an MSN contact."
               + notifiedContact);

       if(typingState == OperationSetTypingNotifications.STATE_TYPING)
       {
           MsnControlMessage msg = new MsnControlMessage();
           msg.setTypingUser(messenger.getOwner().getEmail().getEmailAddress());

           Email targetContactEmail =
               ( (ContactMsnImpl) notifiedContact).getSourceContact().getEmail();

           MsnSwitchboard[] activSB = messenger.getActiveSwitchboards();

           MsnSwitchboard tempSB = null;
           for (int i = 0; i < activSB.length; i++)
           {
               tempSB = activSB[i];

               if (tempSB.containContact(targetContactEmail))
                   tempSB.sendMessage(msg, true);
           }
       }
    }

    /**
     * Sets the messenger instance impl of the lib which communicates with the
     * server
     * 
     * @param messenger
     *            MsnMessenger
     */
    void setMessenger(MsnMessenger messenger)
    {
        this.messenger = messenger;
        messenger.addMessageListener(new TypingListener());
    }

    private class TypingListener
        extends MsnAdapter
    {
        public void controlMessageReceived(MsnSwitchboard switchboard,
                                               MsnControlMessage message,
                                               MsnContact contact)
        {
            String typingUserID = message.getTypingUser();

            if(typingUserID != null)
            {
                Contact sourceContact = opSetPersPresence.findContactByID(typingUserID);

                if(sourceContact == null)
                    return;

                fireTypingNotificationsEvent(sourceContact, STATE_TYPING);
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
            logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceMsnImpl) parentProvider
                        .getOperationSet(OperationSetPersistentPresence.class);
            }
        }
    }

}
