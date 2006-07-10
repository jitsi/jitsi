/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;

/**
 * SIPCommToolBarSeparatorUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommToolBarSeparatorUI extends BasicToolBarSeparatorUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommToolBarSeparatorUI();
    }
    
    public void paint(Graphics g, JComponent c) {        
        Graphics2D g2 = (Graphics2D)g;
        
        g2.setColor(UIManager.getColor("ToolBar.separatorColor"));
        g2.drawLine(c.getWidth()/2, 0, c.getWidth()/2, c.getHeight());
    }
}
