/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

/**
 * Loads the MSN provider factory and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Damian Minkov
 */
public class MsnActivator
    implements BundleActivator
{
    private        ServiceRegistration  msnPpFactoryServReg   = null;
    private static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;

    private static ProtocolProviderFactoryMsnImpl msnProviderFactory = null;
    
    private static ResourceManagementService resourcesService;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        MsnActivator.bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.MSN);

        msnProviderFactory = new ProtocolProviderFactoryMsnImpl();

        //reg the msn account man.
        msnPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    msnProviderFactory,
                    hashtable);
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return ConfigurationService a currently valid implementation of the
     * configuration service.
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryMsnImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactoryMsnImpl getProtocolProviderFactory()
    {
        return msnProviderFactory;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
        msnProviderFactory.stop();
        msnPpFactoryServReg.unregister();
    }
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }
}
