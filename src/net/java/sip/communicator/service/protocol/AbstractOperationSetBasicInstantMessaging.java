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
 * @author Lyubomir Marinov
 */
public abstract class AbstractOperationSetBasicInstantMessaging
    implements OperationSetBasicInstantMessaging
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetBasicInstantMessaging</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetBasicInstantMessaging.class);

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

    /**
     * Create a Message instance with the specified UID, content type
     * and a default encoding.
     * This method can be useful when message correction is required. One can
     * construct the corrected message to have the same UID as the message
     * before correction.
     *
     * @param messageText the string content of the message.
     * @param contentType the MIME-type for <tt>content</tt>
     * @param messageUID the unique identifier of this message.
     * @return Message the newly created message
     */
    public Message createMessageWithUID(
        String messageText, String contentType, String messageUID)
    {
        return createMessage(messageText);
    }

    /**
     * {@inheritDoc}
     */
    public Message createMessageWithUID(String messageText, String messageUID)
    {
        return createMessage(messageText);
    }

    public abstract Message createMessage(
        String content, String contentType, String encoding, String subject);

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    protected void fireMessageDelivered(Message message, Contact to)
    {
        fireMessageEvent(
            new MessageDeliveredEvent(message, to, new Date()));
    }

    protected void fireMessageDeliveryFailed(
        Message message,
        Contact to,
        int errorCode)
    {
        fireMessageEvent(
            new MessageDeliveryFailedEvent(message, to, errorCode));
    }

    enum MessageEventType{
        None,
        MessageDelivered,
        MessageReceived,
        MessageDeliveryFailed,
        MessageDeliveryPending,
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

        if (logger.isDebugEnabled())
            logger.debug("Dispatching Message Listeners=" + listeners.size()
            + " evt=" + evt);

        /*
         * TODO Create a super class like this MessageEventObject that would
         * contain the MessageEventType. Also we could fire an event for the
         * MessageDeliveryPending event type (modify MessageListener and
         * OperationSetInstantMessageTransform).
         */
        MessageEventType eventType = MessageEventType.None;
        if (evt instanceof MessageDeliveredEvent)
        {
            eventType = MessageEventType.MessageDelivered;
        }
        else if (evt instanceof MessageReceivedEvent)
        {
            eventType = MessageEventType.MessageReceived;
        }
        else if (evt instanceof MessageDeliveryFailedEvent)
        {
            eventType = MessageEventType.MessageDeliveryFailed;
        }

        // Transform the event.
        EventObject[] events = messageTransform(evt, eventType);
        for (EventObject event : events)
        {
            try
            {
                if (event == null)
                    return;

                for (MessageListener listener : listeners)
                {
                    switch (eventType)
                    {
                    case MessageDelivered:
                        listener.messageDelivered((MessageDeliveredEvent) event);
                        break;
                    case MessageDeliveryFailed:
                        listener
                            .messageDeliveryFailed((MessageDeliveryFailedEvent) event);
                        break;
                    case MessageReceived:
                        listener.messageReceived((MessageReceivedEvent) event);
                        break;
                    default:
                        /*
                         * We either have nothing to do or we do not know what
                         * to do. Anyway, we'll silence the compiler.
                         */
                        break;
                    }
                }
            }
            catch (Throwable e)
            {
                logger.error("Error delivering message", e);
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
        fireMessageEvent(
            new MessageReceivedEvent(message, from, new Date()));
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

    /**
     * Messages pending delivery to be transformed.
     *
     * @param evt the message delivery event
     * @return returns message delivery events
     */
    public MessageDeliveredEvent[] messageDeliveryPendingTransform(
            final MessageDeliveredEvent evt)
    {
        EventObject[] transformed = messageTransform(
            evt, MessageEventType.MessageDeliveryPending);

        final int size = transformed.length;
        MessageDeliveredEvent[] events =
            new MessageDeliveredEvent[size];
        System.arraycopy(transformed, 0, events, 0, size);
        return events;
    }

    /**
     * Transform provided source event by processing transform layers in
     * sequence.
     *
     * @param evt the source event to transform
     * @param eventType the event type of the source event
     * @return returns the resulting (transformed) events, if any. (I.e. an
     *         array of 0 or more size containing events.)
     */
    private EventObject[] messageTransform(final EventObject evt,
                                           final MessageEventType eventType)
    {
        if (evt == null)
        {
            return new EventObject[0];
        }
        ProtocolProviderService protocolProvider;
        switch (eventType)
        {
        case MessageDelivered:
            protocolProvider
                = ((MessageDeliveredEvent) evt)
                    .getDestinationContact().getProtocolProvider();
            break;
        case MessageDeliveryFailed:
            protocolProvider
                = ((MessageDeliveryFailedEvent) evt)
                    .getDestinationContact().getProtocolProvider();
            break;
        case MessageDeliveryPending:
            protocolProvider
                = ((MessageDeliveredEvent) evt)
                    .getDestinationContact().getProtocolProvider();
            break;
        case MessageReceived:
            protocolProvider
                = ((MessageReceivedEvent) evt)
                    .getSourceContact().getProtocolProvider();
            break;
        default:
            return new EventObject[] {evt};
        }

        OperationSetInstantMessageTransformImpl opSetMessageTransform
            = (OperationSetInstantMessageTransformImpl) protocolProvider
                .getOperationSet(OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform == null)
            return new EventObject[] {evt};

        // 'current' contains the events that need to be transformed. It should
        // not contain null values.
        final LinkedList<EventObject> current = new LinkedList<EventObject>();
        // Add source event as start of transformation.
        current.add(evt);
        // 'next' contains the resulting events after transformation in the
        // current iteration. It should not contain null values.
        final LinkedList<EventObject> next = new LinkedList<EventObject>();
        for (Map.Entry<Integer, Vector<TransformLayer>> entry
                : opSetMessageTransform.transformLayers.entrySet())
        {
            for (TransformLayer transformLayer : entry.getValue())
            {
                next.clear();
                while (!current.isEmpty())
                {
                    final EventObject event = current.remove();
                    switch (eventType)
                    {
                    case MessageDelivered:
                        MessageDeliveredEvent transformedDelivered =
                            transformLayer.messageDelivered(
                                (MessageDeliveredEvent) event);
                        if (transformedDelivered != null)
                        {
                            next.add(transformedDelivered);
                        }
                        break;
                    case MessageDeliveryPending:
                        MessageDeliveredEvent[] evts = transformLayer
                            .messageDeliveryPending(
                                (MessageDeliveredEvent) event);
                        for (MessageDeliveredEvent mde : evts)
                        {
                            if (mde != null)
                            {
                                next.add(mde);
                            }
                        }
                        break;
                    case MessageDeliveryFailed:
                        MessageDeliveryFailedEvent transformedDeliveryFailed =
                            transformLayer.messageDeliveryFailed(
                                (MessageDeliveryFailedEvent) event);
                        if (transformedDeliveryFailed != null)
                        {
                            next.add(transformedDeliveryFailed);
                        }
                        break;
                    case MessageReceived:
                        MessageReceivedEvent transformedReceived =
                            transformLayer
                                .messageReceived((MessageReceivedEvent) event);
                        if (transformedReceived != null)
                        {
                            next.add(transformedReceived);
                        }
                        break;
                    default:
                        next.add(event);
                        /*
                         * We either have nothing to do or we do not know
                         * what to do. Anyway, we'll silence the compiler.
                         */
                        break;
                    }
                }
                // Set events for next round of transformations.
                current.addAll(next);
            }
        }

        return current.toArray(new EventObject[current.size()]);
    }

    /**
     * Determines whether the protocol supports the supplied content type
     * for the given contact.
     *
     * @param contentType the type we want to check
     * @param contact contact which is checked for supported contentType
     * @return <tt>true</tt> if the contact supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType, Contact contact)
    {
        // by default we support default mime type, for other mime-types
        // method must be overridden
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;

        return false;
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt>. Provides a default implementation of this method.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    public void sendInstantMessage( Contact to,
                                    ContactResource toResource,
                                    Message message)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        sendInstantMessage(to, message);
    }

    /**
     * Returns the inactivity timeout in milliseconds.
     *
     * @return The inactivity timeout in milliseconds. Or -1 if undefined
     */
    public long getInactivityTimeout()
    {
        return -1;
    }
}
