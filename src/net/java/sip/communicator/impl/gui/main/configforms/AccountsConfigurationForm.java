/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.LabelTableCellRenderer;
import net.java.sip.communicator.impl.gui.customcontrols.NotEditableTableModel;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.account.AccountRegWizardContainerImpl;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * The <tt>AccountsConfigurationForm</tt> is the form where the user
 * could create, modify or delete an account.
 * 
 * @author Yana Stamcheva
 */
public class AccountsConfigurationForm extends JPanel 
    implements ConfigurationForm, ActionListener {

    private JScrollPane tablePane = new JScrollPane();
    
    private JTable accountsTable = new JTable();
    
    private JPanel rightPanel = new JPanel(new BorderLayout());
    
    private JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 8, 8));
    
    private JButton newButton = new JButton(Messages.getString("new"));
    
    private JButton modifyButton = new JButton(Messages.getString("modify"));
    
    private JButton removeButton = new JButton(Messages.getString("remove"));
    
    private NotEditableTableModel tableModel = new NotEditableTableModel();
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of <tt>AccountsConfigurationForm</tt>.
     * 
     * @param mainFrame the main application window
     */
    public AccountsConfigurationForm(MainFrame mainFrame) {
        super(new BorderLayout());
    
        this.mainFrame = mainFrame;
        
        this.tableInit();
        
        this.buttonsPanelInit();
        
        this.add(tablePane, BorderLayout.CENTER);
        this.add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Initializes the buttons panel.
     */
    private void buttonsPanelInit() {
        this.newButton.addActionListener(this);
        this.modifyButton.addActionListener(this);
        this.removeButton.addActionListener(this);
        
        this.buttonsPanel.add(newButton);
        this.buttonsPanel.add(modifyButton);
        this.buttonsPanel.add(removeButton);
        
        this.rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.rightPanel.add(buttonsPanel, BorderLayout.NORTH);
    }
    
    /**
     * Initializes the accounts table.
     */
    private void tableInit() {
        
        accountsTable.setRowHeight(22);
        accountsTable.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        
        tableModel.addColumn(Messages.getString("protocol"));
        tableModel.addColumn(Messages.getString("account"));
                
        Iterator i = mainFrame.getProtocolProviders();
        
        while(i.hasNext()) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)i.next();
            
            String pName = pps.getProtocolName();
            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(
                    new ImageIcon(Constants.getProtocolIcon(pName)));
            
            tableModel.addRow(new Object[]{protocolLabel,
                    pps.getAccountID().getAccountUserID()});
        }
        
        accountsTable.setShowHorizontalLines(false);
        accountsTable.setShowVerticalLines(false);
        accountsTable.setModel(tableModel);
        
        accountsTable.getColumnModel().getColumn(0)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tablePane.getViewport().add(accountsTable);
    }
    
    /**
     * Returns the title of this configuration form.
     * @return the title of this configuration form.
     */
    public String getTitle() {
        return Messages.getString("accounts");
    }

    /**
     * Returns the icon of this configuration form.
     * @return the icon of this configuration form.
     */
    public Icon getIcon() {
        return new ImageIcon(ImageLoader
                .getImage(ImageLoader.QUICK_MENU_ADD_ICON));
    }

    /**
     * Returns the form of this configuration form.
     * @return the form of this configuration form.
     */
    public Component getForm() {
        return this;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on
     * on the buttons. Shows the account registration wizard when user
     * clicks on "New".
     */
    public void actionPerformed(ActionEvent e) {
        JButton sourceButton = (JButton)e.getSource();
        
        if (sourceButton.equals(newButton)) {
            AccountRegWizardContainerImpl wizard
                = (AccountRegWizardContainerImpl)GuiActivator.getUIService()
                    .getAccountRegWizardContainer();
            
            wizard.setTitle(
                Messages.getString("accountRegistrationWizard"));
           
            wizard.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2 
                    - 250,
                Toolkit.getDefaultToolkit().getScreenSize().height/2 
                    - 100
            );
            
            wizard.showModalDialog();
        }
        else if (sourceButton.equals(modifyButton)) {
            
        }
        else {
            
        }
    }
}
