/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.util.*;
import java.util.List;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>StatusPanel</tt> is the place where the user can see and change
 * its status for all registered protocols. 
 * 
 * @author Yana Stamcheva
 */
public class StatusPanel extends JPanel {

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
    public void addAccount(ProtocolProviderService protocolProvider) {

        StatusSelectorBox protocolStatusCombo = new StatusSelectorBox(
                this.mainFrame, protocolProvider);
        
        this.protocolStatusCombos.put(protocolProvider.getAccountID(),
                protocolStatusCombo);

        this.add(protocolStatusCombo);

        this.getParent().validate();
    }

    /**
     * Removes the selector box, containing all protocol statuses, from 
     * the StatusPanel and refreshes the panel.
     * 
     * @param accountID The identifier of the account to remove.
     */
    public void removeAccount(AccountID accountID) {
        
        StatusSelectorBox protocolStatusCombo = (StatusSelectorBox)
            this.protocolStatusCombos.get(accountID);

        this.protocolStatusCombos.remove(accountID);
        this.remove(protocolStatusCombo);

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
                .get(protocolProvider.getAccountID());

        selectorBox.startConnecting(Constants
                .getProtocolAnimatedIcon(protocolProvider.getProtocolName()));

        selectorBox.repaint();
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
                .get(protocolProvider.getAccountID());

        if(!protocolProvider.isRegistered())
            selectorBox.updateStatus(selectorBox.getOfflineStatus());
        else {            
            if(selectorBox.getLastSelectedStatus() != null) {
                selectorBox.updateStatus(selectorBox.getLastSelectedStatus());
            }
            else {
                ConfigurationService configService
                    = GuiActivator.getConfigurationService();
                
                //find the last contact status saved in the configuration.                
                String lastStatus = null;
                
                Iterator i = mainFrame.getProtocolPresence(protocolProvider)
                    .getSupportedStatusSet();
                
                String prefix = "net.java.sip.communicator.impl.ui.accounts";
                
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
                
                if(lastStatus == null) {                
                    selectorBox.updateStatus(selectorBox.getOnlineStatus());
                }
                else {
                    PresenceStatus status;
                    while(i.hasNext()) {
                        status = (PresenceStatus)i.next();
                        if(status.getStatusName().equals(lastStatus)) {
                            selectorBox.updateStatus(status);
                            break;
                        } 
                    }
                }
            }
        }
        selectorBox.repaint();
    }

    /**
     * Checks if the given protocol has already its <tt>StatusSelectorBox</tt>
     * in the <tt>StatusPanel</tt>.
     * 
     * @param accountID The identifier of the account.
     * @return True if the protcol has already its StatusSelectorBox in the 
     * StatusPanel, False otherwise.
     */
    public boolean containsAccount(AccountID accountID) {
        if (protocolStatusCombos.containsKey(accountID))
            return true;
        else
            return false;
    }   
}