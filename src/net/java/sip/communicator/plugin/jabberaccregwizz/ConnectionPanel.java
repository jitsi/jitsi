/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.swing.*;

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

    private final JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.SERVER"));

    private final JTextField serverField = new JTextField();

    private final JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    private final JTextField portField
        = new JTextField(JabberAccountRegistration.DEFAULT_PORT);

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

        labelsAdvOpPanel.add(serverLabel);
        labelsAdvOpPanel.add(portLabel);
        labelsAdvOpPanel.add(resourceLabel);
        labelsAdvOpPanel.add(priorityLabel);

        valuesAdvOpPanel.add(serverField);
        valuesAdvOpPanel.add(portField);
        valuesAdvOpPanel.add(resourceField);
        valuesAdvOpPanel.add(priorityField);

        JPanel checkBoxesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        checkBoxesPanel.add(sendKeepAliveBox);
        checkBoxesPanel.add(gmailNotificationsBox);
        checkBoxesPanel.add(googleContactsBox);

        advancedOpPanel.add(checkBoxesPanel, BorderLayout.NORTH);
        advancedOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        advancedOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);

        mainPanel.add(advancedOpPanel);

        String serverAddress = parentForm.getServerAddress();
        if (serverAddress != null)
            serverField.setText(serverAddress);

        add(mainPanel, BorderLayout.NORTH);
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
}
