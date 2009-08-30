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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;

import net.java.sip.communicator.util.swing.*;

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
        GuiActivator.getResources().getI18NString("service.gui.IDENTIFIER"));

    private JLabel passwdLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.PASSWORD"));

    private JTextField idValue;

    private JPasswordField passwdField = new JPasswordField(15);

    private JButton loginButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JPanel labelsPanel =
        new TransparentPanel(new GridLayout(0, 1, 8, 8));

    private JPanel textFieldsPanel =
        new TransparentPanel(new GridLayout(0, 1, 8, 8));

    private JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private LoginWindowBackground backgroundPanel;

    private ChatRoomWrapper chatRoom;

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     * @param mainFrame the parent <tt>MainFrame</tt> window.
     * @param chatRoom the chat room for which we're authenticating
     */
    public ChatRoomAuthenticationWindow(ChatRoomWrapper chatRoom)
    {
        this.chatRoom = chatRoom;

        ImageIcon logoImage
            = new ImageIcon(chatRoom.getParentProvider().getImage());

        backgroundPanel = new LoginWindowBackground(logoImage);

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
            new String[]{chatRoom.getParentProvider().getName()}));

        this.enableKeyActions();
    }

    /**
     * Constructs the <tt>AuthenticationWindow</tt>.
     */
    private void init()
    {
        this.idValue = new JTextField(
            chatRoom.getParentProvider().getProtocolProvider()
                .getAccountID().getUserID());

        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setEditable(false);
        this.infoTextArea.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.CHAT_ROOM_REQUIRES_PASSWORD",
                new String[]{chatRoom.getChatRoomName()}));

        this.idLabel.setFont(idLabel.getFont().deriveFont(Font.BOLD));
        this.passwdLabel.setFont(passwdLabel.getFont().deriveFont(Font.BOLD));

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
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("ok"))
        {
            GuiActivator.getUIService().getConferenceChatManager()
                .joinChatRoom(chatRoom, idValue.getText(),
                    new String(passwdField.getPassword()).getBytes());
        }

        this.dispose();
    }

    /**
     * The <tt>LoginWindowBackground</tt> is a <tt>JPanel</tt> that overrides
     * the <code>paintComponent</code> method to provide a custom background
     * image for this window.
     */
    private static class LoginWindowBackground extends JPanel
    {
        private final ImageIcon bgImage;

        public LoginWindowBackground(ImageIcon bgImage)
        {
            this.bgImage = bgImage;
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                Graphics2D g2 = (Graphics2D) g;

                if (bgImage != null)
                    g2.drawImage(bgImage.getImage(), 30, 30, null);

                g2.drawImage(ImageLoader
                    .getImage(ImageLoader.AUTH_WINDOW_BACKGROUND), 0, 0, this
                    .getWidth(), this.getHeight(), null);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Enables the actions when a key is pressed, for now
     * closes the window when esc is pressed.
     */
    private void enableKeyActions() {

        UIAction act = new UIAction() {
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
        this.pack();

        super.setVisible(isVisible);

        if(isVisible)
        {
            this.passwdField.requestFocus(); 
        }
    }
}
