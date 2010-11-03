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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>JoinChatRoomDialog</tt> is the dialog containing the form for joining
 * a chat room.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class JoinChatRoomDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    private SearchChatRoomPanel searchPanel;

    private JButton joinButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.JOIN"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
        .getImage(ImageLoader.ADD_CONTACT_CHAT_ICON)));

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private ChatRoomProviderWrapper chatRoomProvider;

    /**
     * Creates an instance of <tt>JoinChatRoomDialog</tt>.
     * 
     * @param provider the <tt>ChatRoomProviderWrapper</tt>, which will be the chat
     * server for the newly created chat room
     */
    public JoinChatRoomDialog(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;

        this.searchPanel = new SearchChatRoomPanel(chatRoomProvider);

        this.setTitle(
            GuiActivator.getResources()
                .getI18NString("service.gui.JOIN_CHAT_ROOM"));

        this.getRootPane().setDefaultButton(joinButton);
        this.joinButton.setName("join");
        this.cancelButton.setName("cancel");

        this.joinButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.JOIN"));

        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        this.joinButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(joinButton);
        this.buttonsPanel.add(cancelButton);

        this.getContentPane().add(iconLabel, BorderLayout.WEST);
        this.getContentPane().add(searchPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice creates
     * the desired chat room in a separate thread.
     * <br>
     * Note: No specific properties are set right now!
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("service.gui.JOIN"))
        {
            GuiActivator.getUIService().getConferenceChatManager()
                .joinChatRoom(searchPanel.getChatRoomName(), chatRoomProvider);
        }
        this.dispose();
    }

    /**
     * When escape is pressed clicks the cancel button programatically.
     *
     * @param escaped indicates if the window was closed by pressing the Esc
     * key
     */
    protected void close(boolean escaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Shows this dialog. And requests the current focus in the chat room name
     * field.
     */
    public void showDialog()
    {
        this.setVisible(true);

        searchPanel.requestFocusInField();
    }

    /**
     * Reloads icon label.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(ImageLoader
        .getImage(ImageLoader.ADD_CONTACT_CHAT_ICON)));
    }
}
