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
import net.java.sip.communicator.impl.gui.customcontrols.*;
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
 */
public class SimpleStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(SimpleStatusMenu.class);

    private final ProtocolProviderService protocolProvider;

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
        super(displayName, new ImageIcon(onlineImage));

        this.protocolProvider = protocolProvider;

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

        Image statusImage = ImageLoader.getAccountStatusImage(protocolProvider);

        if (protocolProvider.isRegistered())
        {
            setSelected(
                new SelectedObject(new ImageIcon(statusImage), onlineItem));

            // TODO Technically, we're not closing the html element.
            this.setToolTipText(tooltip.concat("<br>" + onlineItem.getText()));
        }
        else
        {
            setSelected(
                new SelectedObject(new ImageIcon(statusImage), offlineItem));

            this.setToolTipText(tooltip.concat("<br>" + offlineItem.getText()));
        }
    }

    public void updateStatus(Object status)
    {
    }
}
