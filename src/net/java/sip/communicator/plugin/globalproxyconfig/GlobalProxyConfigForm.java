/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.globalproxyconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import net.java.sip.communicator.service.configuration.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

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
     * Creates the form.
     */
    public GlobalProxyConfigForm()
    {
        super(new BorderLayout());

        init();
        loadValues();
    }

    /**
     * Creating the configuration form
     */
    private void init()
    {
        serverAddressField.addKeyListener(this);
        portField.addKeyListener(this);
        usernameField.addKeyListener(this);
        passwordField.addKeyListener(this);

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
        typeCombo.addActionListener(this);
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
        constraints.insets = new Insets(20,15,20,15);
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
        constraints.insets = new Insets(20,20,20,20);
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

        add(centerPanel, BorderLayout.NORTH);
    }

    /**
     * Loading the values stored onto configuration form
     */
    private void loadValues()
    {
        ConfigurationService configService =
            GlobalProxyPluginActivator.getConfigurationService();

        String serverAddress = configService.getString(
            ProxyInfo.CONNECTON_PROXY_ADDRESS_PROPERTY_NAME);
        if(serverAddress != null)
            serverAddressField.setText(serverAddress);

        String port = configService.getString(
            ProxyInfo.CONNECTON_PROXY_PORT_PROPERTY_NAME);
        if(port != null)
            portField.setText(port);

        String username = configService.getString(
            ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME);
        if(username != null)
            usernameField.setText(username);

        String password = configService.getString(
            ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME);
        if(password != null)
            passwordField.setText(password);

        // we load the types at the end cause a event will ne trigered
        // when selecting the configured value, which will eventually
        // trigger a save operation
        try
        {
            String type = configService.getString(
                ProxyInfo.CONNECTON_PROXY_TYPE_PROPERTY_NAME);
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
                ProxyInfo.CONNECTON_PROXY_TYPE_PROPERTY_NAME,
                ProxyInfo.ProxyType.NONE.name());

            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_ADDRESS_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_PORT_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME);
            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME);

            return;
        }

        configService.setProperty(
                ProxyInfo.CONNECTON_PROXY_TYPE_PROPERTY_NAME,
                ((ProxyInfo.ProxyType)typeCombo.getSelectedItem()).name());

        String serverAddress = serverAddressField.getText();
        if(serverAddress != null && serverAddress.length() > 0)
            configService.setProperty(
                ProxyInfo.CONNECTON_PROXY_ADDRESS_PROPERTY_NAME, serverAddress);

        String port = portField.getText();
        if(port != null && port.length() > 0)
            configService.setProperty(
                ProxyInfo.CONNECTON_PROXY_PORT_PROPERTY_NAME, port);

        String username = usernameField.getText();
        if(username != null && username.length() > 0)
        {
            configService.setProperty(
                ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME, username);
        }
        else
        {
            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME);
        }

        char[] password = passwordField.getPassword();
        if(password.length > 0)
        {
            configService.setProperty(
                ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME,
                new String(password));
        }
        else
        {
            configService.removeProperty(
                ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME);
        }

        GlobalProxyPluginActivator.initProperties();
    }

    /**
     * A new type was selected in the type combo box.
     * @param e the event
     */
    public void actionPerformed(ActionEvent e)
    {
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
