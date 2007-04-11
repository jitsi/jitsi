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

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
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
        Messages.getI18NString("uin").getText());

    private JLabel passwdLabel = new JLabel(
        Messages.getI18NString("passwd").getText());

    private JLabel uinValueLabel;

    private JPasswordField passwdField = new JPasswordField(15);

    private I18NString okString = Messages.getI18NString("ok");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton loginButton = new JButton(okString.getText());

    private JButton cancelButton = new JButton(cancelString.getText());

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JCheckBox rememberPassCheckBox
        = new JCheckBox(Messages.getI18NString("rememberPassword").getText());

    private LoginWindowBackground backgroundPanel;

    private UserCredentials userCredentials;

    private Object lock = new Object();
    
    private String realm;

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame the parent <tt>MainFrame</tt> window.
     * @param protocolProvider the protocol provider.
     * @param realm the realm
     * @param userCredentials the user credentials
     */
    public AuthenticationWindow(MainFrame mainFrame,
                ProtocolProviderService protocolProvider,
                String realm,
                UserCredentials userCredentials) {

        this.userCredentials = userCredentials;

        this.realm = realm;

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

        this.setTitle(
            Messages.getI18NString("authenticationWindowTitle").getText());

        this.enableKeyActions();
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init() {

        this.uinValueLabel = new JLabel(userCredentials.getUserName());
        if(userCredentials.getPassword() != null) {
            this.passwdField.setText(userCredentials.getPassword().toString());
        }

        this.realmTextArea.setOpaque(false);
        this.realmTextArea.setLineWrap(true);
        this.realmTextArea.setWrapStyleWord(true);
        this.realmTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.realmTextArea.setEditable(false);
        this.realmTextArea.setText(
            Messages.getI18NString("securityAuthorityRealm",
                new String[]{realm}).getText());

        this.uinLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.passwdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.labelsPanel.add(uinLabel);
        this.labelsPanel.add(passwdLabel);
        this.labelsPanel.add(new JLabel());

        this.rememberPassCheckBox.setOpaque(false);
        
        this.textFieldsPanel.add(uinValueLabel);
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
        
        this.loginButton.setMnemonic(okString.getMnemonic());
        this.cancelButton.setMnemonic(cancelString.getMnemonic());

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
     */
    public void actionPerformed(ActionEvent e) {

        JButton button = (JButton) e.getSource();
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
    private class LoginWindowBackground extends JPanel
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

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            if(bgImage != null)
                g2.drawImage(bgImage, 30, 30, null);

            g2.drawImage(ImageLoader.getImage(
                    ImageLoader.AUTH_WINDOW_BACKGROUND),
                    0, 0, this.getWidth(), this.getHeight(), null);
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
     * @return the result code, which shows what was the choice of the user
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
}
