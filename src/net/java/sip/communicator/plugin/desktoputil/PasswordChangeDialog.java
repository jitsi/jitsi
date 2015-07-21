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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * UI dialog to change the master password.
 *
 * @author Dmitri Melnikov
 * @author Boris Grozev
 */
public class PasswordChangeDialog
    extends SIPCommDialog
    implements KeyListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>ResourceManagementService</tt> used by this instance to access
     * the localized and internationalized resources of the application.
     */
    protected final ResourceManagementService resources
        = DesktopUtilActivator.getResources();

    /**
     * Password quality meter.
     */
    private PasswordQualityMeter passwordMeter =
        new PasswordQualityMeter();

    /**
     * Whether to show a current password field or not
     */
    private boolean showCurrentPassword = false;

    /**
     * UI components.
     */
    private JPasswordField currentPasswdField;
    private JPasswordField newPasswordField;
    private JPasswordField newAgainPasswordField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextArea infoTextArea;
    private JProgressBar passwordQualityBar;
    private JPanel textFieldsPanel;
    private JPanel labelsPanel;
    private JPanel buttonsPanel;
    private JPanel qualityPanel;
    private JPanel dataPanel;

    /**
     * Builds the dialog, no current password
     */
    public PasswordChangeDialog()
    {
        this(false);
    }

    /**
     * Builds the dialog.
     *
     * @param showCurrentPassword Whether to show a "current password" field
     */
    public PasswordChangeDialog(boolean showCurrentPassword)
    {
        super(false);

        this.showCurrentPassword = showCurrentPassword;

        initComponents();

        this.setTitle(resources
                .getI18NString("service.gui.CHANGE_PASSWORD"));
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(createIconComponent(), BorderLayout.WEST);
        mainPanel.add(dataPanel);

        this.getContentPane().add(mainPanel);

        this.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);

        if (showCurrentPassword)
        {
            currentPasswdField.requestFocusInWindow();
        }
        else
        {
            newPasswordField.requestFocusInWindow();
        }
    }

    /**
     * Initializes the UI components.
     */
    private void initComponents()
    {
        dataPanel = new TransparentPanel(new BorderLayout(10, 10));
        dataPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // info text
        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setText(resources
                .getI18NString("service.gui.CHANGE_PASSWORD"));

        // label fields
        labelsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        if(showCurrentPassword)
        {
            labelsPanel.add(new JLabel(resources.getI18NString(
                    "plugin.securityconfig.masterpassword.CURRENT_PASSWORD")));
        }
        labelsPanel.add(new JLabel(resources.getI18NString(
                    "plugin.securityconfig.masterpassword.ENTER_PASSWORD")));
        labelsPanel.add(new JLabel(resources.getI18NString(
                    "plugin.securityconfig.masterpassword.REENTER_PASSWORD")));

        // password fields
        ActionListener clickOkButton = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (okButton.isEnabled())
                    okButton.doClick();
            }
        };

        if(showCurrentPassword)
        {
            currentPasswdField = new JPasswordField(15);
            currentPasswdField.addActionListener(clickOkButton);
        }
        newPasswordField = new JPasswordField(15);
        newPasswordField.addKeyListener(this);
        newPasswordField.addActionListener(clickOkButton);
        newAgainPasswordField = new JPasswordField(15);
        newAgainPasswordField.addKeyListener(this);
        newAgainPasswordField.addActionListener(clickOkButton);

        textFieldsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));
        if(showCurrentPassword)
        {
            textFieldsPanel.add(currentPasswdField);
        }
        textFieldsPanel.add(newPasswordField);
        textFieldsPanel.add(newAgainPasswordField);

        // OK and cancel buttons
        okButton = new JButton(resources.getI18NString("service.gui.OK"));
        okButton.setMnemonic(resources.getI18nMnemonic("service.gui.OK"));
        okButton.setEnabled(false);
        cancelButton
            = new JButton(resources.getI18NString("service.gui.CANCEL"));
        cancelButton.setMnemonic(resources.getI18nMnemonic(
            "service.gui.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        passwordQualityBar =
            new JProgressBar(0, PasswordQualityMeter.TOTAL_POINTS);
        passwordQualityBar.setValue(0);

        qualityPanel = new TransparentPanel();
        qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));

        JLabel qualityMeterLabel = new JLabel(resources.getI18NString(
            "plugin.securityconfig.masterpassword.PASSWORD_QUALITY_METER"));
        qualityMeterLabel.setAlignmentX(CENTER_ALIGNMENT);

        qualityPanel.add(qualityMeterLabel);
        qualityPanel.add(passwordQualityBar);
        qualityPanel.add(Box.createVerticalStrut(15));

        buttonsPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 5));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        qualityPanel.add(buttonsPanel);

        dataPanel.add(infoTextArea, BorderLayout.NORTH);
        dataPanel.add(labelsPanel, BorderLayout.WEST);
        dataPanel.add(textFieldsPanel, BorderLayout.CENTER);
        dataPanel.add(qualityPanel, BorderLayout.SOUTH);
    }

    /**
     * Displays an error pop-up.
     *
     * @param message the message to display
     */
    protected void displayPopupError(String message)
    {
        DesktopUtilActivator
            .getUIService()
            .getPopupDialog()
            .showMessagePopupDialog(
                message,
                resources.getI18NString(
                        "service.gui.PASSWORD_CHANGE_FAILURE"),
                PopupDialog.ERROR_MESSAGE);
    }

    /**
     * Displays an info pop-up.
     *
     * @param message the message to display.
     */
    protected void displayPopupInfo(String message)
    {
        DesktopUtilActivator
            .getUIService()
            .getPopupDialog()
            .showMessagePopupDialog(
                message,
                resources.getI18NString(
                        "service.gui.PASSWORD_CHANGE_SUCCESS"),
                PopupDialog.INFORMATION_MESSAGE);
    }

    @Override
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }

    /**
     * When a key is pressed we do 2 things. The first is to compare the two
     * password input fields and enable OK button if they are equal. The second
     * is to measure the password quality of the password from the first input
     * field.
     *
     * @param event key event
     */
    public void keyReleased(KeyEvent event)
    {
        JPasswordField source = (JPasswordField) event.getSource();
        if (newPasswordField.equals(source)
            || newAgainPasswordField.equals(source))
        {
            String password1 = new String(newPasswordField.getPassword());
            String password2 = new String(newAgainPasswordField.getPassword());
            // measure password quality
            passwordQualityBar
                .setValue(passwordMeter.assessPassword(password1));
            // enable OK button if passwords are equal
            boolean eq = (password1.length() != 0)
                        && password1.equals(password2);
            okButton.setEnabled(eq);
            password1 = null;
            password2 = null;
        }
    }

    /**
     * Not overriding.
     *
     * @param arg0 key event
     */
    public void keyPressed(KeyEvent arg0)
    {
    }

    /**
     * Not overriding.
     *
     * @param arg0 key event
     */
    public void keyTyped(KeyEvent arg0)
    {
    }

    /**
     * Creates the icon component to show on the left of this dialog.
     *
     * @return the created component
     */
    private static Component createIconComponent()
    {
        JPanel wrapIconPanel = new JPanel(new BorderLayout());

        JLabel iconLabel = new JLabel();

        iconLabel.setIcon(DesktopUtilActivator.getResources()
            .getImage("service.gui.icons.AUTHORIZATION_ICON"));

        wrapIconPanel.add(iconLabel, BorderLayout.NORTH);

        return wrapIconPanel;
    }

    /**
     * Return a reference to the "ok" button.
     *
     * @return a reference to the "ok" button.
     */
    protected JButton getOkButton()
    {
        return okButton;
    }

    /**
     * Return a reference to the "cancel" button.
     *
     * @return a reference to the "cancel" button.
     */
    protected JButton getCancelButton()
    {
        return cancelButton;
    }

    /**
     * Return the string entered in the password field.
     *
     * @return the string entered in the password field.
     */
    protected String getNewPassword()
    {
        return new String(newPasswordField.getPassword());
    }

    /**
     * Return the string entered in the "current password" field, or null if
     * that field is not shown.
     *
     * @return the string entered in the "current password" field.
     */
    protected String getCurrentPassword()
    {
        if(currentPasswdField == null)
        {
            return null;
        }
        else
        {
            return new String(currentPasswdField.getPassword());
        }
    }

    /**
     * Sets the descriptional text that is displayed
     * @param infoText the new text to display.
     */
    protected void setInfoText(String infoText)
    {
        infoTextArea.setText(infoText);
    }

}
