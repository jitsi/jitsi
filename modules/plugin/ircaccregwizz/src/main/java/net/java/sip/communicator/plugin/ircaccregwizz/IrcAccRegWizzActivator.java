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

import lombok.extern.slf4j.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>IrcAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lionel Ferreira & Michael Tarantino
 * @author Danny van Heumen
 */
@Slf4j
public class IrcAccRegWizzActivator extends DependentActivator
{
    /**
     * OSGi bundle context.
     */
    static BundleContext bundleContext;

    public IrcAccRegWizzActivator()
    {
        super(
            ResourceManagementService.class,
            UIService.class
        );
    }

    /**
     * Start the IRC account registration wizard.
     */
    public void startWithServices(BundleContext bundleContext)
    {
        logger.info("Loading irc account wizard.");
        UIService uiService = getService(UIService.class);
        Resources.resourcesService =
            getService(ResourceManagementService.class);

        WizardContainer wizardContainer =
            uiService.getAccountRegWizardContainer();

        IrcAccountRegistrationWizard ircWizard =
            new IrcAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter = new Hashtable<>();
        containerFilter
            .put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(), ircWizard,
            containerFilter);

        logger.info("IRC account registration wizard [STARTED].");
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
            logger.error("Invalid OSGi filter", ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }
}
