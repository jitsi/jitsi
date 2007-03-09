/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>ViewMenu</tt> is a menu in the main application menu bar.
 * 
 * @author Yana Stamcheva
 */
public class ViewMenu
    extends SIPCommMenu 
    implements ActionListener
{

    private Logger logger = Logger.getLogger(ViewMenu.class.getName());
    
    private I18NString hideCallPanelString
        = Messages.getI18NString("hideCallPanel");
        
    private JCheckBoxMenuItem hideCallPanelItem
        = new JCheckBoxMenuItem(hideCallPanelString.getText());
    
    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>ViewMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ViewMenu(MainFrame mainFrame) {

        super(Messages.getI18NString("view").getText());
        
        this.mainFrame = mainFrame;

        ConfigurationService configService
            = GuiActivator.getConfigurationService();
    
        String isCallPanelShown = configService.getString(
            "net.java.sip.communicator.impl.gui.showCallPanel");

        this.hideCallPanelItem.setSelected(
            !new Boolean(isCallPanelShown).booleanValue());
        
        this.add(hideCallPanelItem);
        
        this.hideCallPanelItem.setName("hideCallPanel");
        
        this.hideCallPanelItem.addActionListener(this);
        
        this.setMnemonic(Messages.getI18NString("view").getMnemonic());
        
        this.hideCallPanelItem.setMnemonic(hideCallPanelString.getMnemonic());
        
        this.hideCallPanelItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_H,
                KeyEvent.CTRL_MASK));
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("hideCallPanel")) {
            CallManager callManager = mainFrame.getCallManager();
            
            if (hideCallPanelItem.isSelected()) {
                callManager.hideCallPanel();
            }
            else {
                callManager.showCallPanel();
            }
        }
    }

    public JCheckBoxMenuItem getHideCallPanelItem()
    {
        return hideCallPanelItem;
    }    
}
