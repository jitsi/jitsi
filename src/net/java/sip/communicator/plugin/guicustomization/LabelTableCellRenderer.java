package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

public class LabelTableCellRenderer
    extends JLabel
    implements TableCellRenderer
{
    public LabelTableCellRenderer()
    {
        this.setOpaque(true);
    }
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (isSelected)
        {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        }
        else
        {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        if (value instanceof JLabel)
        {
            JLabel labelValue = (JLabel) value;
            this.setText(labelValue.getText());
            this.setIcon(labelValue.getIcon());
            this.setBackground(labelValue.getBackground());
        }

        setEnabled(table.isEnabled());
        setFont(table.getFont());

        return this;
    }
}
