/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;

/**
 * The entry point to the Version Service Implementation. We register the
 * VersionServiceImpl instance on the OSGi BUS.
 *
 * @author Emil Ivov
 */
public class VersionActivator
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(VersionActivator.class.getName());

    private        ServiceRegistration  versionServReg   = null;
            static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;

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
        if (logger.isDebugEnabled())
            logger.debug("Started.");
        VersionActivator.bundleContext = context;

        VersionServiceImpl versionServiceImpl = new VersionServiceImpl();

        //reg the icq account man.
        versionServReg =  context.registerService(
                    VersionService.class.getName(),
                    versionServiceImpl,
                    null);
        if (logger.isDebugEnabled())
            logger.debug("SIP Protocol Provider Factory ... [REGISTERED]");
        if (logger.isDebugEnabled())
            logger.debug("SIP Communicator Version: sip-communicator-"
                     + VersionImpl.currentVersion().toString());

        //register properties for those that would like to use them
        getConfigurationService().setProperty(
            "sip-communicator.version"
            , VersionImpl.currentVersion().toString()
            , true);

        getConfigurationService().setProperty(
            "sip-communicator.application.name"
            , VersionImpl.currentVersion().getApplicationName()
            , true);

    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
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
        versionServReg.unregister();
    }
}
