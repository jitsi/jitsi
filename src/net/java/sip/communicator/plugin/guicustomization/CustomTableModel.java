package net.java.sip.communicator.plugin.guicustomization;

import javax.swing.*;
import javax.swing.table.*;

public class CustomTableModel
    extends DefaultTableModel
{
    public boolean isCellEditable(int row, int column)
    {
        Object o = getValueAt(row, column);

        if (column == 0 || o instanceof JLabel)
            return false;

        return true;
    }
}
