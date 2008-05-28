/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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

    private I18NString newString = Messages.getI18NString("new");
    
    private I18NString modifyString = Messages.getI18NString("modify");
    
    private I18NString removeString = Messages.getI18NString("remove");
    
    private JButton newButton = new JButton(newString.getText());

    private JButton modifyButton = new JButton(modifyString.getText());

    private JButton removeButton = new JButton(removeString.getText());

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
        
        this.setPreferredSize(new Dimension(500, 400));
    }

    /**
     * Initializes the buttons panel.
     */
    private void buttonsPanelInit() {
        this.newButton.addActionListener(this);
        this.modifyButton.addActionListener(this);
        this.removeButton.addActionListener(this);

        this.newButton.setMnemonic(newString.getMnemonic());
        this.modifyButton.setMnemonic(modifyString.getMnemonic());
        this.removeButton.setMnemonic(removeString.getMnemonic());

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
        accountsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        tableModel.addColumn("id");
        tableModel.addColumn(Messages.getI18NString("protocol").getText());
        tableModel.addColumn(Messages.getI18NString("account").getText());
        
        TableColumnModel columnModel = accountsTable.getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(0));
        columnModel.getColumn(0)
            .setCellRenderer(new LabelTableCellRenderer());
        columnModel.getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());

        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(361);
        
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

                boolean isHidden = 
                    accountID.getAccountProperties().
                        get("HIDDEN_PROTOCOL") != null;

                if(isHidden)
                    continue;

                serRef = providerFactory
                        .getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                String pName = protocolProvider.getProtocolDisplayName();
                
                Image protocolImage = null;
                try
                {
                    protocolImage
                        = ImageIO.read(new ByteArrayInputStream(
                            protocolProvider.getProtocolIcon()
                                .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
                }
                catch (IOException e)
                {
                    logger.error("Could not read image.", e);
                }
                
                JLabel protocolLabel = new JLabel();
                protocolLabel.setText(pName);
                protocolLabel.setIcon(new ImageIcon(protocolImage));

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
        return Messages.getI18NString("accounts").getText();
    }

    /**
     * Returns the icon of this configuration form.
     * @return the icon of this configuration form.
     */
    public byte[] getIcon() {
        return ImageLoader.getImageInBytes(
                ImageLoader.ACCOUNT_ICON);
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
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt) {
        JButton sourceButton = (JButton)evt.getSource();

        if (sourceButton.equals(newButton)) {
            AccountRegWizardContainerImpl wizard
                = (AccountRegWizardContainerImpl)GuiActivator.getUIService()
                    .getAccountRegWizardContainer();

            wizard.setTitle(
                Messages.getI18NString("accountRegistrationWizard").getText());

            wizard.newAccount();

            wizard.showDialog(false);
        }
        else if (sourceButton.equals(modifyButton)) {

            if(accountsTable.getSelectedRow() != -1) {
                AccountRegWizardContainerImpl wizard
                    = (AccountRegWizardContainerImpl)GuiActivator.getUIService()
                        .getAccountRegWizardContainer();

                wizard.setTitle(
                    Messages.getI18NString("accountRegistrationWizard")
                        .getText());

                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService)tableModel.getValueAt(
                        accountsTable.getSelectedRow(), 0);

                wizard.modifyAccount(protocolProvider);
                wizard.showDialog(false);
            }
        }
        else if(sourceButton.equals(removeButton)){

            if(accountsTable.getSelectedRow() != -1) {
                mainFrame.getLoginManager().setManuallyDisconnected(true);

                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService)tableModel.getValueAt(
                        accountsTable.getSelectedRow(), 0);

                ProtocolProviderFactory providerFactory
                    = GuiActivator.getProtocolProviderFactory(protocolProvider);

                if(providerFactory != null) {
                    int result = JOptionPane.showConfirmDialog(this,
                        Messages.getI18NString("removeAccountMessage").getText(),
                        Messages.getI18NString("removeAccount").getText(),
                        JOptionPane.YES_NO_CANCEL_OPTION);

                    if(result == JOptionPane.YES_OPTION) {
                        ConfigurationService configService
                        = GuiActivator.getConfigurationService();

                    String prefix
                        = "net.java.sip.communicator.impl.gui.accounts";

                    List accounts = configService
                            .getPropertyNamesByPrefix(prefix, true);

                    Iterator accountsIter = accounts.iterator();

                    while(accountsIter.hasNext()) {

                        String accountRootPropName
                            = (String) accountsIter.next();

                        String accountUID
                            = configService.getString(accountRootPropName);

                        if(accountUID.equals(protocolProvider
                                .getAccountID().getAccountUniqueID())) {

                            configService.setProperty(
                                accountRootPropName,
                                null);
                            break;
                        }
                    }
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
    public void serviceChanged(ServiceEvent event)
    {
        //if the event is caused by a bundle being stopped, we don't want to
        //know
        if(event.getServiceReference().getBundle().getState()
            == Bundle.STOPPING)
        {
            return;
        }
        Object sourceService = GuiActivator.bundleContext
            .getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (! (sourceService instanceof ProtocolProviderService)) {
            return;
        }

        ProtocolProviderService pps = (ProtocolProviderService) sourceService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            String pName = pps.getProtocolDisplayName();
            
            Image protocolImage = null;
            try
            {
                protocolImage = ImageIO.read(
                    new ByteArrayInputStream(pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
            catch (IOException e)
            {
                logger.error("Could not read image.", e);
            }
            
            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(new ImageIcon(protocolImage));

            tableModel.addRow(new Object[]{pps, protocolLabel,
                    pps.getAccountID().getUserID()});
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            ProtocolProviderFactory sourceFactory = null;

            ServiceReference[] allBundleServices
                = event.getServiceReference().getBundle()
                    .getRegisteredServices();

            for (int i = 0; i < allBundleServices.length; i++)
            {
                Object service = GuiActivator.bundleContext
                    .getService(allBundleServices[i]);

                if(service instanceof ProtocolProviderFactory)
                {
                    sourceFactory = (ProtocolProviderFactory) service;
                    break;
                }
            }

            tableModel.removeRow(tableModel.rowIndexOf(pps));
        }
    }
}
