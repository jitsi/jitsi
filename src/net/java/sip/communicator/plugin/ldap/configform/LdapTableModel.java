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
package net.java.sip.communicator.plugin.ldap.configform;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.ldap.*;
import net.java.sip.communicator.service.ldap.*;

/**
 * A table model suitable for the directories list in
 * the configuration form. Takes its data in an LdapDirectorySet.
 *
 * @author Sebastien Mazy
 */
public class LdapTableModel
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * a reference to the LdapDirectorySet from the ldap service
     */
    private LdapDirectorySet serverSet =
        LdapActivator.getLdapService().getServerSet();

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
                return Resources.getString("impl.ldap.ENABLED");
            case 1:
                return Resources.getString("impl.ldap.SERVER_NAME");
            case 2:
                return Resources.getString("impl.ldap.SERVER_HOSTNAME");
            case 3:
                return Resources.getString("impl.ldap.PHONE_PREFIX");
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
        return this.serverSet.size();
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
        // 4 columns: "enable", "name", "hostname" and "prefix"
        return 4;
    }

    /**
     * Returns the text for the given cell of the table
     *
     * @param row cell row
     * @param column cell column
     *
     * @see javax.swing.table.AbstractTableModel#getValueAt
     */
    public Object getValueAt(int row, int column)
    {
        switch(column)
        {
            case 0:
                return new Boolean(this.getServerAt(row).isEnabled());
            case 1:
                return this.getServerAt(row).getSettings().getName();
            case 2:
                return this.getServerAt(row).getSettings().getHostname();
            case 3:
                return this.getServerAt(row).getSettings()
                            .getGlobalPhonePrefix();
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
    public LdapDirectory getServerAt(int row)
    {
        int i = 0;
        for(LdapDirectory server : serverSet)
        {
            if(i == row)
                return server;
            i++;
        }

        throw new IllegalArgumentException("row not found");
    }

    /**
     * Returns whether a cell is editable. Only "enable" column (checkboxes)
     * is editable
     *
     * @param row row of the cell
     * @param col column of the cell
     *
     * @return whether the cell is editable
     */
    @Override
    public boolean isCellEditable(int row, int col)
    {
        if(col == 0)
            return true;
        else
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
     * Sets a value in an editable cell, that is to say
     * an enable/disable chekboxin colum 0
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if(columnIndex != 0)
            throw new IllegalArgumentException("non editable column!");
        LdapDirectory server = this.getServerAt(rowIndex);
        LdapConfigForm.RefreshContactSourceThread th = null;

        /* toggle enabled marker and save */
        server.setEnabled(!server.isEnabled());

        if(!server.isEnabled())
        {
            th = new LdapConfigForm.RefreshContactSourceThread(server, null);
        }
        else
        {
            th = new LdapConfigForm.RefreshContactSourceThread(null, server);
        }

        th.start();
        server.getSettings().persistentSave();
    }
}
