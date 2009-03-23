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
 * @author Lubomir Marinov
 */
public class ChatContactListModel
    extends AbstractListModel
{
    private final List<ChatContact> chatContacts = new ArrayList<ChatContact>();

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

    public Object getElementAt(int index)
    {
        return chatContacts.get(index);
    }

    public int getSize()
    {
        return chatContacts.size();
    }

    public void removeElement(ChatContact chatContact)
    {
        int index = chatContacts.indexOf(chatContact);

        if (chatContacts.remove(chatContact) && (index >= 0))
            fireIntervalRemoved(this, index, index);
    }
}
