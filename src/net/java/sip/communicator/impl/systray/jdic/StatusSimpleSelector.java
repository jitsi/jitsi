/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.impl.systray.jdic;


import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
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
    private SystrayServiceJdicImpl parentSystray;
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
            Resources.getString("onlineStatus"));
    /**
     * The menu item for the offline status
     */
    private JMenuItem offlineItem = new JMenuItem(
            Resources.getString("offlineStatus"));
    
    /**
     * Creates an instance of <tt>StatusSimpleSelector</tt>
     * 
     * @param jdicSystray a reference of the parent <tt>Systray</tt>
     * @param protocolProvider the protocol provider
     */
    public StatusSimpleSelector(SystrayServiceJdicImpl jdicSystray,
                                ProtocolProviderService protocolProvider)
    {

        this.provider = protocolProvider;
        this.parentSystray = jdicSystray;

        /* the parent item */

        ImageIcon icon;

        if(provider.isRegistered())
        {
            icon = new ImageIcon(protocolProvider.getProtocolIcon()
                .getIcon(ProtocolIcon.ICON_SIZE_16x16));
        }
        else
        {
            icon = new ImageIcon(LightGrayFilter.createDisabledImage(
                new ImageIcon(protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16)).getImage()));
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
                new ProviderRegistration(provider).start();
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

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        logger.trace("Systray update status for provider: "
            + provider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        this.setIcon(new ImageIcon(presenceStatus.getStatusIcon()));
    }
}
