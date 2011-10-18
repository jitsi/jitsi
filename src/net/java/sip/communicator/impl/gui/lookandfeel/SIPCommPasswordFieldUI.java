/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

import net.java.sip.communicator.util.swing.*;

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
