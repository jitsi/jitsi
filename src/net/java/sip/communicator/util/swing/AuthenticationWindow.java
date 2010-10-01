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
     * The check box indicating if the password should be remembered.
     */
    private final JCheckBox rememberPassCheckBox
        = new SIPCommCheckBox(UtilActivator.getResources()
            .getI18NString("service.gui.REMEMBER_PASSWORD"));

    /**
     * The name of the server, for which this authentication window is about.
     */
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
        super(false);

        this.server = server;

        if(icon != null)
        {
            initIcon(icon);
        }

        if(!isUserNameEditable)
            this.uinValue = new JLabel();
        else
            this.uinValue = new JTextField();

        this.init();

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.enableKeyActions();

        this.setResizable(false);

        /*
         * Workaround for the following bug:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
         * Need to pack() the window after it's opened in order to obtain the
         * correct size of our infoTextArea, otherwise window size is wrong and
         * buttons on the south are cut.
         */
        this.addWindowListener(new WindowAdapter()
        {
            public void windowOpened(WindowEvent e)
            {
                pack();
                removeWindowListener(this);
            }
        });
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
     * Initializes the icon image.
     *
     * @param icon the icon to show on the left of the window
     */
    private void initIcon(ImageIcon icon)
    {
        JLabel iconLabel = new JLabel(icon);

        iconLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel iconPanel = new TransparentPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);

        getContentPane().add(iconPanel, BorderLayout.WEST);
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init()
    {
        setTitle(UtilActivator.getResources().getI18NString(
            "service.gui.AUTHENTICATION_WINDOW_TITLE", new String[]{server}));

        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(
            infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setText(
            UtilActivator.getResources().getI18NString(
                "service.gui.AUTHENTICATION_REQUESTED_SERVER",
                new String[]{server}));
        infoTextArea.setAlignmentX(0.5f);

        JLabel uinLabel
            = new JLabel(UtilActivator.getResources().getI18NString(
                        "service.gui.IDENTIFIER"));
        uinLabel.setFont(uinLabel.getFont().deriveFont(Font.BOLD));

        JLabel passwdLabel
            = new JLabel(UtilActivator.getResources().getI18NString(
                        "service.gui.PASSWORD"));
        passwdLabel.setFont(passwdLabel.getFont().deriveFont(Font.BOLD));

        TransparentPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        labelsPanel.add(uinLabel);
        labelsPanel.add(passwdLabel);
        labelsPanel.add(new JLabel());

        this.rememberPassCheckBox.setOpaque(false);
        this.rememberPassCheckBox.setBorder(null);

        TransparentPanel textFieldsPanel
            = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        textFieldsPanel.add(uinValue);
        textFieldsPanel.add(passwdField);
        textFieldsPanel.add(rememberPassCheckBox);

        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        JPanel southEastPanel = new TransparentPanel(new BorderLayout());
        southEastPanel.add(buttonPanel, BorderLayout.EAST);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 0, 20, 20));

        mainPanel.add(infoTextArea, BorderLayout.NORTH);
        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(textFieldsPanel, BorderLayout.CENTER);
        mainPanel.add(southEastPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel, BorderLayout.EAST);

        this.loginButton.setName("ok");
        this.cancelButton.setName("cancel");

        this.loginButton.setMnemonic(
            UtilActivator.getResources().getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            UtilActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.loginButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.getRootPane().setDefaultButton(loginButton);
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
