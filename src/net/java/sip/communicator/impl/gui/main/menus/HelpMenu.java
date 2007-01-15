/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 * 
 * @author Yana Stamcheva
 */
public class HelpMenu
    extends SIPCommMenu 
    implements ActionListener
{

    private Logger logger = Logger.getLogger(HelpMenu.class.getName());
    
    private I18NString aboutString = Messages.getI18NString("about");
    
    private JMenuItem aboutItem
        = new JMenuItem(aboutString.getText());
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param mainFrame the parent window
     */
    public HelpMenu (MainFrame mainFrame)
    {

        super(Messages.getI18NString("help").getText());
        
        this.mainFrame = mainFrame;
        
        this.add(aboutItem);
        
        this.aboutItem.setName("about");
        
        this.aboutItem.addActionListener(this);
        
        this.setMnemonic(Messages.getI18NString("help").getMnemonic());
        
        this.aboutItem.setMnemonic(aboutString.getMnemonic());        
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
