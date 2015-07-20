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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.plugin.ldap.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.LdapConstants.Auth;
import net.java.sip.communicator.service.ldap.LdapConstants.Encryption;
import net.java.sip.communicator.service.ldap.LdapConstants.Scope;
import net.java.sip.communicator.plugin.desktoputil.*;
//disambiguation

/**
 * The page with hostname/port/encryption fields
 *
 * @author Sebastien Mazy
 * @author Sebastien Vincent
 */
public class DirectorySettingsForm
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * the temporary settings
     */
    private LdapDirectorySettings settings;

    /**
     * Component holding the mail field.
     */
    private JTextField mailField = new JTextField();

    /**
     * Component holding the mail field.
     */
    private JTextField mailSuffixField = new JTextField();

    /**
     * Component holding the work phone field.
     */
    private JTextField workPhoneField = new JTextField();

    /**
     * Component holding the mobile phone field.
     */
    private JTextField mobilePhoneField = new JTextField();

    /**
     * Component holding the home phone field.
     */
    private JTextField homePhoneField = new JTextField();

    /** Radio button for the default query selection. */
    private JRadioButton rdoDefaultQuery
        = new SIPCommRadioButton(
            Resources.getString("impl.ldap.QUERY_DEFAULT"));

    /** Radio button for the default query selection. */
    private JRadioButton rdoCustomQuery
        = new SIPCommRadioButton(
            Resources.getString("impl.ldap.QUERY_CUSTOM"));

    /** Textbox for the custom LDAP query. */
    private JTextArea txtCustomQuery = new JTextArea("", 15, 40);

    /** Checkbox to indicate the automatic wildcard query term expansion */
    private JCheckBox chkMangleQuery = new SIPCommCheckBox(
        Resources.getString("impl.ldap.QUERY_CUSTOM_AUTO_WILDCARD"));

    /** Checkbox to indicate whether photos should be retrieved inline */
    private JCheckBox chkPhotoInline = new SIPCommCheckBox(
        Resources.getString("impl.ldap.QUERY_PHOTO_INLINE"));

    /**
     * component holding the name
     */
    private JTextField nameField;

    /**
     * component holding the hostname
     */
    private JTextField hostnameField;

    /**
     * component holding the connection method
     */
    private JCheckBox encryptionBox = new SIPCommCheckBox(
            Resources.getString("impl.ldap.USE_SSL"));

    /**
     * component displaying the port number
     */
    private JSpinner portSpinner;

    /**
     * model holding the port number
     */
    private SpinnerNumberModel portModel;

    /**
     * the component holding the bind distinguished name
     */
    private JTextField bindDNField;

    /**
     * the component holding the password
     */
    private JPasswordField passwordField;

    /**
     * component holding the name of the server
     *
     * @see net.java.sip.communicator.plugin.ldaptools.components.LdapDNField
     */
    private JTextField baseDNField = new JTextField();

    /**
     * Strings of the authentication method combobox
     * (must be in the same order as in enum Auth)
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants#Auth
     */
     private final String[] authStrings =
     {
        Resources.getString("impl.ldap.AUTH_NONE"),
        Resources.getString("impl.ldap.AUTH_SIMPLE")
     };

    /**
     * Component holding the connection method
     */
    private JComboBox authList = new JComboBox(authStrings);

    /**
     * Strings of the connection method combobox
     * (must be in the same order as in enum Encryption)
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants#Encryption
     */
     private final String[] scopeStrings =
     {
        Resources.getString("impl.ldap.SCOPE_SUB_TREE"),
        Resources.getString("impl.ldap.SCOPE_ONE")
     };

    /**
     * Component holding the connection method
     */
    private JComboBox scopeList = new JComboBox(scopeStrings);

    /**
     * The prefix label.
     */
    private final JLabel prefixLabel = new JLabel(
        Resources.getString("impl.ldap.PHONE_PREFIX"));

    /**
     * The prefix field.
     */
    private final SIPCommTextField prefixField = new SIPCommTextField(
        Resources.getString("impl.ldap.PHONE_PREFIX_EXAMPLE"));

    /**
     * Save button.
     */
    private JButton saveBtn = new JButton(
            Resources.getString("impl.ldap.SAVE"));

    /**
     * Cancel button.
     */
    private JButton cancelBtn = new JButton(
            Resources.getString("impl.ldap.CANCEL"));

    /**
     * Return code.
     */
    private int retCode = 0;

    /**
     * Constructor.
     */
    public DirectorySettingsForm()
    {
        this.settings =
            LdapActivator.getLdapService().getFactory().createServerSettings();
        JTabbedPane pane = new JTabbedPane();
        JPanel panel = new TransparentPanel(new BorderLayout());
        JPanel btnPanel = new TransparentPanel(new FlowLayout(
            FlowLayout.RIGHT));

        this.setTitle(Resources.getString("impl.ldap.CONFIG_FORM_TITLE"));

        setMinimumSize(new Dimension(400, 400));
        setSize(new Dimension(400, 400));
        setPreferredSize(new Dimension(400, 400));

        pane.addTab(Resources.getString("impl.ldap.GENERAL"),
            getContentPanel());
        pane.addTab(Resources.getString("impl.ldap.FIELDS"), getFieldsPanel());
        pane.addTab(Resources.getString("impl.ldap.QUERY"),
            getCustomQueryPanel());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        panel.add(pane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        getContentPane().add(panel);

        pack();
    }

    /**
     * the field panel to display.
     *
     * @return this page's panel
     */
    public JPanel getFieldsPanel()
    {
        JPanel contentPanel = new TransparentPanel(new BorderLayout());
        JPanel basePanel = new TransparentPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel mailLabel = new JLabel(
            Resources.getString("impl.ldap.MAIL_FIELD_NAME"));
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mailLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(mailField, c);
        JLabel mailExampleLabel = new JLabel(
            Resources.getString("impl.ldap.MAIL_FIELD_EXAMPLE"));
        mailExampleLabel.setForeground(Color.GRAY);
        mailExampleLabel.setFont(mailExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mailExampleLabel, c);

        JLabel mailSuffixLabel = new JLabel(
            Resources.getString("impl.ldap.MAILSUFFIX_FIELD_NAME"));
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mailSuffixLabel, c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(mailSuffixField, c);
        JLabel mailSuffixExampleLabel = new JLabel(
            Resources.getString("impl.ldap.MAILSUFFIX_FIELD_EXAMPLE"));
        mailSuffixExampleLabel.setForeground(Color.GRAY);
        mailSuffixExampleLabel.setFont(
            mailSuffixExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mailSuffixExampleLabel, c);

        JLabel workPhoneLabel = new JLabel(
            Resources.getString("impl.ldap.WORKPHONE_FIELD_NAME"));
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(workPhoneLabel, c);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(workPhoneField, c);
        JLabel workPhoneExampleLabel = new JLabel(
            Resources.getString("impl.ldap.WORKPHONE_FIELD_EXAMPLE"));
        workPhoneExampleLabel.setForeground(Color.GRAY);
        workPhoneExampleLabel.setFont(
            workPhoneExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(workPhoneExampleLabel, c);

        JLabel mobilePhoneLabel = new JLabel(
            Resources.getString("impl.ldap.MOBILEPHONE_FIELD_NAME"));
        c.gridx = 0;
        c.gridy = 6;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mobilePhoneLabel, c);
        c.gridx = 1;
        c.gridy = 6;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(mobilePhoneField, c);
        JLabel mobilePhoneExampleLabel = new JLabel(
            Resources.getString("impl.ldap.MOBILEPHONE_FIELD_EXAMPLE"));
        mobilePhoneExampleLabel.setForeground(Color.GRAY);
        mobilePhoneExampleLabel.setFont(
            mobilePhoneExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 7;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(mobilePhoneExampleLabel, c);

        JLabel homePhoneLabel = new JLabel(
            Resources.getString("impl.ldap.HOMEPHONE_FIELD_NAME"));
        c.gridx = 0;
        c.gridy = 8;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(homePhoneLabel, c);
        c.gridx = 1;
        c.gridy = 8;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(homePhoneField, c);
        JLabel homePhoneExampleLabel = new JLabel(
            Resources.getString("impl.ldap.HOMEPHONE_FIELD_EXAMPLE"));
        homePhoneExampleLabel.setForeground(Color.GRAY);
        homePhoneExampleLabel.setFont(
            homePhoneExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 9;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(homePhoneExampleLabel, c);

        contentPanel.add(basePanel, BorderLayout.CENTER);
        return contentPanel;
    }

    /**
     * the panel to display in the card layout of the wizard
     *
     * @return this page's panel
     */
    public JPanel getContentPanel()
    {
        JPanel contentPanel = new TransparentPanel(new BorderLayout());
        JPanel mainPanel = new TransparentPanel();
        JPanel basePanel = new TransparentPanel(new GridBagLayout());
        JPanel authPanel = new TransparentPanel(new GridBagLayout());
        JPanel searchPanel = new TransparentPanel(new GridBagLayout());
        JPanel prefixPanel = new TransparentPanel(new GridBagLayout());

        //JPanel btnPanel = new TransparentPanel(new FlowLayout(
        //        FlowLayout.RIGHT));
        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);

        GridBagConstraints c = new GridBagConstraints();

        /* name text field */
        JLabel nameLabel = new JLabel(
                Resources.getString("impl.ldap.SERVER_NAME"));
        //Resources.getString("ldapNameLabel"));
        this.nameField = new JTextField();
        nameLabel.setLabelFor(nameField);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(nameLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(nameField, c);
        JLabel nameExampleLabel = new JLabel(
            Resources.getString("impl.ldap.SERVER_NAME_EXAMPLE"));
        //Resources.getString("ldapNameExample"));
        nameExampleLabel.setForeground(Color.GRAY);
        nameExampleLabel.setFont(nameExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(nameExampleLabel, c);

        /* hostname text field */
        JLabel hostnameLabel = new JLabel(
                Resources.getString("impl.ldap.SERVER_HOSTNAME"));
        //Resources.getString("ldapHostnameLabel"));
        this.hostnameField = new JTextField();
        hostnameLabel.setLabelFor(hostnameField);
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(hostnameLabel, c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(hostnameField, c);
        JLabel hostExampleLabel = new JLabel("ldap.example.org");
        hostExampleLabel.setForeground(Color.GRAY);
        hostExampleLabel.setFont(hostExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(hostExampleLabel, c);

        /* network port number */
        JLabel portLabel = new JLabel(
                Resources.getString("impl.ldap.SERVER_PORT"));
        this.portModel = new SpinnerNumberModel(389, 1, 65535, 1);
        this.portSpinner = new JSpinner(portModel);
        portLabel.setLabelFor(portSpinner);
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 50, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(portLabel, c);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(portSpinner, c);
        c.gridx = 1;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(encryptionBox, c);

        /* bind DN (Distinguished Name) */
        authList.setSelectedIndex(0);
        JLabel authLabel = new JLabel(
                Resources.getString("impl.ldap.AUTHENTICATION"));
        authLabel.setLabelFor(authList);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 2, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        authPanel.add(authLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        authPanel.add(authList, c);

        JLabel bindDNLabel = new JLabel(
                Resources.getString("impl.ldap.USERNAME"));
        this.bindDNField = new JTextField();
        bindDNLabel.setLabelFor(this.bindDNField);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        authPanel.add(bindDNLabel, c);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        authPanel.add(this.bindDNField, c);
        JLabel bindDNExampleLabel = new JLabel("uid=user,o=example");
        bindDNExampleLabel.setForeground(Color.GRAY);
        bindDNExampleLabel.setFont(bindDNExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        authPanel.add(bindDNExampleLabel, c);
        /* password */
        JLabel passwordLabel = new JLabel(
                Resources.getString("impl.ldap.PASSWORD"));
        this.passwordField = new JPasswordField();
        passwordLabel.setLabelFor(this.passwordField);
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 2, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        authPanel.add(passwordLabel, c);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 20, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        authPanel.add(this.passwordField, c);

        /* base distinguished name selection text field */
        JLabel baseDNLabel = new JLabel(
                Resources.getString("impl.ldap.SEARCH_BASE"));
        baseDNLabel.setLabelFor(baseDNField);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        searchPanel.add(baseDNLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        searchPanel.add(baseDNField, c);
        JLabel baseDNExampleLabel = new JLabel("o=example");
        baseDNExampleLabel.setForeground(Color.GRAY);
        baseDNExampleLabel.setFont(baseDNExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        searchPanel.add(baseDNExampleLabel, c);

        /* search scope */
        scopeList.setSelectedIndex(0);
        JLabel scopeLabel = new JLabel(
                Resources.getString("impl.ldap.SCOPE"));
        scopeLabel.setLabelFor(scopeList);
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        searchPanel.add(scopeLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        searchPanel.add(scopeList, c);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        prefixPanel.add(prefixLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        prefixPanel.add(prefixField, c);

        mainPanel.setLayout(boxLayout);
        mainPanel.add(basePanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(searchPanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(authPanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(prefixPanel);

        /* listeners */
        this.encryptionBox.addActionListener(this);
        this.nameField.addActionListener(this);
        this.hostnameField.addActionListener(this);
        this.bindDNField.addActionListener(this);
        this.passwordField.addActionListener(this);
        this.saveBtn.addActionListener(this);
        this.cancelBtn.addActionListener(this);
        this.authList.addActionListener(this);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
        //contentPanel.add(btnPanel, BorderLayout.SOUTH);

        return contentPanel;
    }

    /**
     * Creates the for the definition of a custom query.
     * 
     * @return a panel for a custom query definition.
     */
    private JPanel getCustomQueryPanel()
    {
        rdoDefaultQuery.addActionListener(this);
        rdoCustomQuery.addActionListener(this);

        JPanel p = new TransparentPanel();
        p.setLayout(new BorderLayout());

        JPanel options = new TransparentPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(rdoDefaultQuery);
        options.add(rdoCustomQuery);
        p.add(options, BorderLayout.NORTH);

        p.add(txtCustomQuery, BorderLayout.CENTER);
        txtCustomQuery.setBorder(new LineBorder(Color.black));
        txtCustomQuery.setLineWrap(true);
        txtCustomQuery.setWrapStyleWord(false);

        JPanel hints = new TransparentPanel();
        hints.setLayout(new BoxLayout(hints, BoxLayout.Y_AXIS));
        hints.add(
            new JLabel(Resources.getString("impl.ldap.QUERY_CUSTOM_HINT",
                new String[] { "<query>" })));
        hints.add(chkMangleQuery);
        hints.add(chkPhotoInline);
        p.add(hints, BorderLayout.SOUTH);

        return p;
    }

    /**
     * Loads the information from the LdapDirectorySettings instance
     * into the UI.
     *
     * @param settings the LdapDirectorySettings to load
     *
     * @see net.java.sip.communicator.service.ldap.LdapDirectorySettings
     */
    public void loadData(LdapDirectorySettings settings)
    {
        //load data from introduction page
        this.nameField.setText(settings.getName());

        // load data from connection page
        this.hostnameField.setText(settings.getHostname());

        this.encryptionBox.setSelected(
                settings.getEncryption() == Encryption.SSL);

        if(settings.getPort() != 0)
        {
            this.portModel.setValue(new Integer(settings.getPort()));
        }
        else
        {
            this.portModel.setValue(
                    new Integer(settings.getEncryption().defaultPort()));
        }

        this.bindDNField.setText(settings.getBindDN());
        this.passwordField.setText(settings.getPassword());

        // load data from search page
        this.baseDNField.setText(settings.getBaseDN());

        this.scopeList.setSelectedIndex(settings.getScope().ordinal());
        this.authList.setSelectedIndex(settings.getAuth().ordinal());
        this.bindDNField.setEnabled(settings.getAuth() == Auth.SIMPLE);
        this.passwordField.setEnabled(settings.getAuth() == Auth.SIMPLE);

        this.mailField.setText(mergeStrings(settings.getMailSearchFields()));
        this.mailSuffixField.setText(settings.getMailSuffix());
        this.workPhoneField.setText(
            mergeStrings(settings.getWorkPhoneSearchFields()));
        this.mobilePhoneField.setText(
            mergeStrings(settings.getMobilePhoneSearchFields()));
        this.homePhoneField.setText(
            mergeStrings(settings.getHomePhoneSearchFields()));
        this.prefixField.setText(settings.getGlobalPhonePrefix());
        this.rdoDefaultQuery.setSelected(
            !"custom".equals(settings.getQueryMode()));
        this.rdoCustomQuery.setSelected(
            "custom".equals(settings.getQueryMode()));
        this.txtCustomQuery.setText(settings.getCustomQuery());
        this.chkMangleQuery.setSelected(settings.isMangleQuery());
        this.chkPhotoInline.setSelected(settings.isPhotoInline());

        txtCustomQuery.setEditable(rdoCustomQuery.isSelected());
        txtCustomQuery.setEnabled(rdoCustomQuery.isSelected());
    }

    /**
     * Merge String elements from a list to a single String separated by space.
     *
     * @param lst list of <tt>String</tt>s
     * @return <tt>String</tt>
     */
    public static String mergeStrings(List<String> lst)
    {
        StringBuilder bld = new StringBuilder();

        for(String s : lst)
        {
            bld.append(s).append(" ");
        }

        return bld.toString();
    }

    /**
     * Merge String elements separated by space into a List.
     *
     * @param attrs <tt>String</tt>
     * @return list of <tt>String</tt>
     */
    public static List<String> mergeString(String attrs)
    {
        StringTokenizer token = new StringTokenizer(attrs, " ");
        List<String> lst = new ArrayList<String>();

        while(token.hasMoreTokens())
        {
            lst.add(token.nextToken());
        }

        return lst;
    }

    /**
     * Saves the information in the LdapDirectorySettings instance
     * from the UI
     *
     * @param settings the LdapDirectorySettings to save the data in
     *
     * @see net.java.sip.communicator.service.ldap.LdapDirectorySettings
     */
    public void saveData(LdapDirectorySettings settings)
    {
        settings.setName(this.nameField.getText());
        settings.setHostname(hostnameField.getText());

        if(encryptionBox.isSelected())
        {
            settings.setEncryption(Encryption.SSL);
        }
        else
        {
            settings.setEncryption(Encryption.CLEAR);
        }

        // auth simple/none
        switch(authList.getSelectedIndex())
        {
            case 0:
                settings.setAuth(Auth.NONE);
                break;
            case 1:
                settings.setAuth(Auth.SIMPLE);
                break;
        }

        settings.setPort(((Integer)this.portSpinner.getValue()).intValue());

        settings.setBindDN(this.bindDNField.getText());

        settings.setPassword(new String(this.passwordField.getPassword()));

        settings.setBaseDN(this.baseDNField.getText());

        switch(scopeList.getSelectedIndex())
        {
            case 0:
                settings.setScope(Scope.SUB);
                break;
            case 1:
                settings.setScope(Scope.ONE);
                break;
        }

        settings.setMailSearchFields(mergeString(mailField.getText()));
        settings.setMailSuffix(mailSuffixField.getText());
        settings.setWorkPhoneSearchFields(
            mergeString(workPhoneField.getText()));
        settings.setMobilePhoneSearchFields(
            mergeString(mobilePhoneField.getText()));
        settings.setHomePhoneSearchFields(
            mergeString(homePhoneField.getText()));
        settings.setGlobalPhonePrefix(prefixField.getText());
        settings.setQueryMode(rdoCustomQuery.isSelected()
            ? "custom"
            : null);
        settings.setCustomQuery(txtCustomQuery.getText());
        settings.setMangleQuery(chkMangleQuery.isSelected());
        settings.setPhotoInline(chkPhotoInline.isSelected());
    }

    /**
     * Implementation of actionPerformed.
     *
     * @param e the ActionEvent triggered
     */
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();

        if(src == encryptionBox)
        {
            int port = ((Integer)portSpinner.getValue()).intValue();

            if(encryptionBox.isSelected())
            {
                if(port == LdapConstants.Encryption.CLEAR.defaultPort())
                {
                    portSpinner.setValue(new Integer(
                            LdapConstants.Encryption.SSL.defaultPort()));
                }
            }
            else
            {
                if(port == LdapConstants.Encryption.SSL.defaultPort())
                {
                    portSpinner.setValue(new Integer(
                            LdapConstants.Encryption.CLEAR.defaultPort()));
                }
            }
        }
        else if(src == saveBtn)
        {
            /* check if name does not match existing server ones */
            if(this.nameField.isEnabled())
            {
                for(LdapDirectory s :
                    LdapActivator.getLdapService().getServerSet())
                {
                    if(s.getSettings().getName().equals(nameField.getText()))
                    {
                        JOptionPane.showMessageDialog(
                                this,
                                Resources.getString(
                                "impl.ldap.SERVER_EXIST"),
                                Resources.getString(
                                        "impl.ldap.SERVER_EXIST"),
                                        JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }

            this.saveData(settings);
            retCode = 1;
            dispose();
        }
        else if(src == cancelBtn)
        {
            retCode = 0;
            dispose();
        }
        else if(src == authList)
        {
            bindDNField.setEnabled(authList.getSelectedIndex() == 1);
            passwordField.setEnabled(authList.getSelectedIndex() == 1);
        }
        else if (src == rdoDefaultQuery)
        {
            txtCustomQuery.setEnabled(false);
            txtCustomQuery.setEditable(false);
            rdoCustomQuery.setSelected(false);
        }
        else if (src == rdoCustomQuery)
        {
            txtCustomQuery.setEnabled(true);
            txtCustomQuery.setEditable(true);
            rdoDefaultQuery.setSelected(false);
        }
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key.
     *
     * @param escaped <tt>true</tt> if this dialog has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    @Override
    protected void close(boolean escaped)
    {
        cancelBtn.doClick();
    }

    /**
     * Show the dialog and returns if the user has modified something (create
     * or modify entry).
     *
     * @return true if the user has modified something (create
     * or modify entry), false otherwise.
     */
    public int showDialog()
    {
        retCode = 0;
        setVisible(true);

        // this will block until user click on save/cancel/press escape/close
        // the window
        setVisible(false);
        return retCode;
    }

    /**
     * Returns LDAP settings.
     *
     * @return LDAP settings
     */
    public LdapDirectorySettings getSettings()
    {
        return settings;
    }

    /**
     * Set the name field enable or not
     *
     * @param enable parameter to set
     */
    public void setNameFieldEnabled(boolean enable)
    {
        this.nameField.setEnabled(enable);
    }

    /**
     * Set the hostname field enable or not
     *
     * @param enable parameter to set
     */
    public void setHostnameFieldEnabled(boolean enable)
    {
        this.hostnameField.setEnabled(enable);
    }

    /**
     * Set the base DN field enable or not
     *
     * @param enable parameter to set
     */
    public void setBaseDNFieldEnabled(boolean enable)
    {
        this.scopeList.setEnabled(enable);
        this.baseDNField.setEnabled(enable);
    }

    /**
     * Set the port field enable or not
     *
     * @param enable parameter to set
     */
    public void setPortFieldEnabled(boolean enable)
    {
        this.encryptionBox.setEnabled(enable);
        this.portSpinner.setEnabled(enable);
    }
}
