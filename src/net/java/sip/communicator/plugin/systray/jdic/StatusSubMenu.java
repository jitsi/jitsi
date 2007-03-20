/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.plugin.systray.jdic;


import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>StatusSubMenu</tt> provides a menu which allow
 * to select the status for each of the protocol providers
 * registered when the menu appears
 * 
 * @author Nicolas Chamouard
 *
 */

public class StatusSubMenu
extends JMenu
//implements ActionListener
{

	/**
	 * A reference of <tt>Systray</tt>
	 */
    private Systray parentSystray;
    
    /**
     * Creates an instance of <tt>StatusSubMenu</tt>.
     * @param tray a reference of the parent <tt>Systray</tt>
     */
    public StatusSubMenu(Systray tray)
    {
        
        parentSystray = tray;
        
        this.setText(Resources.getString("setStatus"));
        this.setIcon(
                new ImageIcon(Resources.getImage("statusMenuIcon")));
        
        /* makes the menu look better */
        this.setPreferredSize(new Dimension(28, 24));
        
        update();
        
    }
    
    /**
     * Updates the Menu by retrieving provider informations
     */
    public void update()
    {
        
        this.removeAll();
        
        Iterator it=parentSystray.getProtocolProviders();
        
        while(it.hasNext()){
            ProtocolProviderService provider = 
                (ProtocolProviderService) it.next();
            
            Map supportedOperationSets
            = provider.getSupportedOperationSets();
            
            OperationSetPresence presence = (OperationSetPresence)
            supportedOperationSets.get(OperationSetPresence.class.getName());
            
            if (presence == null)
            {  
                StatusSimpleSelector s = 
                    new StatusSimpleSelector(parentSystray,provider);
                
                this.add(s);
            }
            else
            {    
                StatusSelector s = 
                    new StatusSelector(parentSystray,provider,presence);
                
                this.add(s);        
            }
        }
    }
        
    
    
}