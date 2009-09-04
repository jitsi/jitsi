/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.osgi.framework.*;

import net.java.otr4j.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * 
 * @author George Politis
 * 
 */
@SuppressWarnings("serial")
public class OtrConfigurationPanel
    extends TransparentPanel
{

    class PrivateKeysPanel
        extends TransparentPanel
    {
        class AccountsComboBox
            extends JComboBox
        {

            class AccountsComboBoxItem
            {
                public AccountID accountID;

                public AccountsComboBoxItem(AccountID accountID)
                {
                    this.accountID = accountID;
                }

                public String toString()
                {
                    return accountID.getDisplayName();
                }
            }

            public AccountsComboBox()
            {
                Map<Object, ProtocolProviderFactory> providerFactoriesMap =
                    new Hashtable<Object, ProtocolProviderFactory>();
                
                if (providerFactoriesMap == null)
                    return;

                for (ProtocolProviderFactory providerFactory : providerFactoriesMap
                    .values())
                {
                    for (AccountID accountID : providerFactory
                        .getRegisteredAccounts())
                    {
                        this.addItem(new AccountsComboBoxItem(accountID));
                    }
                }
            }

            public AccountID getSelectedAccountID()
            {
                Object selectedItem = this.getSelectedItem();
                if (selectedItem instanceof AccountsComboBox)
                    return ((AccountsComboBoxItem) selectedItem).accountID;
                else
                    return null;
            }
        }

        private AccountsComboBox cbAccounts;

        private JLabel lblFingerprint;

        private JButton btnGenerate;

        public PrivateKeysPanel()
        {
            this.initComponents();

            this.openAccount(cbAccounts.getSelectedAccountID());
        }

        private void openAccount(AccountID account)
        {
            if (account == null)
            {
                lblFingerprint.setEnabled(false);
                btnGenerate.setEnabled(false);

                lblFingerprint.setText("No key present");
                btnGenerate.setText("Generate");
            }
            else
            {
                lblFingerprint.setEnabled(true);
                btnGenerate.setEnabled(true);

                String fingerprint =
                    OtrActivator.scOtrEngine.getLocalFingerprint(account);

                if (fingerprint == null || fingerprint.length() < 1)
                {
                    lblFingerprint.setText("No key present");
                    btnGenerate.setText("Generate");
                }
                else
                {
                    lblFingerprint.setText(fingerprint);
                    btnGenerate.setText("Re-generate");
                }
            }
        }

        private void initComponents()
        {
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED),
                OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.MY_PRIVATE_KEYS")));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel pnlAccounts = new TransparentPanel();
            this.add(pnlAccounts);

            pnlAccounts.add(new JLabel("Account: "));

            cbAccounts = new AccountsComboBox();
            cbAccounts.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    openAccount(((AccountsComboBox) e.getSource())
                        .getSelectedAccountID());
                }
            });
            pnlAccounts.add(cbAccounts);

            JPanel pnlFingerprint = new TransparentPanel();
            this.add(pnlFingerprint);

            pnlFingerprint.add(new JLabel("Fingerprint: "));

            lblFingerprint = new JLabel();
            pnlFingerprint.add(lblFingerprint);

            btnGenerate = new JButton();
            btnGenerate.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    AccountID account = cbAccounts.getSelectedAccountID();
                    if (account == null)
                        return;
                    OtrActivator.scOtrEngine.generateKeyPair(account
                        .getAccountUniqueID());
                    openAccount(account);
                }
            });
            pnlFingerprint.add(btnGenerate);
        }
    }

    class KnownFingerprintsPanel
        extends TransparentPanel
    {

        class ContactsTableModel
            extends AbstractTableModel
        {
            public java.util.List<Contact> allContacts = new Vector<Contact>();

            public ContactsTableModel()
            {
                // Get the protocolproviders
                ServiceReference[] protocolProviderRefs = null;
                try
                {
                    protocolProviderRefs =
                        OtrActivator.bundleContext.getServiceReferences(
                            ProtocolProviderService.class.getName(), null);
                }
                catch (InvalidSyntaxException ex)
                {
                    return;
                }

                if (protocolProviderRefs == null
                    || protocolProviderRefs.length < 1)
                    return;

                // Get the metacontactlist service.
                ServiceReference ref =
                    OtrActivator.bundleContext
                        .getServiceReference(MetaContactListService.class
                            .getName());

                MetaContactListService service =
                    (MetaContactListService) OtrActivator.bundleContext
                        .getService(ref);

                // Populate contacts.
                for (int i = 0; i < protocolProviderRefs.length; i++)
                {
                    ProtocolProviderService provider =
                        (ProtocolProviderService) OtrActivator.bundleContext
                            .getService(protocolProviderRefs[i]);

                    Iterator<MetaContact> metaContacts =
                        service.findAllMetaContactsForProvider(provider);
                    while (metaContacts.hasNext())
                    {
                        MetaContact metaContact = metaContacts.next();
                        Iterator<Contact> contacts = metaContact.getContacts();
                        while (contacts.hasNext())
                        {
                            allContacts.add(contacts.next());
                        }
                    }
                }
            }

            public static final int CONTACTNAME_INDEX = 0;

            public static final int VERIFIED_INDEX = 1;

            public static final int FINGERPRINT_INDEX = 2;

            public String getColumnName(int column)
            {
                switch (column)
                {
                case CONTACTNAME_INDEX:
                    return "Contact";
                case VERIFIED_INDEX:
                    return "Verified";
                case FINGERPRINT_INDEX:
                    return "Fingerprint";
                default:
                    return null;
                }
            }

            public Object getValueAt(int row, int column)
            {
                if (row < 0)
                    return null;

                Contact contact = allContacts.get(row);
                switch (column)
                {
                case CONTACTNAME_INDEX:
                    return contact.getDisplayName();
                case VERIFIED_INDEX:
                    return (OtrActivator.scOtrEngine.isContactVerified(contact)) ? "Yes"
                        : "No";
                case FINGERPRINT_INDEX:
                    return OtrActivator.scOtrEngine
                        .getRemoteFingerprint(contact);
                default:
                    return null;
                }
            }

            public int getRowCount()
            {
                return allContacts.size();
            }

            public int getColumnCount()
            {
                return 3;
            }
        }

        public KnownFingerprintsPanel()
        {
            this.initComponents();

            openContact(getSelectedContact());
        }

        private Contact getSelectedContact()
        {
            ContactsTableModel model =
                (ContactsTableModel) contactsTable.getModel();
            int index = contactsTable.getSelectedRow();
            if (index < 0 || index > model.allContacts.size())
                return null;

            return model.allContacts.get(index);
        }

        private void openContact(Contact contact)
        {
            if (contact == null)
            {
                btnForgetFingerprint.setEnabled(false);
                btnVerifyFingerprint.setEnabled(false);
            }
            else
            {
                boolean verified =
                    OtrActivator.scOtrEngine.isContactVerified(contact);

                btnForgetFingerprint.setEnabled(verified);
                btnVerifyFingerprint.setEnabled(!verified);
            }
        }

        JButton btnVerifyFingerprint;

        JButton btnForgetFingerprint;

        JTable contactsTable;

        private void initComponents()
        {
            this
                .setBorder(BorderFactory
                    .createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        OtrActivator.resourceService
                            .getI18NString("plugin.otr.configform.KNOWN_FINGERPRINTS")));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            contactsTable = new JTable();
            contactsTable.setModel(new ContactsTableModel());
            contactsTable
                .setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            contactsTable.setCellSelectionEnabled(false);
            contactsTable.setColumnSelectionAllowed(false);
            contactsTable.setRowSelectionAllowed(true);
            contactsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        if (e.getValueIsAdjusting())
                            return;

                        openContact(getSelectedContact());

                    }
                });

            JScrollPane pnlContacts = new JScrollPane(contactsTable);
            this.add(pnlContacts);

            JPanel pnlButtons = new TransparentPanel();
            this.add(pnlButtons);

            btnVerifyFingerprint = new JButton();
            btnVerifyFingerprint.setText("Verify fingerprint");
            btnVerifyFingerprint.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    OtrActivator.scOtrEngine
                        .verifyContactFingerprint(getSelectedContact());

                }
            });
            pnlButtons.add(btnVerifyFingerprint);

            btnForgetFingerprint = new JButton();
            btnForgetFingerprint.setText("Forget fingerprint");
            btnForgetFingerprint.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    OtrActivator.scOtrEngine
                        .forgetContactFingerprint(getSelectedContact());
                }
            });
            pnlButtons.add(btnForgetFingerprint);
        }
    }

    // TODO We should listen for configuration value changes.
    class DefaultOtrPolicyPanel
        extends TransparentPanel
    {
        public DefaultOtrPolicyPanel()
        {
            this.initComponents();
            this.loadPolicy();
        }

        public void loadPolicy()
        {
            OtrPolicy otrPolicy = OtrActivator.scOtrEngine.getGlobalPolicy();

            boolean otrEnabled = otrPolicy.getEnableManual();
            cbEnable.setSelected(otrEnabled);
            cbAutoInitiate.setEnabled(otrEnabled);
            cbRequireOtr.setEnabled(otrEnabled);
            cbAutoInitiate.setSelected(otrPolicy.getEnableAlways());
            cbRequireOtr.setSelected(otrPolicy.getRequireEncryption());
        }

        private SIPCommCheckBox cbEnable;

        private SIPCommCheckBox cbAutoInitiate;

        private SIPCommCheckBox cbRequireOtr;

        private void initComponents()
        {
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED),
                OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.DEFAULT_SETTINGS")));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            cbEnable =
                new SIPCommCheckBox(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.CB_ENABLE"));
            cbEnable.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy =
                        OtrActivator.scOtrEngine.getGlobalPolicy();

                    otrPolicy.setEnableManual(((JCheckBox) e.getSource())
                        .isSelected());

                    OtrActivator.scOtrEngine.setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();
                }
            });
            this.add(cbEnable);

            cbAutoInitiate =
                new SIPCommCheckBox(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.CB_AUTO"));
            cbAutoInitiate.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy =
                        OtrActivator.scOtrEngine.getGlobalPolicy();

                    otrPolicy.setEnableAlways(((JCheckBox) e.getSource())
                        .isSelected());

                    OtrActivator.scOtrEngine.setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();

                }
            });

            this.add(cbAutoInitiate);

            cbRequireOtr =
                new SIPCommCheckBox(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.CB_REQUIRE"));
            cbRequireOtr.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy =
                        OtrActivator.scOtrEngine.getGlobalPolicy();

                    otrPolicy.setRequireEncryption(((JCheckBox) e.getSource())
                        .isSelected());

                    OtrActivator.scOtrEngine.setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();

                }
            });
            this.add(cbRequireOtr);
        }
    }

    public OtrConfigurationPanel()
    {
        this.initComponents();
    }

    private void initComponents()
    {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.PAGE_START;

        JPanel pnlPrivateKeys = new PrivateKeysPanel();
        c.gridy = 0;
        this.add(pnlPrivateKeys, c);

        JPanel pnlPolicy = new DefaultOtrPolicyPanel();
        c.gridy = 1;
        this.add(pnlPolicy, c);

        JPanel pnlFingerprints = new KnownFingerprintsPanel();
        pnlFingerprints.setMinimumSize(new Dimension(Short.MAX_VALUE,
            Short.MAX_VALUE));
        c.weighty = 1.0;
        c.gridy = 2;
        this.add(pnlFingerprints, c);
    }
}