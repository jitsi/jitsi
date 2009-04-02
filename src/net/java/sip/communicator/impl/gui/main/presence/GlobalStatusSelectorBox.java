/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>GlobalStatusSelectorBox</tt> is a global status selector box, which
 * appears in the status panel, when the user has more than one account. It
 * allows to the user to change globally its status with only one click, instead
 * of going through all accounts and selecting the desired status for every one
 * of them.
 * <p>
 * By default the <tt>GlobalStatusSelectorBox</tt> will show the most connected
 * status of all registered accounts.
 * 
 * @author Yana Stamcheva
 */
public class GlobalStatusSelectorBox
    extends StatusSelectorMenu
    implements ActionListener
{
    private int IMAGE_INDENT = 10;

    private Image arrowImage = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

    private Logger logger = Logger.getLogger(
        GlobalStatusSelectorBox.class.getName());

    private Hashtable accountMenus = new Hashtable();

    private MainFrame mainFrame;

    private ImageIcon onlineIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON));

    private ImageIcon offlineIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON));

    private ImageIcon awayIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_AWAY_ICON));

//    private ImageIcon dndIcon = new ImageIcon(
//        ImageLoader.getImage(ImageLoader.USER_DND_ICON));

    private ImageIcon ffcIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_FFC_ICON));

    private JMenuItem onlineItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.ONLINE"),
        onlineIcon);

    private JMenuItem offlineItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.OFFLINE"),
        offlineIcon);

    private JMenuItem awayItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.AWAY_STATUS"),
        awayIcon);

