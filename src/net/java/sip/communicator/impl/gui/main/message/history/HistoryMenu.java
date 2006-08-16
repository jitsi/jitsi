/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.history;

import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;

/**
 * The <tt>HistoryMenu</tt> is the main menu in the history window.
 * 
 * @author Yana Stamcheva
 */
public class HistoryMenu extends JMenu implements ActionListener {

    private JMenuItem emptyMenuItem = new JMenuItem(Messages
            .getString("emptyHistory"));

    private JMenuItem closeMenuItem 
        = new JMenuItem(Messages.getString("close"));

    private JFrame parentWindow;

    /**
     * Creates an instance of <tt>HistoryMenu</tt>.
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
        
        //disable meni items that are not yet implemented
        this.emptyMenuItem.setEnabled(false);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when user selects an item from the
     * menu. When the close item is selected disposes the window.
     */
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
