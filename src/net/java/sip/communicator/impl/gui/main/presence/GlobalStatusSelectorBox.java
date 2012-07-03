/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.configuration.*;

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
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        GlobalStatusSelectorBox.class.getName() +  "StatusMenuUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommStatusMenuUI.class.getName());
    }

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

    /**
     * The width of the text.
     */
    private int textWidth = 0;

    /**
     * Indicates if this is the first account added.
     */
    private boolean isFirstAccount = true;

    /**
     * Take care for global status items, that only one is selected.
     */
    private ButtonGroup group = new ButtonGroup();

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

        PresenceStatus offlineStatus = null;
        // creates menu item entry for every global status
        for(GlobalStatusEnum status : GlobalStatusEnum.globalStatusSet)
        {
            group.add(createMenuItem(status));

            if(status.getStatus() < 1)
                offlineStatus = status;
        }

        if(!ConfigurationManager.isHideAccountStatusSelectorsEnabled())
            this.addSeparator();

        this.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));

        if(offlineStatus != null)
            this.setIcon(new ImageIcon(offlineStatus.getStatusIcon()));

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
     * @return the created <tt>JCheckBoxMenuItem</tt>
     */
    private JCheckBoxMenuItem createMenuItem(GlobalStatusEnum status)
    {
        JCheckBoxMenuItem menuItem
            = new JCheckBoxMenuItem(
                    GlobalStatusEnum.getI18NStatusName(status),
                    new ImageIcon(status.getStatusIcon()));

        menuItem.setName(status.getStatusName());
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

        if(ConfigurationManager.isHideAccountStatusSelectorsEnabled())
            statusSelectorMenu.setVisible(false);

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

        GuiActivator.getGlobalStatusService().publishStatus(
            GlobalStatusEnum.getStatusByName(itemName));
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

        JCheckBoxMenuItem item = getItemFromStatus(status);
        item.setSelected(true);

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
     * Returns the <tt>JCheckBoxMenuItem</tt> corresponding to the given status.
     * For status constants we use here the values defined in the
     * <tt>PresenceStatus</tt>, but this is only for convenience.
     *
     * @param status the status to which the item should correspond
     * @return the <tt>JCheckBoxMenuItem</tt> corresponding to the given status
     */
    private JCheckBoxMenuItem getItemFromStatus(int status)
    {
        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            return getItemFromName(GlobalStatusEnum.OFFLINE_STATUS);
        }
        else if(status < PresenceStatus.AWAY_THRESHOLD)
        {
            return getItemFromName(GlobalStatusEnum.DO_NOT_DISTURB_STATUS);
        }
        else if(status < PresenceStatus.AVAILABLE_THRESHOLD)
        {
            return getItemFromName(GlobalStatusEnum.AWAY_STATUS);
        }
        else if(status < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            return getItemFromName(GlobalStatusEnum.ONLINE_STATUS);
        }
        else if(status < PresenceStatus.MAX_STATUS_VALUE)
        {
            return getItemFromName(GlobalStatusEnum.FREE_FOR_CHAT_STATUS);
        }
        else
        {
            return getItemFromName(GlobalStatusEnum.OFFLINE_STATUS);
        }
    }

    /**
     * Returns the <tt>JCheckBoxMenuItem</tt> corresponding to the given status
     * name.
     *
     * @param statusName the status name to which the item should correspond
     * @return the <tt>JCheckBoxMenuItem</tt> corresponding to the given status
     * name.
     */
    private JCheckBoxMenuItem getItemFromName(String statusName)
    {
        for(Component c : getMenuComponents())
        {
            if(c instanceof JCheckBoxMenuItem
                && statusName.equals(c.getName()))
            {
                return (JCheckBoxMenuItem)c;
            }
        }

        return null;
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
        return GuiActivator.getGlobalStatusService()
            .getLastPresenceStatus(protocolProvider);
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
        return GuiActivator.getGlobalStatusService()
            .getLastStatusString(protocolProvider);
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
                    getX()
                        + (this.getHeight() - arrowImage.getHeight(null)) / 2
                        + 1,
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

        updateGlobalStatus();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }
}
