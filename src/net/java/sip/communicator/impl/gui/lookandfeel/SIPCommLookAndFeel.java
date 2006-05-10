/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import javax.swing.UIDefaults;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * The default SIP-Communicator look&feel.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommLookAndFeel extends MetalLookAndFeel {

    public boolean isNativeLookAndFeel() {
        return false;
    }

    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public String getDescription() {        
        return "The SIP-Communicator look and feel.";
    }

    public String getID() {        
        return "SIPCommunicator";
    }

    public String getName() {
        return "SIPCommLookAndFeel";
    }

  
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        
        String lfPackageName = "net.java.sip.communicator.impl.gui.lookandfeel.";

        Object[] uiDefaults = {
                "ButtonUI", lfPackageName + "SIPCommButtonUI",
                "SplitPaneUI", lfPackageName + "SIPCommSplitPaneUI"
        };
        table.putDefaults(uiDefaults);
    }
}
