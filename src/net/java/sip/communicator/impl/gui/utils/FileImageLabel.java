/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FileImageLabel</tt> is a <tt>JLabel</tt> associated with a file. It
 * can be dragged to the file system or some other drop area. It also has an
 * extended tooltip that can show a file preview or any other image.
 * 
 * @author Yana Stamcheva
 */
public class FileImageLabel
    extends FileDragLabel
{
    private ImageIcon tooltipIcon;

    private String tooltipTitle;

    /**
     * Sets the icon to show in the tool tip.
     * 
     * @param icon the icon to show in the tool tip.
     */
    public void setToolTipImage(ImageIcon icon)
    {
        Image image = ImageUtils
            .scaleImageWithinBounds(icon.getImage(), 640, 480);

        this.tooltipIcon = new ImageIcon(image);
    }

    /**
     * Sets the text of the tool tip.
     * 
     * @param text the text to set
     */
    public void setToolTipText(String text)
    {
        super.setToolTipText("");

        this.tooltipTitle = text;
    }

    /**
     * Create tool tip.
     */
    public JToolTip createToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(false);

        if (tooltipIcon != null)
            tip.setImage(tooltipIcon);

        if (tooltipTitle != null)
            tip.setTitle(tooltipTitle);

        tip.setComponent(this);

        return tip;
    }

    /**
     * Returns the string to be used as the tooltip for <i>event</i>. We
     * don't really use this string, but we need to return different string
     * each time in order to make the TooltipManager change the tooltip over
     * the different cells in the JList.
     * 
     * @return the string to be used as the tooltip for <i>event</i>.
     */
    public String getToolTipText(MouseEvent event)
    {
        if (tooltipIcon != null)
            return tooltipIcon.toString();

        return "";
    }
}
