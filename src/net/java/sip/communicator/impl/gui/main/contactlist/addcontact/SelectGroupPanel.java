/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>SelectGroupPanel</tt> is where the user should select the group,
 * in which the new contact will be added.
 * 
 * @author Yana Stamcheva
 */
public class SelectGroupPanel
    extends JPanel
    implements  DocumentListener
{
    
    private JTable groupsTable = new JTable();
        
    private BooleanToCheckTableModel tableModel
        = new BooleanToCheckTableModel();
    
    private SIPCommMsgTextArea infoLabel 
        = new SIPCommMsgTextArea(Messages.getString("selectGroupWizard"));
    
    private JLabel infoTitleLabel = new JLabel(
            Messages.getString("selectGroupWizardTitle"));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
    
    private JScrollPane tablePane = new JScrollPane();
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JLabel createGroupLabel = new JLabel(
            Messages.getString("createGroup") + ":");
    
    private JTextField createGroupField = new JTextField();
    
    private JPanel createGroupPanel = new JPanel(new BorderLayout());
    
    private NewContact newContact;
    
    private WizardContainer parentWizard;
    
    private Iterator groupsList;
    
    /**
     * Creates an instance of <tt>SelectGroupPanel</tt>.
     * 
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param groupsList The list of all <tt>MetaContactGroup</tt>s, from which
     * the user could select.
     */
    public SelectGroupPanel(WizardContainer wizard, NewContact newContact, 
            Iterator groupsList) {
        super(new BorderLayout(10, 10));
    
        this.setPreferredSize(new Dimension(500, 200));
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        
        this.parentWizard = wizard;
        
        this.newContact = newContact;
        
        this.groupsList = groupsList;
           
        this.initGroupsTable();
                
        this.infoLabel.setEditable(false);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);

        this.createGroupPanel.add(createGroupLabel, BorderLayout.WEST);
        this.createGroupPanel.add(createGroupField, BorderLayout.CENTER);
        
        this.rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        this.rightPanel.add(createGroupPanel, BorderLayout.SOUTH);
        
        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
        
        this.createGroupField.getDocument().addDocumentListener(this);
    } 
    
    /**
     * Initializes the groups table.
     */
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
        
        groupsTable.setRowHeight(22);
        groupsTable.getColumnModel().getColumn(0).sizeWidthToFit();
        groupsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tablePane.getViewport().add(groupsTable);
    }
    
    public void addCheckBoxCellListener(CellEditorListener l) {
        if(groupsTable.getModel().getRowCount() != 0) {
            groupsTable.getCellEditor(0, 0).addCellEditorListener(l);
        }
    }
    
    /**
     * Checks whether there is a selected check box in the table.
     * @return <code>true</code> if any of the check boxes is selected,
     * <code>false</code> otherwise.
     */
    public boolean isCheckBoxSelected()
    {
        boolean isSelected = false;
        TableModel model = groupsTable.getModel();
        
        for (int i = 0; i < groupsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);
            
            if (value instanceof Boolean) {
                Boolean check = (Boolean)value;
                if (check.booleanValue()) {
                    isSelected = check.booleanValue();
                    break;
                }
            }
        }
        return isSelected;
    }

    /**
     * Adds all selected from user contact groups in the new contact.
     */
    public void addNewContactGroups()
    {
        TableModel model = groupsTable.getModel();
        
        for (int i = 0; i < groupsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);
            
            if (value instanceof Boolean) {
                Boolean check = (Boolean)value;
                if (check.booleanValue()) {             
                    newContact.addGroup(
                            (MetaContactGroup)model.getValueAt(i, 1));
                }
            }
        }
        
        String newGroup = createGroupField.getText();
        if(newGroup != null && newGroup != "") {
            newContact.setNewGroup(newGroup);
        }
    }
    
    public void changedUpdate(DocumentEvent e)
    {   
    }

    public void insertUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }

    public void removeUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }
    
    private void setNextFinishButtonAccordingToUIN()
    {
        if(createGroupField.getText() != null
                    || createGroupField.getText() != ""){
            parentWizard.setNextFinishButtonEnabled(true);
        }
        else {
            parentWizard.setNextFinishButtonEnabled(false);
        }
    }
}
