/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.protocol.*;

public class ConfigurationManager
{
    public static final String ENTER_COMMAND = "Enter";
    
    public static final String CTRL_ENTER_COMMAND = "Ctrl-Enter";
    
    /**
     * Indicates whether the message automatic popup is enabled.
     */
    private static boolean autoPopupNewMessage = false;
    
    private static String sendMessageCommand;
    
    private static boolean isCallPanelShown = true;
    
    private static boolean isShowOffline = true;
    
    private static boolean isApplicationVisible = true;
    
    private static boolean isQuitWarningShown = true;
    
    private static boolean isSendTypingNotifications;
    
    private static boolean isMoveContactConfirmationRequested = true;
    
    private static boolean isMultiChatWindowEnabled;
    
    private static boolean isHistoryLoggingEnabled;
    
    private static boolean isHistoryShown;
    
    private static int chatHistorySize;
    
    private static ConfigurationService configService
        = GuiActivator.getConfigurationService();
    
    private static String lastContactParent = null;

    /**
     * 
     */
    public static void loadGuiConfigurations()
    {
        configService.addPropertyChangeListener(
            new ConfigurationChangeListener());
        
        // Load the "auPopupNewMessage" property.
        String autoPopup = configService.getString(
            "net.java.sip.communicator.impl.gui.autoPopupNewMessage");

        if(autoPopup != null && autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;

        // Load the "sendMessageCommand" property.
        String messageCommand = configService.getString(
            "net.java.sip.communicator.impl.gui.sendMessageCommand");

        if(messageCommand == null || messageCommand != "")
            sendMessageCommand = messageCommand;

        // Load the showCallPanel property.
        String callPanelShown = configService.getString(
            "net.java.sip.communicator.impl.gui.showCallPanel");

        if(callPanelShown != null && callPanelShown != "")
        {
            isCallPanelShown = new Boolean(callPanelShown).booleanValue();
        }

        // Load the "showOffline" property.
        String showOffline = configService.getString(
            "net.java.sip.communicator.impl.gui.showOffline");
        
        if(showOffline != null && showOffline != "")
        {
            isShowOffline = new Boolean(showOffline).booleanValue();
        }

        // Load the "showApplication" property.
        String isVisible = configService.getString(
            "net.java.sip.communicator.impl.systray.showApplication");

        if(isVisible != null && isVisible != "")
        {
            isApplicationVisible = new Boolean(isVisible).booleanValue();
        }

        // Load the "showAppQuitWarning" property.
        String quitWarningShown = configService.getString(
            "net.java.sip.communicator.impl.gui.quitWarningShown");

        if(quitWarningShown != null && quitWarningShown != "")
        {
            isQuitWarningShown
                = new Boolean(quitWarningShown).booleanValue();
        }

        // Load the "sendTypingNotifications" property.
        String isSendTypingNotif = configService.getString(
            "net.java.sip.communicator.impl.gui.sendTypingNotifications");
        
        if(isSendTypingNotif != null && isSendTypingNotif != "")
        {
            isSendTypingNotifications
                = new Boolean(isSendTypingNotif).booleanValue();
        }
        
        // Load the "isMoveContactConfirmationRequested" property.
        String isMoveContactConfirmationRequestedString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested");

        if(isMoveContactConfirmationRequestedString != null
            && isMoveContactConfirmationRequestedString != "")
        {
            isMoveContactConfirmationRequested
                = new Boolean(isMoveContactConfirmationRequestedString)
                .booleanValue();
        }

        // Load the "isMultiChatWindowEnabled" property.
        String isMultiChatWindowEnabledString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isMultiChatWindowEnabled");

        if(isMultiChatWindowEnabledString != null
            && isMultiChatWindowEnabledString != "")
        {
            isMultiChatWindowEnabled
                = new Boolean(isMultiChatWindowEnabledString)
                .booleanValue();
        }
        
        // Load the "isHistoryLoggingEnabled" property.
        String isHistoryLoggingEnabledString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isHistoryLoggingEnabled");

        if(isHistoryLoggingEnabledString != null
            && isHistoryLoggingEnabledString != "")
        {
            isHistoryLoggingEnabled
                = new Boolean(isHistoryLoggingEnabledString)
                .booleanValue();
        }
        
        // Load the "isHistoryShown" property.
        String isHistoryShownString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isMessageHistoryShown");

        if(isHistoryShownString != null
            && isHistoryShownString != "")
        {
            isHistoryShown
                = new Boolean(isHistoryShownString)
                    .booleanValue();
        }
        
        // Load the "chatHistorySize" property.
        String chatHistorySizeString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.messageHistorySize");

        if(chatHistorySizeString != null
            && chatHistorySizeString != "")
        {
            chatHistorySize
                = new Integer(chatHistorySizeString)
                .intValue();
        }

        lastContactParent = configService.getString(
            "net.java.sip.communicator.impl.gui.addcontact.lastContactParent");
    }

    /**
     * Return TRUE if "autoPopupNewMessage" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether new messages should be
     * opened and bring to front.
     * @return TRUE if "autoPopupNewMessage" property is true, otherwise - return
     * FALSE.
     */
    public static boolean isAutoPopupNewMessage()
    {
        return autoPopupNewMessage;
    }

    /**
     * Return TRUE if "showCallPanel" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether the panel containing the
     * call and hangup buttons should be shown.
     * @return TRUE if "showCallPanel" property is true, otherwise - return
     * FALSE.
     */
    public static boolean isCallPanelShown()
    {
        return isCallPanelShown;
    }

    /**
     * Return TRUE if "showOffline" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether offline user should be
     * shown in the contact list or not.
     * @return TRUE if "showOffline" property is true, otherwise - return
     * FALSE.
     */
    public static boolean isShowOffline()
    {
        return isShowOffline;
    }

    /**
     * Return TRUE if "showApplication" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether the main application
     * window should shown or hidden on startup.
     * @return TRUE if "showApplication" property is true, otherwise - return
     * FALSE.
     */
    public static boolean isApplicationVisible()
    {
        return isApplicationVisible;
    }

    /**
     * Return TRUE if "quitWarningShown" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether the quit warning
     * dialog should be shown when user clicks on the X button.
     * @return TRUE if "quitWarningShown" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether the quit warning
     * dialog should be shown when user clicks on the X button.
     */
    public static boolean isQuitWarningShown()
    {
        return isQuitWarningShown;
    }

    /**
     * Return TRUE if "sendTypingNotifications" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether typing
     * notifications are enabled or disabled.
     * @return TRUE if "sendTypingNotifications" property is true, otherwise -
     * return FALSE.
     */
    public static boolean isSendTypingNotifications()
    {
        return isSendTypingNotifications;
    }

    /**
     * Returns TRUE if the "isMoveContactConfirmationRequested" property is true,
     * otherwise - returns FALSE. Indicates to the user interface whether the
     * confirmation window during the move contact process is enabled or not.
     * @return TRUE if the "isMoveContactConfirmationRequested" property is true,
     * otherwise - returns FALSE
     */
    public static boolean isMoveContactConfirmationRequested()
    {
        return isMoveContactConfirmationRequested;
    }

    /**
     * Returns <code>true</code> if the "isMultiChatWindowEnabled" property is
     * true, otherwise - returns <code>false</code>. Indicates to the user
     * interface whether the chat window could contain multiple chats or just
     * one chat.
     * @return <code>true</code> if the "isMultiChatWindowEnabled" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isMultiChatWindowEnabled()
    {
        return isMultiChatWindowEnabled;
    }

    /**
     * Returns <code>true</code> if the "isHistoryLoggingEnabled" property is
     * true, otherwise - returns <code>false</code>. Indicates to the user
     * interface whether the history logging is enabled.
     * @return <code>true</code> if the "isHistoryLoggingEnabled" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isHistoryLoggingEnabled()
    {
        return isHistoryLoggingEnabled;
    }
    
    /**
     * Returns <code>true</code> if the "isHistoryShown" property is
     * true, otherwise - returns <code>false</code>. Indicates to the user
     * whether the history is shown in the chat window.
     * @return <code>true</code> if the "isHistoryShown" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isHistoryShown()
    {
        return isHistoryShown;
    }
    
    /**
     * Return the "sendMessageCommand" property that was saved previously
     * through the <tt>ConfigurationService</tt>. Indicates to the user
     * interface whether the default send message command is Enter or CTRL-Enter.
     * @return "Enter" or "CTRL-Enter" message commands.
     */
    public static String getSendMessageCommand()
    {
        return sendMessageCommand;
    }
    
    /**
     * Return the "lastContactParent" property that was saved previously
     * through the <tt>ConfigurationService</tt>. Indicates 
     * the last selected group on adding new contact 
     * @return group name of the last selected group when adding contact.
     */
    public static String getLastContactParent()
    {
        return lastContactParent;
    }

    /**
     * Returns the number of messages from chat history that would be shown in
     * the chat window.
     * @return the number of messages from chat history that would be shown in
     * the chat window.
     */
    public static int getChatHistorySize()
    {
        return chatHistorySize;
    }

    /**
     * Updates the "showOffline" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isShowOffline <code>true</code> to indicate that the
     * offline users should be shown, <code>false</code> otherwise.
     */
    public static void setShowOffline(boolean isShowOffline)
    {
        ConfigurationManager.isShowOffline = isShowOffline;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.showOffline",
                Boolean.toString(isShowOffline));
    }

