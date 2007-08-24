/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt>
 * in the system tray using the Jdic library.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 */
public class SystrayServiceJdicImpl
    implements SystrayService
{
    /**
     * The systray.
     */
    private SystemTray systray;

    /**
     * The icon in the system tray.
     */
    private TrayIcon trayIcon;

    /**
     * The menu that spring with a right click.
     */
    private TrayMenu menu;

    /**
     * The list of all providers.
     */
    private Map protocolProviderTable = new LinkedHashMap();

    /**
     * The list of all added popup message listeners.
     */
    private Vector popupMessageListeners = new Vector();

    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(SystrayServiceJdicImpl.class.getName());

    private ImageIcon logoIcon;

    /**
     * Creates an instance of <tt>Systray</tt>.
     */
    public SystrayServiceJdicImpl()
    {
        try
        {
            systray = SystemTray.getDefaultSystemTray();
        }
        catch (Throwable e)
        {
            logger.error("Failed to create a systray!", e);
        }

        if(systray != null)
        {
            this.initSystray();

            SystrayActivator.getUIService().setExitOnMainWindowClose(false);
        }
    }

    /**
     * Initializes the systray icon and related listeners.
     */
    private void initSystray()
    {
        menu = new TrayMenu(this);

        String osName = System.getProperty("os.name");
        // If we're running under Windows, we use a special icon without
        // background.
        if (osName.startsWith("Windows"))
        {
           logoIcon = new ImageIcon(
                   Resources.getImage("trayIconWindows"));
        }
        else
        {
            logoIcon = new ImageIcon(
                    Resources.getImage("trayIcon"));
        }

        trayIcon = new TrayIcon(logoIcon, "SIP Communicator", menu);
        trayIcon.setIconAutoSize(true);

        //Show/hide the contact list when user clicks on the systray.
        trayIcon.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIService uiService = SystrayActivator.getUIService();
                
                boolean isVisible;

                isVisible = ! uiService.isVisible();

                uiService.setVisible(isVisible);

                ConfigurationService configService
                    = SystrayActivator.getConfigurationService();

                configService.setProperty(
                        "net.java.sip.communicator.impl.systray.showApplication",
                        new Boolean(isVisible));
            }
        });

        //Notify all interested listener that user has clicked on the systray
        //popup message.
        trayIcon.addBalloonActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIService uiService = SystrayActivator.getUIService();
                
                firePopupMessageEvent(e.getSource());

                ExportedWindow chatWindow
                    = uiService.getExportedWindow(ExportedWindow.CHAT_WINDOW);

                if(chatWindow != null && chatWindow.isVisible())
                {
                    chatWindow.bringToFront();
                }
            }
        });

        systray.addTrayIcon(trayIcon);
    }

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator getProtocolProviders()
    {
        return this.protocolProviderTable.values().iterator();
    }
    /**
     * Display in a balloon the newly received message
     * @param evt the event containing the message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on loging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     */
    public void saveStatusInformation(ProtocolProviderService protocolProvider,
            String statusName)
    {
        ConfigurationService configService
            = SystrayActivator.getConfigurationService();

        if(configService != null)
        {
            String prefix = "net.java.sip.communicator.impl.gui.accounts";

            List accounts = configService
                    .getPropertyNamesByPrefix(prefix, true);

            boolean savedAccount = false;
            Iterator accountsIter = accounts.iterator();

            while(accountsIter.hasNext()) {
                String accountRootPropName
                    = (String) accountsIter.next();

                String accountUID
                    = configService.getString(accountRootPropName);

                if(accountUID.equals(protocolProvider
                        .getAccountID().getAccountUniqueID())) {

                    configService.setProperty(
                            accountRootPropName + ".lastAccountStatus",
                            statusName);

                    savedAccount = true;
                }
            }

            if(!savedAccount) {
                String accNodeName
                    = "acc" + Long.toString(System.currentTimeMillis());

                String accountPackage
                    = "net.java.sip.communicator.impl.gui.accounts."
                            + accNodeName;

                configService.setProperty(accountPackage,
                        protocolProvider.getAccountID().getAccountUniqueID());

                configService.setProperty(
                        accountPackage+".lastAccountStatus",
                        statusName);
            }
        }
    }

    /**
     * Implements the <tt>SystratService.showPopupMessage</tt> method. Shows
     * a popup message, above the systray icon, which has the given title,
     * message content and message type.
     */
    public void showPopupMessage(String title, String messageContent,
        int messageType)
    {
        int trayMsgType = TrayIcon.NONE_MESSAGE_TYPE;

        if (messageType == SystrayService.ERROR_MESSAGE_TYPE)
            trayMsgType = TrayIcon.ERROR_MESSAGE_TYPE;
        else if (messageType == SystrayService.INFORMATION_MESSAGE_TYPE)
            trayMsgType = TrayIcon.INFO_MESSAGE_TYPE;
        else if (messageType == SystrayService.WARNING_MESSAGE_TYPE)
            trayMsgType = TrayIcon.WARNING_MESSAGE_TYPE;

        if(messageContent.length() > 100)
            messageContent = messageContent.substring(0, 100).concat("...");

        this.trayIcon.displayMessage(
            title, messageContent, trayMsgType);
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            this.popupMessageListeners.add(listener);
        }
    }

    /**
     * Implements the <tt>SystrayService.removePopupMessageListener</tt> method.
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            this.popupMessageListeners.remove(listener);
        }
    }

    /**
     * Notifies all interested listeners that a <tt>SystrayPopupMessageEvent</tt>
     * has occured
     * 
     * @param sourceObject the source of this event
     */
    private void firePopupMessageEvent(Object sourceObject)
    {
        SystrayPopupMessageEvent evt
            = new SystrayPopupMessageEvent(sourceObject);

        logger.trace("Will dispatch the following systray msg event: " + evt);

        Iterator listeners = null;
        synchronized (popupMessageListeners)
        {
            listeners = new ArrayList(popupMessageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SystrayPopupMessageListener listener
                = (SystrayPopupMessageListener) listeners.next();

            listener.popupMessageClicked(evt);
        }
    }

    /**
     * Sets a new systray icon.
     * @param image the icon to set.
     */
    public void setSystrayIcon(byte[] image)
    {
        this.trayIcon.setIcon(new ImageIcon(image));
    }
}
