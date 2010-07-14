/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sip2sipaccregwizz;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>GoogleTalkAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class Sip2SipAccRegWizzActivator
    implements BundleActivator
{
    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(
        Sip2SipAccRegWizzActivator.class.getName());

    /**
     * The browser launcher service.
     */
    private static BrowserLauncherService browserLauncherService;

    /**
     * The resources service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The ui service.
     */
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

        System.setProperty(
            "http.agent",
            System.getProperty("sip-communicator.application.name")
                + "/" 
                + System.getProperty("sip-communicator.version"));

        uiService =
            (UIService) bundleContext.getService(bundleContext
                .getServiceReference(UIService.class.getName()));

        Sip2SipAccountRegistrationWizard wizard
            = new Sip2SipAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                Sip2SipAccountRegistrationWizard.PROTOCOL);

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
                .getService(Sip2SipAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
