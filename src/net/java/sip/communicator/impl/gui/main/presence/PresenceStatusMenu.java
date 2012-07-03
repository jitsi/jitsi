/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.main.presence.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>StatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * the list of statuses for a protocol provider. This is where the user could
 * select its status.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class PresenceStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final Logger logger = Logger.getLogger(PresenceStatusMenu.class);

    private Iterator<PresenceStatus> statusIterator;

    private PresenceStatus offlineStatus;

    private PresenceStatus onlineStatus;

    private PresenceStatus lastSelectedStatus;

    private OperationSetPresence presence;

    private JLabel titleLabel;

    /**
     * Take care for global status items, that only one is selected.
     */
    private ButtonGroup group = new ButtonGroup();

    /**
     * Initializes a new <tt>PresenceStatusMenu</tt> instance which is to
     * depict and change the presence status of a specific
     * <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> which is to
     * have its presence status depicted and changed by the new instance
     */
    public PresenceStatusMenu(ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getDisplayName(),
            ImageLoader.getAccountStatusImage(protocolProvider),
            protocolProvider);

        this.presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

        this.statusIterator = this.presence.getSupportedStatusSet();

        String tooltip =
            "<html><b>" + protocolProvider.getAccountID().getDisplayName()
                + "</b><br>Connecting</html>";

        this.setToolTipText(tooltip);

        titleLabel = new JLabel(protocolProvider.getAccountID().getDisplayName());

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = statusIterator.next();
            int connectivity = status.getStatus();

            if (connectivity < 1)
            {
                this.offlineStatus = status;
            }
            else if ((onlineStatus != null
                    && (onlineStatus.getStatus() < connectivity))
                || (onlineStatus == null
                    && (connectivity > 50 && connectivity < 80)))
            {
                this.onlineStatus = status;
            }

            this.addItem(
                    status.getStatusName(),
                    new ImageIcon(status.getStatusIcon()),
                    this);
        }

        this.addSeparator();

        this.add(new StatusMessageMenu(protocolProvider));

        this.setSelectedStatus(offlineStatus);
        updateStatus(offlineStatus);
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     *
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     * case, when the item is selected.
     */
    public void addItem(String text, Icon icon, ActionListener actionListener)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, icon);
        item.setName(text);
        group.add(item);

        item.addActionListener(actionListener);

        this.add(item);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     *
     * @param e an <tt>ActionEvent</tt> which carries the data associated with
     * the performed action
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof JMenuItem)
        {
            String menuItemText = ((JMenuItem) e.getSource()).getText();

            Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

            while (statusSet.hasNext())
            {
                PresenceStatus status = statusSet.next();

                if (status.getStatusName().equals(menuItemText))
                {
                    GuiActivator.getGlobalStatusService()
                        .publishStatus(protocolProvider, status, true);

                    setSelectedStatus(status);

                    break;
                }
            }
        }
    }

    /**
     * Selects a specific <tt>PresenceStatus</tt> in this instance and the
     * <tt>ProtocolProviderService</tt> it depicts.
     *
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     * instance and the <tt>ProtocolProviderService</tt> it depicts
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        OperationSetPresence presence
            = MainFrame.getProtocolPresenceOpSet(protocolProvider);

        if (logger.isTraceEnabled())
            logger.trace("Update status for provider: "
            + protocolProvider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        this.setSelectedStatus(presenceStatus);

        if (protocolProvider.isRegistered()
                && !presence.getPresenceStatus().equals(presenceStatus))
        {
            GuiActivator.getGlobalStatusService()
                .publishStatus(protocolProvider, presenceStatus, false);
        }

        for(int i =0; i < getItemCount(); i++)
        {
            JMenuItem item = getItem(i);

            if(item instanceof JCheckBoxMenuItem)
            {
                if(item.getText().equals(presenceStatus.getStatusName()))
                    item.setSelected(true);
            }
        }
    }

    /**
     * Selects the given status in the status menu.
     *
     * @param status the status to select
     */
    public void setSelectedStatus(PresenceStatus status)
    {
        Icon statusImage = ImageLoader.getAccountStatusImage(protocolProvider);

        SelectedObject selectedObject
            = new SelectedObject(statusImage,
                                status.getStatusName());

        this.setSelected(selectedObject);

        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        this.setToolTipText(tooltip.concat("<br>" + status.getStatusName()));
    }

    /**
     * Returns the Offline status in this selector box.
     *
     * @return the Offline status in this selector box
     */
    public PresenceStatus getOfflineStatus()
    {
        return offlineStatus;
    }

    /**
     * Returns the Online status in this selector box.
     *
     * @return the Online status in this selector box
     */
    public PresenceStatus getOnlineStatus()
    {
        return onlineStatus;
    }

    /**
     * Returns the status that is currently selected.
     *
     * @return the status that is currently selected
     */
    public PresenceStatus getLastSelectedStatus()
    {
        return lastSelectedStatus;
    }

    /**
     * Loads resources for this component.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        this.setIcon(ImageLoader.getAccountStatusImage(protocolProvider));
    }
}
