/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * An implementation of <tt>ChatSession</tt> for conference chatting.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class ConferenceChatSession
    implements  ChatSession,
                ChatRoomMemberPresenceListener,
                ChatRoomPropertyChangeListener
{
    /**
     * The list of all chat participants.
     */
    private final List<ChatContact> chatParticipants
        = new ArrayList<ChatContact>();

    /**
     * The list of available chat transports.
     */
    private final List<ChatTransport> chatTransports
        = new ArrayList<ChatTransport>();

    /**
     * The current chat transport used for messaging.
     */
    private ChatTransport currentChatTransport;

    /**
     * The chat room wrapper, which is the descriptor of this chat session.
     */
    private final ChatRoomWrapper chatRoomWrapper;

    /**
     * The object used for rendering.
     */
    private final ChatSessionRenderer sessionRenderer;

    /**
     * The list of all <tt>ChatSessionChangeListener</tt>-s registered to listen
     * for transport modifications.
     */
    private final java.util.List<ChatSessionChangeListener>
        chatTransportChangeListeners
            = new Vector<ChatSessionChangeListener>();

    /**
     * Creates an instance of <tt>ConferenceChatSession</tt>, by specifying the
     * sessionRenderer to be used for communication with the UI and the chatRoom
     * corresponding to this conference session.
     * 
     * @param sessionRenderer the renderer to be used for communication with the
     * UI.
     * @param chatRoomWrapper the chat room corresponding to this conference
     * session.
     */
    public ConferenceChatSession(   ChatSessionRenderer sessionRenderer,
                                    ChatRoomWrapper chatRoomWrapper)
    {
        this.sessionRenderer = sessionRenderer;
        this.chatRoomWrapper = chatRoomWrapper;

        currentChatTransport
            = new ConferenceChatTransport(this, chatRoomWrapper.getChatRoom());

        chatTransports.add(currentChatTransport);

        this.initChatParticipants();

        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        chatRoom.addMemberPresenceListener(this);
        chatRoom.addPropertyChangeListener(this);
    }

    /**
     * Returns the descriptor of this chat session.
     * 
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor()
    {
        return chatRoomWrapper;
    }

    /**
     * Disposes this chat session.
     */
    public void dispose()
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        chatRoom.removeMemberPresenceListener(this);
        chatRoom.removePropertyChangeListener(this);
        chatRoom.leave();
    }

    /**
     * Returns the name of the chat room.
     * 
     * @return the name of the chat room.
     */
    public String getChatName()
    {
        return chatRoomWrapper.getChatRoomName();
    }

    /**
     * Returns the subject of the chat room.
     * 
     * @return the subject of the chat room.
     */
    public String getChatSubject()
    {
        return chatRoomWrapper.getChatRoom().getSubject();
    }

    /**
     * Returns the configuration form corresponding to the chat room.
     * 
     * @return the configuration form corresponding to the chat room.
     * @throws OperationFailedException if no configuration form is available
     * for the chat room.
     */
    public ChatRoomConfigurationForm getChatConfigurationForm()
        throws OperationFailedException
    {
        return chatRoomWrapper.getChatRoom().getConfigurationForm();
    }

    /**
     * Returns an iterator to the list of all participants contained in this 
     * chat session.
     * 
     * @return an iterator to the list of all participants contained in this 
     * chat session.
     */
    public Iterator<ChatContact> getParticipants()
    {
        return chatParticipants.iterator();
    }

    /**
     * Returns all available chat transports for this chat session.
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
    public ChatTransport getCurrentChatTransport()
    {
        return currentChatTransport;
    }

    /**
     * Returns the default mobile number used to send sms-es in this session. In
     * the case of conference this is for now null.
     * 
     * @return the default mobile number used to send sms-es in this session.
     */
    public String getDefaultSmsNumber()
    {
        return null;
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLast(
            chatHistoryFilter,
            chatRoomWrapper.getChatRoom(),
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param date The date up to which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryBeforeDate(Date date, int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLastMessagesBefore(
            chatHistoryFilter,
            chatRoomWrapper.getChatRoom(),
            date,
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param date The date from which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryAfterDate(Date date, int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findFirstMessagesAfter(
            chatHistoryFilter,
            chatRoomWrapper.getChatRoom(),
            date,
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns the start date of the history of this chat session.
     * 
     * @return the start date of the history of this chat session.
     */
    public long getHistoryStartDate()
    {
        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return 0;

        long startHistoryDate = 0;

        Collection<Object> firstMessage = metaHistory
            .findFirstMessagesAfter(
                chatHistoryFilter,
                chatRoomWrapper.getChatRoom(),
                new Date(0),
                1);

        if(firstMessage.size() > 0)
        {
            Iterator<Object> i = firstMessage.iterator();

            Object o = i.next();

            if(o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;

                startHistoryDate = evt.getTimestamp();
            }
            else if(o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                startHistoryDate = evt.getTimestamp();
            }
        }

        return startHistoryDate;
    }

    /**
     * Returns the end date of the history of this chat session.
     * 
     * @return the end date of the history of this chat session.
     */
    public long getHistoryEndDate()
    {
        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return 0;

        long endHistoryDate = 0;

        Collection<Object> lastMessage = metaHistory
            .findLastMessagesBefore(
                chatHistoryFilter,
                chatRoomWrapper.getChatRoom(),
                new Date(Long.MAX_VALUE), 1);

        if(lastMessage.size() > 0)
        {
            Iterator<Object> i1 = lastMessage.iterator();

            Object o1 = i1.next();

            if(o1 instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if(o1 instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o1;

                endHistoryDate = evt.getTimestamp();
            }
        }

        return endHistoryDate;
    }

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     * 
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.currentChatTransport = chatTransport;
        for (ChatSessionChangeListener l : chatTransportChangeListeners)
        {
            l.currentChatTransportChanged(this);
        }
    }

    /**
     * Sets the default mobile number used to send sms-es in this session.
     * 
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber) {}

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     * 
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return sessionRenderer;
    }

    /**
     * Invoked when <tt>ChatRoomMemberPresenceChangeEvent</tt> are received.
     * When a new <tt>ChatRoomMember</tt> has joined the chat adds it to the
     * list of chat participants on the right of the chat window. When a
     * <tt>ChatRoomMember</tt> has left or quit, or has being kicked it's
     * removed from the chat window.
     * @param evt the <tt>ChatRoomMemberPresenceChangeEvent</tt> that notified
     * us
     */
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if(!sourceChatRoom.equals(chatRoomWrapper.getChatRoom()))
            return;

        String eventType = evt.getEventType();
        ChatRoomMember chatRoomMember = evt.getChatRoomMember();

        String statusMessage = null;

        if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED))
        {
            ConferenceChatContact chatContact
                = new ConferenceChatContact(chatRoomMember);
            
            // Check if not ever present in the chat room. In some cases, the
            // considered chatroom member may appear twice in the chat contact
            // list panel.
            synchronized (chatParticipants)
            {
                if (!chatParticipants.contains(chatContact))
                    chatParticipants.add(chatContact);
                    sessionRenderer.addChatContact(chatContact);
            }
            
            /*
             * When the whole list of members of a given chat room is reported,
             * it doesn't make sense to see "ChatContact has joined #ChatRoom"
             * for all of them one after the other. Such an event occurs not
             * because the ChatContact has joined after us but rather she was
             * there before us.
             */
            if (!evt.isReasonUserList())
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_JOINED",
                    new String[] {sourceChatRoom.getName()});

                sessionRenderer.updateChatContactStatus(
                    chatContact,
                    statusMessage);
            }
        }
        else if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
        {
            if(eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_LEFT",
                    new String[] {sourceChatRoom.getName()});
            }
            else if(eventType.equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_KICKED",
                    new String[] {sourceChatRoom.getName()});
            }
            else if(eventType.equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_QUIT",
                    new String[] {sourceChatRoom.getName()});
            }

            for (ChatContact chatContact : chatParticipants)
            {
                if(chatContact.getDescriptor().equals(chatRoomMember))
                {
                    sessionRenderer.updateChatContactStatus(
                        chatContact, statusMessage);

                    sessionRenderer.removeChatContact(chatContact);
                    break;
                }
            }
        }
    }

    public void chatRoomPropertyChangeFailed(
        ChatRoomPropertyChangeFailedEvent event) {}

    /**
     * Updates the chat panel when a property of the chat room has been modified.
     * 
     * @param evt the event containing information about the property change
     */
    public void chatRoomPropertyChanged(ChatRoomPropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
            ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT))
        {
            sessionRenderer.setChatSubject((String) evt.getNewValue());
        }
    }

    /**
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        return true;
    }

    /**
     * Finds the <tt>ChatTransport</tt> corresponding to the given
     * <tt>descriptor</tt>.
     * @param descriptor the descriptor of the chat transport we're looking for
     * @return the <tt>ChatTransport</tt> corresponding to the given
     * <tt>descriptor</tt>
     */
    public ChatTransport findChatTransportForDescriptor(Object descriptor)
    {
        return MetaContactChatSession.findChatTransportForDescriptor(
            chatTransports,
            descriptor);
    }

    /**
     * Loads the given chat room in the this chat conference panel. Loads all
     * members and adds all corresponding listeners.
     * 
     * @param chatRoom the <tt>ChatRoom</tt> to load
     */
    public void loadChatRoom(ChatRoom chatRoom)
    {
        for (ChatRoomMember member : chatRoom.getMembers())
            sessionRenderer.addChatContact(new ConferenceChatContact(member));

        chatRoom.addPropertyChangeListener(this);
        chatRoom.addMemberPresenceListener(this);

        // Load the subject of the chat room.
        sessionRenderer.setChatSubject(chatRoom.getSubject());
    }

    /**
     * Implements the <tt>ChatPanel.getChatStatusIcon</tt> method.
     *
     * @return the status icon corresponding to this chat room
     */
    public ImageIcon getChatStatusIcon()
    {
        String status = Constants.OFFLINE_STATUS;

        if(chatRoomWrapper.getChatRoom() != null
            && chatRoomWrapper.getChatRoom().isJoined())
            status = Constants.ONLINE_STATUS;

        return new ImageIcon(Constants.getStatusIcon(status));
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public byte[] getChatAvatar()
    {
        return null;
    }

    /**
     * Initializes the list of participants.
     */
    private void initChatParticipants()
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if ((chatRoom != null) && chatRoom.isJoined())
            for (ChatRoomMember member : chatRoom.getMembers())
                chatParticipants.add(new ConferenceChatContact(member));
    }

    /**
     * Indicates if the contact list is supported by this session. The contact
     * list would be supported for all non system and non private sessions.
     * @return <tt>true</tt> to indicate that the contact list is supported,
     * <tt>false</tt> otherwise.
     */
    public boolean isContactListSupported()
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        return
            !chatRoom.isSystem()
                && !ConferenceChatManager.isPrivate(chatRoom);
    }

    /**
     * Adds the given <tt>ChatSessionChangeListener</tt> to the list of
     * transport listeners.
     * @param l the listener to add
     */
    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            if (!chatTransportChangeListeners.contains(l))
                chatTransportChangeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ChatSessionChangeListener</tt> from contained
     * transport listeners.
     * @param l the listener to remove
     */
    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            chatTransportChangeListeners.remove(l);
        }
    }

    /**
     * Adds the given <tt>ChatRoomMemberRoleListener</tt> to the contained
     * chat room role listeners.
     * @param l the listener to add
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener l)
    {
        chatRoomWrapper.getChatRoom().addMemberRoleListener(l);
    }

    /**
     * Adds the given <tt>ChatRoomLocalUserRoleListener</tt> to the contained
     * chat room role listeners.
     * @param l the listener to add
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener l)
    {
        chatRoomWrapper.getChatRoom().addLocalUserRoleListener(l);
    }
}