//    private JMenuItem dndItem = new JMenuItem(
//        GuiActivator.getResources().getI18NString("service.gui.DND_STATUS").getText(),
//        dndIcon);

    private JMenuItem ffcItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.FFC_STATUS"),
        ffcIcon);

    private JLabel titleLabel;

    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     *
     * @param mainFrame The main application window.
     */
    public GlobalStatusSelectorBox(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setUI(new SIPCommStatusMenuUI());
        this.setOpaque(false);

        this.setText("Offline");
        this.setIcon(offlineIcon);

        String tooltip = "<html><b>"
            + "Set global status"
            + "</b></html>";

        this.setToolTipText(tooltip);

        onlineItem.setName(Constants.ONLINE_STATUS);
        offlineItem.setName(Constants.OFFLINE_STATUS);
        awayItem.setName(Constants.AWAY_STATUS);
        ffcItem.setName(Constants.FREE_FOR_CHAT_STATUS);

        onlineItem.addActionListener(this);
        offlineItem.addActionListener(this);
        awayItem.addActionListener(this);
        ffcItem.addActionListener(this);

        titleLabel = new JLabel("Set global status");

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        this.add(onlineItem);
        this.add(ffcItem);
        this.add(awayItem);
        this.add(offlineItem);
        this.addSeparator();
    }

    public void addAccount(ProtocolProviderService protocolProvider)
    {
        OperationSetPersistentPresence presenceOpSet
            = (OperationSetPersistentPresence) protocolProvider
                .getOperationSet(OperationSetPresence.class);

        boolean isHidden =
            protocolProvider.getAccountID().getAccountProperty(
                ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

        if (isHidden)
            return;

        StatusSelectorMenu statusSelectorMenu = null;

        if (presenceOpSet != null)
        {
            statusSelectorMenu
                = new PresenceStatusMenu(mainFrame, protocolProvider);
        }
        else
        {
            statusSelectorMenu = new SimpleStatusMenu(protocolProvider);
        }

        this.add(statusSelectorMenu);

        this.accountMenus.put(protocolProvider, statusSelectorMenu);
    }

    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu statusSelectorMenu
            = (StatusSelectorMenu) this.accountMenus.get(protocolProvider);

        this.remove(statusSelectorMenu);

        this.accountMenus.remove(protocolProvider);
    }

    public boolean containsAccount(ProtocolProviderService protocolProvider)
    {
        return accountMenus.containsKey(protocolProvider);
    }

    /**
     * Returns TRUE if there are selected status selector boxes, otherwise
     * returns FALSE.
     */
    public boolean hasSelectedMenus()
    {
        Enumeration statusMenus = accountMenus.elements();

        while (statusMenus.hasMoreElements())
        {
            StatusSelectorMenu statusSelectorMenu =
                (StatusSelectorMenu) statusMenus.nextElement();

            if (statusSelectorMenu.isSelected())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        Iterator pProviders = mainFrame.getProtocolProviders();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) pProviders.next();

            if(itemName.equals(Constants.ONLINE_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(  protocolProvider,
                        onlineItem.getName());

                    GuiActivator.getUIService().getLoginManager()
                        .login(protocolProvider);
                }
                else
                {
                    OperationSetPresence presence
                        = (OperationSetPresence) protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                onlineItem.getName());

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status = statusSet.next();

                        if( status.getStatus()
                                < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD
                            && status.getStatus()
                                >= PresenceStatus.AVAILABLE_THRESHOLD)
                        {
                            new PublishPresenceStatusThread(presence, status)
                                .start();

                            this.saveStatusInformation( protocolProvider,
                                                        status.getStatusName());

                            break;
                        }
                    }
                }
            }
            else if (itemName.equals(Constants.OFFLINE_STATUS))
            {
                if(    !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERED)
                    && !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERING))
                {
                    OperationSetPresence presence
                        = (OperationSetPresence) protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                offlineItem.getName());

                        GuiActivator.getUIService().getLoginManager()
                            .logoff(protocolProvider);

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status = statusSet.next();

                        if(status.getStatus()
                            < PresenceStatus.ONLINE_THRESHOLD)
                        {
                            this.saveStatusInformation( protocolProvider,
                                status.getStatusName());

                            break;
                        }
                    }

                    try 
                    {
                        GuiActivator.getUIService().getLoginManager()
                            .setManuallyDisconnected(true);

                        protocolProvider.unregister();
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error(
                            "Unable to unregister the protocol provider: "
                            + protocolProvider
                            + " due to the following exception: " + e1);
                    }
                }
            }
            else if (itemName.equals(Constants.FREE_FOR_CHAT_STATUS))
            {
                if (!protocolProvider.isRegistered())
                    continue;

                OperationSetPresence presence
                    = (OperationSetPresence) protocolProvider
                        .getOperationSet(OperationSetPresence.class);

                if (presence == null)
                    continue;

                Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

                PresenceStatus status = null;

                while (statusSet.hasNext())
                {
                    PresenceStatus currentStatus = statusSet.next();

                    if (status == null)
                        status = currentStatus; 

                    if(status.getStatus() < currentStatus.getStatus())
                    {
                        status = currentStatus;
                    }
                }

                if (status != null)
                {
                    new PublishPresenceStatusThread(presence, status)
                        .start();

                    this.saveStatusInformation( protocolProvider,
                        status.getStatusName());
                }
            }
            else if (itemName.equals(Constants.AWAY_STATUS))
            {
                if (!protocolProvider.isRegistered())
                    continue;

                OperationSetPresence presence
                    = (OperationSetPresence) protocolProvider
                        .getOperationSet(OperationSetPresence.class);

                if (presence == null)
                    continue;

                Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

                PresenceStatus status = null;

                while (statusSet.hasNext())
                {
                    PresenceStatus currentStatus = statusSet.next();

                    if (status == null
                        && currentStatus.getStatus()
                            < PresenceStatus.AVAILABLE_THRESHOLD
                        && currentStatus.getStatus()
                            >= PresenceStatus.ONLINE_THRESHOLD)
                    {
                        status = currentStatus;
                    }

                    if (currentStatus.getStatus()
                            < PresenceStatus.AVAILABLE_THRESHOLD
                        && currentStatus.getStatus()
                            >= PresenceStatus.ONLINE_THRESHOLD
                        && currentStatus.getStatus() > status.getStatus())
                    {
                        status = currentStatus;
                    }
                }

                if (status != null)
                {
                    new PublishPresenceStatusThread(presence, status)
                        .start();

                    this.saveStatusInformation( protocolProvider,
                        status.getStatusName());
                }
            }
        }
    }

    public void updateStatus(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu accountMenu =
            (StatusSelectorMenu) accountMenus.get(protocolProvider);

        if (accountMenu == null)
            return;

        if (accountMenu instanceof PresenceStatusMenu)
        {
            PresenceStatusMenu presenceStatusMenu =
                (PresenceStatusMenu) accountMenu;

            if (!protocolProvider.isRegistered())
                presenceStatusMenu.updateStatus(presenceStatusMenu
                    .getOfflineStatus());
            else
            {
                if (presenceStatusMenu.getLastSelectedStatus() != null)
                {
                    presenceStatusMenu.updateStatus(presenceStatusMenu
                        .getLastSelectedStatus());
                }
                else
                {
                    PresenceStatus lastStatus =
                        getLastPresenceStatus(protocolProvider);

                    if (lastStatus == null)
                    {
                        presenceStatusMenu.updateStatus(presenceStatusMenu
                            .getOnlineStatus());
                    }
                    else
                    {
                        presenceStatusMenu.updateStatus(lastStatus);
                    }
                }
            }
        }
        else
        {
            ((SimpleStatusMenu) accountMenu).updateStatus();
        }

        accountMenu.repaint();

        this.updateGlobalStatus();
    }

    public void updateStatus(ProtocolProviderService protocolProvider,
                            PresenceStatus presenceStatus)
    {
        StatusSelectorMenu accountMenu =
            (StatusSelectorMenu) accountMenus.get(protocolProvider);

        if (accountMenu == null)
            return;

        if (accountMenu instanceof PresenceStatusMenu)
        {
            PresenceStatusMenu presenceStatusMenu =
                (PresenceStatusMenu) accountMenu;

            presenceStatusMenu.updateStatus(presenceStatus);
        }

        this.updateGlobalStatus();
    }

    /**
     * Updates the global status by picking the most connected protocol provider
     * status.
     */
    private void updateGlobalStatus()
    {
        int status = 0;

        Iterator pProviders = mainFrame.getProtocolProviders();

        boolean isProtocolHidden;

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) pProviders.next();

            // We do not show hidden protocols in our status bar, so we do not
            // care about their status here.
            isProtocolHidden =
                protocolProvider.getAccountID().getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

            if (isProtocolHidden)
                continue;

            OperationSetPresence presence
                = (OperationSetPresence) protocolProvider
                    .getOperationSet(OperationSetPresence.class);

            if (presence == null)
            {
                if (protocolProvider.isRegistered()
                    && status < PresenceStatus.AVAILABLE_THRESHOLD)
                {
                    status = PresenceStatus.AVAILABLE_THRESHOLD;
                }
            }
            else
            {
                if (protocolProvider.isRegistered()
                    && status < presence.getPresenceStatus().getStatus())
                {
                    status = presence.getPresenceStatus().getStatus();
                }
            }
        }

        JMenuItem item = getItemFromStatus(status);

        SelectedObject selectedObject
            = new SelectedObject(item.getText(),
                                (ImageIcon)item.getIcon(),
                                item);

        setSelected(selectedObject);

        this.revalidate();
        setSystrayIcon(status);
    }

    private void setSystrayIcon(int status)
    {
        SystrayService trayServce = GuiActivator.getSystrayService();
        if(trayServce == null)
            return;

        int imgType = SystrayService.SC_IMG_OFFLINE_TYPE;
        
        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            imgType = SystrayService.SC_IMG_OFFLINE_TYPE;
        }
        else if(status < PresenceStatus.AVAILABLE_THRESHOLD)
        {
            imgType = SystrayService.SC_IMG_AWAY_TYPE;
        }
        else if(status < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            imgType = SystrayService.SC_IMG_TYPE;
        }
        else if(status < PresenceStatus.MAX_STATUS_VALUE)
        {
            imgType = SystrayService.SC_IMG_FFC_TYPE;
        }

        trayServce.setSystrayIcon(imgType);
    }

    /**
     * Publishes the given status to the given presence operation set.
     */
    private class PublishPresenceStatusThread
        extends Thread
    {
        private PresenceStatus status;

        private OperationSetPresence presence;

        /**
         * 
         * @param presence
         * @param status
         */
        public PublishPresenceStatusThread( OperationSetPresence presence,
                                            PresenceStatus status)
        {
            this.presence = presence;
            this.status = status;
        }

        public void run()
        {
            try
            {
                presence.publishPresenceStatus(status, "");
            }
            catch (IllegalArgumentException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_GENERAL_ERROR");

                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                        "service.gui.GENERAL_ERROR"), msgText, e1)
                    .showDialog();
                }
                else if (e1.getErrorCode()
                    == OperationFailedException.NETWORK_FAILURE)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE");

                    new ErrorDialog(null, msgText,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NETWORK_FAILURE"), e1)
                    .showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE");

                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                        "service.gui.NETWORK_FAILURE"), msgText, e1)
                    .showDialog();
                }
                logger.error("Error - changing status", e1);
            }
        }
    }

    /**
     * Returns the <tt>JMenuItem</tt> corresponding to the given status. For
     * status constants we use here the values defined in the
     * <tt>PresenceStatus</tt>, but this is only for convenience.
     * 
     * @param status the status to which the item should correspond
     * @return the <tt>JMenuItem</tt> corresponding to the given status
     */
    private JMenuItem getItemFromStatus(int status)
    {
        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            return offlineItem;
        }
        else if(status < PresenceStatus.AVAILABLE_THRESHOLD)
        {
            return awayItem;
        }
        else if(status < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            return onlineItem;
        }
        else if(status < PresenceStatus.MAX_STATUS_VALUE)
        {
            return ffcItem;
        }
        else
        {
            return offlineItem;
        }
    }

    /**
     * Returns the last status that was stored in the configuration xml for the
     * given protocol provider.
     * 
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration xml for the
     *         given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {
        String lastStatus = getLastStatusString(protocolProvider);

        OperationSetPresence presence =
            (OperationSetPresence) protocolProvider
                .getOperationSet(OperationSetPresence.class);

        if (presence == null)
            return null;

        Iterator<PresenceStatus> i = presence.getSupportedStatusSet();

        if (lastStatus != null)
        {
            PresenceStatus status;
            while (i.hasNext())
            {
                status = i.next();
                if (status.getStatusName().equals(lastStatus))
                {
                    return status;
                }
            }
        }
        return null;
    }

    /**
     * Returns the last contact status saved in the configuration.
     * 
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        ConfigurationService configService =
            GuiActivator.getConfigurationService();

        // find the last contact status saved in the configuration.
        String lastStatus = null;

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts
            = configService.getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {
                lastStatus =
                    configService.getString(accountRootPropName
                        + ".lastAccountStatus");

                if (lastStatus != null)
                    break;
            }
        }

        return lastStatus;
    }

    /**
     * Overwrites the <tt>paintComponent(Graphics g)</tt> method in order to
     * provide a new look and the mouse moves over this component.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            SelectedObject selected = (SelectedObject) this.getSelected();

            int stringWidth = SwingUtilities.computeStringWidth(
                this.getFontMetrics(this.getFont()), selected.getText());

            g.drawImage(arrowImage, stringWidth + 2*IMAGE_INDENT + 2,
                (this.getHeight() - arrowImage.getHeight(null)) / 2 + 3, null);
        }
        finally
        {
            g.dispose();
        }
    }
}
