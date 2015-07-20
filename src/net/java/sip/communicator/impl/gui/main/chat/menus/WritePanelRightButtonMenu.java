/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>WritePanelRightButtonMenu</tt> appears when the user makes a right
 * button click on the chat window write area (where user types messages).
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class WritePanelRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                Skinnable
{
    private ChatContainer chatContainer;

    private JMenuItem cutMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CUT"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

    private JMenuItem copyMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.COPY"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem pasteMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.PASTE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.PASTE_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CLOSE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    /**
     * Creates an instance of <tt>WritePanelRightButtonMenu</tt>.
     *
     * @param chatContainer The window owner of this popup menu.
     */
    public WritePanelRightButtonMenu(ChatContainer chatContainer)
    {
        super();

        this.chatContainer = chatContainer;

        this.init();
    }

    /**
     * Initializes this menu with menu items.
     */
    private void init()
    {
        this.add(copyMenuItem);
        this.add(cutMenuItem);
        this.add(pasteMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.cutMenuItem.setName("cut");
        this.pasteMenuItem.setName("paste");
        this.closeMenuItem.setName("service.gui.CLOSE");

        this.copyMenuItem.addActionListener(this);
        this.cutMenuItem.addActionListener(this);
        this.pasteMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.copyMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.COPY"));
        this.cutMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CUT"));
        this.pasteMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.PASTE"));
        this.closeMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CLOSE"));

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
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("cut"))
        {
            this.chatContainer.getCurrentChat().cut();
        }
        else if (itemText.equalsIgnoreCase("copy"))
        {
            this.chatContainer.getCurrentChat().copyWriteArea();
        }
        else if (itemText.equalsIgnoreCase("paste"))
        {
            this.chatContainer.getCurrentChat().paste();
        }
        else if (itemText.equalsIgnoreCase("service.gui.CLOSE"))
        {
            this.chatContainer.getFrame().setVisible(false);
            this.chatContainer.getFrame().dispose();
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

        closeMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    }

    /**
     * Provides a popup menu with custom entries followed by default
     * operation entries ( copy, paste ,close)
     *
     * @param entries custom menu entries to be added
     * @return right click menu
     */
    public JPopupMenu makeMenu(List <JMenuItem> entries) {

        JPopupMenu rightMenu = new JPopupMenu();

        for(JMenuItem entry : entries) {
            rightMenu.add(entry);
        }

        if(!entries.isEmpty()) rightMenu.addSeparator();

        rightMenu.add(copyMenuItem);
        rightMenu.add(cutMenuItem);
        rightMenu.add(pasteMenuItem);

        rightMenu.addSeparator();

        rightMenu.add(closeMenuItem);

        return rightMenu;
    }

    /**
     * Clear resources.
     */
    public void dispose()
    {
        chatContainer = null;
        cutMenuItem = null;
        copyMenuItem = null;
        pasteMenuItem = null;
        closeMenuItem = null;
    }
}
