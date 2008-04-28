/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
/**
 * The <tt>ChatRoomAuthenticationWindow</tt> is the the authentication window
 * for chat rooms that require password.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomAuthenticationWindow
    extends SIPCommFrame
    implements  ActionListener
{
    private JTextArea infoTextArea = new JTextArea();

    private JLabel idLabel = new JLabel(
        Messages.getI18NString("id").getText());

    private JLabel passwdLabel = new JLabel(
        Messages.getI18NString("passwd").getText());

    private JTextField idValue;

    private JPasswordField passwdField = new JPasswordField(15);

    private I18NString okString = Messages.getI18NString("ok");

    private I18NString cancelString = Messages.getI18NString("cancel");

    private JButton loginButton = new JButton(okString.getText());

    private JButton cancelButton = new JButton(cancelString.getText());

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private LoginWindowBackground backgroundPanel;
    
    private ChatRoom chatRoom;

    private MainFrame mainFrame;
    
    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame the parent <tt>MainFrame</tt> window.
     * @param chatRoom the chat room for which we're authenticating
     */
    public ChatRoomAuthenticationWindow(MainFrame mainFrame, ChatRoom chatRoom)
    {
        this.mainFrame = mainFrame;
        
        this.chatRoom = chatRoom;
        
        ProtocolIcon protocolIcon
            = chatRoom.getParentProvider().getProtocolIcon();

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
     * Constructs the <tt>AuthenticationWindow</tt>.
     */
    private void init()
    {
        this.idValue = new JTextField(
            chatRoom.getParentProvider().getAccountID().getUserID());
        
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setEditable(false);
        this.infoTextArea.setText(
            Messages.getI18NString("chatRoomRequiresPassword",
                new String[]{chatRoom.getName()}).getText());

        this.idLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.passwdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.labelsPanel.add(idLabel);
        this.labelsPanel.add(passwdLabel);

        this.textFieldsPanel.add(idValue);
        this.textFieldsPanel.add(passwdField);

        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.add(infoTextArea, BorderLayout.NORTH);
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
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("ok"))
        {
            new Thread()
            {
                public void run()
                {
                    mainFrame.getMultiUserChatManager()
                        .joinChatRoom(chatRoom, idValue.getText(),
                            new String(passwdField.getPassword()).getBytes());
                }
            }.start();
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
                ChatRoomAuthenticationWindow.this.setVisible(false);
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
        super.setVisible(isVisible);

        if(isVisible)
        {
            this.passwdField.requestFocus(); 
        }
    }
}
