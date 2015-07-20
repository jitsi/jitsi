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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.*;

/**
 * The tooltip shown over a contact in the contact list.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class ExtendedTooltip
    extends JToolTip
    implements AncestorListener,
               WindowFocusListener
{
    private static final Logger logger
        = Logger.getLogger(ExtendedTooltip.class);

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        ExtendedTooltip.class.getName() +  "ToolTipUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            ImageToolTipUI.class.getName());
    }

    private final JLabel imageLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();

    private final JPanel linesPanel = new JPanel();

    private final JTextArea bottomTextArea = new JTextArea();

    private int textWidth = 0;

    private int textHeight = 0;

    private boolean isListViewEnabled;

    /**
     * The parent window where this tooltip was created, not the one the
     * component was added to, but where the focus is when the component is
     * created/added.
     */
    private Window parentWindow = null;

    /**
     * Created a <tt>MetaContactTooltip</tt>.
     * @param isListViewEnabled indicates if the list view is enabled
     */
    public ExtendedTooltip(boolean isListViewEnabled)
    {
        this.isListViewEnabled = isListViewEnabled;

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        JPanel centerPanel = new JPanel(new BorderLayout());

        mainPanel.setOpaque(false);
        centerPanel.setOpaque(false);
        linesPanel.setOpaque(false);
        bottomTextArea.setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        if (isListViewEnabled)
        {
            linesPanel.setLayout(
                new BoxLayout(linesPanel, BoxLayout.Y_AXIS));

            mainPanel.add(imageLabel, BorderLayout.WEST);
            mainPanel.add(centerPanel, BorderLayout.CENTER);

            centerPanel.add(titleLabel, BorderLayout.NORTH);
            centerPanel.add(linesPanel, BorderLayout.CENTER);
        }
        else
        {
            titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainPanel.add(imageLabel, BorderLayout.CENTER);
            mainPanel.add(titleLabel, BorderLayout.NORTH);
        }

        bottomTextArea.setEditable(false);
        bottomTextArea.setLineWrap(true);
        bottomTextArea.setWrapStyleWord(true);
        bottomTextArea.setFont(bottomTextArea.getFont().deriveFont(10f));
        mainPanel.add(bottomTextArea, BorderLayout.SOUTH);

        this.addAncestorListener(this);

        this.add(mainPanel);
    }

    /**
     * Sets the given image to this tooltip.
     *
     * @param imageIcon The image icon to set.
     */
    public void setImage(ImageIcon imageIcon)
    {
        imageLabel.setIcon(imageIcon);
    }

    /**
     * Sets the title of the tooltip. The text would be shown in bold on the top
     * of the tooltip panel.
     *
     * @param titleText The title of the tooltip.
     */
    public void setTitle(String titleText)
    {
        titleLabel.setText(titleText);

        Dimension labelSize
            = ComponentUtils.getStringSize(titleLabel,titleText);
        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     */
    public void addLine(Icon icon, String text)
    {
        JLabel lineLabel = new JLabel(text, icon, JLabel.LEFT);

        linesPanel.add(lineLabel);

        Dimension labelSize = calculateLabelSize(lineLabel);

        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     * @param leftIndent left indent of the label
     */
    public void addSubLine(Icon icon,
                        String text,
                        int leftIndent)
    {
        JLabel lineLabel = new JLabel(  text,
                                        icon,
                                        JLabel.LEFT);

        lineLabel.setBorder(
            BorderFactory.createEmptyBorder(0, leftIndent, 0, 0));
        lineLabel.setFont(lineLabel.getFont().deriveFont(9f));
        lineLabel.setForeground(Color.DARK_GRAY);

        linesPanel.add(lineLabel);

        Dimension labelSize = calculateLabelSize(lineLabel);

        recalculateTooltipSize(labelSize.width + leftIndent, labelSize.height);
    }

    /**
     * Adds the given array of labels as one line in this tool tip.
     *
     * @param labels the labels to add
     */
    public void addLine(JLabel[] labels)
    {
        Dimension lineSize = null;
        JPanel labelPanel;

        if (labels.length > 0)
        {
            labelPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.LEFT, 2, 0));
            linesPanel.add(labelPanel);
        }
        else
            return;

        for (JLabel label : labels)
        {
            labelPanel.add(label);
            if (lineSize == null)
                lineSize = calculateLabelSize(label);
            else
                lineSize = new Dimension(
                    lineSize.width + calculateLabelSize(label).width,
                    lineSize.height);
        }

        if (lineSize != null)
            recalculateTooltipSize(lineSize.width, lineSize.height);
    }

    /**
     * Clear all lines.
     */
    public void removeAllLines()
    {
        linesPanel.removeAll();
    }

    /**
     * Sets the text that would appear on the bottom of the tooltip.
     * @param text the text to set
     */
    public void setBottomText(String text)
    {
        this.bottomTextArea.setText(text);
    }

    /**
     * Calculates label size.
     *
     * @param label the label, which size we should calculate
     * @return the Dimension indicating the label size
     */
    private Dimension calculateLabelSize(JLabel label)
    {
        Icon icon = label.getIcon();
        String text = label.getText();

        int iconWidth = 0;
        int iconHeight = 0;
        if (icon != null)
        {
            iconWidth = icon.getIconWidth();
            iconHeight = icon.getIconHeight();
        }

        int labelWidth
            = ComponentUtils.getStringWidth(label, text)
                + iconWidth
                + label.getIconTextGap();

        int textHeight = ComponentUtils.getStringSize(label, text).height;

        int labelHeight = (iconHeight > textHeight) ? iconHeight : textHeight;

        return new Dimension(labelWidth, labelHeight);
    }

    /**
     * Re-calculates the tooltip size.
     *
     * @param newTextWidth the width of the newly added text that should be
     * added to the global width
     * @param newTextHeight the height of the newly added text that should be
     * added to the global height
     */
    private void recalculateTooltipSize(int newTextWidth, int newTextHeight)
    {
        if (textWidth < newTextWidth)
            textWidth = newTextWidth;

        textHeight += newTextHeight;
    }

    /**
     * When main windows focus is lost hide the tooltip.
     * @param e window event.
     */
    @Override
    public void windowLostFocus(WindowEvent e)
    {
        Window popupWindow
            = SwingUtilities.getWindowAncestor(
                ExtendedTooltip.this);

        if ((popupWindow != null)
            && popupWindow.isVisible()
            // The popup window should normally be a JWindow, so we
            // check here explicitly if for some reason we didn't
            // get something else.
            && (popupWindow instanceof JWindow))
        {
            if (logger.isInfoEnabled())
                logger.info("Tooltip window ancestor to hide: "
                    + popupWindow);

            popupWindow.setVisible(false);
        }
    }

    /**
     * Not used.
     * @param e window event
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {}

    /**
     * @param event ancestor event, something has become visible.
     */
    @Override
    public void ancestorAdded(AncestorEvent event)
    {
        this.parentWindow
            = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow();

        // Hide the tooltip when the parent window hides.
        if (parentWindow != null)
        {
            parentWindow.addWindowFocusListener(this);
        }
        else
        {
            // well, no parent window, this is the case when we hovered over a
            // contact and tooltip started creating and we switched to another
            // window or another window stole our focus, we will hide the
            // tooltip in order to avoid showing it over non jitsi window
            // seems only a problem when using java 1.6 (under macosx),
            // not reproducible with 1.7.
            Window popupWindow
                = SwingUtilities.getWindowAncestor(
                ExtendedTooltip.this);

            if ((popupWindow != null)
                // The popup window should normally be a JWindow, so we
                // check here explicitly if for some reason we didn't
                // get something else.
                && (popupWindow instanceof JWindow))
            {
                popupWindow.setVisible(false);
            }
        }
    }

    /**
     * When the tooltip window is disposed elements are removed from it
     * and this is the time to clear resources.
     * @param event the component has become not visible
     */
    @Override
    public void ancestorRemoved(AncestorEvent event)
    {
        if(this.parentWindow != null)
        {
            this.parentWindow.removeWindowFocusListener(this);
            this.parentWindow = null;
        }

        this.removeAncestorListener(this);
    }

    /**
     * Not used.
     * @param event ancestor event.
     */
    @Override
    public void ancestorMoved(AncestorEvent event)
    {}

    /**
     * Customized UI for this MetaContactTooltip.
     */
    public static class ImageToolTipUI extends MetalToolTipUI
    {
        static ImageToolTipUI sharedInstance = new ImageToolTipUI();

        /**
         * Creates the UI.
         * @param c component
         * @return ui shared instance.
         */
        public static ComponentUI createUI(JComponent c)
        {
            return sharedInstance;
        }

        /**
         * Overwrite the UI paint method to do nothing in order fix double
         * painting of the tooltip text.
         * @param g the <tt>Graphics</tt> object
         * @param c the component used to render the tooltip
         */
        @Override
        public void paint(Graphics g, JComponent c)
        {}

        /**
         * Override ComponentUI update method to set visibility of bottomText.
         * @param g <tt>Graphics</tt> object
         * @param c the component used to render the tooltip
         */
        @Override
        public void update(Graphics g, JComponent c)
        {
            JTextArea bottomTextArea =
                ((ExtendedTooltip)c).bottomTextArea;

            String bottomText = bottomTextArea.getText();
            if(bottomText == null || bottomText.length() <= 0)
                bottomTextArea.setVisible(false);
            else
                bottomTextArea.setVisible(true);
            super.update(g, c);
        }

        /**
         * Returns the size of the given component.
         * @param c the component used to render the tooltip
         * @return the size of the given component.
         */
        @Override
        public Dimension getPreferredSize(JComponent c)
        {
            ExtendedTooltip tooltip = (ExtendedTooltip)c;

            Icon icon = tooltip.imageLabel.getIcon();
            int width = 0;
            if (icon != null)
                width += icon.getIconWidth();

            if (tooltip.isListViewEnabled)
                width += tooltip.textWidth + 15;
            else
                width = tooltip.textWidth > width ? tooltip.textWidth : width;

            int imageHeight = 0;
            if (icon != null)
                imageHeight = icon.getIconHeight();

            int height;
            if (tooltip.isListViewEnabled)
            {
                height = imageHeight > tooltip.textHeight
                    ? imageHeight : tooltip.textHeight;
            }
            else
                height = imageHeight + tooltip.textHeight;

            String bottomText = tooltip.bottomTextArea.getText();
            if(bottomText != null && bottomText.length() > 0)
            {
                // Seems a little messy, but sets the proper size.
                tooltip.bottomTextArea.setColumns(5);
                tooltip.bottomTextArea.setSize(0,0);
                tooltip.bottomTextArea.setSize(
                    tooltip.bottomTextArea.getPreferredSize());

                height += tooltip.bottomTextArea.getPreferredSize().height;
            }

            return new Dimension(width, height);
        }
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @Override
    public String getUIClassID()
    {
        return uiClassID;
    }
}
