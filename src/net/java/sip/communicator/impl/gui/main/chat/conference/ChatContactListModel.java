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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Implements an <tt>AbstractListModel</tt> which represents a member list of
 * <tt>ChatContact</tt>s. The primary purpose of the implementation is to sort
 * the <tt>ChatContact</tt>s according to their member roles and in alphabetical
 * order according to their names.
 *
 * @author Lyubomir Marinov
 */
public class ChatContactListModel
    extends AbstractListModel
    implements ChatRoomMemberPropertyChangeListener
{

    /**
     * The backing store of this <tt>AbstractListModel</tt> listing the
     * <tt>ChatContact</tt>s.
     */
    private final List<ChatContact<?>> chatContacts
        = new ArrayList<ChatContact<?>>();

    /**
     * Current chat session.
     */
    private ChatSession chatSession;

    /**
     * The implementation of the sorting rules - the <tt>ChatContact</tt>s are
     * first sorted according to their roles in decreasing order of their
     * privileges and then they are sorted according to their names in
     * alphabetical order.
     */
    private final Comparator<ChatContact<?>> sorter
        = new Comparator<ChatContact<?>>()
        {
            public int compare(ChatContact<?> chatContact0, ChatContact<?> chatContact1)
            {
                /*
                 * Place ChatMembers with more privileges at the beginning of
                 * the list.
                 */
                if (chatContact0 instanceof ConferenceChatContact)
                {
                    if (chatContact1 instanceof ConferenceChatContact)
                    {
                        int role0
                            = ((ConferenceChatContact) chatContact0).getRole()
                                    .getRoleIndex();
                        int role1
                            = ((ConferenceChatContact) chatContact1).getRole()
                                    .getRoleIndex();

                        if (role0 > role1)
                            return -1;
                        else if (role0 < role1)
                            return 1;
                    }
                    else
                        return -1;
                }
                else if (chatContact1 instanceof ConferenceChatContact)
                    return 1;

                /* By default, sort the ChatContacts in alphabetical order. */
                return
                    chatContact0.getName().compareToIgnoreCase(
                            chatContact1.getName());
            }
        };

    /**
     * Creates the model.
     * @param chatSession The current model chat session.
     */
    public ChatContactListModel(ChatSession chatSession)
    {
        this.chatSession = chatSession;

        // when something like rename on a member change update the UI to
        // reflect it
        Object descriptor = chatSession.getDescriptor();

        if(descriptor instanceof ChatRoomWrapper)
        {
            ((ChatRoomWrapper) descriptor)
                .getChatRoom().addMemberPropertyChangeListener(this);
        }
    }

    /**
     * Listens for property change in chat room members.
     * @param ev the event
     */
    public void chatRoomPropertyChanged(ChatRoomMemberPropertyChangeEvent ev)
    {
        // Translate into
        // ListDataListener.contentsChanged.
        int chatContactCount = chatContacts.size();

        for (int i = 0; i < chatContactCount; i++)
        {
            ChatContact<?> chatContact = chatContacts.get(i);

            if(chatContact.getDescriptor().equals(ev.getSourceChatRoomMember()))
            {
                fireContentsChanged(chatContact, i, i);
                /*
                 * TODO Can ev.sourceChatRoomMember
                 * equal more than one chatContacts
                 * element? If it cannot, it will be
                 * more efficient to break here.
                 */
            }
        }
    }

    /**
     * Adds a specific <tt>ChatContact</tt> to this <tt>AbstractListModel</tt>
     * implementation and preserves the sorting it applies.
     *
     * @param chatContact a <tt>ChatContact</tt> to be added to this
     * <tt>AbstractListModel</tt>
     */
    public void addElement(ChatContact<?> chatContact)
    {
        if (chatContact == null)
            throw new IllegalArgumentException("chatContact");

        int index = -1;

        synchronized(chatContacts)
        {
            int chatContactCount = chatContacts.size();

            for (int i = 0; i < chatContactCount; i++)
            {
                ChatContact<?> containedChatContact = chatContacts.get(i);

                // We don't want duplicates.
                if (chatContact.equals(containedChatContact))
                    return;
                if ((index == -1)
                        && (sorter.compare(containedChatContact, chatContact) > 0))
                {
                    index = i;
                    // Continue in order to prevent duplicates.
                }
            }
            if (index == -1)
                index = chatContactCount;

            chatContacts.add(index, chatContact);
        }
        fireIntervalAdded(this, index, index);
    }

    /* Implements ListModel#getElementAt(int). */
    public ChatContact<?> getElementAt(int index)
    {
        synchronized(chatContacts)
        {
            return chatContacts.get(index);
        }
    }

    /* Implements ListModel#getSize(). */
    public int getSize()
    {
        synchronized(chatContacts)
        {
            return chatContacts.size();
        }
    }

    /**
     * Removes a specific <tt>ChatContact</tt> from this
     * <tt>AbstractListModel</tt> implementation.
     *
     * @param chatContact a <tt>ChatContact</tt> to be removed from this
     * <tt>AbstractListModel</tt> if it's already contained
     */
    public void removeElement(ChatContact<?> chatContact)
    {
        synchronized(chatContacts)
        {
            int index = chatContacts.indexOf(chatContact);

            if ((index >= 0) && chatContacts.remove(chatContact))
                fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Removes all the elements from this model.
     */
    public void removeAllElements()
    {
        if (chatContacts == null || chatContacts.size() <= 0)
            return;

        synchronized(chatContacts)
        {
            int contactsSize = chatContacts.size();
            chatContacts.clear();

            fireIntervalRemoved(this, 0, contactsSize - 1);
        }
    }

    /**
     * Runs clean-up.
     */
    public void dispose()
    {
        Object descriptor = chatSession.getDescriptor();

        if(descriptor instanceof ChatRoomWrapper)
        {
            ((ChatRoomWrapper) descriptor)
                .getChatRoom().removeMemberPropertyChangeListener(this);
        }
    }
}
