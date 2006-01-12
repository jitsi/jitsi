package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.message.MessageWindow;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

/**
 * @author Yana Stamcheva
 *
 * The ContactListPanel contains the contact list.
 */
public class ContactListPanel extends JScrollPane{
	 
	private ContactList clist;
	
	private JPanel mainPanel = new JPanel(); 
	
	private JPanel contactsPanel = new JPanel();
	
	private MainFrame parent;
	
	public ContactListPanel(MainFrame parent, ContactList clist){
		
		this.mainPanel.setBackground(Color.WHITE);
		this.mainPanel.setOpaque(true);
		
		this.parent = parent;
		
		this.mainPanel.setLayout(new BorderLayout());
		this.contactsPanel.setLayout(new BoxLayout(this.contactsPanel, BoxLayout.Y_AXIS));
		
		this.clist = clist;
		this.init();
	}	
	
	private void init(){	
		
		for (int i = 0; i < this.clist.getAllContacts().size(); i ++){
			
			ContactPanel cpanel = new ContactPanel((ContactItem)this.clist.getAllContacts().get(i));	
			cpanel.setPreferredSize(new Dimension(Constants.CONTACTPANEL_WIDTH, Constants.CONTACTPANEL_HEIGHT));
						
			cpanel.addMouseListener(new MouseAdapter(){
				public void mouseEntered(MouseEvent e){
					ContactPanel cpanel = (ContactPanel)e.getSource();
					
					cpanel.setMouseOver(true);				
					cpanel.repaint();				
				}
				
				public void mouseExited(MouseEvent e){
					ContactPanel cpanel = (ContactPanel)e.getSource();
					
					cpanel.setMouseOver(false);				
					cpanel.repaint();				
				}
				
				public void mouseClicked(MouseEvent e){
					
					ContactPanel cpanel = (ContactPanel)e.getSource();
					
					cpanel.setSelected(true);		
					refreshContactsStatus(cpanel);
					
					if((e.getModifiers() & InputEvent.BUTTON1_MASK)
								== InputEvent.BUTTON1_MASK){
						
						MessageWindow msgWindow = new MessageWindow(parent);
						
						msgWindow.addContactToChat(cpanel.getContactItem());
						
						msgWindow.setVisible(true);
						
						msgWindow.getWriteMessagePanel().getEditorPane().requestFocus();
						
						msgWindow.enableKeyboardSending();
					}
					else if((e.getModifiers() & InputEvent.BUTTON3_MASK)
							== InputEvent.BUTTON3_MASK){						
						
						ContactRightButtonMenu popupMenu 
											= new ContactRightButtonMenu(parent, cpanel.getContactItem());
						
						popupMenu.setInvoker(cpanel);
												
						popupMenu.setLocation(popupMenu.calculatePopupLocation());
						popupMenu.setVisible(true);
					}
				}
			});
			
			this.contactsPanel.add(cpanel);
		}
		
		this.mainPanel.add(contactsPanel, BorderLayout.NORTH);
		this.getViewport().add(mainPanel);
	}
		
	public void refreshContactsStatus(ContactPanel cpanelSelected){
		
		for (int i = 0; i < this.contactsPanel.getComponentCount(); i ++){
			ContactPanel cpanel = (ContactPanel)this.contactsPanel.getComponent(i);
						
			if(!cpanel.equals(cpanelSelected)){					
				cpanel.setSelected(false);
			}
		}
	}


	
}
