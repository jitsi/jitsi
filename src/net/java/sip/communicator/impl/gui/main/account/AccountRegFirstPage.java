/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import net.java.sip.communicator.impl.gui.customcontrols.ExtendedTableModel;
import net.java.sip.communicator.impl.gui.customcontrols.LabelTableCellRenderer;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.gui.WizardPage;
import net.java.sip.communicator.service.gui.event.AccountRegistrationEvent;
import net.java.sip.communicator.service.gui.event.AccountRegistrationListener;

/**
 * The <tt>AccountRegFirstPage</tt> is the first page of the account
 * registration wizard. This page contains a list of all registered
 * <tt>AccountRegistrationWizard</tt>s.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegFirstPage extends JPanel
    implements  AccountRegistrationListener,
                WizardPage,
                ListSelectionListener {
        
    private String nextPageIdentifier;

    private ExtendedTableModel tableModel;
    
    private JTable accountRegsTable = new JTable();
    
    private JScrollPane tableScrollPane = new JScrollPane();
    
    private AccountRegWizardContainerImpl wizardContainer;
    
    private JTextArea messageTextArea = new JTextArea(
                Messages.getString("selectAccountRegistration"));
    
    public AccountRegFirstPage(AccountRegWizardContainerImpl container) {
        super(new BorderLayout(10, 10));    
        
        this.wizardContainer = container;
        
        this.tableModel = new ExtendedTableModel();
        
        this.setPreferredSize(new Dimension(500, 200));
        
        this.accountRegsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        
        this.accountRegsTable.getSelectionModel()
            .addListSelectionListener(this);
        
        this.tableInit();
                
        this.messageTextArea.setLineWrap(true);
        this.messageTextArea.setWrapStyleWord(true);
        this.messageTextArea.setFont(
                Constants.FONT.deriveFont(Font.BOLD, 16f));
        
        this.add(messageTextArea, BorderLayout.NORTH);
        this.add(tableScrollPane, BorderLayout.CENTER);
        
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    /**
     * Initializes the account registration's table.
     */
    private void tableInit() {
        //The first column name is not internationalized because it's
        //only for internal use.
        this.tableModel.addColumn("id");
        this.tableModel.addColumn(Messages.getString("name"));
        this.tableModel.addColumn(Messages.getString("description"));
     
        accountRegsTable.setRowHeight(22);
        accountRegsTable.setShowHorizontalLines(false);
        accountRegsTable.setShowVerticalLines(false);
        accountRegsTable.setModel(this.tableModel);
                
        TableColumnModel columnModel = accountRegsTable.getColumnModel();
        
        columnModel.removeColumn(columnModel.getColumn(0));
        
        columnModel.getColumn(0)
            .setCellRenderer(new LabelTableCellRenderer());
        columnModel.getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());
        
        this.tableScrollPane.getViewport().add(accountRegsTable);
    }

    /**
     * When an <tt>AccountRegistrationWizard</tt> has been added to the
     * <tt>AccountRegistrationWizardContainer</tt> adds a line for this
     * wizard in the table.
     */
    public void accountRegistrationAdded(AccountRegistrationEvent event) {
                
        final AccountRegistrationWizard wizard 
            = (AccountRegistrationWizard)event.getSource();
        
        String pName = wizard.getProtocolName();
        
        final JLabel registrationLabel = new JLabel();
        registrationLabel.setText(pName);
        registrationLabel.setIcon(
                new ImageIcon(Constants.getProtocolIcon(pName)));
        
        this.tableModel.addRow(new Object[]{wizard, registrationLabel,
                        wizard.getProtocolDescription()});
    }

    /**
     * When an <tt>AccountRegistrationWizard</tt> has been removed from the
     * <tt>AccountRegistrationWizardContainer</tt> removes the corresponding
     * line from the table.
     */
    public void accountRegistrationRemoved(AccountRegistrationEvent event) {
        AccountRegistrationWizard wizard 
            = (AccountRegistrationWizard)event.getSource();
        
        tableModel.removeRow(tableModel.rowIndexOf(wizard));
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> method.
     * @return the page identifier, which in this case is the
     * DEFAULT_PAGE_IDENTIFIER, which means that this page is the default one
     * for the wizard.
     */
    public Object getIdentifier() {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> method. 
     * @return the identifier of the next wizard page, which in this case
     * is set dynamically when user selects a row in the table.
     */
    public Object getNextPageIdentifier() {
        if(nextPageIdentifier == null) {
            return WizardPage.DEFAULT_PAGE_IDENTIFIER;
        }
        else {
            return nextPageIdentifier;
        }
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> method. 
     * @return this identifier of the previous wizard page, which in this
     * case is null because this is the first page of the wizard.
     */
    public Object getBackPageIdentifier() {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> method.
     * @return this panel
     */
    public Object getWizardForm() {
        return this;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a checkbox is already selected or disables it if 
     * nothing is selected.
     */
    public void pageShowing() {
        if(accountRegsTable.getSelectedRow() > -1)
            this.wizardContainer.setNextFinishButtonEnabled(true);
        else
            this.wizardContainer.setNextFinishButtonEnabled(false);
        
        if(wizardContainer.getCurrentWizard() != null)
            this.wizardContainer.unregisterWizardPages();
        
        this.wizardContainer.removeWizzardIcon();
    }    
    
    /**
     * Handles the <tt>ListSelectionEvent</tt> triggered when user selects
     * a row in the contained in this page table. When a value is selected
     * enables the "Next" wizard button and shows the corresponding icon.
     */
    public void valueChanged(ListSelectionEvent e) {
        if(!wizardContainer.isNextFinishButtonEnabled())
            this.wizardContainer.setNextFinishButtonEnabled(true);
    }

    /**
     * Implements the <tt>WizardPage.pageNext</tt> method, which is invoked
     * from the wizard container when user clicks the "Next" button. We set
     * here the next page identifier to the identifier of the first page of
     * the choosen wizard and register all the pages contained in this wizard
     * in our wizard container.
     */
    public void pageNext() {
        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard)tableModel
                .getValueAt(accountRegsTable.getSelectedRow(), 0);
        
        this.wizardContainer.setCurrentWizard(wizard);
        
        Iterator i = wizard.getPages();
        boolean firstPage = true;
        
        Object identifier = null;
        
        while(i.hasNext()) {
            WizardPage page = (WizardPage)i.next();
            
            identifier = page.getIdentifier();
            
            if(firstPage) {
                firstPage = false;
                
                nextPageIdentifier = (String)identifier;
            }
            
            this.wizardContainer.registerWizardPage(identifier, page);
        }
        
        this.wizardContainer.getSummaryPage()
            .setPreviousPageIdentifier(identifier);
        
        try {
            this.wizardContainer.setWizzardIcon(
                ImageIO.read(new ByteArrayInputStream(wizard.getIcon())));
        }
        catch (IOException e1) {         
            e1.printStackTrace();
        }
    }

    public void pageBack() {
    }
    
    /**
     * Returns a list of all registered Account Registration Wizards.
     * @return a list of all registered Account Registration Wizards
     */
    public Iterator getWizardsList() {
        ArrayList wizardsList = new ArrayList();
        
        for(int i = 0; i < tableModel.getRowCount(); i ++) {
            wizardsList.add(tableModel.getValueAt(i, 0));
        }
        
        return wizardsList.iterator();
    }
}
