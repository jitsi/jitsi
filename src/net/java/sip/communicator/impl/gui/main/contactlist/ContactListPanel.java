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
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.message.MessageWindow;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

/**
 * @author Yana Stamcheva
 * 
 * The ContactListPanel contains the contact list.
 */
public class ContactListPanel extends JScrollPane 
	implements MouseListener {
       
	private MainFrame parent;

	private ContactList contactList;

	private JPanel treePanel = new JPanel(new BorderLayout());

	private Hashtable contactMsgWindows = new Hashtable();

	public ContactListPanel(MainFrame parent) {

		this.parent = parent;        
		
		this.getViewport().add(treePanel);

		this.treePanel.setBackground(Color.WHITE);
        
        this.setHorizontalScrollBarPolicy
            (JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	public void mouseClicked(MouseEvent e) {
		
        if(e.getClickCount() > 1){
            
            int selectedIndex 
                = this.contactList.locationToIndex(e.getPoint());
            
            ListModel listModel = this.contactList.getModel();
            
            Object element 
                = listModel.getElementAt(selectedIndex);
            
            if(element instanceof MetaContactGroup){
                
                MetaContactGroup group = (MetaContactGroup)element;
                
                if(group.countChildContacts() > 0){
                    
                    if(selectedIndex == listModel.getSize() - 1
                       || listModel.getElementAt(selectedIndex + 1) 
                          instanceof MetaContactGroup){
                       
                        //Expand group
                       Iterator iter = group.getChildContacts();
                       
                       while(iter.hasNext()){
                           
                           MetaContact contact = (MetaContact) iter.next();
                           
                           this.contactList
                               .addChild(group,
                                       new MetaContactNode(contact));
                       }
                    }
                    else{
                        //Collapse group
                        ((ContactListModel)listModel)
                            .removeRange(selectedIndex + 1, 
                                selectedIndex + group.countChildContacts());
                    }
                }
            }
        }
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
        
        if (this.contactList.getSelectedValue() instanceof MetaContactNode){
                
            MetaContactNode contactNode 
                = (MetaContactNode) this.contactList.getSelectedValue();
            
            int selectedIndex = this.contactList.getSelectedIndex();
            
			ContactListCellRenderer renderer = (ContactListCellRenderer) 
                this.contactList.getCellRenderer()
                    .getListCellRendererComponent(
							this.contactList,
                            contactNode, 
                            selectedIndex,
							true, true);
           
            Point selectedCellPoint 
                = this.contactList.indexToLocation(selectedIndex);
            
			int translatedX = e.getX() 
                - selectedCellPoint.x;

            int translatedY = e.getY() 
                - selectedCellPoint.y;
            
			Component component 
                = renderer.getComponentAt(translatedX, translatedY);

			if (component instanceof JLabel) {

				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) 
                            == InputEvent.BUTTON1_MASK) {
                    
					SwingUtilities.invokeLater(new RunMessageWindow(
							contactNode.getContact()));
					
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) 
                            == InputEvent.BUTTON3_MASK) {

					ContactRightButtonMenu popupMenu 
                        = new ContactRightButtonMenu(
							parent, contactNode.getContact());

					popupMenu.setInvoker(this.contactList);

					popupMenu.setLocation(popupMenu.getPopupLocation());

					popupMenu.setVisible(true);
				}
			} else if (component instanceof JButton) {
				
				SwingUtilities.invokeLater
                    (new RunInfoWindow(selectedCellPoint, 
                            contactNode.getContact()));
			}
		}
	}

	public void mouseReleased(MouseEvent e) {

	}

	private class RunMessageWindow implements Runnable {

		private MetaContact contactItem;

		private RunMessageWindow(MetaContact contactItem) {
			this.contactItem = contactItem;
		}

		public void run() {

			if (contactMsgWindows.containsKey(this.contactItem)) {

				MessageWindow msgWindow = (MessageWindow) contactMsgWindows
						.get(this.contactItem);

				if (msgWindow.getExtendedState() == JFrame.ICONIFIED)
					msgWindow.setExtendedState(JFrame.NORMAL);

				msgWindow.setVisible(true);
			} else {

				MessageWindow msgWindow = new MessageWindow(parent);

				contactMsgWindows.put(this.contactItem, msgWindow);

				msgWindow.addContactToChat(this.contactItem);

				msgWindow.setVisible(true);

				msgWindow.getWriteMessagePanel().getEditorPane().requestFocus();
			}

		}
	}
	
	
	private class RunInfoWindow implements Runnable {

		private MetaContact contactItem;

		private Point p;
		
		private RunInfoWindow(Point p, MetaContact contactItem) {
		
			this.p = p;
			this.contactItem = contactItem;
		}

		public void run() {

			ContactInfoPanel contactInfoPanel = new ContactInfoPanel(parent, contactItem);

			SwingUtilities.convertPointToScreen(p, contactList);

			// TODO: to calculate popup window posititon properly.
			contactInfoPanel.setPopupLocation(p.x - 140, p.y - 15);
			
			contactInfoPanel.setVisible(true);														
									
			contactInfoPanel.requestFocusInWindow();

		}
	}
   
    public void initTree(MetaContactListService contactListService) {
        
        this.contactList = new ContactList(contactListService);
        
        this.contactList.addMouseListener(this);

        this.treePanel.add(contactList, BorderLayout.NORTH);
    }
    
    public ContactList getContactList(){
    	
    	return this.contactList;
    }
    
}