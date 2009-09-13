/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.*;

/**
 * The tooltip shown over a contact in the contact list.
 *
 * @author Yana Stamcheva
 */
public class ExtendedTooltip
    extends JToolTip
{
    private static final int textRowHeight = 15;

    private final JLabel imageLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();

    private final JPanel linesPanel = new JPanel();

    private int textWidth = 0;

    private int textHeight = 0;

    private boolean isListViewEnabled;

    /**
     * Created a <tt>MetaContactTooltip</tt>.
     */
    public ExtendedTooltip(boolean isListViewEnabled)
    {
        this.isListViewEnabled = isListViewEnabled;

        this.setUI(new ImageToolTipUI());

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        JPanel centerPanel = new JPanel(new BorderLayout());

        mainPanel.setOpaque(false);
        centerPanel.setOpaque(false);
        linesPanel.setOpaque(false);

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

        int stringWidth = GuiUtils.getStringWidth(titleLabel, titleText);

        if (textWidth < stringWidth)
            textWidth = stringWidth;

        textHeight += textRowHeight;
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     */
    public void addLine(ImageIcon icon,
                        String text)
    {
        JLabel lineLabel = new JLabel(  text,
                                        icon,
                                        JLabel.CENTER);

        linesPanel.add(lineLabel);

        int iconWidth = 0;
        if (icon != null)
            iconWidth = icon.getIconWidth();

        int stringWidth
            = GuiUtils.getStringWidth(lineLabel, text)
                + iconWidth
                + lineLabel.getIconTextGap();

        if (textWidth < stringWidth)
            textWidth = stringWidth;

        textHeight += textRowHeight;
    }

    /**
     * Customized UI for this MetaContactTooltip.
     */
    private class ImageToolTipUI extends MetalToolTipUI
    {
        /**
         * Overwrite the UI paint method to do nothing in order fix double
         * painting of the tooltip text.
         */
        public void paint(Graphics g, JComponent c)
        {}

        /**
         * Returns the size of the given component.
         * @return the size of the given component.
         */
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
                height = imageHeight > textHeight ? imageHeight : textHeight;
            else
                height = imageHeight + textHeight;

            return new Dimension(width, height);
        }
    }
}
