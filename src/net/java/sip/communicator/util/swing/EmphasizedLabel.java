/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

/**
 * A label with white shadow.
 * Based on code published on http://explodingpixels.wordpress.com.
 *
 * @author Yana Stamcheva
 */
public class EmphasizedLabel
    extends JLabel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private boolean fUseEmphasisColor;

    /**
     * The color used to paint the shadow.
     */
    public static final Color EMPHASIZED_FONT_COLOR =
            new Color(255, 255, 255, 110);

    /**
     * The color used to paint focused view.
     */
    public static final Color EMPHASIZED_FOCUSED_FONT_COLOR =
            new Color(0x000000);

    /**
     * The color used to paint unfocused view.
     */
    public static final Color EMPHASIZED_UNFOCUSED_FONT_COLOR =
            new Color(0x3f3f3f);

    /**
     * Creates an instance of <tt>EmphasizedLabel</tt>.
     *
     * @param text the text to show in this label
     */
    public EmphasizedLabel(String text)
    {
        super(text);
    }

    /**
     * Overrides the <tt>getPreferredSize()</tt> method in order to enlarge the
     * height of this label, which should welcome the lightening shadow.
     */
    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        d.height += 1;
        return d;
    }

    /**
     * Overrides the <tt>getForeground()</tt> method in order to provide
     * different foreground color, depending on whether the label is focused.
     */
    @Override
    public Color getForeground()
    {
        Color retVal;
        Window window = SwingUtilities.getWindowAncestor(this);
        boolean hasFoucs = window != null && window.isFocused();

        if (fUseEmphasisColor)
            retVal = EMPHASIZED_FONT_COLOR;
        else if (hasFoucs)
            retVal = EMPHASIZED_FOCUSED_FONT_COLOR;
        else
            retVal = EMPHASIZED_UNFOCUSED_FONT_COLOR;

        return retVal;
    }

    /**
     * Paints this label.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        fUseEmphasisColor = true;
        g.translate(0,1);
        super.paintComponent(g);
        g.translate(0,-1);
        fUseEmphasisColor = false;
        super.paintComponent(g);
    }
}
