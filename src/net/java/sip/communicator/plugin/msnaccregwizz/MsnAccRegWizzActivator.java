/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msnaccregwizz;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>MsnAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class MsnAccRegWizzActivator
    implements BundleActivator
{
    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(MsnAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    private static WizardContainer wizardContainer;

    private static MsnAccountRegistrationWizard msnWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception {

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService
            = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        msnWizard = new MsnAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.MSN);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            msnWizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Msn protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Msn protocol
     */
    public static ProtocolProviderFactory getMsnProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.MSN+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("MsnAccRegWizzActivator : " + ex);
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
        if (browserLauncherService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }
}
