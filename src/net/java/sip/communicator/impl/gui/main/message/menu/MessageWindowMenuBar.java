package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.Graphics;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.MessageWindow;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class MessageWindowMenuBar extends JMenuBar {

	private FileMenu fileMenu;
	
	private EditMenu editMenu = new EditMenu();
	
	private JMenu settingsMenu = new JMenu(Messages.getString("settings"));
	
	private JMenu helpMenu = new JMenu(Messages.getString("help"));		
	
	private MessageWindow parentWindow;
		
	public MessageWindowMenuBar(MessageWindow parentWindow){
		
		this.parentWindow = parentWindow;
		
		fileMenu = new FileMenu(this.parentWindow);
		
		this.init();		
	}
	
	public void init(){		
	
		this.add(fileMenu);
		
		this.add(editMenu);
		
		this.add(settingsMenu);
		
		this.add(helpMenu);
		
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
