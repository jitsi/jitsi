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
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * SIPCommButtonUI implementation.
 *  
 * @author Yana Stamcheva
 */
public class SIPCommButtonUI extends MetalButtonUI {
    
    private final static Image buttonLeft 
        = ImageLoader.getImage(ImageLoader.BUTTON_LEFT);
    
    private final static Image buttonMiddle 
        = ImageLoader.getImage(ImageLoader.BUTTON_MIDDLE);
    
    private final static Image buttonRight 
        = ImageLoader.getImage(ImageLoader.BUTTON_RIGHT);
    
    private final static Image buttonRolloverLeft 
        = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER_LEFT);
    
    private final static Image buttonRolloverMiddle 
        = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER_MIDDLE);
    
    private final static Image buttonRolloverRight
        = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER_RIGHT);
    
    private final static SIPCommButtonUI sipCommButtonUI = new SIPCommButtonUI();
    
//  ********************************
    //          Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        return sipCommButtonUI;
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
    
        AbstractButton button = (AbstractButton)c;
        ButtonModel model = button.getModel();
        
        Image leftImg;
        Image middleImg;
        Image rightImg;
        
        if(model.isRollover()){
           
            leftImg = buttonRolloverLeft;
            middleImg = buttonRolloverMiddle;
            rightImg = buttonRolloverRight;
        }
        else{
            leftImg = buttonLeft;
            middleImg = buttonMiddle;
            rightImg = buttonRight;        
        }
        
        int indentWidth  = leftImg.getWidth(null);
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
