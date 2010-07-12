/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing information about the connection.
 *
 * @author Yana Stamcheva
 */
public class ConnectionPanel
    extends TransparentPanel
    implements ItemListener
{
    private final JCheckBox enableDefaultEncryption =
        new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"), true);

    private final JCheckBox enableSipZrtpAttribute =
        new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"), true);

    private final JTextField serverField = new JTextField();

    private final JTextField proxyField = new JTextField();

    private final JTextField authNameField = new JTextField();

    private final JTextField serverPortField
        = new JTextField(SIPAccountRegistration.DEFAULT_PORT);

    private final JTextField proxyPortField
        = new JTextField(SIPAccountRegistration.DEFAULT_PORT);

    private JComboBox transportCombo = new JComboBox(new Object[]
    { "UDP", "TCP", "TLS" });

    private JComboBox keepAliveMethodBox
        = new JComboBox(new Object []
                                {
                                    "REGISTER",
                                    "OPTIONS"
                                });

    private JTextField keepAliveIntervalValue = new JTextField();

    private boolean isServerOverridden = false;

    private SIPAccountRegistrationForm regform;

    /**
     * Creates an instance of the <tt>ConnectionPanel</tt>.
     * @param regform the parent registration form
     */
    public ConnectionPanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout());

        this.regform = regform;

        this.transportCombo.addItemListener(this);

        transportCombo.setSelectedItem(
            SIPAccountRegistration.DEFAULT_TRANSPORT);

        JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

        JPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

        JPanel valuesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

        JLabel serverLabel
            = new JLabel(Resources.getString("plugin.sipaccregwizz.REGISTRAR"));

        JLabel proxyLabel
            = new JLabel(Resources.getString("plugin.sipaccregwizz.PROXY"));

        JLabel authNameLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.AUTH_NAME"));

        JLabel serverPortLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.SERVER_PORT"));

        JLabel proxyPortLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.PROXY_PORT"));

        JLabel transportLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.PREFERRED_TRANSPORT"));

        labelsPanel.add(serverLabel);
        labelsPanel.add(authNameLabel);
        labelsPanel.add(serverPortLabel);
        labelsPanel.add(proxyLabel);
        labelsPanel.add(proxyPortLabel);
        labelsPanel.add(transportLabel);

        valuesPanel.add(serverField);
        valuesPanel.add(authNameField);
        valuesPanel.add(serverPortField);
        valuesPanel.add(proxyField);
        valuesPanel.add(proxyPortField);
        valuesPanel.add(transportCombo);

        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(valuesPanel, BorderLayout.CENTER);

        JPanel encryptionPanel
            = new TransparentPanel(new GridLayout(1, 2, 2, 2));

        encryptionPanel.add(enableDefaultEncryption, BorderLayout.WEST);
        encryptionPanel.add(enableSipZrtpAttribute, BorderLayout.EAST);

        enableDefaultEncryption.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                // Perform action
                JCheckBox cb = (JCheckBox) evt.getSource();

                enableSipZrtpAttribute.setEnabled(cb.isSelected());
            }
        });

        mainPanel.add(encryptionPanel, BorderLayout.SOUTH);

        mainPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.sipaccregwizz.ADVANCED_OPTIONS")));

        this.add(mainPanel, BorderLayout.NORTH);
        this.add(createKeepAlivePanel(), BorderLayout.SOUTH);
    }

    /**
     * Parse the server part from the sip id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param serverAddress the address of the server
     */
    public void setServerFieldAccordingToUIN(String serverAddress)
    {
        if (!regform.isModification() || !isServerOverridden)
        {
            serverField.setText(serverAddress);
            proxyField.setText(serverAddress);
        }
    }

    /**
     * Indicates that the state of the item has changed.
     * @param e the <tt>ItemEvent</tt> that notified us
     */
    public void itemStateChanged(ItemEvent e)
    {
        if (e.getStateChange() == ItemEvent.SELECTED
            && e.getItem().equals("TLS"))
        {
            serverPortField.setText(SIPAccountRegistration.DEFAULT_TLS_PORT);
            proxyPortField.setText(SIPAccountRegistration.DEFAULT_TLS_PORT);
        }
        else
        {
            serverPortField.setText(SIPAccountRegistration.DEFAULT_PORT);
            proxyPortField.setText(SIPAccountRegistration.DEFAULT_PORT);
        }
    }

    /**
     * Creates the keep alive panel.
     * @return the created keep alive panel
     */
    private Component createKeepAlivePanel()
    {
        JPanel emptyLabelPanel = new TransparentPanel();
        emptyLabelPanel.setMaximumSize(new Dimension(40, 35));

        JPanel keepAlivePanel
            = new TransparentPanel(new BorderLayout(10, 10));

        JPanel keepAliveLabels
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        JPanel keepAliveValues
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        JLabel keepAliveMethodLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_METHOD"));

        JLabel keepAliveIntervalLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_INTERVAL"));

        JLabel keepAliveIntervalExampleLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_INTERVAL_INFO"));

        keepAliveLabels.add(keepAliveMethodLabel);
        keepAliveLabels.add(keepAliveIntervalLabel);
        keepAliveLabels.add(emptyLabelPanel);

        keepAliveIntervalExampleLabel.setForeground(Color.GRAY);
        keepAliveIntervalExampleLabel
            .setFont(keepAliveIntervalExampleLabel.getFont().deriveFont(8));
        keepAliveIntervalExampleLabel
            .setMaximumSize(new Dimension(40, 35));
        keepAliveIntervalExampleLabel
            .setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        keepAliveIntervalValue
            .setText(SIPAccountRegistration.DEFAULT_KEEP_ALIVE_INTERVAL);

        keepAliveMethodBox.setSelectedItem(
            SIPAccountRegistration.DEFAULT_KEEP_ALIVE_METHOD);

        keepAliveValues.add(keepAliveMethodBox);
        keepAliveValues.add(keepAliveIntervalValue);
        keepAliveValues.add(keepAliveIntervalExampleLabel);

        keepAlivePanel.add(keepAliveLabels, BorderLayout.WEST);
        keepAlivePanel.add(keepAliveValues, BorderLayout.CENTER);

        keepAlivePanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE")));

        return keepAlivePanel;
    }

    /**
     * Returns the server address.
     * @return the server address
     */
    String getServerAddress()
    {
        return serverField.getText();
    }

    /**
     * Sets the server address.
     * @param serverAddress the server address
     */
    void setServerAddress(String serverAddress)
    {
        serverField.setText(serverAddress);
    }

    /**
     * Enables/disables the server text field.
     * @param isEnabled <tt>true</tt> to enable the server text field,
     * <tt>false</tt> - otherwise
     */
    void setServerEnabled(boolean isEnabled)
    {
        serverField.setEnabled(isEnabled);
    }

    /**
     * Returns the authentication name.
     * @return the authentication name
     */
    String getAuthenticationName()
    {
        return authNameField.getText();
    }

    /**
     * Sets the authentication name.
     * @param authName the authentication name
     */
    void setAuthenticationName(String authName)
    {
        authNameField.setText(authName);
    }

    /**
     * Returns the server port.
     * @return the server port
     */
    String getServerPort()
    {
        return serverPortField.getText();
    }

    /**
     * Sets the server port.
     * @param serverPort the server port
     */
    void setServerPort(String serverPort)
    {
        serverPortField.setText(serverPort);
    }

    /**
     * Returns the proxy.
     * @return the proxy
     */
    String getProxy()
    {
        return proxyField.getText();
    }

    /**
     * Sets the proxy address.
     * @param proxyAddress the proxy address
     */
    void setProxy(String proxyAddress)
    {
        proxyField.setText(proxyAddress);
    }

    /**
     * Return the proxy port.
     * @return the proxy port
     */
    String getProxyPort()
    {
        return proxyPortField.getText();
    }

    /**
     * Sets the proxy port.
     * @param proxyPort the proxy port
     */
    void setProxyPort(String proxyPort)
    {
        proxyPortField.setText(proxyPort);
    }

    /**
     * Returns the selected transport.
     * @return the selected transport
     */
    String getSelectedTransport()
    {
        //Emil: it appears that sometimes the selected item may be null even
        //though the combo box does not allow a null selection.
        Object selectedItem = transportCombo.getSelectedItem();

        if(selectedItem == null)
            selectedItem = transportCombo.getItemAt(0);

        return selectedItem.toString();
    }

    /**
     * Sets the selected transport.
     * @param preferredTransport the transport to select
     */
    void setSelectedTransport(String preferredTransport)
    {
        transportCombo.setSelectedItem(preferredTransport);
    }

    /**
     * Indicates if the default encryption is enabled.
     * @return <tt>true</tt> if the default encryption is enabled,
     * <tt>false</tt> - otherwise
     */
    boolean isDefaultEncryptionEnabled()
    {
        return enableDefaultEncryption.isSelected();
    }

    /**
     * Enables/disables the default encryption.
     * @param isEnable <tt>true</tt> to enable the default encryption,
     * <tt>false</tt> - otherwise
     */
    void enablesDefaultEncryption(boolean isEnable)
    {
        enableDefaultEncryption.setSelected(isEnable);
    }

    /**
     * Indicates if the ZRTP encryption is enabled.
     * @return <tt>true</tt> if <tt>ZRTP</tt> is enabled, <tt>false</tt> -
     * otherwise
     */
    boolean isSipZrtpEnabled()
    {
        return enableSipZrtpAttribute.isSelected();
    }

    /**
     * Enables/disables and selects/deselects the sip zrtp checkbox.
     * @param isSipZrtpEnabled indicates if the sip zrtp is enabled
     * @param isDefaultEncryptionEnabled indicates if the default encryption is
     * enabled
     */
    void setSipZrtpEnabled( boolean isSipZrtpEnabled,
                            boolean isDefaultEncryptionEnabled)
    {
        enableSipZrtpAttribute.setSelected(isSipZrtpEnabled);
        enableSipZrtpAttribute.setEnabled(isDefaultEncryptionEnabled);
    }

    /**
     * Returns the keep alive method.
     * @return the keep alive method
     */
    String getKeepAliveMethod()
    {
        return keepAliveMethodBox.getSelectedItem().toString();
    }

    /**
     * Sets the keep alive method.
     * @param keepAliveMethod the keep alive method
     */
    void setKeepAliveMethod(String keepAliveMethod)
    {
        keepAliveMethodBox.setSelectedItem(keepAliveMethod);
    }

    /**
     * Returns the keep alive interval
     * @return the keep alive interval
     */
    String getKeepAliveInterval()
    {
        return keepAliveIntervalValue.getText();
    }

    /**
     * Sets the keep alive interval
     * @param keepAliveInterval the keep alive interval
     */
    void setKeepAliveInterval(String keepAliveInterval)
    {
        keepAliveIntervalValue.setText(keepAliveInterval);
    }

    /**
     * Sets the <tt>serverOverridden</tt> property.
     * @param isServerOverridden <tt>true</tt> to indicate that the server is
     * overridden, <tt>false</tt> - otherwise
     */
    void setServerOverridden(boolean isServerOverridden)
    {
        this.isServerOverridden = isServerOverridden;
    }
}
