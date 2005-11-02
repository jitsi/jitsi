package net.java.sip.communicator.impl.gui.main;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

/**
 * @author Yana Stamcheva
 *
 * The main menu. 
 */
public class Menu extends JMenuBar {
	private JMenu userMenu = new JMenu();
	private JMenu toolsMenu = new JMenu();
	private JMenu viewMenu = new JMenu();
	private JMenu helpMenu = new JMenu();
	
	public Menu(){
		this.init();
	}
	
	private void init(){
		userMenu.setText("User");
		userMenu.setMnemonic('U');
		userMenu.setToolTipText("User");
		
		toolsMenu.setText("Tools");
		toolsMenu.setMnemonic('T');
		toolsMenu.setToolTipText("Tools");
		
		viewMenu.setText("View");
		viewMenu.setMnemonic('V');
		viewMenu.setToolTipText("View");
		
		helpMenu.setText("Help");
		helpMenu.setMnemonic('H');
		helpMenu.setToolTipText("Help");
		
		this.add(userMenu);
		this.add(toolsMenu);
		this.add(viewMenu);
		this.add(helpMenu);
	}
}
