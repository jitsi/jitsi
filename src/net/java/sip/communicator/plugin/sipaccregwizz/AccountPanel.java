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

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The panel containing all account related information like user name and
 * password.
 *
 * @author Yana Stamcheva
 */
public class AccountPanel
    extends TransparentPanel
    implements DocumentListener,
               ValidatingPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final Logger logger = Logger.getLogger(AccountPanel.class);

    private final JPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private final JPanel valuesPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private final JPanel emptyPanel = new TransparentPanel();

    private final JTextField userIDField = new TrimTextField();

    private final JPasswordField passField = new JPasswordField();

    private final JTextField displayNameField = new JTextField();

    private final JCheckBox rememberPassBox
        = new SIPCommCheckBox(
            Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private final JLabel displayNameLabel
        = new JLabel(Resources.getString("plugin.sipaccregwizz.DISPLAY_NAME"));

    private final JRadioButton existingAccountButton;

    private final JRadioButton createAccountButton;

    private final JPanel uinPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel mainPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private final SIPAccountRegistrationForm regform;

    private Component registrationForm;

    private boolean isSimpleForm;

    private Component registerChoicePanel;

    /**
     * Creates an instance of the <tt>AccountPanel</tt>.
     * @param regform the parent registration form
     */
    public AccountPanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout());

        this.regform = regform;
        this.regform.addValidatingPanel(this);

        this.userIDField.getDocument().addDocumentListener(this);

        this.rememberPassBox.setSelected(true);

        existingAccountButton = new JRadioButton(
            regform.getExistingAccountLabel());

        createAccountButton = new JRadioButton(
            regform.getCreateAccountLabel());

        JLabel uinExampleLabel = new JLabel(regform.getUsernameExample());
        uinExampleLabel.setForeground(Color.GRAY);
        uinExampleLabel.setFont(uinExampleLabel.getFont().deriveFont(8));
        emptyPanel.setMaximumSize(new Dimension(40, 35));
        uinExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel uinLabel
            = new JLabel(regform.getUsernameLabel());

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

        String webSignupLinkText = regform.getWebSignupLinkName();

        if (webSignupLinkText != null && webSignupLinkText.length() > 0)
            southPanel.add(createWebSignupLabel(webSignupLinkText));
        else
        {
            String forgotPassLinkText = regform.getForgotPasswordLinkName();

            if (forgotPassLinkText != null && forgotPassLinkText.length() > 0)
            southPanel.add(createForgotPasswordLabel(forgotPassLinkText));
        }

        uinPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.sipaccregwizz.USERNAME_AND_PASSWORD")));

        SIPAccountCreationFormService createAccountService
            = regform.getCreateAccountService();

        if (createAccountService != null && isSimpleForm)
        {
            registrationForm = createAccountService.getForm();
            registerChoicePanel = createRegisterChoicePanel();
            mainPanel.add(registerChoicePanel, BorderLayout.NORTH);
        }
        else
            mainPanel.add(uinPassPanel, BorderLayout.NORTH);

        this.add(mainPanel, BorderLayout.NORTH);
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

        // Select the existing account radio button by default.
        existingAccountButton.setSelected(true);

        // Indicate that this panel is opened in a simple form.
        setSimpleForm(false);
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the UIN
     * field. Enables or disables the "Next" wizard button according to whether
     * the UIN field is empty.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent e)
    {
        regform.setServerFieldAccordingToUIN(userIDField.getText());
        regform.reValidateInput();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        regform.setServerFieldAccordingToUIN(userIDField.getText());
        regform.reValidateInput();
    }

    public void changedUpdate(DocumentEvent e) {}

    /**
     * Returns the user identifier entered by the user.
     * @return the user identifier
     */
    String getUserID()
    {
        String userID = userIDField.getText();

        if(userID.startsWith("sip:"))
            return userID.substring(4);

        return userID;
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
        regform.reValidateInput();
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
    private Component createWebSignupLabel(String linkName)
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
            @Override
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

    /**
     * Creates the subscribe label.
     * @param linkName the link name
     * @return the newly created subscribe label
     */
    private Component createForgotPasswordLabel(String linkName)
    {
        JLabel subscribeLabel =
            new JLabel("<html><a href=''>"
                + linkName
                + "</a></html>",
                JLabel.RIGHT);

        subscribeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subscribeLabel.setToolTipText(
            Resources.getString("plugin.simpleaccregwizz.FORGOT_PASSWORD"));
        subscribeLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    SIPAccRegWizzActivator.getBrowserLauncher()
                        .openURL(regform.getForgotPasswordLink());
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

    /**
     * Creates a register choice panel.
     * @return the created component
     */
    private Component createRegisterChoicePanel()
    {
        JPanel registerChoicePanel = new TransparentPanel(new GridLayout(0, 1));

        existingAccountButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (existingAccountButton.isSelected())
                {
                    mainPanel.remove(registrationForm);
                    mainPanel.add(uinPassPanel, BorderLayout.CENTER);

                    Window window
                        = SwingUtilities.getWindowAncestor(AccountPanel.this);

                    if (window != null)
                        window.pack();
                }
            }
        });

        createAccountButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (createAccountButton.isSelected())
                {
                    mainPanel.remove(uinPassPanel);
                    mainPanel.add(registrationForm, BorderLayout.CENTER);
                    SwingUtilities.getWindowAncestor(AccountPanel.this).pack();
                }
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();

        existingAccountButton.setOpaque(false);
        createAccountButton.setOpaque(false);

        buttonGroup.add(existingAccountButton);
        buttonGroup.add(createAccountButton);

        registerChoicePanel.add(existingAccountButton);
        registerChoicePanel.add(createAccountButton);

        // By default we select the existing account button.
        existingAccountButton.setSelected(true);

        return registerChoicePanel;
    }

    /**
     * Indicates if the account information provided by this form is for new
     * account or an existing one.
     * @return <tt>true</tt> if the account information provided by this form
     * is for new account or <tt>false</tt> if it's for an existing one
     */
    boolean isCreateAccount()
    {
        return createAccountButton.isSelected();
    }

    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification.
     *
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated()
    {
        return userIDField.getText() != null
                && userIDField.getText().length() > 0;
    }

    /**
     * Sets to <tt>true</tt> if this panel is opened in a simple form and
     * <tt>false</tt> if it's opened in an advanced form.
     *
     * @param isSimpleForm indicates if this panel is opened in a simple form or
     * in an advanced form
     */
    void setSimpleForm(boolean isSimpleForm)
    {
        this.isSimpleForm = isSimpleForm;

        SIPAccountCreationFormService createAccountService
            = regform.getCreateAccountService();

        if (createAccountService != null && isSimpleForm)
        {
            registrationForm = createAccountService.getForm();
            if (uinPassPanel != null)
                mainPanel.remove(uinPassPanel);

            registerChoicePanel = createRegisterChoicePanel();
            mainPanel.add(registerChoicePanel, BorderLayout.NORTH);
        }
        else
        {
            if (registerChoicePanel != null)
                mainPanel.remove(registerChoicePanel);

            mainPanel.add(uinPassPanel, BorderLayout.NORTH);
        }
    }

    /**
     * Returns <tt>true</tt> if this panel is opened in a simple form and
     * <tt>false</tt> if it's opened in an advanced form.
     *
     * @return <tt>true</tt> if this panel is opened in a simple form and
     * <tt>false</tt> if it's opened in an advanced form
     */
    boolean isSimpleForm()
    {
        return isSimpleForm;
    }

    /**
     * Selects the create account button.
     */
    void setCreateButtonSelected()
    {
        createAccountButton.setSelected(true);
    }
}
