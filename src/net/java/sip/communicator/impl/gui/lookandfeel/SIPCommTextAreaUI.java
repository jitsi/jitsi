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
import javax.swing.plaf.basic.BasicTextAreaUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * SIPCommTextAreaUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTextAreaUI extends BasicTextAreaUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommTextAreaUI();
    }
    
    protected void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);        
    }
}
