/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 */
public class HelpMenu
    extends JMenu
    implements ActionListener
{

    private Logger logger = Logger.getLogger(HelpMenu.class.getName());

    private I18NString aboutString = Messages.getI18NString("about");

    private JMenuItem aboutItem = new JMenuItem(aboutString.getText());

    private ChatWindow chatWindow;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public HelpMenu(ChatWindow chatWindow) {

        super(Messages.getI18NString("help").getText());

        this.chatWindow = chatWindow;

        this.add(aboutItem);

        this.aboutItem.setName("about");

        this.aboutItem.addActionListener(this);

        this.aboutItem.setMnemonic(aboutString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param evt the ActionEvent that has just occurred.
     */
    public void actionPerformed(ActionEvent evt) {

        JMenuItem menuItem = (JMenuItem) evt.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("about")) {
            AboutWindow aboutWindow = new AboutWindow(chatWindow);

            aboutWindow.setVisible(true);
        }
    }
}
