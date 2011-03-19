/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>FacebookAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Dai Zhiwei
 */
public class FacebookAccRegWizzActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static Logger logger
        = Logger.getLogger(FacebookAccRegWizzActivator.class.getName());

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The container.
     */
    private static WizardContainer wizardContainer;

    /**
     * Registration wizard.
     */
    private static FacebookAccountRegistrationWizard facebookWizard;

    /**
     * The UI service.
     */
    private static UIService uiService;

    /**
     * The browser launcher service.
     */
    private static BrowserLauncherService browserLauncherService;

    private static ResourceManagementService resourcesService;

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    public void start(BundleContext bc)
    {
        if (logger.isInfoEnabled())
            logger.info("Loading facebook account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        facebookWizard
            = new FacebookAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.FACEBOOK);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            facebookWizard,
            containerFilter);

        if (logger.isInfoEnabled())
            logger.info("Facebook account registration wizard [STARTED].");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bundleContext)
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Facebook protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Facebook protocol
     */
    public static ProtocolProviderFactory getFacebookProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.JABBER + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("FacebookAccRegWizzActivator : " + ex);
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

    /**
     * Gets the browser launcher service.
     * @return the browser launcher service.
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(BrowserLauncherService.class
                    .getName());

            browserLauncherService =
                (BrowserLauncherService) bundleContext
                    .getService(serviceReference);
        }

        return browserLauncherService;
    }

    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils
                    .getService(FacebookAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
