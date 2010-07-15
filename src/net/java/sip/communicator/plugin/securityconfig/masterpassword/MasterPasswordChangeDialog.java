/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * UI dialog to change the master password.
 *
 * @author Dmitri Melnikov
 */
public class MasterPasswordChangeDialog
    extends SIPCommDialog
    implements ActionListener,
               KeyListener
{
    /**
     * Callback interface. Implementing classes know how to change the master
     * password from the old to the new one.
     */
    interface MasterPasswordExecutable
    {
        /**
         * The actions to execute to change the master password.
         *
         * @param masterPassword old master password
         * @param newMasterPassword new master password
         * @return true on success, false on failure.
         */
        public boolean execute(String masterPassword, String newMasterPassword);
    }

    /**
     * Dialog instance of this class.
     */
    private static MasterPasswordChangeDialog dialog;

    /**
     * Password quality meter.
     */
    private static PasswordQualityMeter passwordMeter =
        new PasswordQualityMeter();

    /**
     * Callback to execute on password change.
     */
    private MasterPasswordExecutable callback;

    /**
     * UI components.
     */
    private JTextComponent currentPasswdField;
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
    private JPanel mainPanel;

    /**
     * The <tt>ResourceManagementService</tt> used by this instance to access
     * the localized and internationalized resources of the application.
     */
    private final ResourceManagementService resources
        = SecurityConfigActivator.getResources();

    /**
     * Builds the dialog.
     */
    private MasterPasswordChangeDialog()
    {
        super(false);
        initComponents();

        this.setTitle(
                resources.getI18NString(
                        "plugin.securityconfig.masterpassword.MP_TITLE"));
        this.setMinimumSize(new Dimension(350, 300));
        this.setPreferredSize(new Dimension(350, 300));
        this.setResizable(false);

        this.getContentPane().add(mainPanel);

        this.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);

        if (currentPasswdField instanceof JPasswordField)
        {
            currentPasswdField.requestFocusInWindow();
        }
        else
        {
            newPasswordField.requestFocusInWindow();
        }
    }

    /**
     * Initialises the UI components.
     */
    private void initComponents()
    {
        mainPanel = new TransparentPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // info text
        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setText(
                resources.getI18NString("plugin.securityconfig.masterpassword.INFO_TEXT"));

        // label fields
        labelsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        labelsPanel.add(
                new JLabel(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.CURRENT_PASSWORD")));
        labelsPanel.add(
                new JLabel(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.ENTER_PASSWORD")));
        labelsPanel.add(
                new JLabel(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.REENTER_PASSWORD")));

        // password fields
        if (!SecurityConfigActivator
                .getCredentialsStorageService()
                    .isUsingMasterPassword())
        {
            currentPasswdField
                = new JTextField(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.MP_NOT_SET"));
            currentPasswdField.setEnabled(false);
        }
        else
        {
            currentPasswdField = new JPasswordField(15);
        }
        newPasswordField = new JPasswordField(15);
        newPasswordField.addKeyListener(this);
        newAgainPasswordField = new JPasswordField(15);
        newAgainPasswordField.addKeyListener(this);

        textFieldsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));
        textFieldsPanel.add(currentPasswdField);
        textFieldsPanel.add(newPasswordField);
        textFieldsPanel.add(newAgainPasswordField);

        // OK and cancel buttons
        okButton = new JButton(resources.getI18NString("service.gui.OK"));
        okButton.addActionListener(this);
        okButton.setEnabled(false);
        cancelButton
            = new JButton(resources.getI18NString("service.gui.CANCEL"));
        cancelButton.addActionListener(this);

        passwordQualityBar =
            new JProgressBar(0, PasswordQualityMeter.TOTAL_POINTS);
        passwordQualityBar.setValue(0);

        qualityPanel = new TransparentPanel();
        qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
        qualityPanel.add(
                new JLabel(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.PASSWORD_QUALITY_METER")));
        qualityPanel.add(passwordQualityBar);
        qualityPanel.add(Box.createVerticalStrut(15));

        buttonsPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        qualityPanel.add(buttonsPanel);

        mainPanel.add(infoTextArea, BorderLayout.NORTH);
        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(textFieldsPanel, BorderLayout.CENTER);
        mainPanel.add(qualityPanel, BorderLayout.SOUTH);
    }

    /**
     * OK and Cancel button event handler.
     * 
     * @param e action event
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();
        boolean close = false;
        if (sourceButton.equals(okButton)) // ok button
        {
            CredentialsStorageService credentialsStorageService
                = SecurityConfigActivator.getCredentialsStorageService();
            String oldMasterPassword = null;

            if (credentialsStorageService.isUsingMasterPassword())
            {
                oldMasterPassword =
                    new String(((JPasswordField) currentPasswdField)
                        .getPassword());
                if (oldMasterPassword.length() == 0)
                {
                    displayPopupError(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.MP_CURRENT_EMPTY"));
                    return;
                }
                boolean verified =
                    credentialsStorageService
                        .verifyMasterPassword(oldMasterPassword);
                if (!verified)
                {
                    displayPopupError(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.MP_VERIFICATION_FAILURE_MSG"));
                    return;
                }
            }
            // if the callback executes OK, we close the dialog
            if (callback != null)
            {
                String newPassword = new String(newPasswordField.getPassword());
                close = callback.execute(oldMasterPassword, newPassword);
            }
        }
        else // cancel button
        {
            close = true;
        }

        if (close)
        {
            dialog = null;
            dispose();
        }
    }

    /**
     * Displays an error pop-up.
     *
     * @param message the message to display
     */
    protected void displayPopupError(String message)
    {
        SecurityConfigActivator
            .getUIService()
            .getPopupDialog()
            .showMessagePopupDialog(
                message,
                resources.getI18NString(
                        "plugin.securityconfig.masterpassword.MP_CHANGE_FAILURE"),
                PopupDialog.ERROR_MESSAGE);
    }

    /**
     * Displays an info pop-up.
     *
     * @param message the message to display.
     */
    protected void displayPopupInfo(String message)
    {
        SecurityConfigActivator
            .getUIService()
            .getPopupDialog()
            .showMessagePopupDialog(
                message,
                resources.getI18NString(
                        "plugin.securityconfig.masterpassword.MP_CHANGE_SUCCESS"),
                PopupDialog.INFORMATION_MESSAGE);
    }

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
     * @return dialog instance
     */
    public static MasterPasswordChangeDialog getInstance()
    {
        if (dialog == null)
            dialog = new MasterPasswordChangeDialog();
        return dialog;
    }

    /**
     * @param callbackInstance callback instance.
     */
    public void setCallback(MasterPasswordExecutable callbackInstance)
    {
        this.callback = callbackInstance;
    }
}
