/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.swing.*;
import org.jitsi.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ConnectionPanel
    extends TransparentPanel
    implements ValidatingPanel
{
    /**
     * Serial version UID.
     */
    private final static long serialVersionUID = 0L;

    private final TransparentPanel mainPanel = new TransparentPanel();

    private final JPanel advancedOpPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel serverOpPanel
            = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel labelsAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JPanel valuesAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JCheckBox sendKeepAliveBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"));

    private final JCheckBox gmailNotificationsBox = new SIPCommCheckBox(
        Resources.getString(
            "plugin.jabberaccregwizz.ENABLE_GMAIL_NOTIFICATIONS"));

    private final JCheckBox googleContactsBox = new SIPCommCheckBox(
            Resources.getString(
                "plugin.jabberaccregwizz.ENABLE_GOOGLE_CONTACTS_SOURCE"));

    private final JLabel resourceLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.RESOURCE"));

    private final JTextField resourceField
        = new JTextField(JabberAccountRegistration.DEFAULT_RESOURCE);

    private final JLabel priorityLabel = new JLabel(
        Resources.getString("plugin.jabberaccregwizz.PRIORITY"));

    private final JTextField priorityField
        = new JTextField(JabberAccountRegistration.DEFAULT_PRIORITY);

    private final JCheckBox serverAutoCheckBox = new SIPCommCheckBox(
            Resources.getString(
                "plugin.jabberaccregwizz.OVERRIDE_SERVER_DEFAULT_OPTIONS"),
                JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN);

    private final JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.SERVER"));

    private final JTextField serverField = new JTextField();

    private final JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    private final JTextField portField
        = new JTextField(JabberAccountRegistration.DEFAULT_PORT);

    private final JCheckBox autoGenerateResource = new SIPCommCheckBox(
            Resources.getString("plugin.jabberaccregwizz.AUTORESOURCE"),
                JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN);

    JCheckBox allowNonSecureBox = new SIPCommCheckBox(
            Resources.getString("plugin.jabberaccregwizz.ALLOW_NON_SECURE"),
            false);

    private JComboBox dtmfMethodBox = new JComboBox(new Object []
    {
        Resources.getString(
            "plugin.jabberaccregwizz.DTMF_AUTO"),
        Resources.getString(
            "plugin.sipaccregwizz.DTMF_RTP"),
        Resources.getString(
            "plugin.sipaccregwizz.DTMF_INBAND")
    });

    private final JabberAccountRegistrationForm parentForm;

    /**
     * Creates an instance of <tt>ConnectionPanel</tt> by specifying the parent
     * wizard, where it's contained.
     * @param parentForm the parent form
     */
    public ConnectionPanel(JabberAccountRegistrationForm parentForm)
    {
        super(new BorderLayout());

        this.parentForm = parentForm;
        parentForm.addValidatingPanel(this);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        portField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        priorityField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        serverAutoCheckBox.addActionListener(new ActionListener()
        {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e)
            {
                enablesServerAutoConfigure(serverAutoCheckBox.isSelected());
            }
        });
        serverAutoCheckBox.setSelected(
            parentForm.getRegistration().isServerOverridden());
        enablesServerAutoConfigure(serverAutoCheckBox.isSelected());

        labelsAdvOpPanel.add(serverLabel);
        labelsAdvOpPanel.add(portLabel);

        valuesAdvOpPanel.add(serverField);
        valuesAdvOpPanel.add(portField);

        serverOpPanel.add(serverAutoCheckBox, BorderLayout.NORTH);
        serverOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        serverOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);
        serverOpPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.jabberaccregwizz.SERVER_OPTIONS")));

        JPanel checkBoxesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        //checkBoxesPanel.add(sendKeepAliveBox);
        checkBoxesPanel.add(gmailNotificationsBox);
        checkBoxesPanel.add(googleContactsBox);
        checkBoxesPanel.add(allowNonSecureBox);

        final JPanel resourcePanel
                = new TransparentPanel(new BorderLayout(10, 10));
        resourcePanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.jabberaccregwizz.RESOURCE")));
        resourcePanel.add(autoGenerateResource, BorderLayout.NORTH);
        JPanel resSubPanelLabel =
            new TransparentPanel(new GridLayout(0, 1, 10, 10));
        JPanel resSubPanelValue =
            new TransparentPanel(new GridLayout(0, 1, 10, 10));
        resSubPanelLabel.add(resourceLabel);
        resSubPanelLabel.add(priorityLabel);
        resSubPanelValue.add(resourceField);
        resSubPanelValue.add(priorityField);
        resourcePanel.add(resSubPanelLabel, BorderLayout.WEST);
        resourcePanel.add(resSubPanelValue, BorderLayout.CENTER);

        // default for new account
        googleContactsBox.setSelected(true);
        advancedOpPanel.add(checkBoxesPanel, BorderLayout.NORTH);
        advancedOpPanel.add(serverOpPanel, BorderLayout.CENTER);
        advancedOpPanel.add(resourcePanel, BorderLayout.SOUTH);

        if(autoGenerateResource.isSelected())
        {
            resourceField.setEnabled(false);
        }
        autoGenerateResource.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                resourceField.setEnabled(!autoGenerateResource.isSelected());
            }
        });

        mainPanel.add(advancedOpPanel);

        mainPanel.add(createDTMFPanel());

        String serverAddress = parentForm.getServerAddress();
        if (!StringUtils.isNullOrEmpty(serverAddress))
            serverField.setText(serverAddress);

        add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Creates the DTMF panel.
     * @return the created DTMF panel
     */
    private Component createDTMFPanel()
    {
        JPanel dtmfPanel = new TransparentPanel(new BorderLayout(10, 10));
        JLabel dtmfMethodLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.DTMF_METHOD"));
        dtmfPanel.add(dtmfMethodLabel, BorderLayout.WEST);

        dtmfMethodBox.setSelectedItem(
            parentForm.getRegistration().getDefaultDTMFMethod());
        dtmfPanel.add(dtmfMethodBox, BorderLayout.CENTER);

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
     * Returns the server port.
     * @return the server port
     */
    String getServerPort()
    {
        return portField.getText();
    }

    /**
     * Sets the server port.
     * @param serverPort the server port
     */
    void setServerPort(String serverPort)
    {
        portField.setText(serverPort);
    }

    /**
     * Returns the resource.
     * @return the resource
     */
    String getResource()
    {
        return resourceField.getText();
    }

    /**
     * Sets the resource field value.
     * @param resource the resource to set
     */
    void setResource(String resource)
    {
        resourceField.setText(resource);
    }

    /**
     * Returns the priority field value.
     * @return the priority field value
     */
    String getPriority()
    {
        return priorityField.getText();
    }

    /**
     * Sets the priority field value.
     * @param priority the priority field value
     */
    void setPriority(String priority)
    {
        priorityField.setText(priority);
    }

    /**
     * Returns <tt>true</tt> if the "send keep alive" check box is selected,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the "send keep alive" check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isSendKeepAlive()
    {
        return sendKeepAliveBox.isSelected();
    }

    /**
     * Selects/unselects the "send keep alive" check box according to the given
     * <tt>isSendKeepAlive</tt> property.
     * @param isSendKeepAlive indicates if the "send keep alive" check box
     * should be selected or not
     */
    void setSendKeepAlive(boolean isSendKeepAlive)
    {
        sendKeepAliveBox.setSelected(isSendKeepAlive);
    }

    /**
     * Returns <tt>true</tt> if the "gmail notifications" check box is selected,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the "gmail notifications" check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isGmailNotificationsEnabled()
    {
        return gmailNotificationsBox.isSelected();
    }

    /**
     * Selects/unselects the "gmail notifications" check box according to the
     * given <tt>isEnabled</tt> property.
     * @param isEnabled indicates if the "gmail notifications"
     * check box should be selected or not
     */
    void setGmailNotificationsEnabled(boolean isEnabled)
    {
        gmailNotificationsBox.setSelected(isEnabled);
    }

    /**
     * Returns <tt>true</tt> if the "Google contacts" check box is selected,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the "Google contacts" check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isGoogleContactsEnabled()
    {
        return googleContactsBox.isSelected();
    }

    /**
     * Selects/unselects the "Google contacts" check box according to the
     * given <tt>isEnabled</tt> property.
     * @param isEnabled indicates if the "Google contacts"
     * check box should be selected or not
     */
    void setGoogleContactsEnabled(boolean isEnabled)
    {
        googleContactsBox.setSelected(isEnabled);
    }

    /**
     * Disables Next Button if Port field value is incorrect
     */
    private void setNextButtonAccordingToPortAndPriority()
    {
        try
        {
            Integer.parseInt(getServerPort());
            Integer.parseInt(getPriority());
            parentForm.reValidateInput();
        }
        catch (NumberFormatException ex)
        {
            parentForm.reValidateInput();
        }
    }

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
        return true;
    }

    /**
     * Set auto generate resource value.
     * Set checkbox state.
     * @param value the new value.
     */
    void setAutogenerateResource(boolean value)
    {
        autoGenerateResource.setSelected(value);
    }

    /**
     * Is resource auto generate enabled. Returns checkbox state.
     * @return is resource auto generate enabled.
     */
    boolean isAutogenerateResourceEnabled()
    {
        return autoGenerateResource.isSelected();
    }

    /**
     * Set allow non secure value.
     * @param value the new value.
     */
    void setAllowNonSecure(boolean value)
    {
        this.allowNonSecureBox.setSelected(value);
    }

    /**
     * Is non-TLS allowed.
     * @return is non-TLS allowed
     */
    boolean isAllowNonSecure()
    {
        return allowNonSecureBox.isSelected();
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
            else if(dtmfMethod.equals("INBAND_DTMF"))
            {
                selString =
                    Resources.getString("plugin.sipaccregwizz.DTMF_INBAND");
            }
            else
            {
                selString =
                    Resources.getString("plugin.jabberaccregwizz.DTMF_AUTO");
            }
            dtmfMethodBox.setSelectedItem(selString);
        }
    }

    /**
     * Sets the <tt>serverOverridden</tt> property.
     * @param isServerOverridden <tt>true</tt> to indicate that the server is
     * overridden, <tt>false</tt> - otherwise
     */
    void setServerOverridden(boolean isServerOverridden)
    {
        this.serverAutoCheckBox.setSelected(isServerOverridden);
        enablesServerAutoConfigure(serverAutoCheckBox.isSelected());
    }

    /**
     * Return <tt>isServerOverridden</tt> property.
     * @return <tt>isServerOverridden</tt> property.
     */
    boolean isServerOverridden()
    {
        return this.serverAutoCheckBox.isSelected();
    }

    /**
     * Enables/disables the proxy auto-configuration.
     * @param isEnable <tt>true</tt> to enable proxy auto-configuration,
     * <tt>false</tt> - otherwise
     */
    void enablesServerAutoConfigure(boolean isEnable)
    {
        serverAutoCheckBox.setSelected(isEnable);
        serverField.setEnabled(isEnable);
        portField.setEnabled(isEnable);
        parentForm.reValidateInput();
    }
}
