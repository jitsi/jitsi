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
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.ibm.media.bean.multiplayer.ImageLabel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class SelectAccountPanel extends JPanel {
    
    private JScrollPane tablePane = new JScrollPane();
    
    private JTable accountsTable = new JTable();
        
    private CustomTableModel tableModel = new CustomTableModel();
    
    private NewContact newContact;
    
    private ArrayList protocolProvidersList;
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private JTextPane infoLabel = new JTextPane();
    
    private JLabel infoTitleLabel 
        = new JLabel(Messages.getString("selectProvidersWizardTitle"), 
                JLabel.CENTER);
    
    public SelectAccountPanel(NewContact newContact, 
            ArrayList protocolProvidersList) {
        super(new BorderLayout());
    
        this.setPreferredSize(new Dimension(500, 200));
        this.newContact = newContact;
        
        this.protocolProvidersList = protocolProvidersList;
    
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
       
        this.infoLabel.setEditable(false);
        this.infoLabel.setText(Messages.getString("selectProvidersWizard"));
        
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
                
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        
        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        
        this.add(iconLabel, BorderLayout.WEST);
        
        this.add(rightPanel, BorderLayout.CENTER);
        
        this.tableInit();
    }  
    
    private void tableInit(){
        
        accountsTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        
        tableModel.addColumn("");
        tableModel.addColumn(Messages.getString("account"));
        tableModel.addColumn(Messages.getString("protocol"));
                
        for(int i = 0; i < protocolProvidersList.size(); i ++) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)protocolProvidersList.get(i);
            
            String pName = pps.getProtocolName();
            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(
                    new ImageIcon(Constants.getProtocolIcon(pName)));
            
            tableModel.addRow(new Object[]{new Boolean(false),
                    pps, protocolLabel});
        }
        
        accountsTable.setModel(tableModel);
        
        accountsTable.getColumnModel().getColumn(0).sizeWidthToFit();
        accountsTable.getColumnModel().getColumn(2)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tablePane.getViewport().add(accountsTable);
    }
    
    public void addCheckBoxCellListener(CellEditorListener l) {
        accountsTable.getCellEditor(0, 0).addCellEditorListener(l);
    }
    
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
