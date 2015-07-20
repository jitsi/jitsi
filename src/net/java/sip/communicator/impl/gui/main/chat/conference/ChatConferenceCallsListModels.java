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
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements an <tt>AbstractListModel</tt> which represents a member list of
 * <tt>ConferenceDescription</tt>s. 
 *
 * @author Hristo Terezov
 */
public class ChatConferenceCallsListModels
    extends AbstractListModel
{

    /**
     * The backing store of this <tt>AbstractListModel</tt> listing the
     * <tt>ChatContact</tt>s.
     */
    private final List<ConferenceDescription> chatConferenceCalls
        = new ArrayList<ConferenceDescription>();

    private ChatSession chatSession;
    
    /**
     * Creates the model.
     * @param chatSession The current model chat session.
     */
    public ChatConferenceCallsListModels(ChatSession chatSession)
    {
        this.chatSession = chatSession;
        
    }

    /**
     * Initializes the list of the conferences that are already announced.
     */
    public void initConferences()
    {
        Object descriptor = chatSession.getDescriptor();
        
        if(descriptor instanceof ChatRoomWrapper)
        {
            ChatRoom chatRoom = ((ChatRoomWrapper)descriptor).getChatRoom();
            for(ConferenceDescription cd : 
                chatRoom.getCachedConferenceDescriptions().values())
            {
                if(cd.isAvailable())
                {
                   addElement(cd);
                }
                else
                {
                   removeElement(cd);
                }
            }
        }
    }

    /**
     * Adds a specific <tt>ConferenceDescription</tt> to this model 
     * implementation.
     *
     * @param chatConference a <tt>ConferenceDescription</tt> to be added to 
     * this model.
     */
    public void addElement(ConferenceDescription chatConference)
    {
        if (chatConference == null)
            throw new IllegalArgumentException("ConferenceDescription");
        
        int index = -1;

        synchronized(chatConferenceCalls)
        {
            int chatContactCount = chatConferenceCalls.size();

            if(findConferenceDescription(chatConference) != -1)
                return;
            index = chatContactCount;
            chatConferenceCalls.add(index, chatConference);
        }
        fireIntervalAdded(this, index, index);
    }

    /**
     * Returns <tt>ConferenceDescription</tt> instance at the specified index of
     * the conferences list.
     * 
     * @param index the index.
     * @return the <tt>ConferenceDescription</tt> instance.
     */
    public ConferenceDescription getElementAt(int index)
    {
        synchronized(chatConferenceCalls)
        {
            return chatConferenceCalls.get(index);
        }
    }

    /**
     * Returns the size of the conferences list.
     * 
     * @return the size.
     */
    public int getSize()
    {
        synchronized(chatConferenceCalls)
        {
            return chatConferenceCalls.size();
        }
    }

    /**
     * Removes a specific <tt>ConferenceDescription</tt> from this model 
     * implementation.
     *
     * @param chatConference a <tt>ConferenceDescription</tt> to be removed from
     * this model if it's already contained
     */
    public void removeElement(ConferenceDescription chatConference)
    {
        synchronized(chatConferenceCalls)
        {
            int index = findConferenceDescription(chatConference);

            if (index >= 0)
            {
                chatConferenceCalls.remove(index);
                fireIntervalRemoved(this, index, index);
            }
                
        }
    }
    
    /**
     * Finds <tt>ConferenceDescription</tt> instance from the list of 
     * conferences that matches specific <tt>ConferenceDescription</tt> 
     * instance.
     * 
     * @param cd the <tt>ConferenceDescription</tt> instance we are searching 
     * for.
     * @return the index of the <tt>ConferenceDescription</tt> instance in the
     * list of conferences that has the same call id, URI and supported 
     * transports.
     */
    private int findConferenceDescription(ConferenceDescription cd)
    {
        for(int i = 0; i < chatConferenceCalls.size(); i++)
        {
            if(cd.compareConferenceDescription(chatConferenceCalls.get(i)))
            {
                return i;
            }
        }
        return -1;
    }
}
