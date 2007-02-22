/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Instant messaging functionalites for the Gibberish protocol.
 *
 * @author Emil Ivov
 */
public class OperationSetBasicInstantMessagingGibberishImpl
    implements OperationSetBasicInstantMessaging
{
    /**
     * Currently registered message listeners.
     */
    private Vector messageListeners = new Vector();

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceGibberishImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceGibberishImpl parentProvider = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>OperationSetPersistentPresenceGibberishImpl</tt> instance.
     */
    public OperationSetBasicInstantMessagingGibberishImpl(
                ProtocolProviderServiceGibberishImpl        provider,
                OperationSetPersistentPresenceGibberishImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        if(!messageListeners.contains(listener))
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
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageGibberishImpl(new String(content), contentType
                                       , contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MessageGibberishImpl(messageText, DEFAULT_MIME_TYPE
                                        , DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Unregisteres <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception
     * of incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
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
     */
    public void sendInstantMessage(Contact to, Message message) throws
        IllegalStateException, IllegalArgumentException
    {
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(
                message, to, new Date());

        //first fire an event that we've delivered the message.
        //Note that in a real world protocol implementation we would first wait
        //for a delivery confirmation coming from the protocol.
        fireMessageDelivered(message, to);

        //then fire an event saying we've received the same message as if it
        //has been echoed by the "to" contact.
        fireMessageReceived(message, to);
    }

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    private void fireMessageDelivered(Message message, Contact to)
    {
        MessageDeliveredEvent evt
            = new MessageDeliveredEvent(message, to, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageDelivered(evt);
        }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     *
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    private void fireMessageReceived(Message message, Contact from)
    {
        MessageReceivedEvent evt
            = new MessageReceivedEvent(message, from, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageReceived(evt);
        }
    }

    /**
     * Determines wheter the protocol provider (or the protocol itself) support
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
}
