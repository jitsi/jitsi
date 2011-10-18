/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Implementation of the configuration form.
 *
 * @author Damian Minkov
 */
public class SIPConfigForm
    extends TransparentPanel
{
    /**
     * Creates the form.
     */
    public SIPConfigForm()
    {
        super(new BorderLayout());

        TransparentPanel sipClientPortConfigPanel = new TransparentPanel();
        sipClientPortConfigPanel.setLayout(new BorderLayout(10, 10));
        sipClientPortConfigPanel.setPreferredSize(new Dimension(250, 72));

        add(sipClientPortConfigPanel, BorderLayout.NORTH);

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
            String.valueOf(ConfigurationManager.getClientPort()));
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

                    ConfigurationManager.setClientPort(port);
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
            String.valueOf(ConfigurationManager.getClientSecurePort()));
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

                    ConfigurationManager.setClientSecurePort(port);
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
    }
}