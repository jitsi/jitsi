/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.yahooaccregwizz;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>YahooAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class YahooAccRegWizzActivator implements BundleActivator {

    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
            YahooAccRegWizzActivator.class.getName());

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

        YahooAccountRegistrationWizard yahooWizard
            = new YahooAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(yahooWizard);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Yahoo protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Yahoo protocol
     */
    public static ProtocolProviderFactory getYahooProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.YAHOO+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("YahooAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }
}
