/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import net.java.sip.communicator.service.configuration.*;

public class ConfigurationManager
{
    public static final String ENTER_COMMAND = "Enter";
    
    public static final String CTRL_ENTER_COMMAND = "Ctrl-Enter";
    
    /**
     * Indicates whether the message automatic popup is enabled.
     */
    private static boolean autoPopupNewMessage;
    
    private static String sendMessageCommand;
    
    private static boolean isSendTypingNotifications;
    
    private static boolean isMultiChatWindowEnabled;
    
    private static boolean isHistoryLoggingEnabled;
    
    private static boolean isHistoryShown;
    
    private static int chatHistorySize;
    
    private static int windowTransparency;
    
    private static boolean isTransparentWindowEnabled;
    
    private static ConfigurationService configService
        = GeneralConfigPluginActivator.getConfigurationService();

    /**
     * 
     */
    public static void loadGuiConfigurations()
    {
        // Load the "auPopupNewMessage" property.
        String autoPopupProperty = 
            "net.java.sip.communicator.impl.gui.autoPopupNewMessage";
        
        String autoPopup = configService.getString(autoPopupProperty);

        if(autoPopup == null)
            autoPopup = Resources.getSettingsString(autoPopupProperty);
        
        if(autoPopup != null && autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;

        // Load the "sendMessageCommand" property.
        String messageCommandProperty = 
            "net.java.sip.communicator.impl.gui.sendMessageCommand";
        String messageCommand = configService.getString(messageCommandProperty);
        
        if(messageCommand == null)
            messageCommand = 
                Resources.getSettingsString(messageCommandProperty);

        if(messageCommand != null && messageCommand.length() > 0)
        {
            sendMessageCommand = messageCommand;
        }

        // Load the "sendTypingNotifications" property.
        String isSendTypingNotifProperty = 
            "net.java.sip.communicator.impl.gui.sendTypingNotifications";
        String isSendTypingNotif = 
            configService.getString(isSendTypingNotifProperty);
        
        if(isSendTypingNotif == null)
            isSendTypingNotif = 
                Resources.getSettingsString(isSendTypingNotifProperty);

        if(isSendTypingNotif != null && isSendTypingNotif.length() > 0)
        {
            isSendTypingNotifications
                = new Boolean(isSendTypingNotif).booleanValue();
        }

        // Load the "isMultiChatWindowEnabled" property.
        String isMultiChatWindowEnabledStringProperty
            = "net.java.sip.communicator.impl.gui.isMultiChatWindowEnabled";
        
        String isMultiChatWindowEnabledString
            = configService.getString(isMultiChatWindowEnabledStringProperty);
        
        if(isMultiChatWindowEnabledString == null)
            isMultiChatWindowEnabledString = 
                Resources.
                getSettingsString(isMultiChatWindowEnabledStringProperty);

        if(isMultiChatWindowEnabledString != null
            && isMultiChatWindowEnabledString.length() > 0)
        {
            isMultiChatWindowEnabled
                = new Boolean(isMultiChatWindowEnabledString)
                .booleanValue();
        }
        
        // Load the "isHistoryLoggingEnabled" property.
        String isHistoryLoggingEnabledPropertyString =
            "net.java.sip.communicator.impl.msghistory.isMessageHistoryEnabled";
        
        String isHistoryLoggingEnabledString
            = configService.getString(
            isHistoryLoggingEnabledPropertyString);
        
        if(isHistoryLoggingEnabledString == null)
            isHistoryLoggingEnabledString = 
                Resources.
                getSettingsString(isHistoryLoggingEnabledPropertyString);

        if(isHistoryLoggingEnabledString != null
            && isHistoryLoggingEnabledString.length() > 0)
        {
            isHistoryLoggingEnabled
                = new Boolean(isHistoryLoggingEnabledString)
                .booleanValue();
        }
        
        // Load the "isHistoryShown" property.
        String isHistoryShownStringProperty = 
            "net.java.sip.communicator.impl.gui.isMessageHistoryShown";
        
        String isHistoryShownString
            = configService.getString(isHistoryShownStringProperty);
        
        if(isHistoryShownString == null)
            isHistoryShownString = 
                Resources.getSettingsString(isHistoryShownStringProperty);

        if(isHistoryShownString != null
            && isHistoryShownString.length() > 0)
        {
            isHistoryShown
                = new Boolean(isHistoryShownString)
                    .booleanValue();
        }

        // Load the "chatHistorySize" property.
        String chatHistorySizeStringProperty =
            "net.java.sip.communicator.impl.gui.messageHistorySize";
        String chatHistorySizeString
            = configService.getString(chatHistorySizeStringProperty);

        if(chatHistorySizeString == null)
            chatHistorySizeString = 
                Resources.getSettingsString(chatHistorySizeStringProperty);

        if(chatHistorySizeString != null
            && chatHistorySizeString.length() > 0)
        {
            chatHistorySize
                = new Integer(chatHistorySizeString)
                .intValue();
        }

        // Load the "isTransparentWindowEnabled" property.
        String isTransparentWindowEnabledProperty =
            "net.java.sip.communicator.impl.gui.isTransparentWindowEnabled";

        String isTransparentWindowEnabledString
            = configService.getString(isTransparentWindowEnabledProperty);

        if(isTransparentWindowEnabledString == null)
            isTransparentWindowEnabledString = 
                Resources.getSettingsString(isTransparentWindowEnabledProperty);

        if(isTransparentWindowEnabledString != null
            && isTransparentWindowEnabledString.length() > 0)
        {
            isTransparentWindowEnabled
                = new Boolean(isTransparentWindowEnabledString).booleanValue();
        }

        // Load the "windowTransparency" property.
        String windowTransparencyProperty =
            "net.java.sip.communicator.impl.gui.windowTransparency";

        String windowTransparencyString
            = configService.getString(windowTransparencyProperty);

        if(windowTransparencyString == null)
            windowTransparencyString = 
                Resources.getSettingsString(windowTransparencyProperty);

        if(windowTransparencyString != null
            && windowTransparencyString.length() > 0)
        {
            windowTransparency
                = new Integer(windowTransparencyString).intValue();
        }
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

    public static void setTransparentWindowEnabled(
        boolean isTransparentWindowEnabled)
    {
        ConfigurationManager.isTransparentWindowEnabled =
            isTransparentWindowEnabled;

        configService.setProperty(
            "net.java.sip.communicator.impl.gui.isTransparentWindowEnabled",
            new Boolean(isTransparentWindowEnabled).toString());
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

    public static void setWindowTransparency(int windowTransparency)
    {
        ConfigurationManager.windowTransparency = windowTransparency;

        configService.setProperty(
            "net.java.sip.communicator.impl.gui.windowTransparency",
            new Integer(windowTransparency).toString());
    }

    /**
     * Updates the "autoPopupNewMessage" property.
     * 
     * @param autoPopupNewMessage indicates to the user interface whether new
     * messages should be opened and bring to front. 
     **/
    public static void setAutoPopupNewMessage(boolean autoPopupNewMessage)
    {
        ConfigurationManager.autoPopupNewMessage = autoPopupNewMessage;

        if(autoPopupNewMessage)
            configService.setProperty(
                    "net.java.sip.communicator.impl.gui.autoPopupNewMessage",
                    "yes");
        else
            configService.setProperty(
                    "net.java.sip.communicator.impl.gui.autoPopupNewMessage",
                    "no");
    }

    /**
     * Updates the "sendTypingNotifications" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isSendTypingNotif <code>true</code> to indicate that typing
     * notifications are enabled, <code>false</code> otherwise.
     */
    public static void setSendTypingNotifications(boolean isSendTypingNotif)
    {
        isSendTypingNotifications = isSendTypingNotif;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.sendTypingNotifications",
                Boolean.toString(isSendTypingNotif));
    }

    /**
     * Updates the "sendMessageCommand" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param newMessageCommand the command used to send a message ( it could be
     * ENTER_COMMAND or CTRL_ENTER_COMMAND)
     */
    public static void setSendMessageCommand(String newMessageCommand)
    {
        sendMessageCommand = newMessageCommand;

        configService.setProperty(
                "net.java.sip.communicator.impl.gui.sendMessageCommand",
                newMessageCommand);
    }

    /**
     * Updates the "isMultiChatWindowEnabled" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isMultiChatWindowEnabled indicates if the chat window could
     * contain multiple chats or only one chat.
     */
    public static void setMultiChatWindowEnabled(
        boolean isMultiChatWindowEnabled)
    {
        ConfigurationManager.isHistoryLoggingEnabled = isMultiChatWindowEnabled;

        configService.setProperty(
            "net.java.sip.communicator.impl.gui.isMultiChatWindowEnabled",
            Boolean.toString(isMultiChatWindowEnabled));
    }

    /**
     * Updates the "isHistoryLoggingEnabled" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isHistoryLoggingEnabled indicates if the history logging is
     * enabled.
     */
    public static void setHistoryLoggingEnabled(
        boolean isHistoryLoggingEnabled)
    {
        ConfigurationManager.isHistoryLoggingEnabled = isHistoryLoggingEnabled;
        
        configService.setProperty(
            "net.java.sip.communicator.impl.msghistory.isMessageHistoryEnabled",
            Boolean.toString(isHistoryLoggingEnabled));
    }
    
    /**
     * Updates the "isHistoryLoggingEnabled" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param isHistoryLoggingEnabled indicates if the history logging is
     * enabled.
     */
    public static void setHistoryShown(boolean isHistoryShown)
    {
        ConfigurationManager.isHistoryShown = isHistoryShown;
        
        configService.setProperty(
            "net.java.sip.communicator.impl.gui.isMessageHistoryShown",
            Boolean.toString(isHistoryShown));
    }
    
    /**
     * Updates the "chatHistorySize" property through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param historySize indicates if the history logging is
     * enabled.
     */
    public static void setChatHistorySize(int historySize)
    {
        ConfigurationManager.chatHistorySize = historySize;
        
        configService.setProperty(
            "net.java.sip.communicator.impl.gui.messageHistorySize",
            Integer.toString(chatHistorySize));
    }
}