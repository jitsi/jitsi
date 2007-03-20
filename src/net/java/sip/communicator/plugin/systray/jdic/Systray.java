/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.systray.jdic;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.plugin.systray.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jdesktop.jdic.tray.*;
import org.osgi.framework.*;

/**
 * 
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt> 
 * in the system tray using the Jdic library.
 * 
 * 
 * @author Nicolas Chamouard
 *
 */

public class Systray
    implements ServiceListener,
               MessageListener,
               SecurityAuthority
{   
	
	/**
	 * A reference of the <tt>UIservice</tt>
	 */
    private UIService uiService;
    
    /**
     * The icon in the system tray
     */
    private TrayIcon trayIcon;
    
    /**
     * The menu that spring with a right click
     */
    private TrayMenu menu;
    
    /**
     * this table contains an up-to-date list of all providers
     */
    private Hashtable protocolProviderTable = new Hashtable();
    
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(Systray.class.getName());
        
    /**
     * Creates an instance of <tt>Systray</tt>.
     * @param service a reference of the current <tt>UIservice</tt>
     */
    public Systray(UIService service)
    {    
        this.uiService = service;

        uiService.setExitOnMainWindowClose(false);
        
        BundleContext bc = SystrayActivator.bundleContext;
          
        /* we fill the protocolProviderTable with all
         * running protocol providers at the start of
         * the bundle
         */
        
        bc.addServiceListener(this);
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
        	logger.error("Error while retrieving service refs", ex);
        	return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);
                
                this.protocolProviderTable.put(
                    provider.getAccountID(),
                    provider);
                handleProviderAdded(provider);

            }
        }
        
        /* the system tray icon itself */
        
        SystemTray tray = SystemTray.getDefaultSystemTray();
        
        ImageIcon logoIcon = new ImageIcon(Resources.getImage("trayIcon"));
        menu = new TrayMenu(uiService,this);
        
        trayIcon = new TrayIcon(
            logoIcon, "SIP Communicator", menu);
        trayIcon.setIconAutoSize(true);
        
        trayIcon.addActionListener(new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) 
            {

                if(uiService.isVisible())
                {
                    uiService.setVisible(false);
                }
                else
                {
                    uiService.setVisible(true);
                }
            }
        });
        
        tray.addTrayIcon(trayIcon);    
    }
    
    
    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator getProtocolProviders()
    {    
        return this.protocolProviderTable.values().iterator();   
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }
    
    /**
     * Display in a balloon the newly received message
     * @param evt the event containing the message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        
        ApplicationWindow window = uiService.getChatWindow(evt.getSourceContact());
        
        if(window.isWindowVisible())
        {
        
        String contactName = evt.getSourceContact().getDisplayName();
        String message = evt.getSourceMessage().getContent();
        this.trayIcon.displayMessage(contactName,message,TrayIcon.INFO_MESSAGE_TYPE);
        }
    }
    
    /**
     * When a service ist registered or unregistered, we update
     * the provider tables and add/remove listeners (if it supports
     * BasicInstantMessenging implementation)
     *
     * @param event ServiceEvent
     */
    public void serviceChanged(ServiceEvent event)
    {
    	
        ProtocolProviderService provider = (ProtocolProviderService)
            SystrayActivator.bundleContext.getService(event.getServiceReference());
        
        if (event.getType() == ServiceEvent.REGISTERED){
            protocolProviderTable.put(provider.getAccountID(),provider);
            handleProviderAdded(provider);
            
        }
        if (event.getType() == ServiceEvent.UNREGISTERING){
           protocolProviderTable.remove(provider.getAccountID());
           handleProviderRemoved(provider);
        }

    }
    
    /**
     * Checks if the provider has an implementation 
     * of OperationSetBasicInstantMessaging and
     * if so add a listerner to it
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
        = (OperationSetBasicInstantMessaging) provider
        .getSupportedOperationSets().get(
            OperationSetBasicInstantMessaging.class.getName());
        
        if(opSetIm != null)
            opSetIm.addMessageListener(this);    
        
    }
    
    /**
     * Checks if the provider has an implementation 
     * of OperationSetBasicInstantMessaging and
     * if so remove its listerner
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
        = (OperationSetBasicInstantMessaging) provider
        .getSupportedOperationSets().get(
            OperationSetBasicInstantMessaging.class.getName());
        
        if(opSetIm != null)
            opSetIm.removeMessageListener(this);
        
    }
    
    /**
     * Used to login to the protocol providers
     * @param realm the realm that the credentials are needed for
     * @param defaultValues the values to propose the user by default
     * @return The Credentials associated with the speciefied realm
     */
    public UserCredentials obtainCredentials(String realm, UserCredentials defaultValues)
    {
    	
    	return null;	
    }

    
}
