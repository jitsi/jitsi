/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.swing.*;

/**
 * SIPCommComboBoxUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommComboBoxUI
    extends MetalComboBoxUI
{
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommComboBoxUI();
    }

    public void paint(Graphics g, JComponent c)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }

    protected ComboBoxEditor createEditor()
    {
        return new SIPCommComboBoxEditor.UIResource();
    }
}
