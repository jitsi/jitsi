/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The master password input dialog.
 * 
 * @author Dmitri Melnikov
 */
public class MasterPasswordInputDialog
    extends SIPCommDialog
    implements ActionListener,
               KeyListener
{
    /**
     * Instance of this class.
     */
    private static MasterPasswordInputDialog dialog;

    /**
     * The <tt>ResourceManagementService</tt> used by this instance to access
     * the localized and internationalized resources of the application.
     */
    private final ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * Password obtained from the user.
     */
    private String password;

    /**
     * UI components.
     */
    private JPasswordField currentPasswdField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextArea infoTextArea;
    private JTextArea errorTextArea;
    private JPanel buttonsPanel;
    private JPanel dataPanel;

    /**
     * Builds the dialog.
     */
    private MasterPasswordInputDialog()
    {
        super(false);

        initComponents();

        this.setTitle(resources
            .getI18NString("plugin.securityconfig.masterpassword.MP_TITLE"));
        this.setModal(true);
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(createIconComponent(), BorderLayout.WEST);
        mainPanel.add(dataPanel);

        this.getContentPane().add(mainPanel);

        this.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);
    }

    /**
     * Initializes the UI components.
     */
    private void initComponents()
    {
        dataPanel = new TransparentPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));

        // info text
        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setText(resources
            .getI18NString("plugin.securityconfig.masterpassword.MP_INPUT"));

        // error text
        errorTextArea = new JTextArea();
        errorTextArea.setEditable(false);
        errorTextArea.setOpaque(false);
        errorTextArea.setForeground(Color.red);
        errorTextArea.setFont(errorTextArea.getFont().deriveFont(Font.BOLD));
        errorTextArea.setText(resources
            .getI18NString("plugin.securityconfig.masterpassword"
                + ".MP_VERIFICATION_FAILURE_MSG"));

        // password fields
        currentPasswdField = new JPasswordField(15);
        currentPasswdField.addKeyListener(this);
        currentPasswdField.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                okButton.doClick();
            }
        });

        // OK and cancel buttons
        okButton = new JButton(resources.getI18NString("service.gui.OK"));
        okButton.setMnemonic(resources.getI18nMnemonic("service.gui.OK"));
        okButton.addActionListener(this);

        cancelButton =
            new JButton(resources.getI18NString("service.gui.CANCEL"));
        cancelButton.setMnemonic(resources.getI18nMnemonic(
            "service.gui.CANCEL"));
        cancelButton.addActionListener(this);

        buttonsPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 5));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        rebuildMainPanel(false);
    }

    /**
     * Removes and adds again all the components to the main panel.
     * 
     * @param includeErrorMsg when true also includes an error text component
     */
    private void rebuildMainPanel(boolean includeErrorMsg)
    {
        dataPanel.removeAll();

        if (includeErrorMsg)
            dataPanel.add(errorTextArea);
        dataPanel.add(infoTextArea);
        dataPanel.add(currentPasswdField);
        dataPanel.add(buttonsPanel);
    }

    /**
     * Shows an input dialog to the user to obtain the master password.
     * 
     * @param prevSuccess <tt>true</tt> if any previous call returned a correct
     * master password and there is no need to show an extra "verification 
     * failed" message
     * @return the master password obtained from the user or <tt>null</tt> if
     * none was provided
     */
    public static String showInput(boolean prevSuccess)
    {
        if (dialog == null)
            dialog = new MasterPasswordInputDialog();

        dialog.rebuildMainPanel(!prevSuccess);
        dialog.resetPassword();

        // blocks until user performs an action
        dialog.setVisible(true);

        return dialog.password;
    }

    /**
     * OK button click event handler. Retrieves the password and hides the
     * dialog.
     * 
     * @param e action event 
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();
        if (sourceButton.equals(okButton))
        {
            password
                = new String(
                        currentPasswdField.getPassword());
        }
        // hide dialog and unblock application
        dialog.setVisible(false);
    }

    /**
     * Closes the dialog.
     *
     * @param escaped <tt>true</tt> if this dialog has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escaped)
    {
        cancelButton.doClick();
    }

    /**
     * Resets the password by clearing the input field and setting
     * <tt>password</tt> to <tt>null</tt>. Disables the OK button.
     */
    private void resetPassword()
    {
        password = null;
        currentPasswdField.setText("");
        currentPasswdField.requestFocusInWindow();
        okButton.setEnabled(false);
    }

    /**
     * Disables OK button if the password input field is empty.
     * 
     * @param event key event
     */
    public void keyReleased(KeyEvent event)
    {
        JPasswordField source = (JPasswordField) event.getSource();
        if (currentPasswdField.equals(source))
        {
            String password = new String(currentPasswdField.getPassword());
            okButton.setEnabled(password.length() > 0);
            password = null;
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

        iconLabel.setIcon(GuiActivator.getResources()
            .getImage("service.gui.icons.AUTHORIZATION_ICON"));

        wrapIconPanel.add(iconLabel, BorderLayout.NORTH);

        return wrapIconPanel;
    }
}
