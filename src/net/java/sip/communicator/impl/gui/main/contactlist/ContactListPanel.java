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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import net.java.sip.communicator.impl.gui.main.message.ChatPanel;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

/**
 * @author Yana Stamcheva
 * 
 * Creates the contactlist panel.  
 */
public class ContactListPanel extends JScrollPane 
	implements MouseListener {
       
	private MainFrame parent;

	private ContactList contactList;

	private JPanel treePanel = new JPanel(new BorderLayout());

	private Hashtable contactMsgWindows = new Hashtable();
    
    private ChatWindow tabbedChatWindow;

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     * 
     * @param parent The parent frame.
     */
	public ContactListPanel(MainFrame parent) {

		this.parent = parent;        
		
		this.getViewport().add(treePanel);

		this.treePanel.setBackground(Color.WHITE);
        
        this.setHorizontalScrollBarPolicy
            (JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	public void mouseClicked(MouseEvent e) {
		
        //Expand and collapse groups on double click.
        if(e.getClickCount() > 1){
            
            int selectedIndex 
                = this.contactList.locationToIndex(e.getPoint());
            
            ContactListModel listModel = (ContactListModel)this.contactList.getModel();
            
            Object element 
                = listModel.getElementAt(selectedIndex);
            
            if(element instanceof MetaContactGroup){
                
                MetaContactGroup group = (MetaContactGroup)element;
                
                if(listModel.isGroupClosed(group)){
                    listModel.openGroup(group);
                }
                else{
                    listModel.closeGroup(group);
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
        
        // Open message window, right button menu or contact info when 
        // mouse is pressed. Distinguish on which component was pressed 
        // the mouse and make the appropriate work.
        if (this.contactList.getSelectedValue() instanceof MetaContact){
                
            MetaContact contact 
                = (MetaContact) this.contactList.getSelectedValue();
            
            int selectedIndex = this.contactList.getSelectedIndex();
            
			ContactListCellRenderer renderer = (ContactListCellRenderer) 
                this.contactList.getCellRenderer()
                    .getListCellRendererComponent(
							this.contactList,
                            contact, 
                            selectedIndex,
							true, true);
           
            Point selectedCellPoint 
                = this.contactList.indexToLocation(selectedIndex);
            
			int translatedX = e.getX() 
                - selectedCellPoint.x;

            int translatedY = e.getY() 
                - selectedCellPoint.y;
            
            //get the component under the mouse
			Component component 
                = renderer.getComponentAt(translatedX, translatedY);

            
			if (component instanceof JLabel) {
                
				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) 
                            == InputEvent.BUTTON1_MASK) {
                    
                    //Left click on the contact label opens Chat window
					SwingUtilities.invokeLater(new RunMessageWindow(
							contact));
					
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) 
                            == InputEvent.BUTTON3_MASK) {
				    
                    //Right click on the contact label opens Popup menu
					ContactRightButtonMenu popupMenu 
                        = new ContactRightButtonMenu(
							parent, contact);

					popupMenu.setInvoker(this.contactList);

					popupMenu.setLocation(popupMenu.getPopupLocation());

					popupMenu.setVisible(true);
				}
			} else if (component instanceof JButton) {
				
                //Click on the info button opens the info popup panel
				SwingUtilities.invokeLater
                    (new RunInfoWindow(selectedCellPoint, 
                            contact));
			}
		}
	}

	public void mouseReleased(MouseEvent e) {

	}

    /**
     * Runs the chat window for the specified contact.
     * 
     * @author Yana Stamcheva
     */
	private class RunMessageWindow implements Runnable {

		private MetaContact contactItem;

		private RunMessageWindow(MetaContact contactItem) {
			this.contactItem = contactItem;
		}

		public void run() {

            if(!Constants.TABBED_CHAT_WINDOW){
                
                //If in mode "open all messages in new window"
                
    			if (contactMsgWindows.containsKey(this.contactItem)) {
    			    
                    /*
                     * If a chat window for this contact is already opened
                     * show it. 
    			     */
    				ChatWindow msgWindow = (ChatWindow) contactMsgWindows
    						.get(this.contactItem);
    
    				if (msgWindow.getExtendedState() == JFrame.ICONIFIED)
    					msgWindow.setExtendedState(JFrame.NORMAL);
    				
                    if(!msgWindow.isVisible())
                        msgWindow.setVisible(true);
                    
    			} else {
                    /*
                     * If there's no chat window for the contact
                     * create it and show it. 
                     */
			        ChatWindow msgWindow = new ChatWindow(parent);
              
                    contactMsgWindows.put(this.contactItem, msgWindow);
    
                    msgWindow.addChat(this.contactItem);
                    
    				msgWindow.setVisible(true);
    
    				msgWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
    			}
            }
            else{
                // If in mode "group messages in one chat window"
                
                if(tabbedChatWindow == null){
                    
                    // If there's no open chat window
                    tabbedChatWindow = new ChatWindow(parent);
                     
                    tabbedChatWindow.addWindowListener(new WindowAdapter(){
                        
                        public void windowClosing(WindowEvent e) {
                            tabbedChatWindow = null;
                        }
                    });
                }
                
                /*
                 * Get the hashtable containg all tabs and correspondins 
                 * chat panels.
                 */
                Hashtable contactTabsTable 
                    = tabbedChatWindow.getContactTabsTable();

                if(contactTabsTable.get(this.contactItem.getDisplayName()) 
                        == null){
                    
                    // If there's no open tab for the given contact.                    
                    tabbedChatWindow.addChatTab(this.contactItem);
    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    if(!tabbedChatWindow.isVisible())
                        tabbedChatWindow.setVisible(true);
    
                    tabbedChatWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
                }
                else{
                    // If a tab fot the given contact already exists.
                    tabbedChatWindow.setSelectedContactTab(this.contactItem);
                    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    tabbedChatWindow.setVisible(true);
                    
                    tabbedChatWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
                }
            }
		}
	}
	
    /**
     * Runs the info window for the specified contact at the 
     * appropriate position.
     * 
     * @author Yana Stamcheva
     */
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
        
        this.addKeyListener(new CListKeySearchListener(this.contactList));
        
    }
    
    public ContactList getContactList(){
    	
    	return this.contactList;
    }
    
}