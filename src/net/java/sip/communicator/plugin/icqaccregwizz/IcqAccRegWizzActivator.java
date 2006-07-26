/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import net.java.sip.communicator.service.gui.AccountRegistrationWizardContainer;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.WizardContainer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class IcqAccRegWizzActivator implements BundleActivator {

    public void start(BundleContext bundleContext) throws Exception {
        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());
    
        UIService uiService 
            = (UIService) bundleContext.getService(uiServiceRef);
        
        AccountRegistrationWizardContainer wizardContainer
            = uiService.getAccountRegWizardContainer();
        
        IcqAccountRegistrationWizard icqWizard
            = new IcqAccountRegistrationWizard(wizardContainer);
        
        wizardContainer.addAccountRegistrationWizard(icqWizard);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        
    }
}
