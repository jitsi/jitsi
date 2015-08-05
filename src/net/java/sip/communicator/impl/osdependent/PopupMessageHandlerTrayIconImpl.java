/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.event.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;

/**
 * An implementation  of the <tt>PopupMsystrayessageHandler</tt> using the
 * tray icon.
 */
public class PopupMessageHandlerTrayIconImpl
    extends AbstractPopupMessageHandler
{
    /** the tray icon we will use to popup messages */
    private TrayIcon trayIcon;

    /**
     * Creates a new <tt>PopupMessageHandlerTrayIconImpl</tt> which will uses
     * the provided <tt>TrayIcon</tt> to show message.
     * @param icon the icon we will use to show popup message.
     */
    public PopupMessageHandlerTrayIconImpl(TrayIcon icon)
    {
        trayIcon = icon;
        icon.addBalloonActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                firePopupMessageClicked(new SystrayPopupMessageEvent(e));
            }
        });
    }

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        // remove eventual html code before showing the popup message
        String messageContent = popupMessage.getMessage()
                .replaceAll("</?\\w++[^>]*+>", "");
        String messageTitle = popupMessage.getMessageTitle()
                .replaceAll("</?\\w++[^>]*+>", "");

        if(messageContent.length() > 40)
            messageContent = messageContent.substring(0, 40).concat("...");
        trayIcon.displayMessage(
                messageTitle,
                messageContent,
                java.awt.TrayIcon.MessageType.NONE);
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        return OsDependentActivator.getResources()
            .getI18NString("impl.systray.POPUP_MESSAGE_HANDLER");
    }

    /**
     * Implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>.
     * This handler is able to detect clicks, thus the index is 1.
     * @return a preference index.
     */
    public int getPreferenceIndex()
    {
        return 1;
    }
}
