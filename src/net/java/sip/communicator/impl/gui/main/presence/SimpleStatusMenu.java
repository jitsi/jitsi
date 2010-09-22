/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    private static final Logger logger
        = Logger.getLogger(SimpleStatusMenu.class);

    private final JMenuItem onlineItem;

    private final JMenuItem offlineItem;

    /**
     * Creates an instance of <tt>SimpleStatusMenu</tt>.
     * 
     * @param protocolProvider The protocol provider.
     */
    public SimpleStatusMenu(ProtocolProviderService protocolProvider)
    {
        this(protocolProvider,
            protocolProvider.getAccountID().getDisplayName(),
            ImageLoader.getBytesInImage(
                protocolProvider.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_16x16)));
    }

    private SimpleStatusMenu(ProtocolProviderService protocolProvider,
                             String displayName,
                             Image onlineImage)
    {
        super(displayName, new ImageIcon(onlineImage), protocolProvider);

        this.setToolTipText("<html><b>" + displayName
            + "</b><br>Offline</html>");

        JLabel titleLabel = new JLabel(displayName);

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        onlineItem = createMenuItem(
            "service.gui.ONLINE",
            getIcon(),
            Constants.ONLINE_STATUS);
        offlineItem = createMenuItem(
            "service.gui.OFFLINE",
            new ImageIcon(LightGrayFilter.createDisabledImage(onlineImage)),
            Constants.OFFLINE_STATUS);

        /*
         * Make sure it correctly depicts the status and don't just rely on it
         * being automatically updated.
         */
        updateStatus();
    }

    private JMenuItem createMenuItem(String textKey, Icon icon, String name)
    {
        JMenuItem menuItem =
            new JMenuItem(
                GuiActivator.getResources().getI18NString(textKey),
                icon);

        menuItem.setName(name);
        menuItem.addActionListener(this);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals(Constants.ONLINE_STATUS))
        {
            if (!protocolProvider.isRegistered())
            {
                GuiActivator.getUIService().getLoginManager().login(
                    protocolProvider);
            }
        }
        else
        {
            RegistrationState registrationState =
                protocolProvider.getRegistrationState();

            if (!registrationState.equals(RegistrationState.UNREGISTERED)
                && !registrationState.equals(RegistrationState.UNREGISTERING))
            {
                try
                {
                    GuiActivator.getUIService().getLoginManager()
                        .setManuallyDisconnected(true);
                    protocolProvider.unregister();
                }
                catch (OperationFailedException e1)
                {
                    logger.error("Unable to unregister the protocol provider: "
                        + protocolProvider
                        + " due to the following exception: " + e1);
                }
            }
        }

        saveStatusInformation(protocolProvider, itemName);
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus()
    {
        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        ImageIcon statusImage
            = ImageLoader.getAccountStatusImage(protocolProvider);
        JMenuItem menuItem
            = protocolProvider.isRegistered() ? onlineItem : offlineItem;

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
        setIcon(new ImageIcon(ImageLoader.getBytesInImage(
                protocolProvider.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_16x16))));
        onlineItem.setIcon(getIcon());
        offlineItem.setIcon(
            new ImageIcon(LightGrayFilter.createDisabledImage(
                ImageLoader.getBytesInImage(
                    protocolProvider.getProtocolIcon().getIcon(
                        ProtocolIcon.ICON_SIZE_16x16)))));
    }
}
