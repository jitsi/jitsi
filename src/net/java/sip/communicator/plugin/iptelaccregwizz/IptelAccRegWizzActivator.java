/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.iptelaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>GoogleTalkAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 */
public class IptelAccRegWizzActivator
    implements BundleActivator
{
    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>Logger</tt> used by the <tt>IptelAccRegWizzActivator</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(IptelAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    private static ResourceManagementService resourcesService;

    private static UIService uiService;

    /**
     * Starts this bundle.
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        uiService =
            (UIService) bundleContext.getService(bundleContext
                .getServiceReference(UIService.class.getName()));

        IptelAccountRegistrationWizard wizard
            = new IptelAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                IptelAccountRegistrationWizard.PROTOCOL);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            wizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception {}

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the IP Tel protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the IP Tel protocol
     */
    public static ProtocolProviderFactory getIptelProtocolProviderFactory()
    {
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.SIP + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("IptelAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
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
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            browserLauncherService
                = (BrowserLauncherService) bundleContext.getService(
                    bundleContext.getServiceReference(
                        BrowserLauncherService.class.getName()));
        }
        return browserLauncherService;
    }

    /**
     * Returns the service giving access to resources.
     * @return the service giving access to resources
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService = ResourceManagementServiceUtils
                .getService(IptelAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
