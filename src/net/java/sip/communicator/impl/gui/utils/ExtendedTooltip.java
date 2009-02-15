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
    private static final int textRowHeight = 25;

    private final JLabel imageLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();

    private final JPanel linesPanel = new JPanel();

    private int textWidth;

    private int textHeight;

    /**
     * Created a <tt>MetaContactTooltip</tt>.
     */
    public ExtendedTooltip()
    {
        this.setUI(new ImageToolTipUI());

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        JPanel centerPanel = new JPanel(new BorderLayout());

        mainPanel.setOpaque(false);
        centerPanel.setOpaque(false);
        linesPanel.setOpaque(false);

        mainPanel.add(imageLabel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        centerPanel.add(titleLabel, BorderLayout.NORTH);
        centerPanel.add(linesPanel, BorderLayout.CENTER);

        linesPanel.setLayout(
            new BoxLayout(linesPanel, BoxLayout.Y_AXIS));

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
     * Adds a protocol contact icon and name to this tooltip.
     * 
     * @param protocolContactIcon The icon for the protocol contact to add.
     * @param protocolContactName The name of the protocol contact to add.
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

            width += textWidth;

            int imageHeight = 0;
            if (icon != null)
                imageHeight = icon.getIconHeight();

            int height = imageHeight > textHeight ?
                imageHeight : textHeight;

            return new Dimension(width + 15, height);
        }
    }
}
