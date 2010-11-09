/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * <tt>MessageWaitingEvent<tt> indicates a message waiting event
 * is received.
 *
 * @author Damian Minkov
 */
public class MessageWaitingEvent
        extends EventObject
{
    /**
     * The URI we can use to reach messages from provider that is firing
     * the event.
     */
    private String account;

    /**
     * Number of new/unread messages.
     */
    private int unreadMessages = 0;

    /**
     * Number of old/read messages.
     */
    private int readMessages = 0;

    /**
     * Number of new/unread urgent messages.
     */
    private int unreadUrgentMessages = 0;

    /**
     * Number of old/read messages.
     */
    private int readUrgentMessages = 0;

    /**
     * The message type for this event.
     */
    private OperationSetMessageWaiting.MessageType messageType;

    /**
     * Constructs the Event with the given source, typically the provider and
     * number of messages.
     *
     * @param messageType the message type for this event.
     * @param source the protocol provider from which this event is coming.
     * @param account the account URI we can use to reach the messages.
     * @param unreadMessages the unread messages.
     * @param readMessages the read messages.
     * @param unreadUrgentMessages the unread urgent messages.
     * @param readUrgentMessages the read urgent messages.
     */
    public MessageWaitingEvent(
            ProtocolProviderService source,
            OperationSetMessageWaiting.MessageType messageType,
            String account,
            int unreadMessages,
            int readMessages,
            int unreadUrgentMessages,
            int readUrgentMessages)
    {
        super(source);

        this.messageType = messageType;
        this.account = account;
        this.unreadMessages = unreadMessages;
        this.readMessages = readMessages;
        this.unreadUrgentMessages = unreadUrgentMessages;
        this.readUrgentMessages = readUrgentMessages;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> which originated this event.
     *
     * @return the source <tt>ProtocolProviderService</tt>
     */
    public ProtocolProviderService getSourceProvider()
    {
        return (ProtocolProviderService) getSource();
    }

    /**
     * The URI we can use to reach messages from provider that is firing
     * the event.
     * @return account URI.
     */
    public String getAccount()
    {
        return account;
    }

    /**
     * Number of new/unread messages.
     * @return Number of new/unread messages.
     */
    public int getUnreadMessages()
    {
        return unreadMessages;
    }

    /**
     * Number of old/read messages.
     * @return Number of old/read messages.
     */
    public int getReadMessages()
    {
        return readMessages;
    }

    /**
     * Number of new/unread urgent messages.
     * @return Number of new/unread urgent messages.
     */
    public int getUnreadUrgentMessages()
    {
        return unreadUrgentMessages;
    }

    /**
     * Number of old/read messages.
     * @return Number of old/read messages.
     */
    public int getReadUrgentMessages()
    {
        return readUrgentMessages;
    }

    /**
     * The message type for this event.
     * @return the message type.
     */
    public OperationSetMessageWaiting.MessageType getMessageType()
    {
        return messageType;
    }
}
