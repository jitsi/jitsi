/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.gui.AccountRegistrationWizardContainer;
import net.java.sip.communicator.service.gui.WizardContainer;

public class IcqAccountRegistrationWizard implements AccountRegistrationWizard {

    private FirstWizardPage firstWizardPage;
    
    private ArrayList pages = new ArrayList();
    
    private IcqAccountRegistration registration
        = new IcqAccountRegistration();
    
    public IcqAccountRegistrationWizard(WizardContainer wizardContainer) {
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);
        
        pages.add(firstWizardPage);        
    }
    
    public byte[] getIcon() {
        return Resources.getImage(Resources.ICQ_LOGO);
    }

    public String getProtocolName() {
        return Resources.getString("protocolName");
    }

    public String getProtocolDescription() {
        return Resources.getString("protocolDescription");
    }

    public Iterator getPages() {
        return pages.iterator();
    }

    public Iterator getSummary() {
        Hashtable summaryTable = new Hashtable();
        
        summaryTable.put("UIN", registration.getUin());
        summaryTable.put("Remember password", 
                new Boolean(registration.isRememberPassword()));
        
        return summaryTable.entrySet().iterator();
    }

    public void finish() {
        System.out.println("FINISH!!!!!!!!!!!!!!!!!!");
    }
}
