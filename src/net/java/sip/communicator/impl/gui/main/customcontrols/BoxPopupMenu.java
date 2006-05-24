/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.JPopupMenu;

/**
 * @author Yana Stamcheva
 */
public class BoxPopupMenu extends JPopupMenu {

    private int itemsCount;

    private int gridRowCount = 0;

    private int gridColCount = 0;

    public BoxPopupMenu() {
        super();
    }

    public BoxPopupMenu(int itemsCount) {

        this.itemsCount = itemsCount;

        this.calculateGridDimensions();

        this.setLayout(new GridLayout(
                this.gridRowCount, this.gridColCount, 5, 5));
    }

    /**
     * In order to have a popup which is at the form closest to sqware.
     */
    private void calculateGridDimensions() {

        this.gridRowCount = (int) Math.round(Math.sqrt(this.itemsCount));

        this.gridColCount = (int) Math.round(this.itemsCount / gridRowCount);
    }

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

    public void setItemsCount(int itemsCount) {

        this.itemsCount = itemsCount;

        this.calculateGridDimensions();

        this.setLayout(new GridLayout(this.gridRowCount, this.gridColCount, 5,
                5));
    }

    public int getItemsCount() {
        return itemsCount;
    }
}