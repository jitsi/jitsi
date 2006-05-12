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
import javax.swing.plaf.basic.BasicMenuUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * The SIPCommMenuUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenuUI extends BasicMenuUI {
    /**
     * Creates a new SIPCommMenuUI instance.
     */
    public static ComponentUI createUI(JComponent x) {
        return new SIPCommMenuUI();
    }

    public void paint(Graphics g, JComponent c) {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }
}
