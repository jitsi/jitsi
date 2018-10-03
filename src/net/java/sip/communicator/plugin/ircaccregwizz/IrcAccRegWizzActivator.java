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
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>IrcAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lionel Ferreira & Michael Tarantino
 * @author Danny van Heumen
 */
public class IrcAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    private static Logger logger = Logger.getLogger(
        IrcAccRegWizzActivator.class.getName());

    /**
     * OSGi bundle context.
     */
    static BundleContext bundleContext;

    private static UIService uiService;

    private static WizardContainer wizardContainer;

    private IrcAccountRegistrationWizard ircWizard;

    /**
     * Resource management service instance.
     */
    private static ResourceManagementService resourceService;

    /**
     * Configuration Service instance.
     */
    private static ConfigurationService configService;

    /**
     * Start the IRC account registration wizard.
     *
     * @param dependentService dependent service
     */
    public void start(final Object dependentService)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Loading irc account wizard.");
        }

        uiService = (UIService) dependentService;

        wizardContainer = uiService.getAccountRegWizardContainer();

        ircWizard = new IrcAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter =
            new Hashtable<String, String>();
        containerFilter
            .put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(), ircWizard,
            containerFilter);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                IrcIgnoreConfigForm.class.getName(),
                getClass().getClassLoader(),
                null,
                "plugin.irc.IRC_IGNORE_CONFIG",
                0, true),
            containerFilter);

        if (logger.isInfoEnabled())
        {
            logger.info("IRC account registration wizard [STARTED].");
        }
    }

    /**
     * Returns dependent service class.
     *
     * @return returns dependent service class
     */
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * Set the bundle context.
     *
     * @param context bundle context
     */
    @Override
    public void setBundleContext(final BundleContext context)
    {
        IrcAccRegWizzActivator.bundleContext = context;
    }

    /**
     * Stop the IRC account registration wizard.
     *
     * @param bundleContext bundle context
     */
    public void stop(final BundleContext bundleContext)
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the IRC protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the IRC protocol
     */
    public static ProtocolProviderFactory getIrcProtocolProviderFactory()
    {
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL + "=IRC)";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Get UI Service instance.
     *
     * @return returns UIService instance
     */
    public static UIService getUIService()
    {
        return uiService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * Return the configuration service impl.
     *
     * @return the Configuration service
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService = ServiceUtils.getService(bundleContext, ConfigurationService.class);
        }
        return configService;
    }
}
