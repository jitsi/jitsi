/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.rssaccregwizz;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>RssAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Emil Ivov
 */
public class RssAccRegWizzActivator
    implements BundleActivator
{
    private static Logger logger = Logger.getLogger(
        RssAccRegWizzActivator.class.getName());

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * A currently valid reference to the configuration service.
     */
    private static ConfigurationService configService;

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    public void start(BundleContext bc)
    {
        logger.info("Loading rss account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        UIService uiService
            = (UIService) bundleContext.getService(uiServiceRef);

        AccountRegistrationWizardContainer wizardContainer
            = uiService.getAccountRegWizardContainer();

        RssAccountRegistrationWizard rssWizard
            = new RssAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(rssWizard);

        logger.info("Rss account registration wizard [STARTED].");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {

    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Rss protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Rss protocol
     */
    public static ProtocolProviderFactory getRssProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + "Rss" + ")";

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
}
