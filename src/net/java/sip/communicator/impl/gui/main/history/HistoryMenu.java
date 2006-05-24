/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.history;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;

/**
 * The HistoryMenu is the main menu in the history window.
 * @author Yana Stamcheva
 */
public class HistoryMenu extends JMenu implements ActionListener {

    private JMenuItem emptyMenuItem = new JMenuItem(Messages
            .getString("emptyHistory"));

    private JMenuItem closeMenuItem 
        = new JMenuItem(Messages.getString("close"));

    private JFrame parentWindow;

    /**
     * Creates an instance of HistoryMenu.
     * @param parentWindow The parent window.
     */
    public HistoryMenu(JFrame parentWindow) {

        super(Messages.getString("history"));

        this.parentWindow = parentWindow;

        this.emptyMenuItem.setName("empty");
        this.closeMenuItem.setName("close");

        this.emptyMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.add(emptyMenuItem);
        this.add(closeMenuItem);
    }

    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String menuName = menuItem.getName();

        if (menuName.equalsIgnoreCase("empty")) {
            //TODO: Implement - "empty" history.
        } else if (menuName.equalsIgnoreCase("close")) {
            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();
        }
    }
}
