/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedMenuItem;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

public class EditMenu extends JMenu 
	implements ActionListener{
	
	private AntialiasedMenuItem cutMenuItem 
		= new AntialiasedMenuItem(Messages.getString("cut")
				, new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

	private AntialiasedMenuItem copyMenuItem 
		= new AntialiasedMenuItem(Messages.getString("copy")
				, new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));
	
	private AntialiasedMenuItem pasteMenuItem 
		= new AntialiasedMenuItem(Messages.getString("paste")
				, new ImageIcon(ImageLoader.getImage(ImageLoader.PASTE_ICON)));

	private ChatWindow chatWindow;
	
	public EditMenu(ChatWindow chatWindow){
		
		super(Messages.getString("edit"));
		
		this.chatWindow = chatWindow;
		
		this.cutMenuItem.setName("cut");
		this.copyMenuItem.setName("copy");
		this.pasteMenuItem.setName("paste");
		
		this.cutMenuItem.addActionListener(this);
		this.copyMenuItem.addActionListener(this);
		this.pasteMenuItem.addActionListener(this);
		
		this.add(cutMenuItem);
		this.add(copyMenuItem);
		this.add(pasteMenuItem);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);		
	}

	public void actionPerformed(ActionEvent e) {
		JMenuItem menuItem = (JMenuItem)e.getSource();
		String menuItemName = menuItem.getName();
		
		if (menuItemName.equalsIgnoreCase("cut")) {
			
			JEditorPane editorPane 
				= this.chatWindow.getCurrentChatWritePanel().getEditorPane();
			
			editorPane.cut();
			
		} else if (menuItemName.equalsIgnoreCase("copy")) {
			
			JEditorPane editorPane 
				= this.chatWindow.getConversationPanel()
					.getChatEditorPane();
			
			if(editorPane.getSelectedText() == null){
				editorPane 
					= this.chatWindow.getCurrentChatWritePanel().getEditorPane();
			}
			editorPane.copy();
			
		} else if (menuItemName.equalsIgnoreCase("paste")) {
			
			JEditorPane editorPane 
				= this.chatWindow.getCurrentChatWritePanel().getEditorPane();
			
			editorPane.paste();
		}
	}
}
