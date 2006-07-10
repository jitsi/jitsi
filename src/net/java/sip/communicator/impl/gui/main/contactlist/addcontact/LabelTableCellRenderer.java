/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * Custom <tt>TableCellRenderer</tt> that renders
 * <tt>ProtocolProviderService</tt> objects and <tt>MetaContactGroup</tt>
 * objects.
 * 
 * @author Yana Stamcheva
 */
public class LabelTableCellRenderer extends JLabel
    implements TableCellRenderer {

    public LabelTableCellRenderer(){
        setHorizontalAlignment(JLabel.CENTER);
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        if(value instanceof JLabel) {
            JLabel label = (JLabel)value;
            
            this.setText(label.getText());
            this.setIcon(label.getIcon());
        }
        else if (value instanceof ProtocolProviderService) {
            ProtocolProviderService pps = (ProtocolProviderService)value;
            this.setText(pps.getAccountID().getAccountUserID());
        }
        else if (value instanceof MetaContactGroup) {
            MetaContactGroup group = (MetaContactGroup) value;
            this.setText(group.getGroupName());
        }        
        return this;
    }
}
