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
import javax.swing.plaf.basic.BasicTreeUI;

/**
 * SIPCommTreeUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTreeUI extends BasicTreeUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommTreeUI();
    }
    
    protected void paintHorizontalLine(Graphics g, JComponent c, int y,
            int left, int right) {
    }
    
    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top,
            int bottom) {
    }
}
