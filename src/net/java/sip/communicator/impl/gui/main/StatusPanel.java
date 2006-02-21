/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.FlowLayout;
import java.awt.Image;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class StatusPanel extends JPanel {

	private String[] userProtocols; 
    
	private Hashtable protocolStatusCombos = new Hashtable();
    
    private OperationSetPresence presence;
    
	public StatusPanel(String[] userProtocols) {

		this.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));

		this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
				Constants.CONTACTPANEL_MOVER_START_COLOR));

		this.userProtocols = userProtocols;

		this.init();
	}

	private void init() {

		for (int i = 0; i < userProtocols.length; i++) {
            
            Map protocolStatusMap = Constants
                .getProtocolStatusIcons(userProtocols[i]);

			StatusSelectorBox protocolStatusCombo 
                = new StatusSelectorBox(
					protocolStatusMap, 
                    (Image)protocolStatusMap.get(Constants.OFFLINE_STATUS));

            this.protocolStatusCombos.put(  userProtocols[i], 
                                            protocolStatusCombo);
            
			this.add(protocolStatusCombo);
		}
	}
    
    public void setSelectedStatus(String protocol, Object status){
        
        Map protocolStatusMap = Constants
            .getProtocolStatusIcons(protocol);
        
        StatusSelectorBox selectorBox
            = (StatusSelectorBox)protocolStatusCombos.get(protocol);
        
        selectorBox.setIconImage((Image)protocolStatusMap.get(status));
        
        selectorBox.repaint();
    }
    
    public void setConnecting(String protocol){
        
        Map protocolStatusMap = Constants
            .getProtocolStatusIcons(protocol);
        
        StatusSelectorBox selectorBox
            = (StatusSelectorBox)protocolStatusCombos.get(protocol);
        
        selectorBox.setIconImage(ImageLoader.getImage(ImageLoader.ICQ_CONNECTING));
        
        selectorBox.repaint();
    }

    public Hashtable getProtocolStatusCombos() {
        return protocolStatusCombos;
    }

    public void setProtocolStatusCombos(
            Hashtable protocolStatusCombos) {
        this.protocolStatusCombos = protocolStatusCombos;
    }

    public OperationSetPresence getPresence() {
        return presence;
    }

    public void setPresence(OperationSetPresence presence) {
        this.presence = presence;
        
        Enumeration statusCombos 
            = this.getProtocolStatusCombos().elements();
        
        while(statusCombos.hasMoreElements()){
            
            ((StatusSelectorBox)statusCombos.nextElement())
                .setPresence(presence);
        }
    }
}