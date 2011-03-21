/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog that displays all saved account passwords.
 *
 * @author Dmitri Melnikov
 */
public class SavedPasswordsDialog
    extends SIPCommDialog
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(SavedPasswordsDialog.class);

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

        AccountPasswordsPanel accPassPanel = new AccountPasswordsPanel();
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
    private static class AccountPasswordsPanel
        extends TransparentPanel
    {

        /**
         * The table model for the accounts table.
         */
        private class AccountsTableModel
            extends AbstractTableModel
        {
            /**
             * Index of the first column.
             */
            public static final int ACCOUNT_TYPE_INDEX = 0;
            /**
             * Index of the second column.
             */
            public static final int ACCOUNT_NAME_INDEX = 1;
            /**
             * Index of the third column.
             */
            public static final int PASSWORD_INDEX = 2;

            /**
             * List of accounts with saved passwords.
             */
            public final List<AccountID> savedPasswordAccounts =
                new ArrayList<AccountID>();
            /**
             * Map that associates an {@link AccountID} with its prefix.
             */
            public final Map<AccountID, String> accountIdPrefixes =
                new HashMap<AccountID, String>();

            /**
             * Loads accounts.
             */
            public AccountsTableModel()
            {
                accountIdPrefixes.putAll(
                        SecurityConfigActivator
                            .getAccountIDsWithSavedPasswords());
                savedPasswordAccounts.addAll(new ArrayList<AccountID>(
                    accountIdPrefixes.keySet()));
            }

            /**
             * Returns the name for the given column.
             * 
             * @param column the column index 
             * @return the column name for the given index
             */
            public String getColumnName(int column)
            {
                String key;

                switch (column)
                {
                case ACCOUNT_TYPE_INDEX:
                    key = "plugin.securityconfig.masterpassword.COL_TYPE";
                    break;
                case ACCOUNT_NAME_INDEX:
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

                AccountID accountID = savedPasswordAccounts.get(row);
                switch (column)
                {
                case ACCOUNT_TYPE_INDEX:
                    String protocol
                        = accountID
                            .getAccountPropertyString(
                                ProtocolProviderFactory.PROTOCOL);
                    return
                        (protocol == null)
                            ? resources
                                .getI18NString(
                                    "plugin.securityconfig.masterpassword.PROTOCOL_UNKNOWN")
                            : protocol;
                case ACCOUNT_NAME_INDEX:
                    return accountID.getUserID();
                case PASSWORD_INDEX:
                    String pass =
                        credentialsStorageService
                            .loadPassword(accountIdPrefixes.get(accountID));
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
                return savedPasswordAccounts.size();
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
        public AccountPasswordsPanel()
        {
            this.initComponents();
        }

        /**
         * Returns the {@link AccountID} object for the selected row.
         * @return the selected account
         */
        private AccountID getSelectedAccountID()
        {
            AccountsTableModel model =
                (AccountsTableModel) accountsTable.getModel();
            int index = accountsTable.getSelectedRow();
            if (index < 0 || index > model.savedPasswordAccounts.size())
                return null;

            return model.savedPasswordAccounts.get(index);
        }

        /**
         * Initializes the table's components.
         */
        private void initComponents()
        {
            setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.STORED_ACCOUNT_PASSWORDS")));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            accountsTable = new JTable();
            accountsTable.setModel(new AccountsTableModel());
            accountsTable
                .setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            accountsTable.setCellSelectionEnabled(false);
            accountsTable.setColumnSelectionAllowed(false);
            accountsTable.setRowSelectionAllowed(true);
            accountsTable.getColumnModel().getColumn(
                AccountsTableModel.ACCOUNT_NAME_INDEX).setPreferredWidth(270);
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
                    AccountID selectedAccountID = getSelectedAccountID();
                    if (selectedAccountID != null)
                    {
                        AccountsTableModel model =
                            (AccountsTableModel) accountsTable.getModel();
                        String accountPrefix = model.accountIdPrefixes.get(selectedAccountID);

                        removeSavedPassword(accountPrefix, selectedAccountID);
                        model.savedPasswordAccounts.remove(selectedAccountID);
                        model.accountIdPrefixes.remove(accountPrefix);

                        int selectedRow = accountsTable.getSelectedRow();
                        model.fireTableRowsDeleted(selectedRow, selectedRow);
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
                    AccountsTableModel model =
                        (AccountsTableModel) accountsTable.getModel();
                    if (model.savedPasswordAccounts.isEmpty())
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
                        for (AccountID accountID : model.savedPasswordAccounts)
                        {
                            String accountPrefix =
                                model.accountIdPrefixes.get(accountID);
                            removeSavedPassword(accountPrefix, accountID);
                        }
                        model.savedPasswordAccounts.clear();
                        model.accountIdPrefixes.clear();
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
         * Removes the password from the storage.
         *
         * @param accountPrefix account prefix
         * @param accountID AccountID object
         */
        private void removeSavedPassword(String accountPrefix, AccountID accountID)
        {
            credentialsStorageService.removePassword(accountPrefix);

            logger.debug(accountID + " removed");
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
            AccountsTableModel model =
                (AccountsTableModel) accountsTable.getModel();
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

            do
            {
                master = MasterPasswordInputDialog.showInput(correct);
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
    }
}
