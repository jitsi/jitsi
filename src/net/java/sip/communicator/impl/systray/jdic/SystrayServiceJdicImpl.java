/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.systray.jdic;

import java.awt.event.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jdesktop.jdic.tray.*;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt>
 * in the system tray using the Jdic library.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 */
public class SystrayServiceJdicImpl
    implements  SystrayService
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
     * The list of all added popup message listeners.
     */
    private Vector popupMessageListeners = new Vector();

    /**
     * List of all messages waiting to be shown.
     */
    private ArrayList messageQueue = new ArrayList();

    private Timer popupTimer = new Timer();

    /**
     * The delay between the message pop ups.
     */
    private int messageDelay = 1000;

    private int maxMessageNumber = 3;

    private SystrayMessage aggregatedMessage;

    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(SystrayServiceJdicImpl.class.getName());

    /**
     * The various icons used on the systray
     */
    private ImageIcon currentIcon;
    private ImageIcon logoIcon;
    private ImageIcon logoIconWhite;
    private ImageIcon envelopeIcon;
    private ImageIcon envelopeIconWhite;

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
        popupTimer.scheduleAtFixedRate(new ShowPopupTask(), 0, messageDelay);

        menu = new TrayMenu(this);

        String osName = System.getProperty("os.name");
        // If we're running under Windows, we use a special icon without
        // background.
        if (osName.startsWith("Windows"))
        {
            logoIcon = new ImageIcon(
                    Resources.getImage("trayIconWindows"));
            envelopeIcon = new ImageIcon(
                    Resources.getImage("messageIconWindows"));
        }
        // If we're running under MacOSX, we use a special black and 
        // white icons without background.
        else if (osName.startsWith("Mac OS X"))
        {
            logoIcon = new ImageIcon(
                    Resources.getImage("trayIconMacOSX"));
            logoIconWhite = new ImageIcon(
                    Resources.getImage("trayIconMacOSXWhite"));
            envelopeIcon = new ImageIcon(
                    Resources.getImage("messageIconMacOSX"));
            envelopeIconWhite = new ImageIcon(
                    Resources.getImage("messageIconMacOSXWhite"));
        }
        else
        {
            logoIcon = new ImageIcon(
                    Resources.getImage("trayIcon"));
            envelopeIcon = new ImageIcon(
                    Resources.getImage("messageIcon"));
        }

        currentIcon = logoIcon;
        trayIcon = new TrayIcon(logoIcon,
                                Resources.getString("systrayToolTip"),
                                menu);

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

        // Change the MacOSX icon with the white one when the popup 
        // menu appears
        if (osName.startsWith("Mac OS X"))
        {
            menu.addPopupMenuListener(new PopupMenuListener()
            {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                {
                    if (currentIcon == envelopeIcon)
                    {
                        trayIcon.setIcon(envelopeIconWhite);
                        currentIcon = envelopeIconWhite;
                    }
                    else
                    {
                        trayIcon.setIcon(logoIconWhite);
                        currentIcon = logoIconWhite;                        
                    }
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) 
                {
                    if (currentIcon == envelopeIconWhite)
                    {
                        trayIcon.setIcon(envelopeIcon);
                        currentIcon = envelopeIcon;
                    }
                    else
                    {
                        trayIcon.setIcon(logoIcon);
                        currentIcon = logoIcon;
                    }
                }
        
                public void popupMenuCanceled(PopupMenuEvent e) 
                {
                    popupMenuWillBecomeInvisible(e);
                } 
            });
        }

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
     * Saves the last status for all accounts. This information is used
     * on logging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     * 
     * @param protocolProvider  the protocol provider for which we save the 
     * last selected status
     * @param statusName the status name to save
     */
    public void saveStatusInformation(
            ProtocolProviderService protocolProvider,
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
     * a pop up message, above the Systray icon, which has the given title,
     * message content and message type.
     * 
     * @param title the title of the message
     * @param messageContent the content text
     * @param messageType the type of the message 
     */
    public void showPopupMessage(   String title,
                                    String messageContent,
                                    int messageType)
    {
        int trayMsgType = TrayIcon.NONE_MESSAGE_TYPE;

        if (messageType == SystrayService.ERROR_MESSAGE_TYPE)
            trayMsgType = TrayIcon.ERROR_MESSAGE_TYPE;
        else if (messageType == SystrayService.INFORMATION_MESSAGE_TYPE)
            trayMsgType = TrayIcon.INFO_MESSAGE_TYPE;
        else if (messageType == SystrayService.WARNING_MESSAGE_TYPE)
            trayMsgType = TrayIcon.WARNING_MESSAGE_TYPE;

        if(messageContent.length() > 40)
            messageContent = messageContent.substring(0, 40).concat("...");

        messageQueue.add(new SystrayMessage(title, messageContent, trayMsgType));
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
     * 
     * @param listener the listener to add
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
     * 
     * @param listener the listener to remove
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
     * has occured.
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
     * Sets a new Systray icon.
     * 
     * @param imageType the type of the image to set.
     */
    public void setSystrayIcon(int imageType)
    {
        String osName = System.getProperty("os.name");

        if (imageType == SystrayService.SC_IMG_TYPE)
        {
            if (osName.startsWith("Mac OS X") && this.menu.isVisible())
            {
                this.trayIcon.setIcon(logoIconWhite);
                this.currentIcon = logoIconWhite;
            }
            else
            {
                this.trayIcon.setIcon(logoIcon);
                this.currentIcon = logoIcon;
            }
        }
        else if (imageType == SystrayService.ENVELOPE_IMG_TYPE)
        {
            if (osName.startsWith("Mac OS X") && this.menu.isVisible())
            {
                this.trayIcon.setIcon(envelopeIconWhite);
                this.currentIcon = envelopeIconWhite;
            }
            else
            {
                this.trayIcon.setIcon(envelopeIcon);
                this.currentIcon = envelopeIcon;
            }
        }
    }

    /**
     * Shows the oldest message in the message queue and then removes it from
     * the queue.
     */
    private class ShowPopupTask extends TimerTask
    {
        public void run()
        {
            if(messageQueue.isEmpty())
                return;

            int messageNumber = messageQueue.size();

            SystrayMessage msg = (SystrayMessage) messageQueue.get(0);

            if(messageNumber > maxMessageNumber)
            {
                messageQueue.clear();

                if(aggregatedMessage != null)
                {
                    aggregatedMessage
                        .addAggregatedMessageNumber(messageNumber);
                }
                else
                {
                    String messageContent = msg.getMessageContent();

                    if(!messageContent.endsWith("..."))
                        messageContent.concat("...");

                    aggregatedMessage = new SystrayMessage(
                        "Messages start by: " + messageContent,
                        messageNumber);
                }

                messageQueue.add(aggregatedMessage);
            }
            else
            {
                trayIcon.displayMessage(msg.getTitle(),
                                    msg.getMessageContent(),
                                    msg.getMessageType());

                messageQueue.remove(0);

                if(msg.equals(aggregatedMessage))
                    aggregatedMessage = null;
            }
        }
    }

    /**
     * Represents a systray message.
     */
    private class SystrayMessage
    {
        private String title;
        private String messageContent;
        private int messageType;
        private int aggregatedMessageNumber;

        /**
         * Creates an instance of <tt>SystrayMessage</tt> by specifying the
         * message <tt>title</tt>, the content of the message and the type of
         * the message.
         * 
         * @param title the title of the message
         * @param messageContent the content of the message
         * @param messageType the type of the message
         */
        public SystrayMessage(  String title,
                                String messageContent,
                                int messageType)
        {
            this.title = title;
            this.messageContent = messageContent;
            this.messageType = messageType;
        }

        /**
         * Creates an instance of <tt>SystrayMessage</tt> by specifying the
         * message <tt>title</tt>, the content of the message, the type of
         * the message and the number of messages that this message has
         * aggregated.
         * 
         * @param messageContent the content of the message
         * @param aggregatedMessageNumber the number of messages that this
         * message has aggregated
         */
        public SystrayMessage(  String messageContent,
                                int aggregatedMessageNumber)
        {
            this.aggregatedMessageNumber = aggregatedMessageNumber;

            this.title = "You have received "
                            + aggregatedMessageNumber
                            + " new messages.";

            this.messageContent = messageContent;
            this.messageType = TrayIcon.INFO_MESSAGE_TYPE;
        }

        /**
         * Returns the title of the message.
         * 
         * @return the title of the message
         */
        public String getTitle()
        {
            return title;
        }

        /**
         * Returns the message content.
         * 
         * @return the message content
         */
        public String getMessageContent()
        {
            return messageContent;
        }

        /**
         * Returns the message type.
         * 
         * @return the message type
         */
        public int getMessageType()
        {
            return messageType;
        }

        /**
         * Returns the number of aggregated messages this message represents.
         * 
         * @return the number of aggregated messages this message represents.
         */
        public int getAggregatedMessageNumber()
        {
            return aggregatedMessageNumber;
        }

        /**
         * Adds the given number of messages to the number of aggregated
         * messages contained in this message.
         * 
         * @param messageNumber the number of messages to add to the number of
         * aggregated messages contained in this message
         */
        public void addAggregatedMessageNumber(int messageNumber)
        {
            this.aggregatedMessageNumber += messageNumber;

            this.title = "You have received " + aggregatedMessageNumber
                + " new messages.";
        }
    }
}
