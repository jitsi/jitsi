/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * SIPCommSelectorBox is very similar to a JComboBox. It 
 * allows having a list of icon-text pairs and when choosing
 * on of them, to show only the selected icon without the text.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommSelectorBox extends JLabel 
    implements MouseListener {

    private JPopupMenu popup;
    
    private Object selectedItem;
    
    private Image backgroundImage = ImageLoader
        .getImage(ImageLoader.STATUS_SELECTOR_BOX);
    
    private Object selectedObject;
    
    public SIPCommSelectorBox(){
        
        this.setPreferredSize(new Dimension(
                this.backgroundImage.getWidth(this),
                this.backgroundImage.getHeight(this)));
        
        this.setVerticalAlignment(JLabel.CENTER);
        
        this.setHorizontalAlignment(JLabel.CENTER);
        
        this.popup = new JPopupMenu();
        
        this.popup.setInvoker(this);
        
        this.addMouseListener(this);        
    }
    
    public SIPCommSelectorBox(Object selectedItem){
        this();
        
        this.selectedItem = selectedItem;
        
        if(selectedItem instanceof Image)
            this.setIcon(new ImageIcon((Image)selectedItem));
        else
            this.setText(selectedItem.toString());
    }
        
    public void addItem(String text, Icon icon, 
                ActionListener actionListener){
        
        JMenuItem item = new JMenuItem(text, icon);

        item.addActionListener(actionListener);

        this.popup.add(item);
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
    
    protected void paintComponent(Graphics g){
        
        AntialiasingManager.activateAntialiasing(g);
                
        g.drawImage(this.backgroundImage, 0, 0, this);
        
        super.paintComponent(g);
    }
    
    public void mouseClicked(MouseEvent e) {
        
        if (!this.popup.isVisible()) {
            this.popup.setLocation(calculatePopupLocation());
            this.popup.setVisible(true);            
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
    
    public void setSelected(JMenuItem menuItem){        
        this.setIcon(menuItem.getIcon());
        this.setSelectedObject(menuItem.getText());
    }
    
    public void setSelectedObject(Object o){
        this.selectedObject = o;
    }
    
    public Object getSelectedObject(){
        return this.selectedObject;
    }
}
