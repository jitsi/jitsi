/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;

import org.osgi.framework.*;

import sun.reflect.ReflectionFactory.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>SIPAccountRegistrationWizard</tt> in the UI Service.
 * 
 * @author Yana Stamcheva
 */
public class SIPAccRegWizzActivator
    implements BundleActivator
{

    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(SIPAccRegWizzActivator.class);

    private static WizardContainer wizardContainer;

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static BrowserLauncherService browserLauncherService;

    private static SIPAccountRegistrationWizard sipWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     * 
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {

        bundleContext = bc;

        ServiceReference uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        sipWizard = new SIPAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.SIP);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            sipWizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the SIP protocol.
     * 
     * @return the <tt>ProtocolProviderFactory</tt> for the SIP protocol
     */
    public static ProtocolProviderFactory getSIPProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter =
            "(" + ProtocolProviderFactory.PROTOCOL + "=" + ProtocolNames.SIP
                + ")";

        try
        {
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("SIPAccRegWizzActivator : " + ex);
            return null;
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

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService)bundleContext
                .getService(serviceReference);
        }

        return configService;
    }

    /**
     * Indicates if the advanced account configuration is currently disabled.
     *
     * @return <tt>true</tt> if the advanced account configuration is disabled,
     * otherwise returns false
     */
    public static boolean isAdvancedAccountConfigDisabled()
    {
        // Load the "net.java.sip.communicator.impl.gui.main.account
        // .ADVANCED_CONFIG_DISABLED" property.
        String advancedConfigDisabledDefaultProp
            = Resources.getResources().getSettingsString(
                "impl.gui.main.account.ADVANCED_CONFIG_DISABLED");

        boolean isAdvancedConfigDisabled = false;

        if (advancedConfigDisabledDefaultProp != null)
            isAdvancedConfigDisabled
                = Boolean.parseBoolean(advancedConfigDisabledDefaultProp);

        return getConfigurationService().getBoolean(
                "net.java.sip.communicator.impl.gui.main.account." +
                "ADVANCED_CONFIG_DISABLED",
                isAdvancedConfigDisabled);
    }
}
