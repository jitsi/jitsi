/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import javax.swing.*;

/**
 * The <tt>BoxPopupMenu</tt> is a <tt>JPopupMenu</tt>, which orders its
 * components in a grid, where the column and row count are determined from
 * the total items count in order to have a grid closest to square.
 * 
 * @author Yana Stamcheva
 */
public class BoxPopupMenu extends JPopupMenu {

    private static final long serialVersionUID = -8488327445916201464L;

    private int itemsCount;

    private int gridRowCount = 0;

    private int gridColCount = 0;

    public BoxPopupMenu() {
        super();
    }

    /**
     * Creates an instance of <tt>BoxPopupMenu</tt>.
     * 
     * @param itemsCount The count of components that will be added to
     * the grid.
     */
    public BoxPopupMenu(int itemsCount) {

        this.itemsCount = itemsCount;

        this.calculateGridDimensions();

        this.setLayout(new GridLayout(
                this.gridRowCount, this.gridColCount, 5, 5));
    }

    /**
     * In order to have a popup which is at the form closest to sqware.
     */
    private void calculateGridDimensions()
    {
        this.gridRowCount = (int) Math.round(Math.sqrt(this.itemsCount));

        /*
         * FIXME The original code was "(int)Math.round(itemsCount/gridRowCount)".
         * But it was unnecessary because both itemsCount and gridRowCount are
         * integers and, consequently, itemsCount/gridRowCount gives an integer.
         * Was the intention to have the division produce a real number?
         */
        this.gridColCount = itemsCount / gridRowCount;
    }

    /**
     * Returns the location of the popup depending on the invoking component
     * coordinates.
     * 
     * @return the location of the popup depending on the invoking component
     * coordinates.
     */
    public Point getPopupLocation() {
        Component component = this.getInvoker();

        Point point = new Point();
        int x = component.getX();
        int y = component.getY();

        while (component.getParent() != null) {

            component = component.getParent();

            x += component.getX();
            y += component.getY();
        }

        point.x = x;
        point.y = y + this.getInvoker().getHeight();

        return point;
    }

    /**
     * Sets the count of components that will be added to the grid.
     * 
     * @param itemsCount the count of components that will be added to the grid.
     */
    public void setItemsCount(int itemsCount) {

        this.itemsCount = itemsCount;

        this.calculateGridDimensions();

        this.setLayout(new GridLayout(this.gridRowCount, this.gridColCount, 5,
                5));
    }

    /**
     * Returns the count of components that will be added to the grid.
     * @return the count of components that will be added to the grid.
     */
    public int getItemsCount() {
        return itemsCount;
    }
}
