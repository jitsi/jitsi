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
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.JTextComponent;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * SIPCommTextPaneUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTextPaneUI extends BasicTextPaneUI {
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommTextPaneUI();
    }
    
    protected void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }
}
