package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

public class MockBasicInstantMessaging
    implements OperationSetBasicInstantMessaging
{
    private Vector messageListeners = new Vector();
    private MockPersistentPresenceOperationSet opSetPersPresence = null;

    public MockBasicInstantMessaging(MockProvider provider,
                                     MockPersistentPresenceOperationSet opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
    }

    /**
     * Registeres a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging
     *   method
     */
    public void addMessageListener(MessageListener listener)
    {
        messageListeners.add(listener);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging
     *   method
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageImpl(new String(content), contentType
                                  , contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging
     *   method
     */
    public Message createMessage(String messageText)
    {
        return new MessageImpl(messageText, DEFAULT_MIME_TYPE,
                               DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Unregisteres <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception
     * of incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging
     *   method
     */
    public void removeMessageListener(MessageListener listener)
    {
        messageListeners.remove(listener);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging
     *   method
     */
    public void sendInstantMessage(Contact to, Message message) throws
        IllegalStateException, IllegalArgumentException
    {
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(
                message, to, new Date());

        Iterator iter = messageListeners.iterator();
        while (iter.hasNext())
        {
            MessageListener l = (MessageListener)iter.next();
            l.messageDelivered(msgDeliveredEvt);
        }
    }

    /**
     * Methods for manipulating mock operation set as
     * deliver(receive) messageop
     */
    public void deliverMessage(String to, Message msg)
    {
        Contact sourceContact = opSetPersPresence.findContactByID(to);

        MessageReceivedEvent msgReceivedEvt
                = new MessageReceivedEvent(
                    msg, sourceContact , new Date());
        Iterator iter = messageListeners.iterator();
        while (iter.hasNext())
        {
            MessageListener l = (MessageListener)iter.next();
            l.messageReceived(msgReceivedEvt);
        }
    }

    public class MessageImpl
        implements Message
    {
        private String textContent = null;

        private String contentType = null;

        private String contentEncoding = null;

        private String messageUID = null;

        private String subject = null;

        public MessageImpl(String content,
                              String contentType,
                              String contentEncoding,
                              String subject)
        {
            this.textContent = content;
            this.contentType = contentType;
            this.contentEncoding = contentEncoding;
            this.subject = subject;

            //generate the uid
            this.messageUID = String.valueOf( System.currentTimeMillis())
                              + String.valueOf(hashCode());

        }

        public String getContent()
        {
            return textContent;
        }

        public String getContentType()
        {
            return contentType;
        }

        public String getEncoding()
        {
            return contentEncoding;
        }

        public String getMessageUID()
        {
            return messageUID;
        }

        public byte[] getRawData()
        {
            return getContent().getBytes();
        }

        public int getSize()
        {
            return getContent().length();
        }

        public String getSubject()
        {
            return subject;
        }
    }
}
