/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedPopupMenu;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;

public class StatusSelectorBox extends SIPCommButton
    implements ActionListener{

    private AntialiasedPopupMenu popup;

    private Map itemsMap;
    
    private OperationSetPresence presence;
    
    public StatusSelectorBox() {
        
        super(  ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
                ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
                null);
        
        this.popup = new AntialiasedPopupMenu();
        
        this.popup.setInvoker(this);
        
        //this.addActionListener(this);
    }
    
    public StatusSelectorBox(Map itemsMap, Image selectedItem) {
       
        super(  ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
                ImageLoader.getImage(ImageLoader.STATUS_SELECTOR_BOX),
                selectedItem);
        
        this.itemsMap = itemsMap;
        
        this.popup = new AntialiasedPopupMenu();
        
        this.popup.setInvoker(this);
        
        this.addActionListener(this);
        
        this.init();
    }

    public void init() {

        Iterator iter = itemsMap.entrySet().iterator();
        
        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry)iter.next();
        
            JMenuItem item 
                = new JMenuItem
                    (((IcqStatusEnum)entry.getKey()).getStatusName(), 
                     new ImageIcon((Image)entry.getValue()));
            
            item.addActionListener(this);
            
            this.popup.add(item);
        
        }       
    }

    public void addItem(String text, Icon icon){
        
        JMenuItem item = new JMenuItem( text, icon);

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
                   
            Iterator statusSet = this.presence.getSupportedStatusSet();
            
            while (statusSet.hasNext()){
                
                PresenceStatus status 
                    = ((PresenceStatus)statusSet.next());
                
                if(status.getStatusName().equals(menuItem.getText())
                        && !this.presence.getPresenceStatus().equals(status)){
                    
                    try {
                        if(!status.equals(IcqStatusEnum.OFFLINE))
                            this.presence.publishPresenceStatus(status, "");
                    
                    } catch (IllegalArgumentException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IllegalStateException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (OperationFailedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    
                    break;
                }
                
            }
            
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

    public OperationSetPresence getPresence() {
        return presence;
    }

    public void setPresence(OperationSetPresence presence) {
        this.presence = presence;
    }
}
