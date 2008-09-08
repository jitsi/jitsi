/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
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
                LocalUserChatRoomPresenceListener,
                ServiceListener
{
    private Logger logger = Logger.getLogger(MultiUserChatManager.class);

    private MainFrame mainFrame;

    private ChatWindowManager chatWindowManager;

    private Hashtable chatRoomHistory = new Hashtable();

    private ChatRoomsList chatRoomList;

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

        this.chatRoomList = new ChatRoomsList(mainFrame);

        this.initChatRoomsList();

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Returns the JList containing the list of chat rooms.
     * 
     * @return the JList containing the list of chat rooms.
     */
    public ChatRoomsList getChatRoomList()
    {
        return chatRoomList;
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

        ConferenceChatPanel chatPanel = null;
        
        if(chatWindowManager.isChatOpenedForChatRoom(sourceChatRoom))
        {
            chatPanel = chatWindowManager.getMultiChat(sourceChatRoom);
        }

        String messageType = null;

        if (evt.getEventType()
            == ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED)
        {
            messageType = Constants.OUTGOING_MESSAGE;
        }
        else if (evt.getEventType()
            == ChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED)
        {
            messageType = Constants.ACTION_MESSAGE;
        }

        if(chatPanel != null)
        {
            chatPanel.processMessage(sourceChatRoom.getParentProvider()
                .getAccountID().getUserID(),
                evt.getTimestamp(),
                messageType,
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

        String messageType = null;

        if (evt.getEventType()
            == ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
        {
            messageType = Constants.INCOMING_MESSAGE;
        }
        else if (evt.getEventType()
            == ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED)
        {
            messageType = Constants.SYSTEM_MESSAGE;
        }
        else if (evt.getEventType()
            == ChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED)
        {
            messageType = Constants.ACTION_MESSAGE;
        }

        logger.trace("MESSAGE RECEIVED from contact: "
            + sourceMember.getContactAddress());

        Date date = evt.getTimestamp();
        Message message = evt.getMessage();

        ConferenceChatPanel chatPanel = null;

        if(sourceChatRoom.isSystem())
        {
            MultiUserChatServerWrapper serverWrapper
                = chatRoomList.findServerWrapperFromProvider(
                    sourceChatRoom.getParentProvider());

            chatPanel
                = chatWindowManager.getMultiChat(
                    serverWrapper.getSystemRoomWrapper());
        }
        else
        {
            chatPanel = chatWindowManager
                .getMultiChat(sourceChatRoom, message.getMessageUID());
        }

        chatPanel.processMessage(
            sourceMember.getName(), date,
            messageType,
            message.getContent(),
            message.getContentType());
        
        chatWindowManager.openChat(chatPanel, false);
        
        // Fire notification
        String title = Messages.getI18NString("msgReceived",
            new String[]{sourceMember.getName()}).getText();

        NotificationManager.fireChatNotification(
            sourceChatRoom,
            NotificationManager.INCOMING_MESSAGE,
            title,
            message.getContent());
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

        ConferenceChatPanel chatPanel
            = chatWindowManager.getMultiChat(sourceChatRoom);

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

        ChatRoomWrapper chatRoomWrapper = chatRoomList
            .findChatRoomWrapperFromChatRoom(sourceChatRoom);

        if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED))
        {
            if(chatRoomWrapper != null)
            {
                chatRoomList.refresh();

                ConferenceChatPanel chatPanel
                    = (ConferenceChatPanel) chatWindowManager
                        .getMultiChat(chatRoomWrapper);

                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if(chatWindowManager
                    .isChatOpenedForChatRoom(chatRoomWrapper))
                {
                    chatPanel.loadChatRoom(sourceChatRoom);
                }
                else
                {
                    chatWindowManager.openChat(chatPanel, true);
                }

                chatPanel.updateChatRoomStatus(Constants.ONLINE_STATUS);
            }

            if (sourceChatRoom.isSystem())
            {
                MultiUserChatServerWrapper serverWrapper
                    = chatRoomList.findServerWrapperFromProvider(
                        sourceChatRoom.getParentProvider());

                serverWrapper.setSystemRoom(sourceChatRoom);
            }

            sourceChatRoom.addMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED))
        {
            new ErrorDialog(mainFrame,
                Messages.getI18NString("error").getText(),
                Messages.getI18NString("failedToJoinChatRoom",
                    new String[]{sourceChatRoom.getName()})
                        .getText() + evt.getReason())
                    .showDialog();
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            chatRoomList.refresh();

            sourceChatRoom.removeMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            chatRoomList.refresh();

            sourceChatRoom.removeMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            chatRoomList.refresh();

            sourceChatRoom.removeMessageListener(this);
        }
    }

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     * 
     * @param invitation the invitation to accept.
     */
    public void acceptInvitation(ChatRoomInvitation invitation)
    {
        ChatRoom chatRoom = invitation.getTargetChatRoom();
        byte[] password = invitation.getChatRoomPassword();

        chatRoomList.addChatRoom(new ChatRoomWrapper(chatRoom));

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
                Messages.getI18NString("error").getText(),
                Messages.getI18NString("failedToJoinChatRoom",
                    new String[] {chatRoom.getName()}).getText())
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
     * @param nickname the nickname we choose for the given chat room
     * @param password the password
     */
    public void joinChatRoom(   ChatRoom chatRoom,
                                String nickname,
                                byte[] password)
    {
        try
        {
            if(password != null && password.length > 0)
                chatRoom.joinAs(nickname, password);
            else
                chatRoom.joinAs(nickname);

            ChatRoomWrapper chatRoomWrapper
                = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if(chatRoomWrapper == null)
            {
                chatRoomWrapper = new ChatRoomWrapper(chatRoom);

                chatRoomList.addChatRoom(chatRoomWrapper);
            }

            // We save the choice of the user, before the chat room is really
            // joined, because even the join fails we want the next time when
            // we login to join this chat room automatically.
            ConfigurationManager.updateChatRoomStatus(
                chatRoomWrapper.getParentProvider(),
                chatRoomWrapper.getChatRoomID(),
                Constants.ONLINE_STATUS);
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
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomRegistrationRequired",
                        new String[]{chatRoom.getName()})
                            .getText())
                        .showDialog();
            }
            else if(e.getErrorCode()
                == OperationFailedException.PROVIDER_NOT_REGISTERED)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomNotConnected",
                        new String[]{chatRoom.getName()})
                            .getText())
                        .showDialog();
            }
            else if(e.getErrorCode()
                    == OperationFailedException
                        .SUBSCRIPTION_ALREADY_EXISTS)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomAlreadyJoined",
                        new String[]{chatRoom.getName()})
                            .getText())
                        .showDialog();
            }            
            else
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("failedToJoinChatRoom",
                        new String[]{chatRoom.getName()})
                            .getText())
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

            ChatRoomWrapper chatRoomWrapper
                = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if(chatRoomWrapper == null)
            {
                chatRoomWrapper = new ChatRoomWrapper(chatRoom);

                chatRoomList.addChatRoom(chatRoomWrapper);
            }

            // We save the choice of the user, before the chat room is really
            // joined, because even the join fails we want the next time when
            // we login to join this chat room automatically.
            ConfigurationManager.updateChatRoomStatus(
                chatRoomWrapper.getParentProvider(),
                chatRoomWrapper.getChatRoomID(),
                Constants.ONLINE_STATUS);
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
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomRegistrationRequired",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e).showDialog();
            }
            else if(e.getErrorCode()
                == OperationFailedException.PROVIDER_NOT_REGISTERED)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomNotConnected",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e).showDialog();
            }            
            else if(e.getErrorCode()
                    == OperationFailedException
                        .SUBSCRIPTION_ALREADY_EXISTS)
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("chatRoomAlreadyJoined",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e).showDialog();
            }
            else
            {
                new ErrorDialog(mainFrame,
                    Messages.getI18NString("error").getText(),
                    Messages.getI18NString("failedToJoinChatRoom",
                        new String[]{chatRoom.getName()})
                            .getText(),
                    e).showDialog();
            }

            logger.error("Failed to join chat room: "
                + chatRoom.getName(), e);
        }
    }

    /**
     * Leaves the given <tt>ChatRoom</tt>.
     * 
     * @param chatRoom the <tt>ChatRoom</tt> to leave.
     */
    public void leaveChatRoom(ChatRoom chatRoom)
    {
        chatRoom.leave();

        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(chatRoomWrapper == null)
            return;

        // We save the choice of the user, before the chat room is really
        // joined, because even the join fails we want the next time when
        // we login to join this chat room automatically.
        ConfigurationManager.updateChatRoomStatus(
            chatRoomWrapper.getParentProvider(),
            chatRoomWrapper.getChatRoomID(),
            Constants.OFFLINE_STATUS);
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

    /**
     * Checks if there's an open history window for the given chat room.
     * 
     * @param chatRoomWrapper the chat room wrapper to check for
     * @return TRUE if there's an opened history window for the given chat room,
     *         FALSE otherwise.
     */
    public boolean containsHistoryWindowForChatRoom(
        ChatRoomWrapper chatRoomWrapper)
    {
        return chatRoomHistory.containsKey(chatRoomWrapper);
    }

    /**
     * Returns the history window for the given chat room.
     * 
     * @param chatRoomWrapper the chat room wrapper to search for
     * @return the history window for the given chat room
     */
    public HistoryWindow getHistoryWindowForChatRoom(
        ChatRoomWrapper chatRoomWrapper)
    {
        return (HistoryWindow) chatRoomHistory.get(chatRoomWrapper);
    }

    /**
     * Adds a history window for a given chat room in the table of opened
     * history windows.
     * 
     * @param chatRoomWrapper the chat room wrapper to add
     * @param historyWindow the history window to add
     */
    public void addHistoryWindowForChatRoom(ChatRoomWrapper chatRoomWrapper,
        HistoryWindow historyWindow)
    {
        chatRoomHistory.put(chatRoomWrapper, historyWindow);
    }

    /**
     * Removes the history window for the given chat room.
     * 
     * @param chatRoomWrapper the chat room wrapper to remove the history window
     */
    public void removeHistoryWindowForChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        chatRoomHistory.remove(chatRoomWrapper);
    }

    /**
     * Closes the chat corresponding to the given chat room wrapper, if such
     * exists.
     * 
     * @param chatRoomWrapper the chat room wrapper for which we search a chat
     * to close.
     */
    private void closeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        final ChatWindowManager chatWindowManager
            = mainFrame.getChatWindowManager();

        if(chatWindowManager.isChatOpenedForChatRoom(chatRoomWrapper))
        {
            final ChatPanel chatPanel
                = chatWindowManager.getMultiChat(chatRoomWrapper);

            // We have to be sure that we close the chat in the swing thread
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    chatWindowManager.closeChat(chatPanel);
                }
            });
        }
    }

    /**
     * Initializes the list of chat rooms.
     */
    private void initChatRoomsList()
    {
        try
        {
            ServiceReference[] serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                                        ProtocolProviderService.class.getName(),
                                        null);

            // If we don't have providers at this stage we just return.
            if (serRefs == null)
                return;

            for (int i = 0; i < serRefs.length; i ++)
            {
                ServiceReference protocolProviderRef = serRefs[i];

                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService) GuiActivator
                        .bundleContext.getService(protocolProviderRef);

                this.addChatRoomProvider(protocolProvider);
            }
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Failed to obtain service references.", e);
        }
    }

    /**
     * Handles <tt>ServiceEvent</tt>s triggered by adding or removing a
     * ProtocolProviderService. Updates the list of available chat rooms and
     * chat room servers.
     * 
     * @param event The event to handle.
     */
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(event
            .getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            this.addChatRoomProvider((ProtocolProviderService) service);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.removeChatRoomProvider((ProtocolProviderService) service);
        }

    }
    
    /**
     * Adds the given provider to the list of chat room providers.
     * 
     * @param protocolProvider The provider to add.
     */
    private void addChatRoomProvider(ProtocolProviderService protocolProvider)
    {
        Object multiUserChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        if (multiUserChatOpSet != null)
        {
            chatRoomList.addChatServer(protocolProvider,
                    (OperationSetMultiUserChat) multiUserChatOpSet);
        }
    }

    /**
     * Removes the given provider from the list of chat room providers.
     * 
     * @param protocolProvider The provider to remove.
     */
    private void removeChatRoomProvider(
        ProtocolProviderService protocolProvider)
    {
        Object multiUserChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        if (multiUserChatOpSet != null)
        {
            // Remove all related chat rooms.
            chatRoomList.removeChatServer(protocolProvider);
        }
    }
}
