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
        Account account = (Account) value;

        ImageIcon accountIcon = account.getIcon();

        if (accountIcon != null)
            accountLabel.setIcon(accountIcon);

        accountLabel.setText(account.getName());

        ImageIcon statusIcon = account.getStatusIcon();

        if (statusIcon != null)
            statusLabel.setIcon(statusIcon);

        String statusName = account.getStatusName();

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
}