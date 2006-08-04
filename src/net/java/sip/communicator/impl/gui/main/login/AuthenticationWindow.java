/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.UserCredentials;
/**
 * The <tt>LoginWindow</tt> is the window where the user should type his
 * user identifier and password to login.
 * 
 * @author Yana Stamcheva
 */
public class AuthenticationWindow extends JDialog implements ActionListener {

    private JTextArea realmTextArea = new JTextArea();

    private JLabel uinLabel = new JLabel(Messages.getString("uin"));

    private JLabel passwdLabel = new JLabel(Messages.getString("passwd"));

    private JLabel uinValueLabel;

    private JPasswordField passwdField = new JPasswordField(15);

    private JButton loginButton = new JButton(Messages.getString("ok"));

    private JButton cancelButton = new JButton(Messages.getString("cancel"));

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private JCheckBox rememberPassCheckBox
        = new JCheckBox(Messages.getString(rememberPassword));

    private LoginWindowBackground backgroundPanel;
    
    private UserCredentials userCredentials;
    
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
        
        super(mainFrame);
        
        this.userCredentials = userCredentials;
        
        this.realm = realm;
        
        this.setModal(true);

        backgroundPanel = new LoginWindowBackground(
                Constants.getProtocolBigIcon(
                        protocolProvider.getProtocolName()));
        
        this.backgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        this.backgroundPanel.setBorder(
                BorderFactory.createEmptyBorder(45, 5, 5, 5));

        this.getContentPane().setLayout(new BorderLayout());

        this.init();

        this.getContentPane().add(backgroundPanel, BorderLayout.CENTER);

        this.pack();

        this.setSize(370, 240);

        this.setResizable(false);

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.setTitle(Messages.getString("authenticationWindowTitle"));

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
        
        this.realmTextArea.setLineWrap(true);
        this.realmTextArea.setWrapStyleWord(true);
        this.realmTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.realmTextArea.setEditable(false);
        this.realmTextArea.setText(
                Messages.getString("securityAuthorityRealm", this.realm));
                
        this.uinLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.passwdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        
        this.labelsPanel.add(uinLabel);
        this.labelsPanel.add(passwdLabel);

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

        this.loginButton.setName("login");
        this.cancelButton.setName("cancel");

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
     * Specifies the window location and shows it.
     */
    public void showWindow() {
        
        this.setWindowLocation();
        this.setVisible(true);
    }

    /**
     * Locates the window in the middle of the screen.
     */
    private void setWindowLocation() {
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width
                - this.getWidth()) / 2;

        int y = (Toolkit.getDefaultToolkit().getScreenSize().height
                - this.getHeight()) / 2;

        this.setLocation(x, y);
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
        } else {
            this.dispose();
        }
    }

    /**
     * The <tt>LoginWindowBackground</tt> is a <tt>JPanel</tt> that overrides
     * the <code>paintComponent</code> method to provide a custom background
     * image for this window. 
     */
    private class LoginWindowBackground extends JPanel {
        private Image bgImage;
        public LoginWindowBackground(Image bgImage) {
            this.bgImage = bgImage;
        }
        
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.drawImage(bgImage, 40,
                (this.getHeight()/2 - bgImage.getHeight(null)/2), null);
            
            g2.drawImage(ImageLoader.getImage(
                    ImageLoader.AUTH_WINDOW_BACKGROUND),
                    0, 0, null);
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
}
