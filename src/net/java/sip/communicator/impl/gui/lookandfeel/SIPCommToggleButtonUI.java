/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

public class SIPCommToggleButtonUI
    extends MetalToggleButtonUI
{
            
    // ********************************
    //          Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommToggleButtonUI();
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
     * Overriden to draw a round rectangle.
     */
    protected void paintButtonPressed(Graphics g, AbstractButton b){
        if ( b.isContentAreaFilled() ) {
            Dimension size = b.getSize();
            g.setColor(getSelectColor());
            g.fillRoundRect(0, 0, size.width, size.height, 5, 5);
        }
    }
}
