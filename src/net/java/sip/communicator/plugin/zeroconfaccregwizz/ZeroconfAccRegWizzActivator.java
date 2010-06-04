/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>ZeroconfAccountRegistrationWizard</tt> in the UI Service.
 * 
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ZeroconfAccRegWizzActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(ZeroconfAccRegWizzActivator.class);

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    private static UIService uiService;

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    public void start(BundleContext bc)
    {
        if (logger.isInfoEnabled())
            logger.info("Loading zeroconf account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        WizardContainer wizardContainer
            = uiService.getAccountRegWizardContainer();

        ZeroconfAccountRegistrationWizard zeroconfWizard
            = new ZeroconfAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.ZEROCONF);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            zeroconfWizard,
            containerFilter);

        if (logger.isInfoEnabled())
            logger.info("Zeroconf account registration wizard [STARTED].");
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
     * Returns the <tt>ProtocolProviderFactory</tt> for the Zeroconf protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Zeroconf protocol
     */
    public static ProtocolProviderFactory getZeroconfProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.ZEROCONF + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }
        
        //System.out.println(" SerRefs " +serRefs);

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the bundleContext that we received when we were started.
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
