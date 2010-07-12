/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing all account related information like user name and
 * password.
 *
 * @author Yana Stamcheva
 */
public class AccountPanel
    extends TransparentPanel
    implements DocumentListener
{
    private final Logger logger = Logger.getLogger(AccountPanel.class);

    private final JPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));

    private JPanel emptyPanel = new TransparentPanel();

    private JTextField userIDField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private final JTextField displayNameField = new JTextField();

    private JCheckBox rememberPassBox
        = new SIPCommCheckBox(
            Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private final JLabel displayNameLabel
        = new JLabel(Resources.getString("plugin.sipaccregwizz.DISPLAY_NAME"));

    private final SIPAccountRegistrationForm regform;

    /**
     * Creates an instance of the <tt>AccountPanel</tt>.
     * @param regform the parent registration form
     */
    public AccountPanel(SIPAccountRegistrationForm regform)
    {
        super (new BorderLayout());

        this.regform = regform;

        this.userIDField.getDocument().addDocumentListener(this);

        this.rememberPassBox.setSelected(true);

        JLabel uinExampleLabel = new JLabel(regform.getUsernameExample());
        uinExampleLabel.setForeground(Color.GRAY);
        uinExampleLabel.setFont(uinExampleLabel.getFont().deriveFont(8));
        emptyPanel.setMaximumSize(new Dimension(40, 35));
        uinExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel uinPassPanel
            = new TransparentPanel(new BorderLayout(10, 10));

        JLabel uinLabel
            = new JLabel(Resources.getString("plugin.sipaccregwizz.USERNAME"));

        JLabel passLabel
            = new JLabel(Resources.getString("service.gui.PASSWORD"));

        labelsPanel.add(uinLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(uinExampleLabel);
        valuesPanel.add(passField);

        TransparentPanel southPanel
            = new TransparentPanel(new GridLayout(1, 2));

        uinPassPanel.add(labelsPanel, BorderLayout.WEST);
        uinPassPanel.add(valuesPanel, BorderLayout.CENTER);
        uinPassPanel.add(southPanel, BorderLayout.SOUTH);

        southPanel.add(rememberPassBox);

        String webSignup = regform.getWebSignupLinkName();
        if (webSignup != null)
        {
            southPanel.add(createSubscribeLabel(webSignup));
        }

        uinPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.sipaccregwizz.USERNAME_AND_PASSWORD")));

        this.add(uinPassPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the advanced account panel.
     */
    void initAdvancedForm()
    {
        // If it's not yet added.
        if (displayNameLabel.getParent() == null)
            labelsPanel.add(displayNameLabel);

        // If it's not yet added.
        if (displayNameField.getParent() == null)
            valuesPanel.add(displayNameField);
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the UIN
     * field. Enables or disables the "Next" wizard button according to whether
     * the UIN field is empty.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent e)
    {
        regform.setNextFinishButtonEnabled(userIDField.getText() == null
                                            || userIDField.getText().equals(""));
        regform.setServerFieldAccordingToUIN(userIDField.getText());
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        regform.setNextFinishButtonEnabled(userIDField.getText() == null
                                            || userIDField.getText().equals(""));
        regform.setServerFieldAccordingToUIN(userIDField.getText());
    }

    public void changedUpdate(DocumentEvent e) {}

    /**
     * Returns the user identifier entered by the user.
     * @return the user identifier
     */
    String getUserID()
    {
        return userIDField.getText();
    }

    /**
     * Returns the password entered by the user.
     * @return the password
     */
    char[] getPassword()
    {
        return passField.getPassword();
    }

    /**
     * Indicates if the "remember password" check box is selected.
     * @return <tt>true</tt> if the "remember password" check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isRememberPassword()
    {
        return rememberPassBox.isSelected();
    }

    /**
     * Returns the display name of the account.
     * @return the display name of the account
     */
    String getDisplayName()
    {
        return displayNameField.getText();
    }

    /**
     * Sets the display name of the account.
     * @param displayName the display name of the account
     */
    void setDisplayName(String displayName)
    {
        displayNameField.setText(displayName);
    }

    /**
     * Enables/disables the user id text field.
     * @param isEnabled <tt>true</tt> to enable the user id text field,
     * <tt>false</tt> - otherwise
     */
    void setUserIDEnabled(boolean isEnabled)
    {
        userIDField.setEnabled(isEnabled);
    }

    /**
     * Sets the user id.
     * @param userID the user id to set
     */
    void setUserID(String userID)
    {
        userIDField.setText(userID);
    }

    /**
     * Sets the password
     * @param password the password
     */
    void setPassword(String password)
    {
        this.passField.setText(password);
    }

    /**
     * Sets the password remember check box.
     * @param isRememberPassword <tt>true</tt> to select the remember password
     * check box, <tt>false</tt> - otherwise
     */
    void setRememberPassword(boolean isRememberPassword)
    {
        rememberPassBox.setSelected(isRememberPassword);
    }

    /**
     * Creates the subscribe label.
     * @param linkName the link name
     * @return the newly created subscribe label
     */
    private Component createSubscribeLabel(String linkName)
    {
        JLabel subscribeLabel =
            new JLabel("<html><a href=''>"
                + linkName
                + "</a></html>",
                JLabel.RIGHT);

        subscribeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subscribeLabel.setToolTipText(
            Resources.getString("plugin.simpleaccregwizz.SPECIAL_SIGNUP"));
        subscribeLabel.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    regform.webSignup();
                }
                catch (UnsupportedOperationException ex)
                {
                    // This should not happen, because we check if the
                    // operation is supported, before adding the sign
                    // up.
                    logger.error("The web sign up is not supported.",
                        ex);
                }
            }
        });
        return subscribeLabel;
    }
}
