/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageReceivedEvent</tt>s indicate reception of an instant message.
 *
 * @author Emil Ivov
 */
public class ChatRoomMessageReceivedEvent
    extends EventObject
{
    /**
     * The chat room member that has sent this message.
     */
    private ChatRoomMember from = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * The received <tt>Message</tt>.
     */
    private Message message = null;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>ChatRoom</tt> for which the message is received.
     * @param from the <tt>ChatRoomMember</tt> that has sent this message.
     * @param timestamp the exact date when the event ocurred.
     * @param message the received <tt>Message</tt>.
     */
    public ChatRoomMessageReceivedEvent(ChatRoom source,
                                        ChatRoomMember from,
                                        Date timestamp,
                                        Message message)
    {
        super(source);

        this.from = from;
        this.timestamp = timestamp;
        this.message = message;
    }

    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     */
    public ChatRoomMember getSourceChatRoomMember()
    {
        return from;
    }

    /**
     * Returns the received message.
     * @return the <tt>Message</tt> that triggered this event.
     */
    public Message getMessage()
    {
        return message;
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that triggered this event.
     * @return the <tt>ChatRoom</tt> that triggered this event.
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom) getSource();
    }
}
