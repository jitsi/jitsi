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
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableModel;

import net.java.sip.communicator.impl.gui.customcontrols.BooleanToCheckTableModel;
import net.java.sip.communicator.impl.gui.customcontrols.LabelTableCellRenderer;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommMsgTextArea;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the account,
 * where the new contact will be created.
 * 
 * @author Yana Stamcheva
 */
public class SelectAccountPanel extends JPanel {
    
    private JScrollPane tablePane = new JScrollPane();
    
    private JTable accountsTable = new JTable();
        
    private BooleanToCheckTableModel tableModel
        = new BooleanToCheckTableModel();
    
    private NewContact newContact;
    
    private Iterator protocolProvidersList;
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private SIPCommMsgTextArea infoLabel 
        = new SIPCommMsgTextArea(Messages.getString("selectProvidersWizard"));
    
    private JLabel infoTitleLabel 
        = new JLabel(Messages.getString("selectProvidersWizardTitle"), 
                JLabel.CENTER);
    
    /**
     * Creates and initializes the <tt>SelectAccountPanel</tt>.
     * 
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param protocolProvidersList The list of available 
     * <tt>ProtocolProviderServices</tt>, from which the user could select.
     */
    public SelectAccountPanel(NewContact newContact, 
            Iterator protocolProvidersList) {
        super(new BorderLayout());
    
        this.setPreferredSize(new Dimension(500, 200));
        this.newContact = newContact;
        
        this.protocolProvidersList = protocolProvidersList;
    
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
       
        this.infoLabel.setEditable(false);
               
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
                
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        
        this.add(iconLabel, BorderLayout.WEST);
        
        this.add(rightPanel, BorderLayout.CENTER);
        
        this.tableInit();
    }  
    
    /**
     * Initializes the accounts table.
     */
    private void tableInit(){
        
        accountsTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        
        tableModel.addColumn("");
        tableModel.addColumn(Messages.getString("account"));
        tableModel.addColumn(Messages.getString("protocol"));
                
        while(protocolProvidersList.hasNext()) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)protocolProvidersList.next();
            
            String pName = pps.getProtocolName();
            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(
                    new ImageIcon(Constants.getProtocolIcon(pName)));
            
            tableModel.addRow(new Object[]{new Boolean(false),
                    pps, protocolLabel});
        }
        
        accountsTable.setRowHeight(22);
        accountsTable.setModel(tableModel);
        
        accountsTable.getColumnModel().getColumn(0).sizeWidthToFit();
        accountsTable.getColumnModel().getColumn(2)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tablePane.getViewport().add(accountsTable);
    }
    
    public void addCheckBoxCellListener(CellEditorListener l) {
        if(accountsTable.getModel().getRowCount() != 0) {
            accountsTable.getCellEditor(0, 0).addCellEditorListener(l);
        }
    }
    
    /**
     * Checks whether there is a selected check box in the table.
     * @return <code>true</code> if any of the check boxes is selected,
     * <code>false</code> otherwise.
     */
    public boolean isCheckBoxSelected(){
        boolean isSelected = false;
        TableModel model = accountsTable.getModel();
        
        for (int i = 0; i < accountsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);
            
            if (value instanceof Boolean) {
                Boolean check = (Boolean)value;
                if(check.booleanValue()){
                    isSelected = check.booleanValue();

                    newContact.addProtocolProvider(
                        (ProtocolProviderService)model.getValueAt(i, 1));
                }
            }
        }
        return isSelected;
    }
}
