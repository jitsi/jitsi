/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>FileMenu</tt> is the menu in the chat window menu bar that contains
 * save, print and close.
 * 
 * @author Yana Stamcheva
 */
public class FileMenu extends SIPCommMenu 
    implements ActionListener
{
    private I18NString myChatRoomsString
        = Messages.getI18NString("myChatRooms");

    private I18NString closeString = Messages.getI18NString("close");

    private JMenuItem myChatRoomsItem = new JMenuItem(
        myChatRoomsString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CHAT_ROOM_16x16_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        closeString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    private ChatWindow parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(ChatWindow parentWindow) {

        super(Messages.getI18NString("file").getText());

        this.setOpaque(false);

        this.parentWindow = parentWindow;

        this.setForeground(new Color(
            GuiActivator.getResources()
                .getColor("service.gui.CHAT_MENU_FOREGROUND")));

        this.setMnemonic(Messages.getI18NString("file").getMnemonic());

        this.add(myChatRoomsItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.myChatRoomsItem.setName("myChatRooms");
        this.closeMenuItem.setName("close");

        this.myChatRoomsItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.myChatRoomsItem.setMnemonic(myChatRoomsString.getMnemonic());
        this.closeMenuItem.setMnemonic(closeString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("myChatRooms"))
        {
            ChatRoomListDialog chatRoomsDialog
                = new ChatRoomListDialog(
                    GuiActivator.getUIService().getMainFrame());

            chatRoomsDialog.setPreferredSize(new Dimension(500, 400));
            chatRoomsDialog.setVisible(true);
        }
        else if (itemText.equalsIgnoreCase("close"))
        {
            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();
        }
    }
}
