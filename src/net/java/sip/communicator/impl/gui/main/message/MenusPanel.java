package net.java.sip.communicator.impl.gui.main.message;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.message.menu.MessageWindowMenuBar;
import net.java.sip.communicator.impl.gui.main.message.toolBars.EditTextToolBar;
import net.java.sip.communicator.impl.gui.main.message.toolBars.MainToolBar;

public class MenusPanel extends JPanel {

	private MessageWindowMenuBar menuBar;
	
	private EditTextToolBar editTextToolBar = new EditTextToolBar();
	
	private MainToolBar mainToolBar;
	
	private MessageWindow parentWindow;
	
	public MenusPanel (MessageWindow parentWindow){
		
		super();
		
		this.parentWindow = parentWindow;
		
		mainToolBar = new MainToolBar(this.parentWindow);
		menuBar = new MessageWindowMenuBar(this.parentWindow);
		
		this.setLayout(new GridLayout(0, 1));
		
		this.add(menuBar);
		this.add(mainToolBar);
		//this.add(editTextToolBar);		
	}
		
	public void addToolBar(JToolBar toolBar){
		this.add(toolBar);
	}
}
