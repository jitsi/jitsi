/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>AuthenticationWindow</tt> is the window where the user should type
 * his user identifier and password to login.
 *
 * @author Yana Stamcheva
 */
public class AuthenticationWindow
    extends SIPCommFrame
    implements ActionListener
{
    /**
     * Info text area.
     */
    private final JTextArea infoTextArea = new JTextArea();

    /**
     * The uin component.
     */
    private JComponent uinValue;

    /**
     * The password field.
     */
    private final JPasswordField passwdField = new JPasswordField(15);

    /**
     * The login button.
     */
    private final JButton loginButton = new JButton(
        UtilActivator.getResources().getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private final JButton cancelButton = new JButton(
        UtilActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The labels panel.
     */
    private final TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    /**
     * The text fields panel.
     */
    private final TransparentPanel textFieldsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    /**
     * The panel containing all other components.
     */
    private final TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    /**
     * The panel containing all buttons.
     */
    private final TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The check box indicating if the password should be remembered.
     */
    private final JCheckBox rememberPassCheckBox
        = new SIPCommCheckBox(UtilActivator.getResources()
            .getI18NString("service.gui.REMEMBER_PASSWORD"));

    /**
     * The background of the login window.
     */
    private LoginWindowBackground backgroundPanel;

    private String server;

    /**
     * The user name.
     */
    private String userName;

    /**
     * The password.
     */
    private char[] password;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean isRememberPassword = false;

    /**
     * Indicates if the window has been canceled.
     */
    private boolean isCanceled = false;

    /**
     * A lock used to synchronize data setting.
     */
    private final Object lock = new Object();

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param server the server name
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to display on the left of the authentication window
     */
    public AuthenticationWindow(String server,
                                boolean isUserNameEditable,
                                ImageIcon icon)
    {
        this.server = server;

        Image logoImage = null;
        
        if(icon != null)
        {
        	logoImage = icon.getImage();
        }

        if(!isUserNameEditable)
            this.uinValue = new JLabel();
        else
            this.uinValue = new JTextField();

        backgroundPanel = new LoginWindowBackground(logoImage);
        this.backgroundPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 5, 5, 5));
        this.backgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.backgroundPanel.setPreferredSize(new Dimension(420, 230));

        this.getContentPane().setLayout(new BorderLayout());

        this.init();

        this.getContentPane().add(backgroundPanel, BorderLayout.CENTER);

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.enableKeyActions();
    }

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param icon the icon displayed on the left of the authentication window
     * @param errorMessage an error message explaining a reason for opening
     * the authentication dialog (when a wrong password was provided, etc.)
     */
    public AuthenticationWindow(String userName,
                                char[] password,
                                String server,
                                boolean isUserNameEditable,
                                ImageIcon icon,
                                String errorMessage)
    {
        this(userName, password, server, isUserNameEditable, icon);

        this.infoTextArea.setForeground(Color.RED);
        this.infoTextArea.setText(errorMessage);
    }

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param icon the icon displayed on the left of the authentication window
     */
    public AuthenticationWindow(
                String userName,
                char[] password,
                String server,
                boolean isUserNameEditable,
                ImageIcon icon)
    {
        this(server, isUserNameEditable, icon);

        if (userName != null)
        {
            if (uinValue instanceof JLabel)
               ((JLabel) uinValue).setText(userName);
            else if (uinValue instanceof JTextField)
                ((JTextField) uinValue).setText(userName);
        }

        if (password != null)
            passwdField.setText(new String(password));
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init()
    {
        setTitle(UtilActivator.getResources().getI18NString(
            "service.gui.AUTHENTICATION_WINDOW_TITLE", new String[]{server}));

        this.infoTextArea.setEditable(false);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setFont(
            infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setText(
            UtilActivator.getResources().getI18NString(
                "service.gui.AUTHENTICATION_REQUESTED_SERVER",
                new String[]{server}));

        JLabel uinLabel
            = new JLabel(UtilActivator.getResources().getI18NString(
                        "service.gui.IDENTIFIER"));
        uinLabel.setFont(uinLabel.getFont().deriveFont(Font.BOLD));

        JLabel passwdLabel
            = new JLabel(UtilActivator.getResources().getI18NString(
                        "service.gui.PASSWORD"));
        passwdLabel.setFont(passwdLabel.getFont().deriveFont(Font.BOLD));

        this.labelsPanel.add(uinLabel);
        this.labelsPanel.add(passwdLabel);
        this.labelsPanel.add(new JLabel());

        this.rememberPassCheckBox.setOpaque(false);

        this.textFieldsPanel.add(uinValue);
        this.textFieldsPanel.add(passwdField);
        this.textFieldsPanel.add(rememberPassCheckBox);

        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.add(infoTextArea, BorderLayout.NORTH);
        this.mainPanel.add(labelsPanel, BorderLayout.WEST);
        this.mainPanel.add(textFieldsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.backgroundPanel.add(mainPanel, BorderLayout.CENTER);

        this.loginButton.setName("ok");
        this.cancelButton.setName("cancel");

        this.loginButton.setMnemonic(
            UtilActivator.getResources().getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            UtilActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.loginButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.getRootPane().setDefaultButton(loginButton);

        this.setTransparent(true);
    }

    /**
     * Sets transparent background to all components in the login window,
     * because of the non-white background.
     * @param transparent <code>true</code> to set a transparent background,
     * <code>false</code> otherwise.
     */
    private void setTransparent(boolean transparent)
    {
        this.mainPanel.setOpaque(!transparent);
        this.labelsPanel.setOpaque(!transparent);
        this.textFieldsPanel.setOpaque(!transparent);
        this.buttonsPanel.setOpaque(!transparent);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the buttons is
     * clicked. When "Login" button is chosen installs a new account from
     * the user input and logs in.
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if ("ok".equals(buttonName))
        {
            if(uinValue instanceof JLabel)
                userName = ((JLabel) uinValue).getText();
            else if(uinValue instanceof JTextField)
                userName = ((JTextField) uinValue).getText();

            password = passwdField.getPassword();
            isRememberPassword = rememberPassCheckBox.isSelected();
        }
        else
        {
            isCanceled = true;
        }

        synchronized (lock)
        {
            lock.notify();
        }

        this.dispose();
    }

    /**
     * The <tt>LoginWindowBackground</tt> is a <tt>JPanel</tt> that overrides
     * the <code>paintComponent</code> method to provide a custom background
     * image for this window.
     */
    private static class LoginWindowBackground
        extends TransparentPanel
    {
        private final Image bgImage;

        LoginWindowBackground(Image bgImage)
        {
            this.bgImage = bgImage;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            if (bgImage != null)
            {
                g = g.create();
                try
                {
                    AntialiasingManager.activateAntialiasing(g);

                    g.drawImage(bgImage, 30, 30, null);
                }
                finally
                {
                    g.dispose();
                }
            }
        }
    }

    /**
     * Enables the actions when a key is pressed, for now
     * closes the window when esc is pressed.
     */
    private void enableKeyActions()
    {
        UIAction act = new UIAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                AuthenticationWindow.this.setVisible(false);
            }
        };

        getRootPane().getActionMap().put("close", act);

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    /**
     * Automatically clicks the cancel button, when this window is closed.
     *
     * @param isEscaped indicates if the window has been closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Shows this modal dialog.
     *
     * @param isVisible specifies whether we should be showing or hiding the
     * window.
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        this.setName("AUTHENTICATION");

        super.setVisible(isVisible);

        if(isVisible)
        {
            this.passwdField.requestFocus();

            synchronized (lock)
            {
                try
                {
                    lock.wait();
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Indicates if this window has been canceled.
     *
     * @return <tt>true</tt> if this window has been canceled, <tt>false</tt> -
     * otherwise
     */
    public boolean isCanceled()
    {
        return isCanceled;
    }

    /**
     * Returns the user name entered by the user or previously set if the
     * user name is not editable.
     *
     * @return the user name
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Returns the password entered by the user.
     *
     * @return the password
     */
    public char[] getPassword()
    {
        return password;
    }

    /**
     * Indicates if the password should be remembered.
     *
     * @return <tt>true</tt> if the password should be remembered,
     * <tt>false</tt> - otherwise
     */
    public boolean isRememberPassword()
    {
        return isRememberPassword;
    }
}
