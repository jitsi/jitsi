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
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.ConfigurationManager;
import net.java.sip.communicator.util.swing.*;
/**
 * The <tt>OptionMenu</tt> is a menu in the chat window menu bar.
 *
 * @author Damien Roth
 */
public class OptionsMenu
    extends SIPCommMenu
    implements ActionListener
{
    private ChatWindow chatWindow = null;
    
    private JCheckBoxMenuItem viewToolBar = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_TOOLBAR"));
    private static final String ACTCMD_VIEW_TOOLBAR = "ACTCMD_VIEW_TOOLBAR";
    
    private JCheckBoxMenuItem viewStyleBar = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_STYLEBAR"));
    private static final String ACTCMD_VIEW_STYLEBAR = "ACTCMD_VIEW_STYLEBAR";
    
    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public OptionsMenu(ChatWindow chatWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.TOOLS"));
        this.chatWindow = chatWindow;

        this.setOpaque(false);

        this.setForeground(new Color(
            GuiActivator.getResources()
                .getColor("service.gui.CHAT_MENU_FOREGROUND")));

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.TOOLS"));
        
        this.viewToolBar.setActionCommand(ACTCMD_VIEW_TOOLBAR);
        this.viewToolBar.addActionListener(this);
        this.add(this.viewToolBar);
        
        this.viewStyleBar.setActionCommand(ACTCMD_VIEW_STYLEBAR);
        this.viewStyleBar.addActionListener(this);
        this.add(this.viewStyleBar);
        
        initValues();
    }
    
    private void initValues()
    {
        this.viewToolBar.setSelected(
            ConfigurationManager.isChatToolbarVisible());
        this.viewStyleBar.setSelected(
            ConfigurationManager.isChatStylebarVisible());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        String action = e.getActionCommand();
        
        if (action.equals(ACTCMD_VIEW_TOOLBAR))
        {
            this.chatWindow.setToolbarVisible(this.viewToolBar.isSelected());
            ConfigurationManager
                .setChatToolbarVisible(this.viewToolBar.isSelected());
        }
        else if (action.equals(ACTCMD_VIEW_STYLEBAR))
        {
            this.chatWindow.setStylebarVisible(this.viewStyleBar.isSelected());
            ConfigurationManager
                .setChatStylebarVisible(this.viewStyleBar.isSelected());
        }
    }
}
