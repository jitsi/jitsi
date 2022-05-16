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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;

/**
 * The <tt>SimpleStatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * two statuses ONLINE and OFFLINE. It's used to represent the status of a
 * protocol provider which doesn't support presence operation set.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class SimpleStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    private final JCheckBoxMenuItem onlineItem;

    private final JCheckBoxMenuItem offlineItem;

    /**
     * Take care for global status items, that only one is selected.
     */
    private ButtonGroup group = new ButtonGroup();

    /**
     * Creates an instance of <tt>SimpleStatusMenu</tt>.
     *
     * @param protocolProvider the protocol provider
     */
    public SimpleStatusMenu(ProtocolProviderService protocolProvider)
    {
        this(
                protocolProvider,
                protocolProvider.getAccountID().getDisplayName(),
                getProtocolImage(
                        protocolProvider,
                        ProtocolIcon.ICON_SIZE_16x16));
    }

    /**
     * Creates an instance of <tt>SimpleStatusMenu</tt>.
     *
     * @param protocolProvider the protocol provider
     * @param displayName the display name for the menu
     * @param onlineImage the image used for the online state
     */
    private SimpleStatusMenu(ProtocolProviderService protocolProvider,
                             String displayName,
                             Image onlineImage)
    {
        super(
                displayName,
                (onlineImage == null) ? null : new ImageIcon(onlineImage),
                protocolProvider);

        setToolTipText("<html><b>" + displayName + "</b><br>Offline</html>");

        JLabel titleLabel = new JLabel(displayName);

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        add(titleLabel);
        addSeparator();

        onlineItem = createMenuItem(
            "service.gui.ONLINE",
            getIcon(),
            GlobalStatusEnum.ONLINE_STATUS);
        offlineItem
            = createMenuItem(
                    "service.gui.OFFLINE",
                    (onlineImage == null)
                        ? null
                        : new ImageIcon(
                                LightGrayFilter.createDisabledImage(
                                        onlineImage)),
                    GlobalStatusEnum.OFFLINE_STATUS);
        group.add(onlineItem);
        group.add(offlineItem);

        /*
         * Make sure it correctly depicts the status and don't just rely on it
         * being automatically updated.
         */
        updateStatus(null);
    }

    private JCheckBoxMenuItem createMenuItem(
            String textKey,
            Icon icon,
            String name)
    {
        JCheckBoxMenuItem menuItem
            = new JCheckBoxMenuItem(
                    GuiActivator.getResources().getI18NString(textKey),
                    icon);

        menuItem.setName(name);
        menuItem.addActionListener(this);
        add(menuItem);
        return menuItem;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        if(GuiActivator.getGlobalStatusService() == null)
            return;

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals(GlobalStatusEnum.ONLINE_STATUS))
        {
            GuiActivator.getGlobalStatusService()
                .publishStatus(protocolProvider, GlobalStatusEnum.ONLINE);
        }
        else
        {
            GuiActivator.getGlobalStatusService()
                .publishStatus(protocolProvider, GlobalStatusEnum.OFFLINE);
        }
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     * @param presenceStatus as we do not support presence this param
     * will be null.
     */
    @Override
    public void updateStatus(PresenceStatus presenceStatus)
    {
        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        ImageIcon statusImage
            = ImageLoader.getAccountStatusImage(protocolProvider);
        JCheckBoxMenuItem menuItem
            = protocolProvider.isRegistered() ? onlineItem : offlineItem;
        menuItem.setSelected(true);

        setSelected(new SelectedObject(statusImage, menuItem));
        setToolTipText(tooltip.concat("<br>" + menuItem.getText()));
    }

    /**
     * Loads resources for this component.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        Image image
            = getProtocolImage(protocolProvider, ProtocolIcon.ICON_SIZE_16x16);

        if (image != null)
            setIcon(new ImageIcon(image));

        if (onlineItem != null)
            onlineItem.setIcon(getIcon());

        if ((offlineItem != null) && (image != null))
        {
            offlineItem.setIcon(
                    new ImageIcon(LightGrayFilter.createDisabledImage(image)));
        }
    }

    private static Image getProtocolImage(
            ProtocolProviderService pps,
            String iconSize)
    {
        byte[] bytes = pps.getProtocolIcon().getIcon(iconSize);

        return (bytes == null) ? null : ImageUtils.getBytesInImage(bytes);
    }
}
