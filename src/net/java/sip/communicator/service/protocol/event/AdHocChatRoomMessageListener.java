/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;


/**
 * A listener that registers for <tt>AdHocChatRoomMessageEvent</tt>s issued by a
 * particular <tt>AdHocChatRoom</tt>.
 *
 * @author Valentin Martinet
 */
public interface AdHocChatRoomMessageListener
    extends EventListener
{
    /**
     * Called when a new incoming <tt>Message</tt> has been received.
     * @param evt the <tt>AdHocChatRoomMessageReceivedEvent</tt> containing the 
     * newly received message, its sender and other details.
     */
    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt);

    /**
     * Called when the underlying implementation has received an indication
     * that a message, sent earlier has been successfully received by the
     * destination.
     * @param evt the <tt>AdHocChatRoomMessageDeliveredEvent</tt> containing the
     * id of the message that has caused the event.
     */
    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt);

    /**
     * Called to indicate that delivery of a message sent earlier to the chat
     * room has failed. Reason code and phrase are contained by the
     * <tt>MessageFailedEvent</tt>
     * @param evt the <tt>AdHocChatroomMessageDeliveryFailedEvent</tt> 
     * containing the ID of the message whose delivery has failed.
     */
    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent evt);
}