    /**
     * Updates the "showCallPanel" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isCallPanelShown <code>true</code> to indicate that the
     * call panel should be shown, <code>false</code> otherwise.
     */
    public static void setShowCallPanel(boolean isCallPanelShown)
    {
        ConfigurationManager.isCallPanelShown = isCallPanelShown;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.showCallPanel",
                Boolean.toString(isCallPanelShown));
    }

    /**
     * Updates the "showApplication" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isVisible <code>true</code> to indicate that the
     * application should be shown, <code>false</code> otherwise.
     */
    public static void setApplicationVisible(boolean isVisible)
    {
        isApplicationVisible = isVisible;
            
        configService.setProperty(
                "net.java.sip.communicator.impl.systray.showApplication",
                Boolean.toString(isVisible));
    }

    /**
     * Updates the "showAppQuitWarning" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isWarningShown indicates if the message warning the user that the
     * application would not be closed if she clicks the X button would be
     * shown again.
     */
    public static void setQuitWarningShown(boolean isWarningShown)
    {
        isQuitWarningShown = isWarningShown;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.quitWarningShown",
                Boolean.toString(isQuitWarningShown));
    }

     /**
     * Updates the "lastContactParent" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param groupName the group name of the selected group when adding
     * last contact
     */
    public static void setLastContactParent(String groupName)
    {
        lastContactParent = groupName;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.addcontact.lastContactParent",
                groupName);
    }

    /**
     * Updates the "isMoveContactQuestionEnabled" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isRequested indicates if a confirmation would be requested
     * from user during the move contact process.
     */
    public static void setMoveContactConfirmationRequested(boolean isRequested)
    {
        isMoveContactConfirmationRequested = isRequested;

        configService.setProperty(
            "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested",
            Boolean.toString(isMoveContactConfirmationRequested));
    }

    /**
     * Saves a chat room through the <tt>ConfigurationService</tt>.
     * 
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param oldChatRoomId the old identifier of the chat room
     * @param newChatRoomId the new identifier of the chat room
     * @param newChatRoomName the new chat room name
     */
    public static void updateChatRoom(  ProtocolProviderService protocolProvider,
                                        String oldChatRoomId,
                                        String newChatRoomId,
                                        String newChatRoomName)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext())
        {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID()))
            {
                List chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                Iterator chatRoomsIter = chatRooms.iterator();

                boolean isExistingChatRoom = false;

                while(chatRoomsIter.hasNext())
                {
                    String chatRoomPropName
                        = (String) chatRoomsIter.next();

                    String chatRoomID
                        = configService.getString(chatRoomPropName);

                    if(!oldChatRoomId.equals(chatRoomID))
                        continue;

                    isExistingChatRoom = true;

                    configService.setProperty(chatRoomPropName,
                        newChatRoomId);

                    configService.setProperty(  chatRoomPropName
                                                    + ".chatRoomName",
                                                newChatRoomName);
                }

                if(!isExistingChatRoom)
                {
                    String chatRoomNodeName
                        = "chatRoom" + Long.toString(System.currentTimeMillis());

                    String chatRoomPackage = accountRootPropName
                        + ".chatRooms." + chatRoomNodeName;

                    configService.setProperty(chatRoomPackage,
                        newChatRoomId);

                    configService.setProperty(  chatRoomPackage
                                                    + ".chatRoomName",
                                                newChatRoomName);
                }
            }
        }
    }

    /**
     * Updates the status of the chat room through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room to update
     * @param chatRoomStatus the new status of the chat room
     */
    public static void updateChatRoomStatus(
            ProtocolProviderService protocolProvider,
            String chatRoomId,
            String chatRoomStatus)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List accounts = configService
            .getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext())
        {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                .getAccountID().getAccountUniqueID()))
            {
                List chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                Iterator chatRoomsIter = chatRooms.iterator();

                while(chatRoomsIter.hasNext())
                {
                    String chatRoomPropName
                        = (String) chatRoomsIter.next();

                    String chatRoomID
                        = configService.getString(chatRoomPropName);

                    if(!chatRoomId.equals(chatRoomID))
                        continue;

                    configService.setProperty(  chatRoomPropName
                        + ".lastChatRoomStatus",
                        chatRoomStatus);
                }
            }
        }
    }

    /**
     * Returns the last chat room status, saved through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param protocolProvider the protocol provider, to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room
     * @return the last chat room status, saved through the
     * <tt>ConfigurationService</tt>.
     */
    public static String getChatRoomStatus(
        ProtocolProviderService protocolProvider,
        String chatRoomId)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List accounts = configService
            .getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext())
        {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                .getAccountID().getAccountUniqueID()))
            {
                List chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                Iterator chatRoomsIter = chatRooms.iterator();

                while(chatRoomsIter.hasNext())
                {
                    String chatRoomPropName
                        = (String) chatRoomsIter.next();

                    String chatRoomID
                        = configService.getString(chatRoomPropName);

                    if(!chatRoomId.equals(chatRoomID))
                        continue;

                    return configService.getString(  chatRoomPropName
                                                    + ".lastChatRoomStatus");
                }
            }
        }

        return null;
    }
    
    public static void storeContactListGroupStatus( String groupID,
                                                    boolean status)
    {
        String prefix = "net.java.sip.communicator.impl.gui.contactlist.groups";

        List groups = configService
            .getPropertyNamesByPrefix(prefix, true);

        Iterator groupsIter = groups.iterator();

        boolean isExistingGroup = false;
        while(groupsIter.hasNext())
        {
            String groupRootPropName
                = (String) groupsIter.next();

            String storedID
                = configService.getString(groupRootPropName);

            if(storedID.equals(groupID))
            {
                configService.setProperty(  groupRootPropName
                    + ".isClosed",
                    Boolean.toString(status));

                isExistingGroup = true;
                break;
            }
        }

        if(!isExistingGroup)
        {
            String groupNodeName
                = "group" + Long.toString(System.currentTimeMillis());

            String groupPackage = prefix + "." + groupNodeName;

            configService.setProperty(  groupPackage,
                                        groupID);

            configService.setProperty(  groupPackage
                                            + ".isClosed",
                                        Boolean.toString(status));
        }
    }
    
    public static boolean getContactListGroupStatus(String groupID)
    {
        String prefix = "net.java.sip.communicator.impl.gui.contactlist.groups";

        List groups = configService
            .getPropertyNamesByPrefix(prefix, true);

        Iterator groupsIter = groups.iterator();

        while(groupsIter.hasNext())
        {
            String groupRootPropName
                = (String) groupsIter.next();

            String storedID
                = configService.getString(groupRootPropName);

            if(storedID.equals(groupID))
            {
                String status = (String) configService
                    .getProperty(  groupRootPropName + ".isClosed");

                return new Boolean(status).booleanValue();
            }
        }
        
        return false;
    }

    private static class ConfigurationChangeListener
            implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt) 
        {
            if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.addcontact.lastContactParent"))
                lastContactParent = (String)evt.getNewValue();
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.autoPopupNewMessage"))
            {
                String autoPopupString = (String) evt.getNewValue();
                
                autoPopupNewMessage
                    = new Boolean(autoPopupString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.sendMessageCommand"))
            {
                sendMessageCommand
                    = (String) evt.getNewValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.showCallPanel"))
            {
                String showCallPanelString = (String) evt.getNewValue();
                
                isCallPanelShown
                    = new Boolean(showCallPanelString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.showOffline"))
            {
                String showOfflineString = (String) evt.getNewValue();
                
                isShowOffline
                    = new Boolean(showOfflineString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.systray.showApplication"))
            {
                String showApplicationString = (String) evt.getNewValue();
                
                isApplicationVisible
                    = new Boolean(showApplicationString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.quitWarningShown"))
            {
                String showQuitWarningString = (String) evt.getNewValue();
                
                isQuitWarningShown
                    = new Boolean(showQuitWarningString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.sendTypingNotifications"))
            {
                String sendTypingNorifString = (String) evt.getNewValue();
                
                isSendTypingNotifications
                    = new Boolean(sendTypingNorifString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested"))
            {
                String moveContactConfirmString = (String) evt.getNewValue();
                
                isMoveContactConfirmationRequested
                    = new Boolean(moveContactConfirmString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isMultiChatWindowEnabled"))
            {
                String multiChatWindowString = (String) evt.getNewValue();
                
                isMultiChatWindowEnabled
                    = new Boolean(multiChatWindowString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isHistoryLoggingEnabled"))
            {
                String historyLoggingString = (String) evt.getNewValue();
                
                isHistoryLoggingEnabled
                    = new Boolean(historyLoggingString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isMessageHistoryShown"))
            {
                String historyShownString = (String) evt.getNewValue();
                
                isHistoryShown
                    = new Boolean(historyShownString).booleanValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.messageHistorySize"))
            {
                String chatHistorySizeString = (String) evt.getNewValue();
                
                chatHistorySize
                    = new Integer(chatHistorySizeString).intValue();
            }
        }
    }
}
