/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>StatusPanel</tt> is the place where the user can see and change
 * its status for all registered protocols. 
 * 
 * @author Yana Stamcheva
 */
public class StatusPanel
    extends JMenuBar
    implements ComponentListener
{

    private Hashtable protocolStatusCombos = new Hashtable();

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>StatusPanel</tt>.
     * @param mainFrame The main application window.
     */
    public StatusPanel(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                Constants.MOVER_START_COLOR));
    }

    /**
     * Creates the selector box, containing all protocol statuses, adds it to 
     * the StatusPanel and refreshes the panel.
     * 
     * @param protocolProvider The protocol provider.
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        StatusSelectorBox protocolStatusCombo;
        
        int providerIndex = this.mainFrame.getProviderIndex(protocolProvider); 
        if(mainFrame.getProtocolPresence(protocolProvider) != null) {
            protocolStatusCombo
                = new PresenceStatusSelectorBox(
                        this.mainFrame, protocolProvider,
                        providerIndex);
        }
        else {
            protocolStatusCombo
               = new SimpleStatusSelectorBox(
                        this.mainFrame, protocolProvider,
                        providerIndex);
        }
        
        protocolStatusCombo.addComponentListener(this);
        
        this.protocolStatusCombos.put(protocolProvider,
                protocolStatusCombo);

        if(protocolProvider.getProtocolName().equals("SIP"))
            this.add(protocolStatusCombo, FlowLayout.LEFT);
        else
            this.add(protocolStatusCombo);

        this.getParent().validate();
    }

    /**
     * Removes the selector box, containing all protocol statuses, from 
     * the StatusPanel and refreshes the panel.
     * 
     * @param pps The protocol provider to remove.
     */
    public void removeAccount(ProtocolProviderService pps) {
        
        StatusSelectorBox protocolStatusCombo
            = (StatusSelectorBox) this.protocolStatusCombos.get(pps);

        this.protocolStatusCombos.remove(pps);
        this.remove(protocolStatusCombo);

        this.revalidate();
        this.repaint();
    }
    
    /**
     * Updates the account given by the protocol provider.
     * 
     * @param protocolProvider the protocol provider for the account to update
     */
    public void updateAccount(ProtocolProviderService protocolProvider) {
        StatusSelectorBox protocolStatusCombo
            = (StatusSelectorBox) this.protocolStatusCombos
                .get(protocolProvider);
    
        protocolStatusCombo.setAccountIndex(
                mainFrame.getProviderIndex(protocolProvider));
        
        this.revalidate();
        this.repaint();
    }

    
    /**
     * Shows the protocol animated icon, which indicates that it is in a
     * connecting state.
     * 
     * @param protocolProvider The protocol provider.
     */
    public void startConnecting(ProtocolProviderService protocolProvider) {

        StatusSelectorBox selectorBox 
            = (StatusSelectorBox) protocolStatusCombos
                .get(protocolProvider);

        selectorBox.startConnecting(ImageLoader.getAnimatedImage(
                protocolProvider.getProtocolIcon().getConnectingIcon()));

        selectorBox.repaint();
    }

    public String getLastStatusString(
        ProtocolProviderService protocolProvider)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        
        //find the last contact status saved in the configuration.                
        String lastStatus = null;
                        
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        
        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);
        
        Iterator accountsIter = accounts.iterator();
        
        while(accountsIter.hasNext()) {
            String accountRootPropName 
                = (String) accountsIter.next();
            
            String accountUID 
                = configService.getString(accountRootPropName);
            
            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {
                lastStatus = configService.getString(
                        accountRootPropName + ".lastAccountStatus");
                
                if(lastStatus != null)
                    break;
            }
        }
        
        return lastStatus;
    }
        
    /**
     * Returns the last status that was stored in the configuration xml for the
     * given protocol provider.
     * @param protocolProvider the protocol provider 
     * @return the last status that was stored in the configuration xml for the
     * given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {   
        String lastStatus = getLastStatusString(protocolProvider);
        
        OperationSetPresence presence
            = mainFrame.getProtocolPresence(protocolProvider);
    
        if(presence == null)
            return null;
        
        Iterator i = presence.getSupportedStatusSet();
        
        if(lastStatus != null) {
            PresenceStatus status;
            while(i.hasNext()) {
                status = (PresenceStatus)i.next();
                if(status.getStatusName().equals(lastStatus)) {
                    return status;
                } 
            }
        }        
        return null;
    }
    
    /**
     * Updates the status for this protocol provider.
     *  
     * @param protocolProvider The ProtocolProvider, which presence status to
     * update.
     */
    public void updateStatus(ProtocolProviderService protocolProvider) {
        StatusSelectorBox selectorBox 
            = (StatusSelectorBox) protocolStatusCombos
                .get(protocolProvider);
        
        if(selectorBox == null)
            return;
        
        if(selectorBox instanceof PresenceStatusSelectorBox) {
            PresenceStatusSelectorBox presenceSelectorBox
                = (PresenceStatusSelectorBox) selectorBox;
            
            if(!protocolProvider.isRegistered())
                presenceSelectorBox.updateStatus(
                        presenceSelectorBox.getOfflineStatus());
            else {            
                if(presenceSelectorBox.getLastSelectedStatus() != null) {
                    presenceSelectorBox.updateStatus(
                            presenceSelectorBox.getLastSelectedStatus());
                }
                else {           
                    PresenceStatus lastStatus
                        = getLastPresenceStatus(protocolProvider);
                    
                    if(lastStatus == null) {                
                        presenceSelectorBox.updateStatus(
                                presenceSelectorBox.getOnlineStatus());
                    }
                    else {
                        presenceSelectorBox.updateStatus(lastStatus);
                    }
                }
            }
        }
        else {
            ((SimpleStatusSelectorBox)selectorBox).updateStatus();
        }            
        selectorBox.repaint();
    }

    /**
     * Checks if the given protocol has already its <tt>StatusSelectorBox</tt>
     * in the <tt>StatusPanel</tt>.
     * 
     * @param pps The protocol provider to check.
     * @return True if the protcol has already its StatusSelectorBox in the 
     * StatusPanel, False otherwise.
     */
    public boolean containsAccount(ProtocolProviderService pps) {
        if (protocolStatusCombos.containsKey(pps))
            return true;
        else
            return false;
    }
       
    /**
     * Returns TRUE if there are selected status selector boxes, otherwise
     * returns FALSE.
     */
    public boolean hasSelectedMenus()
    {
        Enumeration statusCombos = protocolStatusCombos.elements();
        
        while(statusCombos.hasMoreElements()) {
            StatusSelectorBox statusSelectorBox
                = (StatusSelectorBox)statusCombos.nextElement();
            
            if(statusSelectorBox.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public void componentHidden(ComponentEvent e)
    {}

    public void componentMoved(ComponentEvent e)
    {
        int compCount = this.getComponentCount();
        int buttonHeight = e.getComponent().getHeight();

        int biggestY = 0;
        for (int i = 0; i < compCount; i ++)
        {
            Component c = this.getComponent(i);
            
            if(c instanceof StatusSelectorBox)
            {
                if(c.getY() > biggestY)
                    biggestY = c.getY();
            }
        }
        
        this.setPreferredSize(
            new Dimension(this.getWidth(), biggestY + buttonHeight));
        
        ((JPanel)this.getParent()).revalidate();
        ((JPanel)this.getParent()).repaint();
    }

    public void componentResized(ComponentEvent e)
    {}

    public void componentShown(ComponentEvent e)
    {}
}