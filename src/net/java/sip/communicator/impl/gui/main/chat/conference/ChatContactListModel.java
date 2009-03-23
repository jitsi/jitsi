/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;

/**
 * Implements an <code>AbstractListModel</code> which represents a member list
 * of <code>ChatContact</code>s. The primary purpose of the implementation is to
 * sort the <code>ChatContact</code>s sorted according to their member roles and
 * in alphabetical order according to their names.
 *  
 * @author Lubomir Marinov
 */
public class ChatContactListModel
    extends AbstractListModel
{

    /**
     * The backing store of this <code>AbstractListModel</code> listing the
     * <code>ChatContact</code>s.
     */
    private final List<ChatContact> chatContacts = new ArrayList<ChatContact>();

    /**
     * The implementation of the sorting rules - the <code>ChatContact</code>s
     * are first sorted according to their roles in decreasing order of their
     * privileges and then they are sorted according to their names in
     * alphabetical order. 
     */
    private final Comparator<ChatContact> sorter = new Comparator<ChatContact>()
    {
        public int compare(ChatContact chatContact0, ChatContact chatContact1)
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
     * Adds a specific <code>ChatContact</code> to this
     * <code>AbstractListModel</code> implementation and preserves the sorting
     * it applies.
     * 
     * @param chatContact a <code>ChatContact</code> to be added to this
     *            <code>AbstractListModel</code>
     */
    public void addElement(ChatContact chatContact)
    {
        if (chatContact == null)
            throw new IllegalArgumentException("chatContact");

        int index = 0;
        int chatContactCount = chatContacts.size();
        for (; index < chatContactCount; index++)
            if (sorter.compare(chatContacts.get(index), chatContact) > 0)
                break;

        chatContacts.add(index, chatContact);
        fireIntervalAdded(this, index, index);
    }

    /* Implements ListModel#getElementAt(int). */
    public Object getElementAt(int index)
    {
        return chatContacts.get(index);
    }

    /* Implements ListModel#getSize(). */
    public int getSize()
    {
        return chatContacts.size();
    }

    /**
     * Removes a specific <code>ChatContact</code> from this
     * <code>AbstractListModel</code> implementation.
     * 
     * @param chatContact a <code>ChatContact</code> to be removed from this
     *            <code>AbstractListModel</code> if it's already contained
     */
    public void removeElement(ChatContact chatContact)
    {
        int index = chatContacts.indexOf(chatContact);

        if (chatContacts.remove(chatContact) && (index >= 0))
            fireIntervalRemoved(this, index, index);
    }
}
