/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * SIPCommButtonUI implementation.
 *  
 * @author Yana Stamcheva
 */
public class SIPCommButtonUI extends MetalButtonUI {
    
    private final static BufferedImage buttonBG 
        = ImageLoader.getImage(ImageLoader.BUTTON);
        
    private final static BufferedImage buttonRolloverBG
        = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER);
        
    // ********************************
    //          Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommButtonUI();
    }
 
    // ********************************
    //          Install
    // ********************************
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(true);        
        b.setRolloverEnabled(true);
    }

    public void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setRolloverEnabled(false);
    }
    
    public void paint(Graphics g, JComponent c) {
    
        AntialiasingManager.activateAntialiasing(g);
        
        AbstractButton button = (AbstractButton)c;
        ButtonModel model = button.getModel();
        
        BufferedImage leftImg;
        BufferedImage middleImg;
        BufferedImage rightImg;
        
        int imgWidth;
        int imgHeight;
        int indentWidth  = 10;
        if(model.isRollover()){
            imgWidth = buttonRolloverBG.getWidth();
            imgHeight = buttonRolloverBG.getHeight();
           
            leftImg = buttonRolloverBG.getSubimage(0, 0, indentWidth, imgHeight);
            middleImg = buttonRolloverBG.getSubimage(indentWidth, 0, 
                                                     imgWidth-2*indentWidth, 
                                                     imgHeight);
            rightImg = buttonRolloverBG.getSubimage(imgWidth-indentWidth, 0, 
                                                    indentWidth, imgHeight);
        }
        else{
            imgWidth = buttonBG.getWidth();
            imgHeight = buttonBG.getHeight();
           
            leftImg = buttonBG.getSubimage(0, 0, 10, imgHeight);
            middleImg = buttonBG.getSubimage(10, 0, imgWidth-20, imgHeight);
            rightImg = buttonBG.getSubimage(imgWidth-10, 0, 10, imgHeight);        
        }
        
        
        g.drawImage(leftImg, 0, 0, indentWidth, c.getHeight(), null);
        g.drawImage(middleImg, indentWidth, 0, 
                c.getWidth()-2*indentWidth, c.getHeight(), null);
        g.drawImage(rightImg, c.getWidth()-indentWidth, 0, 
                indentWidth, c.getHeight(), null);
        
        super.paint(g, c);    
    }
       
    
    protected void paintFocus(Graphics g, AbstractButton b,
            Rectangle viewRect, Rectangle textRect, Rectangle iconRect){
        
        Graphics2D g2 = (Graphics2D)g;
        
        Rectangle focusRect = new Rectangle();
        String text = b.getText();
        boolean isIcon = b.getIcon() != null;
        
        // If there is text
        if ( text != null && !text.equals( "" ) ) {
            if ( !isIcon ) {
                focusRect.setBounds( textRect );
            }
            else {    
                focusRect.setBounds( iconRect.union( textRect ) );
            }
        }
        // If there is an icon and no text
        else if ( isIcon ) {
            focusRect.setBounds( iconRect );
        }

        g2.setStroke(new BasicStroke(0.5f,// Width
                BasicStroke.CAP_ROUND,    // End cap
                BasicStroke.JOIN_ROUND,   // Join style
                10.0f,                    // Miter limit
                new float[] {1.0f,1.0f},// Dash pattern
                2.0f));
        g2.setColor(Color.GRAY);
        g2.drawRoundRect((focusRect.x-3), (focusRect.y-3),
                focusRect.width+4, focusRect.height+4, 5, 5);        
    }
 
    /**
     * Overriden to do nothing.
     */
    protected void paintButtonPressed(Graphics g, AbstractButton b){        
    }
    
    
}
