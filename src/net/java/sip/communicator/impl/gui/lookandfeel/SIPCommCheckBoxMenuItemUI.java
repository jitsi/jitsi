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
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
/**
 * The SIPCommCheckBoxMenuItemUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommCheckBoxMenuItemUI 
    extends BasicCheckBoxMenuItemUI {
    /**
     * Creates a new SIPCommCheckBoxMenuItemUI instance.
     */
    public static ComponentUI createUI(JComponent x) {
        return new SIPCommCheckBoxMenuItemUI();
    }

    public void paint(Graphics g, JComponent c) {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);        
    }
}
