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
    
    private static ConfigurationService configService
        = GeneralConfigPluginActivator.getConfigurationService();

    /**
     * 
     */
    public static void loadGuiConfigurations()
    {
        // Load the "auPopupNewMessage" property.
        String autoPopup = configService.getString(
            "net.java.sip.communicator.impl.gui.autoPopupNewMessage");

        if(autoPopup != null && autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;

        // Load the "sendMessageCommand" property.
        String messageCommand = configService.getString(
            "net.java.sip.communicator.impl.gui.sendMessageCommand");

        if(messageCommand != null && messageCommand != "")
        {
            sendMessageCommand = messageCommand;
        }

        // Load the "sendTypingNotifications" property.
        String isSendTypingNotif = configService.getString(
            "net.java.sip.communicator.impl.gui.sendTypingNotifications");

        if(isSendTypingNotif != null && isSendTypingNotif != "")
        {
            isSendTypingNotifications
                = new Boolean(isSendTypingNotif).booleanValue();
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
            "net.java.sip.communicator.impl.msghistory.isMessageHistoryEnabled");

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