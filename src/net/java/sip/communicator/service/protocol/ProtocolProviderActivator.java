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
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import java.util.*;

/**
 * Implements <code>BundleActivator</code> for the purposes of
 * protocol.jar/protocol.provider.manifest.mf and in order to register and start
 * services independent of the specifics of a particular protocol.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class ProtocolProviderActivator
    implements BundleActivator
{
    /**
     * The object used for logging.
     */
    private final static Logger logger
        = Logger.getLogger(ProtocolProviderActivator.class);

    /**
     * The <code>ServiceRegistration</code> of the <code>AccountManager</code>
     * implementation registered as a service by this activator and cached so
     * that the service in question can be properly disposed of upon stopping
     * this activator.
     */
    private ServiceRegistration accountManagerServiceRegistration;

    /**
     * The account manager.
     */
    private static AccountManager accountManager;

    /**
     * The <code>BundleContext</code> of the one and only
     * <code>ProtocolProviderActivator</code> instance which is currently
     * started.
     */
    private static BundleContext bundleContext;

    /**
     * The <code>ConfigurationService</code> used by the classes in the bundle
     * represented by <code>ProtocolProviderActivator</code>.
     */
    private static ConfigurationService configurationService;

    /**
     * The resource service through which we obtain localized strings.
     */
    private static ResourceManagementService resourceService;

    /**
     * The calendar service instance.
     */
    private static CalendarService calendarService;

    /**
     * The <code>SingleCallInProgressPolicy</code> making sure that the
     * <code>Call</code>s accessible in the <code>BundleContext</code> of this
     * activator will obey to the rule that a new <code>Call</code> should put
     * the other existing <code>Call</code>s on hold.
     */
    private SingleCallInProgressPolicy singleCallInProgressPolicy;

    /**
     * Gets the <code>ConfigurationService</code> to be used by the classes in
     * the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>ConfigurationService</code> to be used by the classes
     *         in the bundle represented by
     *         <code>ProtocolProviderActivator</code>
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = (ConfigurationService)
                    bundleContext.getService(
                        bundleContext.getServiceReference(
                            ConfigurationService.class.getName()));
        }
        return configurationService;
    }

    /**
     * Gets the <code>ResourceManagementService</code> to be used by the classes
     * in the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>ResourceManagementService</code> to be used by the
     *          classes in the bundle represented by
     *          <code>ProtocolProviderActivator</code>
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            resourceService
                = (ResourceManagementService)
                    bundleContext.getService(
                        bundleContext.getServiceReference(
                            ResourceManagementService.class.getName()));
        }
        return resourceService;
    }

    /**
     * Gets the <code>CalendarService</code> to be used by the classes
     * in the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>CalendarService</code> to be used by the
     *          classes in the bundle represented by
     *          <code>ProtocolProviderActivator</code>
     */
    public static CalendarService getCalendarService()
    {
        if (calendarService == null)
        {
            ServiceReference serviceReference
                = bundleContext.getServiceReference(
                    CalendarService.class.getName());
            if(serviceReference == null)
                return null;
            calendarService
                = (CalendarService)
                    bundleContext.getService(serviceReference);
        }
        return calendarService;
    }

    /**
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @param protocolName the name of the protocol, which factory we're
     * looking for
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            String protocolName)
    {
        String osgiFilter
            = "(" + ProtocolProviderFactory.PROTOCOL + "=" + protocolName + ")";
        ProtocolProviderFactory protocolProviderFactory = null;

        try
        {
            ServiceReference[] serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        osgiFilter);

            if ((serRefs != null) && (serRefs.length != 0))
            {
                protocolProviderFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRefs[0]);
            }
        }
        catch (InvalidSyntaxException ex)
        {
            if (logger.isInfoEnabled())
                logger.info("ProtocolProviderActivator : " + ex);
        }

        return protocolProviderFactory;
    }

    /**
     * Registers a new <code>AccountManagerImpl</code> instance as an
     * <code>AccountManager</code> service and starts a new
     * <code>SingleCallInProgressPolicy</code> instance to ensure that only one
     * of the <code>Call</code>s accessible in the <code>BundleContext</code>
     * in which this activator is to execute will be in progress and the others
     * will automatically be put on hold.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void start(BundleContext bundleContext)
    {
        ProtocolProviderActivator.bundleContext = bundleContext;

        accountManager = new AccountManager(bundleContext);
        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                accountManager, null);
        if(logger.isTraceEnabled())
        {
            logger.trace("ProtocolProviderActivator will create "
                + "SingleCallInProgressPolicy instance.");
        }

        singleCallInProgressPolicy =
            new SingleCallInProgressPolicy(bundleContext);
    }

    /**
     * Unregisters the <code>AccountManagerImpl</code> instance registered as an
     * <code>AccountManager</code> service in {@link #start(BundleContext)} and
     * stops the <code>SingleCallInProgressPolicy</code> started there as well.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void stop(BundleContext bundleContext)
    {
        if (accountManagerServiceRegistration != null)
        {
            accountManagerServiceRegistration.unregister();
            accountManagerServiceRegistration = null;
            accountManager = null;
        }

        if (singleCallInProgressPolicy != null)
        {
            singleCallInProgressPolicy.dispose();
            singleCallInProgressPolicy = null;
        }

        if (bundleContext.equals(ProtocolProviderActivator.bundleContext))
            ProtocolProviderActivator.bundleContext = null;

        configurationService = null;
        resourceService = null;
    }

    /**
     * Returns all protocol providers currently registered.
     * @return all protocol providers currently registered.
     */
    public static List<ProtocolProviderService>
        getProtocolProviders()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("ProtocolProviderActivator : " + e);
        }

        List<ProtocolProviderService>
            providersList = new ArrayList<ProtocolProviderService>();

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderService pp
                    = (ProtocolProviderService)bundleContext.getService(serRef);

                providersList.add(pp);
            }
        }
        return providersList;
    }

    /**
     * Get the <tt>AccountManager</tt> of the protocol.
     *
     * @return <tt>AccountManager</tt> of the protocol
     */
    public static AccountManager getAccountManager()
    {
        return accountManager;
    }

    /**
     * Returns OSGI bundle context.
     * @return OSGI bundle context.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
