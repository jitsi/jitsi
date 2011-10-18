/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Implements an <tt>AbstractListModel</tt> which represents a member list of
 * <tt>ChatContact</tt>s. The primary purpose of the implementation is to sort
 * the <tt>ChatContact</tt>s according to their member roles and in alphabetical
 * order according to their names.
 *
 * @author Lubomir Marinov
 */
public class ChatContactListModel
    extends AbstractListModel
{

    /**
     * The backing store of this <tt>AbstractListModel</tt> listing the
     * <tt>ChatContact</tt>s.
     */
    private final List<ChatContact<?>> chatContacts =
        new ArrayList<ChatContact<?>>();

    /**
     * The implementation of the sorting rules - the <tt>ChatContact</tt>s are
     * first sorted according to their roles in decreasing order of their
     * privileges and then they are sorted according to their names in
     * alphabetical order.
     */
    private final Comparator<ChatContact<?>> sorter =
    new Comparator<ChatContact<?>>()
    {
        public int compare(ChatContact<?> chatContact0,
                ChatContact<?> chatContact1)
        {

            /*
             * Place ChatMembers with more privileges at the beginning of the
             * list.
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
        // when something like rename on a member change
        // update the UI to reflect it
        if(chatSession.getDescriptor() instanceof ChatRoomWrapper)
        {
            ((ChatRoomWrapper)chatSession.getDescriptor()).getChatRoom()
                .addMemberPropertyChangeListener(
                    new ChatRoomMemberPropertyChangeListener()
                {
                    public void chatRoomPropertyChanged(
                        ChatRoomMemberPropertyChangeEvent event)
                    {
                        // find the index and fire
                        // that content has changed
                        int chatContactCount = chatContacts.size();

                        for (int i = 0; i < chatContactCount; i++)
                        {
                            ChatContact<?> containedChatContact = chatContacts.get(i);

                            if(containedChatContact.getDescriptor().equals(
                                event.getSourceChatRoomMember()))
                            {
                                fireContentsChanged(containedChatContact, i, i);
                            }
                        }
                    }
            });
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
    public Object getElementAt(int index)
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
}
