package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * SIPCommToolBarUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommToolBarUI extends MetalToolBarUI {

    /**
     * Creates a new SIPCommToolBarUI instance.
     */
    public static ComponentUI createUI(JComponent x) {        
        return new SIPCommToolBarUI();
    }

    public void paint(Graphics g, JComponent c) {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);        
    }
}
