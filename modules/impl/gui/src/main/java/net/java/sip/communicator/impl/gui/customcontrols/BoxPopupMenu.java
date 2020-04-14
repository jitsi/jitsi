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
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>BoxPopupMenu</tt> is a <tt>JPopupMenu</tt>, which orders its
 * components in a grid, where the column and row count are determined from
 * the total items count in order to have a grid closest to square.
 *
 * @author Yana Stamcheva
 */
public class BoxPopupMenu
    extends SIPCommPopupMenu
{
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
