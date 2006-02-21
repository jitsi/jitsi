/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import gov.nist.javax.sip.parser.ContactParser;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.GroupItem;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentBackground;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

public class ContactListCellRenderer extends JPanel 
	implements TreeCellRenderer {

	private JLabel nameLabel = new JLabel();

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

		this.setOpaque(true);	

		this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		this.nameLabel.setIconTextGap(2);
		
		this.add(nameLabel, BorderLayout.CENTER);
		
		this.add(extendPanelButton, BorderLayout.EAST);
	}

	
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		// Find out which node we are rendering and get its text
		ContactNode node = (ContactNode) value;
		
		if(leaf){
			
			if (node.getUserObject() instanceof ContactItem) {
							
				ContactItem contactItem = (ContactItem) node.getUserObject();

				this.nameLabel.setText(contactItem.getNickName());
	
				this.nameLabel.setIcon(contactItem.getUserIcon());
	
				this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
				
				this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
				
				this.setPreferredSize(new Dimension(Constants.MAINFRAME_WIDTH + 20, 17));
				
				this.setBounds(0, 0, Constants.MAINFRAME_WIDTH + 20, 17);				
			} 
		}
		else{ 
			if (node.getUserObject() instanceof GroupItem) {		

				GroupItem groupItem = (GroupItem) node.getUserObject();
					
				this.nameLabel.setText(groupItem.getGroupName());
	
				this.nameLabel.setIcon(new ImageIcon(
						ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));
	
				this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
									
				this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
				
				this.setPreferredSize(new Dimension(Constants.MAINFRAME_WIDTH + 20, 20));
				
				this.setBounds(0, 0, Constants.MAINFRAME_WIDTH + 20, 20);				
			}
		}

		this.isSelected = selected;

		this.isLeaf = leaf;
		
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
			g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 7, 7);

		} 
	}
	
	public SIPCommButton getExtendPanelButton() {
		return extendPanelButton;
	}
}
