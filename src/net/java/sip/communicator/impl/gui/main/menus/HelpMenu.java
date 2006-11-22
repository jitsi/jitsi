/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
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
    
    private JMenuItem aboutItem
        = new JMenuItem(Messages.getString("about"));
    
    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt>.
     */
    public HelpMenu(MainFrame mainFrame) {

        super(Messages.getString("help"));
        
        this.mainFrame = mainFrame;
        
        this.add(aboutItem);
        
        this.aboutItem.setName("about");
        
        this.aboutItem.addActionListener(this);
        
        this.setMnemonic(Messages.getString("help").charAt(0));
        
        this.aboutItem.setMnemonic(
                Messages.getString("about").charAt(0));        
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("about")) {
            AboutWindow aboutWindow = new AboutWindow(mainFrame);
            
            aboutWindow.setVisible(true);
        }
    }
}
