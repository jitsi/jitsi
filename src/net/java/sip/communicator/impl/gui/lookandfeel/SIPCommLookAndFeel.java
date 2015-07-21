/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The default SIP-Communicator look&feel.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommLookAndFeel
    extends MetalLookAndFeel
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * Returns <tt>false</tt> to indicate that this is not a native look&feel.
     *
     * @return <tt>false</tt> to indicate that this is not a native look&feel
     */
    @Override
    public boolean isNativeLookAndFeel()
    {
        return false;
    }

    /**
     * Returns <tt>true</tt> to indicate that this look&feel is supported.
     *
     * @return <tt>false</tt> to indicate that this look&feel is supported
     */
    @Override
    public boolean isSupportedLookAndFeel()
    {
        return true;
    }

    /**
     * Returns the description of this look&feel.
     *
     * @return the description of this look&feel
     */
    @Override
    public String getDescription()
    {
        return "The Jitsi look and feel.";
    }

    /**
     * Returns the identifier of this look&feel.
     *
     * @return the identifier of this look&feel
     */
    @Override
    public String getID()
    {
        return "SIPCommunicator";
    }

    /**
     * Returns the name of this look&feel.
     *
     * @return  the name of this look&feel
     */
    @Override
    public String getName()
    {
        return "SIPCommLookAndFeel";
    }

    /**
     * Initializes class defaults.
     *
     * @param table the default user interface configurations table
     */
    @Override
    protected void initClassDefaults(UIDefaults table)
    {
        super.initClassDefaults(table);

        String lfPackageName = "net.java.sip.communicator.impl.gui.lookandfeel.";

        Object[] uiDefaults = {
                "ButtonUI", lfPackageName + "SIPCommButtonUI",
                "ToggleButtonUI", lfPackageName + "SIPCommToggleButtonUI",
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
                "ToolBarUI", lfPackageName + "SIPCommToolBarUI",
                "ToolBarSeparatorUI", lfPackageName + "SIPCommToolBarSeparatorUI",
                "TabbedPaneUI", "net.java.sip.communicator.plugin.desktoputil.plaf.SIPCommTabbedPaneEnhancedUI",
                "ToolTipUI", lfPackageName + "SIPCommToolTipUI",
                "TextAreaUI", lfPackageName + "SIPCommTextAreaUI",
                "TextPaneUI", lfPackageName + "SIPCommTextPaneUI",
                "CheckBoxUI", lfPackageName + "SIPCommCheckBoxUI",
                "ListUI", lfPackageName + "SIPCommListUI",
                "PopupMenuUI", lfPackageName + "SIPCommPopupMenuUI",
                "SpinnerUI", lfPackageName + "SIPCommSpinnerUI"
        };
        table.putDefaults(uiDefaults);
    }

    /**
     * Returns the disabled icon for the given <tt>component</tt>, based on the
     * given <tt>icon</tt>.
     *
     * @param component the component, for which we make a disabled icon
     * @param icon the icon to make a disabled version for
     *
     * @return the created icon
     */
    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon)
    {
        if (icon instanceof ImageIcon)
        {
            return new IconUIResource(new ImageIcon(LightGrayFilter.
                   createDisabledImage(((ImageIcon)icon).getImage())));
        }
        return null;
    }

    /**
     * Reloads look&feel.
     */
    public void loadSkin()
    {
        initClassDefaults(UIManager.getDefaults());
        if(getCurrentTheme() != null
            && getCurrentTheme() instanceof SIPCommDefaultTheme)
        {
            ((SIPCommDefaultTheme)getCurrentTheme()).loadSkin();
            setCurrentTheme(getCurrentTheme());
        }
    }
}
