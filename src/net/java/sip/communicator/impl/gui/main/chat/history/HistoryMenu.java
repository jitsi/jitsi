/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>HistoryMenu</tt> is the main menu in the history window.
 * 
 * @author Yana Stamcheva
 */
public class HistoryMenu
    extends SIPCommMenu
    implements ActionListener
{
    /**
     * The empty history menu item.
     */
    private JMenuItem emptyMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.EMPTY_HISTORY"));

    /**
     * The close menu item.
     */
    private JMenuItem closeMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CLOSE"));

    private JFrame parentWindow;

    /**
     * Creates an instance of <tt>HistoryMenu</tt>.
     * @param parentWindow The parent window.
     */
    public HistoryMenu(JFrame parentWindow) {

        super(GuiActivator.getResources().getI18NString("service.gui.HISTORY"));

        this.parentWindow = parentWindow;

        this.emptyMenuItem.setName("empty");
        this.closeMenuItem.setName("service.gui.CLOSE");

        this.emptyMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
                "service.gui.EMPTY_HISTORY"));
        this.closeMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CLOSE"));

        this.emptyMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.add(emptyMenuItem);
        this.add(closeMenuItem);

        //disable meni items that are not yet implemented
        this.emptyMenuItem.setEnabled(false);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when user selects an item from the
     * menu. When the close item is selected disposes the window.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String menuName = menuItem.getName();

        if (menuName.equalsIgnoreCase("service.gui.CLOSE"))
        {
            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();
        }
    }
}
