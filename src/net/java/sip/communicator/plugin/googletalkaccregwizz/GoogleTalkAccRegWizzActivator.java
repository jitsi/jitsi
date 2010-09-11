/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>GoogleTalkAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 */
public class GoogleTalkAccRegWizzActivator
    implements BundleActivator
{
    public static BundleContext bundleContext;

    /**
     * The <tt>Logger</tt> used by the <tt>GoogleTalkAccRegWizzActivator</tt>
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GoogleTalkAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

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

        GoogleTalkAccountRegistrationWizard wizard =
            new GoogleTalkAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                GoogleTalkAccountRegistrationWizard.PROTOCOL);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            wizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Google Talk
     * protocol.
     * 
     * @return the <tt>ProtocolProviderFactory</tt> for the Google Talk
     *         protocol
     */
    public static ProtocolProviderFactory getGoogleTalkProtocolProviderFactory()
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
            logger.error("GoogleTalkAccRegWizzActivator : " + ex);
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
    public static BrowserLauncherService getBrowserLauncher() {
        if (browserLauncherService == null)
        {
            browserLauncherService =
                (BrowserLauncherService) bundleContext
                    .getService(bundleContext
                        .getServiceReference(BrowserLauncherService.class
                            .getName()));
        }

        return browserLauncherService;
    }
}
