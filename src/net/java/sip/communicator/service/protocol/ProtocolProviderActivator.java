/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import org.jitsi.service.configuration.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

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
    static ConfigurationService getConfigurationService()
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

        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                new AccountManager(bundleContext), null);

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
}
