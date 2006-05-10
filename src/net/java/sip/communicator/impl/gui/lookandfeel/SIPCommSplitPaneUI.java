/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.metal.MetalSplitPaneUI;

/**
 * SIP-Communicator split pane.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommSplitPaneUI extends MetalSplitPaneUI {
    /**
     * Creates a new MetalSplitPaneUI instance
     */
   public static ComponentUI createUI(JComponent x) {
    return new SIPCommSplitPaneUI();
   }  
   /**
     * Creates the default divider.
     */
   public BasicSplitPaneDivider createDefaultDivider() {       
       return new SIPCommSplitPaneDivider(this);
   }  
}
