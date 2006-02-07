/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.ContactList;
import net.java.sip.communicator.impl.gui.main.ContactRightButtonMenu;
import net.java.sip.communicator.impl.gui.main.GroupItem;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.message.MessageWindow;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

/**
 * @author Yana Stamcheva
 * 
 * The ContactListPanel contains the contact list.
 */
public class ContactListPanel extends JScrollPane implements MouseListener {

	private ContactList contactList;

	private MainFrame parent;

	private ContactListTree contactListTree;
	
	private JPanel treePanel = new JPanel(new BorderLayout());
	
	private Hashtable contactMsgWindows = new Hashtable();
	
	public ContactListPanel(MainFrame parent) {		
		
		this.parent = parent;

		this.contactList = parent.getContactList();

		this.contactListTree = new ContactListTree(new ContactNode(new GroupItem("root")));
		
		this.contactListTree.addMouseListener(this);
		
		this.initTree();
		
		this.treePanel.add(contactListTree, BorderLayout.NORTH);
		
		this.getViewport().add(treePanel);
		
		this.treePanel.setBackground(Color.WHITE);
	}

	private void initTree() {
		
		// TODO: To be removed!!!!
		ContactNode generalGroup = (ContactNode)this.contactListTree
												.addChild(new GroupItem("General"));

		for (int i = 0; i < this.contactList.getAllContacts().size(); i++) {

			this.contactListTree.addChild(generalGroup, (ContactItem) this.contactList
					.getAllContacts().get(i), true);
		}
	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		
		int selRow = this.contactListTree.getRowForLocation(e.getX(), e.getY());

		TreePath selPath = this.contactListTree.getPathForLocation(e.getX(), e.getY());
		
		if (selRow != -1) {

			if (e.getClickCount() == 1) {				
				
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) contactListTree
				  										.getLastSelectedPathComponent();
				  
				  if (node == null) return;
				  				  
				  if (node.isLeaf()) {
					  
					  ContactListCellRenderer renderer = 
							(ContactListCellRenderer) this.contactListTree.getCellRenderer()
							.getTreeCellRendererComponent(	this.contactListTree,
															selPath.getLastPathComponent(), 
															false, false,
															true, selRow, true);
					  
					  //Translate coordinates into cell cordinates.
					  
					  int translatedX = (int)e.getX() - (int)this.contactListTree.getPathBounds(selPath).getX();
					  
					  int translatedY = (int)e.getY() - (int)this.contactListTree.getPathBounds(selPath).getY();
						
					  Component component = renderer.findComponentAt(translatedX, translatedY);
				  
					  ContactItem contactItem = (ContactItem) node.getUserObject();

					  if(component instanceof JLabel){
						  
						  if((e.getModifiers() & InputEvent.BUTTON1_MASK)  ==
						  InputEvent.BUTTON1_MASK){
							  
							  SwingUtilities.invokeLater(new RunMessageWindow(contactItem));							
						  }
						  else if((e.getModifiers() & InputEvent.BUTTON3_MASK) == 
								 InputEvent.BUTTON3_MASK){ 
							  
							  ContactRightButtonMenu popupMenu 
							  			= new ContactRightButtonMenu(parent, contactItem);
								 
							  popupMenu.setInvoker(this.contactListTree);
							
							  popupMenu.setLocation(popupMenu.getPopupLocation());
							
							  popupMenu.setVisible(true);
						  }
					  }
					  else if(component instanceof JButton){
						  
							ContactInfoPanel contactInfoPanel = new ContactInfoPanel(contactItem);
														
							Point p = new Point();
							
							p.x = (int)this.contactListTree.getPathBounds(selPath).getX();
							
							p.y = 	(int)this.contactListTree.getPathBounds(selPath).getY();
							
							SwingUtilities.convertPointToScreen(p, this.contactListTree);
							
							//TODO: to calculate popup window posititon properly.
							contactInfoPanel.setLocation(p.x - 140, p.y - 15);
							
							contactInfoPanel.setVisible(true);
							
							contactInfoPanel.requestFocus();
					  }
				  }				
				  
			}
			else if (e.getClickCount() == 2){
				
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}
	
	private class RunMessageWindow implements Runnable{
		
		private ContactItem contactItem;
		
		private RunMessageWindow(ContactItem contactItem){
			this.contactItem = contactItem;
		}
		
		public void run() {
			
			if (contactMsgWindows.containsKey(this.contactItem)){
				
				MessageWindow msgWindow 
								= (MessageWindow)contactMsgWindows.get(this.contactItem);
				
				if(msgWindow.isVisible()){				
					msgWindow.requestFocus();
				}
			}
			else{				
				
				MessageWindow msgWindow = new MessageWindow(parent);
				 
				contactMsgWindows.put(this.contactItem, msgWindow);
				
				msgWindow.addContactToChat(this.contactItem);
				  
				msgWindow.setVisible(true);
				
				msgWindow.getWriteMessagePanel().getEditorPane().requestFocus();
			}
			
		}
	}
}