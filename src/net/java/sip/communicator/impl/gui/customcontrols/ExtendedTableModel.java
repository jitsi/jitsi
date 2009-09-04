/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.util.*;

import javax.swing.table.*;

/**
 * The <tt>ExtendedTableModel</tt> is a <tt>DefaultTableModel</tt> with one
 * method in addition that allow to obtain the row index from a value.
 *
 * @author Yana Stamcheva
 */
public class ExtendedTableModel extends DefaultTableModel
{
    private static final long serialVersionUID = 0L;

    /**
     * Returns the index of the row, in which the given value is contained.
     * @param value the value to search for
     * @return the index of the row, in which the given value is contained.
     */
    @SuppressWarnings("unchecked") //DefaultTableModel legacy code
    public int rowIndexOf(Object value)
    {
        Vector<Vector<Object>> dataVec = this.getDataVector();

        for(int i = 0; i < dataVector.size(); i ++) {
            Vector<Object> rowVector = dataVec.get(i);

            if(rowVector.contains(value)) {
                return i;
            }
        }
        return -1;
    }


    public boolean isCellEditable(int row, int col)
    {
        return false;
    }
}
