/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>SIPAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class SIPAccRegWizzActivator implements BundleActivator {

    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
            SIPAccRegWizzActivator.class.getName());

    private static ConfigurationService configService;

    /**
     * Starts this bundle.
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception {

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        UIService uiService
            = (UIService) bundleContext.getService(uiServiceRef);

        AccountRegistrationWizardContainer wizardContainer
            = uiService.getAccountRegWizardContainer();

        SIPAccountRegistrationWizard sipWizard
            = new SIPAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(sipWizard);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the SIP protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the SIP protocol
     */
    public static ProtocolProviderFactory getSIPProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.SIP+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("SIPAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);  
    }
}
