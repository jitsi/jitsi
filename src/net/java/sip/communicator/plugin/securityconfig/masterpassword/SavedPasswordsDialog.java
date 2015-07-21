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
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
// disambiguation

/**
 * The dialog that displays all saved account passwords.
 *
 * @author Dmitri Melnikov
 */
public class SavedPasswordsDialog
    extends SIPCommDialog
{
    /**
     * The logger.
     */
    private static Logger logger
        = Logger.getLogger(SavedPasswordsDialog.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * UI components.
     */
    private JPanel mainPanel;
    private JButton closeButton;

    /**
     * The {@link CredentialsStorageService}.
     */
    private static final CredentialsStorageService credentialsStorageService
        = SecurityConfigActivator.getCredentialsStorageService();

    /**
     * The <tt>ResourceManagementService</tt> used by this instance to access
     * the localized and internationalized resources of the application.
     */
    private static final ResourceManagementService resources
        = SecurityConfigActivator.getResources();

    /**
     * Instance of this dialog.
     */
    private static SavedPasswordsDialog dialog;

    /**
     * Builds the dialog.
     */
    private SavedPasswordsDialog()
    {
        super(false);
        initComponents();

        this.setTitle(
                resources.getI18NString(
                        "plugin.securityconfig.masterpassword.SAVED_PASSWORDS"));
        this.setMinimumSize(new Dimension(550, 300));
        this.setPreferredSize(new Dimension(550, 300));
        this.setResizable(false);

        this.getContentPane().add(mainPanel);

        this.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x,y);
    }

    /**
     * Initialises the UI components.
     */
    private void initComponents()
    {
        this.setLayout(new GridBagLayout());
        mainPanel = new TransparentPanel(new BorderLayout(10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.PAGE_START;

        PasswordsPanel accPassPanel = new PasswordsPanel();
        this.add(accPassPanel, c);

        c.gridy = 1;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        closeButton
            = new JButton(resources.getI18NString("service.gui.CLOSE"));
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dialog = null;
                dispose();
            }
        });
        this.add(closeButton, c);
    }

    @Override
    protected void close(boolean isEscaped)
    {
        closeButton.doClick();
    }

    /**
     * @return the {@link SavedPasswordsDialog} instance
     */
    public static SavedPasswordsDialog getInstance()
    {
        if (dialog == null) {
            dialog = new SavedPasswordsDialog();
        }
        return dialog;
    }

    /**
     * Panel containing the accounts table and buttons.
     */
    private static class PasswordsPanel
        extends TransparentPanel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The table model for the accounts table.
         */
        private class PasswordsTableModel
            extends AbstractTableModel
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            /**
             * Index of the first column.
             */
            public static final int TYPE_INDEX = 0;
            /**
             * Index of the second column.
             */
            public static final int USER_NAME_INDEX = 1;
            /**
             * Index of the third column.
             */
            public static final int PASSWORD_INDEX = 2;

            /**
             * The row objects for this model.
             */
            public final List<PasswordsTableRow> savedPasswords =
                    new ArrayList<PasswordsTableRow>();

            /**
             * Returns the name for the given column.
             *
             * @param column the column index
             * @return the column name for the given index
             */
            @Override
            public String getColumnName(int column)
            {
                String key;

                switch (column)
                {
                case TYPE_INDEX:
                    key = "plugin.securityconfig.masterpassword.COL_TYPE";
                    break;
                case USER_NAME_INDEX:
                    key = "plugin.securityconfig.masterpassword.COL_NAME";
                    break;
                case PASSWORD_INDEX:
                    key = "plugin.securityconfig.masterpassword.COL_PASSWORD";
                    break;
                default:
                    return null;
                }
                return resources.getI18NString(key);
            }

            /**
             * Returns the value for the given row and column.
             *
             * @param row table's row
             * @param column table's column
             * @return object inside the table at the given row and column
             */
            public Object getValueAt(int row, int column)
            {
                if (row < 0)
                    return null;

                PasswordsTableRow savedPass = savedPasswords.get(row);
                switch (column)
                {
                case TYPE_INDEX:
                    return savedPass.type;
                case USER_NAME_INDEX:
                    return savedPass.name;
                case PASSWORD_INDEX:
                    String pass =
                        credentialsStorageService
                            .loadPassword(savedPass.property);
                    return
                        (pass == null)
                            ? resources
                                .getI18NString(
                                    "plugin.securityconfig.masterpassword.CANNOT_DECRYPT")
                            : pass;
                default:
                    return null;
                }
            }

            /**
             * Number of rows in the table.
             *
             * @return number of rows
             */
            public int getRowCount()
            {
                return savedPasswords.size();
            }

            /**
             * Number of columns depends on whether we are showing passwords or
             * not.
             *
             * @return number of columns
             */
            public int getColumnCount()
            {
                return showPasswords ? 3 : 2;
            }
        }

        /**
         * Are we showing the passwords column or not.
         */
        private boolean showPasswords = false;

        /**
         * The button to remove the saved password for the selected account.
         */
        private JButton removeButton;

        /**
         * The button to remove saved passwords for all accounts.
         */
        private JButton removeAllButton;

        /**
         * The button to show the saved passwords for all accounts in plain text.
         */
        private JButton showPasswordsButton;

        /**
         * The table itself.
         */
        private JTable accountsTable;

        /**
         * Builds the panel.
         */
        public PasswordsPanel()
        {
            this.initComponents();
            this.initContent();
        }

        /**
         * Returns the {@link AccountID} object for the selected row.
         * @return the selected account
         */
        private PasswordsTableRow getSelectedAccountID()
        {
            PasswordsTableModel model =
                (PasswordsTableModel) accountsTable.getModel();
            int index = accountsTable.getSelectedRow();
            if (index < 0 || index > model.savedPasswords.size())
                return null;

            return model.savedPasswords.get(index);
        }

        /**
         * Initializes the table's components.
         */
        private void initComponents()
        {
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                resources.getI18NString(
                    "plugin.securityconfig.masterpassword.STORED_ACCOUNT_PASSWORDS")));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            accountsTable = new JTable();
            accountsTable.setModel(new PasswordsTableModel());
            accountsTable.setSelectionMode(
                    javax.swing.ListSelectionModel.SINGLE_SELECTION);
            accountsTable.setCellSelectionEnabled(false);
            accountsTable.setColumnSelectionAllowed(false);
            accountsTable.setRowSelectionAllowed(true);
            accountsTable.getColumnModel().getColumn(
                PasswordsTableModel.USER_NAME_INDEX).setPreferredWidth(270);
            accountsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        if (e.getValueIsAdjusting())
                            return;
                        // activate remove button on select
                        removeButton.setEnabled(true);
                    }
                });

            JScrollPane pnlAccounts = new JScrollPane(accountsTable);
            this.add(pnlAccounts);

            JPanel pnlButtons = new TransparentPanel();
            pnlButtons.setLayout(new BorderLayout());
            this.add(pnlButtons);

            JPanel leftButtons = new TransparentPanel();
            pnlButtons.add(leftButtons, BorderLayout.WEST);

            removeButton
                = new JButton(
                    resources.getI18NString(
                            "plugin.securityconfig.masterpassword.REMOVE_PASSWORD_BUTTON"));
            // enabled on row selection
            removeButton.setEnabled(false);
            removeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    PasswordsTableRow selectedRow = getSelectedAccountID();
                    if (selectedRow != null)
                    {
                        PasswordsTableModel model =
                            (PasswordsTableModel) accountsTable.getModel();

                        removeSavedPassword(selectedRow.property);
                        model.savedPasswords.remove(selectedRow);

                        int selectedRowIx = accountsTable.getSelectedRow();
                        model.fireTableRowsDeleted(selectedRowIx, selectedRowIx);
                    }
                }
            });
            leftButtons.add(removeButton);

            removeAllButton
                = new JButton(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.REMOVE_ALL_PASSWORDS_BUTTON"));
            removeAllButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    PasswordsTableModel model =
                        (PasswordsTableModel) accountsTable.getModel();
                    if (model.savedPasswords.isEmpty())
                    {
                        return;
                    }

                    int answer
                        = SecurityConfigActivator
                            .getUIService()
                            .getPopupDialog()
                            .showConfirmPopupDialog(
                                resources.getI18NString(
                                        "plugin.securityconfig.masterpassword.REMOVE_ALL_CONFIRMATION"),
                                resources.getI18NString(
                                        "plugin.securityconfig.masterpassword.REMOVE_ALL_TITLE"),
                                PopupDialog.YES_NO_OPTION);

                    if (answer == PopupDialog.YES_OPTION)
                    {
                        for (PasswordsTableRow row : model.savedPasswords)
                        {
                            removeSavedPassword(row.property);
                        }
                        model.savedPasswords.clear();
                        model.fireTableDataChanged();
                    }
                }
            });
            leftButtons.add(removeAllButton);

            JPanel rightButtons = new TransparentPanel();
            pnlButtons.add(rightButtons, BorderLayout.EAST);
            showPasswordsButton
                = new JButton(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.SHOW_PASSWORDS_BUTTON"));
            showPasswordsButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    // show the master password input only when it's set and the
                    // passwords column is hidden
                    if (credentialsStorageService.isUsingMasterPassword()
                        && !showPasswords)
                    {
                        showOrHidePasswordsProtected();
                    }
                    else
                    {
                        showOrHidePasswords();
                    }
                }
            });
            rightButtons.add(showPasswordsButton);
        }

        /**
         * Loads data that support password saving.
         */
        private void initContent()
        {
            // init content with accounts passwords
            PasswordsTableModel model =
                (PasswordsTableModel) accountsTable.getModel();

            for(Map.Entry<AccountID, String> entry :
                SecurityConfigActivator.getAccountIDsWithSavedPasswords()
                    .entrySet())
            {
                AccountID accID = entry.getKey();

                String protocol = accID.getAccountPropertyString(
                        ProtocolProviderFactory.PROTOCOL);

                if(protocol == null)
                    protocol = resources.getI18NString(
                            "plugin.securityconfig.masterpassword.PROTOCOL_UNKNOWN");

                model.savedPasswords.add(
                    new PasswordsTableRow(
                            entry.getValue(),
                            protocol,
                            accID.getUserID()));
            }

            for(Map.Entry<String, String> entry :
                SecurityConfigActivator.getChatRoomsWithSavedPasswords()
                    .entrySet())
            {
                String description = entry.getKey();

                model.savedPasswords.add(
                    new PasswordsTableRow(
                            entry.getValue(),
                            resources.getI18NString("service.gui.CHAT_ROOM"),
                            description));
            }

            // load provisioning passwords
            String PROVISIONING_PROPERTIES_PREFIX =
                "net.java.sip.communicator.plugin.provisioning.auth";
            ConfigurationService configurationService =
                    SecurityConfigActivator.getConfigurationService();
            String uname = configurationService
                .getString(PROVISIONING_PROPERTIES_PREFIX + ".USERNAME");
            if(uname != null && uname.length() > 0)
                model.savedPasswords.add(
                        new PasswordsTableRow(
                                PROVISIONING_PROPERTIES_PREFIX,
                                resources.getI18NString(
                                    "plugin.provisioning.PROVISIONING"),
                                uname));

            // load http passwords
            String HTTP_PROPERTIES_PREFIX =
                    "net.java.sip.communicator.util.http.credential";
            // This will contain double entries for a password
            // one for username and one for the password
            List<String> httpPasses =
                configurationService.getPropertyNamesByPrefix(
                    HTTP_PROPERTIES_PREFIX, false);

            int prefLen = HTTP_PROPERTIES_PREFIX.length() + 1;
            for(String prop: httpPasses)
            {
                // we skip the entry containing the encoded password
                if(prop.contains("PASSWORD")
                    || prefLen > prop.length())
                    continue;

                model.savedPasswords.add(
                    new PasswordsTableRow(
                        prop,
                        "http://" + prop.substring(prefLen),
                        configurationService.getString(prop)));
            }
        }

        /**
         * Removes the password from the storage.
         *
         * @param property for the saved password
         */
        private void removeSavedPassword(String property)
        {
            credentialsStorageService.removePassword(property);
        }

        /**
         * Toggles the passwords column.
         */
        private void showOrHidePasswords()
        {
            showPasswords = !showPasswords;
            showPasswordsButton.setText(
                    resources.getI18NString(
                            showPasswords
                                ? "plugin.securityconfig.masterpassword.HIDE_PASSWORDS_BUTTON"
                                : "plugin.securityconfig.masterpassword.SHOW_PASSWORDS_BUTTON"));
            PasswordsTableModel model =
                (PasswordsTableModel) accountsTable.getModel();
            model.fireTableStructureChanged();
        }

        /**
         * Displays a master password prompt to the user, verifies the entered
         * password and then executes <tt>showOrHidePasswords</tt> method.
         */
        private void showOrHidePasswordsProtected()
        {
            String master;
            boolean correct = true;

            MasterPasswordInputService masterPasswordInputService
                = SecurityConfigActivator.getMasterPasswordInputService();

            if(masterPasswordInputService == null)
            {
                logger.error("Missing MasterPasswordInputService to show input dialog");
                return;
            }

            do
            {
                master = masterPasswordInputService.showInputDialog(correct);
                if (master == null)
                    return;
                correct
                    = (master.length() != 0)
                        && credentialsStorageService
                            .verifyMasterPassword(master);
            }
            while (!correct);
            showOrHidePasswords();
        }

        /**
         * Object representing passwords table row.
         */
        class PasswordsTableRow
        {
            /**
             * The property we can use to extract password from
             * credential service.
             */
            private String property;

            /**
             * String representing type that is displayed to user.
             */
            private String type;

            /**
             * String representing name that is displayed to user.
             */
            private String name;

            /**
             * Constructing.
             * @param property the property
             * @param type the type of the record.
             * @param name the name of the record.
             */
            PasswordsTableRow(String property, String type, String name)
            {
                this.property = property;
                this.type = type;
                this.name = name;
            }
        }
    }
}
