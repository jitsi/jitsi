/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * Custom <tt>TableCellRenderer</tt> that renders
 * <tt>ProtocolProviderService</tt> objects, <tt>MetaContactGroup</tt>
 * objects and JLabels.
 *
 * @author Yana Stamcheva
 */
public class LabelTableCellRenderer extends JPanel
    implements TableCellRenderer {

    private JLabel label = new JLabel();

    public LabelTableCellRenderer(){
        label.setHorizontalAlignment(JLabel.CENTER);
        this.setOpaque(true);

        this.add(label, BorderLayout.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if(value instanceof JLabel) {
            JLabel labelValue = (JLabel)value;

            label.setText(labelValue.getText());
            label.setIcon(labelValue.getIcon());
        }
        else if (value instanceof ProtocolProviderService) {
            ProtocolProviderService pps = (ProtocolProviderService)value;
            label.setText(pps.getAccountID().getUserID());
        }
        else if (value instanceof MetaContactGroup) {
            MetaContactGroup group = (MetaContactGroup) value;
            label.setText(group.getGroupName());
        }
        else {
            label.setText(value.toString());
        }

        if(isSelected)
            this.setBackground(Constants.SELECTED_END_COLOR);
        else
            this.setBackground(UIManager.getColor("Table.background"));

        return this;
    }
}
