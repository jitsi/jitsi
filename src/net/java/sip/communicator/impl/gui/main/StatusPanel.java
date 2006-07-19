/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.FlowLayout;
import java.awt.Image;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

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

        this.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                Constants.MOVER_START_COLOR));
    }

    /**
     * Creates the selector box, containing all protocol statuses, adds it to 
     * the StatusPanel and refreshes the panel.
     * 
     * @param protocolProvider The protocol provider.
     */
    public void activateAccount(ProtocolProviderService protocolProvider) {

        Map protocolStatusMap = Constants
                .getProtocolStatusIcons(protocolProvider.getProtocolName());

        StatusSelectorBox protocolStatusCombo = new StatusSelectorBox(
                this.mainFrame, protocolProvider, protocolStatusMap,
                (Image) protocolStatusMap.get(Constants.OFFLINE_STATUS));
        
        this.protocolStatusCombos.put(protocolProvider.getAccountID(),
                protocolStatusCombo);

        this.add(protocolStatusCombo);

        this.getParent().validate();
    }

    /**
     * Sets the selected status.
     * 
     * @param protocolProvider The protocol provider.
     * @param status The newly selected status.
     */
    public void setSelectedStatus(ProtocolProviderService protocolProvider,
                                    Object status) {

        Map protocolStatusMap = Constants.getProtocolStatusIcons(
                protocolProvider.getProtocolName());

        StatusSelectorBox selectorBox 
            = (StatusSelectorBox) protocolStatusCombos.get(
                    protocolProvider.getAccountID());

        selectorBox
                .setIcon(new ImageIcon((Image) protocolStatusMap.get(status)));

        selectorBox.repaint();
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
     * Removes the protocol animated icon, which indicates that the connecting 
     * process is finished.
     * 
     * @param protocolProvider The ProtocolProvider.
     */
    public void stopConnecting(ProtocolProviderService protocolProvider) {

        StatusSelectorBox selectorBox 
            = (StatusSelectorBox) protocolStatusCombos
                .get(protocolProvider.getAccountID());

        selectorBox.stopConnecting();

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
    public boolean isAccountActivated(AccountID accountID) {
        if (protocolStatusCombos.containsKey(accountID))
            return true;
        else
            return false;
    }   
}