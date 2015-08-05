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
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.presence.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import org.jitsi.util.*;

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
     * Property that controls whether we hide or show global status message
     * menu.
     */
    private static final String HIDE_GLOBAL_STATUS_MESSAGE
        = "net.java.sip.communicator.impl.gui.main.HIDE_GLOBAL_STATUS_MESSAGE";

    /**
     * The arrow icon shown on the right of the status and indicating that
     * this is a menu.
     */
    private Image arrowImage
        = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

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
     * The global status message menu.
     */
    private GlobalStatusMessageMenu globalStatusMessageMenu = null;

    /**
     * The parent panel that creates us.
     */
    private final AccountStatusPanel accountStatusPanel;

    /**
     * The index of the first status, the online one.
     */
    private int firstStatusIndex = -1;

    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     */
    public GlobalStatusSelectorBox(AccountStatusPanel accountStatusPanel)
    {
        super();

        this.accountStatusPanel = accountStatusPanel;

        JLabel titleLabel = new JLabel(GuiActivator.getResources()
                        .getI18NString("service.gui.SET_GLOBAL_STATUS"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        GlobalStatusEnum offlineStatus = GlobalStatusEnum.ONLINE;

        group.add(createMenuItem(offlineStatus, -1));

        firstStatusIndex = getItemCount();

        if(isPresenceOpSetForProvidersAvailable())
            addAvailableStatuses();

        group.add(createMenuItem(GlobalStatusEnum.OFFLINE, -1));

        if(!GuiActivator.getConfigurationService()
                .getBoolean(HIDE_GLOBAL_STATUS_MESSAGE, false)
            && isPresenceOpSetForProvidersAvailable())
        {
            this.addSeparator();

            globalStatusMessageMenu = new GlobalStatusMessageMenu(true);
            globalStatusMessageMenu.addPropertyChangeListener(
                new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        if(evt.getPropertyName().equals(
                            GlobalStatusMessageMenu.
                                STATUS_MESSAGE_UPDATED_PROP))
                        {
                            changeTooltip((String)evt.getNewValue());
                        }
                    }
                });
            this.add((JMenu)globalStatusMessageMenu.getMenu());
        }

        if(!ConfigurationUtils.isHideAccountStatusSelectorsEnabled())
            this.addSeparator();

        this.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));

        if(offlineStatus != null)
            this.setIcon(new ImageIcon(offlineStatus.getStatusIcon()));

        this.setIconTextGap(2);
        this.setOpaque(false);
        this.setText("Offline");
        changeTooltip(null);

        fitSizeToText();
    }

    /**
     * Adds the available global statuses. All the statuses except ONLINE and
     * OFFLINE, those that will be inserted between them.
     * Check first whether the statuses are not already inserted.
     */
    private void addAvailableStatuses()
    {
        if(hasAvailableStatuses())
            return;

        int index = firstStatusIndex;

        // creates menu item entry for every global status
        // except ONLINE and OFFLINE
        for(GlobalStatusEnum status : GlobalStatusEnum.globalStatusSet)
        {
            if(status.equals(GlobalStatusEnum.OFFLINE)
                || status.equals(GlobalStatusEnum.ONLINE))
                continue;

            group.add(createMenuItem(status, index++));
        }
    }

    /**
     * Removes the available global statuses (those except ONLINE and OFFLINE)
     */
    private void removeAvailableStatuses()
    {
        // removes menu item entry for every global status
        // except ONLINE and OFFLINE
        for(GlobalStatusEnum status : GlobalStatusEnum.globalStatusSet)
        {
            if(status.equals(GlobalStatusEnum.OFFLINE)
                || status.equals(GlobalStatusEnum.ONLINE))
                continue;

            JCheckBoxMenuItem item = getItemFromStatus(status);

            if(item == null)
                continue;

            group.remove(item);
            this.remove(item);
        }
    }

    /**
     * Check for available statuses we have in the menu.
     * All except ONLINE and OFFLINE one.
     * @return
     */
    private boolean hasAvailableStatuses()
    {
        // check menu item entries for every global status
        // except ONLINE and OFFLINE
        for(GlobalStatusEnum status : GlobalStatusEnum.globalStatusSet)
        {
            if(status.equals(GlobalStatusEnum.OFFLINE)
                || status.equals(GlobalStatusEnum.ONLINE))
                continue;

            if(getItemFromStatus(status) != null)
                return true;
        }

        return false;
    }

    /**
     * Changes the tooltip to default or the current set status message.
     * @param message
     */
    private void changeTooltip(String message)
    {
        if(StringUtils.isNullOrEmpty(message))
        {
            if(globalStatusMessageMenu != null)
                globalStatusMessageMenu.clearSelectedItems();

            this.setToolTipText("<html><b>" + GuiActivator.getResources()
                .getI18NString("service.gui.SET_GLOBAL_STATUS")
                + "</b></html>");
            accountStatusPanel.setStatusMessage(null);
        }
        else
        {
            this.setToolTipText("<html><b>" + message + "</b></html>");
            accountStatusPanel.setStatusMessage(message);
        }
    }

    /**
     * Creates a menu item with the given <tt>textKey</tt>, <tt>iconID</tt> and
     * <tt>name</tt>.
     *
     * @param status the global status
     * @param index index the position in the container's list at which to
     * insert the status, where <code>-1</code> means append to the end
     * @return the created <tt>JCheckBoxMenuItem</tt>
     */
    private JCheckBoxMenuItem createMenuItem(
        GlobalStatusEnum status,
        int index)
    {
        JCheckBoxMenuItem menuItem
            = new JCheckBoxMenuItem(
                    GlobalStatusEnum.getI18NStatusName(status),
                    new ImageIcon(status.getStatusIcon()));

        menuItem.setName(status.getStatusName());
        menuItem.addActionListener(this);

        if(index == -1)
            add(menuItem);
        else
            add(menuItem, index);

        return menuItem;
    }

    /**
     * Adds a status menu for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to add a status menu
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        if (protocolProvider.getAccountID().isHidden())
            return;

        OperationSetPersistentPresence presenceOpSet
            = (OperationSetPersistentPresence)
                protocolProvider.getOperationSet(OperationSetPresence.class);

        JMenuItem itemToAdd;

        if(protocolProvider.getAccountID().isStatusMenuHidden())
        {
            itemToAdd = new ReadonlyStatusItem(protocolProvider);
        }
        else
        {
            itemToAdd = (presenceOpSet != null)
                ? new PresenceStatusMenu(protocolProvider)
                : new SimpleStatusMenu(protocolProvider);

        }

        if(ConfigurationUtils.isHideAccountStatusSelectorsEnabled())
            itemToAdd.setVisible(false);

        // If this is the first account in our menu.
        if (isFirstAccount)
        {
            add(itemToAdd);
            isFirstAccount = false;

            // if we have a provider with opset presence add available statuses
            if(presenceOpSet != null)
                addAvailableStatuses();

            return;
        }

        boolean isMenuAdded = false;
        AccountID accountId = protocolProvider.getAccountID();
        // If we already have other accounts.
        for (Component c : getPopupMenu().getComponents())
        {
            if (!(c instanceof StatusEntry))
                continue;

            StatusEntry menu = (StatusEntry) c;
            int menuIndex = getPopupMenu().getComponentIndex(
                    menu.getEntryComponent());

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            int protocolCompare = accountId.getProtocolDisplayName().compareTo(
                menuAccountID.getProtocolDisplayName());

            // If the new account protocol name is before the name of the menu
            // we insert the new account before the given menu.
            if (protocolCompare < 0)
            {
                insert(itemToAdd, menuIndex);
                isMenuAdded = true;
                break;
            }
            else if (protocolCompare == 0)
            {
                // If we have the same protocol name, we check the account name.
                if (accountId.getDisplayName()
                        .compareTo(menuAccountID.getDisplayName()) < 0)
                {
                    insert( itemToAdd, menuIndex);
                    isMenuAdded = true;
                    break;
                }
            }
        }

        if (!isMenuAdded)
            add(itemToAdd);

        // if we have a provider with opset presence add available statuses
        if(presenceOpSet != null)
            addAvailableStatuses();
    }

    /**
     * Removes the status menu corresponding to the account given by
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * menu to remove
     */
    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        StatusEntry menu = getStatusEntry(protocolProvider);

        if (menu != null)
        {
            menu.dispose();
            remove(menu.getEntryComponent());
        }

        // if we do not have provider with presence opset
        // remove available statuses
        if(!isPresenceOpSetForProvidersAvailable())
            removeAvailableStatuses();
    }

    /**
     * Check the available providers for operation set presence.
     * @return do we have a provider with opset presence.
     */
    private boolean isPresenceOpSetForProvidersAvailable()
    {
        for (Component c : getPopupMenu().getComponents())
        {
            if(!(c instanceof StatusEntry))
                continue;

            StatusEntry menu = (StatusEntry) c;

            if(menu.getProtocolProvider()
                .getOperationSet(OperationSetPresence.class) != null)
                return true;
        }

        return false;
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
        return getStatusEntry(protocolProvider) != null;
    }

    /**
     * Starts connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to start
     * connecting for
     */
    public void startConnecting(ProtocolProviderService protocolProvider)
    {
        StatusEntry menu = getStatusEntry(protocolProvider);

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
        StatusEntry menu = getStatusEntry(protocolProvider);

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
            if (!(c instanceof StatusEntry))
                continue;

            StatusEntry menu = (StatusEntry) c;

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

        if(GuiActivator.getGlobalStatusService() != null)
        {
            GuiActivator.getGlobalStatusService().publishStatus(
                GlobalStatusEnum.getStatusByName(itemName));
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
        StatusEntry accountMenu = getStatusEntry(protocolProvider);

        if (accountMenu == null)
            return;

        PresenceStatus presenceStatus;

        if (!protocolProvider.isRegistered())
            presenceStatus = accountMenu.getOfflineStatus();
        else
        {
            presenceStatus
                = AccountStatusUtils.getPresenceStatus(protocolProvider);

            if (presenceStatus == null)
                presenceStatus = accountMenu.getOnlineStatus();
        }

        accountMenu.updateStatus(presenceStatus);

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
        StatusEntry accountMenu = getStatusEntry(protocolProvider);

        if (accountMenu == null)
            return;

        accountMenu.updateStatus(presenceStatus);

        this.updateGlobalStatus();
    }

    /**
     * Updates the global status by picking the most connected protocol provider
     * status.
     */
    private void updateGlobalStatus()
    {
        if(GuiActivator.getGlobalStatusService() == null)
            return;

        PresenceStatus globalStatus
            = GuiActivator.getGlobalStatusService().getGlobalPresenceStatus();

        JCheckBoxMenuItem item = getItemFromStatus(globalStatus);
        item.setSelected(true);

        setSelected(new SelectedObject(item.getText(), item.getIcon(), item));
        fitSizeToText();

        this.revalidate();
        setSystrayIcon(globalStatus);
        
        if(!globalStatus.isOnline())
        {
            changeTooltip(null);
        }
    }

    /**
     * Sets the systray icon corresponding to the given status.
     *
     * @param globalStatus the status, for which we're setting the systray icon.
     */
    private void setSystrayIcon(PresenceStatus globalStatus)
    {
        SystrayService trayService = GuiActivator.getSystrayService();
        if(trayService == null)
            return;

        int imgType = SystrayService.SC_IMG_OFFLINE_TYPE;

        if (globalStatus.equals(GlobalStatusEnum.OFFLINE))
        {
            imgType = SystrayService.SC_IMG_OFFLINE_TYPE;
        }
        else if (globalStatus.equals(GlobalStatusEnum.DO_NOT_DISTURB))
        {
            imgType = SystrayService.SC_IMG_DND_TYPE;
        }
        else if (globalStatus.equals(GlobalStatusEnum.AWAY))
        {
            imgType = SystrayService.SC_IMG_AWAY_TYPE;
        }
        else if (globalStatus.equals(GlobalStatusEnum.EXTENDED_AWAY))
        {
            imgType = SystrayService.SC_IMG_EXTENDED_AWAY_TYPE;
        }
        else if (globalStatus.equals(GlobalStatusEnum.ONLINE))
        {
            imgType = SystrayService.SC_IMG_TYPE;
        }
        else if (globalStatus.equals(GlobalStatusEnum.FREE_FOR_CHAT))
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
     * @param globalStatus the status to which the item should correspond
     * @return the <tt>JCheckBoxMenuItem</tt> corresponding to the given status
     */
    private JCheckBoxMenuItem getItemFromStatus(PresenceStatus globalStatus)
    {
        for(Component c : getMenuComponents())
        {
            if(c instanceof JCheckBoxMenuItem
                && globalStatus.getStatusName().equals(c.getName()))
            {
                return (JCheckBoxMenuItem) c;
            }
        }

        return null;
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
                : ComponentUtils.getStringWidth(this, text);

        this.setPreferredSize(new Dimension(
            textWidth + 2*IMAGE_INDENT + arrowImage.getWidth(null) + 5, 20));
    }

    /**
     * Returns the <tt>StatusEntry</tt> corresponding to the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * corresponding menu we're looking for
     * @return the <tt>StatusEntry</tt> corresponding to the given
     * <tt>protocolProvider</tt>
     */
    private StatusEntry getStatusEntry(ProtocolProviderService protocolProvider)
    {
        for (Component c : getPopupMenu().getComponents())
        {
            if (!(c instanceof StatusEntry))
                continue;

            StatusEntry menu = (StatusEntry) c;

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
    @Override
    public String getUIClassID()
    {
        return uiClassID;
    }

    /**
     * Not used.
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     */
    @Override
    public void updateStatus(PresenceStatus presenceStatus)
    {}
}
