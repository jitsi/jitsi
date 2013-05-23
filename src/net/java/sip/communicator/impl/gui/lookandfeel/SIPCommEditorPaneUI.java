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

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The SIPCommEditorPaneUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommEditorPaneUI extends BasicEditorPaneUI {

    /**
     * Creates a new SIPCommEditorPaneUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommEditorPaneUI();
    }

    @Override
    public void paintSafely(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }
}
