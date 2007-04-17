/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
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

    private static boolean isCallPanelShown;
    
    private static boolean isShowOffline;
    
    private static boolean isApplicationVisible;
    
    private static boolean isSendTypingNotifications;
    
    private static ConfigurationService configService
        = GuiActivator.getConfigurationService();
    
    public static void loadGuiConfigurations()
    {   
        String autoPopup = configService.getString(
            "net.java.sip.communicator.impl.gui.autoPopupNewMessage");
        
        if(autoPopup == null || autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;
        else
            autoPopupNewMessage = false;
        
        String messageCommand = configService.getString(
            "net.java.sip.communicator.impl.gui.sendMessageCommand");
    
        if(messageCommand == null || messageCommand != "")
            sendMessageCommand = messageCommand;
    
        String callPanelShown = configService.getString(
            "net.java.sip.communicator.impl.gui.showCallPanel");
    
        if(callPanelShown != null && callPanelShown != "")
        {
            isCallPanelShown = new Boolean(callPanelShown).booleanValue();
        }
        
        String showOffline = configService.getString(
            "net.java.sip.communicator.impl.gui.showOffline");
        
        if(showOffline != null && showOffline != "")
        {
            isShowOffline = new Boolean(showOffline).booleanValue();
        }

        String isVisible = configService.getString(
            "net.java.sip.communicator.impl.systray.showApplication");
        
        if(isVisible != null && isVisible != "")
        {
            isApplicationVisible = new Boolean(isVisible).booleanValue();
        }

        String isSendTypingNotif = configService.getString(
            "net.java.sip.communicator.impl.gui.sendTypingNotifications");
        
        if(isSendTypingNotif != null && isSendTypingNotif != "")
        {
            isSendTypingNotifications
                = new Boolean(isSendTypingNotif).booleanValue();
        }
    }
    
    public static boolean isAutoPopupNewMessage()
    {
        return autoPopupNewMessage;
    }
    
    public static boolean isCallPanelShown()
    {
        return isCallPanelShown;
    }
    
    public static boolean isShowOffline()
    {
        return isShowOffline;
    }
    
    public static boolean isApplicationVisible()
    {
        return isApplicationVisible;
    }
    
    public static boolean isSendTypingNotifications()
    {
        return isSendTypingNotifications;
    }
    
    public static String getSendMessageCommand()
    {
        return sendMessageCommand;
    }

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
    
    public static void setShowOffline(boolean isShowOffline)
    {
        ConfigurationManager.isShowOffline = isShowOffline;
        
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.showOffline",
                new Boolean(isShowOffline));
    }
    
    public static void setShowCallPanel(boolean isCallPanelShown)
    {
        ConfigurationManager.isCallPanelShown = isCallPanelShown;
            
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.showCallPanel",
                new Boolean(isCallPanelShown));
    }
    
    public static void setApplicationVisible(boolean isVisible)
    {
        isApplicationVisible = isVisible;
            
        configService.setProperty(
                "net.java.sip.communicator.impl.systray.showApplication",
                new Boolean(isVisible));
    }
    
    public static void setSendTypingNotifications(boolean isSendTypingNotif)
    {
        isSendTypingNotifications = isSendTypingNotif;
            
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.sendTypingNotifications",
                new Boolean(isSendTypingNotif));
    }
    
    public static void setSendMessageCommand(String newMessageCommand)
    {
        sendMessageCommand = newMessageCommand;
        
        configService.setProperty(
                "net.java.sip.communicator.impl.gui.sendMessageCommand",
                newMessageCommand);
    }
}
