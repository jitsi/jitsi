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
 * SIPCommTextPaneUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommTextPaneUI
    extends BasicTextPaneUI
{
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTextPaneUI();
    }

    @Override
    protected void paintSafely(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paintSafely(g);
    }
}
