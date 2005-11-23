package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * @author Yana Stamcheva
 *
 * The ContactListPanel contains the contact list.
 */
public class ContactListPanel extends JScrollPane{
	 
	private ContactList clist;
	private JPanel mainPanel = new JPanel(); 
	private JPanel contactsPanel = new JPanel();
	
	public ContactListPanel(ContactList clist){
		
		this.mainPanel.setLayout(new BorderLayout());
		this.contactsPanel.setLayout(new BoxLayout(this.contactsPanel, BoxLayout.Y_AXIS));
		
		this.clist = clist;
		this.init();
	}	
	
	private void init(){	
		
		for (int i = 0; i < this.clist.getAllContacts().size(); i ++){
			
			ContactPanel cpanel = new ContactPanel((ContactItem)this.clist.getAllContacts().get(i));	
			cpanel.setPreferredSize(new Dimension(LookAndFeelConstants.CONTACTPANEL_WIDTH, LookAndFeelConstants.CONTACTPANEL_HEIGHT));
						
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
