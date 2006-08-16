package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Loads the  ICQ provider factory and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Emil Ivov
 */
public class IcqActivator
    implements BundleActivator
{
    private        ServiceRegistration  icqPpFactoryServReg   = null;
    private static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;

    private static ProtocolProviderFactoryIcqImpl icqProviderFactory = null;

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
        this.bundleContext = context;
        Hashtable hashtable = new Hashtable();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.ICQ);

        icqProviderFactory = new ProtocolProviderFactoryIcqImpl();

        //load all icq providers
        icqProviderFactory.loadStoredAccounts();

        //reg the icq account man.
        icqPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    icqProviderFactory,
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
     * @return a reference to the <tt>ProtocolProviderFactoryIcqImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactoryIcqImpl getProtocolProviderFactory()
    {
        return icqProviderFactory;
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
        icqProviderFactory.stop();
        icqPpFactoryServReg.unregister();
    }
}
