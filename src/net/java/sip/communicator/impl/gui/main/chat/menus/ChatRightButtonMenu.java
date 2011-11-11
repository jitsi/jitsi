/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatRightButtonMenu</tt> appears when the user makes a right button
 * click on the chat window conversation area (where sent and received messages
 * are displayed).
 *  
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                Skinnable
{
    private ChatConversationPanel chatConvPanel;

    private JMenuItem copyMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.COPY"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem saveMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.SAVE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SAVE_ICON)));

    private JMenuItem printMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.PRINT"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.PRINT_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CLOSE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    /**
     * Creates an instance of <tt>ChatRightButtonMenu</tt>.
     *  
     * @param chatConvPanel The conversation panel, where this menu will apear.
     */
    public ChatRightButtonMenu(ChatConversationPanel chatConvPanel)
    {
        super();

        this.chatConvPanel = chatConvPanel;

        this.init();
    }

    /**
     * Initializes the menu with all menu items.
     */
    private void init()
    {
        this.add(copyMenuItem);

        this.addSeparator();

        this.add(saveMenuItem);
        this.add(printMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.saveMenuItem.setName("save");
        this.printMenuItem.setName("print");
        this.closeMenuItem.setName("service.gui.CLOSE");

        this.copyMenuItem.addActionListener(this);
        this.saveMenuItem.addActionListener(this);
        this.printMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.copyMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.COPY"));
        this.saveMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.SAVE"));
        this.printMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.PRINT"));
        this.closeMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CLOSE"));

        this.copyMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK));

        this.saveMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_MASK));

        this.printMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_R,
                KeyEvent.CTRL_MASK));

        // Disable all menu items that do nothing.
        this.saveMenuItem.setEnabled(false);
        this.printMenuItem.setEnabled(false);
    }

    /**
     * Disables the copy item.
     */
    public void disableCopy() {
        this.copyMenuItem.setEnabled(false);
    }

    /**
     * Enables the copy item.
     */
    public void enableCopy() {
        this.copyMenuItem.setEnabled(true);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("copy"))
        {
            this.chatConvPanel.copyConversation();
        }
        else if (itemText.equalsIgnoreCase("save"))
        {
            //TODO: Implement save to file.
        }
        else if (itemText.equalsIgnoreCase("print"))
        {
          //TODO: Implement print.
        }
        else if (itemText.equalsIgnoreCase("service.gui.CLOSE"))
        {
            Window window = this.chatConvPanel
                .getChatContainer().getConversationContainerWindow();

            window.setVisible(false);
            window.dispose();
        }
    }

    /**
     * Reloads menu icons.
     */
    public void loadSkin()
    {
        copyMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.COPY_ICON)));

        saveMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SAVE_ICON)));

        printMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.PRINT_ICON)));

        closeMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    }
}
