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

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents an ad-hoc rendez-vous point where multiple chat users could
 * communicate together. This interface describes the main methods used by some
 * protocols for multi user chat, without useless methods (such as kicking a
 * participant) which aren't supported by these protocols (MSN, ICQ, Yahoo!,
 * etc.).
 *
 * <tt>AdHocChatRoom</tt> acts like a simplified <tt>ChatRoom</tt>.
 *
 * @author Valentin Martinet
 */
public interface AdHocChatRoom
{
    /**
     * Returns the name of this <tt>AdHocChatRoom</tt>. The name can't be
     * changed until the <tt>AdHocChatRoom</tt> is ended.
     *
     * @return a <tt>String</tt> containing the name
     */
    public String getName();

    /**
     * Returns the identifier of this <tt>AdHocChatRoom</tt>. The identifier of
     * the ad-hoc chat room would have the following syntax:
     * [adHocChatRoomName]@[adHocChatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>AdHocChatRoom</tt>.
     */
    public String getIdentifier();

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the ad-hoc room such as us being join, left...
     *
     * @param listener a member participation listener.
     */
    public void addParticipantPresenceListener(
            AdHocChatRoomParticipantPresenceListener listener);

    /**
     * Removes a participant presence listener.
     *
     * @param listener a member participation listener.
     */
    public void removeParticipantPresenceListener(
            AdHocChatRoomParticipantPresenceListener listener);

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this ad-hoc chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this ad-hoc chat room.
     */
    public void addMessageListener(AdHocChatRoomMessageListener listener);

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this ad-hoc room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this ad-hoc
     * room
     */
    public void removeMessageListener(AdHocChatRoomMessageListener listener);

    /**
     * Invites another <tt>Contact</tt> to this ad-hoc chat room.
     *
     * @param userAddress the address of the <tt>Contact</tt> of the user to
     * invite to the ad-hoc room.
     * @param reason a reason, subject, or welcome message that would tell
     * users why they are being invited.
     */
    public void invite(String userAddress, String reason);

    /**
     * Returns a <tt>List</tt> of <tt>Contact</tt>s corresponding to all
     * participants currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt>s instances
     * corresponding to all room members.
     */
    public List<Contact> getParticipants();

    /**
     * Returns the number of participants that are currently in this ad-hoc
     * chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this ad-hoc room.
     */
    public int getParticipantsCount();

    /**
     * Create a <tt>Message</tt> instance for sending a simple text messages
     * with default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText);

    /**
     * Sends the <tt>Message</tt> to this ad-hoc chat room.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message message)
        throws OperationFailedException;

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this ad-hoc room.
     */
    public ProtocolProviderService getParentProvider();

    /**
     * Joins this ad-hoc chat room with the nickname of the local user so that
     * the user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the ad-hoc room.
     */
    public void join()
        throws OperationFailedException;

    /**
     * Leaves this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered.
     */
    public void leave();
}
