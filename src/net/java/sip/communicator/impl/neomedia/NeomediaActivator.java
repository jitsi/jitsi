/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class NeomediaActivator
    implements BundleActivator
{
    private final Logger logger = Logger.getLogger(NeomediaActivator.class);

    private static BundleContext bundleContext;

    private static ConfigurationService configurationService;

    private static FileAccessService fileAccessService;

    private static MediaServiceImpl mediaServiceImpl;

    private static NetworkAddressManagerService networkAddressManagerService;

    private static ResourceManagementService resources;

    private ServiceRegistration mediaServiceRegistration;

    /*
     * Implements BundleActivator#start(BundleContext).
     */
    public void start(BundleContext context)
        throws Exception
    {
        logger.debug("Started.");

        NeomediaActivator.bundleContext = context;

        // MediaService
        mediaServiceImpl = new MediaServiceImpl();
        mediaServiceImpl.start();

        mediaServiceRegistration
            = context
                .registerService(
                    MediaService.class.getName(),
                    mediaServiceImpl,
                    null);
        logger.debug("Media Service ... [REGISTERED]");

        // MediaConfigurationForm
        context
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        "net.java.sip.communicator.impl.neomedia.MediaConfigurationPanel",
                        getClass().getClassLoader(),
                        "plugin.mediaconfig.PLUGIN_ICON",
                        "impl.neomedia.configform.TITLE",
                        41),
                null);

        //we use the nist-sdp stack to make parse sdp and we need to set the
        //following property to make sure that it would accept java generated
        //IPv6 addresses that contain address scope zones.
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");
    }

    /*
     * Implements BundleActivator#stop(BundleContext).
     */
    public void stop(BundleContext context)
        throws Exception
    {
        mediaServiceImpl.stop();
        mediaServiceRegistration.unregister();
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     *
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
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
                = bundleContext
                    .getServiceReference(ConfigurationService.class.getName());

            configurationService
                = (ConfigurationService)
                    bundleContext.getService(confReference);
        }
        return configurationService;
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
                = bundleContext
                    .getServiceReference(FileAccessService.class.getName());

            fileAccessService
                = (FileAccessService) bundleContext.getService(faReference);
        }
        return fileAccessService;
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
                = bundleContext
                    .getServiceReference(
                        NetworkAddressManagerService.class.getName());

            networkAddressManagerService
                = (NetworkAddressManagerService)
                    bundleContext.getService(namReference);
        }
        return networkAddressManagerService;
    }

    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
    }
}
