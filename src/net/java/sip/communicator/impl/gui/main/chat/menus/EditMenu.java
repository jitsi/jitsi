/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>EditMenu</tt> is the menu in the chat window menu bar, which contains
 * cut, copy and paste.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class EditMenu
    extends SIPCommMenu
    implements  ActionListener,
                Skinnable
{
    private JMenuItem fontDialogMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.FONT"));

    private JMenuItem cutMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CUT"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

    private JMenuItem copyMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.COPY"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem pasteMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.PASTE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.PASTE_ICON)));

    private ChatWindow chatWindow;

    /**
     * Creates an instance of <tt>EditMenu</tt>.
     *
     * @param chatWindow The parent <tt>ChatWindow</tt>.
     */
    public EditMenu(ChatWindow chatWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.EDIT"));

        this.chatWindow = chatWindow;

        this.cutMenuItem.setName("cut");
        this.copyMenuItem.setName("copy");
        this.pasteMenuItem.setName("paste");
        this.fontDialogMenuItem.setName("font");

        this.cutMenuItem.addActionListener(this);
        this.copyMenuItem.addActionListener(this);
        this.pasteMenuItem.addActionListener(this);
        this.fontDialogMenuItem.addActionListener(this);

        this.add(cutMenuItem);
        this.add(copyMenuItem);
        this.add(pasteMenuItem);
        this.addSeparator();
        this.add(fontDialogMenuItem);

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.EDIT"));
        this.cutMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CUT"));
        this.copyMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.COPY"));
        this.pasteMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.PASTE"));
        this.fontDialogMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.FONT"));

        this.cutMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_X,
                KeyEvent.CTRL_MASK));

        this.copyMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK));

        this.pasteMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.CTRL_MASK));
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String menuItemName = menuItem.getName();

        if (menuItemName.equalsIgnoreCase("cut"))
        {
            this.chatWindow.getCurrentChat().cut();
        }
        else if (menuItemName.equalsIgnoreCase("copy"))
        {
            this.chatWindow.getCurrentChat().copy();
        }
        else if (menuItemName.equalsIgnoreCase("paste"))
        {
            this.chatWindow.getCurrentChat().paste();
        }
        else if (menuItemName.equalsIgnoreCase("font"))
        {
            this.chatWindow.getCurrentChat().getChatWritePanel()
                .getEditTextToolBar().showFontChooserDialog();
        }
    }

    /**
     * Reloads menu icons.
     */
    public void loadSkin()
    {
        cutMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CUT_ICON)));

        copyMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.COPY_ICON)));

        pasteMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.PASTE_ICON)));
    }
}
