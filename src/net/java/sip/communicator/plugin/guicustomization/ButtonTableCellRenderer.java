/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Custom <tt>TableCellRenderer</tt> that renders
 * <tt>ProtocolProviderService</tt> objects, <tt>MetaContactGroup</tt>
 * objects and JLabels.
 * 
 * @author Yana Stamcheva
 */
public class ButtonTableCellRenderer
    extends JPanel
    implements TableCellRenderer
{
    public ButtonTableCellRenderer()
    {
        super(new FlowLayout(FlowLayout.CENTER));

        this.setOpaque(true);
    }

    public Component getTableCellRendererComponent( JTable table,
                                                    Object value,
                                                    boolean isSelected,
                                                    boolean hasFocus,
                                                    int row,
                                                    int column)
    {
        this.removeAll();

        if(value instanceof JButton)
        {
            this.add((JButton) value);
        }

        return this;
    }
}