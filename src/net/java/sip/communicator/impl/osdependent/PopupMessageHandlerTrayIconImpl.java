/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.event.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation  of the <tt>PopupMsystrayessageHandler</tt> using the
 * tray icon.
 */
public class PopupMessageHandlerTrayIconImpl
    extends AbstractPopupMessageHandler
{
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(PopupMessageHandlerTrayIconImpl.class);

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
                TrayIcon.NONE_MESSAGE_TYPE);
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
