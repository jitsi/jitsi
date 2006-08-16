/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.impl.gui.utils.*;

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
                "SplitPaneUI", lfPackageName + "SIPCommSplitPaneUI",
                "ScrollBarUI", lfPackageName + "SIPCommScrollBarUI",
                "ComboBoxUI", lfPackageName + "SIPCommComboBoxUI",
                "TextFieldUI", lfPackageName + "SIPCommTextFieldUI",
                "PasswordFieldUI", lfPackageName + "SIPCommPasswordFieldUI",
                "LabelUI", lfPackageName + "SIPCommLabelUI",
                "EditorPaneUI", lfPackageName + "SIPCommEditorPaneUI",
                "MenuItemUI", lfPackageName + "SIPCommMenuItemUI",
                "CheckBoxMenuItemUI", lfPackageName + "SIPCommCheckBoxMenuItemUI",
                "MenuUI", lfPackageName + "SIPCommMenuUI",
                "ToolBarSeparatorUI", lfPackageName + "SIPCommToolBarSeparatorUI",
                "TabbedPaneUI", lfPackageName + "SIPCommTabbedPaneUI",
                "ToolTipUI", lfPackageName + "SIPCommToolTipUI",
                "TextAreaUI", lfPackageName + "SIPCommTextAreaUI",
                "TextPaneUI", lfPackageName + "SIPCommTextPaneUI",
                "CheckBoxUI", lfPackageName + "SIPCommCheckBoxUI"
        };
        table.putDefaults(uiDefaults);
    }
    
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (icon instanceof ImageIcon) {
            return new IconUIResource(new ImageIcon(LightGrayFilter.
                   createDisabledImage(((ImageIcon)icon).getImage())));
        }
        return null;
    }
}
