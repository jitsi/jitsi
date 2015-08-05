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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.googlecontacts.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * This ConfigurationForm shows the list of Google Contacts account and allow
 * users to manage them.
 *
 * @author Sebastien Mazy
 * @author Sebastien Vincent
 */
public class GoogleContactsConfigForm
    extends TransparentPanel
    implements ConfigurationForm,
                ActionListener,
                ListSelectionListener

{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger for this class.
     */
    private static Logger logger = Logger.getLogger(
            GoogleContactsConfigForm.class);

    /**
     * Opens the new directory registration wizard
     */
    private JButton newButton = new JButton("+");

    /**
     * Opens a directory modification dialog
     */
    private JButton modifyButton = new JButton(
            Resources.getString("impl.googlecontacts.EDIT"));

    /**
     * Pops a directory deletion confirmation dialog
     */
    private JButton removeButton = new JButton("-");

    /**
     * Displays the registered Google Contacts account.
     */
    private JTable accountTable = new JTable();

    /**
     * Contains the new/modify/remove buttons
     */
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    /**
     * Contains the directoryTable
     */
    private JScrollPane scrollPane = new JScrollPane();

    /**
     * Contains the buttonsPanel,
     */
    private JPanel rightPanel = new TransparentPanel(new BorderLayout());

    /**
     * Contains listPanel and rightPanel
     */
    private JPanel mainPanel = this;

    /**
     * Model for the directoryTable
     */
    private GoogleContactsTableModel tableModel =
        new GoogleContactsTableModel();

    /**
     * Settings form.
     */
    private final AccountSettingsForm settingsForm =
        new AccountSettingsForm();

    /**
     * Constructor
     */
    public GoogleContactsConfigForm()
    {
        super(new BorderLayout());
        logger.trace("GoogleContacts configuration form.");
        initComponents();
    }

    /**
     * Inits the swing components
     */
    private void initComponents()
    {
        modifyButton.setEnabled(false);
        removeButton.setEnabled(false);

        newButton.setSize(newButton.getMinimumSize());
        modifyButton.setSize(modifyButton.getMinimumSize());
        removeButton.setSize(removeButton.getMinimumSize());

        accountTable.setRowHeight(22);
        accountTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        accountTable.setShowHorizontalLines(false);
        accountTable.setShowVerticalLines(false);
        accountTable.setModel(tableModel);
        accountTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        accountTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() > 1)
                {
                }
            }
        });

        settingsForm.setModal(true);

        /* consistency with the accounts config form */
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        rightPanel.add(buttonsPanel, BorderLayout.NORTH);

        scrollPane.getViewport().add(accountTable);
        mainPanel.add(scrollPane,  BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.SOUTH);

        mainPanel.setPreferredSize(new Dimension(500, 400));

        buttonsPanel.add(newButton);
        buttonsPanel.add(removeButton);
        buttonsPanel.add(modifyButton);

        accountTable.getSelectionModel().addListSelectionListener(this);

        newButton.setActionCommand("new");
        newButton.addActionListener(this);
        modifyButton.addActionListener(this);
        modifyButton.setActionCommand("modify");
        removeButton.addActionListener(this);
        removeButton.setActionCommand("remove");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getTitle
     */
    public String getTitle()
    {
        return Resources.getString("impl.googlecontacts.CONFIG_FORM_TITLE");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getIcon
     */
    public byte[] getIcon()
    {
        return Resources.getImageInBytes(
                "GOOGLECONTACTS_CONFIG_FORM_ICON");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getForm
     */
    public Object getForm()
    {
        return this;
    }

    /**
     * Required by ConfirgurationForm interface
     *
     * Returns the index of this configuration form in the configuration window.
     * This index is used to put configuration forms in the desired order.
     * <p>
     * 0 is the first position
     * -1 means that the form will be put at the end
     * </p>
     * @return the index of this configuration form in the configuration window.
     *
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getIndex
     */
    public int getIndex()
    {
        return -1;
    }

    /**
     * Processes buttons events (new, modify, remove)
     *
     * @see java.awt.event.ActionListener#actionPerformed
     */
    public void actionPerformed(ActionEvent e)
    {
        int row = accountTable.getSelectedRow();

        if (e.getActionCommand().equals("new"))
        {
            settingsForm.setNameFieldEnabled(true);
            settingsForm.loadData(null);
            int ret = settingsForm.showDialog();

            if(ret == 1)
            {
                GoogleContactsConnectionImpl cnx =
                    (GoogleContactsConnectionImpl) settingsForm.getConnection();
                tableModel.addAccount(cnx, true, cnx.getPrefix());
                new RefreshContactSourceThread(null, cnx).start();
                GoogleContactsActivator.getGoogleContactsService().saveConfig(
                        cnx);
                refresh();
            }
        }

        if (e.getActionCommand().equals("modify") && row != -1)
        {
            settingsForm.setNameFieldEnabled(false);
            GoogleContactsConnectionImpl cnx = tableModel.getAccountAt(row);
            settingsForm.loadData(cnx);

            int ret = settingsForm.showDialog();

            if(ret == 1)
            {
                GoogleContactsActivator.getGoogleContactsService().saveConfig(
                        cnx);
                if(cnx.isEnabled())
                {
                    new RefreshContactSourceThread(cnx, cnx).start();
                }
                refresh();
            }
        }

        if (e.getActionCommand().equals("remove") && row != -1)
        {
            GoogleContactsConnection cnx = tableModel.getAccountAt(row);
            tableModel.removeAccount(cnx.getLogin());
            GoogleContactsActivator.getGoogleContactsService().removeConfig(
                    cnx);
            new RefreshContactSourceThread(cnx, null).start();
            refresh();
        }
    }

    /**
     * Required by ListSelectionListener. Enables the "modify"
     * button when a server is selected in the table
     *
     * @param e event triggered
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if(accountTable.getSelectedRow() == -1)
        {
            modifyButton.setEnabled(false);
            removeButton.setEnabled(false);
            return;
        }
        else if(!e.getValueIsAdjusting())
        {
            modifyButton.setEnabled(true);
            removeButton.setEnabled(true);
            GoogleContactsActivator.getGoogleContactsService().
                saveConfig(tableModel.getAccountAt(
                        accountTable.getSelectedRow()));
        }
    }

    /**
     * refreshes the table display
     */
    private void refresh()
    {
        tableModel.fireTableStructureChanged();
    }

    /**
     * Indicates if this is an advanced configuration form.
     * @return <tt>true</tt> if this is an advanced configuration form,
     * otherwise it returns <tt>false</tt>
     */
    public boolean isAdvanced()
    {
        return true;
    }

    /**
     * Thread that will perform refresh of contact sources.
     */
    public static class RefreshContactSourceThread
        extends Thread
    {
        /**
         * Old connection.
         */
        private GoogleContactsConnection oldCnx = null;

        /**
         * New connection.
         */
        private GoogleContactsConnection newCnx = null;

        /**
         * Constructor.
         *
         * @param oldCnx old connection.
         * @param newCnx new connection.
         */
        RefreshContactSourceThread(GoogleContactsConnection oldCnx,
                GoogleContactsConnection newCnx)
        {
            this.oldCnx = oldCnx;
            this.newCnx = newCnx;
        }

        /**
         * Thread entry point.
         */
        @Override
        public void run()
        {
            if(oldCnx != null)
            {
                GoogleContactsActivator.getGoogleContactsService().
                    removeContactSource(oldCnx);
            }

            if(newCnx != null)
            {
                GoogleContactsActivator.getGoogleContactsService().
                    addContactSource(newCnx, true);
            }
        }
    }
}
