/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.util.swing.*;

/**
 * SIPCommComboBoxUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommComboBoxUI
    extends BasicComboBoxUI
{
    /**
     * Creates the UI for the given component <tt>c</tt>.
     * @param c the component to create an UI for.
     * @return the created ComponentUI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommComboBoxUI();
    }

    /**
     * Paints the UI for the given component through the given graphics object.
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the component to paint
     */
    public void paint(Graphics g, JComponent c)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }

    /**
     * Creates the editor of the combo box related to this UI.
     * @return the created combo box editor
     */
    protected ComboBoxEditor createEditor()
    {
        return new SIPCommComboBoxEditor.UIResource();
    }
}
