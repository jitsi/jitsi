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
package net.java.sip.communicator.plugin.gibberishaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.util.osgi.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>GibberishAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Emil Ivov
 */
public class GibberishAccRegWizzActivator
    extends DependentActivator
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GibberishAccRegWizzActivator.class);

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    private static WizardContainer wizardContainer;

    private static GibberishAccountRegistrationWizard gibberishWizard;

    private static UIService uiService;

    public GibberishAccRegWizzActivator()
    {
        super(UIService.class);
    }

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    @Override
    public void startWithServices(BundleContext bc)
    {
        if (logger.isInfoEnabled())
            logger.info("Loading gibberish account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        gibberishWizard
            = new GibberishAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.GIBBERISH);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            gibberishWizard,
            containerFilter);

        if (logger.isInfoEnabled())
            logger.info("Gibberish account registration wizard [STARTED].");
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Gibberish protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Gibberish protocol
     */
    public static ProtocolProviderFactory getGibberishProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + "Gibberish" + ")";

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
