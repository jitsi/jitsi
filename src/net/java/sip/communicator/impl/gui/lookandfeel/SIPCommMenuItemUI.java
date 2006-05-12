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
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
/**
 * The SIPCommMenuItemUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenuItemUI extends BasicMenuItemUI {
    /**
     * Creates a new SIPCommMenuItemUI instance.
     */
    public static ComponentUI createUI(JComponent x) {
        return new SIPCommMenuItemUI();
    }

    public void paint(Graphics g, JComponent c) {        
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);        
    }
}
