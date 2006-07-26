/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * SipCommunicator default theme.
 *
 * @author Yana Stamcheva
 */

public class SIPCommDefaultTheme
    extends DefaultMetalTheme
{
    private static final ColorUIResource DARK_BLUE
            = new ColorUIResource(0x3f6296);
    private static final ColorUIResource LIGHT_BLUE
            = new ColorUIResource(0x96A9C6);
    private static final ColorUIResource LIGHT_BLUE_GRAY
            = new ColorUIResource(0xD3DDED);
    private static final ColorUIResource LIGHT_GRAY
            = new ColorUIResource(0xEEEEEE);
    private static final ColorUIResource GRAY
            = new ColorUIResource(0xC0C0C0);
    private static final ColorUIResource VERY_LIGHT_GRAY
            = new ColorUIResource(0xFAFAFA);

    private static final ColorUIResource CONTROL_TEXT_COLOR
            = new ColorUIResource(0x333333);
    private static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR
            = new ColorUIResource(0x999999);
    private static final ColorUIResource MENU_DISABLED_FOREGROUND
            = new ColorUIResource(0x999999);
    private static final ColorUIResource OCEAN_BLACK
            = new ColorUIResource(0x333333);

    private static final FontUIResource BASIC_FONT
            = new FontUIResource(Constants.FONT);

    public SIPCommDefaultTheme() {
    }

    public void addCustomEntriesToTable(UIDefaults table) {

        List buttonGradient = Arrays.asList(
                 new Object[] {new Float(.3f), new Float(0f),
                 DARK_BLUE, getWhite(), getSecondary2()});

        List sliderGradient = Arrays.asList(new Object[] {
                new Float(.3f), new Float(.2f),
                DARK_BLUE, getWhite(), new ColorUIResource(LIGHT_BLUE_GRAY) });

        Object textFieldBorder = SIPCommBorders.getTextFieldBorder();

        Object[] defaults = new Object[] {
            "Button.rollover", Boolean.TRUE,

            "CheckBox.rollover", Boolean.TRUE,
            "CheckBox.gradient", buttonGradient,

            "CheckBoxMenuItem.gradient", buttonGradient,

            "Menu.opaque", Boolean.FALSE,

            "InternalFrame.activeTitleGradient", buttonGradient,

            "OptionPane.warningIcon",
                new ImageIcon(ImageLoader.getImage(ImageLoader.WARNING_ICON)),

            "RadioButton.gradient", buttonGradient,
            "RadioButton.rollover", Boolean.TRUE,

            "RadioButtonMenuItem.gradient", buttonGradient,

            "Slider.altTrackColor", new ColorUIResource(0xD2E2EF),
            "Slider.gradient", sliderGradient,
            "Slider.focusGradient", sliderGradient,

            "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
            "SplitPane.dividerFocusColor", LIGHT_BLUE_GRAY,
            "SplitPane.dividerSize", new Integer(5),

            "ScrollBar.width", new Integer(12),
            "ScrollBar.horizontalThumbIcon",
                ImageLoader.getImage(
                        ImageLoader.SCROLLBAR_THUMB_HORIZONTAL),
            "ScrollBar.verticalThumbIcon",
                ImageLoader.getImage(
                        ImageLoader.SCROLLBAR_THUMB_VERTICAL),
            "ScrollBar.horizontalThumbHandleIcon",
                ImageLoader.getImage(
                        ImageLoader.SCROLLBAR_THUMB_HANDLE_HORIZONTAL),
            "ScrollBar.verticalThumbHandleIcon",
                ImageLoader.getImage(
                        ImageLoader.SCROLLBAR_THUMB_HANDLE_VERTICAL),
            "ScrollBar.trackHighlight", GRAY,
            "ScrollBar.highlight", LIGHT_GRAY,
            "ScrollBar.darkShadow", GRAY,

            "TabbedPane.borderHightlightColor", LIGHT_BLUE,
            "TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3),
            "TabbedPane.selected", LIGHT_GRAY,
            "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
            "TabbedPane.unselectedBackground", LIGHT_GRAY,

            "TextField.border", textFieldBorder,
            "PasswordField.border", textFieldBorder,
            "FormattedTextField.border", textFieldBorder,

            "Table.gridColor", LIGHT_BLUE_GRAY,

            "ToggleButton.gradient", buttonGradient,

            "ToolBar.isRollover", Boolean.TRUE,
            "ToolBar.separatorColor", Constants.LIGHT_GRAY_COLOR,
            "ToolBar.separatorSize", new DimensionUIResource(8, 22),
            
            "ToolTip.background", LIGHT_GRAY,
            "ToolTip.backgroundInactive", LIGHT_GRAY,
            "ToolTip.hideAccelerator", Boolean.FALSE,
            
            "TitledBorder.border", SIPCommBorders.getRoundBorder()
        };
        table.putDefaults(defaults);
    }

    /**
     * Overriden to enable picking up the system fonts, if applicable.
     */
    boolean isSystemTheme() {
        return true;
    }

    public String getName() {
        return "SipCommunicator";
    }

    protected ColorUIResource getPrimary1() {
        return DARK_BLUE;
    }

    protected ColorUIResource getPrimary2() {
        return LIGHT_BLUE;
    }

    protected ColorUIResource getPrimary3() {
        return GRAY;
    }

    protected ColorUIResource getSecondary1() {
        return DARK_BLUE;
    }

    protected ColorUIResource getSecondary2() {
        return GRAY;
    }

    protected ColorUIResource getSecondary3() {
        return LIGHT_GRAY;
    }

    protected ColorUIResource getBlack() {
        return OCEAN_BLACK;
    }

    public ColorUIResource getDesktopColor() {
        return VERY_LIGHT_GRAY;
    }

    public ColorUIResource getWindowBackground() {
        return getWhite();
    }
    
    public ColorUIResource getControl(){
        return VERY_LIGHT_GRAY;
    }

    public ColorUIResource getMenuBackground(){
        return VERY_LIGHT_GRAY;
    }

    public ColorUIResource getInactiveControlTextColor() {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    public ColorUIResource getControlTextColor() {
        return CONTROL_TEXT_COLOR;
    }

    public ColorUIResource getMenuDisabledForeground() {
        return MENU_DISABLED_FOREGROUND;
    }

    public FontUIResource getControlTextFont() {
        return BASIC_FONT;
    }

    public FontUIResource getSystemTextFont() {
        return BASIC_FONT;
    }

    public FontUIResource getUserTextFont() {
        return BASIC_FONT;
    }

    public FontUIResource getMenuTextFont() {
        return BASIC_FONT;
    }

    public FontUIResource getWindowTitleFont() {
        return BASIC_FONT;
    }

    public FontUIResource getSubTextFont() {
        return BASIC_FONT;
    }
}
