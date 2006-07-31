/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.ExtendedTableModel;
import net.java.sip.communicator.impl.gui.customcontrols.LabelTableCellRenderer;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.account.AccountRegWizardContainerImpl;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.plugin.icqaccregwizz.IcqAccRegWizzActivator;
import net.java.sip.communicator.service.gui.ConfigurationForm;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The <tt>AccountsConfigurationForm</tt> is the form where the user
 * could create, modify or delete an account.
 *
 * @author Yana Stamcheva
 */
public class AccountsConfigurationForm extends JPanel
    implements  ConfigurationForm,
                ActionListener,
                ServiceListener {

    private Logger logger = Logger.getLogger(
            AccountsConfigurationForm.class.getName());
    
    private JScrollPane tablePane = new JScrollPane();

    private JTable accountsTable = new JTable();

    private JPanel rightPanel = new JPanel(new BorderLayout());

    private JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

    private JButton newButton = new JButton(Messages.getString("new"));

    private JButton modifyButton = new JButton(Messages.getString("modify"));

    private JButton removeButton = new JButton(Messages.getString("remove"));

    private ExtendedTableModel tableModel = new ExtendedTableModel();

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>AccountsConfigurationForm</tt>.
     *
     * @param mainFrame the main application window
     */
    public AccountsConfigurationForm(MainFrame mainFrame) {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        GuiActivator.bundleContext.addServiceListener(this);

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

        ListSelectionModel.SINGLE_SELECTION);    

        accountsTable.setShowHorizontalLines(false);
        accountsTable.setShowVerticalLines(false);
        accountsTable.setModel(tableModel);

        tableModel.addColumn("id");
        tableModel.addColumn(Messages.getString("protocol"));
        tableModel.addColumn(Messages.getString("account"));

        TableColumnModel columnModel = accountsTable.getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(0));
        columnModel.getColumn(0)
            .setCellRenderer(new LabelTableCellRenderer());
        columnModel.getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.initializeAccountsTable();
        
        this.tablePane.getViewport().add(accountsTable);
    }

    /**
     * From all protocol provider factories obtains all already registered
     * accounts and adds them to the table.
     */
    private void initializeAccountsTable() {
        Set set = GuiActivator.getProtocolProviderFactories().entrySet();
        Iterator iter = set.iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) entry.getValue();
            
            ArrayList accountsList
                = providerFactory.getRegisteredAccounts();
            
            AccountID accountID;
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (int i = 0; i < accountsList.size(); i ++) {
                accountID = (AccountID) accountsList.get(i);

                serRef = providerFactory
                        .getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);
                
                String pName = protocolProvider.getProtocolName();
                JLabel protocolLabel = new JLabel();
                protocolLabel.setText(pName);
                protocolLabel.setIcon(
                        new ImageIcon(Constants.getProtocolIcon(pName)));

                tableModel.addRow(new Object[]{protocolProvider, protocolLabel,
                        accountID.getUserID()});
            }
        }
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
    public byte[] getIcon() {
        return ImageLoader.getImageInBytes(
                ImageLoader.QUICK_MENU_ADD_ICON);
    }

    /**
     * Returns the form of this configuration form.
     * @return the form of this configuration form.
     */
    public Object getForm() {
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
            
            wizard.newAccount();

            wizard.showModalDialog();
        }
        else if (sourceButton.equals(modifyButton)) {
            
            if(accountsTable.getSelectedRow() != -1) {
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
                
                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService)tableModel.getValueAt(
                        accountsTable.getSelectedRow(), 0);
                
                wizard.modifyAccount(protocolProvider);
                wizard.showModalDialog();
            }
        }
        else if(sourceButton.equals(removeButton)){
            
            if(accountsTable.getSelectedRow() != -1) {
                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService)tableModel.getValueAt(
                        accountsTable.getSelectedRow(), 0);
                
                ProtocolProviderFactory providerFactory 
                    = GuiActivator.getProtocolProviderFactory(protocolProvider);
                
                if(providerFactory != null) {
                    int result = JOptionPane.showConfirmDialog(this,
                            Messages.getString("removeAccountMessage"),
                            Messages.getString("removeAccount"),
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    
                    if(result == JOptionPane.YES_OPTION) {
                        providerFactory.uninstallAccount(
                            protocolProvider.getAccountID());
                    }
                }
            }
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event) {
        Object service = GuiActivator.bundleContext
            .getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (! (service instanceof ProtocolProviderService)) {
            return;
        }

        ProtocolProviderService pps = (ProtocolProviderService) service;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            String pName = pps.getProtocolName();
            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(
                    new ImageIcon(Constants.getProtocolIcon(pName)));

            tableModel.addRow(new Object[]{pps, protocolLabel,
                    pps.getAccountID().getUserID()});
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            tableModel.removeRow(tableModel.rowIndexOf(pps));
        }
    }
}
