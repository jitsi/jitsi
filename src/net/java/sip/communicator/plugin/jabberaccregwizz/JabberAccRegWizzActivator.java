/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>JabberAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class JabberAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    /**
     * The OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(JabberAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static CredentialsStorageService credentialsService = null;

    private static CertificateService certService;

    private static WizardContainer wizardContainer;

    private static JabberAccountRegistrationWizard jabberWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     */
    @Override
    public void start(Object dependentService)
    {
        uiService = (UIService)dependentService;

        wizardContainer = uiService.getAccountRegWizardContainer();

        jabberWizard = new JabberAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.JABBER);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            jabberWizard,
            containerFilter);
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Jabber protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Jabber protocol
     */
    public static ProtocolProviderFactory getJabberProtocolProviderFactory()
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
            logger.error("JabberAccRegWizzActivator : " + ex);
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
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }

    /**
     * Returns the <tt>CredentialsStorageService</tt> obtained from the bundle
     * context.
     * @return the <tt>CredentialsStorageService</tt> obtained from the bundle
     * context
     */
    public static CredentialsStorageService getCredentialsService()
    {
        if (credentialsService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(CredentialsStorageService.class.getName());

            credentialsService = (CredentialsStorageService)bundleContext
                .getService(serviceReference);
        }

        return credentialsService;
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
     * Returns the <tt>CertificateService</tt> obtained from the bundle
     * context.
     * @return the <tt>CertificateService</tt> obtained from the bundle
     * context
     */
    public static CertificateService getCertificateService()
    {
        if (certService == null)
        {
            ServiceReference serviceReference = bundleContext
                    .getServiceReference(CertificateService.class.getName());

            certService = (CertificateService)bundleContext
                    .getService(serviceReference);
        }

        return certService;
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
            = Resources.getSettingsString(
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
