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
 * An abstract class with a default implementation of some of the methods of
 * the <tt>ChatRoom</tt> interface.
 *
 * @author Boris Grozev
 */
public abstract class AbstractChatRoom
    implements ChatRoom
{
    /**
     * The list of listeners to be notified when a member of the chat room
     * publishes a <tt>ConferenceDescription</tt>
     */
    protected final List<ChatRoomConferencePublishedListener>
            conferencePublishedListeners
                = new LinkedList<ChatRoomConferencePublishedListener>();

    /**
     * The list of all <tt>ConferenceDescription</tt> that were announced and 
     * are not yet processed.
     */
    protected Map<String, ConferenceDescription> cachedConferenceDescriptions
        = new HashMap<String, ConferenceDescription>();
    
    /**
     * {@inheritDoc}
     */
    public void addConferencePublishedListener(
            ChatRoomConferencePublishedListener listener)
    {
        synchronized (conferencePublishedListeners)
        {
            conferencePublishedListeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeConferencePublishedListener(
            ChatRoomConferencePublishedListener listener)
    {
        synchronized (conferencePublishedListeners)
        {
            conferencePublishedListeners.remove(listener);
        }
    }

    /**
     * Returns cached <tt>ConferenceDescription</tt> instances.
     * @return the cached <tt>ConferenceDescription</tt> instances.
     */
    public Map<String, ConferenceDescription> getCachedConferenceDescriptions()
    {
        Map<String, ConferenceDescription> tmpCachedConferenceDescriptions;
        synchronized (cachedConferenceDescriptions)
        {
            tmpCachedConferenceDescriptions 
                = new HashMap<String, ConferenceDescription>(
                    cachedConferenceDescriptions);
        }
        return tmpCachedConferenceDescriptions;
    }

    /**
     * Returns the number of cached <tt>ConferenceDescription</tt> instances.
     * @return the number of cached <tt>ConferenceDescription</tt> instances.
     */
    public synchronized int getCachedConferenceDescriptionSize()
    {
        return cachedConferenceDescriptions.size();
    }

    /**
     * Creates the corresponding <tt>ChatRoomConferencePublishedEvent</tt> and
     * notifies all <tt>ChatRoomConferencePublishedListener</tt>s that
     * <tt>member</tt> has published a conference description.
     *
     * @param member the <tt>ChatRoomMember</tt> that published <tt>cd</tt>.
     * @param cd the <tt>ConferenceDescription</tt> that was published.
     * @param eventType the type of the event.
     */
    protected void fireConferencePublishedEvent(
            ChatRoomMember member,
            ConferenceDescription cd,
            int eventType)
    {
        ChatRoomConferencePublishedEvent evt
                = new ChatRoomConferencePublishedEvent(eventType, this, member, 
                    cd);

        List<ChatRoomConferencePublishedListener> listeners;
        synchronized (conferencePublishedListeners)
        {
            listeners  = new LinkedList<ChatRoomConferencePublishedListener>(
                    conferencePublishedListeners);
        }

        for (ChatRoomConferencePublishedListener listener : listeners)
            listener.conferencePublished(evt);
    }
    
    /**
     * Processes the <tt>ConferenceDescription</tt> instance and adds/removes 
     * it to the list of conferences.
     * 
     * @param cd the <tt>ConferenceDescription</tt> instance to process.
     * @param participantName the name of the participant that sent the 
     * <tt>ConferenceDescription</tt>.
     * @return <tt>true</tt> on success and <tt>false</tt> if fail.
     */
    protected boolean processConferenceDescription(ConferenceDescription cd, 
        String participantName)
    {
        if(cd.isAvailable())
        {
            if(cachedConferenceDescriptions.containsKey(participantName))
                return false;
            cachedConferenceDescriptions.put(participantName, cd);
        }
        else
        {
            ConferenceDescription cachedDescription
                = cachedConferenceDescriptions.get(participantName);
            
            if(cachedDescription == null
                || !cd.compareConferenceDescription(cachedDescription))
                return false;
            
            cachedConferenceDescriptions.remove(participantName);
        }
        
        return true;
        
    }
    
    /**
     * Clears the list with the chat room conferences.
     */
    protected void clearCachedConferenceDescriptionList()
    {
        cachedConferenceDescriptions.clear();
    }
}
