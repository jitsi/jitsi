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

import javax.swing.ComboBoxEditor;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * SIPCommComboBoxUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommComboBoxUI extends MetalComboBoxUI {
    
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommComboBoxUI();
    }
    
    public void paint(Graphics g, JComponent c) {        
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);        
    }
    
    protected ComboBoxEditor createEditor() {
        return new SIPCommComboBoxEditor.UIResource();
    }
}
