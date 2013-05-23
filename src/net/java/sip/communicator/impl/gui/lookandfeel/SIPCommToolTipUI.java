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
 * SIPCommToolTipUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommToolTipUI
    extends BasicToolTipUI
{

    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommToolTipUI();
    }

    @Override
    public void paint(Graphics g, JComponent c)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }
}
