/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.text.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.message.*;
import net.java.sip.communicator.impl.protocol.msn.mail.utils.*;

/**
 * A straightforward implementation of the basic instant messaging operation
 * set.
 *
 * @author Damian Minkov
 */
public class OperationSetBasicInstantMessagingMsnImpl
    extends AbstractOperationSetBasicInstantMessaging
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingMsnImpl.class);

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceMsnImpl opSetPersPresence = null;

    private OperationSetAdHocMultiUserChatMsnImpl opSetMuc = null;
    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetBasicInstantMessagingMsnImpl(
        ProtocolProviderServiceMsnImpl provider)
    {
        this.msnProvider = provider;
        opSetMuc = (OperationSetAdHocMultiUserChatMsnImpl) msnProvider
       .getOperationSet(OperationSetAdHocMultiUserChat.class);
        provider.addRegistrationStateChangeListener(new RegistrationStateListener());
    }

    /**
     * Determines whether the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }
    
    /**
     * Determines whether the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
           return false;
    }

    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageMsnImpl(content, contentType, encoding, subject);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        if( !(to instanceof ContactMsnImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not an MSN contact."
               + to);

        MessageDeliveredEvent msgDeliveryPendingEvt
        = new MessageDeliveredEvent(
                message, to, System.currentTimeMillis());

        msgDeliveryPendingEvt = messageDeliveryPendingTransform(msgDeliveryPendingEvt);
        
        if (msgDeliveryPendingEvt == null)
            return;
        
        msnProvider.getMessenger().
            sendText(
                ((ContactMsnImpl)to).getSourceContact().getEmail(),
                msgDeliveryPendingEvt.getSourceMessage().getContent()
            );
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(message, to, System.currentTimeMillis());


        // msgDeliveredEvt = messageDeliveredTransform(msgDeliveredEvt);
        
        if (msgDeliveredEvt != null)
            fireMessageEvent(msgDeliveredEvt);
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
                "The provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!msnProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
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
                    (OperationSetPersistentPresenceMsnImpl) msnProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                msnProvider.getMessenger().
                    addMessageListener(new MsnMessageListener());
                msnProvider.getMessenger().
                    addEmailListener(new MsnMessageListener());
            }
        }
    }

    private class MsnMessageListener
        extends MsnMessageAdapter
        implements MsnEmailListener
    {
        public void instantMessageReceived(MsnSwitchboard switchboard,
                                           MsnInstantMessage message,
                                           MsnContact contact)
        {
           // FILTER OUT THE GROUP MESSAGES
           if (opSetMuc.isGroupChatMessage(switchboard))
               return;
           
           Message newMessage = createMessage(message.getContent());
            Contact sourceContact = opSetPersPresence.
                findContactByID(contact.getEmail().getEmailAddress());

            if(sourceContact == null)
            {
                logger.debug("received a message from an unknown contact: "
                                   + contact);
                //create the volatile contact
                sourceContact = opSetPersPresence.
                    createVolatileContact(contact);
            }

            MessageReceivedEvent msgReceivedEvt
                = new MessageReceivedEvent(
                    newMessage, sourceContact , System.currentTimeMillis() );
    
            // msgReceivedEvt = messageReceivedTransform(msgReceivedEvt);
            
            if (msgReceivedEvt != null)
                fireMessageEvent(msgReceivedEvt);
        }
        
        /**
         * Received offline text message.
         * 
         * @param body of message
         * @param contentType of message
         * @param encoding of message
         * @param displayName
         * @param from the user who sent this message
         */
        public void offlineMessageReceived(String body,
                                           String contentType, 
                                           String encoding,
                                           MsnContact contact)
        {
            Message newMessage =
                createMessage(body, contentType, encoding, null);

            Contact sourceContact = opSetPersPresence.
                findContactByID(contact.getEmail().getEmailAddress());

            if(sourceContact == null)
            {
                logger.debug("received a message from an unknown contact: "
                                   + contact);
                //create the volatile contact
                sourceContact = opSetPersPresence.
                    createVolatileContact(contact);
            }

            MessageReceivedEvent msgReceivedEvt
                = new MessageReceivedEvent(
                    newMessage, sourceContact , System.currentTimeMillis() );

            fireMessageEvent(msgReceivedEvt);
        }

        public void initialEmailNotificationReceived(MsnSwitchboard switchboard,
                                                     MsnEmailInitMessage message, 
                                                     MsnContact contact)
        {
        }

        public void initialEmailDataReceived(MsnSwitchboard switchboard,
                                             MsnEmailInitEmailData message,
                                             MsnContact contact)
        {
        }

        public void newEmailNotificationReceived(MsnSwitchboard switchboard,
                                                 MsnEmailNotifyMessage message,
                                                 MsnContact contact)
        {
            // we don't process incoming event without email.
            if ((message.getFromAddr() == null)
                || (message.getFromAddr().indexOf('@') < 0))
            {
                return;
            }
            
            String subject = message.getSubject();

            try
            {
                subject = MimeUtility.decodeText(subject);
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }

            Message newMailMessage = new MessageMsnImpl(
                    MessageFormat.format(
                        MsnActivator.getResources()
                            .getI18NString("service.gui.NEW_MAIL"), 
                        new Object[]{message.getFrom(), 
                                     message.getFromAddr(), 
                                     subject}),
                     DEFAULT_MIME_TYPE,
                     DEFAULT_MIME_ENCODING,
                     subject);

             Contact sourceContact = opSetPersPresence.
                 findContactByID(message.getFromAddr());

             if (sourceContact == null)
             {
                 logger.debug("received a new mail from an unknown contact: "
                                    + message.getFrom()
                                    + " &lt;" + message.getFromAddr() + "&gt;");
                 //create the volatile contact
                 sourceContact = opSetPersPresence
                     .createVolatileContact(contact);
             }
             MessageReceivedEvent msgReceivedEvt
                 = new MessageReceivedEvent(
                     newMailMessage, sourceContact, System.currentTimeMillis(),
                     MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);

             fireMessageEvent(msgReceivedEvt);
        }

        public void activityEmailNotificationReceived(MsnSwitchboard switchboard,
                                                      MsnEmailActivityMessage message,
                                                      MsnContact contact)
        {
        }
    }
}
