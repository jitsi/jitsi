/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.gui.WizardPage;

/**
 * The <tt>AccountRegSummaryPage</tt> is the last page of the account
 * registration wizard.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegSummaryPage extends JPanel
    implements WizardPage {
    
    private String backPageIdentifier;
    
    private JPanel keysPanel = new JPanel(new GridLayout(0, 1, 10, 10));
    
    private JPanel valuesPanel = new JPanel(new GridLayout(0, 1, 10, 10));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private AccountRegWizardContainerImpl wizardContainer;
    
    public AccountRegSummaryPage(
            AccountRegWizardContainerImpl wizardContainer) {
        
        super(new BorderLayout());
        
        this.wizardContainer = wizardContainer;
        
        this.mainPanel.add(keysPanel, BorderLayout.WEST);
        this.mainPanel.add(valuesPanel, BorderLayout.CENTER);
        
        this.add(mainPanel, BorderLayout.NORTH);
    }
    
    private void init(Iterator summaryData) {
        while(summaryData.hasNext()) {
            Map.Entry entry = (Map.Entry)summaryData.next();
            
            JLabel keyLabel = new JLabel(entry.getKey().toString()+ ":");
            JLabel valueLabel = new JLabel(entry.getValue().toString());
            
            keysPanel.add(keyLabel);
            valuesPanel.add(valueLabel);
        }
    }
    
    public Object getIdentifier() {
        return WizardPage.SUMMARY_PAGE_IDENTIFIER;
    }

    public Object getNextPageIdentifier() {
        return WizardPage.FINISH_PAGE_IDENTIFIER;
    }

    public Object getBackPageIdentifier() {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    public Object getWizardForm() {
        return this;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageShowing() {
        AccountRegistrationWizard wizard
            = this.wizardContainer.getFirstPage().getCurrentWizard();
        
        this.init(wizard.getSummary());
    }

    public void pageNext() {
        this.wizardContainer.getFirstPage().getCurrentWizard().finish();
    }

    public void pageBack() {
    }
}
