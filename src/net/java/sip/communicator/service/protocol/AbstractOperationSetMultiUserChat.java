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
 * Represents a default implementation of <code>OperationSetMultiUserChat</code>
 * in order to make it easier for implementers to provide complete solutions
 * while focusing on implementation-specific details.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetMultiUserChat
    implements OperationSetMultiUserChat
{

    /**
     * The list of the currently registered
     * <code>ChatRoomInvitationListener</code>s.
     */
    private final List<ChatRoomInvitationListener> invitationListeners
        = new Vector<ChatRoomInvitationListener>();

    /**
     * The list of <code>ChatRoomInvitationRejectionListener</code>s subscribed
     * for events indicating rejection of a multi user chat invitation sent by
     * us.
     */
    private final List<ChatRoomInvitationRejectionListener> invitationRejectionListeners
        = new Vector<ChatRoomInvitationRejectionListener>();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private final List<LocalUserChatRoomPresenceListener> presenceListeners
        = new Vector<LocalUserChatRoomPresenceListener>();

    /*
     * Implements
     * OperationSetMultiUserChat#addInvitationListener(
     * ChatRoomInvitationListener).
     */
    public void addInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            if (!invitationListeners.contains(listener))
                invitationListeners.add(listener);
        }
    }

    /*
     * ImplementsOperationSetMultiUserChat#addInvitationRejectionListener(
     * ChatRoomInvitationRejectionListener).
     */
    public void addInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#addPresenceListener(
     * LocalUserChatRoomPresenceListener).
     */
    public void addPresenceListener(LocalUserChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            if (!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

    /**
     * Fires a new <code>ChatRoomInvitationReceivedEvent</code> to all currently
     * registered <code>ChatRoomInvitationListener</code>s to notify about the
     * receipt of a specific <code>ChatRoomInvitation</code>.
     *
     * @param invitation
     *            the <code>ChatRoomInvitation</code> which has been received
     */
    protected void fireInvitationReceived(ChatRoomInvitation invitation)
    {
        ChatRoomInvitationReceivedEvent evt
            = new ChatRoomInvitationReceivedEvent(
                    this,
                    invitation,
                    new Date(System.currentTimeMillis()));

        ChatRoomInvitationListener[] listeners;
        synchronized (invitationListeners)
        {
            listeners
                = invitationListeners
                    .toArray(
                        new ChatRoomInvitationListener[
                                invitationListeners.size()]);
        }

        for (ChatRoomInvitationListener listener : listeners)
            listener.invitationReceived(evt);
    }

    /**
     * Delivers a <tt>ChatRoomInvitationRejectedEvent</tt> to all
     * registered <tt>ChatRoomInvitationRejectionListener</tt>s.
     *
     * @param sourceChatRoom the room that invitation refers to
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     */
    protected void fireInvitationRejectedEvent(ChatRoom sourceChatRoom,
                                            String invitee,
                                            String reason)
    {
        ChatRoomInvitationRejectedEvent evt
            = new ChatRoomInvitationRejectedEvent(
                    this,
                    sourceChatRoom,
                    invitee,
                    reason,
                    new Date(System.currentTimeMillis()));

        ChatRoomInvitationRejectionListener[] listeners;
        synchronized (invitationRejectionListeners)
        {
            listeners
                = invitationRejectionListeners
                    .toArray(
                        new ChatRoomInvitationRejectionListener[
                                invitationRejectionListeners.size()]);
        }

        for (ChatRoomInvitationRejectionListener listener : listeners)
            listener.invitationRejected(evt);
    }

    /**
     * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom
     *            the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param eventType
     *            the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason
     *            the reason
     */
    public void fireLocalUserPresenceEvent(
        ChatRoom chatRoom,
        String eventType,
        String reason)
    {
        this.fireLocalUserPresenceEvent(chatRoom, eventType, reason, null);
    }

    /**
     * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     * LOCAL_USER_LEFT, etc.
     * @param reason the reason
     * @param alternateAddress address of the new room, if old is destroyed.
     */
    public void fireLocalUserPresenceEvent(
        ChatRoom chatRoom,
        String eventType,
        String reason,
        String alternateAddress)
    {
        LocalUserChatRoomPresenceChangeEvent evt
            = new LocalUserChatRoomPresenceChangeEvent( this,
                                                        chatRoom,
                                                        eventType,
                                                        reason,
                                                        alternateAddress);

        LocalUserChatRoomPresenceListener[] listeners;
        synchronized (presenceListeners)
        {
            listeners
                = presenceListeners
                    .toArray(
                        new LocalUserChatRoomPresenceListener[
                                presenceListeners.size()]);
        }

        for (LocalUserChatRoomPresenceListener listener : listeners)
            listener.localUserPresenceChanged(evt);
    }

    /*
     * Implements
     * OperationSetMultiUserChat#removeInvitationListener(
     * ChatRoomInvitationListener).
     */
    public void removeInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#removeInvitationRejectionListener(
     * ChatRoomInvitationRejectionListener).
     */
    public void removeInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#removePresenceListener(
     * LocalUserChatRoomPresenceListener).
     */
    public void removePresenceListener(
        LocalUserChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            presenceListeners.remove(listener);
        }
    }
}
