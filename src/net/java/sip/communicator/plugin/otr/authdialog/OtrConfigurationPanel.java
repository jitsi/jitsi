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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import net.java.otr4j.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;

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

        private JTextField lblFingerprint;

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

                @Override
                public String toString()
                {
                    return accountID.getDisplayName();
                }
            }

            public AccountsComboBox()
            {
                List<AccountID> accountIDs = OtrActivator.getAllAccountIDs();

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

                lblFingerprint.setText(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.NO_KEY_PRESENT"));
                btnGenerate.setText(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.GENERATE"));
            }
            else
            {
                lblFingerprint.setEnabled(true);
                btnGenerate.setEnabled(true);

                String fingerprint =
                    OtrActivator.scOtrKeyManager
                        .getLocalFingerprint(account);

                if (fingerprint == null || fingerprint.length() < 1)
                {
                    lblFingerprint.setText(OtrActivator.resourceService
                        .getI18NString("plugin.otr.configform.NO_KEY_PRESENT"));
                    btnGenerate.setText(OtrActivator.resourceService
                        .getI18NString("plugin.otr.configform.GENERATE"));
                }
                else
                {
                    lblFingerprint.setText(fingerprint);
                    btnGenerate.setText(OtrActivator.resourceService
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
                OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.MY_PRIVATE_KEYS")));

            JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

            labelsPanel.add(new JLabel(OtrActivator.resourceService
                .getI18NString("service.gui.ACCOUNT") + ": "));

            labelsPanel.add(new JLabel(
                OtrActivator.resourceService
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

            lblFingerprint = new JTextField();
            lblFingerprint.setEditable(false);
            lblFingerprint.setOpaque(false);
            lblFingerprint.setBorder(BorderFactory.createEmptyBorder());
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
                    OtrActivator.scOtrKeyManager
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
                = OtrActivator.scOtrEngine.getGlobalPolicy();

            boolean otrEnabled = otrPolicy.getEnableManual();
            cbEnable.setSelected(otrEnabled);
            cbAutoInitiate.setEnabled(otrEnabled);
            cbRequireOtr.setEnabled(otrEnabled);

            boolean isAutoInit = otrPolicy.getEnableAlways();

            cbAutoInitiate.setSelected(isAutoInit);

            String otrMandatoryPropValue
                = OtrActivator.configService.getString(
                    OtrActivator.OTR_MANDATORY_PROP);
            String defaultOtrPropValue
                = OtrActivator.resourceService.getSettingsString(
                    OtrActivator.OTR_MANDATORY_PROP);

            boolean isMandatory = otrPolicy.getRequireEncryption();
            if (otrMandatoryPropValue != null)
                isMandatory = Boolean.parseBoolean(otrMandatoryPropValue);
            else if (!isMandatory && defaultOtrPropValue != null)
                isMandatory = Boolean.parseBoolean(defaultOtrPropValue);

            cbRequireOtr.setSelected(isMandatory);
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
                new SIPCommCheckBox(OtrActivator.resourceService
                    .getI18NString("plugin.otr.configform.CB_ENABLE"));
            cbEnable.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    OtrPolicy otrPolicy
                        = OtrActivator.scOtrEngine
                            .getGlobalPolicy();

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
                        OtrActivator.scOtrEngine
                            .getGlobalPolicy();

                    boolean isAutoInit
                        = ((JCheckBox) e.getSource()).isSelected();

                    otrPolicy.setSendWhitespaceTag(isAutoInit);

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

                    boolean isRequired
                        = ((JCheckBox) e.getSource()).isSelected();

                    otrPolicy.setRequireEncryption(isRequired);

                    OtrActivator.configService.setProperty(
                        OtrActivator.OTR_MANDATORY_PROP,
                        Boolean.toString(isRequired));

                    OtrActivator.scOtrEngine.setGlobalPolicy(otrPolicy);

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
