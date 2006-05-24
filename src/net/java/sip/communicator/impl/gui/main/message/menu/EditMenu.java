/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
/**
 * The EditMenu is the menu which contains cut, copy and paste.
 * @author Yana Stamcheva
 */
public class EditMenu extends JMenu 
    implements ActionListener {

    private JMenuItem cutMenuItem = new JMenuItem(Messages.getString("cut"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

    private JMenuItem copyMenuItem = new JMenuItem(Messages.getString("copy"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem pasteMenuItem = new JMenuItem(
            Messages.getString("paste"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.PASTE_ICON)));

    private ChatWindow chatWindow;

    /**
     * Creates an instance of EditMenu.
     * @param chatWindow The parent ChatWindow.
     */
    public EditMenu(ChatWindow chatWindow) {

        super(Messages.getString("edit"));

        this.chatWindow = chatWindow;

        this.cutMenuItem.setName("cut");
        this.copyMenuItem.setName("copy");
        this.pasteMenuItem.setName("paste");

        this.cutMenuItem.addActionListener(this);
        this.copyMenuItem.addActionListener(this);
        this.pasteMenuItem.addActionListener(this);

        this.add(cutMenuItem);
        this.add(copyMenuItem);
        this.add(pasteMenuItem);
    }

    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String menuItemName = menuItem.getName();

        if (menuItemName.equalsIgnoreCase("cut")) {

            this.chatWindow.getCurrentChatPanel().cut();            
        }
        else if (menuItemName.equalsIgnoreCase("copy")) {
            
            this.chatWindow.getCurrentChatPanel().copy();
        }
        else if (menuItemName.equalsIgnoreCase("paste")) {

            this.chatWindow.getCurrentChatPanel().paste();
        }
    }
}
