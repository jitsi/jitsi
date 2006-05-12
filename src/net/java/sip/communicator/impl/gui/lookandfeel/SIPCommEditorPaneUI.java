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
import javax.swing.plaf.basic.BasicEditorPaneUI;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * The SIPCommEditorPaneUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommEditorPaneUI extends BasicEditorPaneUI {

    /**
     * Creates a new SIPCommEditorPaneUI instance.
     */
    public static ComponentUI createUI(JComponent x) {
        return new SIPCommEditorPaneUI();
    }

    public void paintSafely(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);        
    }
}
