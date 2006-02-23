/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JTextField;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class SIPCommTextField extends JTextField {

    public SIPCommTextField(){
        
        super();
    }
    
    public SIPCommTextField(int columns){
        
        super(columns);
    }
    
    protected void paintBorder(Graphics g){
        
        Graphics2D g2 = (Graphics2D)g;
        
        g2.setColor(Constants.TOOLBAR_SEPARATOR_COLOR);
        
        g2.setStroke(new BasicStroke(3.0f));
        
        g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 7, 7);        
    }
    
    protected void paintComponent(Graphics g){
        
        AntialiasingManager.activateAntialiasing(g);
        
        super.paintComponent(g);
    }
}
