package net.java.sip.communicator.util.swing.plaf;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SearchTextFieldUI</tt> is the one responsible for the search field
 * look & feel. It draws a search icon inside the field and adjusts the bounds
 * of the editor rectangle according to it.
 *
 * @author Yana Stamcheva
 */
public class SearchTextFieldUI
    extends SIPCommTextFieldUI
{
    private ImageIcon searchImg;

    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public SearchTextFieldUI()
    {
        searchImg = UtilActivator.getResources()
            .getImage("service.gui.icons.SEARCH_ICON");
    }

    /**
     * Implements parent paintSafely method and enables antialiasing.
     * @param g the <tt>Graphics</tt> object that notified us
     */
    protected void paintSafely(Graphics g)
    {
        customPaintBackground(g);
        super.paintSafely(g);
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    protected void customPaintBackground(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g2);
            super.customPaintBackground(g2);

            JTextComponent c = this.getComponent();

            int dy = (c.getY() + c.getHeight()) / 2
                - searchImg.getIconHeight()/2;

            g2.drawImage(searchImg.getImage(), c.getX(), dy + 1, null);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    protected Rectangle getVisibleEditorRect()
    {
        Rectangle rect = super.getVisibleEditorRect();

        if ((rect.width > 0) && (rect.height > 0))
        {
            rect.x += searchImg.getIconWidth() + 8;
            rect.width -= searchImg.getIconWidth() + 15;
            return rect;
        }
        return null;
    }
}
