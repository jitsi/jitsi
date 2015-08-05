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
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public abstract class ChatSession
{
    /**
     * The chat history filter.
     */
    protected final String[] chatHistoryFilter
        = new String[]{ MessageHistoryService.class.getName(),
                        FileHistoryService.class.getName()};

    /**
     * The list of <tt>ChatContact</tt>s contained in this chat session.
     */
    protected final List<ChatContact<?>> chatParticipants
        = new ArrayList<ChatContact<?>>();

    /**
     * The list of <tt>ChatTransport</tt>s available in this session.
     */
    protected final List<ChatTransport> chatTransports
        = new LinkedList<ChatTransport>();

    /**
     * The list of all <tt>ChatSessionChangeListener</tt>-s registered to listen
     * for transport modifications.
     */
    private final List<ChatSessionChangeListener>
            chatTransportChangeListeners
                = new ArrayList<ChatSessionChangeListener>();

    /**
     * The persistable address of the contact from the session.
     */
    protected String persistableAddress = null;

    /**
     * Returns the descriptor of this chat session.
     *
     * @return the descriptor of this chat session.
     */
    public abstract Object getDescriptor();

    /**
     * Returns the persistable address of the contact from the session.
     * @return the persistable address.
     */
    public String getPersistableAddress()
    {
        return persistableAddress;
    }

    /**
     * Returns <code>true</code> if this chat session descriptor is persistent,
     * otherwise returns <code>false</code>.
     * @return <code>true</code> if this chat session descriptor is persistent,
     * otherwise returns <code>false</code>.
     */
    public abstract boolean isDescriptorPersistent();

    /**
     * Returns an iterator to the list of all participants contained in this
     * chat session.
     *
     * @return an iterator to the list of all participants contained in this
     * chat session.
     */
    public Iterator<ChatContact<?>> getParticipants()
    {
        return chatParticipants.iterator();
    }

    /**
     * Returns all available chat transports for this chat session. Each chat
     * transport is corresponding to a protocol provider.
     *
     * @return all available chat transports for this chat session.
     */
    public Iterator<ChatTransport> getChatTransports()
    {
        return chatTransports.iterator();
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     *
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public abstract ChatTransport getCurrentChatTransport();

    /**
     * Returns a list of all <tt>ChatTransport</tt>s contained in this session
     * supporting the given <tt>opSetClass</tt>.
     * @param opSetClass the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ChatTransport</tt>s contained in this session
     * supporting the given <tt>opSetClass</tt>
     */
    public List<ChatTransport> getTransportsForOperationSet(
                                    Class<? extends OperationSet> opSetClass)
    {
        LinkedList<ChatTransport> opSetTransports
            = new LinkedList<ChatTransport>();

        for (ChatTransport transport : chatTransports)
        {
            if(transport.getProtocolProvider()
                    .getOperationSet(opSetClass) != null)
                opSetTransports.add(transport);
        }
        return opSetTransports;
    }

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     *
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public abstract ChatSessionRenderer getChatSessionRenderer();

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     *
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public abstract void setCurrentChatTransport(ChatTransport chatTransport);

    /**
     * Returns the name of the chat. If this chat panel corresponds to a single
     * chat it will return the name of the <tt>MetaContact</tt>, otherwise it
     * will return the name of the chat room.
     *
     * @return the name of the chat
     */
    public abstract String getChatName();

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public abstract Collection<Object> getHistory(int count);

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     *
     * @param date The date up to which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public abstract Collection<Object> getHistoryBeforeDate(Date date, int count);

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     *
     * @param date The date from which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public abstract Collection<Object> getHistoryAfterDate(Date date, int count);

    /**
     * Returns the start date of the history of this chat session.
     *
     * @return the start date of the history of this chat session.
     */
    public abstract Date getHistoryStartDate();

    /**
     * Returns the end date of the history of this chat session.
     *
     * @return the end date of the history of this chat session.
     */
    public abstract Date getHistoryEndDate();

    /**
     * Returns the default mobile number used to send sms-es in this session.
     *
     * @return the default mobile number used to send sms-es in this session.
     */
    public abstract String getDefaultSmsNumber();

    /**
     * Sets the default mobile number used to send sms-es in this session.
     *
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public abstract void setDefaultSmsNumber(String smsPhoneNumber);

    /**
     * Disposes this chat session.
     */
    public abstract void dispose();

    /**
     * Returns the ChatTransport corresponding to the given descriptor.
     *
     * @param descriptor The descriptor of the chat transport we're looking for.
     * @param resourceName The name of the resource if any, null otherwise
     * @return The ChatTransport corresponding to the given descriptor.
     */
    public ChatTransport findChatTransportForDescriptor(Object descriptor,
                                                        String resourceName)
    {
        for (ChatTransport chatTransport : chatTransports)
        {
            String transportResName = chatTransport.getResourceName();

            if (chatTransport.getDescriptor().equals(descriptor)
                && (resourceName == null
                    || (transportResName != null
                        && transportResName.equals(resourceName))))
                return chatTransport;
        }
        return null;
    }

    /**
     * Returns the status icon of this chat session.
     *
     * @return the status icon of this chat session.
     */
    public abstract ImageIcon getChatStatusIcon();

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public abstract byte[] getChatAvatar();

    /**
     * Gets the indicator which determines whether a contact list of (multiple)
     * participants is supported by this <code>ChatSession</code>. For example,
     * UI implementations may use the indicator to determine whether UI elements
     * should be created for the user to represent the contact list of the
     * participants in this <code>ChatSession</code>.
     *
     * @return <tt>true</tt> if this <code>ChatSession</code> supports a contact
     *         list of (multiple) participants; otherwise, <tt>false</tt>
     */
    public abstract boolean isContactListSupported();

    /**
     * Adds the given {@link ChatSessionChangeListener} to this
     * <tt>ChatSession</tt>.
     *
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public void addChatTransportChangeListener(
        ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            if (!chatTransportChangeListeners.contains(l))
                chatTransportChangeListeners.add(l);
        }
    }

    /**
     * Removes the given {@link ChatSessionChangeListener} to this
     * <tt>ChatSession</tt>.
     *
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public void removeChatTransportChangeListener(
        ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            chatTransportChangeListeners.remove(l);
        }
    }

    /**
     * Fires a event that current ChatTransport has changed.
     */
    public void fireCurrentChatTransportChange()
    {
        List<ChatSessionChangeListener> listeners = null;
        synchronized (chatTransportChangeListeners)
        {
            listeners = new ArrayList<ChatSessionChangeListener>(
                chatTransportChangeListeners);
        }

        for (ChatSessionChangeListener l : listeners)
            l.currentChatTransportChanged(this);
    }

    /**
     * Fires a event that current ChatTransport has been updated.
     */
    public void fireCurrentChatTransportUpdated(int eventID)
    {
        List<ChatSessionChangeListener> listeners = null;
        synchronized (chatTransportChangeListeners)
        {
            listeners = new ArrayList<ChatSessionChangeListener>(
                chatTransportChangeListeners);
        }

        for (ChatSessionChangeListener l : listeners)
            l.currentChatTransportUpdated(eventID);
    }
}
