/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import net.java.sip.communicator.service.gui.AccountRegistrationWizardContainer;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.slick.protocol.icq.IcqSlickFixture;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Registers the <tt>IcqAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class IcqAccRegWizzActivator implements BundleActivator {

    public static BundleContext bundleContext;
    
    private static Logger logger = Logger.getLogger(
            IcqAccRegWizzActivator.class.getName());
    
    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception {
    
        bundleContext = bc;
        
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
    
    public static ProtocolProviderFactory getIcqProtocolProviderFactory() {
        
        ServiceReference[] serRefs = null;
        
        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME
            + "="+ProtocolNames.ICQ+")";
        
        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("IcqAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }
}
