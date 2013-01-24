/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.table.*;

/**
 * @author Vincent Lucas
 */
public abstract class MoveableTableModel
    extends AbstractTableModel
{
    /**
     * Move the row.
     *
     * @param rowIndex index of the row
     * @param up true to move up, false to move down
     8
     * @return the next row index
     */
    public abstract int move(int rowIndex, boolean up);
}
