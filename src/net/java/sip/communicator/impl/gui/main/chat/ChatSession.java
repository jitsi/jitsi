/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.msghistory.*;

/**
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public interface ChatSession
{
    final String[] chatHistoryFilter
        = new String[]{ MessageHistoryService.class.getName(),
                        FileHistoryService.class.getName()};
    /**
     * Returns the descriptor of this chat session.
     * 
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor();

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
    public Iterator<ChatContact> getParticipants();

    /**
     * Returns all available chat transports for this chat session. Each chat
     * transport is corresponding to a protocol provider.
     * 
     * @return all available chat transports for this chat session.
     */
    public Iterator<ChatTransport> getChatTransports();

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     * 
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public ChatTransport getCurrentChatTransport();

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     * 
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer();

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     * 
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport);

    /**
     * Returns the name of the chat. If this chat panel corresponds to a single
     * chat it will return the name of the <tt>MetaContact</tt>, otherwise it
     * will return the name of the chat room.
     * 
     * @return the name of the chat
     */
    public String getChatName();

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     * 
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count);

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     * 
     * @param date The date up to which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryBeforeDate(Date date, int count);

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     * 
     * @param date The date from which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryAfterDate(Date date, int count);

    /**
     * Returns the start date of the history of this chat session.
     * 
     * @return the start date of the history of this chat session.
     */
    public long getHistoryStartDate();

    /**
     * Returns the end date of the history of this chat session.
     * 
     * @return the end date of the history of this chat session.
     */
    public long getHistoryEndDate();

    /**
     * Returns the default mobile number used to send sms-es in this session.
     * 
     * @return the default mobile number used to send sms-es in this session.
     */
    public String getDefaultSmsNumber();

    /**
     * Sets the default mobile number used to send sms-es in this session.
     * 
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber);

    /**
     * Disposes this chat session.
     */
    public void dispose();

    /**
     * Returns the ChatTransport corresponding to the given descriptor.
     * 
     * @param descriptor The descriptor of the chat transport we're looking for.
     * @return The ChatTransport corresponding to the given descriptor.
     */
    public ChatTransport findChatTransportForDescriptor(Object descriptor);

    /**
     * Returns the status icon of this chat session.
     *
     * @return the status icon of this chat session.
     */
    public ImageIcon getChatStatusIcon();

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public byte[] getChatAvatar();

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
    public boolean isContactListSupported();
    
    /**
     * Adds the given {@link ChatSessionChangeListener} to this 
     * <tt>ChatSession</tt>.
     * 
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public void addChatTransportChangeListener(ChatSessionChangeListener l);
    
    /**
     * Removes the given {@link ChatSessionChangeListener} to this 
     * <tt>ChatSession</tt>.
     * 
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public void removeChatTransportChangeListener(ChatSessionChangeListener l);
}
