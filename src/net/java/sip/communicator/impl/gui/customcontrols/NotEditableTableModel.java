/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 * 
 * @author Yana Stamcheva
 */
public class NotEditableTableModel extends DefaultTableModel {
    /**
     * 
     */
    
    public int rowIndexOf(Object value) {
        Vector dataVector = this.getDataVector();
        
        for(int i = 0; i < dataVector.size(); i ++) {
            Vector rowVector = (Vector)dataVector.get(i);
            
            if(rowVector.contains(value)) {
                return i;
            }
        }
        return -1;
    }
}
