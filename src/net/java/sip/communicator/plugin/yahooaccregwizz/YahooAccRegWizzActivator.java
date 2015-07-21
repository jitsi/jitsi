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
package net.java.sip.communicator.plugin.yahooaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>YahooAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class YahooAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(YahooAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    private static WizardContainer wizardContainer;

    private static YahooAccountRegistrationWizard yahooWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     */
    @Override
    public void start(Object dependentService)
    {
        uiService = (UIService)dependentService;

        wizardContainer = uiService.getAccountRegWizardContainer();

        yahooWizard = new YahooAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.YAHOO);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            yahooWizard,
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

    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Yahoo protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Yahoo protocol
     */
    public static ProtocolProviderFactory getYahooProtocolProviderFactory()
    {
        ServiceReference[] serRefs = null;
        String osgiFilter
            = "("
                + ProtocolProviderFactory.PROTOCOL
                + "="
                + ProtocolNames.YAHOO
                + ")";

        try {
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("YahooAccRegWizzActivator : " + ex);
        }

        return
            (serRefs == null)
                ? null
                : (ProtocolProviderFactory)
                    bundleContext.getService(serRefs[0]);
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
