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
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SimpleStatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that
 * contains two statuses ONLINE and OFFLINE. It's used to represent the status
 * of a protocol provider which doesn't support presence operation set.
 *
 * @author Yana Stamcheva
 */
public class SimpleStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    private Logger logger = Logger.getLogger(
            SimpleStatusMenu.class.getName());

    private MainFrame mainFrame;

    private ProtocolProviderService protocolProvider;

    private ImageIcon onlineIcon;

    private ImageIcon offlineIcon;

    private JMenuItem onlineItem = new JMenuItem(
            Messages.getI18NString("online").getText(),
            onlineIcon);

    private JMenuItem offlineItem = new JMenuItem(
            Messages.getI18NString("offline").getText(),
            offlineIcon);

    private JLabel titleLabel;

    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     *
     * @param mainFrame The main application window.
     * @param protocolProvider The protocol provider.
     * @param accountIndex If we have more than one account for a protocol,
     * each account has an index.
     */
    public SimpleStatusMenu(MainFrame mainFrame,
            ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getUserID(),
            new ImageIcon(protocolProvider
                .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));

        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;

        this.onlineIcon = new ImageIcon(
                ImageLoader.getBytesInImage(protocolProvider.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));

        this.offlineIcon = new ImageIcon(
            LightGrayFilter.createDisabledImage(
                ImageLoader.getBytesInImage(protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16))));

        String tooltip = "<html><b>"
            + protocolProvider.getAccountID().getUserID()
            + "</b><br>Offline</html>";

        this.setToolTipText(tooltip);

        onlineItem.setName(Constants.ONLINE_STATUS);
        offlineItem.setName(Constants.OFFLINE_STATUS);

        onlineItem.addActionListener(this);
        offlineItem.addActionListener(this);

        titleLabel = new JLabel(protocolProvider.getAccountID().getUserID());

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        this.add(onlineItem);
        this.add(offlineItem);

//        setSelected(offlineItem, offlineIcon);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if(itemName.equals(Constants.ONLINE_STATUS))
        {
            if(!protocolProvider.isRegistered()) {
                GuiActivator.getUIService().getLoginManager()
                    .login(protocolProvider);
            }
        }
        else
        {
            if(    !protocolProvider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERED)
                && !protocolProvider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERING))
            {
                try {
                    GuiActivator.getUIService().getLoginManager()
                        .setManuallyDisconnected(true);
                    protocolProvider.unregister();
                }
                catch (OperationFailedException e1) {
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

        SelectedObject selectedObject;
        if(protocolProvider.isRegistered())
        {
            selectedObject = new SelectedObject(onlineIcon, onlineItem);

            setSelected(selectedObject);

            this.setToolTipText(tooltip.concat("<br>" + onlineItem.getText()));
        }
        else
        {
            selectedObject = new SelectedObject(offlineIcon, offlineItem);

            setSelected(selectedObject);

            this.setToolTipText(tooltip.concat("<br>" + offlineItem.getText()));
        }
    }

    public void updateStatus(Object status)
    {}
}
