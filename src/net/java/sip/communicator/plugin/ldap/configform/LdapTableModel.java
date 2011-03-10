/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ldap.configform;

import javax.swing.table.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.plugin.ldap.*;

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
        // 3 columns: "enable", "name" and "hostname"
        return 3;
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
    public Class<?> getColumnClass(int columnIndex)
    {
        return getValueAt(0, columnIndex).getClass();
    }

    /**
     * Sets a value in an editable cell, that is to say
     * an enable/disable chekboxin colum 0
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if(columnIndex != 0)
            throw new IllegalArgumentException("non editable column!");
        LdapDirectory server = this.getServerAt(rowIndex);

        /* toggle enabled marker and save */
        server.setEnabled(!server.isEnabled());

        if(!server.isEnabled())
        {
            LdapActivator.disableContactSource(server);
        }
        else
        {
            LdapActivator.enableContactSource(server);
        }

        server.getSettings().persistentSave();
    }
}
