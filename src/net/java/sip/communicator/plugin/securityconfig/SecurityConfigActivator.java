/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig;

import java.util.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class SecurityConfigActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static Logger logger
        = Logger.getLogger(SecurityConfigActivator.class);

    /**
     * The {@link BundleContext} of the {@link SecurityConfigActivator}.
     */
    public static BundleContext bundleContext;

    /**
     * The {@link ResourceManagementService} of the {@link OtrActivator}. Can
     * also be obtained from the {@link OtrActivator#bundleContext} on demand,
     * but we add it here for convinience.
     */
    private static ResourceManagementService resourceService;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>NeomediaActivator</tt> instance to read and write
     * configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * The Otr key manager service.
     */
    private static OtrKeyManagerService otrKeyManagerService;

    /**
     * The Otr engine service.
     */
    private static OtrEngineService otrEngineService;

    /**
     * Starts this plugin.
     * @param bc the BundleContext
     * @throws Exception if some of the operations executed in the start method
     * fails
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        // Register the configuration form.
        bundleContext.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.securityconfig.SecurityConfigurationPanel",
                getClass().getClassLoader(),
                "plugin.securityconfig.ICON",
                "plugin.securityconfig.TITLE", 20), null);
    }

    /**
     * Invoked when this bundle is stopped.
     * @param bc the BundleContext
     * @throws Exception if some of the operations executed in the start method
     * fails
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the ResourceManagementService
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            ServiceReference confReference
                = bundleContext
                    .getServiceReference(
                        ResourceManagementService.class.getName());

            resourceService
                = (ResourceManagementService)
                    bundleContext.getService(confReference);
        }
        return resourceService;
    }

    /**
     * Returns a reference to the ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference confReference
                = bundleContext
                    .getServiceReference(ConfigurationService.class.getName());

            configurationService
                = (ConfigurationService)
                    bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to the <tt>OtrEngineService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the <tt>OtrEngineService</tt>
     */
    public static OtrEngineService getOtrEngineService()
    {
        if (otrEngineService == null)
        {
            ServiceReference confReference
                = bundleContext
                    .getServiceReference(OtrEngineService.class.getName());

            otrEngineService
                = (OtrEngineService)
                    bundleContext.getService(confReference);
        }
        return otrEngineService;
    }

    /**
     * Returns a reference to the <tt>OtrKeyManagerService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * <tt>OtrKeyManagerService</tt>
     */
    public static OtrKeyManagerService getOtrKeyManagerService()
    {
        if (otrKeyManagerService == null)
        {
            ServiceReference confReference
                = bundleContext
                    .getServiceReference(OtrKeyManagerService.class.getName());

            otrKeyManagerService
                = (OtrKeyManagerService)
                    bundleContext.getService(confReference);
        }
        return otrKeyManagerService;
    }

    /**
     * Gets all the available accounts in SIP Communicator.
     * 
     * @return a {@link List} of {@link AccountID}.
     */
    public static List<AccountID> getAllAccountIDs()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap
            = getProtocolProviderFactories();

        if (providerFactoriesMap == null)
            return null;

        List<AccountID> accountIDs = new Vector<AccountID>();
        for (ProtocolProviderFactory providerFactory : providerFactoriesMap
            .values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                accountIDs.add(accountID);
            }
        }

        return accountIDs;
    }

    /**
     * Returns a <tt>Map</tt> of <ProtocolName, ProtocolProviderFactory> pairs.
     * @return a <tt>Map</tt> of <ProtocolName, ProtocolProviderFactory> pairs
     */
    private static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Error while retrieving service refs", ex);
            return null;
        }

        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            new Hashtable<Object, ProtocolProviderFactory>();
        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory =
                    (ProtocolProviderFactory) bundleContext.getService(serRef);

                providerFactoriesMap.put(serRef
                    .getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
