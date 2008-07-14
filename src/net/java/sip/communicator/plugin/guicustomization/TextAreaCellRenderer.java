package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

public class TextAreaCellRenderer
    extends JTextArea
    implements TableCellRenderer
{
    public TextAreaCellRenderer()
    {
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        String stringValue = (String) value;

        setText(stringValue);
        setSize(table.getColumnModel().getColumn(column).getWidth(),
            getPreferredSize().height);
        if (table.getRowHeight(row) != getPreferredSize().height)
        {
            table.setRowHeight(row, getPreferredSize().height);
        }
        return this;
    }
}