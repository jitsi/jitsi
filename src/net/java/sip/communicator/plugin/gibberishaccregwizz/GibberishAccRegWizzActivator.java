/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.gibberishaccregwizz;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>GibberishAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Emil Ivov
 */
public class GibberishAccRegWizzActivator
    implements BundleActivator
{
    private static Logger logger = Logger.getLogger(
        GibberishAccRegWizzActivator.class.getName());

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * A currently valid reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static AccountRegistrationWizardContainer wizardContainer;

    private static GibberishAccountRegistrationWizard gibberishWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    public void start(BundleContext bc)
    {
        logger.info("Loading gibberish account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        gibberishWizard
            = new GibberishAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(gibberishWizard);

        logger.info("Gibberish account registration wizard [STARTED].");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bundleContext)
    {
        wizardContainer.removeAccountRegistrationWizard(gibberishWizard);
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Gibberish protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Gibberish protocol
     */
    public static ProtocolProviderFactory getGibberishProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + "Gibberish" + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the bundleContext that we received when we were started.
     *
     * @return a currently valid instance of a bundleContext.
     */
    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns the <tt>UIService</tt>.
     * 
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        return uiService;
    }
}
