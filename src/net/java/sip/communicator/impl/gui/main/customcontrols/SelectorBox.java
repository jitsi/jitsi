/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.main.utils.SelectorBoxItem;

public class SelectorBox extends SIPCommButton
	implements ActionListener{

	private AntialiasedPopupMenu popup;

	private Object[] items;

	public SelectorBox() {
		
		super(	ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
				ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
				null);
		
		this.popup = new AntialiasedPopupMenu();
		
		this.popup.setInvoker(this);
		
		this.addActionListener(this);
	}
	
	public SelectorBox(Object[] items, SelectorBoxItem selectedItem) {
		
		super(	ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
				ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
				selectedItem.getIcon());
		
		this.items = items;
		
		this.popup = new AntialiasedPopupMenu();
		
		this.popup.setInvoker(this);
		
		this.addActionListener(this);
		
		this.init();
	}

	public void init() {

		for (int i = 0; i < items.length; i++) {

			if (items[i] instanceof SelectorBoxItem) {

				SelectorBoxItem status = (SelectorBoxItem) items[i];
				JMenuItem item = new JMenuItem(	status.getText(), 
												new ImageIcon(status.getIcon()));
				
				item.addActionListener(this);
				
				this.popup.add(item);
			}
		}		
	}

	public void addItem(String text, Icon icon){
		
		JMenuItem item = new JMenuItem(	text, icon);

		item.addActionListener(this);

		this.popup.add(item);
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
