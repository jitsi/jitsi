/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>JabberAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class JabberAccRegWizzActivator implements BundleActivator {

    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
            JabberAccRegWizzActivator.class.getName());

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

        JabberAccountRegistrationWizard jabberWizard
            = new JabberAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(jabberWizard);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Jabber protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Jabber protocol
     */
    public static ProtocolProviderFactory getJabberProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.JABBER+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("JabberAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfigurationService() {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }
}
