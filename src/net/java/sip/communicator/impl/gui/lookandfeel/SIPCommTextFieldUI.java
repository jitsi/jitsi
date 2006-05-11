/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalTextFieldUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * SIPCommTextFieldUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTextFieldUI extends MetalTextFieldUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommTextFieldUI();
    }
    
    protected void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }
}
