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
package net.java.sip.communicator.plugin.provisioning;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class ProvisioningForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The enable provisioning check box.
     */
    private final JCheckBox enableCheckBox;

    /**
     * The DHCP provisioning discovery button.
     */
    private final JRadioButton dhcpButton;

    /**
     * The DNS provisioning discovery button.
     */
    private final JRadioButton dnsButton;

    /**
     * The Bonjour provisioning discovery button.
     */
    private final JRadioButton bonjourButton;

    /**
     * The manual provisioning button.
     */
    private final JRadioButton manualButton;

    /**
     * The URI field to specify manually a provisioning server.
     */
    private final SIPCommTextField uriField;

    /**
     * The field used to show the username.
     */
    private final JTextField usernameField;

    /**
     * A field to show the password.
     */
    private final JPasswordField passwordField;

    /**
     * The button that will delete the password.
     */
    private final JButton forgetPasswordButton;

    /**
     * Creates an instance of the <tt>ProvisioningForm</tt>.
     */
    public ProvisioningForm()
    {
        super(new BorderLayout());

        final ResourceManagementService resources
            = ProvisioningActivator.getResourceService();

        ConfigurationService config
            = ProvisioningActivator.getConfigurationService();

        enableCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.provisioning.ENABLE_DISABLE"));

        dhcpButton = new SIPCommRadioButton(
            resources.getI18NString("plugin.provisioning.DHCP"));

        dnsButton = new SIPCommRadioButton(
            resources.getI18NString("plugin.provisioning.DNS"));

        bonjourButton = new SIPCommRadioButton(
            resources.getI18NString("plugin.provisioning.BONJOUR"));

        manualButton = new SIPCommRadioButton(
            resources.getI18NString("plugin.provisioning.MANUAL"));

        uriField = new SIPCommTextField(
            resources.getI18NString("plugin.provisioning.URI"));

        JPanel mainPanel = new TransparentPanel();

        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        enableCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        mainPanel.add(enableCheckBox, c);

        final ButtonGroup buttonGroup = new ButtonGroup();

        buttonGroup.add(dhcpButton);
        buttonGroup.add(bonjourButton);
        buttonGroup.add(dnsButton);
        buttonGroup.add(manualButton);

        final JPanel radioButtonPanel
            = new TransparentPanel(new GridLayout(0, 1));

        radioButtonPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("plugin.provisioning.AUTO")));

        radioButtonPanel.add(dhcpButton);
        radioButtonPanel.add(bonjourButton);
        radioButtonPanel.add(dnsButton);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 20, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        mainPanel.add(radioButtonPanel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 26, 0, 0);
        c.gridx = 0;
        c.gridy = 2;
        mainPanel.add(manualButton, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 51, 0, 0);
        c.gridx = 0;
        c.gridy = 3;
        mainPanel.add(uriField, c);

        JPanel uuidPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.LEFT));

        final JTextField uuidPane = new JTextField();
        uuidPane.setEditable(false);
        uuidPane.setOpaque(false);
        uuidPane.setText(
                config.getString(ProvisioningServiceImpl.
                    PROVISIONING_UUID_PROP));

        uuidPanel.add(new JLabel(resources.getI18NString(
                "plugin.provisioning.UUID")));
        uuidPanel.add(uuidPane);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.insets = new Insets(10, 10, 0, 0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy = 4;
        mainPanel.add(uuidPanel, c);

        JButton clipboardBtn = new JButton(resources.getI18NString(
                "plugin.provisioning.COPYTOCLIPBOARD"));
        clipboardBtn.addActionListener(new ActionListener()
        {
            /**
             * {@inheritsDoc}
             */
            public void actionPerformed(ActionEvent evt)
            {
                Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
                if(clipboard != null)
                {
                    String selection = uuidPane.getText();
                    StringSelection data = new StringSelection(selection);
                    clipboard.setContents(data, data);
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            ProvisioningForm.this,
                            resources.getI18NString(
                                    "plugin.provisioning.CLIPBOARD_FAILED"),
                            resources.getI18NString(
                                    "plugin.provisioning.CLIPBOARD_FAILED"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridwidth = 0;
        c.gridx = 0;
        c.gridy = 5;
        mainPanel.add(clipboardBtn, c);

        JPanel userPassPanel = new TransparentPanel(new BorderLayout());
        userPassPanel.setBorder(BorderFactory.createTitledBorder(
                ProvisioningActivator.getResourceService().getI18NString(
                    "plugin.provisioning.CREDENTIALS")));
        JPanel labelPanel = new TransparentPanel(new GridLayout(0, 1));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));
        valuesPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
        labelPanel.add(new JLabel(
            ProvisioningActivator.getResourceService().getI18NString(
                "plugin.simpleaccregwizz.LOGIN_USERNAME")));
        labelPanel.add(new JLabel(
            ProvisioningActivator.getResourceService().getI18NString(
                "service.gui.PASSWORD")));
        usernameField = new JTextField();
        usernameField.setEditable(false);
        passwordField = new JPasswordField();
        passwordField.setEditable(false);
        valuesPanel.add(usernameField);
        valuesPanel.add(passwordField);
        userPassPanel.add(labelPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        forgetPasswordButton = new JButton(resources
            .getI18NString("plugin.provisioning.FORGET_PASSWORD"));
        buttonPanel.add(forgetPasswordButton);
        userPassPanel.add(buttonPanel, BorderLayout.SOUTH);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridwidth = 0;
        c.gridx = 0;
        c.gridy = 6;
        mainPanel.add(userPassPanel, c);

        JTextPane pane = new JTextPane();
        pane.setForeground(Color.RED);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(ProvisioningActivator.getResourceService().getI18NString(
            "plugin.provisioning.RESTART_WARNING",
            new String[]{ProvisioningActivator.getResourceService()
                .getSettingsString("service.gui.APPLICATION_NAME")}));

        c.gridy = 7;
        mainPanel.add(pane, c);

        initButtonStates();
        initListeners();
    }

    /**
     * Initializes all contained components.
     */
    private void initButtonStates()
    {
        String provMethod = ProvisioningActivator.getProvisioningService().
            getProvisioningMethod();
        boolean isProvEnabled
            = (provMethod != null
                && provMethod.length() > 0
                && !provMethod.equals("NONE"));

        enableCheckBox.setSelected(isProvEnabled);

        if (isProvEnabled)
        {
            if (provMethod.equals("DHCP"))
                dhcpButton.setSelected(true);
            else if (provMethod.equals("DNS"))
                dnsButton.setSelected(true);
            else if (provMethod.equals("Bonjour"))
                bonjourButton.setSelected(true);
            else if (provMethod.equals("Manual"))
            {
                manualButton.setSelected(true);

                String uri = ProvisioningActivator.getProvisioningService().
                    getProvisioningUri();
                if (uri != null)
                    uriField.setText(uri);
            }
        }

        dhcpButton.setEnabled(isProvEnabled);
        manualButton.setEnabled(isProvEnabled);
        uriField.setEnabled(manualButton.isSelected());
        bonjourButton.setEnabled(isProvEnabled);
        dnsButton.setEnabled(false);

        // creadentials
        forgetPasswordButton.setEnabled(isProvEnabled);
        usernameField.setText(ProvisioningActivator.getConfigurationService()
            .getString(ProvisioningServiceImpl.PROPERTY_PROVISIONING_USERNAME));

        if(ProvisioningActivator.getCredentialsStorageService()
            .isStoredEncrypted(
                    ProvisioningServiceImpl.PROPERTY_PROVISIONING_PASSWORD))
        {
            passwordField.setText(
                ProvisioningActivator.getCredentialsStorageService()
                .loadPassword(
                    ProvisioningServiceImpl.PROPERTY_PROVISIONING_PASSWORD));
        }
    }

    /**
     * Initializes all listeners.
     */
    private void initListeners()
    {
        enableCheckBox.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                boolean isSelected = enableCheckBox.isSelected();

                dhcpButton.setEnabled(isSelected);
                bonjourButton.setEnabled(isSelected);
                manualButton.setEnabled(isSelected);
                forgetPasswordButton.setEnabled(isSelected);

                String provisioningMethod = null;

                if (isSelected)
                {
                    if (dhcpButton.isSelected())
                    {
                        provisioningMethod = "DHCP";
                    }
                    else if (dnsButton.isSelected())
                    {
                        provisioningMethod = "DNS";
                    }
                    else if (bonjourButton.isSelected())
                    {
                        provisioningMethod = "Bonjour";
                    }
                    else if (manualButton.isSelected())
                    {
                        provisioningMethod = "Manual";
                    }
                    else
                    {
                        dhcpButton.setSelected(true);
                        provisioningMethod = "DHCP";
                    }
                }

                ProvisioningActivator.getProvisioningService().
                    setProvisioningMethod(provisioningMethod);
            }
        });

        dhcpButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (dhcpButton.isSelected())
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningMethod("DHCP");
            }
        });

        dnsButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (dnsButton.isSelected())
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningMethod("DNS");
            }
        });

        bonjourButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (bonjourButton.isSelected())
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningMethod("Bonjour");
            }
        });

        manualButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                boolean isSelected = manualButton.isSelected();

                uriField.setEnabled(isSelected);

                if (isSelected)
                {
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningMethod("Manual");

                    String uriText = uriField.getText();
                    if (uriText != null && uriText.length() > 0)
                        ProvisioningActivator.getProvisioningService().
                            setProvisioningUri(uriText);
                }
                else
                {
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningUri(null);
                }
            }
        });

        uriField.addFocusListener(new FocusListener()
        {
            public void focusLost(FocusEvent e)
            {
                // If the manual button isn't selected we have nothing more
                // to do here.
                if (!manualButton.isSelected())
                    return;

                String uriText = uriField.getText();
                if (uriText != null && uriText.length() > 0)
                    ProvisioningActivator.getProvisioningService().
                        setProvisioningUri(uriText);
            }

            public void focusGained(FocusEvent e) {}
        });

        forgetPasswordButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                if(passwordField.getPassword() == null
                    || passwordField.getPassword().length == 0)
                {
                    return;
                }

                int result = JOptionPane.showConfirmDialog(
                    (Component)ProvisioningActivator.getUIService()
                        .getExportedWindow(ExportedWindow.MAIN_WINDOW).
                            getSource(),
                    ProvisioningActivator.getResourceService().getI18NString(
                            "plugin.provisioning.REMOVE_CREDENTIALS_MESSAGE"),
                    ProvisioningActivator.getResourceService().getI18NString(
                        "service.gui.REMOVE"),
                    JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION)
                {
                    ProvisioningActivator.getCredentialsStorageService()
                        .removePassword(ProvisioningServiceImpl.
                            PROPERTY_PROVISIONING_PASSWORD);
                    ProvisioningActivator.getConfigurationService()
                        .removeProperty(ProvisioningServiceImpl.
                            PROPERTY_PROVISIONING_USERNAME);

                    usernameField.setText("");
                    passwordField.setText("");
                }
            }
        });
    }
}
