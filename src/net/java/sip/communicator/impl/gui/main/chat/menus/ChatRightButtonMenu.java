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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

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

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.closeMenuItem.setName("service.gui.CLOSE");

        this.copyMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.copyMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.COPY"));

        this.closeMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CLOSE"));

        this.copyMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK));
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

        closeMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    }

    /**
     * Clear resources.
     */
    public void dispose()
    {
        this.chatConvPanel = null;
        copyMenuItem = null;
        closeMenuItem = null;
    }
}
