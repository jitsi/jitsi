/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * {@link OperationSetBasicInstantMessaging} in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 * 
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetBasicInstantMessaging
    implements OperationSetBasicInstantMessaging
{
    private static final Logger logger =
        Logger.getLogger(AbstractOperationSetBasicInstantMessaging.class);

    /**
     * A list of listeners registered for message events.
     */
    private final List<MessageListener> messageListeners =
        new LinkedList<MessageListener>();

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     * 
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        synchronized (messageListeners)
        {
            if (!messageListeners.contains(listener))
            {
                messageListeners.add(listener);
            }
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     * 
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param encoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *            subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
        String encoding, String subject)
    {
        String contentAsString = null;
        boolean useDefaultEncoding = true;
        if (encoding != null)
        {
            try
            {
                contentAsString = new String(content, encoding);
                useDefaultEncoding = false;
            }
            catch (UnsupportedEncodingException ex)
            {
                logger.warn("Failed to decode content using encoding "
                    + encoding, ex);

                // We'll use the default encoding.
            }
        }
        if (useDefaultEncoding)
        {
            encoding = Charset.defaultCharset().name();
            contentAsString = new String(content);
        }

        return createMessage(contentAsString, contentType, encoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     * 
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return createMessage(messageText, DEFAULT_MIME_TYPE,
            DEFAULT_MIME_ENCODING, null);
    }

    public abstract Message createMessage(String content, String contentType,
        String encoding, String subject);

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     * 
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    protected void fireMessageDelivered(Message message, Contact to)
    {
        fireMessageEvent(new MessageDeliveredEvent(message, to, new Date()));
    }

    /**
     * Delivers the specified event to all registered message listeners.
     * 
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     *            registered message listeners.
     */
    protected void fireMessageEvent(EventObject evt)
    {
        Collection<MessageListener> listeners = null;
        synchronized (this.messageListeners)
        {
            listeners = new ArrayList<MessageListener>(this.messageListeners);
        }

        logger.debug("Dispatching Message Listeners=" + listeners.size()
            + " evt=" + evt);

        for (Iterator<MessageListener> listenerIter = listeners.iterator(); listenerIter
            .hasNext();)
        {
            MessageListener listener = listenerIter.next();

            if (evt instanceof MessageDeliveredEvent)
            {
                listener.messageDelivered((MessageDeliveredEvent) evt);
            }
            else if (evt instanceof MessageReceivedEvent)
            {
                listener.messageReceived((MessageReceivedEvent) evt);
            }
            else if (evt instanceof MessageDeliveryFailedEvent)
            {
                listener
                    .messageDeliveryFailed((MessageDeliveryFailedEvent) evt);
            }
        }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     * 
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    protected void fireMessageReceived(Message message, Contact from)
    {
        fireMessageEvent(new MessageReceivedEvent(message, from, new Date()));
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     * 
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        synchronized (messageListeners)
        {
            messageListeners.remove(listener);
        }
    }
}
