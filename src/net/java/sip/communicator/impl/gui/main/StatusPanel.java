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
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class StatusPanel extends JPanel {

	private String[] userProtocols; 
    
	private Hashtable protocolStatusCombos = new Hashtable();
    
    private MainFrame mainFrame;
    
	public StatusPanel(MainFrame mainFrame, String[] userProtocols) {

        this.mainFrame = mainFrame;
        
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
                    this.mainFrame,
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
        
        selectorBox.setIcon(new ImageIcon(
                (Image)protocolStatusMap.get(status)));
        
        selectorBox.repaint();
    }
    
    public void startConnecting(String protocol){
        
        Map protocolStatusMap = Constants
            .getProtocolStatusIcons(protocol);
        
        StatusSelectorBox selectorBox
            = (StatusSelectorBox)protocolStatusCombos.get(protocol);
        
        selectorBox.startConnecting(ImageLoader
                .getAnimatedImage(ImageLoader.ICQ_CONNECTING));
        
        selectorBox.repaint();
    }

    public void stopConnecting(String protocol){
        
        StatusSelectorBox selectorBox
            = (StatusSelectorBox)protocolStatusCombos.get(protocol);
        
        selectorBox.stopConnecting();
        
        selectorBox.repaint();
    }
    
    public Hashtable getProtocolStatusCombos() {
        return protocolStatusCombos;
    }

    public void setProtocolStatusCombos(
            Hashtable protocolStatusCombos) {
        this.protocolStatusCombos = protocolStatusCombos;
    }
    
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
}