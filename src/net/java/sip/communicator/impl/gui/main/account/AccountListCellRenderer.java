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

    private final JPanel westPanel = new TransparentPanel(new BorderLayout());

    private final JLabel accountLabel = new JLabel();

    private final JLabel statusLabel = new JLabel();

    private boolean isSelected = false;

    private Account account;

    private int index;

    /**
     * Creates an instance of this cell renderer.
     */
    public AccountListCellRenderer()
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(100, 38));
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        accountLabel.setFont(accountLabel.getFont().deriveFont(Font.BOLD));
        westPanel.add(accountLabel, BorderLayout.CENTER);

        add(westPanel, BorderLayout.WEST);
        add(statusLabel, BorderLayout.EAST);
    }

    /**
     * Returns the renderer component for the cell given by all the parameters.
     * @param list the parent list
     * @param value the value of the cell
     * @param index the index of the cell
     * @param isSelected indicates if the cell is selected
     * @param cellHasFocus indicates if the cell has the focus
     * @return the component rendering the cell
     */
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
        this.account = (Account) value;

        Icon accountIcon = account.getIcon();

        if (accountIcon != null)
            accountLabel.setIcon(accountIcon);

        accountLabel.setText(account.getName());

        Icon statusIcon = account.getStatusIcon();

        if (statusIcon != null)
            statusLabel.setIcon(statusIcon);

        String statusName = account.getStatusName();

        if (statusName != null)
            statusLabel.setText(statusName);

        setEnabled(list.isEnabled());
        setFont(list.getFont());

        if (!account.isEnabled())
        {
            accountLabel.setForeground(Color.GRAY);
            statusLabel.setForeground(Color.GRAY);
        }
        else
        {
            accountLabel.setForeground(Color.BLACK);
            statusLabel.setForeground(Color.BLACK);
        }

        this.index = index;
        this.isSelected = isSelected;

        this.setBounds(0, 0, list.getWidth(), getPreferredSize().height);

        this.addCheckBox();

        return this;
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     * @param g the <tt>Graphics</tt> object
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
     * @param g the <tt>Graphics</tt> object
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
     * Adds a check box component to this renderer.
     */
    private void addCheckBox()
    {
        for (Component c : westPanel.getComponents())
        {
            if (c instanceof JCheckBox)
                westPanel.remove(c);
        }

        final JCheckBox checkBox = new JCheckBox();

        checkBox.setBounds(5, 5, 45, 45);

        westPanel.add(checkBox, BorderLayout.WEST);

        checkBox.setSelected(account.isEnabled());
    }

    /**
     * Returns the component if any at the given location.
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the component we found, otherwise null
     */
    public Component findComponent(int x, int y)
    {
        return westPanel.findComponentAt(x, y);
    }
}