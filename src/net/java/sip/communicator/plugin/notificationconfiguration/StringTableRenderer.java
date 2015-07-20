/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

/**
 * @author Alexandre Maillard
 * @author Yana Stamcheva
 */
class StringTableRenderer
    extends DefaultTableCellRenderer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
    @Override
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

            setForeground(entry.getEnabled() ? Color.BLACK : Color.GRAY);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIcon(null);
            setText(Resources.getString("plugin.notificationconfig.event."
                + entry.getEvent()));
        }
        else if (value instanceof String)
        {
            String stringValue = (String) value;

            if(stringValue.equals(NotificationsTable.ENABLED))
            {
                setHorizontalAlignment(SwingConstants.CENTER);
                setIcon(NotificationsTable.getColumnIconValue(column));
                setText(null);
            }
            else if (stringValue.equals(NotificationsTable.DISABLED))
            {
                setIcon(null);
                setText(null);
            }
        }

        if(isSelected)
        {
            setBackground(new Color(209, 212, 225));
            setOpaque(true);
        }
        else
        {
            setBackground(Color.WHITE);
        }
        return this;
    }
}
