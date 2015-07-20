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
package net.java.sip.communicator.impl.googlecontacts.configform;

import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.impl.googlecontacts.*;
import net.java.sip.communicator.service.googlecontacts.*;


/**
 * A table model suitable for the directories list in
 * the configuration form. Takes its data in an LdapDirectorySet.
 *
 * @author Sebastien Mazy
 * @author Sebastien Vincent
 */
public class GoogleContactsTableModel
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Google Contacts service reference.
     */
    private final GoogleContactsServiceImpl googleService =
        GoogleContactsActivator.getGoogleContactsService();

    /**
     * Add account from table.
     *
     * @param cnx account
     * @param enabled if the account should be enabled
     * @param prefix phone number prefix
     */
    public void addAccount( GoogleContactsConnection cnx,
                            boolean enabled,
                            String prefix)
    {
        if(cnx != null)
        {
            GoogleContactsConnectionImpl cnxImpl
                = (GoogleContactsConnectionImpl) cnx;
            cnxImpl.setEnabled(enabled);
            cnxImpl.setPrefix(prefix);

            googleService.getAccounts().add(cnxImpl);
        }
    }

    /**
     * Remove account from table.
     *
     * @param login account login to remove
     */
    public void removeAccount(String login)
    {
        Iterator<GoogleContactsConnectionImpl> it =
            googleService.getAccounts().iterator();

        while(it.hasNext())
        {
            GoogleContactsConnection cnx = it.next();
            if(cnx.getLogin().equals(login))
            {
                it.remove();
                return;
            }
        }
    }

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
                return Resources.getString("impl.googlecontacts.ENABLED");
            case 1:
                return Resources.getString("impl.googlecontacts.ACCOUNT_NAME");
            case 2:
                return Resources.getString("impl.googlecontacts.PREFIX");
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
        return googleService.getAccounts().size();
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
        // 3 columns: "enable", "account name", "prefix"
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
                return new Boolean(getAccountAt(row).isEnabled());
            case 1:
                return getAccountAt(row).getLogin();
            case 2:
                return getAccountAt(row).getPrefix();
            default:
                throw new IllegalArgumentException("column not found");
        }
    }

    /**
     * Returns the account credentials at the row 'row'
     *
     * @param row the row
     *
     * @return the login/password for the account
     */
    public GoogleContactsConnectionImpl getAccountAt(int row)
    {
        if(row < 0 || row >= googleService.getAccounts().size())
        {
            throw new IllegalArgumentException("row not found");
        }
        else
        {
            return googleService.getAccounts().get(row);
        }
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
        if(col == 1)
            return false;
        else
            return true;
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
        return getValueAt(0, columnIndex).getClass();
    }

    /**
     * Sets a value in an editable cell, that is to say
     * an enable/disable chekbox in colum 0
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if(columnIndex != 0 && columnIndex != 2)
            throw new IllegalArgumentException("non editable column!");

        GoogleContactsConfigForm.RefreshContactSourceThread th = null;
        GoogleContactsConnectionImpl cnx = getAccountAt(rowIndex);

        if (columnIndex == 0)
        {
            if(cnx.isEnabled())
            {
                th = new GoogleContactsConfigForm.RefreshContactSourceThread(cnx,
                        null);
            }
            else
            {
                th = new GoogleContactsConfigForm.RefreshContactSourceThread(null,
                        cnx);
            }

            cnx.setEnabled(!cnx.isEnabled());

            th.start();
        }
        else if (columnIndex == 2)
        {
            cnx.setPrefix(aValue.toString());
        }
    }
}
