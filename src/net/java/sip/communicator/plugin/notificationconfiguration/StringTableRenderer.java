/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

/**
 * @author Alexandre Maillard
 * @author Yana Stamcheva
 */
class StringTableRenderer extends DefaultTableCellRenderer
{
    /**
     * Creates an instance of <tt>StringTableRenderer</tt>.
     */
    StringTableRenderer()
    {
        super();
    }

    /**
     * Returns the rendering component.
     * @param table the parent table
     * @param value the value of the cell
     * @param isSelected indicates if the cell is selected
     * @param hasFocus indicates if the cell has the focus
     * @param row the cell row
     * @param column the cell column
     * @return the rendering component
     */
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
    {
        if (value instanceof NotificationEntry)
        {
            NotificationEntry entry = (NotificationEntry) value;
            setIcon(null);
            setText(entry.getEvent());
            setHorizontalAlignment(SwingConstants.LEFT);

            if (entry.getEnabled())
            {
                this.setForeground(Color.BLACK);
            }
            else
            {
                this.setForeground(Color.GRAY);
            }
        }
        else if (value instanceof String)
        {
            String stringValue = (String) value;

            if(stringValue.equals(NotificationsTable.ENABLED))
            {
                setText(null);
                setIcon(NotificationsTable.getColumnIconValue(column));
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else if (stringValue.equals(NotificationsTable.DISABLED))
            {
                setText(null);
                setIcon(null);
            }
        }

        if(isSelected)
        {
            this.setOpaque(true);
            this.setBackground(new Color(209, 212, 225));
        }
        else
        {
            this.setBackground(Color.WHITE);
        }
        return this;
    }
}
