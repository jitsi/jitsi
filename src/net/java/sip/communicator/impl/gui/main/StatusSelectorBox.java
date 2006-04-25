/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedPopupMenu;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;
import net.java.sip.communicator.util.Logger;

public class StatusSelectorBox extends JLabel
    implements MouseListener {

    private AntialiasedPopupMenu popup;

    private Map itemsMap;
    
    private Logger logger = Logger.getLogger(StatusSelectorBox.class.getName());
    
    private MainFrame mainFrame;
    
    private BufferedImage[] animatedImageArray;
    
    private Image backgroundImage = ImageLoader
        .getImage(ImageLoader.STATUS_SELECTOR_BOX);
    
    private Connecting connecting = new Connecting();
    
    private ProtocolProviderService protocolProvider;
    
    public StatusSelectorBox(	MainFrame mainFrame,
    							ProtocolProviderService protocolProvider) {
        
        this.setPreferredSize(new Dimension(
                                this.backgroundImage.getWidth(this),
                                this.backgroundImage.getHeight(this)));
        
        this.setVerticalAlignment(JLabel.CENTER);
        
        this.setHorizontalAlignment(JLabel.CENTER);
        
        this.mainFrame = mainFrame;
        
        this.protocolProvider = protocolProvider;
                
        this.popup = new AntialiasedPopupMenu();
        
        this.popup.setInvoker(this);
        
        this.addMouseListener(this);
    }
    
    public StatusSelectorBox(   MainFrame mainFrame,
                                ProtocolProviderService protocolProvider,
                                Map itemsMap, 
                                Image selectedItem) {
       
        this.setPreferredSize(new Dimension(
                this.backgroundImage.getWidth(this),
                this.backgroundImage.getHeight(this)));
       
        this.setVerticalAlignment(JLabel.CENTER);
        
        this.setHorizontalAlignment(JLabel.CENTER);
        
        this.setIcon(new ImageIcon(selectedItem));
        
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
        this.itemsMap = itemsMap;
        
        this.popup = new AntialiasedPopupMenu();
        
        this.popup.setInvoker(this);
        
        this.addMouseListener(this);
        
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
            
            item.addActionListener(new ItemActionListener());
            
            this.popup.add(item);
        
        }       
    }

    public void addItem(String text, Icon icon){
        
        JMenuItem item = new JMenuItem( text, icon);

        item.addActionListener(new ItemActionListener());

        this.popup.add(item);
    }
    
    private class ItemActionListener implements ActionListener{
        
        public void actionPerformed (ActionEvent e) {
            
            if (e.getSource() instanceof JMenuItem){
                
                JMenuItem menuItem = (JMenuItem) e.getSource();            
                       
                OperationSetPresence presence 
                    = mainFrame.getProtocolPresence(protocolProvider);
                
                Iterator statusSet = presence.getSupportedStatusSet();
                
                while (statusSet.hasNext()){
                    
                    PresenceStatus status 
                        = ((PresenceStatus)statusSet.next());
                    
                    if(status.getStatusName().equals(menuItem.getText())
                            && !presence.getPresenceStatus()
                                .equals(status)){
                        
                        try {
                            
                            if(status.equals(IcqStatusEnum.ONLINE)){
                                
                                if(protocolProvider.isRegistered()){
                                  
                                    presence
                                            .publishPresenceStatus(status, "");
                                }
                                else{
                                    protocolProvider.register(null);                                
                                }
                            }
                            else if(status.equals(IcqStatusEnum.OFFLINE)){
                                protocolProvider.unregister();
                            }
                            else {                      
                                
                                presence.publishPresenceStatus(status, "");
                            }                    
                        } catch (IllegalArgumentException e1) {
                            
                            logger.error("Error - changing status", e1);
                            
                        } catch (IllegalStateException e1) {
                            
                            logger.error("Error - changing status", e1);
                            
                        } catch (OperationFailedException e1) {
                            
                            if(e1.getErrorCode() 
                                    == OperationFailedException.GENERAL_ERROR){
                            
                                JOptionPane.showMessageDialog(
                                        null,
                                        Messages.getString
                                            ("statusChangeGeneralError"),
                                        Messages.getString
                                            ("generalError"),                                        
                                        JOptionPane.ERROR_MESSAGE);
                            }
                            else if(e1.getErrorCode()
                                    == OperationFailedException.NETWORK_FAILURE){
                                
                                JOptionPane.showMessageDialog(
                                        null,
                                        Messages.getString
                                            ("statusChangeNetworkFailure"),
                                        Messages.getString
                                            ("networkFailure"), 
                                        JOptionPane.ERROR_MESSAGE);
                            }
                            else if(e1.getErrorCode()
                                    == OperationFailedException
                                           .PROVIDER_NOT_REGISTERED){
                                
                                JOptionPane.showMessageDialog(
                                        null,                                         
                                        Messages.getString
                                            ("statusChangeNetworkFailure"),
                                        Messages.getString
                                            ("networkFailure"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                            
                            logger.error("Error - changing status", e1);
                        }                        
                        break;
                    }                
                }                
                setIcon(menuItem.getIcon());
            }
        } 
    }
    
    public void startConnecting(BufferedImage[] images){
        
        this.animatedImageArray = images;
        
        this.setIcon(new ImageIcon(images[0]));
        
        this.connecting.start();
    }
    
    public void stopConnecting(){
        
        this.connecting.stop();
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
        // TODO Auto-generated method stub
        
    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    
    
    private class Connecting extends Timer {
         
        public Connecting(){      
            
            super(100, null);
            
            this.addActionListener(new TimerActionListener());
        }
        
        private class TimerActionListener implements ActionListener {
            
            int j = 1;
            
            public void actionPerformed(ActionEvent evt) {
                
                StatusSelectorBox.this.setIcon(new ImageIcon(animatedImageArray[j]));
                j = (j+1) % animatedImageArray.length;
                
            }
            
        }
    }
}
