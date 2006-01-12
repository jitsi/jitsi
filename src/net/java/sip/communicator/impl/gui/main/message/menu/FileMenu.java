package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedMenuItem;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.MessageWindow;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class FileMenu extends JMenu 
	implements ActionListener{

	private AntialiasedMenuItem saveMenuItem 
						= new AntialiasedMenuItem(Messages.getString("save")
									, new ImageIcon(Constants.SAVE_ICON));

	private AntialiasedMenuItem printMenuItem 
						= new AntialiasedMenuItem(Messages.getString("print")
									, new ImageIcon(Constants.PRINT_ICON));
	
	private AntialiasedMenuItem closeMenuItem 
						= new AntialiasedMenuItem(Messages.getString("close")
									, new ImageIcon(Constants.CLOSE_ICON));
	
	private AntialiasedMenuItem quitMenuItem 
						= new AntialiasedMenuItem(Messages.getString("quit")
									, new ImageIcon(Constants.QUIT_ICON));
	
	private MessageWindow parentWindow;
	
	public FileMenu(MessageWindow parentWindow){
		
		super(Messages.getString("file"));
	
		this.parentWindow = parentWindow;
		
		this.add(saveMenuItem);
		this.add(printMenuItem);
		
		this.addSeparator();
		
		this.add(closeMenuItem);
		this.add(quitMenuItem);
		
		this.saveMenuItem.setName("save");
		this.printMenuItem.setName("print");
		this.closeMenuItem.setName("close");
		this.quitMenuItem.setName("quit");
		
		this.saveMenuItem.addActionListener(this);
		this.printMenuItem.addActionListener(this);
		this.closeMenuItem.addActionListener(this);
		this.quitMenuItem.addActionListener(this);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}

	public void actionPerformed(ActionEvent e) {
		
		JMenuItem menuItem = (JMenuItem)e.getSource();
		String itemText = menuItem.getName();
		
		if (itemText.equalsIgnoreCase("save")){
			
		} else if (itemText.equalsIgnoreCase("print")){
			
		} else if (itemText.equalsIgnoreCase("close")){
			
			this.parentWindow.setVisible(false);
			this.parentWindow.dispose();
			
		} else if (itemText.equalsIgnoreCase("quit")){		
			
			this.parentWindow.setVisible(false);
			this.parentWindow.dispose();
			this.parentWindow.getParentWindow().setVisible(false);
			this.parentWindow.getParentWindow().dispose();			
	        System.exit(0);
		}
		
	}
}
