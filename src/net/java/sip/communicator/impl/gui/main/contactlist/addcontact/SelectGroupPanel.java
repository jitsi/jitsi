/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableModel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommTranspTextPane;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

public class SelectGroupPanel extends JPanel {
    
    private JTable groupsTable = new JTable();
        
    private CustomTableModel tableModel = new CustomTableModel();
    
    private SIPCommTranspTextPane infoLabel 
        = new SIPCommTranspTextPane(Messages.getString("selectGroupWizard"));
    
    private JLabel infoTitleLabel = new JLabel(
            Messages.getString("selectGroupWizardTitle"));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout());
    
    private JScrollPane tablePane = new JScrollPane();
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private NewContact newContact;
    
    private Iterator groupsList;
        
    public SelectGroupPanel(NewContact newContact, 
            Iterator groupsList) {
        super(new BorderLayout());
    
        this.setPreferredSize(new Dimension(500, 200));
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        
        this.newContact = newContact;
        
        this.groupsList = groupsList;
           
        this.initGroupsTable();
                
        this.infoLabel.setEditable(false);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        
        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    } 
    
    private void initGroupsTable(){
        groupsTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        
        tableModel.addColumn("");
        tableModel.addColumn(Messages.getString("group"));
        
        while(groupsList.hasNext()) {
            
            MetaContactGroup group
                = (MetaContactGroup)groupsList.next();
            
            tableModel.addRow(new Object[]{new Boolean(false), group});
        }
        
        groupsTable.setModel(tableModel);
        
        groupsTable.getColumnModel().getColumn(0).sizeWidthToFit();
        groupsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tablePane.getViewport().add(groupsTable);
    }
    
    public void addCheckBoxCellListener(CellEditorListener l) {
        groupsTable.getCellEditor(0, 0).addCellEditorListener(l);
    }
    
    public boolean isCheckBoxSelected(){
        boolean isSelected = false;
        TableModel model = groupsTable.getModel();
        
        for (int i = 0; i < groupsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);
            
            if (value instanceof Boolean) {
                Boolean check = (Boolean)value;
                if (check.booleanValue()) {
                    isSelected = check.booleanValue();
                    newContact.addGroup((MetaContactGroup)model.getValueAt(i, 1));
                }
            }
        }
        return isSelected;
    }
}
