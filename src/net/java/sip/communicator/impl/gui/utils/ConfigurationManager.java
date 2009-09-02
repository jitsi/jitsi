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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
    
    private static int chatWriteAreaSize;
    
    private static int windowTransparency;
    
    private static boolean isTransparentWindowEnabled;
    
    private static boolean isWindowDecorated;
    
    private static boolean isChatToolbarVisible;
    
    private static boolean isChatStylebarVisible;
    
    private static String sendFileLastDir;
    
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
        String autoPopupProperty = 
            "service.gui.AUTO_POPUP_NEW_MESSAGE";

        String autoPopup = configService.getString(autoPopupProperty);
        
        if(autoPopup == null)
            autoPopup = GuiActivator.getResources().
                getSettingsString(autoPopupProperty);

        if(autoPopup != null && autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;

        // Load the "sendMessageCommand" property.
        String messageCommandProperty = 
            "service.gui.SEND_MESSAGE_COMMAND";
        String messageCommand = configService.getString(messageCommandProperty);
        
        if(messageCommand == null)
            messageCommand = 
                GuiActivator.getResources().getSettingsString(messageCommandProperty);

        if(messageCommand == null || messageCommand.length() == 0)
            sendMessageCommand = messageCommand;

        // Load the showCallPanel property.
        String callPanelShown = configService.getString(
            "net.java.sip.communicator.impl.gui.showCallPanel");

        if(callPanelShown != null && callPanelShown.length() > 0)
        {
            isCallPanelShown = Boolean.parseBoolean(callPanelShown);
        }

        // Load the "showOffline" property.
        String showOffline = configService.getString(
            "net.java.sip.communicator.impl.gui.showOffline");
        
        if(showOffline != null && showOffline.length() > 0)
        {
            isShowOffline = Boolean.parseBoolean(showOffline);
        }

        // Load the "showApplication" property.
        String isVisible = configService.getString(
            "net.java.sip.communicator.impl.systray.showApplication");

        if(isVisible != null && isVisible.length() > 0)
        {
            isApplicationVisible = new Boolean(isVisible).booleanValue();
        }

        // Load the "showAppQuitWarning" property.
        String quitWarningShown = configService.getString(
            "net.java.sip.communicator.impl.gui.quitWarningShown");

        if(quitWarningShown != null && quitWarningShown.length() > 0)
        {
            isQuitWarningShown
                = Boolean.parseBoolean(quitWarningShown);
        }

        // Load the "sendTypingNotifications" property.
        String isSendTypingNotifProperty = 
            "service.gui.SEND_TYPING_NOTIFICATIONS_ENABLED";
        String isSendTypingNotif = 
            configService.getString(isSendTypingNotifProperty);
        
        if(isSendTypingNotif == null)
            isSendTypingNotif = 
                GuiActivator.getResources().
                    getSettingsString(isSendTypingNotifProperty);
        
        if(isSendTypingNotif != null && isSendTypingNotif.length() > 0)
        {
            isSendTypingNotifications
                = Boolean.parseBoolean(isSendTypingNotif);
        }
        
        // Load the "isMoveContactConfirmationRequested" property.
        String isMoveContactConfirmationRequestedString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested");

        if(isMoveContactConfirmationRequestedString != null
            && isMoveContactConfirmationRequestedString.length() > 0)
        {
            isMoveContactConfirmationRequested
                = Boolean.parseBoolean(isMoveContactConfirmationRequestedString)
                ;
        }

        // Load the "isMultiChatWindowEnabled" property.
        String isMultiChatWindowEnabledStringProperty
            = "service.gui.IS_MULTI_CHAT_WINDOW_ENABLED";
        
        String isMultiChatWindowEnabledString
            = configService.getString(isMultiChatWindowEnabledStringProperty);
        
        if(isMultiChatWindowEnabledString == null)
            isMultiChatWindowEnabledString = 
                GuiActivator.getResources().
                    getSettingsString(isMultiChatWindowEnabledStringProperty);

        if(isMultiChatWindowEnabledString != null
            && isMultiChatWindowEnabledString.length() > 0)
        {
            isMultiChatWindowEnabled
                = Boolean.parseBoolean(isMultiChatWindowEnabledString)
                ;
        }
        
        // Load the "isHistoryLoggingEnabled" property.
        String isHistoryLoggingEnabledString
            = configService.getString(
            "net.java.sip.communicator.impl.gui.isHistoryLoggingEnabled");

        if(isHistoryLoggingEnabledString != null
            && isHistoryLoggingEnabledString.length() > 0)
        {
            isHistoryLoggingEnabled
                = Boolean.parseBoolean(isHistoryLoggingEnabledString)
                ;
        }
        
        // Load the "isHistoryShown" property.
        String isHistoryShownStringProperty = 
            "service.gui.IS_MESSAGE_HISTORY_SHOWN";
        
        String isHistoryShownString
            = configService.getString(isHistoryShownStringProperty);
        
        if(isHistoryShownString == null)
            isHistoryShownString = 
                GuiActivator.getResources().
                    getSettingsString(isHistoryShownStringProperty);

        if(isHistoryShownString != null
            && isHistoryShownString.length() > 0)
        {
            isHistoryShown
                = Boolean.parseBoolean(isHistoryShownString)
                    ;
        }
        
        // Load the "chatHistorySize" property.
        String chatHistorySizeStringProperty =
            "service.gui.MESSAGE_HISTORY_SIZE";
        String chatHistorySizeString
            = configService.getString(chatHistorySizeStringProperty);
        
        if(chatHistorySizeString == null)
            chatHistorySizeString = 
                GuiActivator.getResources().
                    getSettingsString(chatHistorySizeStringProperty);

        if(chatHistorySizeString != null
            && chatHistorySizeString.length() > 0)
        {
            chatHistorySize
                = new Integer(chatHistorySizeString)
                .intValue();
        }

        // Load the "CHAT_WRITE_AREA_SIZE" property.
        String chatWriteAreaSizeStringProperty =
            "net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE";
        String chatWriteAreaSizeString
            = configService.getString(chatWriteAreaSizeStringProperty);

        if(chatWriteAreaSizeString == null)
            chatWriteAreaSizeString = 
                GuiActivator.getResources().
                    getSettingsString(chatWriteAreaSizeStringProperty);

        if(chatWriteAreaSizeString != null
            && chatWriteAreaSizeString.length() > 0)
        {
            chatWriteAreaSize
                = new Integer(chatWriteAreaSizeString).intValue();
        }

        // Load the "isTransparentWindowEnabled" property.
        String isTransparentWindowEnabledProperty =
            "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED";

        String isTransparentWindowEnabledString
            = configService.getString(isTransparentWindowEnabledProperty);

        if(isTransparentWindowEnabledString == null)
            isTransparentWindowEnabledString = 
                GuiActivator.getResources().
                    getSettingsString(isTransparentWindowEnabledProperty);

        if(isTransparentWindowEnabledString != null
            && isTransparentWindowEnabledString.length() > 0)
        {
            isTransparentWindowEnabled
                = Boolean.parseBoolean(isTransparentWindowEnabledString);
        }

        // Load the "windowTransparency" property.
        String windowTransparencyProperty =
            "impl.gui.WINDOW_TRANSPARENCY";

        String windowTransparencyString
            = configService.getString(windowTransparencyProperty);

        if(windowTransparencyString == null)
            windowTransparencyString = 
                GuiActivator.getResources().
                    getSettingsString(windowTransparencyProperty);

        if(windowTransparencyString != null
            && windowTransparencyString.length() > 0)
        {
            windowTransparency
                = new Integer(windowTransparencyString).intValue();
        }

        // Load the "isWindowDecorated" property.
        String isWindowDecoratedProperty
            = "impl.gui.IS_WINDOW_DECORATED";

        String isWindowDecoratedString
            = configService.getString(isWindowDecoratedProperty);

        if(isWindowDecoratedString == null)
            isWindowDecoratedString = 
                GuiActivator.getResources().
                    getSettingsString(isWindowDecoratedProperty);

        if(isWindowDecoratedString != null
            && isWindowDecoratedString.length() > 0)
        {
            isWindowDecorated
                = Boolean.parseBoolean(isWindowDecoratedString);
        }
        
        // Load the "isChatToolbarVisible" property
        isChatToolbarVisible
            = configService.getBoolean(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showToolbar",
                true);
        // Load the "isChatToolbarVisible" property
        isChatStylebarVisible
            = configService.getBoolean(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showStylebar",
                true);

        // Load the "lastContactParent" property.
        lastContactParent = configService.getString(
            "net.java.sip.communicator.impl.gui.addcontact.lastContactParent");

        // Load the "sendFileLastDir" property.
        sendFileLastDir = configService.getString(
            "net.java.sip.communicator.impl.gui.chat.filetransfer.SEND_FILE_LAST_DIR");
    }

    /**
     * Return TRUE if "autoPopupNewMessage" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether new messages should be
     * opened and bring to front.
     * @return TRUE if "autoPopupNewMessage" property is true, otherwise
     * - return FALSE.
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
     * Returns <code>true</code> if the "isWindowDecorated" property is
     * true, otherwise - returns <code>false</code>..
     * @return <code>true</code> if the "isWindowDecorated" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isWindowDecorated()
    {
        return isWindowDecorated;
    }
    
    /**
     * Returns <code>true</code> if the "isChatToolbarVisible" property is
     * true, otherwise - returns <code>false</code>..
     * @return <code>true</code> if the "isChatToolbarVisible" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isChatToolbarVisible()
    {
        return isChatToolbarVisible;
    }
    
    /**
     * Returns <code>true</code> if the "isChatStylebarVisible" property is
     * true, otherwise - returns <code>false</code>..
     * @return <code>true</code> if the "isChatStylebarVisible" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isChatStylebarVisible()
    {
        return isChatStylebarVisible;
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
     * Returns the preferred height of the chat write area.
     * 
     * @return the preferred height of the chat write area.
     */
    public static int getChatWriteAreaSize()
    {
        return chatWriteAreaSize;
    }

    /**
     * Returns <code>true</code> if transparent windows are enabled,
     * <code>false</code> otherwise.
     * 
     * @return <code>true</code> if transparent windows are enabled,
     * <code>false</code> otherwise.
     */
    public static boolean isTransparentWindowEnabled()
    {
        return isTransparentWindowEnabled;
    }

    /**
     * Returns the transparency value for all transparent windows.
     * 
     * @return the transparency value for all transparent windows.
     */
    public static int getWindowTransparency()
    {
        return windowTransparency;
    }

    /**
     * Returns the last opened directory of the send file file chooser.
     * 
     * @return the last opened directory of the send file file chooser
     */
    public static String getSendFileLastDir()
    {
        return sendFileLastDir;
    }

    /**
     * Sets the transparency value for all transparent windows.
     * 
     * @param transparency the transparency value for all transparent windows.
     */
    public static void setWindowTransparency(int transparency)
    {
        windowTransparency = transparency;
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
     * Updates the "isTransparentWindowEnabled" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isTransparent indicates if the transparency is enabled in the
     * application.
     */
    public static void setTransparentWindowEnabled(boolean isTransparent)
    {
        isTransparentWindowEnabled = isTransparent;

        configService.setProperty(
                "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED",
                Boolean.toString(isTransparentWindowEnabled));
    }
    
    /**
     * Updates the "isChatToolbarVisible" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isVisible indicates if the chat toolbar is visible.
     */
    public static void setChatToolbarVisible(boolean isVisible)
    {
        isChatToolbarVisible = isVisible;
        
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showToolbar",
                Boolean.toString(isChatToolbarVisible));
    }
    
    /**
     * Updates the "isChatStylebarVisible" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isVisible indicates if the chat stylebar is visible.
     */
    public static void setChatStylebarVisible(boolean isVisible)
    {
        isChatStylebarVisible = isVisible;
        
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showStylebar",
                Boolean.toString(isChatStylebarVisible));
    }

    /**
     * Updates the "net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE"
     * property through the <tt>ConfigurationService</tt>.
     * 
     * @param size the new size to set
     */
    public static void setChatWriteAreaSize(int size)
    {
        chatWriteAreaSize = size;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE",
                Integer.toString(chatWriteAreaSize));
    }

    /**
     * Updates the "SEND_FILE_LAST_DIR"
     * property through the <tt>ConfigurationService</tt>.
     * 
     * @param lastDir last download directory
     */
    public static void setSendFileLastDir(String lastDir)
    {
        sendFileLastDir = lastDir;

        configService.setProperty(
            "net.java.sip.communicator.impl.gui.chat.filetransfer.SEND_FILE_LAST_DIR",
            lastDir);
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
    public static void saveChatRoom(  ProtocolProviderService protocolProvider,
                                        String oldChatRoomId,
                                        String newChatRoomId,
                                        String newChatRoomName)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                boolean isExistingChatRoom = false;

                for (String chatRoomPropName : chatRooms)
                {
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

        List<String> accounts = configService
            .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                .getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
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

        List<String> accounts = configService
            .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                .getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
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

        List<String> groups = configService
            .getPropertyNamesByPrefix(prefix, true);

        boolean isExistingGroup = false;
        for (String groupRootPropName : groups)
        {
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

        List<String> groups = configService
            .getPropertyNamesByPrefix(prefix, true);
        for (String groupRootPropName : groups)
        {
            String storedID
                = configService.getString(groupRootPropName);

            if(storedID.equals(groupID))
            {
                String status = (String) configService
                    .getProperty(  groupRootPropName + ".isClosed");

                return Boolean.parseBoolean(status);
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
                "service.gui.AUTO_POPUP_NEW_MESSAGE"))
            {
                String autoPopupString = (String) evt.getNewValue();
                
                autoPopupNewMessage
                    = Boolean.parseBoolean(autoPopupString);
            }
            else if (evt.getPropertyName().equals(
                "service.gui.SEND_MESSAGE_COMMAND"))
            {
                sendMessageCommand
                    = (String) evt.getNewValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.showCallPanel"))
            {
                String showCallPanelString = (String) evt.getNewValue();
                
                isCallPanelShown
                    = Boolean.parseBoolean(showCallPanelString);
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.showOffline"))
            {
                String showOfflineString = (String) evt.getNewValue();
                
                isShowOffline
                    = Boolean.parseBoolean(showOfflineString);
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
                    = Boolean.parseBoolean(showQuitWarningString);
            }
            else if (evt.getPropertyName().equals(
                "service.gui.SEND_TYPING_NOTIFICATIONS_ENABLED"))
            {
                String sendTypingNorifString = (String) evt.getNewValue();
                
                isSendTypingNotifications
                    = Boolean.parseBoolean(sendTypingNorifString);
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested"))
            {
                String moveContactConfirmString = (String) evt.getNewValue();
                
                isMoveContactConfirmationRequested
                    = Boolean.parseBoolean(moveContactConfirmString);
            }
            else if (evt.getPropertyName().equals(
                "service.gui.IS_MULTI_CHAT_WINDOW_ENABLED"))
            {
                String multiChatWindowString = (String) evt.getNewValue();
                
                isMultiChatWindowEnabled
                    = Boolean.parseBoolean(multiChatWindowString);
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.isHistoryLoggingEnabled"))
            {
                String historyLoggingString = (String) evt.getNewValue();
                
                isHistoryLoggingEnabled
                    = Boolean.parseBoolean(historyLoggingString);
            }
            else if (evt.getPropertyName().equals(
                "service.gui.IS_MESSAGE_HISTORY_SHOWN"))
            {
                String historyShownString = (String) evt.getNewValue();
                
                isHistoryShown
                    = Boolean.parseBoolean(historyShownString);
            }
            else if (evt.getPropertyName().equals(
                "service.gui.MESSAGE_HISTORY_SIZE"))
            {
                String chatHistorySizeString = (String) evt.getNewValue();

                chatHistorySize
                    = new Integer(chatHistorySizeString).intValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE"))
            {
                String chatWriteAreaSizeString = (String) evt.getNewValue();

                chatWriteAreaSize
                    = new Integer(chatWriteAreaSizeString).intValue();
            }
            else if (evt.getPropertyName().equals(
                "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED"))
            {
                String isTransparentString = (String) evt.getNewValue();

                isTransparentWindowEnabled
                    = Boolean.parseBoolean(isTransparentString);
            }
            else if (evt.getPropertyName().equals(
                "impl.gui.WINDOW_TRANSPARENCY"))
            {
                String windowTransparencyString = (String) evt.getNewValue();

                windowTransparency
                    = new Integer(windowTransparencyString).intValue();
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showStylebar"))
            {
                String chatBarString = (String) evt.getNewValue();
                isChatStylebarVisible = Boolean.parseBoolean(chatBarString);
            }
            else if (evt.getPropertyName().equals(
                "net.java.sip.communicator.impl.gui.chat.ChatWindow.showToolbar"))
            {
                String chatBarString = (String) evt.getNewValue();
                isChatToolbarVisible = Boolean.parseBoolean(chatBarString);
            }
        }
    }
}
