/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The tooltip shown over a contact in the contact list.
 *
 * @author Yana Stamcheva
 */
public class ExtendedTooltip
    extends JToolTip
{
    private final JLabel imageLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();

    private final JPanel linesPanel = new JPanel();

    private final JTextArea bottomTextArea = new JTextArea();

    private int textWidth = 0;

    private int textHeight = 0;

    private boolean isListViewEnabled;

    /**
     * Created a <tt>MetaContactTooltip</tt>.
     * @param isListViewEnabled indicates if the list view is enabled
     */
    public ExtendedTooltip(final Window parentWindow, boolean isListViewEnabled)
    {
        this.isListViewEnabled = isListViewEnabled;

        this.setUI(new ImageToolTipUI());

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

        // Hide the tooltip when the parent window hides.
        parentWindow.addWindowFocusListener(new WindowFocusListener()
        {
            public void windowLostFocus(WindowEvent e)
            {
                Window popupWindow
                    = SwingUtilities.getWindowAncestor(ExtendedTooltip.this);

                if (popupWindow != null && popupWindow.isVisible())
                    popupWindow.setVisible(false);
            }

            public void windowGainedFocus(WindowEvent e) {}
        });

        // Hide the tooltip if the parent window isn't active
        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt)
            {
                if (!parentWindow.isActive())
                {
                    Window popupWindow
                        = SwingUtilities.getWindowAncestor(ExtendedTooltip.this);

                    if (popupWindow != null && popupWindow.isVisible())
                        popupWindow.setVisible(false);
                }
            }
        });

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

        Dimension labelSize = GuiUtils.getStringSize(titleLabel,titleText);
        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     */
    public void addLine(Icon icon,
                        String text)
    {
        JLabel lineLabel = new JLabel(  text,
                                        icon,
                                        JLabel.LEFT);

        linesPanel.add(lineLabel);

        Dimension labelSize = calculateLabelSize(lineLabel);

        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds the given array of labels as one line in this tool tip.
     *
     * @param labels the labels to add
     */
    public void addLine(JLabel[] labels)
    {
        Dimension lineSize = null;
        JPanel labelPanel = null;

        if (labels.length > 0)
        {
            labelPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.LEFT, 2, 0));
            linesPanel.add(labelPanel);
        }
        else
            return;

        if (labelPanel != null)
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

        recalculateTooltipSize(lineSize.width, lineSize.height);
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
            = GuiUtils.getStringWidth(label, text)
                + iconWidth
                + label.getIconTextGap();

        int textHeight = GuiUtils.getStringSize(label, text).height;

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
     * Customized UI for this MetaContactTooltip.
     */
    private class ImageToolTipUI extends MetalToolTipUI
    {
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
            Icon icon = imageLabel.getIcon();
            int width = 0;
            if (icon != null)
                width += icon.getIconWidth();

            if (isListViewEnabled)
                width += textWidth + 15;
            else
                width = textWidth > width ? textWidth : width;

            int imageHeight = 0;
            if (icon != null)
                imageHeight = icon.getIconHeight();

            int height = 0;
            if (isListViewEnabled)
            {
                height = imageHeight > textHeight ? imageHeight : textHeight;
            }
            else
                height = imageHeight + textHeight;

            String bottomText = bottomTextArea.getText();
            if(bottomText != null && bottomText.length() > 0)
            {
                // Seems a little messy, but sets the proper size.
                bottomTextArea.setColumns(5);
                bottomTextArea.setSize(0,0);
                bottomTextArea.setSize(bottomTextArea.getPreferredSize());

                height += bottomTextArea.getPreferredSize().height;
            }

            return new Dimension(width, height);
        }
    }
}
