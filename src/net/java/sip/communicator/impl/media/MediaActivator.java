/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Invoke "Service Binder" to parse the service XML and register
 * all services.
 *
 * @author Martin Andre
 * @author Emil Ivov
 */
public class MediaActivator
    implements BundleActivator
{
    private final Logger logger = Logger.getLogger(MediaActivator.class.getName());

    private static MediaServiceImpl mediaServiceImpl = null;

    private static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;
    private static NetworkAddressManagerService networkAddressManagerService
                                                              = null;
    private static FileAccessService    fileAccessService     = null;
    private static ResourceManagementService resources;

    private ServiceRegistration mediaServiceRegistration      = null;

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
    public void start(BundleContext context)
        throws Exception
    {
        logger.debug("Started.");
/*
        MediaActivator.bundleContext = context;

        // MediaService
        mediaServiceImpl = new MediaServiceImpl();
        mediaServiceImpl.start();

        mediaServiceRegistration =
            context.registerService(MediaService.class.getName(),
                mediaServiceImpl, null);
        logger.debug("Media Service ... [REGISTERED]");

        // MediaConfigurationForm
        context.registerService(ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.impl.media.MediaConfigurationPanel",
                getClass().getClassLoader(),
                "plugin.mediaconfig.PLUGIN_ICON",
                "impl.media.configform.TITLE",
                40),
              null);

        //we use the nist-sdp stack to make parse sdp and we need to set the
        //following property to make sure that it would accept java generated
        //IPv6 addresses that contain address scope zones.
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");
*/
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
        if (configurationService == null)
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
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if (networkAddressManagerService == null)
        {
            ServiceReference namReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            networkAddressManagerService = (NetworkAddressManagerService)
                bundleContext.getService(namReference);
        }
        return networkAddressManagerService;
    }

    /**
     * Returns a reference to a FileAccessService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * FileAccessService .
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null && bundleContext != null)
        {
            ServiceReference faReference
                = bundleContext.getServiceReference(
                    FileAccessService.class.getName());

            fileAccessService = (FileAccessService)bundleContext
                .getService(faReference);
        }
        return fileAccessService;
    }

    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
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
    public void stop(BundleContext context)
        throws Exception
    {
        if (mediaServiceImpl != null)
        {
            mediaServiceImpl.stop();
            mediaServiceImpl = null;
        }
        if (mediaServiceRegistration != null)
        {
            mediaServiceRegistration.unregister();
            mediaServiceRegistration = null;
        }
    }

}
