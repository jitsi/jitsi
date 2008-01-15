/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.util.*;
import java.util.List;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * SipCommunicator default theme.
 * 
 * @author Yana Stamcheva
 */

public class SIPCommDefaultTheme
    extends DefaultMetalTheme
{
    /**
     * Used for tooltip borders, progress bar selection background, scroll bar
     * thumb shadow, tabbed pane focus, toolbar docking foreground
     */
    private static final ColorUIResource PRIMARY_CONTROL_DARK_SHADOW =
        new ColorUIResource(ColorResources.getColor("primaryControlDarkShadow"));

    /**
     * Used for desktop color, menu selected background, focus color, slider
     * foreground, progress bar foreground, combo box selection background,
     * scroll bar thumb
     */
    private static final ColorUIResource PRIMARY_CONTROL_SHADOW =
        new ColorUIResource(ColorResources.getColor("primaryControlShadow"));

    /**
     * Used for progress bar border, tooltip border inactive, tooltip foreground
     * inactive, scroll bar dark shadow.
     */
    private static final ColorUIResource CONTROL_DARK_SHADOW =
        new ColorUIResource(ColorResources.getColor("controlDarkShadow"));

    private static final ColorUIResource CONTROL_SHADOW =
        new ColorUIResource(ColorResources.getColor("controlShadow"));

    /**
     * Used for window title inactive background, menu background, tooltip
     * inactive background, combo box background, desktop icon background,
     * scroll bar background, tabbed pane tab area background.
     */
    private static final ColorUIResource CONTROL_COLOR =
        new ColorUIResource(ColorResources.getColor("controlColor"));

    /**
     * Used for text hightlight color, window title background, scroll bar thumb
     * hightlight, split pane devider focus color, Tree.line, Tree.hash,
     * ToolBar.floatingForeground
     */
    private static final ColorUIResource PRIMARY_CONTROL_COLOR
        = new ColorUIResource(ColorResources.getColor("primaryControlColor"));

    // Used to paint a gradient for a check box or a radio button.
    private static final ColorUIResource BUTTON_GRADIENT_DARK_COLOR
        = new ColorUIResource(ColorResources.getColor("buttonGradientDark"));

    private static final ColorUIResource BUTTON_GRADIENT_LIGHT_COLOR
        = new ColorUIResource(ColorResources.getColor("buttonGradientLight"));

    private static final ColorUIResource SLIDER_GRADIENT_DARK_COLOR
        = new ColorUIResource(ColorResources.getColor("sliderGradientDark"));

    private static final ColorUIResource SLIDER_GRADIENT_LIGHT_COLOR
        = new ColorUIResource(ColorResources.getColor("sliderGradientLight"));

    private static final ColorUIResource SELECTION_FOREGROUND
        = new ColorUIResource(ColorResources.getColor("selectionForeground"));

    private static final ColorUIResource SELECTION_BACKGROUND
        = new ColorUIResource(ColorResources.getColor("selectionBackground"));

    private static final ColorUIResource SPLIT_PANE_DEVIDER_FOCUS_COLOR
        = new ColorUIResource(ColorResources
            .getColor("splitPaneDeviderFocused"));

    private static final ColorUIResource TABBED_PANE_HIGHLIGHT_COLOR
        = new ColorUIResource(ColorResources
            .getColor("tabbedPaneBorderHighlight"));

    private static final ColorUIResource TABLE_GRID_COLOR
        = new ColorUIResource(ColorResources.getColor("tableGrid"));

    private static final ColorUIResource SCROLL_BAR_TRACK_HIGHLIGHT
        = new ColorUIResource(ColorResources
            .getColor("scrollBarTrackHighlight"));

    private static final ColorUIResource SCROLL_BAR_DARK_SHADOW
        = new ColorUIResource(ColorResources.getColor("scrollBarDarkShadow"));

    private static final ColorUIResource DESKTOP_BACKGROUND_COLOR
        = new ColorUIResource(ColorResources.getColor("desktopBackgroundColor"));

    private static final ColorUIResource CONTROL_TEXT_COLOR
        = new ColorUIResource(ColorResources.getColor("textColor"));

    private static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR
        = new ColorUIResource(ColorResources.getColor("inactiveTextColor"));

    private static final ColorUIResource MENU_DISABLED_FOREGROUND
        = new ColorUIResource(ColorResources
            .getColor("menuDisabledForeground"));

    private static final FontUIResource BASIC_FONT
        = new FontUIResource(Constants.FONT);

    public SIPCommDefaultTheme()
    {
    }

    public void addCustomEntriesToTable(UIDefaults table)
    {

        List buttonGradient
            = Arrays.asList(new Object[]
               { new Float(.3f), new Float(0f), BUTTON_GRADIENT_DARK_COLOR,
                getWhite(), BUTTON_GRADIENT_LIGHT_COLOR });

        List sliderGradient
            = Arrays.asList(new Object[]
               { new Float(.3f), new Float(.2f), SLIDER_GRADIENT_DARK_COLOR,
                getWhite(), SLIDER_GRADIENT_LIGHT_COLOR });

        Object textFieldBorder = SIPCommBorders.getTextFieldBorder();

        Object[] defaults =
            new Object[]
            {
                "Button.rollover", Boolean.TRUE,

                "CheckBox.rollover", Boolean.TRUE,
                "CheckBox.gradient", buttonGradient,

                "CheckBoxMenuItem.gradient", buttonGradient,

                "Menu.opaque", Boolean.FALSE,

                "MenuBar.border", null,

                "Menu.borderPainted", Boolean.FALSE,
                "Menu.border", textFieldBorder,
                "Menu.selectionBackground", SELECTION_BACKGROUND,
                "Menu.selectionForeground", SELECTION_FOREGROUND,
                "Menu.margin", new InsetsUIResource(0, 0, 0, 0),

                "MenuItem.borderPainted", Boolean.FALSE,
                "MenuItem.border", textFieldBorder,
                "MenuItem.selectionBackground", SELECTION_BACKGROUND,
                "MenuItem.selectionForeground", SELECTION_FOREGROUND,

                "CheckBoxMenuItem.borderPainted", Boolean.FALSE,
                "CheckBoxMenuItem.border", textFieldBorder,
                "CheckBoxMenuItem.selectionBackground", SELECTION_BACKGROUND,
                "CheckBoxMenuItem.selectionForeground", SELECTION_FOREGROUND,

                "InternalFrame.activeTitleGradient", buttonGradient,

                "OptionPane.warningIcon",
                new ImageIcon(ImageLoader.getImage(ImageLoader.WARNING_ICON)),

                "OptionPane.errorIcon",
                new ImageIcon(ImageLoader.getImage(ImageLoader.ERROR_ICON)),

                "OptionPane.infoIcon",
                new ImageIcon(ImageLoader.getImage(ImageLoader.INFO_ICON)),

                "RadioButton.gradient", buttonGradient,
                "RadioButton.rollover", Boolean.TRUE,

                "RadioButtonMenuItem.gradient", buttonGradient,

                "Spinner.arrowButtonBorder", SIPCommBorders.getTextFieldBorder(),

                "Slider.altTrackColor", SLIDER_GRADIENT_LIGHT_COLOR,
                "Slider.gradient", sliderGradient,
                "Slider.focusGradient", sliderGradient,

                "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
                "SplitPane.dividerFocusColor", SPLIT_PANE_DEVIDER_FOCUS_COLOR,
                "SplitPane.dividerSize", new Integer(5),

                "ScrollBar.width", new Integer(12),
                "ScrollBar.horizontalThumbIcon",
                ImageLoader.getImage(ImageLoader.SCROLLBAR_THUMB_HORIZONTAL),
                "ScrollBar.verticalThumbIcon",
                ImageLoader.getImage(ImageLoader.SCROLLBAR_THUMB_VERTICAL),
                "ScrollBar.horizontalThumbHandleIcon",
                ImageLoader
                    .getImage(ImageLoader.SCROLLBAR_THUMB_HANDLE_HORIZONTAL),
                "ScrollBar.verticalThumbHandleIcon",
                ImageLoader
                    .getImage(ImageLoader.SCROLLBAR_THUMB_HANDLE_VERTICAL),
                "ScrollBar.trackHighlight", SCROLL_BAR_TRACK_HIGHLIGHT,
                "ScrollBar.highlight", SELECTION_BACKGROUND,
                "ScrollBar.darkShadow", SCROLL_BAR_DARK_SHADOW,

                "TabbedPane.borderHightlightColor", TABBED_PANE_HIGHLIGHT_COLOR,
                "TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3),
                "TabbedPane.selected", SELECTION_BACKGROUND,
                "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
                "TabbedPane.unselectedBackground", SELECTION_BACKGROUND,

                "TextField.border", textFieldBorder,
                "TextField.margin", new InsetsUIResource(3, 3, 3, 3),

                "PasswordField.border", textFieldBorder,
                "PasswordField.margin", new InsetsUIResource(3, 3, 3, 3),

                "FormattedTextField.border", textFieldBorder,
                "FormattedTextField.margin", new InsetsUIResource(3, 3, 3, 3),

                "Table.gridColor", TABLE_GRID_COLOR,
                "Table.background", getDesktopColor(),

                "ToggleButton.gradient", buttonGradient,

                "ToolBar.isRollover", Boolean.TRUE,
                "ToolBar.separatorColor", PRIMARY_CONTROL_COLOR,
                "ToolBar.separatorSize", new DimensionUIResource(8, 22),

                "ToolTip.background", SELECTION_BACKGROUND,
                "ToolTip.backgroundInactive", SELECTION_BACKGROUND,
                "ToolTip.hideAccelerator", Boolean.FALSE,

                "TitledBorder.border", SIPCommBorders.getBoldRoundBorder()
            };

        table.putDefaults(defaults);
    }

    /**
     * Overriden to enable picking up the system fonts, if applicable.
     */
    boolean isSystemTheme()
    {
        return true;
    }

    public String getName()
    {
        return "SipCommunicator";
    }

    protected ColorUIResource getPrimary1()
    {
        return PRIMARY_CONTROL_DARK_SHADOW;
    }

    protected ColorUIResource getPrimary2()
    {
        return PRIMARY_CONTROL_SHADOW;
    }

    protected ColorUIResource getPrimary3()
    {
        return PRIMARY_CONTROL_COLOR;
    }

    protected ColorUIResource getSecondary1()
    {
        return CONTROL_DARK_SHADOW;
    }

    protected ColorUIResource getSecondary2()
    {
        return CONTROL_SHADOW;
    }

    protected ColorUIResource getSecondary3()
    {
        return CONTROL_COLOR;
    }

    protected ColorUIResource getBlack()
    {
        return CONTROL_TEXT_COLOR;
    }

    public ColorUIResource getDesktopColor()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    public ColorUIResource getWindowBackground()
    {
        return getWhite();
    }

    public ColorUIResource getControl()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    public ColorUIResource getMenuBackground()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    public ColorUIResource getInactiveControlTextColor()
    {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    public ColorUIResource getMenuDisabledForeground()
    {
        return MENU_DISABLED_FOREGROUND;
    }

    public FontUIResource getControlTextFont()
    {
        return BASIC_FONT;
    }

    public FontUIResource getSystemTextFont()
    {
        return BASIC_FONT;
    }

    public FontUIResource getUserTextFont()
    {
        return BASIC_FONT;
    }

    public FontUIResource getMenuTextFont()
    {
        return BASIC_FONT;
    }

    public FontUIResource getWindowTitleFont()
    {
        return BASIC_FONT;
    }

    public FontUIResource getSubTextFont()
    {
        return BASIC_FONT;
    }
}
