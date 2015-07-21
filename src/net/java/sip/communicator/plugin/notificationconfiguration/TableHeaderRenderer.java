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
class TableHeaderRenderer implements TableCellRenderer
{
    // Create a JLabel for use as a renderer and pre-load this label
    // with an icon image.
    private JLabel l;

    /**
     * Creates an instance of <tt>TableHeaderRenderer</tt>.
     * @param JLIcon the label of the column.
     */
    TableHeaderRenderer(JLabel JLIcon)
    {
        this.l = JLIcon;
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
    public Component getTableCellRendererComponent (
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
    {
        // Extract the original header renderer for this column.
        TableCellRenderer tcr = table.getTableHeader().getDefaultRenderer ();

        // Extract the component used to render the column header.
        Component c = tcr.getTableCellRendererComponent (
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

        // Establish the font, foreground color, and border for the
        // JLabel so that the rendered header will look the same as the
        // other rendered headers.
        l.setFont (c.getFont ());
        l.setForeground (c.getForeground ());
        l.setBorder (((JComponent) c).getBorder ());

        // Establish the column name.

        l.setText ((String) value);

        // Return the cached JLabel a the renderer for this column
        // header.

        return l;
    }
}
