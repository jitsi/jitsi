package net.java.sip.communicator.impl.gui.main.history;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedMenuItem;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class HistoryMenu extends JMenu implements ActionListener {
	
	private AntialiasedMenuItem emptyMenuItem 
						= new AntialiasedMenuItem(Messages.getString("emptyHistory"));

	private AntialiasedMenuItem closeMenuItem 
						= new AntialiasedMenuItem(Messages.getString("close"));
		
	private JFrame parentWindow;
	
	public HistoryMenu(JFrame parentWindow){
		
		super(Messages.getString("history"));
		
		this.parentWindow = parentWindow;
		
		this.emptyMenuItem.setName("empty");
		this.closeMenuItem.setName("close");
		
		this.emptyMenuItem.addActionListener(this);
		this.closeMenuItem.addActionListener(this);
		
		this.add(emptyMenuItem);
		this.add(closeMenuItem);		
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);		
	}

	public void actionPerformed(ActionEvent e) {
		JMenuItem menuItem = (JMenuItem)e.getSource();
		String menuName = menuItem.getName();
		
		if(menuName.equalsIgnoreCase("empty")){
			
		}
		else if(menuName.equalsIgnoreCase("close")){
			
			this.parentWindow.setVisible(false);
			this.parentWindow.dispose();
		}
	}
}
