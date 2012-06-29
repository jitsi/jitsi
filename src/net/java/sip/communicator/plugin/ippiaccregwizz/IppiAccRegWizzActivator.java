/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ippiaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>IppiAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class IppiAccRegWizzActivator
    implements BundleActivator
{
    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

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

        IppiAccountRegistrationWizard wizard
            = new IppiAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                IppiAccountRegistrationWizard.PROTOCOL);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            wizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception {}

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
                .getService(IppiAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
