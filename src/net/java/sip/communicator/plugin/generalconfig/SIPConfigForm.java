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
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the configuration form.
 *
 * @author Damian Minkov
 */
public class SIPConfigForm
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private Box pnlSslProtocols;

    /**
     * Creates the form.
     */
    public SIPConfigForm()
    {
        super(new BorderLayout());
        Box box = Box.createVerticalBox();
        add(box, BorderLayout.NORTH);

        TransparentPanel sipClientPortConfigPanel = new TransparentPanel();
        sipClientPortConfigPanel.setLayout(new BorderLayout(10, 10));
        sipClientPortConfigPanel.setPreferredSize(new Dimension(250, 50));

        box.add(sipClientPortConfigPanel);

        TransparentPanel labelPanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        TransparentPanel valuePanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));

        sipClientPortConfigPanel.add(labelPanel,
            BorderLayout.WEST);
        sipClientPortConfigPanel.add(valuePanel,
            BorderLayout.CENTER);

        labelPanel.add(new JLabel(
            Resources.getString(
                "plugin.generalconfig.SIP_CLIENT_PORT")));
        labelPanel.add(new JLabel(
            Resources.getString(
                "plugin.generalconfig.SIP_CLIENT_SECURE_PORT")));

        final JTextField clientPortField = new JTextField(6);
        clientPortField.setText(
            String.valueOf(ConfigurationUtils.getClientPort()));
        valuePanel.add(clientPortField);
        clientPortField.addFocusListener(new FocusListener()
        {
            private String oldValue = null;

            public void focusLost(FocusEvent e)
            {
                try
                {
                    int port =
                        Integer.valueOf(clientPortField.getText());

                    if(port <= 0 || port > 65535)
                        throw new NumberFormatException(
                            "Not a port number");

                    ConfigurationUtils.setClientPort(port);
                }
                catch (NumberFormatException ex)
                {
                    // not a number for port
                    String error =
                        Resources.getString(
                            "plugin.generalconfig.ERROR_PORT_NUMBER");
                    GeneralConfigPluginActivator.getUIService().
                    getPopupDialog().showMessagePopupDialog(
                        error,
                        error,
                        PopupDialog.ERROR_MESSAGE);
                    clientPortField.setText(oldValue);
                }
            }

            public void focusGained(FocusEvent e)
            {
                oldValue = clientPortField.getText();
            }
        });

        final JTextField clientSecurePortField = new JTextField(6);
        clientSecurePortField.setText(
            String.valueOf(ConfigurationUtils.getClientSecurePort()));
        valuePanel.add(clientSecurePortField);
        clientSecurePortField.addFocusListener(new FocusListener()
        {
            private String oldValue = null;

            public void focusLost(FocusEvent e)
            {
                try
                {
                    int port
                        = Integer.valueOf(clientSecurePortField.getText());

                    if(port <= 0 || port > 65535)
                        throw new NumberFormatException(
                            "Not a port number");

                    ConfigurationUtils.setClientSecurePort(port);
                }
                catch (NumberFormatException ex)
                {
                    // not a number for port
                    String error =
                        Resources.getString(
                            "plugin.generalconfig.ERROR_PORT_NUMBER");
                    GeneralConfigPluginActivator.getUIService().
                    getPopupDialog().showMessagePopupDialog(
                        error,
                        error,
                        PopupDialog.ERROR_MESSAGE);
                    clientSecurePortField.setText(oldValue);
                }
            }

            public void focusGained(FocusEvent e)
            {
                oldValue = clientSecurePortField.getText();
            }
        });

        String configuredProtocols = Arrays.toString(
            ConfigurationUtils.getEnabledSslProtocols());

        pnlSslProtocols = Box.createVerticalBox();
        pnlSslProtocols.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.generalconfig.SIP_SSL_PROTOCOLS")));
        pnlSslProtocols.setAlignmentX(Component.LEFT_ALIGNMENT);
        for(String protocol : ConfigurationUtils.getAvailableSslProtocols())
        {
            JCheckBox chkProtocol = new SIPCommCheckBox(protocol);
            chkProtocol.addActionListener(this);
            chkProtocol.setSelected(configuredProtocols.contains(protocol));
            pnlSslProtocols.add(chkProtocol);
        }
        pnlSslProtocols.add(new JLabel(
            Resources.getString(
                "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN")));
        JPanel sslWrapper = new TransparentPanel(new BorderLayout());
        sslWrapper.add(pnlSslProtocols, BorderLayout.CENTER);
        box.add(sslWrapper);
    }

    public void actionPerformed(ActionEvent e)
    {
        List<String> enabledSslProtocols = new ArrayList<String>(
            pnlSslProtocols.getComponentCount());
        for(Component child : pnlSslProtocols.getComponents())
        {
            if(child instanceof JCheckBox)
                if(((JCheckBox) child).isSelected())
                    enabledSslProtocols.add(((JCheckBox) child).getText());
        }
        ConfigurationUtils.setEnabledSslProtocols(
            enabledSslProtocols.toArray(new String[]{}));
    }
}
