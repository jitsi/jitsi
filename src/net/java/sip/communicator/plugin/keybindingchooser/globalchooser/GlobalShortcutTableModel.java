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
package net.java.sip.communicator.plugin.keybindingchooser.globalchooser;

import java.util.*;

import javax.swing.table.*;

/**
 * Table model for global shortcuts.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutTableModel
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * List of shortcuts.
     */
    private List<GlobalShortcutEntry> shortcuts =
        new ArrayList<GlobalShortcutEntry>();

    /**
     * Returns the title for this column
     *
     * @param column the column
     *
     * @return the title for this column
     *
     * @see javax.swing.table.AbstractTableModel#getColumnName
     */
    @Override
    public String getColumnName(int column)
    {
        switch(column)
        {
            case 0:
                return Resources.getString(
                    "plugin.keybindings.globalchooser.SHORTCUT_NAME");
            case 1:
                return Resources.getString(
                    "plugin.keybindings.globalchooser.SHORTCUT_PRIMARY");
            case 2:
                return Resources.getString(
                    "plugin.keybindings.globalchooser.SHORTCUT_SECOND");
            default:
                throw new IllegalArgumentException("column not found");
        }
    }

    /**
     * Returns the number of rows in the table
     *
     * @return the number of rows in the table
     * @see javax.swing.table.AbstractTableModel#getRowCount
     */
    public int getRowCount()
    {
        return shortcuts.size();
    }

    /**
     * Returns the number of column in the table
     *
     * @return the number of columns in the table
     *
     * @see javax.swing.table.AbstractTableModel#getColumnCount
     */
    public int getColumnCount()
    {
        // 3 columns: "name", "primary shortcut", "second shortcut"
        return 3;
    }

    /**
     * Returns the text for the given cell of the table
     *
     * @param row cell row
     * @param column cell column
     * @return object at the row/column
     * @see javax.swing.table.AbstractTableModel#getValueAt
     */
    public Object getValueAt(int row, int column)
    {
        switch(column)
        {
            case 0:
                return getEntryAt(row).getAction();
            case 1:
                return getEntryAt(row).getEditShortcut1() ?
                    "Press key" : GlobalShortcutEntry.getShortcutText(
                    getEntryAt(row).getShortcut());
            case 2:
                return getEntryAt(row).getEditShortcut2() ?
                    "Press key" : GlobalShortcutEntry.getShortcutText(
                    getEntryAt(row).getShortcut2());
            default:
                throw new IllegalArgumentException("column not found");
        }
    }

    /**
     * Returns the LdapDirectory at the row 'row'
     *
     * @param row the row on which to find the LdapDirectory
     *
     * @return the LdapDirectory found
     */
    public GlobalShortcutEntry getEntryAt(int row)
    {
        int i = 0;

        for(GlobalShortcutEntry entry : shortcuts)
        {
            if(i == row)
                return entry;
            i++;
        }

        throw new IllegalArgumentException("row not found");
    }

    /**
     * Returns whether a cell is editable.
     * @param row row of the cell
     * @param col column of the cell
     *
     * @return whether the cell is editable
     */
    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    /**
     * Overrides a method that always returned Object.class
     * Now it will return Boolean.class for the first method,
     * letting the DefaultTableCellRenderer create checkboxes.
     *
     * @param columnIndex index of the column
     * @return Column class
     */
    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        Object o = getValueAt(0, columnIndex);
        if(o == null)
            return String.class;
        return o.getClass();
    }

    /**
     * Sets a value in an editable cell.
     *
     * @param aValue value to set
     * @param rowIndex row index
     * @param columnIndex column index
     */
    public void ssetValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if(columnIndex != 0)
            throw new IllegalArgumentException("non editable column!");
    }

    /**
     * Adds an entry.
     *
     * @param entry entry to add
     */
    public void addEntry(GlobalShortcutEntry entry)
    {
        shortcuts.add(entry);
    }

    /**
     * Adds an entry.
     *
     * @param entry entry to add
     */
    public void removeEntry(GlobalShortcutEntry entry)
    {
        shortcuts.remove(entry);
    }

    /**
     * Returns all shortcuts.
     *
     * @return all shortcuts.
     */
    public List<GlobalShortcutEntry> getEntries()
    {
        return shortcuts;
    }
}
