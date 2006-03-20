/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.Contact;
import net.kano.joscar.ssiitem.GroupItem;

public class ContactListCellRenderer extends JPanel 
	implements ListCellRenderer {

	private JLabel nameLabel = new JLabel();
	
	private JPanel buttonsPanel 
		= new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

	private SIPCommButton extendPanelButton 
		= new SIPCommButton(ImageLoader.getImage(ImageLoader.MORE_INFO_ICON), 
							ImageLoader.getImage(ImageLoader.MORE_INFO_ICON));

	private boolean isSelected = false;

	private boolean isLeaf = true;
	
	/**
	 * Initialize the panel containing the node.
	 */
	public ContactListCellRenderer() {

		super(new BorderLayout());

		this.setBackground(Color.WHITE);
		this.buttonsPanel.setOpaque(false);
		
		this.setOpaque(true);	

		this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		this.nameLabel.setIconTextGap(2);
		
		this.add(nameLabel, BorderLayout.CENTER);
	}

	
	public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        ContactList contactList = (ContactList)list;
        ContactListModel listModel = (ContactListModel)contactList.getModel();
        
		if (value instanceof MetaContact) {
			 
            MetaContact contactItem = (MetaContact)value;

            String toolTipText = "<html>" + contactItem.getDisplayName();
            
			this.nameLabel.setText(contactItem.getDisplayName());

			this.nameLabel.setIcon(listModel.getMetaContactStatusIcon(contactItem));

			this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
			
			this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            
            // We should set the bounds of the cell explicitely in order to 
            // make getComponentAt work properly. 
            this.setBounds(0, 0, list.getWidth() - 2, 17);
            
            this.buttonsPanel.removeAll();
            //this.buttonsPanel.add(extendPanelButton);
         
            Iterator i = contactItem.getContacts();
            while(i.hasNext()){
                Contact protocolContact = (Contact)i.next();
                
                String protocolName 
                    = protocolContact.getProtocolProvider().getProtocolName();
                
                Image protocolStatusIcon = (Image)Constants.getProtocolStatusIcons
                                        (protocolName).get(protocolContact.getPresenceStatus());
                
                SIPCommButton contactProtocolButton = new SIPCommButton(protocolStatusIcon, 
                                                                                                            protocolStatusIcon);
                this.buttonsPanel.add(contactProtocolButton);
            	
            	//toolTipText 
            		//+= "<img src='" 
            			//+ ImageLoader.getImagePath(protocolStatusIcon) 
            			//+ "'></img>";
            			
            }
       
            this.add(buttonsPanel, BorderLayout.EAST);
            
            toolTipText += "</html>";
            this.setToolTipText(toolTipText);
            
            this.isLeaf = true;
		}
		else if (value instanceof MetaContactGroup) {		

			MetaContactGroup groupItem = (MetaContactGroup) value;
				
			this.nameLabel.setText(groupItem.getGroupName());

			this.nameLabel.setIcon(new ImageIcon(
					ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));

			this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
								
			this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			
			// We should set the bounds of the cell explicitely in order to 
            // make getComponentAt work properly. 
            this.setBounds(0, 0, list.getWidth() - 2, 20);
			
            this.remove(buttonsPanel);
            
            this.isLeaf = false;
		}
        
		this.isSelected = isSelected;
		
		return this;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;

		AntialiasingManager.activateAntialiasing(g2);		
		
		if(!this.isLeaf){

			g2.setColor(Constants.CONTACTPANEL_MOVER_START_COLOR);
			g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);
		}
		
		if (this.isSelected) {
		
			g2.setColor(Constants.CONTACTPANEL_SELECTED_END_COLOR);			
			g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);
			
			g2.setColor(Constants.CONTACTPANEL_BORDER_COLOR);
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(0, 0, this.getWidth() - 1, 
								this.getHeight() - 1, 7, 7);
		} 
	}
	
	public SIPCommButton getExtendPanelButton() {
		return extendPanelButton;
	}
}
