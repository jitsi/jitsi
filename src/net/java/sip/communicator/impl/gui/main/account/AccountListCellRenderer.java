/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>AccountListCellRenderer</tt> is the cell renderer used from the 
 * {@link AccountList}.
 * 
 * @author Yana Stamcheva
 */
public class AccountListCellRenderer
    extends TransparentPanel
    implements ListCellRenderer
{
    private static final Color rowColor
        = new Color(GuiActivator.getResources()
                .getColor("service.gui.LIST_ROW"));

    private final JLabel accountLabel = new JLabel();

    private final JLabel statusLabel = new JLabel();

    private boolean isSelected = false;

    private int index;

    /**
     * Creates an instance of this cell renderer.
     */
    public AccountListCellRenderer()
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(100, 38));
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.add(accountLabel, BorderLayout.WEST);
        this.add(statusLabel, BorderLayout.EAST);
    }

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) value;

        Image protocolImage =
            ImageLoader.getBytesInImage(protocolProvider.getProtocolIcon()
                .getIcon(ProtocolIcon.ICON_SIZE_32x32));

        accountLabel.setIcon(new ImageIcon(protocolImage));

        accountLabel.setText(protocolProvider.getAccountID()
            .getDisplayName());

        ImageIcon statusImage
            = ImageLoader.getAccountStatusImage(protocolProvider);

        if (statusImage != null)
            statusLabel.setIcon(statusImage);

        String statusName = getAccountStatus(protocolProvider);

        if (statusName != null)
            statusLabel.setText(statusName);

        setEnabled(list.isEnabled());
        setFont(list.getFont());

        this.index = index;
        this.isSelected = isSelected;

        return this;
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (index%2 > 0)
        {
            g2.setColor(rowColor);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        }

        if (this.isSelected)
        {
            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }

    /**
     * Returns the current presence status of the given protocol provider.
     * 
     * @param protocolProvider the protocol provider which status we're looking
     * for.
     * @return the current presence status of the given protocol provider.
     */
    private String getAccountStatus(ProtocolProviderService protocolProvider)
    {
        String status;

        OperationSetPresence presence
            = (OperationSetPresence) protocolProvider
                .getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            status = presence.getPresenceStatus().getStatusName();
        }
        else
        {
            if (protocolProvider.isRegistered())
            {
                status = GuiActivator.getResources()
                    .getI18NString("service.gui.ONLINE");
            }
            else
            {
                status = GuiActivator.getResources()
                    .getI18NString("service.gui.OFFLINE");
            }
        }

        return status;
    }
}