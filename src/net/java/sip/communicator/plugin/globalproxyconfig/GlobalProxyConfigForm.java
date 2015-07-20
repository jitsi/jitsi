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
package net.java.sip.communicator.plugin.globalproxyconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.configuration.*;

/**
 * Implementation of the configuration form.
 *
 * @author Damian Minkov
 */
public class GlobalProxyConfigForm
    extends TransparentPanel
    implements ActionListener,
                KeyListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Hold the available proxy types.
     */
    private JComboBox typeCombo;

    /**
     * The proxy server address.
     */
    private JTextField serverAddressField = new JTextField();

    /**
     * The proxy server port.
     */
    private JTextField portField = new JTextField();

    /**
     * The username if any.
     */
    private JTextField usernameField = new JTextField();

    /**
     * The password for accessing proxy, if any.
     */
    private JPasswordField passwordField = new JPasswordField();

    /**
     * The dns forward global checkbox, hides/shows the panel with values.
     */
    private JCheckBox dnsForwardCheck = new SIPCommCheckBox();

    /**
     * Dns server address initially filled with the value of the proxy.
     */
    private JTextField dnsForwardServerAddressField = new JTextField();

    /**
     * Dns server port, initially filled with the value of the proxy.
     */
    private JTextField dnsForwardPortField = new JTextField();

    /**
     * Tha panel containing address and port for dns forwarding.
     */
    private TransparentPanel dnsAddressPane;

    /**
     * Creates the form.
     */
    public GlobalProxyConfigForm()
    {
        super(new BorderLayout());

        init();
    }

    /**
     * Creating the configuration form
     */
    private void init()
    {
        TransparentPanel centerPanel = new TransparentPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5,10,0,0);
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = 0;

        centerPanel.add(new JLabel(
            Resources.getString("plugin.globalproxy.PROXY_TYPE")),
            constraints);
        constraints.gridy = 1;
        centerPanel.add(new JLabel(
            Resources.getString("plugin.globalproxy.PROXY_ADDRESS")),
            constraints);
        constraints.gridy = 2;
        centerPanel.add(new JLabel(
            Resources.getString("plugin.globalproxy.PROXY_USERNAME")),
            constraints);
        constraints.gridy = 3;
        centerPanel.add(new JLabel(
            Resources.getString("plugin.globalproxy.PROXY_PASSWORD")),
            constraints);

        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        typeCombo = new JComboBox(ProxyInfo.ProxyType.values());
        typeCombo.setEditable(false);
        centerPanel.add(typeCombo, constraints);

        constraints.gridy = 1;
        constraints.gridwidth = 1;
        centerPanel.add(serverAddressField, constraints);
        constraints.gridx = 2;
        constraints.weightx = 0;
        centerPanel.add(
            new JLabel(
            Resources.getString("plugin.globalproxy.PROXY_PORT")),
            constraints);
        constraints.gridx = 3;
        constraints.weightx = 1;
        centerPanel.add(portField, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        constraints.gridy = 2;
        centerPanel.add(usernameField, constraints);
        constraints.gridy = 3;
        centerPanel.add(passwordField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 4;
        constraints.gridheight = 2;
        constraints.insets = new Insets(15,15,0,15);
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(Resources.getResources().getI18NString(
            "plugin.globalproxy.DESCRIPTION",
            new String[]{Resources.getResources().getSettingsString(
                "service.gui.APPLICATION_NAME")}));
        centerPanel.add(
            pane,
            constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 4;
        constraints.gridheight = 2;
        constraints.insets = new Insets(0, 20, 0, 20);
        JEditorPane table = new JEditorPane();
        table.setContentType("text/html");
        table.setEditable(false);
        table.setOpaque(false);

        table.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        table.setText(Resources.getString(
            "plugin.globalproxy.PROTOCOL_SUPPORT"));
        centerPanel.add(
            table,
            constraints);

        constraints.weightx = 0;
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.insets = new Insets(5,10,0,0);
        dnsForwardCheck.setText(Resources.getResources()
            .getI18NString("plugin.globalproxy.FWD_DNS"));
        centerPanel.add(dnsForwardCheck, constraints);

        constraints.gridy = 10;
        constraints.insets = new Insets(0, 38, 0, 0);
        String dnsForwardLabelText = Resources.getResources()
                    .getI18NString("plugin.globalproxy.FWD_DNS_NOTE");
        JTextPane dnspane = new JTextPane();
        dnspane.setEditable(false);
        dnspane.setOpaque(false);
        dnspane.setText(dnsForwardLabelText);
        dnspane.setForeground(Color.GRAY);
        dnspane.setFont(dnspane.getFont().deriveFont(8));
        centerPanel.add(dnspane, constraints);

        constraints.gridy = 11;
        constraints.gridwidth = 4;
        constraints.gridheight = 2;
        dnsAddressPane =
            new TransparentPanel(new GridLayout(2, 2));
        dnsAddressPane.add(new JLabel(Resources.getResources()
            .getI18NString("plugin.globalproxy.FWD_DNS_ADDR")));
        dnsAddressPane.add(dnsForwardServerAddressField);
        dnsAddressPane.add(new JLabel(Resources.getResources()
            .getI18NString("plugin.globalproxy.FWD_DNS_PORT")));
        dnsAddressPane.add(dnsForwardPortField);

        dnsAddressPane.setVisible(false);
        centerPanel.add(dnsAddressPane, constraints);

        add(centerPanel, BorderLayout.NORTH);

        loadValues();

        // now after loading has finished we can add all the listeners
        // so we can get further changes
        serverAddressField.addKeyListener(this);
        portField.addKeyListener(this);
        usernameField.addKeyListener(this);
        passwordField.addKeyListener(this);

        typeCombo.addActionListener(this);

        dnsForwardServerAddressField.addKeyListener(this);
        dnsForwardPortField.addKeyListener(this);

        dnsForwardCheck.addActionListener(this);
    }

    /**
     * Loading the values stored onto configuration form
     */
    private void loadValues()
    {
        ConfigurationService configService =
            GlobalProxyPluginActivator.getConfigurationService();

        String serverAddress = configService.getString(
            ProxyInfo.CONNECTION_PROXY_ADDRESS_PROPERTY_NAME);
        if(serverAddress != null)
            serverAddressField.setText(serverAddress);

        String port = configService.getString(
            ProxyInfo.CONNECTION_PROXY_PORT_PROPERTY_NAME);
        if(port != null)
            portField.setText(port);

        String username = configService.getString(
            ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME);
        if(username != null)
            usernameField.setText(username);

        String password = configService.getString(
            ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME);
        if(password != null)
            passwordField.setText(password);

        // we load the types at the end cause a event will ne trigered
        // when selecting the configured value, which will eventually
        // trigger a save operation
        try
        {
            String type = configService.getString(
                ProxyInfo.CONNECTION_PROXY_TYPE_PROPERTY_NAME);
            if(type != null)
                typeCombo.setSelectedItem(ProxyInfo.ProxyType.valueOf(type));
        } catch (IllegalArgumentException e)
        {
            // wrong proxy type stored in configuration
        }

        if(typeCombo.getSelectedItem().equals(ProxyInfo.ProxyType.NONE))
        {
            serverAddressField.setEnabled(false);
            portField.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
        }

        // load dns forward values
        if(configService.getBoolean(
            ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PROPERTY_NAME,
            false))
        {
            dnsForwardCheck.setSelected(true);

            dnsForwardServerAddressField.setText(
                (String)configService.getProperty(
                  ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_ADDRESS_PROPERTY_NAME));
            dnsForwardPortField.setText(
                (String)configService.getProperty(
                     ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PORT_PROPERTY_NAME));

            dnsAddressPane.setVisible(true);
        }
    }

    /**
     * Function which save values onto configuration file after save button is
     * clicked
     */
    private void saveValues()
    {
        ConfigurationService configService =
            GlobalProxyPluginActivator.getConfigurationService();

        if(typeCombo.getSelectedItem().equals(ProxyInfo.ProxyType.NONE))
        {
            configService.setProperty(
                ProxyInfo.CONNECTION_PROXY_TYPE_PROPERTY_NAME,
                ProxyInfo.ProxyType.NONE.name());

            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_ADDRESS_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_PORT_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME);
        }
        else
        {
            configService.setProperty(
                    ProxyInfo.CONNECTION_PROXY_TYPE_PROPERTY_NAME,
                    ((ProxyInfo.ProxyType)typeCombo.getSelectedItem()).name());

            String serverAddress = serverAddressField.getText();
            if(serverAddress != null && serverAddress.length() > 0)
                configService.setProperty(
                    ProxyInfo.CONNECTION_PROXY_ADDRESS_PROPERTY_NAME,
                    serverAddress);

            String port = portField.getText();
            if(port != null && port.length() > 0)
                configService.setProperty(
                    ProxyInfo.CONNECTION_PROXY_PORT_PROPERTY_NAME, port);

            String username = usernameField.getText();
            if(username != null && username.length() > 0)
            {
                configService.setProperty(
                    ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME,
                    username);
            }
            else
            {
                configService.removeProperty(
                    ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME);
            }

            char[] password = passwordField.getPassword();
            if(password.length > 0)
            {
                configService.setProperty(
                    ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME,
                    new String(password));
            }
            else
            {
                configService.removeProperty(
                    ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME);
            }
        }

        // save dns forward values
        if(dnsForwardCheck.isSelected())
        {
            configService.setProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PROPERTY_NAME,
                Boolean.TRUE);
            configService.setProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_ADDRESS_PROPERTY_NAME,
                dnsForwardServerAddressField.getText().trim());
            configService.setProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PORT_PROPERTY_NAME,
                dnsForwardPortField.getText().trim());
        }
        else
        {
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_ADDRESS_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTION_PROXY_FORWARD_DNS_PORT_PROPERTY_NAME);
        }

        GlobalProxyPluginActivator.initProperties();
    }

    /**
     * A new type was selected in the type combo box.
     * @param e the event
     */
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource().equals(dnsForwardCheck))
        {
            // lets show or hide the fields
            dnsAddressPane.setVisible(dnsForwardCheck.isSelected());

            if(dnsForwardCheck.isSelected())
            {
                if(dnsForwardServerAddressField.getText().length() == 0)
                    dnsForwardServerAddressField.setText(
                        serverAddressField.getText());

                if(dnsForwardPortField.getText().length() == 0)
                    dnsForwardPortField.setText("53");
            }

            revalidate();
            repaint();

            // and save initial values
            saveValues();

            return;
        }

        // else this is the typeCombo action

        if(typeCombo.getSelectedItem().equals(ProxyInfo.ProxyType.NONE))
        {
            serverAddressField.setEnabled(false);
            portField.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
        }
        else
        {
            serverAddressField.setEnabled(true);
            portField.setEnabled(true);
            usernameField.setEnabled(true);
            passwordField.setEnabled(true);
        }

        saveValues();
    }

    /**
     * Not used.
     * @param e
     */
    public void keyTyped(KeyEvent e)
    {}

    /**
     * Not used.
     * @param e
     */
    public void keyPressed(KeyEvent e)
    {}

    /**
     * Used to listen for changes and saving on every change.
     * @param e
     */
    public void keyReleased(KeyEvent e)
    {
        saveValues();
    }
}
