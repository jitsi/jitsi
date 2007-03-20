/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.plugin.systray.jdic;


import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;


/**
 * The <tt>StatusSimpleSelector</tt> is a submenu which allow
 * to select a status for a protocol provider which does not 
 * support the OperationSetPresence
 * 
 * @author Nicolas Chamouard
 *
 */
public class StatusSimpleSelector
    extends JMenu
    implements ActionListener
{
	/**
	 * A reference of <tt>Systray</tt>
	 */
    private Systray parentSystray;
	/**
	 * The protocol provider
	 */
    private ProtocolProviderService provider;
    
	/**
     * The logger for this class.
     */
    private Logger logger = Logger.getLogger(
            StatusSimpleSelector.class.getName());
    
    /**
     * The menu item for the online status
     */
    private JMenuItem onlineItem = new JMenuItem(
            Resources.getString("onlineStatus"),
            new ImageIcon(Resources.getImage("sipLogo")));
    /**
     * The menu item for the offline status
     */
    private JMenuItem offlineItem = new JMenuItem(
            Resources.getString("offlineStatus"),
            new ImageIcon(Resources.getImage("sipLogoOffline")));
    
    /**
     * Creates an instance of <tt>StatusSimpleSelector</tt>
     * 
     * @param tray a reference of the parent <tt>Systray</tt>
     * @param pro the protocol provider
     */
    public StatusSimpleSelector(Systray tray,ProtocolProviderService pro)
    {
              
        this.provider = pro;
        this.parentSystray = tray;
  
        /* the parent item */
        
        ImageIcon icon;
        
        if(provider.isRegistered())
        {
            icon = new ImageIcon(Resources.getImage("sipLogo"));
        }
        else
        {
            icon = new ImageIcon(Resources.getImage("sipLogoOffline"));
        }
        
        this.setText(provider.getAccountID().getUserID());
        this.setIcon(icon);
        
        /* the menu itself */
        
        this.onlineItem.addActionListener(this);
        this.offlineItem.addActionListener(this);
        
        this.onlineItem.setName("online");
        this.offlineItem.setName("offline");
        
        this.add(onlineItem);
        this.add(offlineItem);
    }

    /**
     * Change the status of the protocol according to
     * the menu item selected
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {

        JMenuItem menuItem = (JMenuItem) evt.getSource();
        String itemName = menuItem.getName();

        if(itemName.equals("online")) 
        {
            if(!this.provider.isRegistered()) 
            {
                new ProviderRegistration(this.provider,parentSystray).start();
            }
        }
        else 
        {
            if(    !this.provider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERED)
                && !this.provider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERING))
            {
                new ProviderUnRegistration(this.provider).start();
            }
        }
    }
}
