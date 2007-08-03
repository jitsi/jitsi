/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MultiUserChatManager</tt> is the one that manages chat room
 * invitations.
 * 
 * @author Yana Stamcheva
 */
public class MultiUserChatManager
    implements  ChatRoomMessageListener,                
                ChatRoomInvitationListener,
                ChatRoomInvitationRejectionListener,
                LocalUserChatRoomPresenceListener
{
    private Logger logger = Logger.getLogger(MultiUserChatManager.class);
    
    private MainFrame mainFrame;
    
    private ChatWindowManager chatWindowManager;
    
    /**
     * Creates an instance of <tt>MultiUserChatManager</tt>, by passing to it
     * the main application window object.
     * 
     * @param mainFrame the main application window
     */
    public MultiUserChatManager(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
        
        this.chatWindowManager = mainFrame.getChatWindowManager();
    }
    
    public void invitationReceived(ChatRoomInvitationReceivedEvent evt)
    {
        OperationSetMultiUserChat multiUserChatOpSet
            = evt.getSourceOperationSet();

        InvitationReceivedDialog dialog = new InvitationReceivedDialog(
            this, multiUserChatOpSet, evt.getInvitation());
        
        dialog.setVisible(true);
    }
        
    public void invitationRejected(ChatRoomInvitationRejectedEvent evt)
    {   
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
        
        logger.trace("MESSAGE DELIVERED to chat room: "
            + sourceChatRoom.getName());

        Message msg = evt.getMessage();

        ChatPanel chatPanel = null;
        
        if(chatWindowManager.isChatOpenedForChatRoom(sourceChatRoom))
        {
            chatPanel = chatWindowManager.getMultiChat(sourceChatRoom);
        }
        
        if(chatPanel != null)
        {
            chatPanel.processMessage(sourceChatRoom.getParentProvider()
                .getAccountID().getUserID(),
                evt.getTimestamp(),
                Constants.OUTGOING_MESSAGE,
                msg.getContent(),
                msg.getContentType());
        }
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
        
        ChatRoomMember sourceMember = evt.getSourceChatRoomMember();
        
        logger.info("MESSAGE RECEIVED from contact: "
            + sourceMember.getContactAddress());
        
        Date date = evt.getTimestamp();
        Message message = evt.getMessage();
        
        ChatPanel chatPanel = chatWindowManager.getMultiChat(sourceChatRoom);
        
        chatPanel.processMessage(
            sourceMember.getName(), date,
            Constants.INCOMING_MESSAGE,
            message.getContent(),
            message.getContentType());
        
        chatWindowManager.openChat(chatPanel, false);

        GuiActivator.getAudioNotifier()
            .createAudio(Sounds.INCOMING_MESSAGE).play();
    }    

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area shows an error message, explaining the problem.
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

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

        ChatPanel chatPanel = chatWindowManager.getMultiChat(sourceChatRoom);

        chatPanel.processMessage(
                destMember.getName(),
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());

        chatPanel.processMessage(
                destMember.getName(),
                new Date(System.currentTimeMillis()),
                Constants.ERROR_MESSAGE,
                errorMsg, "text");

        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Implements the
     * <tt>LocalUserChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method.
     */
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();
        
        if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED))
        {
            ChatRoomsList chatRoomsList
                = mainFrame.getChatRoomsListPanel().getChatRoomsList();
            
            ChatRoomWrapper chatRoomWrapper = chatRoomsList
                .findChatRoomWrapperFromChatRoom(sourceChatRoom);
            
            if(chatRoomWrapper != null)
            {
                chatRoomsList.refresh();
                
                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if(chatWindowManager
                    .isChatOpenedForChatRoom(chatRoomWrapper))
                {
                    ConferenceChatPanel chatPanel
                        = (ConferenceChatPanel) chatWindowManager
                            .getMultiChat(chatRoomWrapper);
                    
                    chatPanel.loadChatRoom(sourceChatRoom);
                }
            }
            else
            {
                chatRoomsList.addChatRoom(new ChatRoomWrapper(sourceChatRoom));
            }
            
            sourceChatRoom.addMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED))
        {
            
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT))
        {
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED))
        {
            
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED))
        {
            
        }
    }
    
    public void acceptInvitation(ChatRoomInvitation invitation)
    {
        ChatRoom chatRoom = invitation.getTargetChatRoom();
        byte[] password = invitation.getChatRoomPassword();
        
        try
        {
            if(password == null)
                chatRoom.join();
            else
                chatRoom.join(password);
        }
        catch (OperationFailedException e)
        {
            new ErrorDialog(mainFrame,
                Messages.getI18NString("failedToJoinChatRoom",
                    new String[] {chatRoom.getName()}).getText(),
                Messages.getI18NString("error").getText())
                    .showDialog();
            
            logger.error("Failed to join chat room: "
                + chatRoom.getName(), e);
        }
    }

    /**
     * Rejects the given invitation with the specified reason.
     * 
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(OperationSetMultiUserChat multiUserChatOpSet,
        ChatRoomInvitation invitation, String reason)
    {
        multiUserChatOpSet.rejectInvitation(invitation, reason);
    }
 
    /**
     * Joins the given chat room with the given password and manages all the
     * exceptions that could occur during the join process.
     * 
     * @param chatRoom the chat room to join
     * @param password the password
     */
    public void joinChatRoom(ChatRoom chatRoom, String nickname, byte[] password)
    {
        try
        {
            if(password != null && password.length > 0)
                chatRoom.joinAs(nickname, password);
            else
                chatRoom.joinAs(nickname);
        }
        catch (OperationFailedException e)
        {
            if(e.getErrorCode()
                == OperationFailedException
                    .AUTHENTICATION_FAILED)
            {                
                ChatRoomAuthenticationWindow authWindow
                    = new ChatRoomAuthenticationWindow(mainFrame, chatRoom);
                
                authWindow.setVisible(true);                
            }
            else if(e.getErrorCode()
                == OperationFailedException
                    .REGISTRATION_REQUIRED)
            {                
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomRegistrationRequired",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }
            else if(e.getErrorCode()
                == OperationFailedException.PROVIDER_NOT_REGISTERED)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomNotConnected",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }
            else if(e.getErrorCode()
                    == OperationFailedException
                        .SUBSCRIPTION_ALREADY_EXISTS)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomAlreadyJoined",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }            
            else
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("failedToJoinChatRoom",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }
            
            logger.error("Failed to join chat room: "
                + chatRoom.getName(), e);
        }
    }
    
    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     * 
     * @param chatRoom the chat room to join
     */
    public void joinChatRoom(ChatRoom chatRoom)
    {
        try
        {
            chatRoom.join();                        
        }
        catch (OperationFailedException e)
        {
            if(e.getErrorCode()
                == OperationFailedException
                    .AUTHENTICATION_FAILED)
            {
                ChatRoomAuthenticationWindow authWindow
                    = new ChatRoomAuthenticationWindow(mainFrame, chatRoom);
                
                authWindow.setVisible(true);
            }
            else if(e.getErrorCode()
                == OperationFailedException
                    .REGISTRATION_REQUIRED)
            {                
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomRegistrationRequired",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e,
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }
            else if(e.getErrorCode()
                == OperationFailedException.PROVIDER_NOT_REGISTERED)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomNotConnected",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e,
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }            
            else if(e.getErrorCode()
                    == OperationFailedException
                        .SUBSCRIPTION_ALREADY_EXISTS)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("chatRoomAlreadyJoined",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e,
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }            
            else
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("failedToJoinChatRoom",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e,
                    Messages.getI18NString("error").getText())
                        .showDialog();
            }
            
            logger.error("Failed to join chat room: "
                + chatRoom.getName(), e);
        }
    }
    
    /**
     * Returns the main application frame.
     * 
     * @return the main application frame
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }
}
