/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import net.java.sip.communicator.impl.gui.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>LoginWindow</tt> is the window where the user should type his
 * user identifier and password to login.
 *
 * @author Yana Stamcheva
 */
public class AuthenticationWindow
    extends SIPCommFrame
    implements  ActionListener,
                ExportedWindow
{

    private JTextArea realmTextArea = new JTextArea();

    private JLabel uinLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.IDENTIFIER"));

    private JLabel passwdLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.PASSWORD"));

    private JComponent uinValue;

    private JPasswordField passwdField = new JPasswordField(15);

    private JButton loginButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    private TransparentPanel textFieldsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JCheckBox rememberPassCheckBox
        = new SIPCommCheckBox(GuiActivator.getResources()
            .getI18NString("service.gui.REMEMBER_PASSWORD"));

    private LoginWindowBackground backgroundPanel;

    private UserCredentials userCredentials;

    private Object lock = new Object();

    private String realm;

    private boolean isUserNameEditable = false;

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame the parent <tt>MainFrame</tt> window.
     * @param protocolProvider the protocol provider.
     * @param realm the realm
     * @param userCredentials the user credentials
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not.
     */
    public AuthenticationWindow(MainFrame mainFrame,
                ProtocolProviderService protocolProvider,
                String realm,
                UserCredentials userCredentials,
                boolean isUserNameEditable)
    {
        this.userCredentials = userCredentials;

        this.realm = realm;

        this.isUserNameEditable = isUserNameEditable;

        ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();

        Image logoImage = null;

        if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_64x64))
            logoImage = ImageLoader.getBytesInImage(
                protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64));
        else if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_48x48))
            logoImage = ImageLoader.getBytesInImage(
                protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48));

        if(logoImage != null)
            backgroundPanel = new LoginWindowBackground(logoImage);
        else
            backgroundPanel = new LoginWindowBackground();

        this.backgroundPanel.setPreferredSize(new Dimension(420, 230));

        this.backgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        this.backgroundPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 5, 5, 5));

        this.getContentPane().setLayout(new BorderLayout());

        this.init();

        this.getContentPane().add(backgroundPanel, BorderLayout.CENTER);

        this.setResizable(false);

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.setTitle(GuiActivator.getResources().getI18NString(
            "service.gui.AUTHENTICATION_WINDOW_TITLE",
            new String[]{protocolProvider.getProtocolName()}));

        this.enableKeyActions();
    }

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame the parent <tt>MainFrame</tt> window.
     * @param protocolProvider the protocol provider.
     * @param realm the realm
     * @param userCredentials the user credentials
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not.
     * @param errorMessage an error message explaining a reason for opening
     * the authentication dialog (when a wrong password was provided, etc.)
     */
    public AuthenticationWindow(MainFrame mainFrame,
                ProtocolProviderService protocolProvider,
                String realm,
                UserCredentials userCredentials,
                boolean isUserNameEditable,
                String errorMessage)
    {
        this(  mainFrame,
                protocolProvider,
                realm,
                userCredentials,
                isUserNameEditable);

        this.realmTextArea.setForeground(Color.RED);
        this.realmTextArea.setText(errorMessage);
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init()
    {
        if(!isUserNameEditable)
            this.uinValue = new JLabel(userCredentials.getUserName());
        else
            this.uinValue = new JTextField(userCredentials.getUserName());

        if(userCredentials.getPassword() != null) {
            this.passwdField.setText(userCredentials.getPassword().toString());
        }

        this.realmTextArea.setEditable(false);
        this.realmTextArea.setOpaque(false);
        this.realmTextArea.setLineWrap(true);
        this.realmTextArea.setWrapStyleWord(true);
        this.realmTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.realmTextArea.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.SECURITY_AUTHORITY_REALM",
                new String[]{realm}));

        this.uinLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.passwdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.labelsPanel.add(uinLabel);
        this.labelsPanel.add(passwdLabel);
        this.labelsPanel.add(new JLabel());

        this.rememberPassCheckBox.setOpaque(false);

        this.textFieldsPanel.add(uinValue);
        this.textFieldsPanel.add(passwdField);
        this.textFieldsPanel.add(rememberPassCheckBox);

        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.add(realmTextArea, BorderLayout.NORTH);
        this.mainPanel.add(labelsPanel, BorderLayout.WEST);
        this.mainPanel.add(textFieldsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.backgroundPanel.add(mainPanel, BorderLayout.CENTER);

        this.loginButton.setName("ok");
        this.cancelButton.setName("cancel");

        this.loginButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.loginButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.getRootPane().setDefaultButton(loginButton);

        this.setTransparent(true);
    }

    /**
     * Sets transparent background to all components in the login window,
     * because of the nonwhite background.
     * @param transparent <code>true</code> to set a transparent background,
     * <code>false</code> otherwise.
     */
    private void setTransparent(boolean transparent) {
        this.mainPanel.setOpaque(!transparent);
        this.labelsPanel.setOpaque(!transparent);
        this.textFieldsPanel.setOpaque(!transparent);
        this.buttonsPanel.setOpaque(!transparent);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the buttons is
     * clicked. When "Login" button is choosen installs a new account from
     * the user input and logs in.
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt) {

        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("ok")) {
            userCredentials.setPassword(
                    passwdField.getPassword());
            userCredentials.setPasswordPersistent(
                    rememberPassCheckBox.isSelected());
        }
        else {
            this.userCredentials = null;
        }

        synchronized (lock) {
            lock.notify();
        }

        this.dispose();
    }

    /**
     * The <tt>LoginWindowBackground</tt> is a <tt>JPanel</tt> that overrides
     * the <code>paintComponent</code> method to provide a custom background
     * image for this window.
     */
    private class LoginWindowBackground extends TransparentPanel
    {
        private Image bgImage;
        public LoginWindowBackground(Image bgImage)
        {
            this.bgImage = bgImage;
        }

        public LoginWindowBackground()
        {
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            if (bgImage != null)
                g2.drawImage(bgImage, 30, 30, null);
        }
    }

    /**
     * Enables the actions when a key is pressed, for now
     * closes the window when esc is pressed.
     */
    private void enableKeyActions() {

        AbstractAction act = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                AuthenticationWindow.this.setVisible(false);
            }
        };

        getRootPane().getActionMap().put("close", act);

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

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
    public void setVisible(boolean isVisible)
    {
        this.setName("AUTHENTICATION");

        super.setVisible(isVisible);

        if(isVisible)
        {
            this.passwdField.requestFocus();

            synchronized (lock) {
                try {
                    lock.wait();
                }
                catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     *
     * @return the <tt>WindowID</tt> of this authentication window.
     */
    public WindowID getIdentifier()
    {
         return ExportedWindow.AUTHENTICATION_WINDOW;
    }

    /**
     * This dialog could not be minimized.
     */
    public void minimize()
    {
    }

    /**
     * This dialog could not be maximized.
     */
    public void maximize()
    {
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings this
     * window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }
    
    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}
}
