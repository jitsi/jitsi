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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * The <tt>AuthenticationWindow</tt> is the window where the user should type
 * his user identifier and password to login.
 *
 * @author Yana Stamcheva
 */
public class AuthenticationWindow
    extends SIPCommDialog
    implements ActionListener,
               AuthenticationWindowService.AuthenticationWindow
{
    private static final long serialVersionUID = 1L;

    /**
     * Used for logging.
     */
    private static Logger logger = Logger.getLogger(AuthenticationWindow.class);

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
        DesktopUtilActivator.getResources().getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private final JButton cancelButton = new JButton(
        DesktopUtilActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The check box indicating if the password should be remembered.
     */
    private final JCheckBox rememberPassCheckBox = new SIPCommCheckBox(
        DesktopUtilActivator.getResources()
            .getI18NString("service.gui.REMEMBER_PASSWORD"),
        DesktopUtilActivator.getConfigurationService()
            .getBoolean(PNAME_SAVE_PASSWORD_TICKED, false));

    /**
     * Property to disable/enable allow save password option
     * in authentication window. By default it is enabled.
     */
    private static final String PNAME_ALLOW_SAVE_PASSWORD =
        "net.java.sip.communicator.util.swing.auth.ALLOW_SAVE_PASSWORD";

    /**
     * Property to set whether the save password option in
     * the authentication window is ticked by default or not.
     * By default it is not ticked
     */
    private static final String PNAME_SAVE_PASSWORD_TICKED =
        "net.java.sip.communicator.util.swing.auth.SAVE_PASSWORD_TICKED";

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
     * The condition that decides whether to continue waiting for data.
     */
    private boolean buttonClicked = false;

    /**
     * Used to override default Authentication window title.
     */
    private String windowTitle = null;

    /**
     * Used to override default window text.
     */
    private String windowText = null;

    /**
     * Used to override username label text.
     */
    private String usernameLabelText = null;

    /**
     * Used to override password label text.
     */
    private String passwordLabelText = null;

    /**
     * The sign up link if specified.
     */
    private String signupLink = null;

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
        this(null, null, server, isUserNameEditable, false,
             icon, null, null, null, null, null, null);
    }

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param server the server name
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to display on the left of the authentication window
     * @param windowTitle customized window title
     * @param windowText customized window text
     * @param usernameLabelText customized username field label text
     * @param passwordLabelText customized password field label text
     * @param errorMessage an error message if this dialog is shown to indicate
     * the user that something went wrong
     * @param signupLink an URL that allows the user to sign up
     */
    AuthenticationWindow(String userName,
                                char[] password,
                                String server,
                                boolean isUserNameEditable,
                                boolean isRememberPassword,
                                ImageIcon icon,
                                String windowTitle,
                                String windowText,
                                String usernameLabelText,
                                String passwordLabelText,
                                String errorMessage,
                                String signupLink)
    {
        super(
            (Window)(DesktopUtilActivator.getUIService() == null ?
                        null :
                        DesktopUtilActivator.getUIService()
                            .getExportedWindow(ExportedWindow.MAIN_WINDOW)),
            false);

        this.windowTitle = windowTitle;
        this.windowText = windowText;
        this.usernameLabelText = usernameLabelText;
        this.passwordLabelText = passwordLabelText;
        this.isRememberPassword = isRememberPassword;
        this.signupLink = signupLink;

        init(userName, password, server, isUserNameEditable, icon, errorMessage);
    }

    /**
     * Initializes this authentication window.
     *
     * @param server the server
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to show on the authentication window
     */
    private void init(  String userName,
                        char[] password,
                        String server,
                        boolean isUserNameEditable,
                        ImageIcon icon,
                        String errorMessage)
    {
        this.server = server;

        initIcon(icon);

        if(!isUserNameEditable)
            this.uinValue = new JLabel();
        else
            this.uinValue = new JTextField();

        this.init(isUserNameEditable);

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
            @Override
            public void windowOpened(WindowEvent e)
            {
                pack();
                removeWindowListener(this);
            }
        });

        if (OSUtils.IS_MAC)
            getRootPane()
                .putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);

        if (userName != null)
        {
            if (uinValue instanceof JLabel)
               ((JLabel) uinValue).setText(userName);
            else if (uinValue instanceof JTextField)
                ((JTextField) uinValue).setText(userName);
        }

        if (password != null)
            passwdField.setText(new String(password));

        if(errorMessage != null)
        {
            this.infoTextArea.setForeground(Color.RED);
            this.infoTextArea.setText(errorMessage);
        }
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
        this(userName, password, server,
                    isUserNameEditable,
                    false,
                    icon, null, null, null, null, errorMessage, null);
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
     * @param signupLink an URL that allows the user to sign up
     */
    public AuthenticationWindow(String userName,
                                char[] password,
                                String server,
                                boolean isUserNameEditable,
                                ImageIcon icon,
                                String errorMessage,
                                String signupLink)
    {
        this(userName, password, server,
                    isUserNameEditable,
                    false,
                    icon, null, null, null, null, errorMessage, signupLink);
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
        this(userName, password, server, isUserNameEditable, icon, null, null);
    }

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param owner the owner of this dialog
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param icon the icon displayed on the left of the authentication window
     */
    public AuthenticationWindow(
                Dialog owner,
                String userName,
                char[] password,
                String server,
                boolean isUserNameEditable,
                ImageIcon icon)
    {
        super(owner, false);

        init(userName, password, server, isUserNameEditable, icon, null);
    }

    /**
     * Shows or hides the "save password" checkbox.
     * @param allow the checkbox is shown when allow is <tt>true</tt>
     */
    public void setAllowSavePassword(boolean allow)
    {
        rememberPassCheckBox.setVisible(allow);
    }

    /**
     * Initializes the icon image.
     *
     * @param icon the icon to show on the left of the window
     */
    private void initIcon(ImageIcon icon)
    {
        // If an icon isn't provided set the application logo icon by default.
        if (icon == null)
            icon = DesktopUtilActivator.getResources()
                .getImage("service.gui.SIP_COMMUNICATOR_LOGO_64x64");

        JLabel iconLabel = new JLabel(icon);

        iconLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel iconPanel = new TransparentPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);

        getContentPane().add(iconPanel, BorderLayout.WEST);
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     * @param isUserNameEditable indicates if the user name is editable
     */
    private void init(boolean isUserNameEditable)
    {
        String title;

        if(windowTitle != null)
            title = windowTitle;
        else
            title = DesktopUtilActivator.getResources().getI18NString(
                "service.gui.AUTHENTICATION_WINDOW_TITLE", new String[]{server});

        String text;
        if(windowText != null)
            text = windowText;
        else
            text = DesktopUtilActivator.getResources().getI18NString(
                        "service.gui.AUTHENTICATION_REQUESTED_SERVER",
                        new String[]{server});

        String uinText;
        boolean showUsernameInDialog = true;
        if(usernameLabelText != null)
        {
            // if username is not editable and username label text is empty
            // we do not want to display it
            if(usernameLabelText.length() == 0
                && !isUserNameEditable)
            {
                showUsernameInDialog = false;
            }
            uinText = usernameLabelText;
        }
        else
            uinText = DesktopUtilActivator.getResources().getI18NString(
                            "service.gui.IDENTIFIER");

        String passText;
        if(passwordLabelText != null)
            passText = passwordLabelText;
        else
            passText = DesktopUtilActivator.getResources().getI18NString(
                            "service.gui.PASSWORD");

        setTitle(title);

        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(
            infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setText(text);
        infoTextArea.setAlignmentX(0.5f);

        JLabel uinLabel = new JLabel(uinText);
        uinLabel.setFont(uinLabel.getFont().deriveFont(Font.BOLD));

        JLabel passwdLabel = new JLabel(passText);
        passwdLabel.setFont(passwdLabel.getFont().deriveFont(Font.BOLD));

        TransparentPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        if(showUsernameInDialog)
            labelsPanel.add(uinLabel);

        labelsPanel.add(passwdLabel);

        TransparentPanel textFieldsPanel
            = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        if(showUsernameInDialog)
            textFieldsPanel.add(uinValue);

        textFieldsPanel.add(passwdField);

        JPanel southFieldsPanel = new TransparentPanel(new GridLayout(1, 2));

        this.rememberPassCheckBox.setOpaque(false);
        this.rememberPassCheckBox.setBorder(null);

        southFieldsPanel.add(rememberPassCheckBox);
        if (signupLink != null && signupLink.length() > 0)
            southFieldsPanel.add(createWebSignupLabel(
                DesktopUtilActivator.getResources().getI18NString(
                    "plugin.simpleaccregwizz.SIGNUP"), signupLink));
        else
            southFieldsPanel.add(new JLabel());

        boolean allowRememberPassword = true;

        String allowRemPassStr = DesktopUtilActivator.getResources().getSettingsString(
                PNAME_ALLOW_SAVE_PASSWORD);
        if(allowRemPassStr != null)
        {
            allowRememberPassword = Boolean.parseBoolean(allowRemPassStr);
        }
        allowRememberPassword = DesktopUtilActivator.getConfigurationService()
            .getBoolean(PNAME_ALLOW_SAVE_PASSWORD, allowRememberPassword);

        setAllowSavePassword(allowRememberPassword);

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

        JPanel mainFieldsPanel = new TransparentPanel(new BorderLayout(0, 10));
        mainFieldsPanel.add(labelsPanel, BorderLayout.WEST);
        mainFieldsPanel.add(textFieldsPanel, BorderLayout.CENTER);
        mainFieldsPanel.add(southFieldsPanel, BorderLayout.SOUTH);

        mainPanel.add(infoTextArea, BorderLayout.NORTH);
        mainPanel.add(mainFieldsPanel, BorderLayout.CENTER);
        mainPanel.add(southEastPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel, BorderLayout.EAST);

        this.loginButton.setName("ok");
        this.cancelButton.setName("cancel");
        if(loginButton.getPreferredSize().width
            > cancelButton.getPreferredSize().width)
            cancelButton.setPreferredSize(loginButton.getPreferredSize());
        else
            loginButton.setPreferredSize(cancelButton.getPreferredSize());

        this.loginButton.setMnemonic(
            DesktopUtilActivator.getResources().getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            DesktopUtilActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

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

        // release the caller that opened the window
        buttonClicked = true;
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
        @SuppressWarnings("serial")
        UIAction act = new UIAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                close(true);
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
    public void setVisible(final boolean isVisible)
    {
        this.setName("AUTHENTICATION");

        if(getOwner() != null)
            setModal(true);

        if(isVisible)
        {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e)
                {
                    removeWindowFocusListener(this);

                    if (uinValue instanceof JTextField &&
                        "".equals(((JTextField) uinValue).getText()))
                    {
                        uinValue.requestFocusInWindow();
                    }
                    else
                        passwdField.requestFocusInWindow();
                }
            });
        }

        super.setVisible(isVisible);

        if(isVisible)
        {
            if(getOwner() != null)
                return;

            synchronized (lock)
            {
                while(!buttonClicked)
                {
                    try
                    {
                        lock.wait();
                    }
                    catch (InterruptedException e)
                    {} // we don't care, just retry
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

    /**
     * Creates the subscribe label.
     * @param linkName the link name
     * @return the newly created subscribe label
     */
    private Component createWebSignupLabel( String linkName,
                                            final String linkURL)
    {
        JLabel subscribeLabel =
            new JLabel("<html><a href=''>"
                + linkName
                + "</a></html>",
                JLabel.RIGHT);

        subscribeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subscribeLabel.setToolTipText(
            DesktopUtilActivator.getResources().getI18NString(
                "plugin.simpleaccregwizz.SPECIAL_SIGNUP"));
        subscribeLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    DesktopUtilActivator.getBrowserLauncher()
                        .openURL(linkURL);
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
     * Returns the icon corresponding to the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which icon
     * we're looking for
     * @return the icon to show on the authentication window
     */
    public static ImageIcon getAuthenticationWindowIcon(
        ProtocolProviderService protocolProvider)
    {
        Image image = null;

        if(protocolProvider != null)
        {
            ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();

            if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_64x64))
                image = ImageUtils.getBytesInImage(
                    protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64));
            else if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_48x48))
                image = ImageUtils.getBytesInImage(
                    protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48));
        }

        if (image != null)
            return new ImageIcon(image);

        return null;
    }

}
