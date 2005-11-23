package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Component;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.java.sip.communicator.impl.gui.main.LookAndFeelConstants;
import net.java.sip.communicator.impl.gui.main.Status;

public class StatusSelectorBox extends SIPCommButton
	implements ActionListener{

	private JPopupMenu popup;

	private Object[] items;
	
	public StatusSelectorBox(Object[] items, Status currentStatus) {
		
		super(	LookAndFeelConstants.STATUS_SELECTOR_BOX,
				LookAndFeelConstants.STATUS_SELECTOR_BOX,
				currentStatus.getIcon());
				
		this.popup = new JPopupMenu();
		
		this.items = items;
		
		this.init();
	}

	public void init() {

		for (int i = 0; i < items.length; i++) {

			if (items[i] instanceof Status) {

				Status status = (Status) items[i];
				JMenuItem item = new JMenuItem(	status.getText(), 
												new ImageIcon(status.getIcon()));
				
				item.addActionListener(this);
				
				this.popup.add(item);
			}
		}
		
		this.popup.setInvoker(this);		
		this.addActionListener(this);
		
	}

	public void actionPerformed (ActionEvent e) {
		
		if (e.getSource() instanceof SIPCommButton){
	
			if (!this.popup.isVisible()) {
				this.popup.setLocation(this.calculatePopupLocation());
				this.popup.setVisible(true);			
			}		
		}
		else if (e.getSource() instanceof JMenuItem){
			
			JMenuItem menuItem = (JMenuItem) e.getSource();			
			
			this.setIconImage(((ImageIcon)menuItem.getIcon()).getImage());			
		}
	}
	
	public Point calculatePopupLocation(){
		
		Component component = this;
		Point point = new Point();
		int x = this.getX();
		int y = this.getY();
		
		while(component.getParent() != null){
			
			component = component.getParent();
			
			x += component.getX();
			y += component.getY();
		}
		
		point.x = x;
		point.y = y + this.getHeight();
		
		return point;
	}
}
