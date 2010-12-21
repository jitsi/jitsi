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
import net.java.sip.communicator.service.resources.*;
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
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class GlobalStatusSelectorBox
    extends StatusSelectorMenu
    implements ActionListener
{
    /**
     * The indent of the image.
     */
    private static final int IMAGE_INDENT = 10;

    /**
     * The arrow icon shown on the right of the status and indicating that
     * this is a menu.
     */
    private Image arrowImage
        = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

    /**
     * The object used for logging.
     */
    private final Logger logger
        = Logger.getLogger(GlobalStatusSelectorBox.class);

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

//    private ImageIcon dndIcon = new ImageIcon(
//        ImageLoader.getImage(ImageLoader.USER_DND_ICON));

    /**
     * The item corresponding to the online status.
     */
    private final JMenuItem onlineItem;

    /**
     * The item corresponding to the offline status.
     */
    private final JMenuItem offlineItem;

    /**
     * The item corresponding to the away status.
     */
    private final JMenuItem awayItem;

    /**
     * The item corresponding to DnD status.
     */
    private final JMenuItem dndItem;

    /**
     * The item corresponding to the free for chat status.
     */
    private final JMenuItem ffcItem;

    /**
     * The width of the text.
     */
    private int textWidth = 0;

    /**
     * Indicates if this is the first account added.
     */
    private boolean isFirstAccount = true;

    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     *
     * @param mainFrame The main application window.
     */
    public GlobalStatusSelectorBox(MainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;

        JLabel titleLabel = new JLabel(GuiActivator.getResources()
                        .getI18NString("service.gui.SET_GLOBAL_STATUS"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        onlineItem
            = createMenuItem(
                "service.gui.ONLINE",
                ImageLoader.USER_ONLINE_ICON,
                Constants.ONLINE_STATUS);
        ffcItem
            = createMenuItem(
                "service.gui.FFC_STATUS",
                ImageLoader.USER_FFC_ICON,
                Constants.FREE_FOR_CHAT_STATUS);
        awayItem
            = createMenuItem(
                "service.gui.AWAY_STATUS",
                ImageLoader.USER_AWAY_ICON,
                Constants.AWAY_STATUS);
        dndItem
            = createMenuItem(
                "service.gui.DND_STATUS",
                ImageLoader.USER_DND_ICON,
                Constants.DO_NOT_DISTURB_STATUS);
        offlineItem
            = createMenuItem(
                "service.gui.OFFLINE",
                ImageLoader.USER_OFFLINE_ICON,
                Constants.OFFLINE_STATUS);

        this.addSeparator();

        this.setUI(new SIPCommStatusMenuUI());

        this.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        this.setIcon(offlineItem.getIcon());
        this.setIconTextGap(2);
        this.setOpaque(false);
        this.setText("Offline");
        this.setToolTipText("<html><b>" + GuiActivator.getResources()
                        .getI18NString("service.gui.SET_GLOBAL_STATUS")
                        + "</b></html>");

        fitSizeToText();
    }

    /**
     * Creates a menu item with the given <tt>textKey</tt>, <tt>iconID</tt> and
     * <tt>name</tt>.
     * @param textKey the text of the item
     * @param iconID the icon of the item
     * @param name the name of the item
     * @return the created <tt>JMenuItem</tt>
     */
    private JMenuItem createMenuItem(
        String textKey,
        ImageID iconID,
        String name)
    {
        JMenuItem menuItem
            = new JMenuItem(
                    GuiActivator.getResources().getI18NString(textKey),
                    new ImageIcon(ImageLoader.getImage(iconID)));

        menuItem.setName(name);
        menuItem.addActionListener(this);

        add(menuItem);

        return menuItem;
    }

    /**
     * Adds a status menu for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to add a status menu
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        boolean isHidden
            = protocolProvider
                    .getAccountID()
                        .getAccountProperty(
                            ProtocolProviderFactory.IS_PROTOCOL_HIDDEN)
                != null;

        if (isHidden)
            return;

        OperationSetPersistentPresence presenceOpSet
            = (OperationSetPersistentPresence)
                protocolProvider.getOperationSet(OperationSetPresence.class);
        StatusSelectorMenu statusSelectorMenu
            = (presenceOpSet != null)
                ? new PresenceStatusMenu(protocolProvider)
                : new SimpleStatusMenu(protocolProvider);

        // If this is the first account in our menu.
        if (isFirstAccount)
        {
            add(statusSelectorMenu);
            isFirstAccount = false;
            return;
        }

        boolean isMenuAdded = false;
        AccountID accountId = protocolProvider.getAccountID();
        // If we already have other accounts.
        for (Component c : getPopupMenu().getComponents())
        {
            if (!(c instanceof StatusSelectorMenu))
                continue;

            StatusSelectorMenu menu = (StatusSelectorMenu) c;
            int menuIndex = getPopupMenu().getComponentIndex(menu);

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            int protocolCompare = accountId.getProtocolDisplayName().compareTo(
                menuAccountID.getProtocolDisplayName());

            // If the new account protocol name is before the name of the menu
            // we insert the new account before the given menu.
            if (protocolCompare < 0)
            {
                insert(statusSelectorMenu, menuIndex);
                isMenuAdded = true;
                break;
            }
            else if (protocolCompare == 0)
            {
                // If we have the same protocol name, we check the account name.
                if (accountId.getDisplayName()
                        .compareTo(menuAccountID.getDisplayName()) < 0)
                {
                    insert( statusSelectorMenu, menuIndex);
                    isMenuAdded = true;
                    break;
                }
            }
        }

        if (!isMenuAdded)
            add(statusSelectorMenu);
    }

    /**
     * Removes the status menu corresponding to the account given by
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * menu to remove
     */
    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu menu = getStatusSelectorMenu(protocolProvider);

        if (menu != null)
            remove(menu);
    }

    /**
     * Checks if a menu for the given <tt>protocolProvider</tt> exists.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to check
     * @return <tt>true</tt> to indicate that a status menu for the given
     * <tt>protocolProvider</tt> already exists, otherwise returns
     * <tt>false</tt>
     */
    public boolean containsAccount(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu menu = getStatusSelectorMenu(protocolProvider);

        if (menu != null)
            return true;

        return false;
    }

    /**
     * Starts connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to start
     * connecting for
     */
    public void startConnecting(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu menu = getStatusSelectorMenu(protocolProvider);

        if (menu != null)
            menu.startConnecting();
    }

    /**
     * Stops connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to stop
     * connecting for
     */
    public void stopConnecting(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu menu = getStatusSelectorMenu(protocolProvider);

        if (menu != null)
            menu.stopConnecting();
    }

    /**
     * Returns <tt>true</tt> if there are selected status selector boxes,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if there are selected status selector boxes,
     * otherwise returns <tt>false</tt>
     */
    public boolean hasSelectedMenus()
    {
        for (Component c : getComponents())
        {
            if (!(c instanceof StatusSelectorMenu))
                continue;

            StatusSelectorMenu menu = (StatusSelectorMenu) c;

            if (menu.isSelected())
                return true;
        }
        return false;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        Iterator<ProtocolProviderService> pProviders
            = mainFrame.getProtocolProviders();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = pProviders.next();

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
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                onlineItem.getName());

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet
                        = presence.getSupportedStatusSet();

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
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                offlineItem.getName());

                        GuiActivator.getUIService().getLoginManager()
                            .logoff(protocolProvider);

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet
                        = presence.getSupportedStatusSet();

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
                // we search for highest available status here
                publishStatus(
                        protocolProvider,
                        PresenceStatus.AVAILABLE_THRESHOLD,
                        PresenceStatus.MAX_STATUS_VALUE);
            }
            else if (itemName.equals(Constants.DO_NOT_DISTURB_STATUS))
            {
                // status between online and away is DND
                publishStatus(
                        protocolProvider,
                        PresenceStatus.ONLINE_THRESHOLD,
                        PresenceStatus.AWAY_THRESHOLD);
            }
            else if (itemName.equals(Constants.AWAY_STATUS))
            {
                // a status in the away interval
                publishStatus(
                        protocolProvider,
                        PresenceStatus.AWAY_THRESHOLD,
                        PresenceStatus.AVAILABLE_THRESHOLD);
            }
        }
    }

    /**
     * Publish present status. We search for the highest value in the
     * given interval.
     * 
     * @param protocolProvider the protocol provider to which we
     * change the status.
     * @param floorStatusValue the min status value.
     * @param ceilStatusValue the max status value.
     */
    private void publishStatus(
            ProtocolProviderService protocolProvider,
            int floorStatusValue, int ceilStatusValue)
    {
        if (!protocolProvider.isRegistered())
            return;

        OperationSetPresence presence
            = protocolProvider
                .getOperationSet(OperationSetPresence.class);

        if (presence == null)
            return;

        Iterator<PresenceStatus> statusSet
            = presence.getSupportedStatusSet();

        PresenceStatus status = null;

        while (statusSet.hasNext())
        {
            PresenceStatus currentStatus = statusSet.next();

            if (status == null
                && currentStatus.getStatus() < ceilStatusValue
                && currentStatus.getStatus() >= floorStatusValue)
            {
                status = currentStatus;
            }

            if (status != null)
            {
                if (currentStatus.getStatus() < ceilStatusValue
                    && currentStatus.getStatus() >= floorStatusValue
                    && currentStatus.getStatus() > status.getStatus())
                {
                    status = currentStatus;
                }
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

    /**
     * Updates the status of the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the menu to update
     */
    public void updateStatus(ProtocolProviderService protocolProvider)
    {
        StatusSelectorMenu accountMenu = getStatusSelectorMenu(protocolProvider);

        if (accountMenu == null)
            return;

        if (accountMenu instanceof PresenceStatusMenu)
        {
            PresenceStatusMenu presenceStatusMenu
                = (PresenceStatusMenu) accountMenu;
            PresenceStatus presenceStatus;

            if (!protocolProvider.isRegistered())
                presenceStatus = presenceStatusMenu.getOfflineStatus();
            else
            {
                presenceStatus = presenceStatusMenu.getLastSelectedStatus();
                if (presenceStatus == null)
                {
                    presenceStatus = getLastPresenceStatus(protocolProvider);
                    if (presenceStatus == null)
                        presenceStatus = presenceStatusMenu.getOnlineStatus();
                }
            }

            presenceStatusMenu.updateStatus(presenceStatus);
        }
        else
        {
            ((SimpleStatusMenu) accountMenu).updateStatus();
        }

        accountMenu.repaint();

        this.updateGlobalStatus();
    }

    /**
     * Updates the status of the given <tt>protocolProvider</tt> with the given
     * <tt>presenceStatus</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the menu to update
     * @param presenceStatus the new status to set
     */
    public void updateStatus(ProtocolProviderService protocolProvider,
                             PresenceStatus presenceStatus)
    {
        StatusSelectorMenu accountMenu = getStatusSelectorMenu(protocolProvider);

        if (accountMenu == null)
            return;

        if (accountMenu instanceof PresenceStatusMenu)
            ((PresenceStatusMenu) accountMenu).updateStatus(presenceStatus);

        this.updateGlobalStatus();
    }

    /**
     * Updates the global status by picking the most connected protocol provider
     * status.
     */
    private void updateGlobalStatus()
    {
        int status = 0;

        Iterator<ProtocolProviderService> pProviders
            = mainFrame.getProtocolProviders();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider = pProviders.next();

            // We do not show hidden protocols in our status bar, so we do not
            // care about their status here.
            boolean isProtocolHidden =
                protocolProvider.getAccountID().getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

            if (isProtocolHidden)
                continue;

            if (!protocolProvider.isRegistered())
                continue;

            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);
            int presenceStatus
                = (presence == null)
                    ? PresenceStatus.AVAILABLE_THRESHOLD
                    : presence.getPresenceStatus().getStatus();

            if (status < presenceStatus)
                status = presenceStatus;
        }

        JMenuItem item = getItemFromStatus(status);

        setSelected(new SelectedObject(item.getText(), item.getIcon(), item));
        fitSizeToText();

        this.revalidate();
        setSystrayIcon(status);
    }

    /**
     * Sets the systray icon corresponding to the given status.
     * 
     * @param status the status, for which we're setting the systray icon.
     */
    private void setSystrayIcon(int status)
    {
        SystrayService trayService = GuiActivator.getSystrayService();
        if(trayService == null)
            return;

        int imgType = SystrayService.SC_IMG_OFFLINE_TYPE;

        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            imgType = SystrayService.SC_IMG_OFFLINE_TYPE;
        }
        else if(status < PresenceStatus.AWAY_THRESHOLD)
        {
            imgType = SystrayService.SC_IMG_DND_TYPE;
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

        trayService.setSystrayIcon(imgType);
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
         * Publishes the given <tt>status</tt> through the given
         * <tt>presence</tt> operation set.
         * @param presence the operation set through which we publish the status
         * @param status the status to publish
         */
        public PublishPresenceStatusThread( OperationSetPresence presence,
                                            PresenceStatus status)
        {
            this.presence = presence;
            this.status = status;
        }

        @Override
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
        else if(status < PresenceStatus.AWAY_THRESHOLD)
        {
            return dndItem;
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

        if (lastStatus != null)
        {
            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if (presence == null)
                return null;

            Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
            PresenceStatus status;

            while (i.hasNext())
            {
                status = i.next();
                if (status.getStatusName().equals(lastStatus))
                    return status;
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
        // find the last contact status saved in the configuration.
        String lastStatus = null;

        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts
            = configService.getPropertyNamesByPrefix(prefix, true);
        String protocolProviderAccountUID
            = protocolProvider.getAccountID().getAccountUniqueID();

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProviderAccountUID))
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
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (textWidth != 0)
        {
            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.drawImage(
                    arrowImage,
                    textWidth + 2*IMAGE_INDENT + 2,
                    (this.getHeight() - arrowImage.getHeight(null)) / 2 + 3,
                    null);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Computes the width of the text in pixels in order to position the arrow
     * during its painting.
     */
    private void fitSizeToText()
    {
        String text = getText();

        textWidth
            = (text == null)
                ? 0
                : GuiUtils.getStringWidth(this, text);

        this.setPreferredSize(new Dimension(
            textWidth + 2*IMAGE_INDENT + arrowImage.getWidth(null) + 5, 20));
    }

    /**
     * Returns the <tt>StatusSelectorMenu</tt> corresponding to the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * corresponding menu we're looking for
     * @return the <tt>StatusSelectorMenu</tt> corresponding to the given
     * <tt>protocolProvider</tt>
     */
    private StatusSelectorMenu getStatusSelectorMenu(
                                    ProtocolProviderService protocolProvider)
    {
        for (Component c : getPopupMenu().getComponents())
        {
            if (!(c instanceof StatusSelectorMenu))
                continue;

            StatusSelectorMenu menu = (StatusSelectorMenu) c;

            if (menu.getProtocolProvider() != null
                && menu.getProtocolProvider().equals(protocolProvider))
                return menu;
        }
        return null;
    }

    /**
     * Loads all icons and updates global status.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        arrowImage
            = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

        onlineItem.setIcon(new ImageIcon(ImageLoader.getImage(
                ImageLoader.USER_ONLINE_ICON)));

        ffcItem.setIcon(new ImageIcon(ImageLoader.getImage(
                ImageLoader.USER_FFC_ICON)));

        dndItem.setIcon(new ImageIcon(ImageLoader.getImage(
                ImageLoader.USER_DND_ICON)));

        awayItem.setIcon(new ImageIcon(ImageLoader.getImage(
                ImageLoader.USER_AWAY_ICON)));

        offlineItem.setIcon(new ImageIcon(ImageLoader.getImage(
                ImageLoader.USER_OFFLINE_ICON)));

        updateGlobalStatus();
    }
}
