/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.configuration.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Activates the SIP package
 * @author Emil Ivov
 */
public class SipActivator
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(SipActivator.class.getName());

    private        ServiceRegistration  sipPpFactoryServReg   = null;
    private static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;

    private static ProtocolProviderFactorySipImpl sipProviderFactory = null;

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
        logger.debug("Started.");
        this.bundleContext = context;
        Hashtable hashtable = new Hashtable();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.SIP);

        sipProviderFactory = new ProtocolProviderFactorySipImpl();

        //load all icq providers
        sipProviderFactory.loadStoredAccounts();

        //reg the icq account man.
        sipPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    sipProviderFactory,
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
     * @return a reference to the <tt>ProtocolProviderFactorySipImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactorySipImpl getProtocolProviderFactory()
    {
        return sipProviderFactory;
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
        logger.debug("Stopped.");
    }
}
