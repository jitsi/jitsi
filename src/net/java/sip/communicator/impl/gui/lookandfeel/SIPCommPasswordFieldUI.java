/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.text.JTextComponent;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
/**
 * The SIPCommPasswordFieldUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommPasswordFieldUI extends BasicPasswordFieldUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommPasswordFieldUI();
    }
    
    protected void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }
    
    protected void paintBackground(Graphics g) {        
        JTextComponent c = this.getComponent();
        g.setColor(c.getBackground());
        g.fillRoundRect(1, 1, c.getWidth()-2, c.getHeight()-2, 5, 5);
    }
}
