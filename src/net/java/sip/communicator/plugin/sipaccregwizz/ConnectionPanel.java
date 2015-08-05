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
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.sip.*;
import org.jitsi.util.*;

/**
 * The panel containing information about the connection.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ConnectionPanel
    extends TransparentPanel
    implements ItemListener,
               DocumentListener,
               ValidatingPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final JTextField serverField = new JTextField();

    private final JTextField proxyField = new JTextField();

    private final JTextField authNameField = new JTextField();

    private final JTextField serverPortField = new JTextField(4);

    private final JTextField proxyPortField = new JTextField(4);

    private final JTextField voicemailAliasField = new JTextField(4);

    private final JTextField voicemailCheckField = new JTextField(4);

    private final JCheckBox proxyAutoCheckBox;

    private final JComboBox certificate = new JComboBox();

    private JComboBox transportCombo
        = new JComboBox(new String[] { "UDP", "TCP", "TLS" });

    private JComboBox keepAliveMethodBox
        = new JComboBox(new String[] { "NONE", "REGISTER", "OPTIONS", "CRLF" });

    private JTextField keepAliveIntervalValue
            = new JTextField(SipAccountID.getDefaultStr(
                    ProtocolProviderFactory.KEEP_ALIVE_INTERVAL));

    private JComboBox dtmfMethodBox
        = new JComboBox(
                new String[]
                        {
                            Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_AUTO"),
                            Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_RTP"),
                            Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_SIP_INFO"),
                            Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_INBAND")
                        });

    /**
     * The text field used to change the DTMF minimal tone duration.
     */
    private JTextField dtmfMinimalToneDurationValue = new JTextField();

    private final JCheckBox mwiCheckBox;

    private boolean isServerOverridden = false;

    private SIPAccountRegistrationForm regform;

    /**
     * Creates an instance of the <tt>ConnectionPanel</tt>.
     * @param regform the parent registration form
     */
    public ConnectionPanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout(10, 10));

        this.regform = regform;
        this.regform.addValidatingPanel(this);

        proxyAutoCheckBox = new SIPCommCheckBox(
                Resources.getString("plugin.sipaccregwizz.PROXY_AUTO"),
                regform.getRegistration().isProxyAutoConfigure());
        enablesProxyAutoConfigure(proxyAutoCheckBox.isSelected());
        proxyAutoCheckBox.addActionListener(new ActionListener()
        {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e)
            {
                enablesProxyAutoConfigure(proxyAutoCheckBox.isSelected());
                ConnectionPanel.this.regform.reValidateInput();
            }
        });

        this.transportCombo.addItemListener(this);

        transportCombo.setSelectedItem(
            regform.getRegistration().getPreferredTransport());

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel registrarMainPanel =
            new TransparentPanel(new BorderLayout(10, 10));

        JPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        JPanel valuesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

        JLabel serverLabel
            = new JLabel(Resources.getString("plugin.sipaccregwizz.REGISTRAR"));

        JLabel authNameLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.AUTH_NAME"));

        JLabel serverPortLabel
            = new JLabel(Resources.getString("service.gui.PORT"));

        JLabel certLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.CLIENT_CERTIFICATE"));

        labelsPanel.add(serverLabel);
        labelsPanel.add(authNameLabel);
        labelsPanel.add(certLabel);

        serverField.setText(regform.getRegistration().getServerAddress());
        serverPortField.setText(regform.getRegistration().getServerPort());

        JPanel serverPanel = new TransparentPanel(new BorderLayout(5, 5));
        serverPanel.add(serverField, BorderLayout.CENTER);
        JPanel serverPortPanel = new TransparentPanel(
                new BorderLayout(5, 5));
        serverPortPanel.add(serverPortLabel, BorderLayout.WEST);
        serverPortPanel.add(serverPortField, BorderLayout.EAST);
        serverPanel.add(serverPortPanel, BorderLayout.EAST);

        valuesPanel.add(serverPanel);
        valuesPanel.add(authNameField);
        valuesPanel.add(certificate);
        initCertificateAliases(null);

        registrarMainPanel.add(labelsPanel, BorderLayout.WEST);
        registrarMainPanel.add(valuesPanel, BorderLayout.CENTER);
        registrarMainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 5, 10, 5));

        mainPanel.add(registrarMainPanel);

        proxyAutoCheckBox.setSelected(
            regform.getRegistration().isProxyAutoConfigure());
        if(!StringUtils.isNullOrEmpty(
                regform.getRegistration().getProxy()))
            proxyField.setText(regform.getRegistration().getProxy());
        if(!StringUtils.isNullOrEmpty(
                regform.getRegistration().getProxyPort()))
            proxyPortField.setText(regform.getRegistration().getProxyPort());

        JLabel proxyLabel
            = new JLabel(Resources.getString("plugin.sipaccregwizz.PROXY"));

        JLabel proxyPortLabel
            = new JLabel(Resources.getString("service.gui.PORT"));

        JLabel transportLabel
            = new JLabel(Resources.getString(
                "plugin.sipaccregwizz.PREFERRED_TRANSPORT"));

        JPanel proxyMainPanel
            = new TransparentPanel(new BorderLayout(10, 10));

        proxyField.getDocument().addDocumentListener(this);
        proxyPortField.getDocument().addDocumentListener(this);
        JPanel proxyPanel = new TransparentPanel(new BorderLayout(5, 5));
        proxyPanel.add(proxyField, BorderLayout.CENTER);
        JPanel proxyPortPanel = new TransparentPanel(
                new BorderLayout(5, 5));
        proxyPortPanel.add(proxyPortLabel, BorderLayout.WEST);
        proxyPortPanel.add(proxyPortField, BorderLayout.EAST);
        proxyPanel.add(proxyPortPanel, BorderLayout.EAST);

        labelsPanel = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        valuesPanel = new TransparentPanel(new GridLayout(0, 1, 10, 10));

        labelsPanel.add(proxyLabel);
        labelsPanel.add(transportLabel);
        valuesPanel.add(proxyPanel);
        valuesPanel.add(transportCombo);

        proxyMainPanel.add(proxyAutoCheckBox, BorderLayout.NORTH);
        proxyMainPanel.add(labelsPanel, BorderLayout.WEST);
        proxyMainPanel.add(valuesPanel, BorderLayout.CENTER);
        proxyMainPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.sipaccregwizz.PROXY_OPTIONS")));

        mainPanel.add(proxyMainPanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(createKeepAlivePanel());

        JPanel encryptionPanel
            = new TransparentPanel(new GridLayout(1, 2, 2, 2));

        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(encryptionPanel);

        JPanel voicemailPanel = new TransparentPanel(new BorderLayout(10, 10));
        JPanel voicemailUriPanelLabels
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        JPanel voicemailUriPanelValues
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

        mwiCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.MWI"),
            regform.getRegistration().isMessageWaitingIndicationsEnabled());

        voicemailUriPanelLabels.add(new JLabel(
                    Resources.getString("plugin.sipaccregwizz.VOICEMAIL_URI")));
        voicemailUriPanelLabels.add(new JLabel(
            Resources.getString("plugin.sipaccregwizz.VOICEMAIL_CHECK_URI")));
        voicemailUriPanelValues.add(voicemailAliasField);
        voicemailUriPanelValues.add(voicemailCheckField);
        voicemailPanel.setBorder(BorderFactory.createTitledBorder(
                    Resources.getString("plugin.sipaccregwizz.VOICEMAIL")));

        voicemailAliasField.setText(regform.getRegistration().getVoicemailURI());
        voicemailCheckField.setText(regform.getRegistration().getVoicemailCheckURI());

        voicemailPanel.add(mwiCheckBox, BorderLayout.NORTH);
        voicemailPanel.add(voicemailUriPanelLabels, BorderLayout.WEST);
        voicemailPanel.add(voicemailUriPanelValues, BorderLayout.CENTER);

        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(voicemailPanel);

        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(createDTMFPanel());

        this.add(mainPanel, BorderLayout.NORTH);
    }

    private void initCertificateAliases(String id)
    {
        certificate.removeAllItems();
        certificate.insertItemAt(
                Resources.getString("plugin.sipaccregwizz.NO_CERTIFICATE"),
                0);
        certificate.setSelectedIndex(0);
        for(CertificateConfigEntry e
                : SIPAccRegWizzActivator.getCertificateService()
                        .getClientAuthCertificateConfigs())
        {
            certificate.addItem(e);
            if(e.getId().equals(id))
                certificate.setSelectedItem(e);
        }
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
            //proxyField.setText(serverAddress);
        }
    }

    /**
     * Indicates that the state of the item has changed.
     * @param e the <tt>ItemEvent</tt> that notified us
     */
    public void itemStateChanged(ItemEvent e)
    {
        // do not set default values cause they are counted
        // as overrrided ones
//        if (e.getStateChange() == ItemEvent.SELECTED
//            && e.getItem().equals("TLS"))
//        {
//            serverPortField.setText(SIPAccountRegistration.DEFAULT_TLS_PORT);
//            proxyPortField.setText(SIPAccountRegistration.DEFAULT_TLS_PORT);
//        }
//        else
//        {
//            serverPortField.setText(SIPAccountRegistration.DEFAULT_PORT);
//            proxyPortField.setText(SIPAccountRegistration.DEFAULT_PORT);
//        }
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

        keepAliveMethodBox.setSelectedItem(
                SipAccountID.getDefaultStr(
                        ProtocolProviderFactory.KEEP_ALIVE_METHOD));
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
     * Creates the DTMF panel.
     * @return the created DTMF panel
     */
    private Component createDTMFPanel()
    {
        JPanel emptyLabelPanel = new TransparentPanel();

        // Labels.
        JPanel dtmfLabels = new TransparentPanel(new GridLayout(0, 1, 5, 5));
        JLabel dtmfMethodLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.DTMF_METHOD"));
        JLabel minimalDTMFToneDurationLabel = new JLabel(
            Resources.getString(
                "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION"));
        dtmfLabels.add(dtmfMethodLabel);
        dtmfLabels.add(minimalDTMFToneDurationLabel);
        dtmfLabels.add(emptyLabelPanel);

        // Values
        JPanel dtmfValues = new TransparentPanel(new GridLayout(0, 1, 5, 5));
        dtmfMethodBox.addItemListener(new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        boolean isEnabled = false;
                        String selectedItem
                            = (String) dtmfMethodBox.getSelectedItem();
                        if(selectedItem != null
                            && (selectedItem.equals(Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_AUTO"))
                                || selectedItem.equals(Resources.getString(
                                        "plugin.sipaccregwizz.DTMF_RTP")))
                          )
                        {
                            isEnabled = true;
                        }
                        dtmfMinimalToneDurationValue.setEnabled(isEnabled);
                    }
                });
        dtmfMethodBox.setSelectedItem(
                regform.getRegistration().getDTMFMethod());
        dtmfMinimalToneDurationValue
            .setText(regform.getRegistration().getDtmfMinimalToneDuration());
        JLabel dtmfMinimalToneDurationExampleLabel = new JLabel(
                Resources.getString(
                    "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION_INFO"));
        dtmfMinimalToneDurationExampleLabel.setForeground(Color.GRAY);
        dtmfMinimalToneDurationExampleLabel.setFont(
                dtmfMinimalToneDurationExampleLabel.getFont().deriveFont(8));
        dtmfMinimalToneDurationExampleLabel.setMaximumSize(
                new Dimension(40, 35));
        dtmfMinimalToneDurationExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0));
        dtmfValues.add(dtmfMethodBox);
        dtmfValues.add(dtmfMinimalToneDurationValue);
        dtmfValues.add(dtmfMinimalToneDurationExampleLabel);

        // DTMF panel
        JPanel dtmfPanel = new TransparentPanel(new BorderLayout(10, 10));
        dtmfPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.sipaccregwizz.DTMF")));
        dtmfPanel.add(dtmfLabels, BorderLayout.WEST);
        dtmfPanel.add(dtmfValues, BorderLayout.CENTER);

        return dtmfPanel;
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
     * Gets the ID of the selected client TLS certificate or <tt>null</tt> if no
     * certificate is selected.
     *
     * @return the ID of the selected client TLS certificate or <tt>null</tt> if
     *         no certificate is selected.
     */
    String getCertificateId()
    {
        Object selectedItem = certificate.getSelectedItem();

        if((selectedItem != null)
                && (selectedItem instanceof CertificateConfigEntry))
        {
            return ((CertificateConfigEntry) selectedItem).getId();
        }
        return null;
    }

    /**
     * Sets the selected client TLS certificate entry.
     * @param id The ID of the entry to select.
     */
    void setCertificateId(String id)
    {
        initCertificateAliases(id);
    }

    /**
     * Returns the keep alive method.
     * @return the keep alive method
     */
    String getKeepAliveMethod()
    {
        Object selItem = keepAliveMethodBox.getSelectedItem();

        if(selItem != null)
            return selItem.toString();
        else
            return null;
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
     * Returns the DTMF method.
     * @return the DTMF method
     */
    String getDTMFMethod()
    {
        Object selItem = dtmfMethodBox.getSelectedItem();

        // By default sets DTMF mezthod to auto.
        if(selItem == null)
        {
            return null;
        }

        String selString = selItem.toString();
        if(selString.equals(
                    Resources.getString("plugin.sipaccregwizz.DTMF_RTP")))
        {
            return "RTP_DTMF";
        }
        else if(selString.equals(
                    Resources.getString(
                        "plugin.sipaccregwizz.DTMF_SIP_INFO")))
        {
            return "SIP_INFO_DTMF";
        }
        else if(selString.equals(
                    Resources.getString("plugin.sipaccregwizz.DTMF_INBAND")))
        {
            return "INBAND_DTMF";
        }
        else
        {
            return "AUTO_DTMF";
        }
    }

    /**
     * Sets the DTMF method.
     * @param dtmfMethod the DTMF method
     */
    void setDTMFMethod(String dtmfMethod)
    {
        if(dtmfMethod == null)
        {
            dtmfMethodBox.setSelectedItem(0);
        }
        else
        {
            String selString;
            if(dtmfMethod.equals("RTP_DTMF"))
            {
                selString =
                    Resources.getString("plugin.sipaccregwizz.DTMF_RTP");
            }
            else if(dtmfMethod.equals("SIP_INFO_DTMF"))
            {
                selString =
                    Resources.getString("plugin.sipaccregwizz.DTMF_SIP_INFO");
            }
            else if(dtmfMethod.equals("INBAND_DTMF"))
            {
                selString =
                    Resources.getString("plugin.sipaccregwizz.DTMF_INBAND");
            }
            else
            {
                selString =
                    Resources.getString("plugin.sipaccregwizz.DTMF_AUTO");
            }
            dtmfMethodBox.setSelectedItem(selString);
        }
    }

    /**
     * Returns the minimal DTMF tone duration.
     *
     * @return The minimal DTMF tone duration.
     */
    String getDtmfMinimalToneDuration()
    {
        return dtmfMinimalToneDurationValue.getText();
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
     * Returns the voicemail URI.
     * @return the voicemail URI.
     */
    String getVoicemailURI()
    {
        return voicemailAliasField.getText();
    }

    /**
     * Sets the voicemail URI.
     * @param voicemailURI the voicemail URI.
     */
    void setVoicemailURI(String voicemailURI)
    {
        voicemailAliasField.setText(voicemailURI);
    }

    /**
     * Returns the voicemail check URI.
     * @return the voicemail URI.
     */
    String getVoicemailCheckURI()
    {
        return voicemailCheckField.getText();
    }

    /**
     * Sets the voicemail check URI.
     * @param voicemailCheckURI the voicemail URI.
     */
    void setVoicemailCheckURI(String voicemailCheckURI)
    {
        voicemailCheckField.setText(voicemailCheckURI);
    }

    /**
     * Returns is message waiting indications is enabled.
     * @return is message waiting indications is enabled.
     */
    boolean isMessageWaitingEnabled()
    {
        return mwiCheckBox.isSelected();
    }

    /**
     * Sets is message waiting indications is enabled.
     * @param enabled is message waiting indications is enabled.
     */
    void setMessageWaitingIndications(boolean enabled)
    {
        mwiCheckBox.setSelected(enabled);
    }

    /**
     * Sets the minimal DTMF tone duration
     *
     * @param dtmfMinimalToneDuration
     */
    void setDtmfMinimalToneDuration(String dtmfMinimalToneDuration)
    {
        dtmfMinimalToneDurationValue.setText(dtmfMinimalToneDuration);
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

    /**
     * Indicates if the proxy auto-configure is enabled.
     * @return <tt>true</tt> if the proxy auto-configuration is enabled,
     * <tt>false</tt> - otherwise
     */
    boolean isProxyAutoConfigureEnabled()
    {
        return proxyAutoCheckBox.isSelected();
    }

    /**
     * Enables/disables the proxy auto-configuration.
     * @param isEnable <tt>true</tt> to enable proxy auto-configuration,
     * <tt>false</tt> - otherwise
     */
    void enablesProxyAutoConfigure(boolean isEnable)
    {
        proxyAutoCheckBox.setSelected(isEnable);
        proxyField.setEnabled(!isEnable);
        proxyPortField.setEnabled(!isEnable);
        transportCombo.setEnabled(!isEnable);
        regform.reValidateInput();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * proxy or port field. Enables or disables the "Next" wizard button
     * according to whether the fields are empty.
     * @param e the <tt>DocumentEvent</tt> that notified us     */
    public void insertUpdate(DocumentEvent e)
    {
        regform.reValidateInput();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the proxy and port fields. Enables or disables the "Next" wizard
     * button according to whether the fields are empty.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        regform.reValidateInput();
    }

    /**
     * Not used.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e){}

    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification.
     * Checks whether proxy field values are ok to continue with
     * account creating.
     *
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated()
    {
        if(!proxyAutoCheckBox.isSelected())
        {
            return
                proxyField.getText() != null
                    && proxyField.getText().length() > 0
                && proxyPortField.getText() != null
                    && proxyPortField.getText().length() > 0;
        }

        return true;
    }
}
