/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import net.java.otr4j.*;
import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A special {@link Panel} that manages the OTR configuration.
 * 
 * @author George Politis
 */
@SuppressWarnings("serial")
public class OtrConfigurationPanel
    extends TransparentPanel
{
    /**
     * Creates the <tt>OtrConfigurationPanel</tt>
     */
    public OtrConfigurationPanel()
    {
        this.initComponents();
    }

    /**
     * A special {@link Panel} for Private Keys display.
     * 
     * @author George Politis
     */
    private static class PrivateKeysPanel
        extends TransparentPanel
    {
        private AccountsComboBox cbAccounts;

        private JLabel lblFingerprint;

        private JButton btnGenerate;

        public PrivateKeysPanel()
        {
            super(new BorderLayout());

            this.initComponents();

            this.openAccount(cbAccounts.getSelectedAccountID());
        }

        /**
         * A special {@link JComboBox} for {@link AccountID} enumeration.
         * 
         * @author George Politis
         */
        private static class AccountsComboBox
            extends JComboBox
        {
            /**
             * A class hosted in an {@link AccountsComboBox} that holds a single
             * {@link AccountID}.
             * 
             * @author George Politis
             */
            private static class AccountsComboBoxItem
            {
                public final AccountID accountID;

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
                List<AccountID> accountIDs
                    = SecurityConfigActivator.getAllAccountIDs();

                if (accountIDs == null)
                    return;

                for (AccountID accountID : accountIDs)
                    this.addItem(new AccountsComboBoxItem(accountID));
            }

            /**
             * Gets the selected {@link AccountID} for this
             * {@link AccountsComboBox}.
             * 
             * @return the selected account id
             */
            public AccountID getSelectedAccountID()
            {
                Object selectedItem = this.getSelectedItem();
                if (selectedItem instanceof AccountsComboBoxItem)
                    return ((AccountsComboBoxItem) selectedItem).accountID;
                else
                    return null;
            }
        }

        /**
         * Sets up the {@link PrivateKeysPanel} components so that they reflect
         * the {@link AccountID} param.
         * 
         * @param account the {@link AccountID} to setup the components for.
         */
        private void openAccount(AccountID account)
        {
            if (account == null)
            {
                lblFingerprint.setEnabled(false);
                btnGenerate.setEnabled(false);

                lblFingerprint.setText(SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.NO_KEY_PRESENT"));
                btnGenerate.setText(SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.GENERATE"));
            }
            else
            {
                lblFingerprint.setEnabled(true);
                btnGenerate.setEnabled(true);

                String fingerprint =
                    SecurityConfigActivator.getOtrKeyManagerService()
                        .getLocalFingerprint(account);

                if (fingerprint == null || fingerprint.length() < 1)
                {
                    lblFingerprint.setText(SecurityConfigActivator.getResources()
                        .getI18NString("plugin.otr.configform.NO_KEY_PRESENT"));
                    btnGenerate.setText(SecurityConfigActivator.getResources()
                        .getI18NString("plugin.otr.configform.GENERATE"));
                }
                else
                {
                    lblFingerprint.setText(fingerprint);
                    btnGenerate.setText(SecurityConfigActivator.getResources()
                        .getI18NString("plugin.otr.configform.REGENERATE"));
                }
            }
        }

        /**
         * Initializes the {@link PrivateKeysPanel} components.
         */
        private void initComponents()
        {
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED),
                SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.MY_PRIVATE_KEYS")));

            JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

            labelsPanel.add(new JLabel(SecurityConfigActivator.getResources()
                .getI18NString("service.gui.ACCOUNT") + ": "));

            labelsPanel.add(new JLabel(
                SecurityConfigActivator.getResources()
                .getI18NString("plugin.otr.configform.FINGERPRINT") + ": "));

            JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));

            cbAccounts = new AccountsComboBox();
            cbAccounts.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    openAccount(((AccountsComboBox) e.getSource())
                        .getSelectedAccountID());
                }
            });
            valuesPanel.add(cbAccounts);

            lblFingerprint = new JLabel();
            valuesPanel.add(lblFingerprint);

            JPanel buttonPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            btnGenerate = new JButton();
            buttonPanel.add(btnGenerate);
            btnGenerate.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    AccountID account = cbAccounts.getSelectedAccountID();
                    if (account == null)
                        return;
                    SecurityConfigActivator.getOtrKeyManagerService()
                        .generateKeyPair(account);
                    openAccount(account);
                }
            });

            add(labelsPanel, BorderLayout.WEST);
            add(valuesPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.EAST);
        }
    }

    /**
     * A special {@link Panel} for OTR policy display.
     * 
     * @author George Politis
     */
    private static class DefaultOtrPolicyPanel
        extends TransparentPanel
    {
        // TODO We should listen for configuration value changes.
        public DefaultOtrPolicyPanel()
        {
            this.initComponents();
            this.loadPolicy();
        }

        /**
         * Sets up the {@link DefaultOtrPolicyPanel} components so that they
         * reflect the global OTR policy.
         * 
         */
        public void loadPolicy()
        {
            OtrPolicy otrPolicy
                = SecurityConfigActivator.getOtrEngineService()
                    .getGlobalPolicy();

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

        /**
         * Initializes the {@link DefaultOtrPolicyPanel} components.
         */
        private void initComponents()
        {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            cbEnable =
                new SIPCommCheckBox(SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.CB_ENABLE"));
            cbEnable.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy
                        = SecurityConfigActivator.getOtrEngineService()
                            .getGlobalPolicy();

                    otrPolicy.setEnableManual(((JCheckBox) e.getSource())
                        .isSelected());

                    SecurityConfigActivator.getOtrEngineService()
                        .setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();
                }
            });
            this.add(cbEnable);

            cbAutoInitiate =
                new SIPCommCheckBox(SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.CB_AUTO"));
            cbAutoInitiate.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy =
                        SecurityConfigActivator.getOtrEngineService()
                            .getGlobalPolicy();

                    otrPolicy.setEnableAlways(((JCheckBox) e.getSource())
                        .isSelected());

                    SecurityConfigActivator.getOtrEngineService()
                        .setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();
                }
            });

            this.add(cbAutoInitiate);

            cbRequireOtr =
                new SIPCommCheckBox(SecurityConfigActivator.getResources()
                    .getI18NString("plugin.otr.configform.CB_REQUIRE"));
            cbRequireOtr.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy =
                        SecurityConfigActivator.getOtrEngineService()
                            .getGlobalPolicy();

                    otrPolicy.setRequireEncryption(((JCheckBox) e.getSource())
                        .isSelected());

                    SecurityConfigActivator.getOtrEngineService()
                        .setGlobalPolicy(otrPolicy);

                    DefaultOtrPolicyPanel.this.loadPolicy();

                }
            });
            this.add(cbRequireOtr);
        }
    }

    /**
     * Initializes all 3 panels of the {@link OtrConfigurationPanel}
     */
    private void initComponents()
    {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.PAGE_START;

        JPanel pnlPrivateKeys = new PrivateKeysPanel();
        c.gridy = 0;
        this.add(pnlPrivateKeys, c);

        JPanel pnlFingerprints = new KnownFingerprintsPanel();
        pnlFingerprints.setMinimumSize(new Dimension(Short.MAX_VALUE, 160));
        c.weighty = 1.0;
        c.gridy = 1;
        this.add(pnlFingerprints, c);

        JPanel pnlPolicy = new DefaultOtrPolicyPanel();
        c.gridy = 2;
        this.add(pnlPolicy, c);
    }
}