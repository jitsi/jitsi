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

import net.java.sip.communicator.util.swing.*;

/**
 * SIPCommTextAreaUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTextAreaUI extends BasicTextAreaUI {

    public static ComponentUI createUI(JComponent c) {
        c.setOpaque(false);
        return new SIPCommTextAreaUI();
    }
    
    protected void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);        
    }
}
