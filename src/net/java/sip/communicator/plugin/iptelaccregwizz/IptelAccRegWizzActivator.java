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
package net.java.sip.communicator.plugin.iptelaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>GoogleTalkAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 */
public class IptelAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>Logger</tt> used by the <tt>IptelAccRegWizzActivator</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(IptelAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    private static ResourceManagementService resourcesService;

    private static UIService uiService;

    /**
     * Starts this bundle.
     */
    @Override
    public void start(Object dependentService)
    {
        uiService = (UIService)dependentService;

        IptelAccountRegistrationWizard wizard
            = new IptelAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                IptelAccountRegistrationWizard.PROTOCOL);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            wizard,
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

    public void stop(BundleContext bundleContext) throws Exception {}

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the IP Tel protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the IP Tel protocol
     */
    public static ProtocolProviderFactory getIptelProtocolProviderFactory()
    {
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.SIP + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("IptelAccRegWizzActivator : " + ex);
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
                .getService(IptelAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
