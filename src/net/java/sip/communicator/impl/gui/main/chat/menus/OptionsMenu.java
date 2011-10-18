/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.menus;

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
 * @author Yana Stamcheva
 */
public class OptionsMenu
    extends SIPCommMenu
    implements ActionListener
{
    private ChatWindow chatWindow = null;

    private JCheckBoxMenuItem viewToolBar = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_TOOLBAR"));
    private static final String ACTCMD_VIEW_TOOLBAR = "ACTCMD_VIEW_TOOLBAR";

    private JCheckBoxMenuItem viewSmileys = new JCheckBoxMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.VIEW_SMILEYS"));
    private static final String ACTCMD_VIEW_SMILEYS = "ACTCMD_VIEW_SMILEYS";

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public OptionsMenu(ChatWindow chatWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.TOOLS"));
        this.chatWindow = chatWindow;

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.TOOLS"));

        this.viewToolBar.setActionCommand(ACTCMD_VIEW_TOOLBAR);
        this.viewToolBar.addActionListener(this);
        this.add(this.viewToolBar);

        this.viewSmileys.setActionCommand(ACTCMD_VIEW_SMILEYS);
        this.viewSmileys.addActionListener(this);
        this.add(this.viewSmileys);

        initValues();
    }

    /**
     * Initializes the values of menu items.
     */
    private void initValues()
    {
        this.viewToolBar.setSelected(
            ConfigurationManager.isChatToolbarVisible());

        this.viewSmileys.setSelected(
            ConfigurationManager.isShowSmileys());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        String action = e.getActionCommand();

        if (action.equals(ACTCMD_VIEW_TOOLBAR))
        {
            this.chatWindow.setToolbarVisible(viewToolBar.isSelected());
            ConfigurationManager
                .setChatToolbarVisible(viewToolBar.isSelected());
        }
        else if (action.equals(ACTCMD_VIEW_SMILEYS))
        {
            ConfigurationManager.setShowSmileys(viewSmileys.isSelected());
        }
    }
}
