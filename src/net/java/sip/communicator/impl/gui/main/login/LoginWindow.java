/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
/**
 * The <tt>LoginWindow</tt> is the window where the user should type his
 * user identifier and password to login.
 * 
 * @author Yana Stamcheva
 */
public class LoginWindow extends JDialog implements ActionListener {

    private JLabel uinLabel = new JLabel(Messages.getString("uin"));

    private JLabel passwdLabel = new JLabel(Messages.getString("passwd"));

    private JComboBox uinComboBox;

    private JPasswordField passwdField = new JPasswordField(15);

    private JButton loginButton = new JButton(Messages.getString("login"));

    private JButton cancelButton = new JButton(Messages.getString("cancel"));

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private LoginWindowBackground backgroundPanel = new LoginWindowBackground();

    private ProtocolProviderFactory providerFactory;

    private LoginManager loginManager;

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     * @param protocolName The name of the protocol.
     * @param providerFactory The provider factory.
     */
    public LoginWindow(MainFrame mainFrame, String protocolName,
            ProtocolProviderFactory providerFactory) {
        super(mainFrame);

        this.providerFactory = providerFactory;

        this.setModal(true);

        this.backgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        this.backgroundPanel.setBorder(
                BorderFactory.createEmptyBorder(60, 5, 5, 5));

        this.getContentPane().setLayout(new BorderLayout());

        this.init();

        this.getContentPane().add(backgroundPanel, BorderLayout.CENTER);

        this.pack();

        this.setSize(370, 240);

        this.setResizable(false);

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.setTitle(Messages.getString("loginWindowTitle", protocolName));

        this.enableKeyActions();
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init() {

        this.uinLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.passwdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.uinComboBox = new JComboBox();

        this.uinComboBox.setEditable(true);

        this.labelsPanel.add(uinLabel);
        this.labelsPanel.add(passwdLabel);

        this.textFieldsPanel.add(uinComboBox);
        this.textFieldsPanel.add(passwdField);

        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);

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
        this.uinComboBox.setOpaque(!transparent);
        this.buttonsPanel.setOpaque(!transparent);
    }

    /**
     * Specifies the window location and shows it.
     */
    public void showWindow() {
        if (!this.isVisible()) {
            this.setWindowLocation();
            this.setVisible(true);
        }
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
     * Returns the provider factory for this login window.
     * @return The provider factory for this login window.
     */
    public ProtocolProviderFactory getProviderFactory() {
        return providerFactory;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the buttons is
     * clicked. When "Login" button is choosen installs a new account from
     * the user input and logs in. 
     */
    public void actionPerformed(ActionEvent e) {

        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("login")) {

            this.dispose();

            AccountID accountID = this.loginManager.installAccount(
                    providerFactory, 
                    uinComboBox.getSelectedItem().toString(), 
                    new String(passwdField.getPassword()));
            
            this.loginManager.login(providerFactory, accountID);
            
        } else {
            this.dispose();
        }
    }

    /**
     * Returns the <tt>LoginManager</tt> for this login window.
     * @return The <tt>LoginManager</tt> for this login window.
     */
    public LoginManager getLoginManager() {
        return loginManager;
    }

    /**
     * Sets the <tt>LoginManager</tt> for this login window.
     * @param loginManager The related LoginManager.
     */
    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    /**
     * The <tt>LoginWindowBackground</tt> is a <tt>JPanel</tt> that overrides
     * the <code>paintComponent</code> method to provide a custom background
     * image for this window. 
     */
    private class LoginWindowBackground extends JPanel {
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.drawImage(ImageLoader.getImage(ImageLoader.LOGIN_WINDOW_LOGO),
                    0, 0, null);

            g2.setColor(new Color(255, 255, 255, 100));

            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Enables the actions when a key is pressed, for now
     * closes the window when esc is pressed.
     */
    private void enableKeyActions() {

        AbstractAction act = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                LoginWindow.this.setVisible(false);
            }
        };

        getRootPane().getActionMap().put("close", act);

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }
}
