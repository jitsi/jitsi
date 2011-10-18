/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;

/**
 * @author Yana Stamcheva
 */
public class ChatRoomListModel
    extends AbstractListModel
{
    private final ChatRoomList chatRoomList;

    public ChatRoomListModel()
    {
        chatRoomList = GuiActivator.getUIService()
            .getConferenceChatManager().getChatRoomList();
    }

    public Object getElementAt(int index)
    {
        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = chatRoomList.getChatRoomProviders();

        int currentIndex = 0;
        while(chatRoomProviders.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProviders.next();

            if (currentIndex == index)
            {
                // the current index is the index of the group so if this is the
                // searched index we return the group
                return provider;
            }
            else
            {
                int childCount = provider.countChatRooms();

                if (index <= (currentIndex + childCount))
                {
                    // if the searched index is lower than or equal to
                    // the greater child index in this group then our element is
                    // here
                    ChatRoomWrapper chatRoom = provider.getChatRoom(
                        index - currentIndex - 1);

                    return chatRoom;
                }
                else
                {
                    currentIndex += 1 + childCount;
                }
            }
        }

        return null;
    }

    public int getSize()
    {
        int size = 0;

        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = chatRoomList.getChatRoomProviders();

        while (chatRoomProviders.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProviders.next();

            size += 1 + provider.countChatRooms();
        }

        return size;
    }

    public int indexOf(Object o)
    {
        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = chatRoomList.getChatRoomProviders();

        int currentIndex = 0;
        while(chatRoomProviders.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProviders.next();

            if (provider.equals(o))
            {
                // the current index is the index of the group so if this is the
                // searched index we return the group
                return currentIndex;
            }
            else
            {
                if (o instanceof ChatRoomWrapper)
                {
                    int i = provider.indexOf((ChatRoomWrapper) o);

                    if (i != -1)
                    {
                        return currentIndex + i + 1;
                    }
                }

                currentIndex += provider.countChatRooms() + 1;
            }
        }

        return -1;
    }

    /**
     * Informs interested listeners that the content has changed of the cells
     * given by the range from startIndex to endIndex.
     *
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentChanged(int startIndex, int endIndex)
    {
        fireContentsChanged(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that new cells are added from startIndex to
     * endIndex.
     *
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentAdded(int startIndex, int endIndex)
    {
        fireIntervalAdded(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that a range of cells is removed.
     *
     * @param startIndex The start index of the range.
     * @param endIndex The end index of the range.
     */
    public void contentRemoved(int startIndex, int endIndex)
    {
        fireIntervalAdded(this, startIndex, endIndex);
    }
}
