/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ConferenceChatPanel</tt> is the chat panel corresponding to a
 * multi user chat.
 *
 * @author Yana Stamcheva
 */
public class ConferenceChatPanel
    extends ChatPanel
    implements  ChatRoomMessageListener,
                ChatRoomPropertyChangeListener,
                ChatRoomLocalUserStatusListener,
                ChatRoomMemberListener
{
    private Logger logger = Logger.getLogger(ConferenceChatPanel.class);

    private ChatRoom chatRoom;

    private ChatWindowManager chatWindowManager;

    /**
     * Creates an instance of <tt>ConferenceChatPanel</tt>.
     *
     * @param chatWindow the <tt>ChatWindow</tt> that contains this chat panel
     * @param chatRoom the <tt>ChatRoom</tt> object, which provides us the multi
     * user chat functionality
     */
    public ConferenceChatPanel(ChatWindow chatWindow, ChatRoom chatRoom)
    {
        super(chatWindow);

        this.chatWindowManager = chatWindow.getMainFrame().getChatWindowManager();

        this.chatRoom = chatRoom;

        List membersList = chatRoom.getMembers();

        for (int i = 0; i < membersList.size(); i ++)
        {
            ChatContact chatContact
                = new ChatContact((ChatRoomMember)membersList.get(i));
            
            getChatContactListPanel()
                .addContact(chatContact);
        }

        this.chatRoom.addMessageListener(this);
        this.chatRoom.addChatRoomPropertyChangeListener(this);
        this.chatRoom.addLocalUserStatusListener(this);
        this.chatRoom.addMemberListener(this);
    }

    /**
     * Implements the <tt>ChatPanel.getChatName</tt> method.
     *
     * @return the name of the chat room.
     */
    public String getChatName()
    {
        return chatRoom.getName();
    }

    /**
     * Implements the <tt>ChatPanel.getChatIdentifier</tt> method.
     *
     * @return the <tt>ChatRoom</tt>
     */
    public Object getChatIdentifier()
    {
        return chatRoom;
    }

    /**
     * Implements the <tt>ChatPanel.getChatStatus</tt> method.
     *
     * @return the status of this chat room
     */
    public PresenceStatus getChatStatus()
    {
        return null;
    }

    /**
     * Implements the <tt>ChatPanel.loadHistory</tt> method.
     * <br>
     * Loads the history for this chat room.
     */
    public void loadHistory()
    {

    }

    /**
     * Implements the <tt>ChatPanel.loadHistory(escapedMessageID)</tt> method.
     * <br>
     * Loads the history for this chat room and escapes the last message
     * received.
     */
    public void loadHistory(String escapedMessageID)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Implements the <tt>ChatPanel.loadPreviousFromHistory</tt> method.
     * <br>
     * Loads the previous "page" in the history.
     */
    public void loadPreviousFromHistory()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Implements the <tt>ChatPanel.loadNextFromHistory</tt> method.
     * <br>
     * Loads the next "page" in the history.
     */
    public void loadNextFromHistory()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Implements the <tt>ChatPanel.sendMessage</tt> method.
     * <br>
     * Sends a message to the chat room.
     */
    protected void sendMessage()

    {    
        String body = this.getTextFromWriteArea();
        Message msg = chatRoom.createMessage(body);
        
        try
        {   
            chatRoom.sendMessage(msg);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send message.", ex);
            
            this.refreshWriteArea();
    
            this.processMessage(
                    chatRoom.getName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    msg.getContent());
    
            this.processMessage(
                    chatRoom.getName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("msgDeliveryInternalError")
                        .getText());
        }
    }

    /**
     * Implements the <tt>ChatPanel.treatReceivedMessage</tt> method.
     * <br>
     * Treats a received message from the given contact.
     */
    public void treatReceivedMessage(Contact sourceContact)
    {
    }

    /**
     * Implements the <tt>ChatPanel.sendTypingNotification</tt> method.
     * <br>
     * Sends a typing notification.
     */
    public int sendTypingNotification(int typingState)
    {
        return 0;
    }

    /**
     * Implements the <tt>ChatPanel.getFirstHistoryMsgTimestamp</tt> method.
     */
    public Date getFirstHistoryMsgTimestamp()
    {
        return null;
    }

    /**
     * Implements the <tt>ChatPanel.getLastHistoryMsgTimestamp</tt> method.
     */
    public Date getLastHistoryMsgTimestamp()
    {
        return null;
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageReceived</tt> method.
     * <br>
     * Obtains the corresponding <tt>ChatPanel</tt> and proccess the message
     * there.
     */
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if(!sourceChatRoom.equals(chatRoom))
            return;

        ChatRoomMember sourceMember = evt.getSourceChatRoomMember();
        
        logger.trace("MESSAGE RECEIVED from contact: "
            + sourceMember.getContactAddress());
        
        Date date = evt.getTimestamp();
        Message message = evt.getMessage();

        ChatRoom chatRoom = (ChatRoom) evt.getSource();

        ChatPanel chatPanel = chatWindowManager.getChatRoom(chatRoom);

        chatPanel.processMessage(
                sourceMember.getName(), date,
                Constants.INCOMING_MESSAGE, message.getContent());

        chatWindowManager.openChat(chatPanel, false);

        GuiActivator.getAudioNotifier()
            .createAudio(Sounds.INCOMING_MESSAGE).play();
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * Shows the message in the conversation area and clears the write message
     * area.
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if(!sourceChatRoom.equals(chatRoom))
            return;

        ChatRoomMember destMember = evt.getDestinationChatRoomMember();
        
        logger.trace("MESSAGE DELIVERED to contact: "
            + destMember.getContactAddress());

        Message msg = evt.getMessage();

        ChatPanel chatPanel = null;

        if(chatWindowManager.isChatOpenedForChatRoom(sourceChatRoom))
            chatPanel = chatWindowManager.getChatRoom(sourceChatRoom);

        if (chatPanel != null)
        {
            ProtocolProviderService protocolProvider
                = destMember.getProtocolProvider();

            logger.trace("MESSAGE DELIVERED: process message to chat for contact: "
                    + destMember.getContactAddress());

            chatPanel.processMessage(getChatWindow().getMainFrame()
                    .getAccount(protocolProvider), evt.getTimestamp(),
                    Constants.OUTGOING_MESSAGE, msg.getContent());

            chatPanel.refreshWriteArea();
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area show an error message, explaining the problem.
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if(!sourceChatRoom.equals(chatRoom))
            return;

        String errorMsg = null;

        Message sourceMessage = (Message) evt.getSource();

        ChatRoomMember destMember = evt.getDestinationChatRoomMember();

        if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED) {

            errorMsg = Messages.getI18NString(
                    "msgDeliveryOfflineNotSupported").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE) {

            errorMsg = Messages.getI18NString("msgNotDelivered").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED) {

            errorMsg = Messages.getI18NString(
                    "msgSendConnectionProblem").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR) {

            errorMsg = Messages.getI18NString(
                    "msgDeliveryInternalError").getText();
        }
        else {
            errorMsg = Messages.getI18NString(
                    "msgDeliveryFailedUnknownError").getText();
        }

        ChatPanel chatPanel = chatWindowManager
            .getChatRoom(chatRoom);

        chatPanel.refreshWriteArea();

        chatPanel.processMessage(
                destMember.getName(),
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                sourceMessage.getContent());

        chatPanel.processMessage(
                destMember.getName(),
                new Date(System.currentTimeMillis()),
                Constants.ERROR_MESSAGE,
                errorMsg);

        chatWindowManager.openChat(chatPanel, false);
    }

    public void chatRoomChanged(ChatRoomPropertyChangeEvent event)
    {   
    }

    public void localUserStatusChanged(ChatRoomLocalUserStatusChangeEvent evt)
    {   
    }

    public void memberStatusChanged(ChatRoomMemberEvent evt)
    {   
    }
}
