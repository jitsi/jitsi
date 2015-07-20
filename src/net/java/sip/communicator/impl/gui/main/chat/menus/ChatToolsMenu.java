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
import net.java.sip.communicator.impl.gui.main.menus.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>OptionMenu</tt> is a menu in the chat window menu bar.
 *
 * @author Damien Roth
 * @author Yana Stamcheva
 */
public class ChatToolsMenu
    extends ToolsMenu
    implements ActionListener
{
    private ChatWindow chatWindow = null;

    private JCheckBoxMenuItem viewToolBar = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_TOOLBAR"));
    private static final String ACTCMD_VIEW_TOOLBAR = "ACTCMD_VIEW_TOOLBAR";

    private JCheckBoxMenuItem viewSmileys = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_SMILEYS"));
    private static final String ACTCMD_VIEW_SMILEYS = "ACTCMD_VIEW_SMILEYS";

    private JCheckBoxMenuItem chatSimpleTheme = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.VIEW_SIMPLE_CHAT_THEME"));
    private static final String ACTCMD_VIEW_SIMPLE_THEME
        = "ACTCMD_VIEW_SIMPLE_THEME";

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public ChatToolsMenu(ChatWindow chatWindow)
    {
        super(true);

        this.chatWindow = chatWindow;

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.TOOLS"));

        // Add a separator before adding the specific chat items.
        this.addSeparator();

        this.viewToolBar.setActionCommand(ACTCMD_VIEW_TOOLBAR);
        this.viewToolBar.addActionListener(this);
        this.add(viewToolBar);

        this.viewSmileys.setActionCommand(ACTCMD_VIEW_SMILEYS);
        this.viewSmileys.addActionListener(this);
        this.add(viewSmileys);

        this.chatSimpleTheme.setActionCommand(ACTCMD_VIEW_SIMPLE_THEME);
        this.chatSimpleTheme.addActionListener(this);
        this.add(chatSimpleTheme);

        initValues();
    }

    /**
     * Initializes the values of menu items.
     */
    private void initValues()
    {
        this.viewToolBar.setSelected(
            ConfigurationUtils.isChatToolbarVisible());

        this.viewSmileys.setSelected(
            ConfigurationUtils.isShowSmileys());

        this.chatSimpleTheme.setSelected(
            ConfigurationUtils.isChatSimpleThemeEnabled());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);

        String action = e.getActionCommand();

        if (action.equals(ACTCMD_VIEW_TOOLBAR))
        {
            this.chatWindow.setToolbarVisible(viewToolBar.isSelected());
            ConfigurationUtils
                .setChatToolbarVisible(viewToolBar.isSelected());
        }
        else if (action.equals(ACTCMD_VIEW_SMILEYS))
        {
            ConfigurationUtils.setShowSmileys(viewSmileys.isSelected());
        }
        else if (action.equals(ACTCMD_VIEW_SIMPLE_THEME))
        {
            ConfigurationUtils.setChatSimpleThemeEnabled(
                chatSimpleTheme.isSelected());

            List<ChatPanel> currentChats = chatWindow.getChats();
            if (currentChats != null)
            {
                Iterator<ChatPanel> chatsIter = currentChats.iterator();
                while (chatsIter.hasNext())
                {
                    chatsIter.next().loadSkin();
                }
            }
        }
    }
}
