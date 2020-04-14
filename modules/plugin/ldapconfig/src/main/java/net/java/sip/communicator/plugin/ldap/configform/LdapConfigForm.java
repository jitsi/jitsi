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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.ldap.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * This ConfigurationForm shows the list of LDAP directories
 * and allow users to manage them.
 *
 * @author Sebastien Mazy
 * @author Sebastien Vincent
 */
public class LdapConfigForm
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
    private static Logger logger = Logger.getLogger(LdapConfigForm.class);

    /**
     * opens the new directory registration wizard
     */
    private JButton newButton = new JButton("+");

    /**
     * opens a directory modification dialog
     */
    private JButton modifyButton = new JButton(
            Resources.getString("impl.ldap.EDIT"));

    /**
     * pops a directory deletion confirmation dialog
     */
    private JButton removeButton = new JButton("-");

    /**
     * displays the registered directories
     */
    private JTable directoryTable = new JTable();

    /**
     * contains the new/modify/remove buttons
     */
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    /**
     * contains the directoryTable
     */
    private JScrollPane scrollPane = new JScrollPane();

    /**
     * contains the buttonsPanel,
     */
    private JPanel rightPanel = new TransparentPanel(new BorderLayout());

    /**
     * contains listPanel and rightPanel
     */
    private JPanel mainPanel = this;

    /**
     * model for the directoryTable
     */
    private LdapTableModel tableModel = new LdapTableModel();

    /**
     * Settings form.
     */
    private final DirectorySettingsForm settingsForm =
        new DirectorySettingsForm();

    /**
     * Constructor
     */
    public LdapConfigForm()
    {
        super(new BorderLayout());
        logger.trace("New LDAP configuration form.");
        this.initComponents();
    }

    /**
     * Inits the swing components
     */
    private void initComponents()
    {
        this.modifyButton.setEnabled(false);
        this.removeButton.setEnabled(false);

        this.newButton.setSize(newButton.getMinimumSize());
        this.modifyButton.setSize(modifyButton.getMinimumSize());
        this.removeButton.setSize(removeButton.getMinimumSize());

        this.directoryTable.setRowHeight(22);
        this.directoryTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        this.directoryTable.setShowHorizontalLines(false);
        this.directoryTable.setShowVerticalLines(false);
        this.directoryTable.setModel(tableModel);
        this.directoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.directoryTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() > 1)
                {
                    int row =
                        LdapConfigForm.this.directoryTable.getSelectedRow();

                    if(row >= 0)
                    {
                        LdapFactory factory =
                            LdapActivator.getLdapService().getFactory();
                        LdapDirectorySet serverSet =
                            LdapActivator.getLdapService().getServerSet();
                        LdapDirectory oldServer =
                            LdapConfigForm.this.tableModel.getServerAt(row);
                        LdapDirectorySettings settings =
                            oldServer.getSettings();
                        settingsForm.loadData(settings);
                        settingsForm.setNameFieldEnabled(false);

                        int ret = settingsForm.showDialog();

                        if(ret == 1)
                        {
                            LdapDirectory newServer = factory.createServer(
                                    settingsForm.getSettings());
                            serverSet.removeServerWithName(
                                    oldServer.getSettings().
                                    getName());
                            new RefreshContactSourceThread(oldServer,
                                    newServer).start();
                            serverSet.addServer(newServer);
                            refresh();
                        }
                    }

                }
            }
        });

        settingsForm.setModal(true);

        /*
         * TODO fix: table width change when refreshing table
        this.directoryTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        this.directoryTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        this.directoryTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        */

        /* consistency with the accounts config form */
        this.rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.rightPanel.add(this.buttonsPanel, BorderLayout.NORTH);

        this.scrollPane.getViewport().add(this.directoryTable);
        this.mainPanel.add(this.scrollPane,  BorderLayout.CENTER);
        this.mainPanel.add(this.rightPanel, BorderLayout.SOUTH);

        this.mainPanel.setPreferredSize(new Dimension(500, 400));

        this.buttonsPanel.add(this.newButton);
        this.buttonsPanel.add(this.removeButton);
        this.buttonsPanel.add(this.modifyButton);

        this.directoryTable.getSelectionModel().addListSelectionListener(this);

        this.newButton.setActionCommand("new");
        this.newButton.addActionListener(this);
        this.modifyButton.addActionListener(this);
        this.modifyButton.setActionCommand("modify");
        this.removeButton.addActionListener(this);
        this.removeButton.setActionCommand("remove");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getTitle
     */
    public String getTitle()
    {
        return Resources.getString("impl.ldap.CONFIG_FORM_TITLE");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getIcon
     */
    public byte[] getIcon()
    {
        return Resources.getImageInBytes("LDAP_CONFIG_FORM_ICON");
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
        int row = this.directoryTable.getSelectedRow();

        LdapFactory factory = LdapActivator.getLdapService().getFactory();
        LdapDirectorySet serverSet =
            LdapActivator.getLdapService().getServerSet();

        if (e.getActionCommand().equals("new"))
        {
            LdapDirectorySettings settings = factory.createServerSettings();
            settingsForm.loadData(settings);
            settingsForm.setNameFieldEnabled(true);

            int ret = settingsForm.showDialog();

            if(ret == 1)
            {
                LdapDirectory server = factory.createServer(
                        settingsForm.getSettings());
                new RefreshContactSourceThread(null, server).start();
                serverSet.addServer(server);
                refresh();
            }
        }

        if (e.getActionCommand().equals("modify") && row != -1)
        {
            LdapDirectory oldServer = this.tableModel.getServerAt(row);
            LdapDirectorySettings settings = oldServer.getSettings();
            settingsForm.loadData(settings);
            settingsForm.setNameFieldEnabled(false);

            int ret = settingsForm.showDialog();

            if(ret == 1)
            {
                LdapDirectory newServer = factory.createServer(
                        settingsForm.getSettings());
                new RefreshContactSourceThread(oldServer, newServer).start();
                serverSet.removeServerWithName(oldServer.getSettings().
                        getName());
                serverSet.addServer(newServer);
                refresh();
            }
        }

        if (e.getActionCommand().equals("remove") && row != -1)
        {
            new RefreshContactSourceThread(this.tableModel.getServerAt(row),
                    null).start();
            serverSet.removeServerWithName(this.tableModel.getServerAt(row).
                    getSettings().getName());
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
        if(this.directoryTable.getSelectedRow() == -1)
        {
            this.modifyButton.setEnabled(false);
            this.removeButton.setEnabled(false);
        }
        else if(!e.getValueIsAdjusting())
        {
            this.modifyButton.setEnabled(true);
            this.removeButton.setEnabled(true);
        }
    }

    /**
     * refreshes the table display
     */
    private void refresh()
    {
        this.tableModel.fireTableStructureChanged();
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
         * LDAP directory to remove.
         */
        private LdapDirectory oldLdap = null;

        /**
         * LDAP directory to add.
         */
        private LdapDirectory newLdap = null;

        /**
         * Constructor.
         *
         * @param oldLdap LDAP directory to remove
         * @param newLdap LDAP directory to add.
         */
        RefreshContactSourceThread(LdapDirectory oldLdap, LdapDirectory newLdap)
        {
            this.oldLdap = oldLdap;
            this.newLdap = newLdap;
        }

        /**
         * Thread entry point.
         */
        @Override
        public void run()
        {
            LdapService service = LdapActivator.getLdapService();
            if(oldLdap != null)
            {
                service.removeContactSource(oldLdap);
            }

            if(newLdap != null)
            {
                service.createContactSource(newLdap);
            }
        }
    }
}

