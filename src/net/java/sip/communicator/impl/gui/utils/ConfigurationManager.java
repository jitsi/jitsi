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
    /**
     * Indicates whether the message automatic popup is enabled.
     */
    private static boolean autoPopupNewMessage;

    public static void loadGuiConfigurations()
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        
        String autoPopup = configService.getString(
            "net.java.sip.communicator.impl.gui.autoPopupNewMessage");
    
        if(autoPopup == null || autoPopup.equalsIgnoreCase("yes"))
            autoPopupNewMessage = true;
        else
            autoPopupNewMessage = false;
    }
    
    public static boolean isAutoPopupNewMessage()
    {
        return autoPopupNewMessage;
    }

    public static void setAutoPopupNewMessage(boolean autoPopupNewMessage)
    {
        ConfigurationManager.autoPopupNewMessage = autoPopupNewMessage;
        
        ConfigurationService configService
                = GuiActivator.getConfigurationService();
            
        if(autoPopupNewMessage)
            configService.setProperty(
                    "net.java.sip.communicator.impl.gui.autoPopupNewMessage",
                    "yes");
        else
            configService.setProperty(
                    "net.java.sip.communicator.impl.gui.autoPopupNewMessage",
                    "no");
    }
}
