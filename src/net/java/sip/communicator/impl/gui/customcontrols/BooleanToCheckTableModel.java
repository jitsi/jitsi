/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import javax.swing.table.DefaultTableModel;

/**
 * Custom table model, that allows represent a boolean value with a check
 * box.
 * 
 * @author Yana Stamcheva
 */
public class BooleanToCheckTableModel extends DefaultTableModel {

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the first column in the wizard would contain text 
     * ("true"/"false"), rather than a check box.
     */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
}
