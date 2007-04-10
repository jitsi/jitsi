/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.aimaccregwizz;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>AimAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class AimAccRegWizzActivator implements BundleActivator {

    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
        AimAccRegWizzActivator.class);
    
    private static BrowserLauncherService browserLauncherService;
    
    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception {

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        UIService uiService
            = (UIService) bundleContext.getService(uiServiceRef);

        AccountRegistrationWizardContainer wizardContainer
            = uiService.getAccountRegWizardContainer();

        AimAccountRegistrationWizard aimWizard
            = new AimAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(aimWizard);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the AIM protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the AIM protocol
     */
    public static ProtocolProviderFactory getAimProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.AIM+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("AimAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
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
