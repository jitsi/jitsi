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
 * An implementation of <tt>ChatSession</tt> for ad-hoc conference chatting.
 * 
 * @author Valentin Martinet
 */
public class AdHocConferenceChatSession
    implements  ChatSession,
                AdHocChatRoomParticipantPresenceListener
{
    private final List<ChatContact> chatParticipants
        = new ArrayList<ChatContact>();

    private final List<ChatTransport> chatTransports
        = new ArrayList<ChatTransport>();

    private ChatTransport currentChatTransport;

    private final AdHocChatRoomWrapper chatRoomWrapper;

    private final ChatSessionRenderer sessionRenderer;

    /**
     * Creates an instance of <tt>AdHocConferenceChatSession</tt>, by specifying
     * the sessionRenderer to be used for communication with the UI and the 
     * ad-hoc chat room corresponding to this conference session.
     * 
     * @param sessionRenderer the renderer to be used for communication with the
     * UI.
     * @param chatRoomWrapper the ad-hoc chat room corresponding to this 
     * conference session.
     */
    public AdHocConferenceChatSession(  ChatSessionRenderer  sessionRenderer,
                                        AdHocChatRoomWrapper chatRoomWrapper)
    {
        this.sessionRenderer = sessionRenderer;
        this.chatRoomWrapper = chatRoomWrapper;

        this.currentChatTransport = new AdHocConferenceChatTransport(
                    this, chatRoomWrapper.getAdHocChatRoom());

        chatTransports.add(currentChatTransport);

        this.initChatParticipants();

        AdHocChatRoom chatRoom = chatRoomWrapper.getAdHocChatRoom();
        chatRoom.addParticipantPresenceListener(this);
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
        AdHocChatRoom chatRoom = chatRoomWrapper.getAdHocChatRoom();
        chatRoom.removeParticipantPresenceListener(this);
    }

    /**
     * Returns the name of the ad-hoc chat room.
     * 
     * @return the name of the ad-hoc chat room.
     */
    public String getChatName()
    {
        return chatRoomWrapper.getAdHocChatRoomName();
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
        return null;
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
            chatRoomWrapper.getAdHocChatRoom(),
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
            chatRoomWrapper.getAdHocChatRoom(),
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
            chatRoomWrapper.getAdHocChatRoom(),
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
                chatRoomWrapper.getAdHocChatRoom(),
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
                chatRoomWrapper.getAdHocChatRoom(),
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
    }

    /**
     * Sets the default mobile number used to send sms-es in this session.
     * 
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber)
    {}

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
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        return false;
    }

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
    public void loadChatRoom(AdHocChatRoom chatRoom)
    {
        for (Contact contact : chatRoom.getParticipants())
        {
            sessionRenderer.addChatContact(
                    new AdHocConferenceChatContact(contact));
            
        }
       
        chatRoom.addParticipantPresenceListener(this);

    }

    /**
     * Implements the <tt>ChatPanel.getChatStatusIcon</tt> method.
     *
     * @return the status icon corresponding to this ad-hoc chat room
     */
    public ImageIcon getChatStatusIcon()
    {
        String status = Constants.OFFLINE_STATUS;

        if(chatRoomWrapper.getAdHocChatRoom() != null)
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
        AdHocChatRoom chatRoom = chatRoomWrapper.getAdHocChatRoom();

        if (chatRoom != null)
        {
            for (Contact contact : chatRoom.getParticipants())
            {
                chatParticipants.add(new AdHocConferenceChatContact(contact));
            }
        }
    }

    /* Implements ChatSession#isContactListSupported(). */
    public boolean isContactListSupported()
    {
        return true;
    }

    /**
     * Invoked when <tt>AdHocChatRoomParticipantPresenceChangeEvent</tt> are 
     * received. When a new participant (<tt>Contact</tt>) has joined the chat 
     * adds it to the list of chat participants on the right of the chat window.
     * When a participant has left or quit it's removed from the chat window.
     */
    public void participantPresenceChanged(
            AdHocChatRoomParticipantPresenceChangeEvent evt) {
        AdHocChatRoom sourceChatRoom = evt.getAdHocChatRoom();

        if(!sourceChatRoom.equals(chatRoomWrapper.getAdHocChatRoom()))
            return;

        String eventType = evt.getEventType();
        Contact participant = evt.getParticipant();

        String statusMessage = null;

        if (eventType.equals(
                AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED))
        {
            AdHocConferenceChatContact chatContact
                = new AdHocConferenceChatContact(participant);

            chatParticipants.add(chatContact);

            sessionRenderer.addChatContact(chatContact);

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
        else if (eventType.equals(
                AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT)
            || 
                eventType.equals(
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_QUIT))
        {
            if(eventType.equals(
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_LEFT",
                    new String[] {sourceChatRoom.getName()});
            }
            else if(eventType.equals(
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_QUIT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_QUIT",
                    new String[] {sourceChatRoom.getName()});
            }

            for (ChatContact chatContact : chatParticipants)
            {
                if(chatContact.getDescriptor().equals(participant))
                {
                    sessionRenderer.updateChatContactStatus(
                        chatContact, statusMessage);

                    sessionRenderer.removeChatContact(chatContact);
                    break;
                }
            }
        }
    }

    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
    }

    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
    }
}
