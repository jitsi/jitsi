/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 *  SIPCommPopupMenuUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommPopupMenuUI extends BasicPopupMenuUI
{
    /**
     * Creates a new SIPCommPopupMenuUI instance.
     */
    public static ComponentUI createUI(JComponent c) {
        return new SIPCommPopupMenuUI();
    }
}
