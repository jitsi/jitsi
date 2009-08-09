/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.callhistoryform;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in
 * the SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of
 * JLabel, which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted.
 * 
 * @author Yana Stamcheva
 */
public class CallListCellRenderer
    extends JPanel
    implements ListCellRenderer
{

    private JPanel dataPanel = new JPanel(new BorderLayout());

    private JLabel nameLabel = new JLabel();

    private JPanel timePanel = new JPanel(new BorderLayout(8, 0));

    private JLabel timeLabel = new JLabel();

    private JLabel durationLabel = new JLabel();

    private JLabel iconLabel = new JLabel();

    private Icon incomingIcon
        = ExtendedCallHistorySearchActivator.getResources()
            .getImage("plugin.callhistorysearch.INCOMING_CALL");

    private Icon outgoingIcon
        = ExtendedCallHistorySearchActivator.getResources()
            .getImage("plugin.callhistorysearch.OUTGOING_CALL");

    private boolean isSelected = false;

    private boolean isLeaf = true;

    private String direction;

    /**
     * Initialize the panel containing the node.
     */
    public CallListCellRenderer()
    {

        super(new BorderLayout(5, 5));

        this.setBackground(Color.WHITE);

        this.setOpaque(true);

        this.dataPanel.setOpaque(false);

        this.timePanel.setOpaque(false);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.nameLabel.setIconTextGap(2);

        this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

        this.dataPanel.add(nameLabel, BorderLayout.WEST);

        this.add(dataPanel, BorderLayout.CENTER);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * 
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {

        this.dataPanel.remove(timePanel);
        this.dataPanel.remove(timeLabel);
        this.dataPanel.remove(durationLabel);
        this.remove(iconLabel);

        if (value instanceof GuiCallPeerRecord)
        {

            GuiCallPeerRecord peer = (GuiCallPeerRecord) value;

            this.direction = peer.getDirection();

            if (direction.equals(GuiCallPeerRecord.INCOMING_CALL))
                iconLabel.setIcon(incomingIcon);
            else
                iconLabel.setIcon(outgoingIcon);

            this.nameLabel.setText(peer.getPeerName());
 
            this.timeLabel.setText(
                ExtendedCallHistorySearchActivator.getResources()
                    .getI18NString("service.gui.AT") + " "
                + GuiUtils.formatTime(peer.getStartTime()));

            this.durationLabel.setText(
                ExtendedCallHistorySearchActivator.getResources()
                    .getI18NString("service.gui.DURATION") + " "
                + GuiUtils.formatTime(peer.getCallTime()));

            // this.nameLabel.setIcon(listModel
            // .getMetaContactStatusIcon(contactItem));

            this.timePanel.add(timeLabel, BorderLayout.WEST);
            this.timePanel.add(durationLabel, BorderLayout.EAST);
            this.dataPanel.add(timePanel, BorderLayout.EAST);

            this.add(iconLabel, BorderLayout.WEST);

            this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 25);

            this.isLeaf = true;
        }
        else if (value instanceof String)
        {

            String dateString = (String) value;

            this.nameLabel.setText(dateString);

            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // We should set the bounds of the cell explicitely in order to
            // make getComponentAt work properly.
            this.setBounds(0, 0, list.getWidth() - 2, 20);

            this.isLeaf = false;
        }

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

    private void internalPaintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        if (!this.isLeaf)
        {

            GradientPaint p = new GradientPaint(0, 0,
                Constants.BORDER_COLOR, this.getWidth(), this
                    .getHeight(), Constants.GRADIENT_LIGHT_COLOR);

            g2.setPaint(p);
            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }
        else
        {
            if (direction.equals(GuiCallPeerRecord.INCOMING_CALL))
            {

                GradientPaint p = new GradientPaint(0, 0,
                    Constants.HISTORY_IN_CALL_COLOR, this.getWidth(), this
                        .getHeight(), Constants.GRADIENT_LIGHT_COLOR);

                g2.setPaint(p);
                g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1,
                    7, 7);
            }
            else if (direction.equals(GuiCallPeerRecord.OUTGOING_CALL))
            {

                GradientPaint p = new GradientPaint(0, 0,
                    Constants.HISTORY_OUT_CALL_COLOR, this.getWidth(), this
                        .getHeight(), Constants.GRADIENT_LIGHT_COLOR);

                g2.setPaint(p);
                g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1,
                    7, 7);
            }
        }

        if (this.isSelected)
        {

            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 2, this.getHeight() - 1,
                7, 7);
        }
    }
}
